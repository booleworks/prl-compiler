// SPDX-License-Identifier: MIT
// Copyright 2023 BooleWorks GmbH

package com.booleworks.prl.model

import com.booleworks.prl.compiler.FeatureStore
import com.booleworks.prl.compiler.IntegerStore
import com.booleworks.prl.compiler.PropertyStore
import com.booleworks.prl.model.constraints.Feature
import com.booleworks.prl.model.datastructures.FeatureAssignment
import com.booleworks.prl.model.rules.AnyRule
import com.booleworks.prl.model.slices.AnySliceSelection
import com.booleworks.prl.model.slices.Slice
import com.booleworks.prl.model.slices.evaluateProperties
import com.booleworks.prl.parser.PrlVersion

@Suppress("UNCHECKED_CAST")
data class PrlModel(
    val header: PrlModelHeader,
    val moduleHierarchy: ModuleHierarchy,
    val featureStore: FeatureStore,
    val integerStore: IntegerStore,
    val rules: List<AnyRule>,
    val propertyStore: PropertyStore
) {
    private val featureMap: Map<String, Feature> by lazy {
        mutableMapOf<String, Feature>().apply {
            moduleHierarchy.modules().forEach {
                featureStore.allDefinitions(it).forEach { def -> this[def.feature.fullName] = def.feature }
            }
        }
    }

    fun getModule(moduleName: String) =
        moduleHierarchy.moduleForName(moduleName) ?: throw IllegalArgumentException("Could not find module $moduleName")

    fun <T : Feature> getFeature(featureCode: String, moduleName: String? = null): T {
        val searchName = if (moduleName != null)
            Feature.fullNameOf(featureCode, moduleName)
        else
            Feature.fullNameOf(featureCode, moduleHierarchy.modules().first().fullName)
        val feature = featureMap[searchName]
            ?: throw IllegalArgumentException("Could not find feature '$featureCode' in module '$moduleName'")
        return feature as T
    }

    fun features() = booleanFeatures() + enumFeatures() + intFeatures()
    fun booleanFeatures() = featureStore.booleanFeatures()
    fun enumFeatures() = featureStore.enumFeatures()
    fun intFeatures() = featureStore.intFeatures()
    fun enumValues() = featureStore.enumDefinitions().associate { it.feature to it.values }
    fun containsBooleanFeatures() = featureStore.containsBooleanFeatures()
    fun containsEnumFeatures() = featureStore.containsEnumFeatures()
    fun containsIntFeatures() = featureStore.containsIntFeatures()
    fun evaluate(assignment: FeatureAssignment) = rules.all { it.evaluate(assignment) }
    fun evaluateEachRule(assignment: FeatureAssignment) = rules.associateWith { it.evaluate(assignment) }
    fun restrict(assignment: FeatureAssignment) = rules.map { it.restrict(assignment) }
    fun syntacticSimplify() = rules.map { it.syntacticSimplify() }
    fun rules(selections: List<AnySliceSelection>) = rules.filter { evaluateProperties(it.properties, selections) }
    fun rules(slice: Slice) = rules.filter { evaluateProperties(it.properties, slice.selector()) }
    fun featureDefinitions(selections: List<AnySliceSelection>) =
        featureStore.allDefinitions().filter { evaluateProperties(it.properties, selections) }

    fun featureDefinitions(slice: Slice) =
        featureStore.allDefinitions().filter { evaluateProperties(it.properties, slice.selector()) }

    fun propertyDefinition(name: String) = propertyStore.definition(name)

    fun toRuleFile() =
        RuleFile(header, moduleHierarchy.modules().map { toRuleSet(it) }, propertyStore.slicingPropertyDefinitions)

    private fun toRuleSet(module: Module): RuleSet {
        val groups = featureStore.groups(module)
        return RuleSet(
            module,
            featureStore.allDefinitions(module).filterNot { groups.contains(it.feature) },
            rules.filter { it.module == module },
            module.imports,
            module.lineNumber
        )
    }
}

data class PrlModelHeader(val version: PrlVersion, val properties: Map<String, AnyProperty>) {
    fun stripProperties() = PrlModelHeader(version, mapOf())
}
