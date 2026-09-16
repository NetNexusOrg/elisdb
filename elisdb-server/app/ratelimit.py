"""In-memory fixed-window rate limiter (single-process).

The sync server runs as a single uvicorn worker (see ``Dockerfile``), so a
process-local counter is sufficient. Each identifier (an authenticated key or a
client IP) gets a budget of requests per fixed window; excess requests are
rejected by the caller with HTTP 429.
"""

import threading
import time


class RateLimiter:
    """Fixed-window counter keyed by an opaque identifier string."""

    def __init__(self, max_requests: int, window_seconds: float) -> None:
        self.max_requests = max_requests
        self.window_seconds = window_seconds
        self._lock = threading.Lock()
        self._windows: dict[str, tuple[float, int]] = {}

    def allow(self, identifier: str) -> bool:
        """Record one request for ``identifier``; False when over the budget."""
        now = time.monotonic()
        with self._lock:
            self._prune(now)
            start, count = self._windows.get(identifier, (now, 0))
            if now - start >= self.window_seconds:
                start, count = now, 0
            if count >= self.max_requests:
                return False
            self._windows[identifier] = (start, count + 1)
            return True

    def _prune(self, now: float) -> None:
        expired = [
            ident
            for ident, (start, _) in self._windows.items()
            if now - start >= self.window_seconds
        ]
        for ident in expired:
            del self._windows[ident]
