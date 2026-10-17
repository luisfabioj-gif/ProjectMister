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
if [[ "${BOSS_QA_CONTINUATION:-false}" == true ]]; then
    # The production APK must match a rebuilt reference byte for byte. Twenty-five
    # earlier modes are independently checked, including every UEFA/cup career.
    # Repeat live/upgrade setup, then rerun all forty division careers and every
    # downstream backup, compact, release-install and key-migration assertion.
    python3 tools/verified-qa-prefix.py --check-local
    timeout 600s adb shell am instrument -w -e mode full com.projectmister.game.test/com.projectmister.game.qa.SmokeRunner | tee qa-output/candidate.txt
    grep -q 'PASS worldwide transfers free agents reload budget and lineup verified' qa-output/candidate.txt
    grep -q 'PASS calendar recorded results and shootout orientation verified' qa-output/candidate.txt
    grep -q 'PASS completed match survives reload' qa-output/candidate.txt
    ! grep -q 'FAIL\|INSTRUMENTATION_FAILED' qa-output/candidate.txt
else
    timeout 600s adb shell am instrument -w -e mode feedback com.projectmister.game.test/com.projectmister.game.qa.SmokeRunner | tee qa-output/feedback.txt
    grep -q 'PASS all eleven feedback areas, exact identities, timed scouting, ordinal tables and sixty-four stadium upgrades verified' qa-output/feedback.txt
    ! grep -q 'FAIL\|INSTRUMENTATION_FAILED' qa-output/feedback.txt
    # Exercise the previously failing annual draw first. It restores the preserved
    # baseline career on success, so the upgrade and full gameplay gates still run.
    timeout 600s adb shell am instrument -w -e mode europe -e country ENG com.projectmister.game.test/com.projectmister.game.qa.SmokeRunner | tee qa-output/europe-ENG.txt
    grep -q 'PASS ENG combined UEFA qualifying domestic calendars two seasons watched quick matches exact archives and backups verified' qa-output/europe-ENG.txt
    ! grep -q 'FAIL\|INSTRUMENTATION_FAILED' qa-output/europe-ENG.txt
    # Expanded Portuguese career scenarios precede the natural live match. Allow
    # their total runtime; the runner retains its separate 75-second progress bounds.
    timeout 600s adb shell am instrument -w -e mode full com.projectmister.game.test/com.projectmister.game.qa.SmokeRunner | tee qa-output/candidate.txt
    grep -q 'PASS worldwide transfers free agents reload budget and lineup verified' qa-output/candidate.txt
    grep -q 'PASS calendar recorded results and shootout orientation verified' qa-output/candidate.txt
    grep -q 'PASS completed match survives reload' qa-output/candidate.txt
    ! grep -q 'FAIL\|INSTRUMENTATION_FAILED' qa-output/candidate.txt
    timeout 480s adb shell am instrument -w -e mode rules com.projectmister.game.test/com.projectmister.game.qa.SmokeRunner | tee qa-output/rules.txt
    grep -q 'PASS twenty registration cases, calendar-week accounting and ten isolated final league decisions verified' qa-output/rules.txt
    ! grep -q 'FAIL\|INSTRUMENTATION_FAILED' qa-output/rules.txt
    timeout 600s adb shell am instrument -w -e mode uefa-players com.projectmister.game.test/com.projectmister.game.qa.SmokeRunner | tee qa-output/uefa-players.txt
    grep -q 'PASS UEFA actual A/B lists, late free-agent exclusion, saved goalkeeper identities, registered ban service, three winter additions and atomic backups verified' qa-output/uefa-players.txt
    ! grep -q 'FAIL\|INSTRUMENTATION_FAILED' qa-output/uefa-players.txt
    timeout 600s adb shell am instrument -w -e mode cup-players com.projectmister.game.test/com.projectmister.game.qa.SmokeRunner | tee qa-output/cup-players.txt
    grep -q 'PASS actual cup dismissal, eligible lineup, ban service, transfer persistence and atomic backup rejection verified' qa-output/cup-players.txt
    grep -q 'PASS Scottish Cup transfer restriction blocks an actual ineligible lineup and preserves playable progression and exact saves' qa-output/cup-players.txt
    grep -q 'PASS French actual cup match-sheet continuity, free-agent registrations, unused bench distinction and exact saves verified' qa-output/cup-players.txt
    ! grep -q 'FAIL\|INSTRUMENTATION_FAILED' qa-output/cup-players.txt
    for country in ES PT DE IT FR NL BE SCO TR; do
        timeout 600s adb shell am instrument -w -e mode europe -e country "$country" com.projectmister.game.test/com.projectmister.game.qa.SmokeRunner | tee "qa-output/europe-$country.txt"
        grep -q "PASS $country combined UEFA qualifying domestic calendars two seasons watched quick matches exact archives and backups verified" "qa-output/europe-$country.txt"
        ! grep -q 'FAIL\|INSTRUMENTATION_FAILED' "qa-output/europe-$country.txt"
    done
    timeout 300s adb shell am instrument -w -e mode national-cups com.projectmister.game.test/com.projectmister.game.qa.SmokeRunner | tee qa-output/national-cups.txt
    grep -q 'PASS German recurring cup live matches two seasons reloads history and backup rejection verified' qa-output/national-cups.txt
    ! grep -q 'FAIL\|INSTRUMENTATION_FAILED' qa-output/national-cups.txt
    for country in ENG ES IT FR NL BE SCO TR; do
        timeout 360s adb shell am instrument -w -e mode expanded-cups -e country "$country" com.projectmister.game.test/com.projectmister.game.qa.SmokeRunner | tee "qa-output/cups-$country.txt"
        grep -q "PASS $country recurring cups watched and quick matches two seasons reloads history and backup rejection verified" "qa-output/cups-$country.txt"
        ! grep -q 'FAIL\|INSTRUMENTATION_FAILED' "qa-output/cups-$country.txt"
    done
fi
timeout 900s adb shell am instrument -w -e mode divisions com.projectmister.game.test/com.projectmister.game.qa.SmokeRunner | tee qa-output/divisions.txt
grep -q 'PASS all twenty standalone and twenty linked division careers verified' qa-output/divisions.txt
grep -q 'PASS FR watched playoffs and season transition verified' qa-output/divisions.txt
grep -q 'PASS NL watched playoffs and season transition verified' qa-output/divisions.txt
grep -q 'PASS IT watched playoffs and season transition verified' qa-output/divisions.txt
grep -q 'PASS ENG watched playoffs and season transition verified' qa-output/divisions.txt
grep -q 'PASS career backup export, validation, cancellation, restore and load verified' qa-output/divisions.txt
! grep -q 'FAIL\|INSTRUMENTATION_FAILED' qa-output/divisions.txt

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
