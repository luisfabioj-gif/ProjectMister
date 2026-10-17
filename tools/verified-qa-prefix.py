#!/usr/bin/env python3
"""Reuse passed Android modes only for the identical game and a division-only QA fix."""
import argparse
import hashlib
import io
import json
import os
from pathlib import Path
import subprocess
import time
import urllib.error
import urllib.request
import zipfile

REPO = "luisfabioj-gif/ProjectMister"
RUN = 38984023562
BASE = "85d4de03593b22a0bf9d3d6e5a34e00ea838aa37"
DEST = Path("qa-output/previous-verified")
PRODUCTION = ["app/src/main", "app/build.gradle", "build.gradle", "settings.gradle", "gradle.properties", "gradle", "project-mister-dev.keystore"]
QA = "app/src/androidTest/java/com/projectmister/game/qa/SmokeRunner.java"
REQUIRED = {
    "baseline.txt": ["baseline career seeded"],
    "feedback.txt": ["all eleven feedback areas, exact identities, timed scouting, ordinal tables and sixty-four stadium upgrades verified"],
    "candidate.txt": ["worldwide transfers free agents reload budget and lineup verified", "calendar recorded results and shootout orientation verified", "completed match survives reload"],
    "rules.txt": ["twenty registration cases, calendar-week accounting and ten isolated final league decisions verified"],
    "uefa-players.txt": ["UEFA actual A/B lists, late free-agent exclusion, saved goalkeeper identities, registered ban service, three winter additions and atomic backups verified"],
    "cup-players.txt": ["actual cup dismissal, eligible lineup, ban service, transfer persistence and atomic backup rejection verified", "Scottish Cup transfer restriction blocks an actual ineligible lineup and preserves playable progression and exact saves", "French actual cup match-sheet continuity, free-agent registrations, unused bench distinction and exact saves verified"],
    "national-cups.txt": ["German recurring cup live matches two seasons reloads history and backup rejection verified"],
}
for country in ("ENG", "ES", "PT", "DE", "IT", "FR", "NL", "BE", "SCO", "TR"):
    REQUIRED[f"europe-{country}.txt"] = [f"{country} combined UEFA qualifying domestic calendars two seasons watched quick matches exact archives and backups verified"]
for country in ("ENG", "ES", "IT", "FR", "NL", "BE", "SCO", "TR"):
    REQUIRED[f"cups-{country}.txt"] = [f"{country} recurring cups watched and quick matches two seasons reloads history and backup rejection verified"]


def digest(data):
    return hashlib.sha256(data).hexdigest()


def validate_reports(root):
    reports = {}
    for name, markers in REQUIRED.items():
        raw = (Path(root) / name).read_bytes()
        text = raw.decode("utf-8")
        if "FAIL" in text or "INSTRUMENTATION_FAILED" in text:
            raise ValueError(f"Prior Android mode did not pass: {name}")
        for marker in markers:
            if "PASS " + marker not in text:
                raise ValueError(f"Missing terminal acceptance in {name}: {marker}")
        reports[name] = {"sha256": digest(raw), "passing_assertions": sum(line.startswith("PASS ") for line in text.splitlines())}
    return reports


def eligible(base=BASE):
    if subprocess.run(["git", "diff", "--quiet", base, "HEAD", "--", *PRODUCTION]).returncode:
        return False
    if subprocess.run(["git", "diff", "--quiet", base, "HEAD", "--", "tests/fixtures", "app/src/androidTest/AndroidManifest.xml", "tools/build-test-sdk.sh"]).returncode:
        return False
    def outside_division_method(text):
        start = text.index("    private void verifyDivisionCareers()")
        end = text.index("    private Class<?> gameClass", start)
        return text[:start], text[end:]
    old = subprocess.check_output(["git", "show", f"{base}:{QA}"], text=True)
    return outside_division_method(old) == outside_division_method(Path(QA).read_text())


def api(path):
    request = urllib.request.Request("https://api.github.com/" + path, headers={"Authorization": "Bearer " + os.environ["GH_TOKEN"], "Accept": "application/vnd.github+json", "X-GitHub-Api-Version": "2022-11-28"})
    with urllib.request.urlopen(request, timeout=60) as response:
        return json.load(response)


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, request, fp, code, message, headers, newurl):
        return None


def archive(artifact):
    # Keep the repository token on api.github.com; never forward it to blob storage.
    request = urllib.request.Request(artifact["archive_download_url"], headers={"Authorization": "Bearer " + os.environ["GH_TOKEN"]})
    try:
        with urllib.request.build_opener(NoRedirect).open(request, timeout=60) as response:
            raw = response.read()
    except urllib.error.HTTPError as error:
        if error.code not in (301, 302, 303, 307, 308):
            raise
        with urllib.request.urlopen(error.headers["Location"], timeout=120) as response:
            raw = response.read()
    if artifact.get("digest") != "sha256:" + digest(raw):
        raise ValueError("Prior artifact ZIP checksum mismatch")
    with zipfile.ZipFile(io.BytesIO(raw)) as files:
        if files.testzip() is not None:
            raise ValueError("Prior artifact ZIP CRC failure")
        DEST.mkdir(parents=True, exist_ok=True)
        for item in files.infolist():
            path = (DEST / item.filename).resolve()
            if not path.is_relative_to(DEST.resolve()):
                raise ValueError("Invalid prior artifact path")
            # Preserve the failure as diagnostics, never as a passed division gate.
            if item.filename == "divisions.txt":
                path = DEST / "diagnostics/divisions-before-index-fix.txt"
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_bytes(files.read(item))
            else:
                files.extract(item, DEST)
    return digest(raw)


def write_output(reuse):
    with open(os.environ["GITHUB_OUTPUT"], "a") as output:
        output.write(f"reuse={'true' if reuse else 'false'}\n")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--api", type=int, choices=(35, 36))
    parser.add_argument("--validate-directory", type=Path)
    parser.add_argument("--check-local", action="store_true")
    args = parser.parse_args()
    if args.validate_directory:
        print(json.dumps(validate_reports(args.validate_directory), indent=2))
        return
    if args.check_local:
        evidence = json.loads((DEST / "evidence.json").read_text())
        assert eligible() and evidence["source"] == BASE and evidence["run"] == RUN
        assert int(subprocess.check_output(["adb", "shell", "getprop", "ro.build.version.sdk"], text=True).strip()) == evidence["api"]
        assert evidence["reports"] == validate_reports(DEST)
        proof = json.loads(Path("qa-output/apk-equivalence.json").read_text())
        actual = digest(Path("app/build/outputs/apk/debug/app-debug.apk").read_bytes())
        assert proof["source"] == BASE and proof["candidate_sha256"] == proof["reference_sha256"] == actual
        print(f"PASS identical APK and all {len(REQUIRED)} inherited Android modes validated; all forty division careers and downstream release gates must run now")
        return
    if not eligible():
        print("Game code, fixtures or other QA modes changed: run the complete native suite")
        write_output(False)
        return
    try:
        run = api(f"repos/{REPO}/actions/runs/{RUN}")
        if run["head_sha"] != BASE:
            raise ValueError("Wrong source run")
        found = None
        for attempt in range(20):
            artifacts = api(f"repos/{REPO}/actions/runs/{RUN}/artifacts?per_page=100")["artifacts"]
            found = next((a for a in artifacts if a["name"] == f"BOSS-XI-v2.3-QA-api{args.api}" and not a["expired"]), None)
            if found:
                break
            if attempt < 19:
                time.sleep(30)
        if found is None:
            raise ValueError("Prior API-specific evidence is unavailable")
        sha = archive(found)
        reports = validate_reports(DEST)
        (DEST / "evidence.json").write_text(json.dumps({"source": BASE, "run": RUN, "api": args.api, "artifact_id": found["id"], "zip_sha256": sha, "reports": reports}, indent=2) + "\n")
    except (OSError, ValueError, KeyError, AssertionError, zipfile.BadZipFile) as error:
        print(f"Prior evidence cannot be reused ({type(error).__name__}); run the complete native suite")
        write_output(False)
        return
    print(f"PASS {len(REQUIRED)} previously completed Android {args.api} modes verified for unchanged game code; the corrected division suite and release checks remain mandatory")
    write_output(True)


if __name__ == "__main__":
    main()
