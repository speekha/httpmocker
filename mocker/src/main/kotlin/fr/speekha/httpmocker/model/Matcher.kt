package fr.speekha.httpmocker.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty


data class Matcher
@JsonCreator constructor(

    @JsonProperty("request")
    val request: RequestDescriptor = RequestDescriptor(),

    @JsonProperty("response")
    val response: ResponseDescriptor
)