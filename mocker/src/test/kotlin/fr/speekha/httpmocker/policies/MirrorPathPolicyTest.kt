package fr.speekha.httpmocker.policies

import fr.speekha.httpmocker.buildRequest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class MirrorPathPolicyTest {

    val policy: FilingPolicy = MirrorPathPolicy()

    @Test
    fun `should keep the same path as the URL`() {
        val request = buildRequest(
            "http://www.somestuff.com/test/with/path", listOf("header" to "value"), "POST", "body"
        )
        Assertions.assertEquals("test/with/path.json", policy.getPath(request))
    }
}