package org.jetbrains.kotlin.spec.tests

import js.externals.jquery.JQuery
import js.externals.jquery.`$`
import org.jetbrains.kotlin.spec.MarkUp
import org.jetbrains.kotlin.spec.SpecConfig
import org.jetbrains.kotlin.spec.entities.SectionTestSet
import org.jetbrains.kotlin.spec.tests.loaders.GithubTestsLoader
import org.jetbrains.kotlin.spec.tests.loaders.LoaderByTestsMapFile
import org.jetbrains.kotlin.spec.utils.format
import kotlin.browser.window

class SpecTestsLoader() {
    private val loader = LoaderByTestsMapFile()


    companion object {
        private const val EXCEPTED_SELECTORS = ".grammar-rule"
        private val paragraphSelector = listOf(".paragraph", "DL", "UL", "OL").joinToString(",")
        private val sectionTagNames = listOf("H1", "H2", "H3", "H4", "H5")

        private const val LOADING_ICON_PATH = "./resources/images/loading.gif"
        private const val LOADING_ICON_HTML = "<img src=\"$LOADING_ICON_PATH\" />"
        private const val SET_BRANCH_ICON = "./resources/images/set-branch.png"

        private const val LOAD_TESTS_TEXT = "Load tests"
        private val notLoadedTestsText = "Tests for \"{1}\" in \"${GithubTestsLoader.getBranch()}\" branch aren't yet written."

        const val SECTION_PATH_SEPARATOR = ", "

        fun insertLoadIcon(headerElement: JQuery) {
            headerElement.append("""
                <a href="#" class="set-branch" title="The compiler repo branch from which the tests will be taken"><img src="${this.SET_BRANCH_ICON}" /></a></span>
                <a href="#" data-id="${headerElement.attr("id")}" data-type="${headerElement.prop("tagName").toString().toLowerCase()}" class="load-tests" title="Show tests coverage">Load tests</a>
            """)
        }


        fun getParagraphsInfo(sectionElement: JQuery): List<Map<String, Any>>? {
            var nextSibling = sectionElement.get().run {
                if (size == 0) return@getParagraphsInfo null
                this[0].nextElementSibling
            }
            val sectionName = sectionElement.attr("id")
            val paragraphsMap = mutableListOf<Map<String, Any>>()
            var paragraphCounter = 1

            if (sectionName.isEmpty())
                return null

            while (nextSibling != null) {
                if (sectionTagNames.indexOf(nextSibling.tagName) != -1) break

                val isParagraph = nextSibling.matches(paragraphSelector)
                val childParagraph = nextSibling.querySelector(".paragraph")

                if ((isParagraph || childParagraph != null) && !`$`(nextSibling).`is`(EXCEPTED_SELECTORS)) {
                    val nextParagraph = childParagraph ?: nextSibling
                    paragraphsMap.add(
                            mapOf(
                                    "paragraphElement" to nextParagraph,
                                    "sentenceCount" to `$`(nextParagraph).find(".sentence").length.toString()
                            )
                    )
                    paragraphCounter++
                }
                nextSibling = nextSibling.nextElementSibling
            }

            return paragraphsMap
        }

        fun showMarkup() {
            `$`("h2, h3, h4, h5").each { _, section ->
                val sectionTagName = section.tagName.toLowerCase()
                val sectionElement = `$`(section)
                val paragraphsInfo = getParagraphsInfo(sectionElement) ?: return@each null
                val sectionsPath = mutableSetOf<String>().apply {
                    if (sectionTagName == "h3" || sectionTagName == "h4" || sectionTagName == "h5") {
                        add(getParentSectionName(sectionElement, "h2"))
                    }
                    if (sectionTagName == "h4" || sectionTagName == "h5") {
                        add(getParentSectionName(sectionElement, "h3"))
                    }
                    if (sectionTagName == "h5") {
                        add(getParentSectionName(sectionElement, "h4"))
                    }
                    add(sectionElement.attr("id"))
                }

                MarkUp.showMarkUpForParagraphs(paragraphsInfo, sectionsPath.joinToString(", "))
            }
        }

        private fun getNestedSections(sectionElement: JQuery): List<String> {
            val placeCurrentSectionLevel = sectionTagNames.indexOf(sectionElement.prop("tagName").toString().toUpperCase())
            val otherSectionTagNames = sectionTagNames.slice(0..placeCurrentSectionLevel)
            val nestedSectionTagNames = sectionTagNames.slice(placeCurrentSectionLevel until sectionTagNames.size)
            var nextSibling = sectionElement.get()[0].nextElementSibling
            val nestedSectionIds = mutableListOf<String>()

            while (nextSibling != null) {
                if (otherSectionTagNames.indexOf(nextSibling.tagName) != -1)
                    break

                if (nestedSectionTagNames.indexOf(nextSibling.tagName) != -1) {
                    nestedSectionIds.add(nextSibling.getAttribute("id")!!)
                }

                nextSibling = nextSibling.nextElementSibling
            }

            return nestedSectionIds
        }

//        fun loadHelperFile(helperName: String) =
//                GithubTestsLoader.loadFileFromRawGithub(
//                        "$helperName.kt",
//                        testType = GithubTestsLoader.TestFileType.SPEC_TEST,
//                        customFolder = "helpers"
//                )

        fun parseTestFiles(
                sectionTestSet: SectionTestSet,
                currentSection: String,
                sectionsPath: List<String>,
                paragraphsInfo: List<Map<String, Any>>
        ) {
            println("parseTestFiles!!")
            val pathPrefix = "${sectionsPath.joinToString(SECTION_PATH_SEPARATOR)}$SECTION_PATH_SEPARATOR$currentSection"

            TestsCoverageColorsArranger.showCoverageOfParagraphs(paragraphsInfo, sectionTestSet, pathPrefix)
        }


        fun getParentSectionName(element: JQuery, type: String) = element.prevAll(type).first().attr("id")

        fun onSetBranchIconClick() {
            val currentBranch = window.localStorage.getItem("spec-tests-branch") ?: GithubTestsLoader.DEFAULT_BRANCH
            val newBranch = window.prompt("Specify the Kotlin compiler repo branch from which the spec tests will be taken:", currentBranch)

            if (newBranch != null && newBranch != currentBranch) {
                window.localStorage.setItem("spec-tests-branch", newBranch)
            }
        }
    }


    private lateinit var sectionPrevLoaded: String
    private var originalSectionName: String? = null
    private var numberSectionsLoaded = 0

    fun onTestsLoadingLinkClick(link: JQuery) {
        val section = link.parent("h3, h4, h5")
        val paragraphsInfo = getParagraphsInfo(section)
        val nestedSections = getNestedSections(section)
        val sectionName = section.attr("id")
        val sectionsPath = mutableListOf(getParentSectionName(section, "h2")) //what is the purpose?

        if (originalSectionName == null) {
            originalSectionName = sectionName
            numberSectionsLoaded = 1
        }

        link.html(LOADING_ICON_HTML)

        if (link.data("type") == "h4" || link.data("type") == "h5") {
            sectionsPath.add(getParentSectionName(section, "h3"))
        }
        if (link.data("type") == "h5") {
            sectionsPath.add(getParentSectionName(section, "h4"))
        }

        //println("link.data(\"type\")-->${link.data("type")}\nsectionsPath:-->" + sectionsPath.toString())
        loader.loadTestFilesForSectionByTestMapsNew(sectionName, sectionsPath, SpecConfig.testAreasToLoad)
                .then { sectionTestSet ->
//                    println("sectionTestSet.loadedTestAreas.size = "+sectionTestSet.loadedTestAreas.size)
//                    sectionTestSet.loadedTestAreas.forEach { testArea ->
//                        println("testArea=${testArea.key.path}")
//                        println("testArea.value.size=${testArea.value.size}")
//                    }

                    if (paragraphsInfo != null)
                        parseTestFiles(sectionTestSet, sectionName, sectionsPath, paragraphsInfo)

                    link.html("Reload tests")

                    if (originalSectionName == sectionName) {
                        section.nextAll(".paragraph.with-tests").first().get()[0].scrollIntoView()
                        originalSectionName = null
                        sectionPrevLoaded = sectionName
                    }
                }.catch {
                    numberSectionsLoaded--
                    if (originalSectionName == sectionName) {
                        originalSectionName = null
                        sectionPrevLoaded = sectionName
                    }
                    if (numberSectionsLoaded == 0) {
                        window.alert(notLoadedTestsText.format(sectionPrevLoaded))
                    }
                    link.html(LOAD_TESTS_TEXT)
                }

        nestedSections.forEach { sectionId ->
            numberSectionsLoaded++
            `$`("#${sectionId.replace(".", """\\.""")} .load-tests").click()
        }
    }
}
