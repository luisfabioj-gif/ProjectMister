#!/usr/bin/env bash
set -euo pipefail
mkdir -p qa-output
trap 'adb logcat -d > qa-output/logcat.txt; adb pull /sdcard/Android/data/com.projectmister.game/files/qa qa-output/ >/dev/null 2>&1 || true' EXIT
adb install -r /tmp/boss-baseline/app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/test-manual/qa.apk
adb logcat -c
timeout 180s adb shell am instrument -w -e mode seed-feedback com.projectmister.game.test/com.projectmister.game.qa.SmokeRunner | tee qa-output/baseline.txt
grep -q 'PASS baseline career seeded' qa-output/baseline.txt
if grep -Eq 'FAIL|INSTRUMENTATION_FAILED' qa-output/baseline.txt; then exit 1; fi
adb install -r app/build/outputs/apk/debug/app-debug.apk
for mode in refinements fixes premium full; do
    timeout 720s adb shell am instrument -w -e mode "$mode" com.projectmister.game.test/com.projectmister.game.qa.SmokeRunner | tee "qa-output/$mode.txt"
    grep -q 'PASS' "qa-output/$mode.txt"
    if grep -Eq 'FAIL|INSTRUMENTATION_FAILED' "qa-output/$mode.txt"; then exit 1; fi
done
grep -q 'PASS all eight identity presentation stadium and match refinements verified' qa-output/refinements.txt
grep -q 'PASS all nine requested recruitment UI inbox possession and goal-reset fixes verified' qa-output/fixes.txt
grep -q 'PASS premium shell, all club screens, functional navigation, squad filters, lineup selection and live presentation verified' qa-output/premium.txt
grep -q 'PASS completed match survives reload' qa-output/full.txt
adb shell wm size 720x1280
adb shell wm density 320
timeout 180s adb shell am instrument -w -e mode compact com.projectmister.game.test/com.projectmister.game.qa.SmokeRunner | tee qa-output/compact.txt
grep -q 'PASS compact landscape formations usable' qa-output/compact.txt
if grep -Eq 'FAIL|INSTRUMENTATION_FAILED' qa-output/compact.txt; then exit 1; fi
adb shell wm size reset
adb shell wm density reset
sha256sum app/build/outputs/apk/debug/app-debug.apk > qa-output/apk-sha256.txt
adb logcat -d > qa-output/logcat.txt
if grep -A 80 'FATAL EXCEPTION' qa-output/logcat.txt | grep -q 'com.projectmister.game'; then exit 1; fi
