#!/usr/bin/env python3
"""Valida ligações relativas e âncoras na documentação em Markdown.

Uso: python scripts/check-docs.py [raiz]

Sem dependências externas. Sai com código 1 e lista os problemas encontrados.
"""

from __future__ import annotations

import re
import sys
import unicodedata
from pathlib import Path

SKIP_DIRS = {
    ".git",
    ".venv",
    "node_modules",
    "build",
    ".gradle",
    ".idea",
    ".opencode",
    "openspec",
}
LINK = re.compile(r"\[[^\]]*\]\(([^)\s]+)(?:\s+\"[^\"]*\")?\)")
REF_LINK = re.compile(r"^\s{0,3}\[[^\]]+\]:\s*(\S+)", re.MULTILINE)
HEADING = re.compile(r"^\s{0,3}#{1,6}\s+(.*?)\s*#*\s*$", re.MULTILINE)
FENCE = re.compile(r"(```.*?```|~~~.*?~~~)", re.DOTALL)
INLINE_CODE = re.compile(r"`[^`\n]*`")


def slug(text: str) -> str:
    text = INLINE_CODE.sub("", text).strip().lower()
    text = unicodedata.normalize("NFKD", text)
    text = "".join(c for c in text if not unicodedata.combining(c))
    text = re.sub(r"[^\w\s-]", "", text)
    return re.sub(r"\s+", "-", text)


def anchors(path: Path) -> set[str]:
    text = FENCE.sub("", path.read_text(encoding="utf-8", errors="replace"))
    return {slug(m.group(1)) for m in HEADING.finditer(text)}


def markdown_files(root: Path) -> list[Path]:
    return sorted(
        p
        for p in root.rglob("*.md")
        if not any(part in SKIP_DIRS for part in p.relative_to(root).parts)
    )


def main(argv: list[str]) -> int:
    root = Path(argv[1] if len(argv) > 1 else ".").resolve()
    errors: list[str] = []
    cache: dict[Path, set[str]] = {}

    for doc in markdown_files(root):
        text = FENCE.sub("", doc.read_text(encoding="utf-8", errors="replace"))
        targets = LINK.findall(text) + REF_LINK.findall(text)
        for target in targets:
            if re.match(r"^[a-z][a-z0-9+.-]*:", target, re.I):  # http, mailto, ...
                continue
            path_part, _, anchor = target.partition("#")
            if not path_part:  # âncora no mesmo documento
                target_path = doc
            else:
                target_path = (doc.parent / path_part).resolve()
                if not target_path.exists():
                    errors.append(f"{doc.relative_to(root)}: destino inexistente -> {target}")
                    continue
            if anchor and target_path.suffix == ".md":
                if target_path not in cache:
                    cache[target_path] = anchors(target_path)
                if anchor.lower() not in cache[target_path]:
                    errors.append(
                        f"{doc.relative_to(root)}: âncora inexistente -> {target}"
                    )

    if errors:
        print("Ligações inválidas:")
        for error in sorted(set(errors)):
            print(f"  - {error}")
        return 1

    total = len(markdown_files(root))
    print(f"documentação ok: {total} ficheiros Markdown, todas as ligações válidas")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
