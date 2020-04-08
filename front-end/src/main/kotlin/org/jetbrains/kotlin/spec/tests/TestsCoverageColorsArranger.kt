package org.jetbrains.kotlin.spec.tests

import js.externals.jquery.JQuery
import js.externals.jquery.`$`
import org.jetbrains.kotlin.spec.MarkUp
import org.jetbrains.kotlin.spec.entities.ParagraphTestSet
import org.jetbrains.kotlin.spec.entities.SectionTestSet
import org.jetbrains.kotlin.spec.entities.SentenceTestSet
import org.jetbrains.kotlin.spec.utils.format
import org.w3c.dom.HTMLElement

object TestsCoverageColorsArranger {

    const val TEMPLATE = """
            <div class='test-coverage-view'>
                <select name='test-area'>
                    <option value='diagnostics'>Front-end diagnostics tests</option>
                    <option value='box'>Codegen box tests</option>
                </select>
                <select name='test-type'></select>
                <select name='test-number'></select>
            </div>
        """


    private fun detectUnexpectedBehaviour(testsOfType: Map<String, Map<String, Any>>): Boolean {
        var unexpectedBehaviour = false

        for ((_, test) in testsOfType) {
            if (test["unexpectedBehaviour"] as Boolean) {
                unexpectedBehaviour = true
            }
            (test["cases"].unsafeCast<List<Map<String, Map<String, Any>>>>()).forEach { testCase ->
                if (testCase["infoElements"]?.get("UNEXPECTED BEHAVIOUR") != null) {
                    unexpectedBehaviour = true
                }
            }
        }

        return unexpectedBehaviour
    }

    private fun showSentenceCoverage(sentence: JQuery, sentenceTestSet: SentenceTestSet) {
        println("sentenceTestSet = ")
        //  sentenceTestSet.loadedTestAreas.forEach { print( it.key.path+" " + it.value.size ) }
        val testsByArea = mutableListOf<String>()
        var unexpectedBehaviour = false

        //todo unexpectedBehaviour = unexpectedBehaviour || detectUnexpectedBehaviour(testType)
        for ((testArea, listOfTests) in sentenceTestSet.loadedTestAreas) {

            val map = listOfTests.groupBy { it.testPath.testType }

            val testNumberByTypeInfo = mutableListOf<String>()
            for ((posOrNeg, typedTests) in map) {

                testNumberByTypeInfo.add(typedTests.size.toString() + " " + posOrNeg.toString()) // 1 positive etc..

                if (testNumberByTypeInfo.size > 0) {
                    testsByArea.add("<b>" + testArea.description + "</b>: " + testNumberByTypeInfo.joinToString(", "))
                }
            }

        }

        if (unexpectedBehaviour) {
            sentence.addClass("unexpected-behaviour")
                    .parent().before("<span class='unexpected-behaviour-marker'></span>")
        }

        if (testsByArea.isNotEmpty()) {
            sentence.prepend("<span class='coverage-info'>{1}</span>".format(testsByArea.joinToString("<br />")))
                    .data("tests", sentenceTestSet)
                    .addClass("covered")

        }
    }

    private fun showParagraphCoverage(paragraph: JQuery, sectionPath: String, paragraphTestSet: ParagraphTestSet) {
        val sentences = `$`(paragraph).find(".sentence")
        var sentenceCounter = 1
        val paragraphNumber = paragraphTestSet.paragraphNumber

        MarkUp.insertParagraphNumber(`$`(paragraph), paragraphNumber, sectionPath, paragraphNumber)

        sentences.each { _, el ->
            val sentence = `$`(el)

            val existingNumberInfo = sentence.find(".number-info")
            if (existingNumberInfo.length.toInt() > 0) {
                existingNumberInfo.remove()
            }

            val sentenceTestSet = SentenceTestSet(paragraphTestSet = paragraphTestSet,
                    sentenceNumber = sentenceCounter)
            showSentenceCoverage(sentence, sentenceTestSet)
            MarkUp.insertSentenceNumber(sentence, sentenceCounter, sectionPath, paragraphNumber, sentenceCounter)
            sentenceCounter++
        }
    }


    fun showCoverageOfParagraphs(paragraphsInfo: List<Map<String, Any>>, sectionTestSet: SectionTestSet, sectionsPath: String) {
        paragraphsInfo.forEachIndexed { paragraphIndex, paragraph ->
            val paragraphNumber = paragraphIndex + 1
            val paragraphEl = `$`(paragraph["paragraphElement"] as HTMLElement).apply { addClass("with-tests") }

            val paragraphTestSet = ParagraphTestSet(sectionTestSet = sectionTestSet,
                    paragraphNumber = paragraphNumber)
            showParagraphCoverage(paragraphEl, sectionsPath, paragraphTestSet)
        }
    }
}
