#!/usr/bin/env sh
set -e

CDS_ARGS=""
if [ -f /tmp/app.jsa ]; then
  CDS_ARGS="$JAVA_CDS_OPTS"
fi

exec java $CDS_ARGS -jar app.jar