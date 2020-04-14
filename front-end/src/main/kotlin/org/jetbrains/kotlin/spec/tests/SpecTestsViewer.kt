package org.jetbrains.kotlin.spec.tests

import js.externals.jquery.JQuery
import js.externals.jquery.`$`
import org.jetbrains.kotlin.spec.entities.SentenceTestSet
import org.jetbrains.kotlin.spec.entities.Test
import org.jetbrains.kotlin.spec.entities.TestCase
import org.jetbrains.kotlin.spec.utils.*
import kotlin.js.Promise
import kotlin.js.json

external fun require(module: String): dynamic

val KotlinPlayground = require("kotlin-playground")

enum class NavigationType { PREV, NEXT }

class SpecTestsViewer {
    companion object {
        private const val TESTS_VIEWER_SELECTOR = ".test-coverage-view"
        private const val TEST_AREA_SELECTOR = "$TESTS_VIEWER_SELECTOR select[name='test-area']"
        private const val TEST_AREA_OPTION_SELECTOR = "$TEST_AREA_SELECTOR option"
        private const val TEST_TYPE_SELECTOR = "$TESTS_VIEWER_SELECTOR select[name='test-type']"
        private const val TEST_TYPE_OPTION_SELECTOR = "$TEST_TYPE_SELECTOR option"
        private const val TEST_TYPE_OPTION_TEMPLATE = "<option value='{1}'>{2}</option>"
        private const val TEST_NUMBER_SELECTOR = "$TESTS_VIEWER_SELECTOR select[name='test-number']"
        private const val TEST_NUMBER_OPTION_SELECTOR = "$TEST_NUMBER_SELECTOR option"
        private const val TEST_NUMBER_OPTION_TEMPLATE = "<option value='{1}'>{1}: {2}</option>"
        private const val TEST_CODE_WRAPPER_SELECTOR = ".test-code-wrapper"
        private const val TEST_CODE_SELECTOR = "$TESTS_VIEWER_SELECTOR .test-code"
        private const val TEST_CODE_TEMPLATE = "<div class='test-code'>{1}</div>"
        private const val TEST_CASE_INFO_SELECTOR = ".test-case-info"
        private const val TEST_VIEWER_BODY_TEMPLATE = """
                <div class='test-case-info'>
                    <div class='test-title' style='text-align: center;margin: 10px 0;'><b>{1}</b></div>
                    <div style='text-align: center;font-size: 14px;margin-top: 5px;'>
                        <a href='#' class='prev-testcase disabled'>Prev testcase</a> | Test case #<b class='testcase-number'>1</b> | <a href='#' class='next-testcase'>Next testcase</a>
                    </div>
                    <div class='test-code-wrapper' style='margin-top: 15px;'></div>
                </div>
            """
        private const val NEXT_TESTCASE_SELECTOR = ".next-testcase"
        private const val PREV_TESTCASE_SELECTOR = ".prev-testcase"
        private const val TESTCASE_NUMBER_SELECTOR = ".testcase-number"

        private const val MAIN_FUN_CODE = "\nfun main() { println(\"Test passed\") }"
        private const val SAMPLE_WRAP_CODE = "//sampleStart\n{1}\n//sampleEnd"
    }

    private lateinit var currentSentenceTests: SentenceTestSet
    private lateinit var testPopup: Popup

    private fun insertCode(testCaseCode: TestCase, helperFilesContent: List<Map<String, Any>>? = null) {
        val code = StringBuilder()

        helperFilesContent?.forEach { helperFile ->
           //todo fix code.append("${helperFile[TestAreaEnum.DIAGNOSTICS.content]}\n\n")
        }

        code.append(SAMPLE_WRAP_CODE.format(testCaseCode.code ?: return))
        code.append(MAIN_FUN_CODE)

        `$`(TEST_CODE_WRAPPER_SELECTOR).html(TEST_CODE_TEMPLATE.format(code.toString().escapeHtml()))

        KotlinPlayground(TEST_CODE_SELECTOR, json(
                "callback" to { testPopup.computeSizes() },
                "onChange" to { testPopup.computeSizes() }
        ))
    }

    private fun showTestCaseCode(test: Test, helperFilesContent: List<Map<String, Any>>? = null) {
        `$`(TEST_CASE_INFO_SELECTOR).remove()
        `$`(TESTS_VIEWER_SELECTOR).append(TEST_VIEWER_BODY_TEMPLATE.format(test.testElementInfo.description))

        val testCases = test.testContainer.testCases

        insertCode(testCases.first(), helperFilesContent)
        // insertCode(testCases[0], helperFilesContent)

        if (testCases.size == 1) {
            `$`(NEXT_TESTCASE_SELECTOR).addClass("disabled")
        }
    }

    private fun getHelpers(helperNames: List<String>): Promise<Array<out Map<String, Any>>> {
        val helperFilesPromises = mutableListOf<Promise<Map<String, Any>>>()

//        helperNames.forEach { helperName ->
//            helperFilesPromises.add(SpecTestsLoader.loadHelperFile(helperName))
//        } todo fix

        return Promise.all(helperFilesPromises.toTypedArray())
    }

    fun showViewer(sentenceElement: JQuery) {
        println("!!!!!!!showViewer!!!!!!")
        val tests =
                sentenceElement.data("tests").unsafeCast<SentenceTestSet>()

//        tests.loadedTestAreas.forEach { map ->
//            println("map.key.path " + map.key.path + "  map.value.size" + map.value.size)
//        }

        val sentenceText = "sentence {1}".format(sentenceElement.clone().children(".number-info").text())

        currentSentenceTests = tests
        testPopup = Popup(
                PopupConfig(
                        title = "Test coverage of $sentenceText",
                        content = TestsCoverageColorsArranger.TEMPLATE,
                        width = 800,
                        height = 300
                )
        ).apply { open() }

        `$`(TEST_AREA_OPTION_SELECTOR).each { _, el ->
            val testArea = `$`(el)


            if (tests.getTestsByTestArea(testArea.attr("value")) == null) {
                testArea.remove()
            }
        }

        `$`(TEST_AREA_SELECTOR).`val`(`$`(TEST_AREA_OPTION_SELECTOR).eq(0).`val`().toString())

        onTestAreaChange()
    }

    /*
    testArea = codegen/box or diagnostics
    * */
    fun onTestAreaChange() {
        println("!!!onTestAreaChange!!! ")
        val testArea = `$`(TEST_AREA_SELECTOR).`val`().toString().apply { if (isEmpty()) return }
        println("!!!testArea = " + testArea)
        val tests = currentSentenceTests.getTestsByTestArea(testArea) ?: return
        val testTypes = mapOf("pos" to "Positive", "neg" to "Negative")

        //TODO: fix appends if test type option is not appended yet
        tests.forEach { test ->
            `$`(TEST_TYPE_SELECTOR).append(TEST_TYPE_OPTION_TEMPLATE.format(test.testPath.testType.shortName, test.testPath.testType.name
                    ?: return@forEach))
        }

        `$`(TEST_TYPE_SELECTOR).show().`val`(`$`(TEST_TYPE_OPTION_SELECTOR).eq(0).`val`().toString())

        onTestTypeChange()
    }

    fun onTestTypeChange() {
        val testType = `$`(TEST_TYPE_SELECTOR).`val`().toString().apply { if (isEmpty()) return }
        val testArea = `$`(TEST_AREA_SELECTOR).`val`().toString()

        println("!!!onTestTypeChange!!! ")
        println("!!!testType = " + testType)
        println("!!!testArea = " + testArea)
        val tests = currentSentenceTests.getTestsByTestType(testArea, testType) ?: return

        `$`(TEST_NUMBER_SELECTOR).empty()
        tests.forEach { test ->
            `$`(TEST_NUMBER_SELECTOR).append(TEST_NUMBER_OPTION_TEMPLATE.format(test.testElementInfo.testNumber, test.testElementInfo.description))
        }

        `$`(TEST_NUMBER_SELECTOR).show().`val`(`$`(TEST_NUMBER_OPTION_SELECTOR).eq(0).`val`().toString())

        onTestNumberChange()
    }

    fun onTestNumberChange() {
        val testNumber = `$`(TEST_NUMBER_SELECTOR).`val`().toString().apply { if (isEmpty()) return }
        val testArea = `$`(TEST_AREA_SELECTOR).`val`().toString()
        val testType = `$`(TEST_TYPE_SELECTOR).`val`().toString()

        println("!!!onTestNumberChange!!! ")
        println("!!!testType = " + testType)
        println("!!!testArea = " + testArea)
        println("!!!testNumber = " + testNumber)

        val tests = currentSentenceTests.getTestsByTestNumber(testArea, testType, testNumber) ?: return

        /* if (tests["helpers"] != null) {
             getHelpers(tests["helpers"].unsafeCast<List<String>>()).then {
                 showTestCaseCode(tests, it.toList())
             } todo
         } else {
             showTestCaseCode(tests)
         }*/

        showTestCaseCode(tests)

    }

    fun navigateTestCase(navigationLink: JQuery, navigationType: NavigationType) {
        if (navigationLink.hasClass("disabled")) return

        val testArea = `$`(TEST_AREA_SELECTOR).`val`().toString()
        val testType = `$`(TEST_TYPE_SELECTOR).`val`().toString()
        val testNumber = `$`(TEST_NUMBER_SELECTOR).`val`().toString()
        val tests = currentSentenceTests.getTestsByTestNumber(testArea,testType,testNumber) ?: return
        val currentNumber = `$`(TESTCASE_NUMBER_SELECTOR).text().toInt()
        val targetTestCase = if (navigationType == NavigationType.PREV) currentNumber - 2 else currentNumber
        val testCases = tests.testContainer.testCases

        `$`(TESTCASE_NUMBER_SELECTOR).html((targetTestCase + 1).toString())

//        if (tests["helpers"] != null) {
//            getHelpers(tests["helpers"].unsafeCast<List<String>>()).then {
//                insertCode(testCases[targetTestCase], it.toList())
//            } //todo
//        } else {
//            insertCode(testCases[targetTestCase])
//        }

        insertCode(testCases [targetTestCase])

        if (targetTestCase + 1 >= testCases.size) {
            `$`(NEXT_TESTCASE_SELECTOR).addClass("disabled")
        } else if (navigationType == NavigationType.PREV && `$`(NEXT_TESTCASE_SELECTOR).hasClass("disabled")) {
            `$`(NEXT_TESTCASE_SELECTOR).removeClass("disabled")
        }
        if (navigationType == NavigationType.NEXT && `$`(PREV_TESTCASE_SELECTOR).hasClass("disabled")) {
            `$`(PREV_TESTCASE_SELECTOR).removeClass("disabled")
        } else if (targetTestCase + 1 == 1) {
            `$`(PREV_TESTCASE_SELECTOR).addClass("disabled")
        }
    }
}
