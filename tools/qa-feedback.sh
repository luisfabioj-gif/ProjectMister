#!/usr/bin/env bash
set -euo pipefail
mkdir -p qa-output
trap 'adb logcat -d > qa-output/logcat.txt; adb pull /sdcard/Android/data/com.projectmister.game/files/qa qa-output/ >/dev/null 2>&1 || true' EXIT
adb install -r /tmp/boss-baseline/app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/test-manual/qa.apk
adb logcat -c
timeout 180s adb shell am instrument -w -e mode seed com.projectmister.game.test/com.projectmister.game.qa.SmokeRunner | tee qa-output/baseline.txt
grep -q 'PASS baseline career seeded' qa-output/baseline.txt
! grep -q 'FAIL\|INSTRUMENTATION_FAILED' qa-output/baseline.txt
adb install -r app/build/outputs/apk/debug/app-debug.apk
for mode in fixes premium full; do
    timeout 720s adb shell am instrument -w -e mode "$mode" com.projectmister.game.test/com.projectmister.game.qa.SmokeRunner | tee "qa-output/$mode.txt"
    grep -q 'PASS' "qa-output/$mode.txt"
    ! grep -q 'FAIL\|INSTRUMENTATION_FAILED' "qa-output/$mode.txt"
done
grep -q 'PASS all nine requested recruitment UI inbox possession and goal-reset fixes verified' qa-output/fixes.txt
grep -q 'PASS completed match survives reload' qa-output/full.txt
adb shell wm size 720x1280
adb shell wm density 320
timeout 180s adb shell am instrument -w -e mode compact com.projectmister.game.test/com.projectmister.game.qa.SmokeRunner | tee qa-output/compact.txt
grep -q 'PASS compact landscape formations usable' qa-output/compact.txt
! grep -q 'FAIL\|INSTRUMENTATION_FAILED' qa-output/compact.txt
adb shell wm size reset
adb shell wm density reset
sha256sum app/build/outputs/apk/debug/app-debug.apk > qa-output/apk-sha256.txt
adb logcat -d > qa-output/logcat.txt
if grep -A 80 'FATAL EXCEPTION' qa-output/logcat.txt | grep -q 'com.projectmister.game'; then exit 1; fi
