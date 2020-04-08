package org.jetbrains.kotlin.spec.entities.testElement

import kotlinx.serialization.json.JsonObject

/**
 * Enum contains all options which could be defined in testMap.json's tests
 */
enum class TestElementKey {
    specVersion,
    casesNumber,
    description,
    path,
    unexpectedBehaviour,
    ;

    override fun toString(): String {
        return name
    }
    //todo add isPrimary flag
}

/**contains parsed info of test values declared in json test element*/
class TestElementInfo(jsonObject: JsonObject, val testNumber: Int) {
    val specVersion : String
    val casesNumber : Int
    val description : String
    var path : String
    val unexpectedBehaviour : Boolean

    private val valuesMap: MutableMap<TestElementKey, String>

    init {
        valuesMap = parseTestMap(jsonObject)

        specVersion = valuesMap[TestElementKey.specVersion]!!
        casesNumber = (valuesMap[TestElementKey.casesNumber]!!).toInt()
        description = valuesMap[TestElementKey.description]!!
        path = valuesMap[TestElementKey.path]!!
        unexpectedBehaviour = (valuesMap[TestElementKey.unexpectedBehaviour]!!).isNotEmpty()
    }

    companion object {
        /**jsonElement is a testMap.json test object*/
        private fun parseTestMap(jsonObject: JsonObject): MutableMap<TestElementKey, String> {
            val map = mutableMapOf<TestElementKey, String>()
            TestElementKey.values().forEach {
                map[it] = jsonObject[it.name]?.primitive?.content ?: ""
            }
            return map
        }
    }
}