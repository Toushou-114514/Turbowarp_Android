# Turbowarp Android

> The full TurboWarp editor, bundled into your phone. Works offline.
>
> [Download](https://github.com/Toushou-114514/Turbowarp_Android/releases/latest)
>
> ![Java](https://img.shields.io/badge/Java-71%25-brightgreen?style=flat-square&logo=openjdk)
![JavaScript](https://img.shields.io/badge/JavaScript-19%25-orange?style=flat-square&logo=javascript)
![XML](https://img.shields.io/badge/XML-7%25-blue?style=flat-square)
![Other](https://img.shields.io/badge/Other-3%25-lightgrey?style=flat-square)

[繁體中文](README.md) | [简体中文](README.zh.md) | **English**

---

| | |
| --- | --- |
| Version | 1.0 |
| Package | `app.turbowarp.android` |
| Requires | Android 7.0 (API 24) or newer |
| Size | ~6.5 MB |
| Languages | Simplified Chinese / English (system default supported) |

## Contents

- [What is this](#what-is-this)
- [Features](#features)
- [Home screen](#home-screen)
- [Editor](#editor)
- [Settings](#settings)
- [Import & export](#import--export)
- [Install](#install)
- [FAQ](#faq)
- [Known limitations](#known-limitations)
- [For developers](#for-developers)
- [Credits & licence](#credits--licence)

## What is this

Turbowarp Android is an Android app that ships the [TurboWarp](https://turbowarp.org) editor **inside the APK**.

Open it and start coding — there is no editor to download. Projects live on your phone and can be exported as `.sb3` whenever you like.

## Features

| Feature | Details |
| --- | --- |
| Offline editor | Editor, block icons and the addons page are all bundled |
| Project list | Cards with thumbnail, title and last-modified date |
| Save | One button in the editor's top-right corner, stored on the phone |
| Export | Writes the `.sb3` to the system **Downloads** folder |
| Share | Sends the `.sb3` to WeChat, QQ and other apps |
| Import | Local `.sb3` files, or a Scratch project link / ID |
| Rename | Rename from the home screen; renaming in the editor syncs back |
| Theme | 8 preset accent colours plus a custom colour picker |
| Dark mode | System / Light / Dark, applied to the home screen and the editor |
| High contrast | Stronger colours and borders for easier reading |
| Two languages | Simplified Chinese and English, for the UI and the editor alike |

## Home screen

Two buttons in the top-right corner:

- **Import** — opens the import dialog
- **Settings** — opens the settings screen

In the middle is the project grid: each card shows a thumbnail, the title and the last-modified date. The `⋮` menu on a card offers:

| Item | Action |
| --- | --- |
| Export project | Save the `.sb3` to the system **Downloads** folder |
| Rename | Change the project title |
| Share | Share the `.sb3` |
| Delete | Delete the project (asks for confirmation) |

The `+` button at the bottom-right creates a new project and opens the editor.

## Editor

The editor is TurboWarp itself, with a few phone-specific tweaks:

- **No extra red title bar** — every pixel belongs to the editor
- **The app's own Save button sits in the top-right corner** of the menu bar, replacing TurboWarp's "Save to computer"
- Entries that do not apply are removed: `Save to computer`, `Save as…`, `Package project`, `See Project Page`, `TurboWarp Feedback`
- **The project title field** (middle of the menu bar) shows the current name; renaming there updates the card on the home screen
- **The addons page is bundled**, so tapping *Addons* works; the back button returns to the editor instead of closing it

## Settings

| Setting | Options |
| --- | --- |
| Theme colour | 8 presets, or tap `+` for a custom R/G/B picker |
| Dark mode | System / Light / Dark |
| High contrast | On / Off |
| Language | System / Chinese (Simplified only) / English |
| About | Version, author, GitHub repository |

Theme colour, dark mode, high contrast and language apply to **both the home screen and the editor**.

## Import & export

**Import** (the ⤓ button) offers two choices:

1. **From this device** — pick a `.sb3`; it opens right away and takes the file name as its title
2. **From Scratch** — paste a project link or ID (e.g. `https://scratch.mit.edu/projects/104/` or `104`)
   - Project data is fetched through TurboWarp's relay API
   - The returned project JSON is packed into an `.sb3` for you
   - **Note:** costumes and sounds are still streamed from Scratch's CDN, so the first open needs a network connection

**Save** — the button in the editor's top-right corner writes the project to the app's private storage; the home screen updates the thumbnail and date immediately.

**Export** — card menu → *Export project*; the file lands in **Downloads** with the project title as its name.

**Share** — card menu → *Share*; pick WeChat / QQ to send the `.sb3`.

## Install

1. Open the [releases page](https://github.com/Toushou-114514/Turbowarp_Android/releases/latest)
2. Download `turbowarp_android.apk`
3. Tap it to install
4. Open the app

Upgrading over an existing install keeps all your projects.

## FAQ

**The editor is slow to open, or the screen stays white.**
The first launch unpacks the bundled assets. If it never loads, update *Android System WebView* in the system settings.

**Sprites from an imported Scratch project are blank.**
Images and sounds are downloaded from Scratch's CDN. The project itself (blocks, variables) is already stored locally and will render once you are online.

**Export fails.**
Android 9 and older need storage permission — grant it in system settings.

**WeChat / QQ is missing from the share sheet.**
Send one file from WeChat first; the system only registers it as a file handler after that.

**The app UI and the editor use different languages.**
Set the language explicitly to *Chinese* or *English* instead of *System*.

## Known limitations

- Costume/sound libraries, extensions and Scratch-imported assets need a network connection
- No cloud sync, accounts or online publishing
- Projects live in the app's private storage — uninstalling removes them, so export backups
- No tablet-specific layout (the grid simply fits more columns on large screens)

## For developers

The project **does not use Gradle**. It is packaged directly with the SDK toolchain — aapt2 → javac → d8 → zipalign → apksigner — with no AndroidX and plain framework APIs only.

**Requirements**

- JDK 8+
- Android SDK Build-Tools 34.0.0
- Android SDK Platform 34 (`android.jar`)

## Credits & licence

- Editor: [TurboWarp](https://turbowarp.org), GPL-3.0
- Blocks and runtime: [Scratch](https://scratch.mit.edu), scratch-gui, scratch-vm
- This is an unofficial project, not affiliated with the Scratch Foundation or the TurboWarp team

## Misc

- Toushou — https://github.com/Toushou-114514
- Repository: https://github.com/Toushou-114514/Turbowarp_Android
- Special thanks to [DeepSeek](https://deepseek.com)
- This tool is safe and virus-free. If it ever isn't, DeepSeek wrote it.
- If you find it useful, please give the repository a star ⭐

