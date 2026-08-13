# Holy Quran Reader (16-Line) — V1 Engineering Plan

## Goal
Ship a polished Android v1 APK for reading the Holy Quran with a **16-lines-per-page** mushaf layout, left/right swipe navigation, tap-to-highlight, bookmarks, and search (page / surah / ayat).

## Important constraint (current repo)
No PDF was present in the repository at build time. V1 therefore uses **structured Arabic Quran text** laid out into **exactly 16 lines per page**, targeting a **~559-page** IndoPak-style mushaf experience.

If you later provide the exact 559-page PDF, v1.1 can switch to image/PDF page rendering while keeping the same navigation, bookmark, and search UX.

---

## Product decisions for V1

| Area | Decision |
|------|----------|
| Platform | Native Android (Kotlin + Jetpack Compose) |
| Min SDK | 26 |
| Target SDK | 34 |
| Page model | Fixed **16 lines / page**, RTL mushaf feel |
| Navigation | Horizontal pager (swipe left/right) |
| Highlight | Tap a line/ayah → soft yellow highlight |
| Bookmarks | Local persistence (DataStore) |
| Search | Jump by page number, surah number/name, ayat number |
| Theme | White / warm grey / bookish parchment yellow |
| Offline | Fully offline after install |
| Audio / translation | Out of scope for v1 |

---

## Information architecture

```text
Splash / Home
  └─ Reader (default landing, last-read page)
       ├─ Top chrome (auto-hide): surah · page · bookmark · search
       ├─ Page canvas (16 lines, parchment)
       └─ Bottom chrome: page slider / quick jump

Search sheet
  ├─ Page number
  ├─ Surah picker
  └─ Ayat within surah

Bookmarks sheet
  └─ Saved pages with surah/ayat context → tap to open
```

---

## How the app will look

### Color system
- `--parchment`: `#F4EBD0` (page background)
- `--paper`: `#FFFDF8`
- `--ink`: `#1F1A14`
- `--muted`: `#6E675C`
- `--rule`: `#C9B896`
- `--highlight`: `#FFE08A` (tap highlight)
- `--chrome`: `#EFE7D6`

### Reader screen (primary)
- Full-bleed parchment page (not a floating card)
- Centered Arabic text, 16 fixed rows
- Thin ornamental top/bottom rules
- Surah header line when a new surah starts on the page
- Soft yellow highlight on the touched line
- Minimal chrome that fades while reading

### Search
- Bottom sheet, warm grey/cream panel
- Three clear entry modes: Page · Surah · Ayat
- Results jump directly into the pager

### Bookmarks
- Simple list of page + surah/ayat labels
- Star/bookmark toggle on reader chrome

See interactive HTML mock: `prototypes/reader.html`

---

## Architecture

```text
UI (Compose)
  ReaderScreen / SearchSheet / BookmarksSheet
        │
ViewModel
  page state, highlight, last-read, bookmarks, search
        │
Domain
  PageRepository · SearchIndex · BookmarkStore
        │
Data
  assets/quran_pages.json  (prebuilt 16-line pages)
  assets/surahs.json
  DataStore bookmarks + last page
```

### Page engine (build-time)
1. Load Uthmani Quran text.
2. Pack ayah tokens into visual lines (target width).
3. Emit pages of **exactly 16 lines**.
4. Attach metadata: pageNo, surah range, ayat range, line→ayah map.
5. Ship JSON in `assets/` for offline use.

### Touch highlight
Each line is a tappable Compose row. On tap:
- set `highlightedLineId`
- optionally persist “reading point” for resume

### Swipe
`HorizontalPager` with RTL layout direction so mushaf page-turning feels natural.

---

## Delivery phases

1. **Plan + prototypes** (this doc + HTML)
2. **App scaffold** Compose + theme + navigation shell
3. **Data pipeline** generate 16-line pages (~559)
4. **Core reader** swipe + highlight + last page
5. **Bookmark + search**
6. **APK build**
7. **UI/test subagents** → fix findings → final APK

---

## Risks & mitigations

| Risk | Mitigation |
|------|------------|
| Exact PDF line breaks unknown | Text layout approximates 16-line mushaf; PDF mode in v1.1 |
| Large Arabic font rendering | Bundle Noto Naskh Arabic; test on mdpi/xxhdpi |
| Search ambiguity (surah names) | Support Arabic + common English names + number |
| APK size | Text JSON + one font keeps APK modest |

---

## Recommended additions (beyond your list)

High value for a reader app:
1. **Resume last page** on launch
2. **Night / sepia reading modes** (sepia already close to brand)
3. **Font size control** (keep 16 lines; scale glyphs within the grid)
4. **Keep screen on** while reading
5. **Share ayah** text
6. **Juz / Hizb index** (common mushaf navigation)
7. **Translation toggle** (English) as optional overlay
8. **Audio recitation** (v2)
9. **Exact PDF/page-image mode** once your 559-page file is available

---

## Questions / assumptions used for V1

Assumptions made so delivery is not blocked:
1. **Script**: Uthmani Arabic is acceptable for v1.
2. **No PDF in repo**: text layout used instead of rasterized PDF pages.
3. **Highlight unit**: line (which maps to ayah range) rather than freehand ink.
4. **Bookmarks**: page-level (with ayah context), not multi-color annotations.
5. **Language of UI chrome**: English labels; Quran text Arabic.
6. **App name**: “Quran 16-Line”.

Please confirm later if you want:
- IndoPak Nastaliq glyph style vs Uthmani
- Exact PDF drop-in as source of truth
- Translation language(s)
- Audio in v2

## Update: PDF source (v1.1)
The app now renders the repository-uploaded Taj Company 16-line mushaf PDF:

`48AlQuranAlKareem16Lines-TajCompany-Www.momeen.blogspot.com-Www.quranpdf.blogspot.in.pdf`

Bundled as `app/src/main/assets/quran_16_lines.pdf` (559 pages). Tap highlight uses 16 equal horizontal line bands over each PDF page.
