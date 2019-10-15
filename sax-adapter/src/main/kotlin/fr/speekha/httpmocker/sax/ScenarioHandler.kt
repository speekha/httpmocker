/*
 * Copyright 2019 David Blanc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.speekha.httpmocker.sax

import fr.speekha.httpmocker.getLogger
import fr.speekha.httpmocker.model.Matcher
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.util.LinkedList

class ScenarioHandler : DefaultHandler() {

    private val logger = getLogger()

    private val buildingStack = LinkedList<Builder>()

    private lateinit var scenarios: ScenariosBuilder

    override fun startDocument() {
        scenarios = ScenariosBuilder()
    }

    override fun startElement(
        uri: String?,
        localName: String?,
        qName: String?,
        attributes: Attributes?
    ) {
        val parent = buildingStack.peek()
        try {

            val builder = when (qName) {
                "scenarios" -> scenarios
                "case" -> CaseBuilder(scenarios)
                "request" -> RequestBuilder(parent as CaseBuilder, attributes)
                "response" -> ResponseBuilder(parent as CaseBuilder, attributes)
                "error" -> ErrorBuilder(parent as CaseBuilder, attributes)
                "url" -> UrlBuilder(parent as RequestBuilder, attributes)
                "headers" -> HeadersBuilder(parent as NodeWithHeaders)
                "header" -> HeaderBuilder(parent as HeadersBuilder, attributes)
                "param" -> ParamBuilder(parent as UrlBuilder, attributes)
                "body" -> BodyBuilder(parent as NodeWithBody, attributes)
                else -> scenarios
            }
            buildingStack.push(builder)
        } catch (e: Throwable) {
            logger.error("Invalid XML", e)
            throw IllegalStateException("Invalid XML input", e)
        }
    }

    override fun endElement(uri: String?, localName: String?, qName: String?) {
        if (qName?.isNotEmpty() == true) {
            buildingStack.pop().build()
        }
    }

    override fun characters(ch: CharArray, start: Int, length: Int) {
        buildingStack.peek().addTextContent(String(ch, start, length))
    }

    fun getScenarios(): List<Matcher> = scenarios.build()
}
