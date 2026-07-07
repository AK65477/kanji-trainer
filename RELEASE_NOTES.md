# Release Notes

## v0.1.1 — known-card retirement (versionCode 7)

Manual retirement for cards you already know perfectly, distributed as a `v0.1.1` APK.

### Changes

- **Mark a card as already mastered.** During the answer phase, tap `I know this perfectly` / `완벽히 알아요` / `完全に覚えています` and confirm once to mark that card mastered and keep it out of future sessions.
- The action deliberately does not write a review log entry, so today's review count and timed-review history are not inflated.
- No database schema changes — existing progress is preserved on update. The card is stored using the existing SRS state table with a far-future due date.

## v0.1.0 — updated build (versionCode 6)

Review drill, distributed as an updated `v0.1.0` APK.

### Changes

- **Review round after a session.** When you miss cards in a session, a short
  review round lets you re-drill just those cards, shuffled, until you get them
  right (capped at a few passes). It's reinforcement only — these answers never
  change your review schedule or mastery, which were already set during the
  session. Skippable.
- No database changes — existing progress is preserved on update.

## v0.1.0 — updated build (versionCode 5)

Audio, distributed as an updated `v0.1.0` APK.

### Changes

- **Listen to the example sentence.** After you answer, a speaker button next to
  the sentence reads it aloud with the device's Japanese text-to-speech voice, so
  you can check the pronunciation and pitch you couldn't get from reading alone.
  It appears only once the card is answered (so it can't reveal the reading early)
  and only when a Japanese voice is available.
- No database changes — existing progress is preserved on update.

## v0.1.0 — updated build (versionCode 3)

Progress backup + study-order refinements, distributed as an updated `v0.1.0` APK.

### Changes

- **Cross-device progress backup.** Home → "진도 백업 · 기기 간 이동" exports your
  progress (SRS schedule, mastery, review + session history) to a file you can
  move to another device and import back. Import replaces local progress after an
  explicit confirmation, and warns first if this device has newer study activity
  than the backup. Seed cards stay in the app; only progress is carried.
- **Same word no longer repeats back-to-back in a session.** The seed groups some
  cards for the same word at consecutive positions; sessions now spread them apart
  so a just-answered word isn't asked again immediately.
- Still no database changes — existing progress is preserved on update.

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
