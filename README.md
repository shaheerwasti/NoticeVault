# NoticeVault v2.0.0

Silently archive all Android notifications. Keep your notification bar clean.
Review everything as a report whenever you want.

## Features
- Silently intercepts and archives ALL notifications
- Floating bubble shows unread count, tap to open report
- AI categorization (local keywords + Claude API fallback)
- Manual app whitelist
- Export report as CSV
- Daily 8 AM digest
- 100% on-device storage

## Build & Install (via GitHub Actions)

1. Fork or upload this repo to your GitHub account
2. Go to Actions tab > "Build NoticeVault APK" > Run workflow
3. Download `NoticeVault-v2.0.0-debug.apk` from the Artifacts section
4. Transfer APK to your phone and install it

## First-Time Setup on Phone

1. Open NoticeVault
2. Grant **Notification Access**: Settings > Apps > Special App Access > Notification Access
3. Grant **Display Over Other Apps**: prompted automatically
4. (Optional) Go to Settings in the app and add your Claude API key for smarter categorization
5. Enter package names in the whitelist for any apps whose notifications you DO want to see normally

## Whitelist Example

```
com.google.android.dialer
com.android.phone
com.whatsapp
```

## Privacy

All data stays on your device. No accounts, no cloud, no tracking.
