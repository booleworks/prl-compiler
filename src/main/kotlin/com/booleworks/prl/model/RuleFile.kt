// SPDX-License-Identifier: MIT
// Copyright 2023 BooleWorks GmbH

package com.booleworks.prl.model

import com.booleworks.prl.model.datastructures.FeatureRenaming
import com.booleworks.prl.model.rules.AnyRule
import com.booleworks.prl.parser.PragmaticRuleLanguage.INDENT
import com.booleworks.prl.parser.PragmaticRuleLanguage.KEYWORD_HEADER
import com.booleworks.prl.parser.PragmaticRuleLanguage.KEYWORD_IMPORT
import com.booleworks.prl.parser.PragmaticRuleLanguage.KEYWORD_MODULE
import com.booleworks.prl.parser.PragmaticRuleLanguage.KEYWORD_PRL_VERSION
import com.booleworks.prl.parser.PragmaticRuleLanguage.KEYWORD_PROPERTIES
import com.booleworks.prl.parser.PragmaticRuleLanguage.KEYWORD_SLICING
import com.booleworks.prl.parser.PragmaticRuleLanguage.SYMBOL_LBRA
import com.booleworks.prl.parser.PragmaticRuleLanguage.SYMBOL_RBRA
import java.util.Objects

class RuleFile(
    val header: PrlModelHeader,
    val ruleSets: List<RuleSet>,
    val slicingProperties: Map<String, AnySlicingPropertyDefinition>,
    val fileName: String? = null
) {
    fun rename(renaming: FeatureRenaming) =
        RuleFile(header, ruleSets.map { it.rename(renaming) }, slicingProperties, fileName)

    fun stripProperties() =
        RuleFile(header.stripProperties(), ruleSets.map { it.stripProperties() }, slicingProperties, fileName)

    fun stripMetaInfo() = RuleFile(header, ruleSets.map { it.stripMetaInfo() }, slicingProperties, fileName)
    fun stripAll() = RuleFile(header.stripProperties(), ruleSets.map { it.stripAll() }, slicingProperties, fileName)

    override fun toString() = StringBuilder().apply { appendString(this) }.toString()
    override fun hashCode() = Objects.hash(fileName, slicingProperties, ruleSets)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as RuleFile
        if (ruleSets != other.ruleSets) return false
        if (slicingProperties != other.slicingProperties) return false
        return fileName == other.fileName
    }

    private fun appendString(appendable: Appendable) {
        appendable.append(KEYWORD_HEADER).append(" ").append(SYMBOL_LBRA).append(System.lineSeparator())
        appendable.append("  ").append(KEYWORD_PRL_VERSION).append(" ").append(header.version.toString())
            .append(System.lineSeparator())
        header.properties.forEach { appendable.append("  ").append(it.value.toString()).append(System.lineSeparator()) }
        appendable.append(SYMBOL_RBRA).append(System.lineSeparator()).append(System.lineSeparator())
        if (slicingProperties.isNotEmpty()) {
            appendable.append(KEYWORD_SLICING).append(" ").append(KEYWORD_PROPERTIES).append(" ").append(SYMBOL_LBRA)
                .append(System.lineSeparator())
            val slicingPropertyValues = slicingProperties.values.toList()
            slicingPropertyValues.forEach { it.appendString(appendable.append("  ")).append(System.lineSeparator()) }
            appendable.append(SYMBOL_RBRA).append(System.lineSeparator()).append(System.lineSeparator())
        }
        for (i in ruleSets.indices) {
            ruleSets[i].appendString(appendable).append(System.lineSeparator())
            if (i < ruleSets.size - 1) appendable.append(System.lineSeparator())
        }
    }
}

class RuleSet(
    val module: Module,
    val featureDefinitions: List<AnyFeatureDef>,
    val rules: List<AnyRule>,
    val imports: List<Module>? = listOf(),
    val lineNumber: Int? = null
) {
    fun rename(renaming: FeatureRenaming) =
        RuleSet(
            module,
            featureDefinitions.map { it.rename(renaming) },
            rules.map { it.rename(renaming) },
            imports,
            lineNumber
        )

    fun stripProperties() = RuleSet(
        module,
        featureDefinitions.map { it.stripProperties() },
        rules.map { it.stripProperties() },
        imports,
        lineNumber
    )

    fun stripMetaInfo() = RuleSet(
        module,
        featureDefinitions.map { it.stripMetaInfo() },
        rules.map { it.stripMetaInfo() },
        imports,
        lineNumber
    )

    fun stripAll() =
        RuleSet(module, featureDefinitions.map { it.stripAll() }, rules.map { it.stripAll() }, imports, lineNumber)

    override fun hashCode() = Objects.hash(module, featureDefinitions, rules, imports)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as RuleSet
        if (module != other.module) return false
        if (featureDefinitions != other.featureDefinitions) return false
        if (rules != other.rules) return false
        if (imports != other.imports) return false
        return true
    }

    override fun toString() = StringBuilder().apply { appendString(this) }.toString()
    fun appendString(app: Appendable): Appendable {
        app.append(KEYWORD_MODULE).append(" ").append(module.fullName).append(" ").append(SYMBOL_LBRA)
            .append(System.lineSeparator())
        if (!imports.isNullOrEmpty()) {
            imports.forEach {
                app.append(INDENT).append(KEYWORD_IMPORT).append(" ").append(it.fullName).append(System.lineSeparator())
            }
            if (featureDefinitions.isNotEmpty() || rules.isNotEmpty()) app.append(System.lineSeparator())
        }
        if (featureDefinitions.isNotEmpty()) {
            featureDefinitions.forEach { it.appendString(app, 1).append(System.lineSeparator()) }
            if (rules.isNotEmpty()) app.append(System.lineSeparator())
        }
        rules.forEach { it.appendString(app, 1, module).append(System.lineSeparator()) }
        app.append(SYMBOL_RBRA)
        return app
    }
}
