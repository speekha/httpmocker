# HttpMocker

HttpMocker is a kotlin library that allows to mock http calls in an app using OkHttp.
Thanks to the MockResponseInterceptor, network calls will not be dispatched to the network be read from JSON
configuration files instead. The interceptor also allows to record scenarios to they can be reused later.