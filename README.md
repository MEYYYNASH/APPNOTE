# GlassNotes (React SPA)

A clean glassmorphic mobile-first Notes web application rewritten from Android Jetpack Compose into modern React + TypeScript + Vite, preserving all core business logic and features.

- **GitHub Repository:** [https://github.com/MEYYYNASH/APPNOTE](https://github.com/MEYYYNASH/APPNOTE)
- **Framework:** React 19, TypeScript, Tailwind CSS, Vite

## Features

- **Frosted Glass UI System:** Translucent glassmorphism panels with 28px backdrop blur, saturation boost, 1px specular edges, ambient drifting color blobs, and 6 accent themes (Blue, Green, Amber, Violet, Pink, Slate).
- **Dual & System Theme Support:** Automatic System detection, Light mode, and Dark mode.
- **Rich Note Taking:**
  - Standard text notes with live word count and Markdown preview toggle.
  - Interactive checklists with progress tracking (`X of Y done`), animated checkboxes, and progress percentage bars.
  - Pinning & unpinning notes (pinned notes always sort to the top).
  - Folder categorization and comma-separated tags.
- **Rich Attachments:**
  - **Voice Memos:** In-app audio recording with live waveform animation bars, elapsed timer, speaker test tone, and inline player with progress bar.
  - **Photo Crop & Scanner:** Interactive crop tool with Free, 1:1, 4:3, and 16:9 aspect ratio presets, rotate 90°, and rule-of-thirds grid.
  - **Drawing Canvas:** Freehand sketch canvas with multi-color palette, touch/mouse drawing, and direct note attachment.
- **Version History & Snapshots:** Automated note version snapshot history with one-tap restore and full activity log.
- **Reminders:** Scheduled alarms with presets (10s, 15m, 1h, Tomorrow 9:00 AM) and custom date-time picker with audio chime alerts.
- **Search & Filters:** Real-time search across titles, contents, tags, and folders, with filter tabs (All, Pinned, Checklists, Archive, Trash) and expandable type/folder/date filters.
- **Bilingual English & Khmer:** Full interface localization in English and ភាសាខ្មែរ (Khmer).
- **Data & Privacy:** All data is kept locally in `localStorage` with in-memory fallback. Full JSON backup export (clipboard & file download) and restore support.
