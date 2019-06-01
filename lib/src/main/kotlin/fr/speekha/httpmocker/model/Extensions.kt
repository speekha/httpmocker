package fr.speekha.httpmocker.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty


data class Extensions
@JsonCreator constructor(

    @JsonProperty("mime_type")
    val mimeType: String,

    @JsonProperty("extension")
    val extension: String
)