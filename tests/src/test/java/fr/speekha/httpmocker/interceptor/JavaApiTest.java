/*
 * Copyright 2019-2020 David Blanc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.speekha.httpmocker.interceptor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import fr.speekha.httpmocker.Mode;
import fr.speekha.httpmocker.builder.FileLoader;
import fr.speekha.httpmocker.io.HttpRequest;
import fr.speekha.httpmocker.jackson.JacksonMapper;
import fr.speekha.httpmocker.model.ResponseDescriptor;
import fr.speekha.httpmocker.okhttp.builder.InterceptorBuilder;
import fr.speekha.httpmocker.policies.FilingPolicy;
import fr.speekha.httpmocker.serialization.Mapper;
import okhttp3.OkHttpClient;
import okhttp3.Response;

@DisplayName("Java API")
class JavaApiTest extends OkHttpTests {

    @Nested
    @DisplayName("Given a Java code base, When using the Kotlin API")
    public class WithJavaCode extends OkHttpTests {

        @Test
        @DisplayName("Then dynamic mocks should work properly")
        public void shouldUseDynamicMocksWithJavaApi() throws IOException {
            initInterceptor(getFilingPolicy());
            Response response = executeRequest("/dynamic");
            Assertions.assertEquals("dynamic", response.body().string());
        }

        @Test
        @DisplayName("Then static mocks should work properly")
        public void shouldUseStaticMocksWithJavaApi() throws IOException {
            FilingPolicy filingPolicy = getFilingPolicy();
            initInterceptor(filingPolicy);
            executeRequest("/static");
            HttpRequest request = new HttpRequest("GET", "http", "127.0.0.1", getServer().getPort(), "/static");
            Mockito.verify(filingPolicy).getPath(request);
        }


        @Test
        @DisplayName("Then recording should work properly")
        public void shouldRecordWithJavaApi() throws IOException {
            enqueueServerResponse(200, "body", new ArrayList<>(), null);
            FilingPolicy filingPolicy = getFilingPolicy();
            initInterceptor(filingPolicy);
            getInterceptor().setMode(Mode.RECORD);
            executeRequest("record/request");
            assertFileExists(RecordTestsKt.SAVE_FOLDER + "/request.json");
            assertFileExists(RecordTestsKt.SAVE_FOLDER + "/request_body_0.txt");
        }

        @AfterEach
        public void clearFolder() {
            RecordTestsKt.clearTestFolder();
        }

        private void initInterceptor(FilingPolicy policy) {
            Mapper mapper = new JacksonMapper();
            interceptor = new InterceptorBuilder()
                    .useDynamicMocks(this::getResponseDescriptor)
                    .decodeScenarioPathWith(policy)
                    .loadFileWith((FileLoader) file -> getClass().getClassLoader().getResourceAsStream(file))
                    .parseScenariosWith(mapper)
                    .setInterceptorStatus(Mode.ENABLED)
                    .saveScenarios(new File(RecordTestsKt.SAVE_FOLDER), policy)
                    .build();
            client = new OkHttpClient.Builder().addInterceptor(interceptor).build();
        }

        @Nullable
        private ResponseDescriptor getResponseDescriptor(okhttp3.Request request) {
            if (request.url().encodedPath().contains("dynamic")) {
                return new ResponseDescriptor(0L, 0, "text/plain", new ArrayList<>(), "dynamic", null);
            } else {
                return null;
            }
        }

        @Nullable
        private ResponseDescriptor getResponseDescriptor(HttpRequest request) {
            if (request.getPath().contains("dynamic")) {
                return new ResponseDescriptor(0L, 0, "text/plain", new ArrayList<>(), "dynamic", null);
            } else {
                return null;
            }
        }

        @NotNull
        private FilingPolicy getFilingPolicy() {
            FilingPolicy filingPolicy = Mockito.mock(FilingPolicy.class);
            Mockito.when(filingPolicy.getPath(Mockito.any())).thenReturn("request.json");
            return filingPolicy;
        }
    }
}
