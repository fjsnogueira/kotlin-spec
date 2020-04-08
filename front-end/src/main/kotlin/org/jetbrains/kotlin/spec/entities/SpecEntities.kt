package org.jetbrains.kotlin.spec.entities

import org.jetbrains.kotlin.spec.entities.testElement.TestElementInfo
import org.jetbrains.kotlin.spec.testArea.TestArea

interface TestSet {

    val loadedTestAreas: MutableMap<TestArea, List<Test>>
}

//todo make loadedTestAreas private
class SectionTestSet(override val loadedTestAreas: MutableMap<TestArea, List<Test>>) : TestSet {


}

class ParagraphTestSet(sectionTestSet: SectionTestSet, val paragraphNumber: Int) : TestSet {
    override val loadedTestAreas: MutableMap<TestArea, List<Test>> = mutableMapOf()

    init {
        sectionTestSet.loadedTestAreas.forEach { map ->
            loadedTestAreas[map.key] = map.value.filter { test -> test.testPath.paragraphNumber == paragraphNumber.toString() }
        }
    }
}


class SentenceTestSet(paragraphTestSet: ParagraphTestSet, sentenceNumber: Int) : TestSet {
    override val loadedTestAreas: MutableMap<TestArea, List<Test>> = mutableMapOf()

    init {
        paragraphTestSet.loadedTestAreas.forEach { map ->
            loadedTestAreas[map.key] = map.value.filter { test -> test.testPath.sentenceNumber == sentenceNumber.toString() }
        }
    }

    fun getTestsByTestArea(testAreaString: String): List<Test>? {
        loadedTestAreas.forEach { map ->
            if (map.key.attributeValue == testAreaString)
                return if (map.value.isEmpty())
                    null else map.value
        }
        return null
    }

    fun getTestsByTestType(testAreaString: String, testTypeShortName: String): List<Test>? {
        val result = getTestsByTestArea(testAreaString)?.filter { test -> test.testPath.testType.shortName == testTypeShortName }
        return when {
            result == null -> null
            result.isEmpty() -> null
            else -> result
        }
    }

    fun getTestsByTestNumber(testAreaString: String, testTypeShortName: String, testNumber: String): Test? {
        getTestsByTestType(testAreaString, testTypeShortName)?.forEach { test ->
            if (test.testElementInfo.testNumber.toString() == testNumber)
                return test
        }
        return null
    }
}

class Test(val testElementInfo: TestElementInfo, val testContainer: TestContainer, val testPath: TestPath)

class TestCase(val code: String, infoElements: Map<String, String?>? =null)


class TestContainer(content: String, testArea: TestArea){
    val testCases: List<TestCase> = testArea.testCasesParser(content)
}