package fr.speekha.httpmocker.demo.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class Repo(
    val id: Long = 0,
    val name: String = "",
    val topContributor: String? = null
) {

    @JsonCreator
    constructor(
        @JsonProperty("id")
        id: Long = 0,

        @JsonProperty("name")
        name: String = ""
    ) : this(id, name, null)
}