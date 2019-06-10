package fr.speekha.httpmocker.policies

import fr.speekha.httpmocker.buildRequest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ServerSpecificPolicyTest {

    @Test
    fun `should include URL host in path`() {
        val policy: FilingPolicy = ServerSpecificPolicy()
        val request = buildRequest(
            "http://www.somestuff.com/test/with/path", listOf("header" to "value"), "POST", "body"
        )
        Assertions.assertEquals("www.somestuff.com/test/with/path.json", policy.getPath(request))
    }
}