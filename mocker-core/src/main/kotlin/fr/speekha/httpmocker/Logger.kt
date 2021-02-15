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

class Logger constructor(clazz: KClass<*>) {

    private val logger = LoggerFactory.getLogger(clazz.java)

    fun debug(message: String) {
        logger.debug(message)
    }

    fun info(message: String) {
        logger.info(message)
    }

    fun warn(message: String) {
        logger.warn(message)
    }

    fun error(message: String, exception: Throwable? = null) {
        if (exception != null) {
            logger.error(message, exception)
        } else {
            logger.error(message)
        }
    }
}

inline fun <reified T : Any> T.getLogger(): Logger = Logger(this::class)
