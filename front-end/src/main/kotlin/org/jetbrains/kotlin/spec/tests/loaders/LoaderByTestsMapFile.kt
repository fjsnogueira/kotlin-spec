package org.jetbrains.kotlin.spec.tests.loaders

import org.jetbrains.kotlin.spec.entities.*
import org.jetbrains.kotlin.spec.entities.testElement.TestElementInfo
import org.jetbrains.kotlin.spec.testArea.TestArea
import org.jetbrains.kotlin.spec.tests.loaders.GithubTestsLoader.Companion.loadTestFileFromRawGithub
import org.jetbrains.kotlin.spec.tests.loaders.GithubTestsLoader.Companion.loadTestMapFileFromRawGithub
import org.jetbrains.kotlin.spec.utils.format
import kotlin.js.Promise


class LoaderByTestsMapFile : GithubTestsLoader {
    private val testsMapFilename = "testsMap.json"

    private fun loadTestsMapFile(sectionsPath: String, testAreasToLoad: ArrayList<TestArea>) =
            loadTestMapFileFromRawGithub("$sectionsPath/$testsMapFilename",
                    null,
                    GithubTestsLoader.TestFileType.SPEC_TEST,
                    testAreasToLoad = testAreasToLoad)

    private fun getPromisesForTestFilesFromTestMap(
            testsMapData: TestMapData?,
            sectionsPath: List<String>,
            sectionName: String
    ): Array<Promise<Test>> {

       // println("!!getPromisesForTestFilesFromTestMap")

        val promises = mutableListOf<Promise<Test>>()
      //  println("testsMapData?.testMapJsonElement =" + testsMapData?.testMapJsonElement)
        val testsMap = testsMapData?.testMapJsonElement ?: return promises.toTypedArray()

      //  println("testsMap.jsonObject.toString():--> " + testsMap.jsonObject.toString())


        for ((paragraph, testsByParagraphs) in testsMap.jsonObject) {
            //println("paragraph")
            /**testType : pos or neg */
            for ((testType, testsByTypes) in testsByParagraphs.jsonObject) {
                for ((testSentence, testsBySentences) in testsByTypes.jsonObject) {

                    testsBySentences.jsonArray.forEachIndexed() { i, testInfo ->
                        val testPathInSpec = "{1}.{2}.p-{3}.{4}.{5}.{6}"
                                .format(sectionsPath.joinToString("."), sectionName, paragraph, testType, testSentence, i + 1)
                        val testFilePath = testInfo.jsonObject["path"]?.primitive?.content!!

                        val testElementInfo = TestElementInfo(testInfo.jsonObject, i + 1)
                        if (testElementInfo.path.isEmpty())
                            testElementInfo.path = "{1}/{2}/p-{3}/{4}/{5}.{6}.kt"
                                    .format(sectionsPath.joinToString("/"), sectionName, paragraph, testType, testSentence, i + 1)


                      //  println(" testType " +testType.toString())

                        val testPath = TestPath(paragraph, TestType.getByShortName(testType), testSentence)
                     //   println("TestPath testType" +testPath.testType.toString())
                        promises.add(loadTestFileFromRawGithub(testFilePath, testPathInSpec, testElementInfo = testElementInfo,  testPath = testPath, testArea =testsMapData.testArea))
                    }
                }
            }
        }

        return promises.toTypedArray()
    }


    fun loadTestFilesForSectionByTestMapsNew(sectionName: String, sectionsPath: List<String>, testAreasToLoad: ArrayList<TestArea>)
            : Promise<Promise<SectionTestSet>> {
        return loadTestsMapFile(sectionsPath.joinToString("/") + "/" + sectionName, testAreasToLoad)
                .then { testsMapDataForSectionByTestAreaMap ->
                 //   println("azaz=" + (sectionsPath.joinToString("/") + "/" + sectionName))
                  //  println("testsMapDataForSectionByTestAreaMap size =" + testsMapDataForSectionByTestAreaMap.size)
                  //  println("testAreasToLoad size =" + testAreasToLoad.size)

                    val querySet = mutableListOf<Promise<Boolean>>()
                    val resultMap = mutableMapOf<TestArea,  List<Test>>()
                    testAreasToLoad.forEach {
                    //    println("loadTestFilesForSectionByTestMapsNew:-->" + it.path)
                        querySet.add(getPromiseForTests(it, testsMapDataForSectionByTestAreaMap, sectionsPath, sectionName, resultMap))
                    }
                //    println("querySet size =" + querySet.size)
                    Promise.all(querySet.toTypedArray()
                    ).then {
                    //    println("lolololo")
                     //   testAreasToLoad.forEach { it -> println(" Test Area path=${it.path}")}

                        SectionTestSet(resultMap)
                    }
                }
    }

    private fun getPromiseForTests(
            testArea: TestArea,
            testsMapDataForSectionByTestAreaEnumMap: MutableMap<TestArea, TestMapData>,
            sectionsPath: List<String>,
            sectionName: String,
            mapOfTests: MutableMap<TestArea,  List<Test>>
    ): Promise<Boolean> {

      //  println("!!getPromiseForTests")
      //  println("testsMapDataForSectionByTestAreaEnumMap size=" + testsMapDataForSectionByTestAreaEnumMap.size)
//        println("testsMapDataForSectionByTestAreaEnumMap[testArea]?.testMapJsonElement ="
//                + testsMapDataForSectionByTestAreaEnumMap[testArea]?.testMapJsonElement)
        return Promise.all(
                getPromisesForTestFilesFromTestMap(
                        testsMapDataForSectionByTestAreaEnumMap[testArea],
                        sectionsPath, sectionName)
        ).then { tests ->
        //    println("tests.size = "+ tests.size)
            mapOfTests.put(testArea, tests.toList())
           // testArea.listOfTests.addAll(tests)
            //println("getPromiseForTests path=${testArea.path}, size=${testArea.listOfTests.size}, tests.size=${tests.size}")
            true //todo change to promise<any>
        }
    }

    override fun loadTestFiles(sectionName: String, sectionsPath: List<String>, paragraphsInfo: List<Map<String, Any>>): Promise<Promise<Array<out Map<String, Any>>>> {
        //todo make GithubTestsLoader generic
        TODO()
    }
}