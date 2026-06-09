/*
 *  Copyright 2019-2021 David Blanc
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package fr.speekha.httpmocker

import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

actual fun buildLogger(clazz: KClass<*>): Logger = JvmLogger(clazz)

class JvmLogger(clazz: KClass<*>) : Logger {

    private val logger = LoggerFactory.getLogger(clazz.java)

    override fun debug(message: String) {
        logger.debug(message)
    }

    override fun info(message: String) {
        logger.info(message)
    }

    override fun warn(message: String) {
        logger.warn(message)
    }

    override fun error(message: String, exception: Throwable?) {
        if (exception != null) {
            logger.error(message, exception)
        } else {
            logger.error(message)
        }
    }
}
