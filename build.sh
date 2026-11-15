#!/bin/bash
# Hand-rolled APK build: aapt2 (qemu) -> javac -> d8 -> zipalign -> apksigner
set -e

SDK=${ANDROID_HOME:-/opt/android-sdk}
BT=$SDK/build-tools/34.0.0
JAR=$SDK/platforms/android-34/android.jar
PROJ=$(cd "$(dirname "$0")" && pwd)
OUT=$PROJ/build
KS=$PROJ/debug.keystore
QEMU=qemu-x86_64-static

rm -rf "$OUT"
mkdir -p "$OUT/res" "$OUT/gen" "$OUT/classes" "$OUT/dex"

echo "== 1/6 aapt2 compile resources"
"$QEMU" "$BT/aapt2" compile --dir "$PROJ/res" -o "$OUT/res.zip"

echo "== 2/6 aapt2 link"
"$QEMU" "$BT/aapt2" link \
  -o "$OUT/base.apk" \
  -I "$JAR" \
  --manifest "$PROJ/AndroidManifest.xml" \
  -R "$OUT/res.zip" \
  --java "$OUT/gen" \
  -A "$PROJ/assets" \
  --min-sdk-version 24 \
  --target-sdk-version 34 \
  --auto-add-overlay

echo "== 3/6 javac"
find "$PROJ/src" -name '*.java' > "$OUT/sources.txt"
find "$OUT/gen" -name '*.java' >> "$OUT/sources.txt"
javac -encoding UTF-8 -nowarn -source 8 -target 8 \
  -bootclasspath "$JAR" -classpath "$JAR" \
  -d "$OUT/classes" @"$OUT/sources.txt"
if [ ! -f "$OUT/classes/app/turbowarp/android/MainActivity.class" ]; then
  echo "!! javac did not produce MainActivity.class - aborting" >&2
  exit 1
fi

echo "== 4/6 d8 (dex)"
find "$OUT/classes" -name '*.class' > "$OUT/classes.txt"
"$BT/d8" --release --min-api 24 --lib "$JAR" \
  --output "$OUT/dex" @"$OUT/classes.txt"

DEXSIZE=$(stat -c%s "$OUT/dex/classes.dex")
echo "    classes.dex: $DEXSIZE bytes"
for CLS in MainActivity EditorActivity Store Ui ShareProvider; do
  if ! grep -aq "Lapp/turbowarp/android/$CLS;" "$OUT/dex/classes.dex"; then
    echo "!! $CLS missing from classes.dex - build is broken" >&2
    exit 1
  fi
done

echo "== 5/6 package"
cp "$OUT/base.apk" "$OUT/unaligned.apk"
python3 - "$OUT/unaligned.apk" "$OUT/dex" <<'PY'
import sys, zipfile, os, glob
apk, dexdir = sys.argv[1], sys.argv[2]
with zipfile.ZipFile(apk, 'a', zipfile.ZIP_DEFLATED) as z:
    for f in sorted(glob.glob(os.path.join(dexdir, 'classes*.dex'))):
        z.write(f, os.path.basename(f))
PY
"$QEMU" "$BT/zipalign" -f -p 4 "$OUT/unaligned.apk" "$OUT/aligned.apk"

echo "== 6/6 sign"
if [ ! -f "$KS" ]; then
  keytool -genkeypair -keystore "$KS" -alias tw -storepass turbowarp \
    -keypass turbowarp -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=Turbowarp Android, O=TW, C=CN" >/dev/null 2>&1
fi
"$BT/apksigner" sign --ks "$KS" --ks-pass pass:turbowarp --ks-key-alias tw \
  --key-pass pass:turbowarp --out "$OUT/turbowarp_android.apk" "$OUT/aligned.apk"

"$BT/apksigner" verify --print-certs "$OUT/turbowarp_android.apk" | head -4
ls -lh "$OUT/turbowarp_android.apk"
echo "BUILD OK"
