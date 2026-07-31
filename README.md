# Quran 16-Line

Android reader for the Holy Quran using the **Taj Company 16-lines mushaf PDF** (559 pages).

## Source PDF
The app ships the repository PDF:

`48AlQuranAlKareem16Lines-TajCompany-Www.momeen.blogspot.com-Www.quranpdf.blogspot.in.pdf`

Bundled renderable copy: `app/src/main/assets/quran_16_lines.pdf`

## V1.1 features
- Exact mushaf pages via Android `PdfRenderer`
- Left/right swipe (RTL)
- Tap a line band to highlight (16 equal lines per page)
- Bookmark pages
- Search by page, surah, or ayat
- Offline

## Download
Always the same link (APK is overwritten on each release):

https://raw.githubusercontent.com/iumer/Quran-16-line/cursor/quran-reader-v1-76fd/dist/Quran16Line.apk

## Build
```bash
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew assembleRelease
```

## Docs
- Plan: `docs/V1_PLAN.md`
- Prototype: `prototypes/reader.html`
