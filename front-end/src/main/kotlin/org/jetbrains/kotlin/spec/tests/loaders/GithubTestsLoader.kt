package org.jetbrains.kotlin.spec.tests.loaders

import js.externals.jquery.JQueryAjaxSettings
import js.externals.jquery.JQueryPromise
import js.externals.jquery.JQueryXHR
import js.externals.jquery.`$`
import org.jetbrains.kotlin.spec.entities.Test
import org.jetbrains.kotlin.spec.entities.TestContainer
import org.jetbrains.kotlin.spec.entities.TestMapData
import org.jetbrains.kotlin.spec.entities.TestPath
import org.jetbrains.kotlin.spec.entities.testElement.TestElementInfo
import org.jetbrains.kotlin.spec.testArea.TestArea
import org.jetbrains.kotlin.spec.utils.format
import kotlin.browser.window
import kotlin.js.Promise

interface GithubTestsLoader {
    enum class TestFileType { SPEC_TEST, IMPLEMENTATION_TEST }

    companion object {
        private const val RAW_GITHUB_URL = "https://raw.githubusercontent.com/JetBrains/kotlin"

        const val SPEC_TEST_DATA_PATH = "compiler/tests-spec/testData"
        const val LINKED_SPEC_TESTS_FOLDER = "linked"

        const val DEFAULT_BRANCH = "spec-tests"

        fun getBranch() = window.localStorage.getItem("spec-tests-branch") ?: DEFAULT_BRANCH

        fun loadTestFileFromRawGithub(
                path: String,
                testPathInSpec: String? = null,
                customFolder: String? = null,
                testElementInfo: TestElementInfo,
                testPath: TestPath,
                testArea: TestArea
        ): Promise<Test> {
            println("!!loadTestFileFromRawGithub")

            return Promise { requestResolve, requestReject ->
                val testContainerMap = mutableMapOf<String, String>()

                val testsPaths = TestsPaths(customFolder, path, testPathInSpec) //todo get rid of ?

                val queryForTestArea: JQueryPromise<Unit> = getQueryForTest(testContainerMap, requestReject, testsPaths)

                `$`.`when`(queryForTestArea).then({ _: Any?, _: Any ->
                    val testContainer = TestContainer(
                            content = testContainerMap["content"]!!,
                            testArea = testArea)
//                    println("testPath"+testPath.toString())
                    requestResolve(Test(testElementInfo, testContainer, testPath))
                })
            }
        }

        fun loadTestMapFileFromRawGithub(
                path: String,
                testPathInSpec: String? = null,
                testType: TestFileType,
                customFolder: String? = null,
                testAreasToLoad: ArrayList<TestArea>
        ): Promise<MutableMap<TestArea, TestMapData>> {
            return Promise { requestResolve, requestReject ->
                val resultMap = mutableMapOf<TestArea, TestMapData>()

                val testsPaths = TestsPaths(customFolder, path, testPathInSpec)

                val querySet = mutableSetOf<JQueryPromise<Unit>>()
                testAreasToLoad.forEach { testArea ->
                    querySet.add(
                            getQueryForTestMap(testArea, resultMap, testType, requestReject, testsPaths)
                    )
                }

                val queryArray = querySet.toTypedArray()
                `$`.`when`(*queryArray)
                        .then({ _: Any?, _: Any ->
                            //     println("loadTestMapFileFromRawGithub succeed resultMap.size=" + resultMap.size)
                            requestResolve(resultMap)
                        }, {
                            //  println("loadTestMapFileFromRawGithub fail resultMap.size=" + resultMap.size)
                            requestResolve(resultMap)
                        })
            }
        }


        class TestsPaths(
                val customFolder: String?,
                val path: String, val testPathInSpec: String?
        )

        private fun getFullPath(testFileType: TestFileType, testArea: TestArea, customFolder: String?, path: String) =
                when (testFileType) {
                    TestFileType.SPEC_TEST -> "{1}/{2}/{3}/{4}/{5}/{6}"
                            .format(RAW_GITHUB_URL, getBranch(), SPEC_TEST_DATA_PATH, testArea.path, customFolder
                                    ?: LINKED_SPEC_TESTS_FOLDER, path)
                    TestFileType.IMPLEMENTATION_TEST -> "{1}/{2}/{3}".format(RAW_GITHUB_URL, getBranch(), path)
                }

        private fun getFullPathForTest(path: String) = "{1}/{2}/{3}".format(RAW_GITHUB_URL, getBranch(), path)

        private fun getQueryForTestMap(
                testArea: TestArea,
                resultMap: MutableMap<TestArea, TestMapData>,
                testType: TestFileType,
                requestReject: (Throwable) -> Unit,
                testsPaths: TestsPaths
        ): JQueryPromise<Unit> {


            return `$`.ajax(getFullPath(testType, testArea, testsPaths.customFolder, testsPaths.path),
                    jQueryAjaxSettings { /*requestReject(Exception())*/ }).then(
                    doneFilter = { response: Any?, _: Any ->
                        //println("content:-->" + response.toString())
                        println("-------------")
                        //println("content Area :-->" + testArea.path)
                        // println("contentPath:-->" + (testsPaths.testPathInSpec ?: testsPaths.path))
                        // println("fullPath:-->" + getFullPath(testType, testArea, testsPaths.customFolder, testsPaths.path))
                        val testMapData = TestMapData(content = response.toString(), testArea = testArea)
                        resultMap[testArea] = testMapData
                        //  println("getQueryForTestMap: resultMap.size =" + resultMap.size + " testArea=" + testArea.path)

                    }
            )
        }

        private fun getQueryForTest(
                testContainerMap: MutableMap<String, String>,
                requestReject: (Throwable) -> Unit,
                testsPaths: TestsPaths
        ) = `$`.ajax(getFullPathForTest(testsPaths.path),
                jQueryAjaxSettings { requestReject(Exception()) }).then(
                { response: Any?, reject: Any ->
                    testContainerMap["content"] = response.toString()
                    testContainerMap["contentPath"] = (testsPaths.testPathInSpec ?: testsPaths.path)
                },
                { }
        )

        private fun jQueryAjaxSettings(requestReject: (Throwable) -> Unit): JQueryAjaxSettings {
            return object : JQueryAjaxSettings { //TODO here?
                override var cache: Boolean?
                    get() = false
                    set(_) {}
                override var type: String?
                    get() = "GET"
                    set(_) {}
                override val error: ((jqXHR: JQueryXHR, textStatus: String, errorThrown: String) -> Any)?
                    get() = { _, _, _ -> requestReject(Exception()) }
            }
        }
    }

    fun loadTestFiles(sectionName: String, sectionsPath: List<String>, paragraphsInfo: List<Map<String, Any>>): Promise<Promise<Array<out Map<String, Any>>>>
}