package fr.speekha.httpmocker.demo.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class Repo
@JsonCreator
constructor(
    @JsonProperty("id")
    val id: Long = 0,

    @JsonProperty("name")
    val name: String = ""
)