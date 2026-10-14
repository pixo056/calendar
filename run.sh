#!/bin/sh
set -eu
CALENDAR_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$CALENDAR_ROOT"
if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    CALENDAR_JAVA="$JAVA_HOME/bin/java"
elif command -v java >/dev/null 2>&1; then
    CALENDAR_JAVA=java
else
    echo 'Java 25 or newer is required. Install Java and set JAVA_HOME or add java to PATH.' >&2
    exit 1
fi
CALENDAR_JAR="$CALENDAR_ROOT/calendar.jar"
if [ ! -f "$CALENDAR_JAR" ]; then
    CALENDAR_JAR="$CALENDAR_ROOT/target/calendar.jar"
fi
if [ ! -f "$CALENDAR_JAR" ]; then
    if [ ! -f "$CALENDAR_ROOT/pom.xml" ]; then
        echo 'calendar.jar is missing. Extract the complete Calendar ZIP archive first.' >&2
        exit 1
    fi
    export MAVEN_USER_HOME="$CALENDAR_ROOT/.mvn/user-home"
    sh ./mvnw package
fi
echo "Open http://localhost:${PORT:-8080} in your browser. Keep this terminal open."
exec "$CALENDAR_JAVA" -jar "$CALENDAR_JAR" "$@"
