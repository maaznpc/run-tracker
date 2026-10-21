#!/usr/bin/env sh
# Minimal Gradle wrapper launcher. The official Gradle 8.13 wrapper JAR is bundled.
set -eu
STRIDE_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
if [ -n "${JAVA_HOME:-}" ]; then
  STRIDE_JAVA="$JAVA_HOME/bin/java"
else
  STRIDE_JAVA=java
fi
exec "$STRIDE_JAVA" ${JAVA_OPTS:-} ${GRADLE_OPTS:-} -classpath "$STRIDE_ROOT/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
