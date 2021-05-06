# HttpMocker

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/fr.speekha.httpmocker/mocker-core/badge.svg?color=blue)](https://search.maven.org/search?q=fr.speekha.httpmocker)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](https://www.apache.org/licenses/LICENSE-2.0)
[![Kotlin](https://img.shields.io/badge/kotlin-1.4.31-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![HacktoberFest](https://badgen.net/badge/hacktoberfest/friendly)](https://hacktoberfest.digitalocean.com/)

[![CircleCI](https://circleci.com/gh/speekha/httpmocker/tree/develop.svg?style=shield)](https://circleci.com/gh/speekha/httpmocker/tree/develop)
[![codebeat badge](https://codebeat.co/badges/a361e616-a1a0-4e85-b6a7-2bc82f31f7ac)](https://codebeat.co/projects/github-com-speekha-httpmocker-develop)
[![CodeFactor](https://www.codefactor.io/repository/github/speekha/httpmocker/badge)](https://www.codefactor.io/repository/github/speekha/httpmocker)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/754e2a65060a48c9bfc580a36063d206)](https://www.codacy.com/app/speekha/httpmocker)
[![Codacy Badge](https://api.codacy.com/project/badge/Coverage/754e2a65060a48c9bfc580a36063d206)](https://www.codacy.com/app/speekha/httpmocker)

**HttpMocker** is a very lightweight Kotlin library that allows to mock HTTP calls relying on either
OkHttp or the Ktor client libraries.

* It can be used for unit or integration tests: by providing predefined responses and instead of actual 
calls to servers, you're avoiding the risk of unpredictable results due to network failure or server errors. 

* It can also be used to implement an offline mode in your application for debugging or demo purposes:
you can prepare complete scenarios that will let users explore your app's features without the need for an actual
connection or account.

Thanks to the MockResponseInterceptor (for OkHttp) or the mockableHttpClient (for Ktor), web service 
calls will not be dispatched to the network, but responses will be read from static configuration files
or computed dynamically instead. The mocker also allows recording scenarios that can be reused later.

## Current Version

```gradle
httpmocker_version = '2.0.0-alpha'
```

Current version is stable for Android/JVM builds. It still has Alpha status because we would like to add support for iOS. 
Any help with implemention and deployment for iOS is welcome.

## Gradle 

### Maven Central

For stable versions, check that you have the `mavenCentral` repository.

```gradle
// Add Jcenter to your repositories if needed
repositories {
	mavenCentral()
}
```

For Snapshot versions, check that you have Sonatype's snapshot repository.

```gradle
// Add oss.sonatype.org to your repositories if needed
repositories {
    maven { url 'https://oss.sonatype.org/content/repositories/snapshots' }
}
```

### Dependencies

#### Adding HttpMocker

This library contains several parts: 
* a core module handling the mock logic
* an engine module corresponding to the HTTP library you use (OkHttp or Ktor)
* an additional adapter to parse the scenario files for static mocks

You can add the dependency to your `build.gradle` by adding on of the following lines:

```gradle
// Core module: you don't need to add it explicitly to your gradle (it will be included as a transitive dependency 
// of the other modules)
implementation "fr.speekha.httpmocker:mocker-core:2.0.0-alpha"

// Handles mocks for OkHttp
implementation "fr.speekha.httpmocker:mocker-okhttp:2.0.0-alpha"

// Handles mocks for KTor
implementation "fr.speekha.httpmocker:mocker-ktor:2.0.0-alpha"
```

Currently, there are six possible options that are provided for parsing, based on some of the 
most commonly used serialization libraries and on a custom implementation (no third party dependency): 
* Jackson
* Gson
* Moshi
* Kotlinx serialization
* Custom JSON implementation
* Custom Sax-based implementation (uses XML scenarios instead of JSON)

This should allow you to choose one matching what you already use in your application (in order to prevent 
duplicate libraries in your classpath, like Jackson and GSON). If you would prefer to use XML instead 
of JSON, the SAX-based parser allows you to do it. If you choose one of these options, all you need to add is the 
corresponding `implementation` line in your gradle file:

```gradle
// Parses JSON scenarios using Jackson
implementation "fr.speekha.httpmocker:jackson-adapter:2.0.0-alpha"

// Parses JSON scenarios using Gson
implementation "fr.speekha.httpmocker:gson-adapter:2.0.0-alpha"

// Parses JSON scenarios using Moshi
implementation "fr.speekha.httpmocker:moshi-adapter:2.0.0-alpha"

// Parses JSON scenarios using Kotlinx Serialization
implementation "fr.speekha.httpmocker:kotlinx-adapter:2.0.0-alpha"

// Parses JSON scenarios using a custom JSON parser
implementation "fr.speekha.httpmocker:custom-adapter:2.0.0-alpha"

// Parses XML scenarios using a custom SAX parser
implementation "fr.speekha.httpmocker:sax-adapter:2.0.0-alpha"
```

If none of those options suit your needs, you can also provide your own implementation of the `Mapper` class. You can 
also bypass that dependency altogether if you don't plan on using static mocks or the recording mode.

#### External dependencies

* HttpMocker is a mocking library for third party HTTP clients, so it depends on OkHttp (as of v1.3.0, HttpMocker uses 
OkHttp 4 API, previous versions used OkHttp 3) or Ktor (v1.5.0), depending on which implementation you chose.
* It also uses SLF4J API for logging.
* JSON parsers depend on their respective external libraries: Jackson, Gson, Moshi or KotlinX serialization.

### Proguard rules

Since Jackson, Gson or Kotlinx serialization use some type of introspection or annotation 
processing to parse JSON streams, it is recommended to keep the mapping classes unobfuscated. 
You can refer to those libraries recommendations and to the [proguard-rules.pro](demo/proguard-rules.pro) file included 
in the demo app for an example of required rules.

The custom and moshi parsers are immune to obfuscation because they do not use any introspection.

## Quickstart

### Setting up HttpMocker with OkHttp

Mocking HTTP calls relies on a simple Interceptor: MockResponseInterceptor. All you need to set it up
is to add it to your OkHttp client. Here's an example with minimal configuration of dynamic mocks 
(this configuration will respond to every request with a `HTTP 200 Ok` code and a body containing 
only `Fake response body`) 
* __Java-friendly builder syntax__:
```kotlin
    val interceptor = Builder()
        .useDynamicMocks {
            ResponseDescriptor(code = 200, body = "Fake response body")
        }
        .setMode(ENABLED)
        .build()
    val client = OkHttpClient.Builder()
        .addInterceptor(interceptor)
        .build()
```

* **Kotlin DSL syntax**:
```kotlin
    val interceptor = mockInterceptor {
        useDynamicMocks {
            ResponseDescriptor(code = 200, body = "Fake response body")
        }
        setMode(ENABLED)
    }
    val client = OkHttpClient.Builder()
        .addInterceptor(interceptor)
        .build()
```

### Setting up HttpMocker with Ktor

Mocking HTTP calls relies on a HTTP client that will include two engines: one for actual network calls and one for mocked
calls. The syntax to create this client is very similar to the one for the standard HTTP client, but uses a 
`mockableHttpClient` builder instead of the usual `httpClient`. You can keep all your usual configuration and just add 
the mock configuration inside a `mock` section. Here is an example that shows how the mock configuration is added to a 
regular Ktor client (with a CIO engine and JSON parsing with Kotlinx Serialization):
```kotlin
    val client = mockablHttpClient(CIO) {
        // This part defines the mock configuration to use
        mock {
            useDynamicMocks {
                ResponseDescriptor(code = 200, body = "Fake response body")
            }
            setMode(ENABLED)
        }

        // Here is the rest of your standard HTTP Configuration
        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }
        expectSuccess = false
        followRedirects = false
    }
```

### General use

HttpMocker supports four modes:
* Disabled
* Enabled
* Mixed
* Record

You can set that mode when initializing your mocker object (if not, it is disabled by default), but you 
can also change its value dynamically later on.

```kotlin
    // For OkHttp, you can change the mode directly in your interceptor: 
    interceptor.mode = ENABLED

    // For Ktor, you will need to access the engine configuration:
    (client.engine as MockEngine).mode = ENABLED
```

If your mocker is `disabled`, it will not interfere with actual network calls. If it is `enabled`, 
it will need to find scenarios to mock the HTTP calls. Dynamic mocks imply that you have to 
provide the response for each request programmatically, which allows you to define stateful 
responses (identical calls could lead to different answers based on what the user did in between 
these calls). The response can be provided as a `ResponseDescriptor` by implementing the 
`RequestCallback` interface, or you can simply provide a lambda function to do the computation. 

*NB: If you use dynamic mocks, the `bodyFile` attribute of your `ResponseDescriptor` is not needed (it 
will be ignored). Its only use is for static scenarios that could save the response body in a separate 
file (it keeps things clearer by not including the response file mixed with the scenario).*

Another option is to use static mocks. Static mocks are scenarios stored as static files. Here is 
an example for an Android app using static mocks, with a few more options:
```kotlin
    // For OkHttp:
    val interceptor = mockInterceptor {
            parseScenariosWith(mapper)
            decodeScenarioPathWith(MirrorPathPolicy("json"))
            loadFileWith { 
                context.assets.open(it).asReader()
            }
            setMode(ENABLED)
            recordScenariosIn(rootFolder)
            addFakeNetworkDelay(50L)
        }

    // For Ktor:
    val client = mockableHttpClient(CIO) {
            mock {
                parseScenariosWith(mapper)
                decodeScenarioPathWith(MirrorPathPolicy("json"))
                loadFileWith { 
                    context.assets.open(it).asReader()
                }
                setMode(ENABLED)
                recordScenariosIn(rootFolder)
                addFakeNetworkDelay(50L)
            }
        }
```

In this example, we decided to store the scenarios in the `assets` folder of the app (but you 
could also have them as resources in your classpath and use the `Classloader` to access them or 
even store them in a certain folder and access that folder with any File API you're comfortable 
with). You also need to provide the `FilingPolicy` you want to use: that policy defines which file to 
check to find a match for a request. A few policies are provided in the library, but you can also 
define your own. 

Additionally, you need to provide a Mapper to parse the scenario files. As stated earlier, scenarios 
can be stored as JSON or XML depending on your preference, or any other format, for that matter,
as long as you provide your own `Mapper` class to serialize and deserialize the business objects. As 
far as this lib is concerned though, a few mappers are available out of the box to handle JSON or 
XML formats. The main mappers are based on Jackson, Gson, Moshi and Kotlinx serialization. They 
are provided in specific modules so you can choose one based on the library you might already use 
in your application, thus limiting the risk for duplicate libraries serving the same purpose in 
your app. Two additional implementations, based on a custom JSON or XML parsers that do not need 
any other dependencies are also available. 

Static and dynamic scenarios can be used together. Several dynamic callbacks can be added to the 
mocker, but only one static configuration is allowed. Dynamic callbacks will be used first to 
find a suitable mock, and if none is found, the static configuration will be tested next.

If you choose the `mixed` mode, requests that can not be answered by a predefined scenario will 
actually be executed. Hence the mixed mode: responses can come from a mock scenario (either static or 
dynamic) or from an actual HTTP call.

Finally, the mocker also has a `recording` mode. This mode allows you to record scenarios 
without interfering with your request. If you choose this mode to produce your static scenarios, 
you will have to provide a root folder where the scenarios should be stored. Also, you should 
realize that all request and response attributes will be recorded. Generally, you will want to 
review the resulting scenarios and clean them up a bit manually. For instance, each request will be 
recorded with its HTTP method, its path, each header or parameter, its body. But all those details 
might not be very important to you. Maybe all you care about is the URL and method, in which case, 
you can delete all the superfluous criteria manually.

## Building static scenarios

Answering a request with a static mock is done in two steps:
* First, if the mocker is enabled (or in mixed mode), it will try to compute a file name were the appropriate 
scenario should be stored. Based on the filing policy you chose, those files can be organized in a lot of different 
ways: all in a single file, or different files in the same folder, in separate folders matching the URL path,
ignoring or not the server hostname. This part allows you to decide how you want to organize your mock files for an 
easier navigation (if you have lots of scenarios to handle, the Single File policy will probably not be the best option).

* Second, once the file is found, its content will be loaded, and a more exact match will have to be found. Scenario 
files contain a list of "Matchers", that is a list of request patterns and corresponding responses. Based on the 
request it is trying to answer, the mocker is going to scan through all the request declarations and stop as soon
as it finds one that matches the situation.

When writing a request pattern, the elements included are supposed to be found in the requests to match. The more 
elements, the more precise the match has to be. The less elements, the more permissive the match. A request can even 
be omitted altogether (in this case, all requests match):
* When specifying a method, matching request have to use the same HTTP method.
* When specifying query parameters, matching requests must have at least all these parameters, but can have more.
* When specifying headers, matching requests must have all the same headers, but can have more.

*NB: A strict matching flag can be added in your scenarios that will force query parameters and headers to be exact 
matches: if your request includes additional query parameters or headers that are not included in the scenario, it 
will not match.*

Here is an example of scenario in JSON form:
 
```json
[
  {
    "request": {
      "method": "post",
      "headers": {
        "myHeader": "myHeaderValue"
      },
      "params": {
        "myParam": "myParamValue"
      },
      "body": ".*1.*"
    },
    "response": {
      "delay": 50,
      "code": 200,
      "media-type": "application/json",
      "headers": {
        "myHeader": "headerValue1"
      },
      "body-file": "body_content.txt"
    }
  }, {
       "response": {
         "delay": 50,
         "code": 200,
         "media-type": "application/json",
         "headers": {
           "myHeader": "headerValue2"
         },
         "body": "No body here"
       }
  }
]
```

Here is the same example in XML format:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<scenarios>
    <case>
        <request>
            <url method="post">
                <param name="myParam">myParamValue</param>
            </url>
            <header name="myHeader">myHeaderValue</header>
            <body>.*1.*</body>
        </request>
        <response delay="50" code="200" media-type="application/json">
            <header name="myHeader">headerValue1</header>
            <body file="body_content.txt" />
        </response>
    </case>
    <case>
        <response delay="50" code="200" media-type="application/json">
            <header name="myHeader">headerValue2</header>
            <body>No body here</body>
        </response>
    </case>
</scenarios>
```

In this example, a POST request on the corresponding URL, including a query param `myParam` with the value 
`myParamValue`, a header `myHeader` with the value `myHeaderValue` and a body containing the digit `1` (based on 
the regex used as body) will match the first case: it will be answered with an HTTP 200 response of type 
`application/json`, with a header `myHeader` of value `headerValue1`. The body for this response will be found in 
a nearby file named `body_content.txt`. In any other cases, the request will be answered with the second response:
a HTTP 200 response with `headerValue2` as header and a simple string `No body here` as body.

Finally, in some cases, network connections might fail with an exception instead of an HTTP error code. This kind
of scenario can also be mocked quite simply. All that is required is to replace the response section by an error 
one, as in the example below:

```json
[
  {
    "request": {
      "method": "get"
    },
    "error": {
      "type": "java.io.IOException",
      "message": "Connection was reset by server"
    }
  }
]
```
The corresponding XML version would go like this:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<scenarios>
    <case>
        <request>
            <url method="get" />
        </request>
        <error type="java.io.IOException">Connection was reset by server</error>
    </case>
</scenarios>
```
In that case, the mocker will try to instantiate an exception matching the type declared in your scenario.

## Author

[![Follow me](https://avatars.githubusercontent.com/u/17544216?s=60)](https://twitter.com/speekha)

[![Follow me](https://img.shields.io/twitter/follow/speekha?style=social)](https://twitter.com/speekha)

## Publications

[Introductory blog post on Kt. Academy](https://blog.kotlin-academy.com/httpmock-my-first-oss-library-5bae8adbccf4)

[Kotlin Weekly](https://mailchi.mp/kotlinweekly/kotlin-weekly-155) and 
[Android Weekly](https://androidweekly.net/issues/issue-371) mentioned it
