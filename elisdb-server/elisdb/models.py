"""Pydantic models and the client name normalization used across the server."""

import re
import unicodedata

from pydantic import BaseModel, ConfigDict, Field, field_validator


def normalize_nome(nome: str) -> str:
    """Trim and collapse inner whitespace so name-based duplicate detection is
    insensitive to surrounding/extra whitespace; uppercase so the stored/presented
    name is consistent regardless of input casing."""
    return re.sub(r"\s+", " ", nome).strip().upper()


CATALOG_CATEGORIES = (
    "roedores",
    "insetos_rastejantes",
    "insetos_voadores",
    "tratamentos",
)
DEVICE_CATEGORIES = frozenset(
    {"roedores", "insetos_rastejantes", "insetos_voadores"}
)
MAX_CATALOG_NAME_LENGTH = 80
MAX_SERVICES_PER_CLIENT = 200
MAX_SERVICE_QUANTITY = 9999


def normalize_catalog_name(nome: str) -> str:
    """Normalize a catalog item name for duplicate detection only.

    Trims, collapses whitespace, removes diacritics and uppercases, so
    ``Rato\\u0301pido`` and ``RATOPIDO`` are the same name inside a category.
    """
    decomposed = unicodedata.normalize("NFKD", nome)
    without_diacritics = "".join(
        ch for ch in decomposed if not unicodedata.combining(ch)
    )
    return re.sub(r"\s+", " ", without_diacritics).strip().upper()


ALLOWED_PHOTO_MIMES = frozenset({"image/jpeg", "image/png", "image/webp"})

# Multiple tags live in the single `tag` text field, separated by `;`.
MAX_TAGS_PER_CLIENT = 10
MAX_TAG_LENGTH = 100
MAX_TAGS_FIELD_LENGTH = 500


def normalize_tags(value: str | None) -> str | None:
    """Return the canonical `;`-separated tag string.

    Tags are trimmed, uppercased, de-duplicated and sorted. Empty values and
    empty tags are dropped. Raises ValueError when the limits are exceeded.
    """
    if value is None:
        return None
    tags = sorted({part.strip().upper() for part in value.split(";") if part.strip()})
    if len(tags) > MAX_TAGS_PER_CLIENT:
        raise ValueError(f"too many tags (maximum {MAX_TAGS_PER_CLIENT})")
    if any(len(tag) > MAX_TAG_LENGTH for tag in tags):
        raise ValueError(f"tag too long (maximum {MAX_TAG_LENGTH} characters)")
    return ";".join(tags) if tags else None


def sniff_image_mime(data: bytes) -> str | None:
    """Return the supported image MIME type detected from the file signature."""
    if data.startswith(b"\xff\xd8\xff"):
        return "image/jpeg"
    if data.startswith(b"\x89PNG\r\n\x1a\n"):
        return "image/png"
    if len(data) >= 12 and data[:4] == b"RIFF" and data[8:12] == b"WEBP":
        return "image/webp"
    return None


class FotoIn(BaseModel):
    id: str
    mime: str
    ordem: int = 0
    # None means the push predates the profile-photo flag; the stored value is
    # preserved instead of being cleared.
    preferida: bool | None = None

    @field_validator("id")
    @classmethod
    def _id_must_be_plain(cls, v: str) -> str:
        # Reject path separators and traversal sequences so photo files can only
        # ever be written inside the configured photos directory (CVE-style path
        # traversal). Client ids are generated filenames, so this is safe.
        if any(ch in v for ch in ("/", "\\")) or ".." in v:
            raise ValueError("photo id must not contain path separators or '..'")
        return v

    @field_validator("mime")
    @classmethod
    def _mime_must_be_supported(cls, v: str) -> str:
        mime = v.strip().lower()
        if mime not in ALLOWED_PHOTO_MIMES:
            raise ValueError(f"unsupported photo mime type: {v}")
        return mime


class ClienteIn(BaseModel):
    id: str
    nome: str = Field(max_length=200)
    contrato: str | None = Field(None, max_length=100_000)
    tag: str | None = Field(None, max_length=MAX_TAGS_FIELD_LENGTH)
    latitude: float | None = None
    longitude: float | None = None
    notas: str | None = Field(None, max_length=100_000)
    ativo: bool = True
    updated_at: int
    # None preserves the stored associations; an empty list clears them and a
    # non-empty list replaces them atomically with the client record.
    servicos: list["ServicoIn"] | None = None

    @field_validator("tag")
    @classmethod
    def _normalize_tag_field(cls, v: str | None) -> str | None:
        return normalize_tags(v)

    @field_validator("servicos")
    @classmethod
    def _service_list_limits(
        cls, v: list["ServicoIn"] | None
    ) -> list["ServicoIn"] | None:
        if v is None:
            return None
        if len(v) > MAX_SERVICES_PER_CLIENT:
            raise ValueError(
                f"too many services (maximum {MAX_SERVICES_PER_CLIENT})"
            )
        ids = [service.item_id for service in v]
        if len(ids) != len(set(ids)):
            raise ValueError("duplicate catalog item in services")
        return v


class ServicoIn(BaseModel):
    item_id: str = Field(min_length=1, max_length=64)
    quantidade: int | None = None


class ServicoOut(BaseModel):
    item_id: str
    quantidade: int | None = None


class RecordIn(BaseModel):
    client: ClienteIn
    fotos: list[FotoIn] = []


class PushRequest(BaseModel):
    records: list[RecordIn] = []


class FotoOut(BaseModel):
    id: str
    mime: str
    ordem: int = 0
    preferida: bool = False
    hash: str | None = None
    # False when the metadata row exists but the server has lost the bytes.
    # Clients must keep their local copy instead of treating it as a deletion.
    available: bool = True


class ClienteOut(BaseModel):
    id: str
    nome: str
    contrato: str | None = None
    tag: str | None = None
    latitude: float | None = None
    longitude: float | None = None
    notas: str | None = None
    ativo: bool = True
    updated_at: int
    created_at: int = 0
    # Server-assigned cursor for incremental pulls; independent of updated_at,
    # which stays the device timestamp that decides last-write-wins.
    delivery_ts: int = 0
    # None means "not included" (the summary list); a list is the complete set.
    servicos: list[ServicoOut] | None = None

    model_config = ConfigDict(from_attributes=True)


class ClienteComFotosOut(BaseModel):
    client: ClienteOut
    fotos: list[FotoOut] = []
    # False would mean the photo list may be partial. Current server always
    # returns the full metadata set, including placeholders for missing bytes.
    photos_complete: bool = True


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

    field: str  # nome | contrato | tag | notas | localizacao | ativo
    old: str | None = None
    new: str | None = None
    truncated: bool = False


class AuditServiceChange(BaseModel):
    """One catalog association added, removed or re-quantified by a push.

    ``nome`` is the item name at the time of the change, so a later rename does
    not rewrite history.
    """

    item_id: str
    nome: str
    change: str  # added | removed | quantity
    old: int | None = None
    new: int | None = None


class AuditPhotoChanges(BaseModel):
    """Photos added to / removed from a client by an accepted edit."""

    added: list[str] = []
    removed: list[str] = []
    reordered: bool = False


class AuditDetails(BaseModel):
    """What changed in an accepted write, as recorded next to the actor."""

    fields: list[AuditFieldChange] = []
    photos: AuditPhotoChanges | None = None
    # Associations installed on the client, when the push changed them.
    services: list[AuditServiceChange] = []
    # Present on delete entries: the record as it was immediately before removal.
    deleted_client: dict | None = None


class AuditEntryOut(BaseModel):
    action: str  # create | edit | delete
    username: str | None = None
    key_id: str
    ts: int
    # Older entries predate change tracking and carry no details.
    details: AuditDetails | None = None


class HistoryPageOut(BaseModel):
    entries: list[AuditEntryOut]
    next_after_id: int | None = None


class ActivityEntryOut(AuditEntryOut):
    """An audit entry in the global activity log.

    Client entries name their client; catalog entries name the catalog item and
    carry no client.
    """

    kind: str = "client"
    cliente_id: str | None = None
    cliente_nome: str | None = None
    item_id: str | None = None
    item_nome: str | None = None


class ActivityPageOut(BaseModel):
    entries: list[ActivityEntryOut]
    # Cursor for the next (older) page; absent on the last page.
    next_before_id: int | None = None


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


class CatalogItemIn(BaseModel):
    id: str = Field(min_length=1, max_length=64)
    categoria: str
    nome: str = Field(min_length=1, max_length=MAX_CATALOG_NAME_LENGTH)
    ativo: bool = True
    updated_at: int

    @field_validator("id")
    @classmethod
    def _id_must_be_safe(cls, v: str) -> str:
        if not re.fullmatch(r"[A-Za-z0-9_-]{1,64}", v):
            raise ValueError("catalog id must use letters, digits, '_' or '-'")
        return v

    @field_validator("categoria")
    @classmethod
    def _category_must_exist(cls, v: str) -> str:
        if v not in CATALOG_CATEGORIES:
            raise ValueError(f"unknown catalog category: {v}")
        return v

    @field_validator("nome")
    @classmethod
    def _name_must_not_be_blank(cls, v: str) -> str:
        if not normalize_catalog_name(v):
            raise ValueError("catalog name cannot be blank")
        return v


class CatalogPushRequest(BaseModel):
    items: list[CatalogItemIn] = []


class CatalogItemOut(BaseModel):
    id: str
    categoria: str
    nome: str
    ativo: bool = True
    created_at: int = 0
    updated_at: int

    model_config = ConfigDict(from_attributes=True)


class CatalogResponse(BaseModel):
    items: list[CatalogItemOut] = []
    # True when the list is the complete server catalog, including inactive
    # items. A partial response would make clients keep their local copy.
    complete: bool = True


class CatalogPushResult(BaseModel):
    id: str
    status: str  # accepted | unchanged | stale | invalid
    reason: str | None = None
    item: CatalogItemOut | None = None


class AppReleaseOut(BaseModel):
    """Metadata for the Android package published on the server."""

    version_name: str
    version_code: int
    size: int
    sha256: str
    filename: str
