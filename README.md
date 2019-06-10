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
// latest
httpmocker_version = '1.0.0-SNAPSHOT'
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
// Add Jcenter to your repositories if needed
repositories {
    maven { url 'https://oss.jfrog.org/artifactory/oss-snapshot-local' }
}
```

### Dependencies

```gradle
implementation "fr.speekha.httpmocker:mocker:1.0.0-SNAPSHOT"
```

# Quickstart

TODO