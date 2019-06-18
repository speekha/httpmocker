#!/bin/sh
./gradlew mocker:artifactoryPublish -Dsnapshot=true -Dbintray.user=$1 -Dbintray.key=$2 --stacktrace