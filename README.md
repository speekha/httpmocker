# HttpMocker

**HttpMocker** is a very lightweight Kotlin library that allows to mock HTTP calls relying on the OkHttp library.

* It can be used for unit or integration tests, so your tests can rely on predefined responses and not
execute actual calls to servers, thus avoiding the risk of unpredictable results due to network failure or 
server errors. 
* It can also be used to implement an offline mode in your application for debugging or demo purposes.

Thanks to the MockResponseInterceptor, network calls will not be dispatched to the network but read from JSON
configuration files instead. The interceptor will also allow to record scenarios so they can be reused later.


## Current Version

```gradle
httpmocker_version = '1.1.0'
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

The main dependency to add to your project is the core library:

```gradle
implementation "fr.speekha.httpmocker:mocker:1.1.0"
```

On top of that library, you will need to provide an adapter to parse the scenario files. Currently, there are three
possible options, so you can use one of the following modules based on a lib already present in your application 
(this will help you prevent duplicate libraries in your classpath, like Jackson and GSON). You can also provide 
your own implementation of the Mapper class if none of those options suit your needs.
```gradle
// Parses JSON scenarios using Jackson
implementation "fr.speekha.httpmocker:mocker-jackson-adapter:1.1.0"

// Parses JSON scenarios using Gson
implementation "fr.speekha.httpmocker:mocker-gson-adapter:1.1.0"

// Parses JSON scenarios using Moshi
implementation "fr.speekha.httpmocker:mocker-moshi-adapter:1.1.0"
```

### Proguard rules

Since most JSON parsers use some type of introspection to parse JSON sreams, it is recommended to keep the mapping 
classes unobfuscated. Depending on the adapter you choose to use, it will be fr.speekha.httpmocker.<adapter package>
where <adapter package> can be jackson, gson, moshi or moshikotlin. For instance, if you choose the Jackson parser:

```
-keep class fr.speekha.httpmocker.jackson.** { *; }
```

# Quickstart

Mocking http calls relies on a simple Interceptor : MockResponseInterceptor. All you need to set it up
is to add it to your OkHttp client. Here's an example for an Android app with minimal configuration:

```kotlin
    val interceptor = MockResponseInterceptor.Builder()
        .parseScenariosWith(mapper)
        .decodeScenarioPathWith(filingPolicy)
        .loadFileWith(context.assets::open)
        .setInterceptorStatus(ENABLED)
        .build()
    val client = OkHttpClient.Builder()
        .addInterceptor(interceptor)
        .build()
```
The interceptor's builder offer a few more options:
```kotlin
    val interceptor = MockResponseInterceptor.Builder()
        .parseScenariosWith(mapper)
        .decodeScenarioPathWith(filingPolicy)
        .loadFileWith { context.assets.open(it) }
        .setInterceptorStatus(ENABLED)
        .saveScenariosIn(File(rootFolder))
        .setInterceptorStatus(mode)
        .addFakeNetworkDelay(50L)
        .build()
```
If your interceptor is enabled, it will need to find scenarios to mock the http calls. In the previous example,
we decided to store the scenarios in the assets folder of the app (but you could also have them as resources in 
your classpath and use the classloader to access them or even store them in a certain folder and access that 
folder with any File API you're comfortable with). You also need to provide the FilePolicy you want to use: that 
policy defines which file to check to find a match for a request. A few policies are provided in the library, but
you can also define your own. An InMemoryPolicy allows to configure your scenarios entirely through code and keep
them in memory instead of reading them from an actual file on the disc.

If your interceptor is disabled, it will not interfere with actual network calls. If you choose the mixed mode, 
requests that can not be answered by a predefined scenario will actually be executed. Hence the mixed mode: responses 
can come from a scenario file or from an actual HTTP call.

Finally, the interceptor also has a recording mode. This mode allows you to record scenarios without interfering 
with your request. If you choose this mode to produce your scenarios, you will have to provide a root folder where 
the scenarios should be stored. Also, you should realize that all request and responses attributes will be recorded.
Generally, you will want to review the resulting scenarios and clean them up a bit manually. For instance, each 
request will be recorded with its HTTP method, its path, each header or parameter, its body. But all those details 
might not be very important to you. Maybe all you care about is the URL and method, in which case, you can delete 
all the superfluous criteria manually.

## Building scenarios

Answering a request is done in two steps:
- First, if the interceptor is enabled (or in mixed mode), it will try to compute a file name were the appropriate 
scenario should be stored. Based on the filing policy you choose, those files can be organized in a lot of different 
ways: all in the same folder, in separate folders matching the URL path, ignoring or not the server hostname.
- Second, once the file is found, its content will be loaded, and a more exact match will have to be found. Scenario 
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
 
 In this example, a POST request on the corresponding URL, including a query param "myParam" with the value 
 "myParamValue", a header "myHeader" with the value "myHeaderValue" and a body containing the digit '1' (based on 
 the regex used as body) will match the first case: it will be answered a HTTP 200 response of type 
 "application/json", with a header "myHeader" of value "headerValue1". The body for this response will be found in 
 a nearby file name "body_content.txt". In any other cases, the request will be answered with the second response:
 a HTTP 200 response with "headerValue2" as header and a simple string "No body here" as body.