package com.booleworks.prl.parser

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Paths

class ParserModulesTest {
    @Test
    fun testEmptyFile() {
        val ruleFile: PrlRuleFile = parseRuleFile(Paths.get("test-files/prl/parser/empty.prl"))
        assertThat(ruleFile).isNotNull
        assertThat(ruleFile.ruleSets).isEmpty()
        assertThat(ruleFile.fileName).isEqualTo("empty.prl")
    }

    @Test
    fun testEmptyModuleName() {
        val ruleFile: PrlRuleFile = parseRuleFile(File("test-files/prl/parser/empty_module_name.prl"))
        assertThat(ruleFile).isNotNull
        assertThat(ruleFile.ruleSets.map { it.module.fullName })
            .containsExactly("")
        assertThat(ruleFile.ruleSets[0].lineNumber).isEqualTo(5)
        assertThat(ruleFile.fileName).isEqualTo("empty_module_name.prl")
    }

    @Test
    fun testEmptyModuleNameMixedWithModules() {
        val ruleFile: PrlRuleFile = parseRuleFile(File("test-files/prl/parser/empty_module_name_mixed.prl"))
        assertThat(ruleFile).isNotNull
        assertThat(ruleFile.ruleSets.map { it.module.fullName })
            .containsExactly("", "com.booleworks", "com.booleworks.p1")
        assertThat(ruleFile.ruleSets[0].lineNumber).isEqualTo(5)
        assertThat(ruleFile.ruleSets[1].lineNumber).isEqualTo(9)
        assertThat(ruleFile.ruleSets[2].lineNumber).isEqualTo(13)
        assertThat(ruleFile.fileName).isEqualTo("empty_module_name_mixed.prl")
    }

    @Test
    fun testEmptyModules() {
        val ruleFile: PrlRuleFile = parseRuleFile(File("test-files/prl/parser/empty_modules.prl"))
        assertThat(ruleFile).isNotNull
        assertThat(ruleFile.ruleSets.map { it.module.fullName })
            .containsExactly("", "com.booleworks", "com.booleworks.p1", "com.booleworks.p2")
        assertThat(ruleFile.ruleSets[0].lineNumber).isEqualTo(5)
        assertThat(ruleFile.ruleSets[1].lineNumber).isEqualTo(8)
        assertThat(ruleFile.ruleSets[2].lineNumber).isEqualTo(11)
        assertThat(ruleFile.ruleSets[3].lineNumber).isEqualTo(14)
        assertThat(ruleFile.fileName).isEqualTo("empty_modules.prl")
    }

    @Test
    fun testImports() {
        val ruleFile: PrlRuleFile = parseRuleFile("test-files/prl/parser/imports.prl")
        assertThat(ruleFile).isNotNull
        assertThat(ruleFile.ruleSets).hasSize(1)
        val ruleSet: PrlRuleSet = ruleFile.ruleSets[0]
        assertThat(ruleSet.imports).containsExactly(
            PrlModuleImport("com.booleworks.p1", 6),
            PrlModuleImport("com.booleworks.p2", 8),
            PrlModuleImport("com.booleworks.p3", 9)
        )
        assertThat(ruleSet.lineNumber).isEqualTo(5)
        assertThat(ruleSet.imports[0].lineNumber).isEqualTo(6)
        assertThat(ruleSet.imports[1].lineNumber).isEqualTo(8)
        assertThat(ruleSet.imports[2].lineNumber).isEqualTo(9)
        assertThat(ruleFile.fileName).isEqualTo("imports.prl")
    }

}
