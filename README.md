# HttpMocker

[![Kotlin](https://img.shields.io/badge/kotlin-1.4.10-blue.svg)](http://kotlinlang.org)
[![CircleCI](https://circleci.com/gh/speekha/httpmocker/tree/develop.svg?style=shield)](https://circleci.com/gh/speekha/httpmocker/tree/develop)
[![Download](https://api.bintray.com/packages/speekha/httpmocker/mocker/images/download.svg)](https://bintray.com/speekha/httpmocker/mocker/_latestVersion)

[![codebeat badge](https://codebeat.co/badges/a361e616-a1a0-4e85-b6a7-2bc82f31f7ac)](https://codebeat.co/projects/github-com-speekha-httpmocker-develop)
[![CodeFactor](https://www.codefactor.io/repository/github/speekha/httpmocker/badge)](https://www.codefactor.io/repository/github/speekha/httpmocker)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/754e2a65060a48c9bfc580a36063d206)](https://www.codacy.com/app/speekha/httpmocker)
[![Codacy Badge](https://api.codacy.com/project/badge/Coverage/754e2a65060a48c9bfc580a36063d206)](https://www.codacy.com/app/speekha/httpmocker)

**HttpMocker** is a very lightweight Kotlin library that allows to mock HTTP calls relying on the OkHttp library.

* It can be used for unit or integration tests, so your tests can rely on predefined responses and not
execute actual calls to servers, thus avoiding the risk of unpredictable results due to network failure or 
server errors. 

* It can also be used to implement an offline mode in your application for debugging or demo purposes.

Thanks to the MockResponseInterceptor, network calls will not be dispatched to the network but read from JSON
configuration files or computed dynamically instead. The interceptor will also allow to record scenarios so they
can be reused later.

## Current Version

```gradle
httpmocker_version = '1.3.0'
```

## Gradle 

### Jcenter 

For stable versions, check that you have the `jcenter` repository. 

```gradle
// Add Jcenter to your repositories if needed
repositories {
	jcenter()    
}
```

### OJO

For Snapshot versions, check that you have the `OJO` snapshot repository. 

```gradle
// Add oss.jfrog.org to your repositories if needed
repositories {
    maven { url 'https://oss.jfrog.org/artifactory/oss-snapshot-local' }
}
```

### Dependencies

#### Adding HttpMocker

This library contains two parts: a core module handling the mock logic, and an additional adapter to parse the scenario 
files for static mocks. Currently, there are six possible options that are provided for parsing, based on some of the 
most commonly used serialization libraries and on a custom implementations (no third party dependency): 
* Jackson
* Gson
* Moshi
* Kotlinx serialization)
* Custom JSON implementation
* Custom Sax-based implementation
This should allow you to choose one matching what you already use in your application (in order to prevent 
duplicate libraries in your classpath, like Jackson and GSON). If you would prefer to use XML instead 
of JSON, the SAX-based parser allows to do it. If you choose one of these options, all you need to add is the 
corresponding `implementation` line in your gradle file:

```gradle
// Parses JSON scenarios using Jackson
implementation "fr.speekha.httpmocker:jackson-adapter:1.2.0"

// Parses JSON scenarios using Gson
implementation "fr.speekha.httpmocker:gson-adapter:1.2.0"

// Parses JSON scenarios using Moshi
implementation "fr.speekha.httpmocker:moshi-adapter:1.2.0"

// Parses JSON scenarios using Kotlinx Serialization
implementation "fr.speekha.httpmocker:kotlinx-adapter:1.2.0"

// Parses JSON scenarios using a custom JSON parser
implementation "fr.speekha.httpmocker:custom-adapter:1.2.0"

// Parses XML scenarios using a custom SAX parser
implementation "fr.speekha.httpmocker:sax-adapter:1.2.0"
```

If none of those options suit your needs or if you would prefer to only use dynamic mocks, you can add 
the main dependency to your project (using static mocks will require that you provide your own implementation 
of the `Mapper` class):

```gradle
implementation "fr.speekha.httpmocker:mocker:1.2.0"
```

#### External dependencies

* HttpMocker is a mocking library for OkHttp connections, so it depends on OkHttp 3.14.7.
* It also depends on the SLF4J API for logging.
* JSON parsers depend on their respective external libraries: Jackson 2.10.3, Gson 2.8.6, Moshi 
1.9.2 or KotlinX serialization 0.20.0.

### Proguard rules

Since Jackson, Gson or Kotlinx serialization use some type of introspection or annotation 
processing to parse JSON streams, it is recommended to keep the mapping classes unobfuscated. 
You can refer to the [proguard-rules.pro](demo/proguard-rules.pro) file included in the demo app 
for an example of required rules.

The custom and moshi parsers are immune to obfuscation because they do not use any introspection.

## Quickstart

Mocking http calls relies on a simple Interceptor: MockResponseInterceptor. All you need to set it up
is to add it to your OkHttp client. Here's an example with minimal configuration of dynamic mocks
using the Java-friendly builder syntax:
```kotlin
    val interceptor = Builder()
        .useDynamicMocks{
            ResponseDescriptor(code = 200, body = "Fake response body")
        }
        .setInterceptorStatus(ENABLED)
        .build()
    val client = OkHttpClient.Builder()
        .addInterceptor(interceptor)
        .build()
```

You can write the same thing in a more Kotlin-friendly style:
```kotlin
    val interceptor = mockInterceptor {
        useDynamicMocks{
            ResponseDescriptor(code = 200, body = "Fake response body")
        }
        setInterceptorStatus(ENABLED)
    }
    val client = OkHttpClient.Builder()
        .addInterceptor(interceptor)
        .build()
```

If your interceptor is disabled, it will not interfere with actual network calls. If it is enabled, 
it will need to find scenarios to mock the HTTP calls. Dynamic mocks imply that you have to 
provide the response for each request programmatically, which allows you to define stateful 
responses (identical calls could lead to different answers based on what the user did in between 
these calls). The response can be provided as a `ResponseDescriptor` by implementing the 
`RequestCallback` interface, or you can simply provide a lambda function to do the computation. 

NB: If you use dynamic mocks, the `bodyFile` attribute of your `ResponseDescriptor` is not needed (it 
will be ignored). Its only use is for static scenarios that could save the response body in a separate 
file.

Another option is to use static mocks. Static mocks are scenarios stored as static files. Here is 
an example for an Android app using static mocks, with a few more options:
```kotlin
    val interceptor = mockInterceptor {
            parseScenariosWith(mapper)
            decodeScenarioPathWith(filingPolicy)
            loadFileWith { 
                context.assets.open(it) 
            }
            setInterceptorStatus(ENABLED)
            saveScenariosIn(File(rootFolder))
            addFakeNetworkDelay(50L)
        }
```

In this example, we decided to store the scenarios in the assets folder of the app (but you 
could also have them as resources in your classpath and use the `Classloader` to access them or 
even store them in a certain folder and access that folder with any File API you're comfortable 
with). You also need to provide the `FilingPolicy` you want to use: that policy defines which file to 
check to find a match for a request. A few policies are provided in the library, but you can also 
define your own. 

Additionally, you need to provide a Mapper to parse the scenario files. Scenarios can be stored as 
JSON or XML depending on your preference, or any other format, for that matter,
as long as you provide your own `Mapper` class to serialize and deserialize the business objects. As 
far as this lib is concerned though, a few mappers are available out of the box to handle JSON or 
XML formats. The main mappers are based on Jackson, Gson, Moshi and Kotlinx serialization. They 
are provided in specific modules so you can choose one based on the library you might already use 
in your application, thus limiting the risk for duplicate libraries serving the same purpose in 
your app. Two additional implementations, based on a custom JSON or XML parsers that do not need 
any other dependencies are also available. 

Static and dynamic scenarios can be used together. Several dynamic callbacks can be added to the 
interceptor, but only one static configuration is allowed. Dynamic callbacks will be used first to 
find a suitable mock, and if none is found, the static configuration will be tested next.

If you choose the mixed mode, requests that can not be answered by a predefined scenario will 
actually be executed. Hence the mixed mode: responses can come from a scenario file (or dynamic 
mock) or from an actual HTTP call.

Finally, the interceptor also has a recording mode. This mode allows you to record scenarios 
without interfering with your request. If you choose this mode to produce your static scenarios, 
you will have to provide a root folder where the scenarios should be stored. Also, you should 
realize that all request and responses attributes will be recorded. Generally, you will want to 
review the resulting scenarios and clean them up a bit manually. For instance, each request will be 
recorded with its HTTP method, its path, each header or parameter, its body. But all those details 
might not be very important to you. Maybe all you care about is the URL and method, in which case, 
you can delete all the superfluous criteria manually.

## Building static scenarios

Answering a request with a static mock is done in two steps:
* First, if the interceptor is enabled (or in mixed mode), it will try to compute a file name were the appropriate 
scenario should be stored. Based on the filing policy you chose, those files can be organized in a lot of different 
ways: all in the same folder, in separate folders matching the URL path, ignoring or not the server hostname.

* Second, once the file is found, its content will be loaded, and a more exact match will have to be found. Scenario 
files contain a list of "Matchers", that is a list of request patterns and corresponding responses. Based on the 
request it is trying to answer, the interceptor is going to scan through all the request declarations and stop as soon
as it finds one that matches the situation.

When writing a request pattern, the elements included are supposed to be found in the requests to match. The more 
elements, the more precise the match has to be. The less elements, the more permissive the match. A request can even 
be omitted altogether (in this case, all requests match):
* When specifying a method, matching request have to use the same HTTP method.
* When specifying query parameters, matching requests must have at least all these parameters (but can have more).
* When specifying headers, matching request must have all the same headers (but can have more).

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

In this example, a POST request on the corresponding URL, including a query param "myParam" with the value 
"myParamValue", a header "myHeader" with the value "myHeaderValue" and a body containing the digit '1' (based on 
the regex used as body) will match the first case: it will be answered a HTTP 200 response of type 
"application/json", with a header "myHeader" of value "headerValue1". The body for this response will be found in 
a nearby file name "body_content.txt". In any other cases, the request will be answered with the second response:
a HTTP 200 response with "headerValue2" as header and a simple string "No body here" as body.

Finally, in some cases, network connections might fail with an exception instead of a HTTP error code. This kind
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

## Author

[![Follow me](https://avatars.githubusercontent.com/u/17544216?s=60)](https://twitter.com/speekha)

[![Follow me](https://img.shields.io/twitter/follow/speekha?style=social)](https://twitter.com/speekha)

## Publications

[Introductory blog post on Kt. Academy](https://blog.kotlin-academy.com/httpmock-my-first-oss-library-5bae8adbccf4)

[Kotlin Weekly](https://mailchi.mp/kotlinweekly/kotlin-weekly-155) and 
[Android Weekly](https://androidweekly.net/issues/issue-371) mentioned it
