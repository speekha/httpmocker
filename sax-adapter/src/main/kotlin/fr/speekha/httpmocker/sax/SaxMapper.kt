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

import fr.speekha.httpmocker.model.Matcher
import fr.speekha.httpmocker.serialization.Mapper
import javax.xml.parsers.SAXParserFactory

class SaxMapper : Mapper {

    private val parser = SAXParserFactory.newInstance().newSAXParser()
    private val handler: ScenarioHandler = ScenarioHandler()

    override fun deserialize(payload: String): List<Matcher>? {
        parser.parse(payload.byteInputStream(), handler)
        return handler.getScenarios()
    }

    override fun serialize(matchers: List<Matcher>): String = matchers.toXml()
}
