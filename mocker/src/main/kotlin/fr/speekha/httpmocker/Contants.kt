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

package fr.speekha.httpmocker

import fr.speekha.httpmocker.model.ResponseDescriptor

const val RECORD_NOT_SUPPORTED_ERROR =
    "Recording is not supported with the current parameters."

const val NO_RECORDER_ERROR =
    "Recording configuration is not complete. Please add a Mapper."

const val NO_ROOT_FOLDER_ERROR =
    "Network calls can not be recorded without a folder where to save files. Please add a root folder."

internal val HTTP_RESPONSES_CODE: Map<Int, String> = mapOf(
    200 to "OK",
    201 to "Created",
    202 to "Accepted",
    203 to "Non-Authoritative Information",
    204 to "No Content",
    205 to "Reset Content",
    206 to "Partial Content",
    207 to "Multi-Status",
    208 to "Already Reported",
    210 to "Content Different",
    226 to "IM Used",
    300 to "Multiple Choices",
    301 to "Moved Permanently",
    302 to "Found",
    303 to "See Other",
    304 to "Not Modified",
    305 to "Use Proxy",
    306 to "Switch Proxy",
    307 to "Temporary Redirect",
    308 to "Permanent Redirect",
    310 to "Too many Redirects",
    400 to "Bad Request",
    401 to "Unauthorized",
    402 to "Payment Required",
    403 to "Forbidden",
    404 to "Not Found",
    405 to "Method Not Allowed",
    406 to "Not Acceptable",
    407 to "Proxy Authentication Required",
    408 to "Request Time-out",
    409 to "Conflict",
    410 to "Gone",
    411 to "Length Required",
    412 to "Precondition Failed",
    413 to "Request Entity Too Large",
    414 to "Request-URI Too Long",
    415 to "Unsupported Media Type",
    416 to "Requested range unsatisfiable",
    417 to "Expectation failed",
    418 to "I’m a teapot",
    421 to "Bad mapping / Misdirected Request",
    422 to "Unprocessable entity",
    423 to "Locked",
    424 to "Method failure",
    425 to "Unordered Collection",
    426 to "Upgrade Required",
    428 to "Precondition Required",
    429 to "Too Many Requests",
    431 to "Request Header Fields Too Large",
    444 to "No Response",
    449 to "Retry With",
    450 to "Blocked by Windows Parental Controls",
    451 to "Unavailable For Legal Reasons",
    456 to "Unrecoverable Error",
    495 to "SSL Certificate Error",
    496 to "SSL Certificate Required",
    497 to "HTTP Request Sent to HTTPS Port",
    498 to "Token expired/invalid",
    499 to "Client Closed Request",
    500 to "Internal Server Error",
    501 to "Not Implemented",
    502 to "Bad Gateway",
    503 to "Service unavailable",
    504 to "Gateway Time-out",
    505 to "HTTP Version not supported",
    506 to "Variant Also Negotiates",
    507 to "Insufficient storage",
    508 to "Loop detected",
    509 to "Bandwidth Limit Exceeded",
    510 to "Not extended",
    511 to "Network authentication required",
    520 to "Unknown Error",
    521 to "Web Server Is Down",
    522 to "Connection Timed Out",
    523 to "Origin Is Unreachable",
    524 to "A Timeout Occurred",
    525 to "SSL Handshake Failed",
    526 to "Invalid SSL Certificate",
    527 to "Railgun Error"
)

internal fun responseNotFound(body: String = "Page not found") =
    ResponseDescriptor(code = 404, body = body)
