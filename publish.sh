#!/bin/sh
./gradlew mocker:bintrayUpload -Dbintray.user=$1 -Dbintray.key=$2 --stacktrace