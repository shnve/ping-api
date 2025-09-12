#!/bin/bash

CURRENT_VERSION=$(mvn -q -Dexec.executable="echo" -Dexec.args='${project.version}' --non-recursive org.codehaus.mojo:exec-maven-plugin:1.3.1:exec)
echo "CURRENT_VERSION=${CURRENT_VERSION}"

if [[ "${CURRENT_VERSION}" == *"SNAPSHOT" ]]; then
  RELEASE_VERSION=$(echo $CURRENT_VERSION | sed -r "s/(.*)-SNAPSHOT/\1/g")
  echo "RELEASE_VERSION=${RELEASE_VERSION}"
  mvn versions:set -DnewVersion=${RELEASE_VERSION} versions:commit
  git add .
  git commit -m "Releasing version ${RELEASE_VERSION}"
  git tag ${RELEASE_VERSION}
  NEXT_SNAPSHOT_VERSION=$(echo $RELEASE_VERSION | sed -r 's/^([0-9]+)\.([0-9]+)\.([0-9]+)$/echo "\1\.$((\2+1))\.0-SNAPSHOT"/ge')
  echo "NEXT_SNAPSHOT_VERSION=${NEXT_SNAPSHOT_VERSION}"
  mvn versions:set -DnewVersion=${NEXT_SNAPSHOT_VERSION} versions:commit
  git add .
  git commit -m "Prepare next development cycle with version ${NEXT_SNAPSHOT_VERSION}"
  git push --tags origin $1
else
  echo "Can't release a release version"
  exit 1
fi
