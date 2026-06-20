# Public Release Checklist

Use this before creating a GitHub release or attaching an APK.

## Repository Hygiene

- Confirm only Kanji Trainer source is included.
- Do not commit screenshots, UI dumps, local build output, keystores, or private notes.
- Do not commit `local.properties`.
- Do not commit personal notes or private learning logs.

## Licensing

- Keep `LICENSE` and `NOTICE.md` in the repository root.
- Keep the in-app Sources screen reachable from the home screen.
- If seed data starts importing exact examples from Tatoeba/Tanaka, update
  `NOTICE.md` and the in-app Sources screen.
- If seed data starts importing substantial JMdict glosses or KANJIDIC2 fields,
  keep the data under CC BY-SA 4.0 and preserve attribution.

## APK Signing

- Do not publish a debug-signed APK as an official release.
- Run `scripts/generate-release-keystore.ps1` once on the release machine.
- Confirm `keystore.properties` exists locally and is not tracked by Git.
- Back up the generated keystore and `keystore.properties` privately.
- Build official GitHub APKs with `:app:assembleRelease`.

## Validation

Run:

```powershell
.\gradlew.bat :app:assembleDebug --no-daemon --console=plain
```

Recommended before release:

```powershell
.\gradlew.bat :core:srs:test :app:assembleDebug --no-daemon --console=plain
```

Release build:

```powershell
.\gradlew.bat :app:assembleRelease --no-daemon --console=plain
```

Manual smoke test:

- Open Kanji Trainer.
- Start a general kanji session.
- Start a name-only kanji session.
- Open kanji collection.
- Open one kanji detail.
- Open Sources and confirm attribution text is visible.
- Confirm PRIVACY.md and RELEASE_NOTES.md are up to date.




