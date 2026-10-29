#!/usr/bin/env python3
"""Fail if the merged release manifest requests a Play-restricted permission.

Usage: check-permissions.py <dir>   (searched recursively for merged release
AndroidManifest.xml files, e.g. app/build/intermediates)

The app declares only INTERNET, which keeps it out of every Play Console
permission declaration (STORE.md › Google Play policy review). A library
update can merge new permissions in silently; this catches the ones Google
Play restricts or requires a declaration for, so they are a deliberate choice.
"""
import pathlib
import sys
import xml.etree.ElementTree as ET

ANDROID = "{http://schemas.android.com/apk/res/android}"

RESTRICTED = {
    # Local network (Android 17+, declaration for targetSdk 37)
    "android.permission.ACCESS_LOCAL_NETWORK",
    "android.permission.NEARBY_WIFI_DEVICES",
    # Contacts (Manage Contacts declaration)
    "android.permission.READ_CONTACTS",
    "android.permission.WRITE_CONTACTS",
    "android.permission.GET_ACCOUNTS",
    # Location (minimum scope, location button, ACCESS_FINE_LOCATION declaration)
    "android.permission.ACCESS_FINE_LOCATION",
    "android.permission.ACCESS_COARSE_LOCATION",
    "android.permission.ACCESS_BACKGROUND_LOCATION",
    # All files access and media
    "android.permission.MANAGE_EXTERNAL_STORAGE",
    "android.permission.READ_EXTERNAL_STORAGE",
    "android.permission.WRITE_EXTERNAL_STORAGE",
    "android.permission.READ_MEDIA_IMAGES",
    "android.permission.READ_MEDIA_VIDEO",
    # SMS and Call Log groups
    "android.permission.READ_SMS",
    "android.permission.SEND_SMS",
    "android.permission.RECEIVE_SMS",
    "android.permission.RECEIVE_MMS",
    "android.permission.RECEIVE_WAP_PUSH",
    "android.permission.READ_CALL_LOG",
    "android.permission.WRITE_CALL_LOG",
    "android.permission.PROCESS_OUTGOING_CALLS",
    # Other declaration-gated permissions
    "android.permission.QUERY_ALL_PACKAGES",
    "android.permission.REQUEST_INSTALL_PACKAGES",
    "android.permission.USE_FULL_SCREEN_INTENT",
    "android.permission.SCHEDULE_EXACT_ALARM",
    "android.permission.USE_EXACT_ALARM",
    "com.google.android.gms.permission.AD_ID",
}

manifests = [
    f for f in pathlib.Path(sys.argv[1]).rglob("AndroidManifest.xml")
    if "merged_manifest" in f.parts and any("release" in p.lower() for p in f.parts)
]
if not manifests:
    print(f"::error::No merged release manifest found under {sys.argv[1]}")
    sys.exit(1)

bad = set()
for f in manifests:
    root = ET.parse(f).getroot()
    tags = ("uses-permission", "uses-permission-sdk-23")
    names = {e.get(ANDROID + "name") for t in tags for e in root.iter(t)}
    for name in sorted(names & RESTRICTED):
        print(f"::error file={f}::Play-restricted permission {name} in the merged manifest")
        bad.add(name)
    print(f"{f}: {', '.join(sorted(n for n in names if n)) or '(none)'}")

sys.exit(1 if bad else 0)
