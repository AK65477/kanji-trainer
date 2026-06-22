# Release Notes

## v0.1.0 — updated build (versionCode 2)

Scheduling fix, distributed as an updated `v0.1.0` APK.

### Changes

- **Due reviews now come before new cards in a session.** Previously every
  new card was stamped with the import time, so cards you had already studied
  were buried behind the entire unstudied backlog and rarely resurfaced. Now a
  card you just missed reappears on its due date, and new cards fill the
  remaining slots (capped per session) so the review load stays manageable.
- No database changes. Existing learners keep all of their progress after
  updating; the build is signed with the same key, so it installs as an
  in-place update over an earlier `v0.1.0` install.

## v0.1.0

Initial public MVP.

### Highlights

- Sentence-context kanji reading quizzes
- Card-level spaced repetition scheduling
- Response-time aware outcomes: Mastered, Good, Recalled, and Review again
- General kanji deck and name-kanji deck
- Kanji collection and detail screens
- Optional mnemonic notes
- Korean, English, and Japanese UI resources
- In-app Sources and Licenses screen
- Local-first app with no analytics, ads, or third-party tracking

### Install

Download `app-release.apk` from this release's assets and install it on Android.
The app is distributed through GitHub only, not Google Play.

Android may ask you to allow installing apps from your browser or file manager.
Only install APKs from this repository's Releases page.

### Verification

Release APK SHA-256 (versionCode 2):

```text
f2dd29c823c51faed6c4832cb5dc5e02df760e8c7da910e1de1ba1d12b5829d0
```

Signing certificate SHA-256:

```text
d75d00fa3af1515f8075f5e101e9861eb0cb7382381b53f463c71976c1d960ee
```

### Distribution

Attach only the release-signed APK to GitHub Releases:

```text
app/build/outputs/apk/release/app-release.apk
```

Do not attach debug APKs as official releases.
