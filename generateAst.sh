#!/bin/sh

./gradlew clean build
java -jar build/libs/lox-0.0.1-SNAPSHOT.jar ./src/main/java/com/craftinginterpreters/lox
