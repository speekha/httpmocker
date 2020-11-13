#!/bin/sh
#
# Copyright 2019-2020 David Blanc
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

./gradlew --no-daemon mocker-core:bintrayUpload mocker-okhttp:bintrayUpload mocker-ktor:bintrayUpload jackson-adapter:bintrayUpload gson-adapter:bintrayUpload moshi-adapter:bintrayUpload custom-adapter:bintrayUpload kotlinx-adapter:bintrayUpload sax-adapter:bintrayUpload -Dbintray.user=$1 -Dbintray.key=$2 --stacktrace