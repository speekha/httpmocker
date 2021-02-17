#!/bin/sh
#
# Copyright 2019-2021 David Blanc
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

./gradlew --no-daemon --no-parallel mocker-core:uploadArchives mocker-okhttp:uploadArchives mocker-ktor:uploadArchives jackson-adapter:uploadArchives gson-adapter:uploadArchives moshi-adapter:uploadArchives custom-adapter:uploadArchives kotlinx-adapter:uploadArchives sax-adapter:uploadArchives -DNEXUS_USERNAME=$1 -DNEXUS_PASSWORD=$2 --stacktrace