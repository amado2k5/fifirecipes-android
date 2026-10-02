#!/usr/bin/env python3
"""Turn failed tests in JUnit XML reports into GitHub Actions annotations.

Usage: ci-annotate-failures.py <dir> [...]   (searched recursively for *.xml)

Annotations show on the run's checks page and through the GitHub API, so the
failing test and its message can be found without the raw job log or the
report artifact. Never fails the step itself.
"""
import pathlib
import sys
import xml.etree.ElementTree as ET


def esc(s):
    return s.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")


total = 0
for root in sys.argv[1:]:
    for f in sorted(pathlib.Path(root).rglob("*.xml")):
        try:
            tree = ET.parse(f)
        except ET.ParseError:
            continue
        for case in tree.iter("testcase"):
            for bad in list(case.findall("failure")) + list(case.findall("error")):
                total += 1
                name = f"{case.get('classname', '')}.{case.get('name', '')}"
                msg = (bad.get("message") or (bad.text or "").strip().splitlines()[0:1] or ["(no message)"])
                msg = msg if isinstance(msg, str) else msg[0]
                detail = "\n".join((bad.text or "").strip().splitlines()[:15])
                print(f"::error title=Test failed: {esc(name)[:200]}::{esc(msg + chr(10) + detail)[:3000]}")
print(f"{total} failing test(s) annotated")
