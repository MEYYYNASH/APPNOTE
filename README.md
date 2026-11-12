# APPNOTE (GlassNotes)

Native Android frosted-glass notes application built with Kotlin and Jetpack Compose, connected directly to GitHub Releases for automatic APK updates.

- **Official Repository:** [https://github.com/MEYYYNASH/APPNOTE](https://github.com/MEYYYNASH/APPNOTE)
- **Latest Releases:** [https://github.com/MEYYYNASH/APPNOTE/releases](https://github.com/MEYYYNASH/APPNOTE/releases)
- **Pre-built Root APK:** [`GlassNotes.apk`](./GlassNotes.apk)

## Features

- **Frosted Glass UI System:** Custom Light & Dark glassmorphism tokens (`--glass`, `--card`, `--field`, `--edge`, `--bg1 / --bg2`) with 6 accent themes (Blue, Green, Amber, Violet, Pink, Slate).
- **Rich Note Taking:** Markdown text notes, checklists, voice recordings, photo cropping, document scanning, freehand drawing, reminders, version history, and Google Drive `.ttf/.otf` font import.
- **GitHub Releases Auto-Update:**
  - Connects to `https://api.github.com/repos/MEYYYNASH/APPNOTE/releases`
  - Verifies APK `SHA-256` checksum and Android signing certificate
  - Installs updates via Android `FileProvider` Package Installer
  - Includes in-app **Add to Releases (GitHub)** publisher and automated GitHub Actions CI/CD (`.github/workflows/android-release.yml`)
