package fr.speekha.httpmocker.policies

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fr.speekha.httpmocker.model.Matcher
import okhttp3.Request
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream

/**
 * A filing policy allowing to configure requests programmatically. It can be used with a
 * @see fr.speekha.httpmocker.MockResponseInterceptor like this:
 * <pre>{@code
 * val policy = InMemoryPolicy()
 * val interceptor = MockResponseInterceptor(policy, policy::matchRequest)
 * }</pre>
 */
class InMemoryPolicy : FilingPolicy {

    private val matchers = mutableMapOf<String, List<Matcher>>()

    private val mapper = jacksonObjectMapper()

    override fun getPath(request: Request): String = request.url().toString()

    /**
     * Adds a new rule for a certain URL
     * @param url the URL being matched by this rule
     * @param matcher the detailed request and response included in this rule
     */
    fun addMatcher(url: String, matcher: Matcher) {
        matchers[url] = (matchers[url] ?: listOf()) + matcher
    }

    /**
     * The loading function needed by the interceptor
     */
    fun matchRequest(url: String): InputStream? = matchers[url]?.let {
        PipedInputStream().apply {
            val pipeOut = PipedOutputStream()
            pipeOut.connect(this)
            mapper.writeValue(pipeOut, it)
        }
    }
}