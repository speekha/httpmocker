package fr.speekha.httpmocker.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class Header
@JsonCreator constructor(
    @JsonProperty("name")
    val name: String = "",

    @JsonProperty("value")
    var value: String = ""
)