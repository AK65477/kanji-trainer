<p align="center">
  <img src="kanji_app_icon.png" alt="Kanji Trainer app icon" width="120" height="120">
</p>

<h1 align="center">Kanji Trainer</h1>

<p align="center">
  <a href="LICENSE"><img alt="Code license: MIT" src="https://img.shields.io/badge/code-MIT-blue.svg"></a>
  <a href="NOTICE.md"><img alt="Study data: CC BY-SA 4.0" src="https://img.shields.io/badge/study%20data-CC%20BY--SA%204.0-lightgrey.svg"></a>
</p>

Kanji Trainer is a small Android app for learning kanji by reading them inside
real sentence context from day one. It focuses on fast recognition of kanji words
and expressions rather than isolated character memorization.

It is not trying to replace full kanji courses such as WaniKani or dictionary
apps such as Kanji Study. Its narrower goal is immediate reading feel: see a
highlighted kanji word or expression inside a sentence, choose its reading
quickly, and let the SRS bring it back when the memory is likely to fade.
> [!NOTE]
> The bundled cards, answer choices, and translations were generated and then
> automatically validated with structural checks. They are usable seed data, but
> some items may still feel awkward in real reading practice. In particular,
> there may be cases where a more natural reading appears as a distractor, or
> where multiple readings are technically possible but one is more natural in the
> sentence. These cases are being collected and corrected over time. Please report
> them with the **Card quality issue** template.

## Download

Install the latest release APK from GitHub Releases:

<https://github.com/AK65477/kanji-trainer/releases/latest>

Download `app-release.apk` from the release assets and install it on Android.
The app is distributed through GitHub only, not Google Play.

Android may ask you to allow installing apps from your browser or file manager.
Only install APKs from this repository's Releases page.

Android application ID:

```text
io.github.ak65477.kanjitrainer
```

## Install And Update With Obtainium

Kanji Trainer is a good fit for [Obtainium](https://github.com/ImranR98/Obtainium),
which can track GitHub Releases and notify you when a new APK is available.

Use this repository URL in Obtainium:

```text
https://github.com/AK65477/kanji-trainer
```

Release asset name:

```text
app-release.apk
```

## Verify The APK

For the current `v0.1.0` release (versionCode 4), the APK file hash is:

```text
3bb5eba9a7168defd14086ed7fe843e6fc91ac7a26f52cf45add03ddd740e82b
```

The signing certificate SHA-256 is:

```text
d75d00fa3af1515f8075f5e101e9861eb0cb7382381b53f463c71976c1d960ee
```

This repository contains the source code, release notes, privacy policy, and
license notices so users can inspect what the APK is built from.

## Recommended Learner Level

Kanji Trainer is best for learners who already know hiragana and katakana and
have at least beginner listening/reading intuition. It is designed for building
kanji reading speed in context, not for learning kana from zero.

## What Makes It Different

Kanji Trainer is built for the moment when you can almost read a sentence, but
one kanji word slows everything down.

- It drills readings inside short sentence contexts instead of isolated kanji.
- It values fast, stable recognition, not just getting an answer right once.
- It uses SRS review so remembered cards come back before they fade too far.
- It separates general kanji practice from name-only kanji practice.
- It includes kanji collection and detail screens for checking what is becoming familiar.
- It keeps labels gentle: Mastered, Good, Recalled, and Review again.

The goal is not only to know a kanji, but to recognize its reading quickly
enough that reading keeps flowing.

## Content Quality

The bundled cards are intended as practical, high-quality seed data for real use.
Structural checks cover target spans, answer/distractor duplication, kanji-card
links, and underlinked kanji.

That said, not every card should be treated as if it were individually polished
by a professional textbook editor. Some example sentences and distractors may
still improve over time through review, usage data, and user feedback.

## Current Scope

- Sentence-based kanji SRS
- General-use kanji deck
- Name-only kanji deck
- Kanji collection and detail screens
- Optional mnemonic notes
- Local Room database
- Bundled seed data
- Korean, English, and Japanese UI resources
- No analytics, ads, or third-party tracking

Conversation tracking and topic recommendation are intentionally not part of
this public Kanji Trainer app.

## Build From Source

Requirements:

- Android Studio or JDK 17
- Android SDK 35

From the repository root:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat :app:assembleDebug --no-daemon --console=plain
```

Debug APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Release APKs

Before publishing a release APK, configure a real release signing key. The debug
key is fine for local testing only.

Generate local signing files:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/generate-release-keystore.ps1
```

Then build a release APK:

```powershell
.\gradlew.bat :app:assembleRelease --no-daemon --console=plain
```

Attach only the release-signed APK to GitHub Releases:

```text
app/build/outputs/apk/release/app-release.apk
```

Release signing details are in `docs/RELEASE_SIGNING.md`.

## Privacy

Kanji Trainer is local-first and does not collect or transmit personal data. See `PRIVACY.md`.


## Attribution And License

The bundled study data is licensed separately from the code. See `NOTICE.md`.

Code: MIT License. See `LICENSE`.

Bundled study data and any EDRDG-derived or EDRDG-informed data: CC BY-SA 4.0.
See `NOTICE.md`.




