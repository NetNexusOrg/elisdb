"""Unit tests for ed25519 request signing, freshness and replay protection."""

import base64
import hashlib
import time

from app import auth

from conftest import make_key


def test_build_message_covers_method_path_body_and_timestamp():
    message = auth.build_message("POST", "/api/push?x=1", b"hello", 1700000000)
    digest = hashlib.sha256(b"hello").hexdigest()
    assert message.decode() == f"POST\n/api/push?x=1\n{digest}\n1700000000"


def test_sign_and_verify_roundtrip():
    private, _seed, public = make_key()
    message = auth.build_message("GET", "/api/latest", b"", 1700000000)
    signature = auth.sign(message, private)
    assert auth.verify_signature(public, signature, message)


def test_verify_rejects_tampered_message():
    private, _seed, public = make_key()
    message = auth.build_message("GET", "/api/latest", b"", 1700000000)
    signature = auth.sign(message, private)
    tampered = auth.build_message("GET", "/api/latest", b"", 1700000001)
    assert not auth.verify_signature(public, signature, tampered)


def test_verify_rejects_wrong_key():
    private, _seed, _public = make_key()
    _other_private, _seed2, other_public = make_key()
    message = auth.build_message("GET", "/api/latest", b"", 1700000000)
    signature = auth.sign(message, private)
    assert not auth.verify_signature(other_public, signature, message)


def test_verify_rejects_malformed_input():
    assert not auth.verify_signature("not-base64!!", "also-not", b"msg")


def test_timestamp_freshness_window():
    now = 1_700_000_000
    assert auth.is_timestamp_fresh(now, now=now)
    assert auth.is_timestamp_fresh(now - auth.TIMESTAMP_WINDOW_SECONDS, now=now)
    assert auth.is_timestamp_fresh(now + auth.TIMESTAMP_WINDOW_SECONDS, now=now)
    assert not auth.is_timestamp_fresh(now - auth.TIMESTAMP_WINDOW_SECONDS - 1, now=now)
    assert not auth.is_timestamp_fresh(now + auth.TIMESTAMP_WINDOW_SECONDS + 1, now=now)


def test_replay_cache_records_and_expires():
    # A far-past timestamp keeps this test from pruning entries recorded at the
    # real 2026 clock by other tests.
    now = 1_700_000_000
    signature = base64.b64encode(b"unique-replay-signature").decode()
    assert not auth.is_replay(signature, now=now)
    auth.record_replay(signature, now=now)
    assert auth.is_replay(signature, now=now)
    assert auth.is_replay(signature, now=now + auth.TIMESTAMP_WINDOW_SECONDS - 1)
    assert not auth.is_replay(signature, now=now + auth.TIMESTAMP_WINDOW_SECONDS + 1)


def test_distinct_signatures_do_not_collide():
    now = int(time.time())
    a = base64.b64encode(b"a" * 64).decode()
    b = base64.b64encode(b"b" * 64).decode()
    auth.record_replay(a, now=now)
    assert auth.is_replay(a, now=now)
    assert not auth.is_replay(b, now=now)


def test_consume_replay_is_atomic_and_single_use():
    now = 1_700_000_000
    signature = base64.b64encode(b"consume-once").decode()
    assert auth.consume_replay(signature, now=now) is True
    assert auth.consume_replay(signature, now=now) is False
    # Expiry follows the same window as the replay cache.
    assert auth.consume_replay(signature, now=now + auth.TIMESTAMP_WINDOW_SECONDS + 1) is True


def test_consume_replay_keeps_future_timestamp_signature_until_it_goes_stale():
    now = 1_700_000_000
    signature = base64.b64encode(b"future-timestamp").decode()
    timestamp = now + auth.TIMESTAMP_WINDOW_SECONDS - 1

    assert auth.consume_replay(signature, now=now, timestamp=timestamp) is True
    # Still inside the signed timestamp's freshness window, so it must remain
    # cached even though the receive-time window has elapsed.
    assert auth.consume_replay(signature, now=now + 301, timestamp=timestamp) is False
    assert (
        auth.consume_replay(
            signature,
            now=timestamp + auth.TIMESTAMP_WINDOW_SECONDS + 1,
            timestamp=timestamp,
        )
        is True
    )
