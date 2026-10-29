# Play preparation tools

These tools prepare local artifacts. They do not upload to Google Play, YouTube or GitHub.

## 1.5.0 update

Use the already verified `artifacts/1.5.0/` AAB, APK and mapping. Generate a universal APK from that AAB with bundletool and install it, plus the matching upload-signed releaseChecks APK, **only on a disposable emulator**. Set the capture resolution to 1080×1920 / 360dpi, enable the Japanese 12-key keyboard and System UI demo mode, then run:

```sh
adb -s emulator-5556 shell am instrument -w -r \
  -e class io.github.hatake716.rocketboy.ReleaseStoreCaptureTest#capture150StoreUpdate \
  io.github.hatake716.rocketboy.releasechecks/androidx.test.runner.AndroidJUnitRunner
```

Require `OK (1 test)`, then pull the app's external `files/play-store` directory to `artifacts/play-1.5.0/verification/capture/`. This scenario seeds saved rank scores and meteor positions, and checks real input/scoring, collection, competitive results, music controls and top10. The manifest discloses the demo scores. No helper code is shipped in the app.

Review all eight captures, update the current store copy and validation documents, then run `python3 tools/play/package_update.py` in a Pillow environment. It verifies existing artifacts/captures, converts screenshots to RGB without changing pixel colors, creates a contact sheet, and packages an explicit allowlist. It also checks that the prior 1.4.1 ZIP and quote review CSV remain unchanged. It does not build or install the app. Physical update evidence, when available, belongs in `artifacts/play-1.5.0/verification/physical-update.json` and the validation documents.

The scripts `prepare_materials.py`, `render_graphics.py` and `package_release.py` below are **historical 1.4.1 tools**. Do not run them to prepare 1.5.0: they overwrite current copy and quote review files with first-release templates. The existing icon and feature graphic are reused unchanged in 1.5.0.

## Historical 1.4.1 preparation

1. Configure the ignored root `keystore.properties` using the example. Keep the upload key outside the repository. `app` and the separate `releaseChecks` harness must use the same signing certificate.
2. Run `./gradlew :app:testDebugUnitTest :app:lintRelease :app:assembleRelease :app:bundleRelease :app:assembleDebug :app:assembleDebugAndroidTest :releaseChecks:assembleDebug`.
3. Verify the APK with `apksigner verify --verbose --print-certs`, the AAB with `jarsigner -verify`, and the AAB structure with the official `bundletool validate`. Use `bundletool build-apks` with the configured upload key and password **files** to test an APK generated from the AAB. Never paste passwords into command arguments or logs.
4. Use an isolated emulator. For this capture, the Android emulator was launched from `heisei-camera-api35` using `-read-only -no-snapshot -no-window -no-audio -port 5556`. The disposable instance does not save back changes; the ordinary emulator on 5554 and physical devices were not modified.
5. Set only the disposable emulator to 1080×1920 and 360 dpi. Enable the Japanese 12-key Gboard layout, complete its initial setup and use System UI demo mode to hide notifications. These are capture settings, not an app requirement.
6. Install the signed release APK and `tools/play/release-checks/build/outputs/apk/debug/releaseChecks-debug.apk` on the disposable emulator. Run:

   ```sh
   adb -s emulator-5556 shell am instrument -w -r \
     -e class io.github.hatake716.rocketboy.ReleaseStoreCaptureTest#releaseAcceptanceAndScreenshots \
     io.github.hatake716.rocketboy.releasechecks/androidx.test.runner.AndroidJUnitRunner
   ```

   The dedicated harness packages its own Kotlin and AndroidX test runtime; a re-signed ordinary debug test APK is unsuitable because release R8 can rename or remove the runtime classes it assumes are present. No test or capture helper is added to the shipping app. This test clears **only the disposable emulator app's preferences**, seeds deterministic saved missions, enters Japanese through the visible input field and uses the actual fire button. It verifies the release version, `debuggable=false`, scores, collection, competitive game over, BGM state, privacy navigation and top10. All scores shown are computed by the shipping engine. Capture fixtures are disclosed in the screenshot manifest.
7. Pull `/sdcard/Android/data/io.github.hatake716.rocketboy/files/play-store`. Inspect the eight captures and privacy/top10 verification images. They must be actual app screens; do not invent scores or overlay a fake keyboard.
8. Run `python3 tools/play/prepare_materials.py`, then `python3 tools/play/render_graphics.py` and `python3 tools/play/package_release.py` in a Pillow-enabled Python environment. The graphics use the game's vector path and pixel alphabet; they do not import third-party artwork.
9. Verify ZIP entries exclude keys/passwords/debug APKs and that all prepared files have checksums. Shut down only the disposable emulator after capture. Review the open publication items in `docs/PLAY_CONTENT_REVIEW.md`.

For NixOS on the preparation host, Gradle used:

```
-Pandroid.aapt2FromMavenOverride=/nix/store/9m25nd8pznvbbywjmpg6z4kb3ap2inc4-android-sdk-build-tools-36.0.0/libexec/android-sdk/build-tools/36.0.0/aapt2
```

`ANDROID_HOME` and `ANDROID_SDK_ROOT` were `/home/takeshi/Android/Sdk`, and `ANDROID_AVD_HOME` was `/home/takeshi/.config/.android/avd`. These host-specific paths are not required on a standard Android SDK installation.
