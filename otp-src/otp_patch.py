from pathlib import Path
p=Path("extracted/NewMaDurgaAndroid/app/src/main/java/com/newmadurga/billing/MainActivity.java")
s=p.read_text()
s=s.replace("import android.Manifest;", "import android.Manifest;\nimport com.google.firebase.FirebaseApp;\nimport com.google.firebase.auth.FirebaseAuth;")
needle="        super.onCreate(state);\n"
insert=("        super.onCreate(state);\n"
        "        FirebaseApp.initializeApp(this);\n"
        "        if (FirebaseAuth.getInstance().getCurrentUser() == null) {\n"
        "            startActivity(new Intent(this, OtpLoginActivity.class));\n"
        "            finish();\n"
        "            return;\n"
        "        }\n")
if "OtpLoginActivity.class" not in s:
    if needle not in s: raise SystemExit("MainActivity onCreate anchor not found")
    s=s.replace(needle,insert,1)
p.write_text(s)
g=Path("extracted/NewMaDurgaAndroid/app/build.gradle")
gs=g.read_text()
if "firebase-auth" not in gs:
    gs=gs.replace("android {", "dependencies {\n    implementation platform('com.google.firebase:firebase-bom:33.7.0')\n    implementation 'com.google.firebase:firebase-auth'\n}\n\nandroid {",1)
g.write_text(gs)
m=Path("extracted/NewMaDurgaAndroid/app/src/main/AndroidManifest.xml")
ms=m.read_text()
if "OtpLoginActivity" not in ms:
    ms=ms.replace('<activity android:name=".MainActivity"', '<activity android:name=".OtpLoginActivity" android:exported="false" />\n        <activity android:name=".MainActivity"',1)
m.write_text(ms)
Path("extracted/NewMaDurgaAndroid/app/src/main/java/com/newmadurga/billing/OtpLoginActivity.java").write_text(Path("otp-src/OtpLoginActivity.java").read_text())
