#!/usr/bin/env bash
set -euo pipefail
# Exercise the owner-side finalizer with a throwaway identity and non-production
# metadata. Neither this keystore nor the signed fixture is an artifact.
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
export BOSS_QA_UPLOAD_PASSWORD
BOSS_QA_UPLOAD_PASSWORD=$(openssl rand -hex 24)
keytool -genkeypair -storetype PKCS12 -keyalg RSA -keysize 2048 -validity 2 -dname 'CN=BOSS XI Disposable Bundle QA' \
  -keystore "$work/qa.p12" -alias qa -storepass:env BOSS_QA_UPLOAD_PASSWORD -noprompt
keytool -exportcert -rfc -keystore "$work/qa.p12" -alias qa -storepass:env BOSS_QA_UPLOAD_PASSWORD -file "$work/qa.pem"
play_key=$(openssl x509 -in "$work/qa.pem" -pubkey -noout | openssl pkey -pubin -outform DER | base64 -w0)
printf 'billing_public_key=%s\nprivacy_url=https://example.invalid/privacy\nsupport_email=support@example.invalid\n' "$play_key" > "$work/public.properties"
bundle=app/build/outputs/bundle/release/app-release.aab
tool=app/build/toolchain/bundletool.jar
printf '%s\n' "$BOSS_QA_UPLOAD_PASSWORD" | java tools/PreparePlayUpload.java "$bundle" "$work/configured.aab" "$work/qa.p12" qa "$work/public.properties"
java -jar "$tool" validate --bundle="$work/configured.aab"
python3 - "$bundle" "$work/configured.aab" <<'PY'
from zipfile import ZipFile
import sys
with ZipFile(sys.argv[1]) as a, ZipFile(sys.argv[2]) as b:
    def payload(z): return {i.filename for i in z.infolist() if not i.is_dir() and not i.filename.startswith('META-INF/')}
    assert payload(a) == payload(b)
    changed = {n for n in payload(a) if a.read(n) != b.read(n)}
    assert changed == {'base/assets/play-release.properties'}, changed
    print('PASS configured AAB payload differs only in public release settings')
PY
java -jar "$tool" build-apks --bundle="$work/configured.aab" --output="$work/configured.apks" \
  --device-spec=app/build/bundle-qa/device.json --ks=project-mister-dev.keystore \
  --ks-key-alias=projectmister --ks-pass=pass:projectmisterdev --key-pass=pass:projectmisterdev
java -jar "$tool" install-apks --apks="$work/configured.apks"
timeout 150s adb shell am instrument -w -e mode editor-configured com.projectmister.game.test/com.projectmister.game.qa.SmokeRunner | tee qa-output/editor-configured.txt
grep -q 'PASS configured bundle checks Play and remains locked without a purchase' qa-output/editor-configured.txt
! grep -Eq 'FAIL|INSTRUMENTATION_FAILED' qa-output/editor-configured.txt
