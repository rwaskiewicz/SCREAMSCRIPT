#!/bin/sh

./gradlew clean build -b ./build-generate-ast.gradle
java -jar build/libs/lox-0.0.1-SNAPSHOT.jar ./src/main/java/com/craftinginterpreters/lox
