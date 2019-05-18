package fr.speekha.httpmocker.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize

data class ResponseDescriptor
@JsonCreator constructor(

    @JsonProperty("code")
    val code: Int = 200,

    @JsonProperty("media-type")
    val mediaType: String = "text/plain",

    @JsonProperty("headers")
    @JsonDeserialize(using = HeadersDeserializer::class)
    val headers: List<Header> = emptyList(),

    @JsonProperty("body")
    val body: String = ""
)