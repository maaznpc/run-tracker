#!/usr/bin/env python3
"""Fetch the official, version-1 MediaPipe Pose Landmarker Lite task for bundling."""
from pathlib import Path
import hashlib
import os
import sys
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[1]
DESTINATION = ROOT / "app/src/main/assets/pose_landmarker_lite.task"
URL = "https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_lite/float16/1/pose_landmarker_lite.task"

def validate(path):
    if path.stat().st_size < 1_000_000:
        raise ValueError("Model is smaller than expected")
    with zipfile.ZipFile(path) as task:
        if task.testzip() is not None:
            raise ValueError("Model ZIP checksum failed")
        names = task.namelist()
        if sum(name.endswith(".tflite") for name in names) < 2:
            raise ValueError("Expected the pose detection and landmark models")

def main():
    DESTINATION.parent.mkdir(parents=True, exist_ok=True)
    if DESTINATION.exists():
        validate(DESTINATION)
        print("Official pose model is already bundled.")
    else:
        temporary = DESTINATION.with_suffix(".download")
        try:
            print("Downloading MediaPipe Pose Landmarker Lite v1 from Google…", flush=True)
            # urllib respects the configured proxy and CA trust; no direct-network fallback.
            with urllib.request.urlopen(URL, timeout=120) as response, temporary.open("wb") as output:
                while chunk := response.read(1024 * 1024):
                    output.write(chunk)
            validate(temporary)
            os.replace(temporary, DESTINATION)
        finally:
            temporary.unlink(missing_ok=True)
    digest = hashlib.sha256(DESTINATION.read_bytes()).hexdigest()
    (ROOT / "docs/pose-model.sha256").write_text(digest + "  app/src/main/assets/pose_landmarker_lite.task\n")
    print("Bundled model SHA-256: " + digest)
    return 0

if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as error:
        print("Model download failed: " + str(error), file=sys.stderr)
        print("Check network access to storage.googleapis.com, then run this script again.", file=sys.stderr)
        sys.exit(1)
