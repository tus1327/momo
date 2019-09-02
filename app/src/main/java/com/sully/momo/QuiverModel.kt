package com.sully.momo

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.File

data class Notebook(val dir: File, val meta: Meta) {
    data class Meta(
        @SerializedName("name") val name: String,
        @SerializedName("uuid") val uuid: String
    )
}


data class Note(val dir: File) {
    val content: Content = File(dir, "content.json").readToJson(Content::class.java)
    val meta: Meta = File(dir, "meta.json").readToJson(Meta::class.java)

    data class Meta(
        @SerializedName("created_at") val createdAt: Long,
        @SerializedName("updated_at") val updatedAt: Long,
        @SerializedName("tags") val tags: List<String>,
        @SerializedName("title") val title: String,
        @SerializedName("uuid") val uuid: String
    )

    data class Content(
        @SerializedName("title") val title: String,
        @SerializedName("cells") val cells: List<Cell>
    ) {
        data class Cell(
            @SerializedName("type") val type: String,
            @SerializedName("language") val language: String,
            @SerializedName("data") val data: String
        )
    }
}

private fun <T> File.readToJson(clazz: Class<T>): T {
    val joinToString = reader().readText()
    return Gson().fromJson(joinToString, clazz)
}