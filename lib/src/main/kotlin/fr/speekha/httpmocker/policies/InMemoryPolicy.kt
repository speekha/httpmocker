package fr.speekha.httpmocker.policies

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fr.speekha.httpmocker.model.Matcher
import okhttp3.Request
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream

class InMemoryPolicy : FilingPolicy {

    private val matchers = mutableMapOf<String, List<Matcher>>()

    private val mapper = jacksonObjectMapper()

    override fun getPath(request: Request): String = request.url().toString()

    fun addMatcher(url: String, matcher: Matcher) {
        matchers[url] = (matchers[url] ?: listOf()) + matcher
    }

    fun matchRequest(url: String): InputStream? = matchers[url]?.let {
        PipedInputStream().apply {
            val pipeOut = PipedOutputStream()
            pipeOut.connect(this)
            mapper.writeValue(pipeOut, it)
        }
    }
}