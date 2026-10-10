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
timeout 480s adb shell am instrument -w -e mode full com.projectmister.game.test/com.projectmister.game.qa.SmokeRunner | tee qa-output/candidate.txt
grep -q 'PASS worldwide transfers free agents reload budget and lineup verified' qa-output/candidate.txt
grep -q 'PASS calendar recorded results and shootout orientation verified' qa-output/candidate.txt
grep -q 'PASS completed match survives reload' qa-output/candidate.txt
! grep -q 'FAIL\|INSTRUMENTATION_FAILED' qa-output/candidate.txt
timeout 300s adb shell am instrument -w -e mode national-cups com.projectmister.game.test/com.projectmister.game.qa.SmokeRunner | tee qa-output/national-cups.txt
grep -q 'PASS German recurring cup live matches two seasons reloads history and backup rejection verified' qa-output/national-cups.txt
! grep -q 'FAIL\|INSTRUMENTATION_FAILED' qa-output/national-cups.txt
for country in ENG ES IT FR NL BE SCO TR; do
    timeout 360s adb shell am instrument -w -e mode expanded-cups -e country "$country" com.projectmister.game.test/com.projectmister.game.qa.SmokeRunner | tee "qa-output/cups-$country.txt"
    grep -q "PASS $country recurring cups watched and quick matches two seasons reloads history and backup rejection verified" "qa-output/cups-$country.txt"
    ! grep -q 'FAIL\|INSTRUMENTATION_FAILED' "qa-output/cups-$country.txt"
done
timeout 480s adb shell am instrument -w -e mode divisions com.projectmister.game.test/com.projectmister.game.qa.SmokeRunner | tee qa-output/divisions.txt
grep -q 'PASS all twenty standalone and twenty linked division careers verified' qa-output/divisions.txt
grep -q 'PASS FR watched playoffs and season transition verified' qa-output/divisions.txt
grep -q 'PASS NL watched playoffs and season transition verified' qa-output/divisions.txt
grep -q 'PASS IT watched playoffs and season transition verified' qa-output/divisions.txt
grep -q 'PASS ENG watched playoffs and season transition verified' qa-output/divisions.txt
grep -q 'PASS career backup export, validation, cancellation, restore and load verified' qa-output/divisions.txt
! grep -q 'FAIL\|INSTRUMENTATION_FAILED' qa-output/divisions.txt
for country in PT DE ENG ES IT FR NL BE SCO TR; do
    timeout 600s adb shell am instrument -w -e mode europe -e country "$country" com.projectmister.game.test/com.projectmister.game.qa.SmokeRunner | tee "qa-output/europe-$country.txt"
    grep -q "PASS $country combined UEFA qualifying domestic calendars two seasons watched quick matches exact archives and backups verified" "qa-output/europe-$country.txt"
    ! grep -q 'FAIL\|INSTRUMENTATION_FAILED' "qa-output/europe-$country.txt"
done
adb shell wm size 720x1280
adb shell wm density 320
timeout 120s adb shell am instrument -w -e mode compact com.projectmister.game.test/com.projectmister.game.qa.SmokeRunner | tee qa-output/compact.txt
grep -q 'PASS compact landscape formations usable' qa-output/compact.txt
! grep -q 'FAIL\|INSTRUMENTATION_FAILED' qa-output/compact.txt
adb shell wm size reset
adb shell wm density reset
bash tools/qa-release-bundle.sh
bash tools/qa-key-migration.sh
adb shell am force-stop com.projectmister.game
adb shell am start -W -n com.projectmister.game/.MainActivity
adb shell pidof com.projectmister.game | grep -Eq '[0-9]+'
adb logcat -d > qa-output/logcat.txt
if grep -A 80 'FATAL EXCEPTION' qa-output/logcat.txt | grep -q 'com.projectmister.game'; then exit 1; fi
