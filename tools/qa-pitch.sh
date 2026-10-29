#!/usr/bin/env bash
set -euo pipefail
mkdir -p qa-output
trap 'adb logcat -d > qa-output/logcat.txt; adb pull /sdcard/Android/data/com.projectmister.game/files/qa qa-output/ >/dev/null 2>&1 || true' EXIT
adb install -r /tmp/boss-baseline/app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/test-manual/qa.apk
adb logcat -c
run_mode() {
    local mode="$1"
    timeout 900s adb shell am instrument -w -e mode "$mode" com.projectmister.game.test/com.projectmister.game.qa.SmokeRunner | tee "qa-output/$mode.txt"
    grep -q 'PASS' "qa-output/$mode.txt"
    if grep -Eq 'FAIL|INSTRUMENTATION_FAILED' "qa-output/$mode.txt"; then exit 1; fi
}
run_mode seed-feedback
grep -q 'PASS baseline career seeded' qa-output/seed-feedback.txt
adb install -r app/build/outputs/apk/debug/app-debug.apk
for mode in presentation refinements fixes premium natural-match; do run_mode "$mode"; done
grep -q 'PASS all five pitch table result tactics and goal-commentary requests verified' qa-output/presentation.txt
grep -q 'PASS all eight identity presentation stadium and match refinements verified' qa-output/refinements.txt
grep -q 'PASS all nine requested recruitment UI inbox possession and goal-reset fixes verified' qa-output/fixes.txt
grep -q 'PASS premium shell, all club screens, functional navigation, squad filters, lineup selection and live presentation verified' qa-output/premium.txt
grep -q 'PASS natural full match advances without stalls and preserves its result' qa-output/natural-match.txt
adb shell wm size 720x1280
adb shell wm density 320
run_mode compact
grep -q 'PASS compact landscape formations usable' qa-output/compact.txt
adb shell wm size reset
adb shell wm density reset
sha256sum app/build/outputs/apk/debug/app-debug.apk > qa-output/apk-sha256.txt
adb logcat -d > qa-output/logcat.txt
if grep -A 80 'FATAL EXCEPTION' qa-output/logcat.txt | grep -q 'com.projectmister.game'; then exit 1; fi
