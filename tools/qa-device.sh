#!/usr/bin/env bash
set -euo pipefail
mkdir -p qa-output
trap 'adb logcat -d > qa-output/logcat.txt; adb pull /sdcard/Android/data/com.projectmister.game/files/qa qa-output/ >/dev/null 2>&1 || true' EXIT
adb install -r /tmp/boss-baseline/app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/test-manual/qa.apk
adb logcat -c
adb shell am instrument -w -e mode seed com.projectmister.game.test/com.projectmister.game.qa.SmokeRunner | tee qa-output/baseline.txt
grep -q 'PASS baseline career seeded' qa-output/baseline.txt
! grep -q 'FAIL\|INSTRUMENTATION_FAILED' qa-output/baseline.txt
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am instrument -w -e mode full com.projectmister.game.test/com.projectmister.game.qa.SmokeRunner | tee qa-output/candidate.txt
grep -q 'PASS completed match survives reload' qa-output/candidate.txt
! grep -q 'FAIL\|INSTRUMENTATION_FAILED' qa-output/candidate.txt
adb shell am force-stop com.projectmister.game
adb shell am start -W -n com.projectmister.game/.MainActivity
adb shell pidof com.projectmister.game | grep -Eq '[0-9]+'
adb logcat -d > qa-output/logcat.txt
if grep -A 80 'FATAL EXCEPTION' qa-output/logcat.txt | grep -q 'com.projectmister.game'; then exit 1; fi
