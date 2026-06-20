# Release Signing

GitHub release APKs must be signed with a private release key. Do not publish a
debug-signed APK as an official release.

## Generate Local Signing Files

From the repository root:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/generate-release-keystore.ps1
```

This creates:

- `C:\Users\<you>\AndroidKeys\kanji-trainer-release.jks`
- `keystore.properties`

Both files are private. They are ignored by Git.

## Build a Release APK

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat :app:assembleRelease --no-daemon --console=plain
```

Output:

```text
app/build/outputs/apk/release/app-release.apk
```

## Keep This Key

Android updates require the same application ID and the same signing key. Back
up the keystore and `keystore.properties` somewhere private. If the key is lost,
existing users cannot update to newly signed APKs over the old install.

## Android Developer Verification

For future Android sideload restrictions, keep the application ID stable and
register that package name in the Android Developer Console when Google requires
verification for your distribution path. The proof step uses an APK signed with
this private key, so the release key should be created before the first public
GitHub APK.

Current application ID:

```text
io.github.ak65477.kanjitrainer
```
