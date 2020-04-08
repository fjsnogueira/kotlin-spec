package org.jetbrains.kotlin.spec

import org.jetbrains.kotlin.spec.testArea.TestArea

class SpecConfig {
    companion object{
        val testAreasToLoad = arrayListOf( TestArea.CodegenBox, TestArea.Diagnostics)
    }

}