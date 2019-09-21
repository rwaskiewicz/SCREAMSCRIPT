#!/bin/sh

set -e 

./gradlew clean build
java -jar build/libs/lox-0.0.1-SNAPSHOT.jar
