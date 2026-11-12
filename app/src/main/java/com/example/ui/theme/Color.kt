package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Exact CSS variables requested:
// Light:
// --bg1 / --bg2 (background): #cfe3ff / #e4d9ff
// --glass (large panels): rgba(255,255,255,.5)
// --card (cards): rgba(255,255,255,.55)
// --field (inputs, buttons): rgba(255,255,255,.6)
// --edge (borders): rgba(255,255,255,.75)
val LightBgStart = Color(0xFFCFE3FF)
val LightBgEnd = Color(0xFFE4D9FF)
val LightGlassPane = Color(red = 1f, green = 1f, blue = 1f, alpha = 0.50f)
val LightGlassCard = Color(red = 1f, green = 1f, blue = 1f, alpha = 0.55f)
val LightGlassField = Color(red = 1f, green = 1f, blue = 1f, alpha = 0.60f)
val LightGlassBorder = Color(red = 1f, green = 1f, blue = 1f, alpha = 0.75f)
val LightTextPrimary = Color(0xFF0D1B2E)
val LightTextSecondary = Color(0xFF52657D)

// Dark:
// --bg1 / --bg2 (background): #050d1c / #140b2e
// --glass (large panels): rgba(18,28,50,.5)
// --card (cards): rgba(255,255,255,.06)
// --field (inputs, buttons): rgba(255,255,255,.07)
// --edge (borders): rgba(255,255,255,.13)
val DarkBgStart = Color(0xFF050D1C)
val DarkBgEnd = Color(0xFF140B2E)
val DarkGlassPane = Color(red = 18f / 255f, green = 28f / 255f, blue = 50f / 255f, alpha = 0.50f)
val DarkGlassCard = Color(red = 1f, green = 1f, blue = 1f, alpha = 0.06f)
val DarkGlassField = Color(red = 1f, green = 1f, blue = 1f, alpha = 0.07f)
val DarkGlassBorder = Color(red = 1f, green = 1f, blue = 1f, alpha = 0.13f)
val DarkTextPrimary = Color(0xFFEAF1FF)
val DarkTextSecondary = Color(0xFF8FA3BF)

// Drifting Blobs (#3b82f6, #8b5cf6, #06b6d4 with --blob:.5 light / .4 dark)
val BlobBlueLight = Color(red = 0x3B / 255f, green = 0x82 / 255f, blue = 0xF6 / 255f, alpha = 0.50f)
val BlobVioletLight = Color(red = 0x8B / 255f, green = 0x5C / 255f, blue = 0xF6 / 255f, alpha = 0.50f)
val BlobCyanLight = Color(red = 0x06 / 255f, green = 0xB6 / 255f, blue = 0xD4 / 255f, alpha = 0.50f)

val BlobBlueDark = Color(red = 0x3B / 255f, green = 0x82 / 255f, blue = 0xF6 / 255f, alpha = 0.40f)
val BlobVioletDark = Color(red = 0x8B / 255f, green = 0x5C / 255f, blue = 0xF6 / 255f, alpha = 0.40f)
val BlobCyanDark = Color(red = 0x06 / 255f, green = 0xB6 / 255f, blue = 0xD4 / 255f, alpha = 0.40f)
