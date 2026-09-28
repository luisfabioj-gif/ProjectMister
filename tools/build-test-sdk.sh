#!/usr/bin/env bash
set -euo pipefail
: "${ANDROID_HOME:?}" "${JAVA_HOME:?}"
bt="$ANDROID_HOME/build-tools/35.0.0"; platform="$ANDROID_HOME/platforms/android-35/android.jar"
out=app/build/test-manual
mkdir -p "$out/classes" "$out/dex"
"$bt/aapt2" link -o "$out/resources.apk" -I "$platform" --manifest app/src/androidTest/AndroidManifest.xml --min-sdk-version 26 --target-sdk-version 35
"$JAVA_HOME/bin/javac" -source 17 -target 17 -cp "$platform" -d "$out/classes" app/src/androidTest/java/com/projectmister/game/qa/*.java
"$JAVA_HOME/bin/jar" cf "$out/classes.jar" -C "$out/classes" .
"$bt/d8" --lib "$platform" --min-api 26 --output "$out/dex" "$out/classes.jar"
python3 - <<'PY'
import zipfile,shutil
from pathlib import Path
p=Path('app/build/test-manual');shutil.copy(p/'resources.apk',p/'unsigned.apk')
with zipfile.ZipFile(p/'unsigned.apk','a',zipfile.ZIP_DEFLATED) as z:
 for d in (p/'dex').glob('*.dex'):z.write(d,d.name)
PY
"$bt/zipalign" -f -p 4 "$out/unsigned.apk" "$out/aligned.apk"
"$bt/apksigner" sign --ks project-mister-dev.keystore --ks-key-alias projectmister --ks-pass pass:projectmisterdev --key-pass pass:projectmisterdev --out "$out/qa.apk" "$out/aligned.apk"
