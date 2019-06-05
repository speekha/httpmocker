package fr.speekha.httpmocker.policies

import fr.speekha.httpmocker.buildRequest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class SingleFolderPolicyTest {


    @Test
    fun `should store configuration files in a single folder`() {
        val policy: FilingPolicy = SingleFolderPolicy("folder")
        val request = buildRequest(
            "http://www.somestuff.com/test/with/path", listOf("header" to "value"), "POST", "body"
        )
        Assertions.assertEquals("folder/test_with_path.json", policy.getPath(request))
    }

    @Test
    fun `should handle empty root folder`() {
        val policy: FilingPolicy = SingleFolderPolicy("")
        val request = buildRequest(
            "http://www.somestuff.com/test/with/path", listOf("header" to "value"), "POST", "body"
        )
        Assertions.assertEquals("test_with_path.json", policy.getPath(request))
    }


}