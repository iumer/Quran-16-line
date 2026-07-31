# Quran 16-Line

Android reader for the Holy Quran with a **16-lines-per-page** mushaf layout (559 pages).

## V1 features
- Left/right swipeable pages (RTL mushaf navigation)
- Tap a line to highlight your reading point
- Bookmark pages
- Search by page, surah, or ayat
- Offline Arabic text (Uthmani)
- Bookish parchment UI (white / grey / warm yellow)

## Plan & prototype
- Engineering plan: [`docs/V1_PLAN.md`](docs/V1_PLAN.md)
- Interactive UI prototype: [`prototypes/reader.html`](prototypes/reader.html)

## Build
```bash
# Requires Android SDK 34 + JDK 17
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew assembleRelease
```

Installable artifact from this agent run:
- `/opt/cursor/artifacts/Quran16Line-v1.0.0.apk`

## Note on the PDF
No PDF was present in the repository at build time. V1 packs the complete Quran text into exactly **16 lines per page** across **559 pages**. Drop in your PDF later for pixel-identical mushaf pages (planned v1.1).
