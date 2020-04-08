package org.jetbrains.kotlin.spec.entities

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.json.JsonElement
import org.jetbrains.kotlin.spec.testArea.TestArea


class TestMapData(content: String, val testArea: TestArea) {
    val testMapJsonElement: JsonElement = Json(JsonConfiguration.Stable).parseJson(content)
}