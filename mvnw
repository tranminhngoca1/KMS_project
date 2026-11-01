#!/bin/sh
MAVEN_VERSION=3.9.9
MAVEN_HOME="$HOME/.m2/wrapper/dists/apache-maven-$MAVEN_VERSION"
if [ ! -f "$MAVEN_HOME/bin/mvn" ]; then
  mkdir -p "$HOME/.m2/wrapper/dists"
  curl -sL "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MAVEN_VERSION/apache-maven-$MAVEN_VERSION-bin.tar.gz" | tar xz -C "$HOME/.m2/wrapper/dists"
fi
exec "$MAVEN_HOME/bin/mvn" "$@"

