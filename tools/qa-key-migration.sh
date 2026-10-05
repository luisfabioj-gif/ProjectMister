#!/usr/bin/env bash
set -euo pipefail
# Disposable emulator-only identity. Never a production upload key; never uploaded.
bundletool=app/build/toolchain/bundletool.jar
bundle=app/build/outputs/bundle/release/app-release.aab
work=app/build/bundle-qa
key="$work/migration-test.jks"
password="$work/migration-password.txt"
trap 'rm -f "$key" "$password"' EXIT
run_mode() {
  timeout 180s adb shell am instrument -w -e mode "$1" com.projectmister.game.test/com.projectmister.game.qa.SmokeRunner | tee "qa-output/$1.txt"
  ! grep -q 'FAIL\|INSTRUMENTATION_FAILED' "qa-output/$1.txt"
}
run_mode migration-export
grep -q 'PASS migration backup exported before uninstall' qa-output/migration-export.txt
adb pull /sdcard/Android/data/com.projectmister.game/files/migration.bossxi "$work/migration.bossxi"
export BOSS_MIGRATION_TEST_PASSWORD
BOSS_MIGRATION_TEST_PASSWORD=$(openssl rand -hex 24)
printf '%s' "$BOSS_MIGRATION_TEST_PASSWORD" > "$password"
keytool -genkeypair -keystore "$key" -alias migration-test -storepass:env BOSS_MIGRATION_TEST_PASSWORD -keypass:env BOSS_MIGRATION_TEST_PASSWORD -keyalg RSA -keysize 2048 -validity 2 -dname 'CN=BOSS XI Disposable Migration Test' -noprompt
java -jar "$bundletool" build-apks --bundle="$bundle" --output="$work/migration-test.apks" --device-spec="$work/device.json" --overwrite --ks="$key" --ks-key-alias=migration-test --ks-pass="file:$password" --key-pass="file:$password"
"$ANDROID_HOME/build-tools/35.0.0/apksigner" sign --ks "$key" --ks-key-alias migration-test --ks-pass env:BOSS_MIGRATION_TEST_PASSWORD --key-pass env:BOSS_MIGRATION_TEST_PASSWORD --out "$work/migration-qa.apk" app/build/test-manual/qa.apk
# Verify it is actually a different certificate before uninstalling synthetic QA data.
keytool -exportcert -keystore "$key" -alias migration-test -storepass:env BOSS_MIGRATION_TEST_PASSWORD -file "$work/migration-cert.der"
keytool -exportcert -keystore project-mister-dev.keystore -alias projectmister -storepass projectmisterdev -file "$work/development-cert.der"
! cmp -s "$work/migration-cert.der" "$work/development-cert.der"
# Uninstall removes external app files too. Archive the pre-migration evidence
# separately so the final capture cannot overwrite startup/report files.
mkdir -p qa-output/before-key-migration
adb pull /sdcard/Android/data/com.projectmister.game/files/qa qa-output/before-key-migration/
test -s qa-output/before-key-migration/qa/fr-promotion-review.png
test -s qa-output/before-key-migration/qa/eng-promotion-review.png
test -s qa-output/before-key-migration/qa/be-promotion-review.png
test -s qa-output/before-key-migration/qa/es-promotion-review.png
test -s qa-output/before-key-migration/qa/pt-promotion-review.png
adb uninstall com.projectmister.game
adb uninstall com.projectmister.game.test
java -jar "$bundletool" install-apks --apks="$work/migration-test.apks"
adb install "$work/migration-qa.apk"
run_mode migration-prepare
grep -q 'PASS different-key installation starts with empty storage' qa-output/migration-prepare.txt
adb push "$work/migration.bossxi" /sdcard/Android/data/com.projectmister.game/files/migration.bossxi
run_mode migration-restore
grep -q 'PASS different signing key uninstall-install career migration verified' qa-output/migration-restore.txt
unset BOSS_MIGRATION_TEST_PASSWORD
