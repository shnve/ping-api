#!/bin/bash -x
set -eo pipefail

cd /opt/app
exec java $JAVA_OPTS $@ -jar app.jar
