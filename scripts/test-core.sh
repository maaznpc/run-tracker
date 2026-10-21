#!/usr/bin/env bash
set -euo pipefail
stride_root="$(cd "$(dirname "$0")/.." && pwd)"
stride_classes="$(mktemp -d)"
trap 'rm -rf "$stride_classes"' EXIT
java com.sun.tools.javac.Main -d "$stride_classes" "$stride_root/app/src/main/java/com/stride/form/GaitMath.java" "$stride_root/tests/GaitMathChecks.java"
java -cp "$stride_classes" com.stride.form.GaitMathChecks
