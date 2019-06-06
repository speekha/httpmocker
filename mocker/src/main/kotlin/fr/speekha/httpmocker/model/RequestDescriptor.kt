package fr.speekha.httpmocker.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize


data class RequestDescriptor
@JsonCreator constructor(

    @JsonProperty("method")
    val method: String? = null,

    @JsonProperty("headers")
    @JsonDeserialize(using = HeadersDeserializer::class)
    @JsonSerialize(using = HeadersSerializer::class)
    val headers: List<Header> = emptyList(),

    @JsonProperty("params")
    val params: Map<String, String> = emptyMap(),

    @JsonProperty("body")
    val body: String? = null

)