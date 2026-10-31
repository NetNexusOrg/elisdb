"""Pydantic models and the client name normalization used across the server."""

import re

from pydantic import BaseModel, ConfigDict, Field, field_validator


def normalize_nome(nome: str) -> str:
    """Trim and collapse inner whitespace so name-based duplicate detection is
    insensitive to surrounding/extra whitespace; uppercase so the stored/presented
    name is consistent regardless of input casing."""
    return re.sub(r"\s+", " ", nome).strip().upper()


class FotoIn(BaseModel):
    id: str
    mime: str
    ordem: int = 0

    @field_validator("id")
    @classmethod
    def _id_must_be_plain(cls, v: str) -> str:
        # Reject path separators and traversal sequences so photo files can only
        # ever be written inside the configured photos directory (CVE-style path
        # traversal). Client ids are generated filenames, so this is safe.
        if any(ch in v for ch in ("/", "\\")) or ".." in v:
            raise ValueError("photo id must not contain path separators or '..'")
        return v


class ClienteIn(BaseModel):
    id: str
    nome: str = Field(max_length=200)
    contrato: str | None = Field(None, max_length=100_000)
    latitude: float | None = None
    longitude: float | None = None
    notas: str | None = Field(None, max_length=100_000)
    ativo: bool = True
    updated_at: int


class RecordIn(BaseModel):
    client: ClienteIn
    fotos: list[FotoIn] = []


class PushRequest(BaseModel):
    records: list[RecordIn] = []


class FotoOut(BaseModel):
    id: str
    mime: str
    ordem: int = 0
    hash: str


class ClienteOut(BaseModel):
    id: str
    nome: str
    contrato: str | None = None
    latitude: float | None = None
    longitude: float | None = None
    notas: str | None = None
    ativo: bool = True
    updated_at: int
    created_at: int = 0
    # Server-assigned cursor for incremental pulls; independent of updated_at,
    # which stays the device timestamp that decides last-write-wins.
    delivery_ts: int = 0

    model_config = ConfigDict(from_attributes=True)


class ClienteComFotosOut(BaseModel):
    client: ClienteOut
    fotos: list[FotoOut] = []


class TombstoneOut(BaseModel):
    id: str
    deleted_at: int
    # Delivery cursor of the deletion (see ClienteOut.delivery_ts).
    delivery_ts: int = 0


class AuditFieldChange(BaseModel):
    """One client field changed by an accepted edit.

    ``old`` is null when the field was empty/missing before, ``new`` when it was
    cleared. Text values are shortened and flagged with ``truncated`` so a long
    notes body cannot bloat the audit trail.
    """

    field: str  # nome | contrato | notas | localizacao | ativo
    old: str | None = None
    new: str | None = None
    truncated: bool = False


class AuditPhotoChanges(BaseModel):
    """Photos added to / removed from a client by an accepted edit."""

    added: list[str] = []
    removed: list[str] = []
    reordered: bool = False


class AuditDetails(BaseModel):
    """What changed in an accepted write, as recorded next to the actor."""

    fields: list[AuditFieldChange] = []
    photos: AuditPhotoChanges | None = None


class AuditEntryOut(BaseModel):
    action: str  # create | edit
    username: str | None = None
    key_id: str
    ts: int
    # Older entries predate change tracking and carry no details.
    details: AuditDetails | None = None


class PushResult(BaseModel):
    id: str
    status: str  # accepted | duplicate | stale | error
    existing_id: str | None = None
    # Present only on "duplicate": the canonical record (client + photos) so the
    # client can reconcile without a separate round-trip.
    client: ClienteOut | None = None
    fotos: list[FotoOut] = []
    # Photo ids the push referenced whose content the server does not have; the
    # client must upload them again instead of recording them as sent.
    missing_photos: list[str] = []


class AppReleaseOut(BaseModel):
    """Metadata for the Android package published on the server."""

    version_name: str
    version_code: int
    size: int
    sha256: str
    filename: str
