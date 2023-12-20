package com.booleworks.prl

import com.booleworks.prl.parser.PrlModule
import com.booleworks.prl.parser.PrlRuleSet

object TestUtil {

    fun moduleInputFrom(moduleNames: Collection<String>) = moduleNames.map { createEmptyRuleSet(0, it) }

    private fun createEmptyRuleSet(lineNumber: Int, moduleName: String) = PrlRuleSet(PrlModule(moduleName), listOf(), listOf(), listOf(), lineNumber)
}
