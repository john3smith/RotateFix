# RotateFix 1.0.2 verification

Checked apps now appear before unchecked apps. A stable selection sort preserves
the existing locale-aware app-name order inside both groups. Selection changes,
initial loading, resume and search all rebuild the visible ordering.

Validated on 2026-10-21:

- `testDebugUnitTest lintDebug assembleDebug` succeeded: 5 unit tests passed,
  no lint errors, 4 existing warnings.
- Regular Android Emulator, API 35: installed with `adb install -r` over 1.0.0;
  package ID, signing certificate, app UID and selected app preference retained.
- Existing checked Chrome appeared first after updating.
- Checking Camera placed Camera and Chrome above unchecked BabyCam LAN, with
  Camera before Chrome. Unchecking Chrome returned it to the unchecked group.
- Search `ca` kept selected Camera first among matching apps. Search `ch`
  excluded selected Camera because it did not match; unchecked matches remained.
- Clear all restored name ordering. Selecting Chrome by tapping its row and
  restarting retained the selection and placed Chrome first. Original selection
  was restored after verification.
- No new libraries, native binaries, images or fonts. Runtime continues to use
  Android platform APIs only; JUnit remains host-test-only. Existing notices are
  included. APK and staged-source checks found no signing keys, credentials,
  runtime preferences or user data. No network permission was added.

This is a debug-signed update APK, not a completed app-store submission. The
changed list workflow was verified on the emulator; physical-device orientation
and manufacturer-specific behavior were not retested by this UI-only change.
