"""Ed25519 request signing and verification.

Message format (verified against the request):
    METHOD\\nPATH[?QUERY]\\nSHA256(body)\\nTIMESTAMP

The path component SHALL include the raw query string (as `?query`) when one is
present, so query parameters are covered by the signature. Clients produce the
signature with the app's embedded private key; the server verifies it against a
public key from the allowlist.
"""

import base64
import hashlib
import threading
import time

from cryptography.exceptions import InvalidSignature
from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PublicKey
from cryptography.hazmat.primitives.asymmetric.ed25519 import (
    Ed25519PrivateKey,
)

# Max clock skew accepted between client timestamp and server clock (seconds).
TIMESTAMP_WINDOW_SECONDS = 300

TIMESTAMP_HEADER = "X-Elisdb-Timestamp"
SIGNATURE_HEADER = "X-Elisdb-Signature"
KEY_HEADER = "X-Elisdb-Key"

# Replay cache: a captured, signed request can be replayed within the timestamp
# window, so once a signature has been seen and accepted it is remembered until
# its timestamp window expires. The cache is per-process and protected by a lock
# (uvicorn may serve requests on a threadpool).
_replay_lock = threading.Lock()
_replay_cache: dict[str, int] = {}


def _prune_replay(now: int) -> None:
    expired = [sig for sig, expiry in _replay_cache.items() if expiry <= now]
    for sig in expired:
        del _replay_cache[sig]


def is_replay(signature_b64: str, now: int | None = None) -> bool:
    """Return True if the signature was already consumed within its window."""
    now = int(time.time()) if now is None else now
    with _replay_lock:
        _prune_replay(now)
        return signature_b64 in _replay_cache


def record_replay(signature_b64: str, now: int | None = None) -> None:
    """Remember an accepted signature until its timestamp window expires."""
    now = int(time.time()) if now is None else now
    with _replay_lock:
        _replay_cache[signature_b64] = now + TIMESTAMP_WINDOW_SECONDS


def consume_replay(signature_b64: str, now: int | None = None) -> bool:
    """Atomically consume a signature.

    Returns True exactly once per signature within its timestamp window. The
    check and the record happen under the same lock, so two concurrent identical
    requests cannot both pass.
    """
    now = int(time.time()) if now is None else now
    with _replay_lock:
        _prune_replay(now)
        if signature_b64 in _replay_cache:
            return False
        _replay_cache[signature_b64] = now + TIMESTAMP_WINDOW_SECONDS
        return True


def build_message(method: str, path: str, body: bytes, timestamp: int) -> bytes:
    digest = hashlib.sha256(body).hexdigest()
    return f"{method}\n{path}\n{digest}\n{timestamp}".encode("utf-8")


def sign(message: bytes, private_key: Ed25519PrivateKey) -> str:
    return base64.b64encode(private_key.sign(message)).decode("ascii")


def verify_signature(
    public_key_b64: str, signature_b64: str, message: bytes
) -> bool:
    try:
        public_key = Ed25519PublicKey.from_public_bytes(base64.b64decode(public_key_b64))
        signature = base64.b64decode(signature_b64)
        public_key.verify(signature, message)
        return True
    except (ValueError, InvalidSignature, base64.binascii.Error):
        return False


def is_timestamp_fresh(timestamp: int, now: int | None = None) -> bool:
    now = int(time.time()) if now is None else now
    return abs(now - timestamp) <= TIMESTAMP_WINDOW_SECONDS
