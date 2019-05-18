package fr.speekha.httpmocker.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize


data class Matcher
@JsonCreator constructor(

    @JsonProperty("request")
    val request: RequestDescriptor = RequestDescriptor(),

    @JsonProperty("response")
    val response: ResponseDescriptor
)