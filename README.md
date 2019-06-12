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
httpmocker_version = '1.0.0'
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

```gradle
implementation "fr.speekha.httpmocker:mocker:1.0.0"
```

# Quickstart

Mocking http calls relies on a simple Interceptor : MockResponseInterceptor. All you need to set it up
is to add it to your OkHttp client. Here's an example for an Android app:

```kotlin
    val interceptor = MockResponseInterceptor(MirrorPathPolicy()) { 
            context.assets.open(it) 
        }
    val client = OkHttpClient.Builder()
                             .addInterceptor(interceptor)
                             .build()
```
Once your interceptor is set up, you can decide to enable it or disable it by changing its state:
```kotlin
    interceptor.mode = ENABLED
```
If your interceptor is enabled, it will need to find scenarios to mock the http calls. In the previous example,
we decided to store the scenarios in the assets folder of the app (but you could also have them as resources in 
your classpath and use the classloader to access them or even store them in a certain folder and access that 
folder with any File API you're comfortable with). You also need to provide the FilePolicy you want to use: that 
policy defines which file to check to find a match for a request. A few policies are provided in the library, but
you can also define your own.

If your interceptor is disabled, it will not interfere with actual network calls. If you choose the mixed mode, 
requests that can be answered by a predefined scenario will actually be executed. Hence the mixed mode: response 
can come from a scenario file or from an actual HTTP call.

Finally, the interceptor also has a recording mode. This mode allows you to record scenarios without interfering 
with your request. If you choose this mode to produce your scenarios, you will have to provide a root folder where 
the scenarios should be stored. Also, you should realize that all request and responses attributes will be recorded.
Generally, you will want to review the resulting scenarios and clean them up a bit manually. For instance, each 
request will be recorded with its HTTP method, its path, each header or parameter, its body. But all those details 
might not be very important to you. Maybe all you care about is the URL and method, in which case, you can delete 
all the superfluous criteria manually.