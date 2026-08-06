#!/usr/bin/env python3
"""Rebuild surah / ayah / page directory from the verified Taj 16-line layout map.

Source: tools/taj_layout_source/taj16_page_ayah_map.json
(derived from legeRise/quran-indopak-ayah-coordinates all_paras_enhanced —
manually verified ayah boxes for the Taj Company IndoPak 16-line mushaf).

App display page N == layout page N (PDF cover is skipped separately).
"""

from __future__ import annotations

import json
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app" / "src" / "main" / "assets"
DOCS = ROOT / "docs"
SOURCE = ROOT / "tools" / "taj_layout_source" / "taj16_page_ayah_map.json"

READER_PAGE_COUNT = 558  # 559 PDF pages − 1 skipped cover
LAYOUT_CONTENT_LAST = 549


def main() -> None:
    layout = json.loads(SOURCE.read_text(encoding="utf-8"))
    surahs = json.loads((ASSETS / "surahs.json").read_text(encoding="utf-8"))

    ayah_first_page: dict[str, int] = layout["ayah_first_page"]
    ayah_first_line: dict[str, int] = layout["ayah_first_line"]  # 1..16 visual
    surah_start_page = {int(k): v for k, v in layout["surah_start_page"].items()}
    surah_start_line = {int(k): v for k, v in layout["surah_start_line"].items()}
    page_ayahs = {int(k): v for k, v in layout["page_ayahs"].items()}

    # --- surahs.json ---
    for s in surahs:
        sid = s["id"]
        if sid not in surah_start_page:
            raise SystemExit(f"Missing surah start for {sid}")
        s["page"] = surah_start_page[sid]
        s["startLine"] = surah_start_line[sid]  # 1-based visual line

    # --- ayah_index.json ---
    expected_keys = []
    for s in surahs:
        for a in range(1, s["totalVerses"] + 1):
            expected_keys.append(f"{s['id']}:{a}")
    missing = [k for k in expected_keys if k not in ayah_first_page]
    if missing:
        raise SystemExit(f"Missing {len(missing)} ayahs e.g. {missing[:5]}")
    ayah_index = {k: ayah_first_page[k] for k in expected_keys}

    # Invert: page -> list of ayah keys in reading order
    by_page: dict[int, list[tuple[int, int]]] = defaultdict(list)
    for pn, items in sorted(page_ayahs.items()):
        for item in items:
            by_page[pn].append((item["surah"], item["ayah"]))

    name_by_id = {s["id"]: s["transliteration"] for s in surahs}

    def label_for(page: int, ayahs: list[tuple[int, int]]) -> dict:
        if page == 1 or not ayahs:
            if page == 1:
                return {
                    "page": page,
                    "printedPage": None,
                    "surahStart": None,
                    "surahEnd": None,
                    "ayahStart": None,
                    "ayahEnd": None,
                    "label": "Title",
                }
            return {
                "page": page,
                "printedPage": page,
                "surahStart": None,
                "surahEnd": None,
                "ayahStart": None,
                "ayahEnd": None,
                "label": "End matter",
            }

        surahs_on_page = []
        for s, _ in ayahs:
            if not surahs_on_page or surahs_on_page[-1] != s:
                surahs_on_page.append(s)
        first_s, first_a = ayahs[0]
        last_s, last_a = ayahs[-1]

        if len(surahs_on_page) == 1:
            label = f"{name_by_id[first_s]} · Ayah {first_a}–{last_a}"
            if first_a == last_a:
                label = f"{name_by_id[first_s]} · Ayah {first_a}"
        elif len(surahs_on_page) == 2:
            label = f"{name_by_id[surahs_on_page[0]]}–{name_by_id[surahs_on_page[1]]}"
        else:
            label = f"{name_by_id[surahs_on_page[0]]}–{name_by_id[surahs_on_page[-1]]}"

        return {
            "page": page,
            "printedPage": page,
            "surahStart": first_s,
            "surahEnd": last_s,
            "ayahStart": first_a,
            "ayahEnd": last_a,
            "label": label,
        }

    pages = []
    for page in range(1, READER_PAGE_COUNT + 1):
        pages.append(label_for(page, by_page.get(page, [])))

    page_index = {"pageCount": READER_PAGE_COUNT, "pages": pages}

    # --- logs / docs ---
    log = {
        "source_pdf": "app/src/main/assets/quran_16_lines.pdf",
        "layout_source": "tools/taj_layout_source/taj16_page_ayah_map.json",
        "layout_upstream": layout.get("source"),
        "pdf_page_count_reader": READER_PAGE_COUNT,
        "content_page_range": f"2-{LAYOUT_CONTENT_LAST}",
        "method": (
            "Full directory rebuilt from manually verified Taj Company 16-line "
            "IndoPak ayah bounding boxes (every ayah → page). Not Madani-scaled."
        ),
        "verified_spot_checks": {
            "1_Al-Fatihah": surah_start_page[1],
            "2_Al-Baqarah": surah_start_page[2],
            "54_Al-Qamar": surah_start_page[54],
            "55_Ar-Rahman": surah_start_page[55],
            "67_Al-Mulk": surah_start_page[67],
            "72_Al-Jinn": surah_start_page[72],
            "73_Al-Muzzammil": surah_start_page[73],
            "74_Al-Muddaththir": surah_start_page[74],
            "75_Al-Qiyamah": surah_start_page[75],
            "114_An-Nas": surah_start_page[114],
            "page_479": pages[478]["label"],
        },
        "surah_to_pdf_page": {str(k): v for k, v in sorted(surah_start_page.items())},
        "surah_start_line_1_based": {
            str(k): v for k, v in sorted(surah_start_line.items())
        },
        "ayah_count": len(ayah_index),
    }

    docs_index = {
        "title": "Taj Company 16-line surah/page directory",
        "method": log["method"],
        "content_page_range": log["content_page_range"],
        "verified_spot_checks": log["verified_spot_checks"],
        "surah_to_page": log["surah_to_pdf_page"],
    }

    (ASSETS / "surahs.json").write_text(
        json.dumps(surahs, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    (ASSETS / "ayah_index.json").write_text(
        json.dumps(ayah_index, ensure_ascii=False, separators=(",", ":")) + "\n",
        encoding="utf-8",
    )
    (ASSETS / "page_index.json").write_text(
        json.dumps(page_index, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    (ASSETS / "surah_page_log.json").write_text(
        json.dumps(log, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    DOCS.mkdir(exist_ok=True)
    (DOCS / "SURAH_PAGE_INDEX.json").write_text(
        json.dumps(docs_index, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )

    # Sanity: client bug page
    p479 = pages[478]
    assert p479["surahStart"] == 54 and p479["surahEnd"] == 55, p479
    assert "Rahman" in p479["label"] or "Qamar" in p479["label"], p479
    assert surah_start_page[55] == 479
    assert ayah_index["55:1"] == 479
    assert ayah_index["54:48"] == 478  # starts end of 478, continues onto 479
    assert ayah_index["54:55"] == 479
    print("Rebuilt directory:")
    print(f"  surahs={len(surahs)} ayahs={len(ayah_index)} pages={len(pages)}")
    print(f"  page 479 → {p479['label']}")
    print(f"  Ar-Rahman starts page {surah_start_page[55]} line {surah_start_line[55]}")
    print(f"  An-Nas starts page {surah_start_page[114]}")


if __name__ == "__main__":
    main()
