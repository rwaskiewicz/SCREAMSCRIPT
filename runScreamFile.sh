#!/bin/sh

set -e

if [ "$#" -ne 2 ]; then
  echo "Usage ./runLoxFile.sh -- <FILE>"
  exit 1;
fi

./gradlew clean build
java -jar build/libs/lox-0.0.1-SNAPSHOT.jar $2
