#!/bin/sh

# warning, this generates java code (spooky!) - this is not idempotent as an invocation could result in failing to
# compile if certain interface methods are not implemented
./gradlew clean build -b ./build-ast.gradle
java -jar build/libs/lox-0.0.1-SNAPSHOT.jar ./src/main/java/com/craftinginterpreters/lox
