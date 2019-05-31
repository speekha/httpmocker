package fr.speekha.httpmocker.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize

data class ResponseDescriptor
@JsonCreator constructor(

    @JsonProperty("delay")
    val delay: Long = 0,

    @JsonProperty("code")
    val code: Int = 200,

    @JsonProperty("media-type")
    val mediaType: String = "text/plain",

    @JsonProperty("headers")
    @JsonDeserialize(using = HeadersDeserializer::class)
    @JsonSerialize(using = HeadersSerializer::class)
    val headers: List<Header> = emptyList(),

    @JsonProperty("body")
    val body: String = "",

    @JsonProperty("body-file")
    val bodyFile: String? = null
)