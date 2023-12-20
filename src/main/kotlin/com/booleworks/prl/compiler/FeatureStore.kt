// SPDX-License-Identifier: MIT
// Copyright 2023 BooleWorks GmbH

package com.booleworks.prl.compiler

import com.booleworks.prl.compiler.PropertyStore.Companion.uniqueSlices
import com.booleworks.prl.model.AnyFeatureDef
import com.booleworks.prl.model.AnySlicingPropertyDefinition
import com.booleworks.prl.model.EnumFeatureDefinition
import com.booleworks.prl.model.FeatureDefinition
import com.booleworks.prl.model.Module
import com.booleworks.prl.model.ModuleHierarchy
import com.booleworks.prl.model.Visibility
import com.booleworks.prl.model.constraints.BooleanFeature
import com.booleworks.prl.model.constraints.EnumFeature
import com.booleworks.prl.model.constraints.IntFeature
import com.booleworks.prl.parser.PrlBooleanFeatureDefinition
import com.booleworks.prl.parser.PrlEnumFeatureDefinition
import com.booleworks.prl.parser.PrlFeature
import com.booleworks.prl.parser.PrlFeatureDefinition
import com.booleworks.prl.parser.PrlIntFeatureDefinition

data class FeatureStore internal constructor(
    internal val booleanFeatures: MutableMap<String, MutableList<AnyFeatureDef>> = mutableMapOf(),
    internal val intFeatures: MutableMap<String, MutableList<AnyFeatureDef>> = mutableMapOf(),
    internal val enumFeatures: MutableMap<String, MutableList<AnyFeatureDef>> = mutableMapOf(),
    internal val groups: MutableList<BooleanFeature> = mutableListOf(),
    internal val nonUniqueFeatures: MutableSet<String> = mutableSetOf()
) {
    /**
     * Adds a given feature definition in a given module to the feature store.
     * Is the definition already present, a compiler error is returned.
     *
     * A feature definition is uniquely determined by its name and its module -
     * its feature type is not relevant for uniqueness.
     *
     * Accessible features in other modules with the same name yield to a
     * warning message (Shadowing).
     */
    internal fun addDefinition(
        module: Module,
        definition: PrlFeatureDefinition,
        state: CompilerState,
        slicingProperties: MutableMap<String, AnySlicingPropertyDefinition> = mutableMapOf(),
        isGroup: Boolean = false
    ) {
        val map = mapForType(definition)
        addDefinitionToMap(module, definition, slicingProperties, map, isGroup, state)
        if (!state.hasErrors()) {
            val def = map[definition.code]!!
            checkVisibilityCollisions(module, def, state)
        }
    }

    internal fun generateDefinitionMap(
        features: Collection<PrlFeature>,
        module: Module,
        moduleHierarchy: ModuleHierarchy,
        state: CompilerState
    ): Map<PrlFeature, AnyFeatureDef> {
        val map = mutableMapOf<PrlFeature, AnyFeatureDef>()
        for (feature in features) {
            val definitions = findMatchingDefinitions(module, feature, moduleHierarchy)
            when {
                definitions.isEmpty() -> state.addError("No feature definition found for ${feature.featureCode}")
                definitions.size > 1 -> state.addError("Feature definition is not unique: ${feature.featureCode}")
                else -> map[feature] = definitions.first()
            }
        }
        return map
    }

    internal fun size() = booleanFeatures.map { it.value.size }.sum() + enumFeatures.map { it.value.size }
        .sum() + intFeatures.map { it.value.size }.sum()

    /**
     * Returns all definitions matching a given feature in a given module.
     * In a well-defined rule file each feature usage should yield exactly one
     * feature definition.
     */
    fun findMatchingDefinitions(
        module: Module,
        feature: PrlFeature,
        moduleHierarchy: ModuleHierarchy
    ): List<AnyFeatureDef> {
        val fullFeature = feature.splitFullQualifiedFeatureName()
        val moduleOfFeature = if (fullFeature.isQualified) moduleHierarchy.moduleForName(fullFeature.moduleName)
            ?: return emptyList() else module
        val matchingFeatures = getAllMatchingDefinitions(fullFeature.featureCode)
        val featureWithSameModuleName = matchingFeatures.filter { it.module.fullName == moduleOfFeature.fullName }
        if (fullFeature.moduleName.isNotEmpty()) {
            return featureWithSameModuleName.filter { isFeatureVisible(it, module) }
        }
        if (featureWithSameModuleName.isNotEmpty()) {
            return listOf(featureWithSameModuleName.first())
        }
        return getVisibleFeaturesInConnectedModules(moduleOfFeature, matchingFeatures)
    }

    fun allDefinitions() =
        booleanFeatures.values.flatten() + enumFeatures.values.flatten() + intFeatures.values.flatten()

    fun allDefinitions(module: Module) =
        booleanFeatures.values.flatten().filter { it.module == module } + enumFeatures.values.flatten()
            .filter { it.module == module } + intFeatures.values.flatten().filter { it.module == module }

    fun allDefinitionMaps(): Map<String, List<AnyFeatureDef>> = booleanFeatures + enumFeatures + intFeatures

    fun groups(module: Module) = groups.filter { it.module == module }.toSet()

    fun booleanFeatures() = booleanFeatures.flatMap { it.value }.map { it.feature as BooleanFeature }
    fun enumFeatures() = enumFeatures.flatMap { it.value }.map { it.feature as EnumFeature }
    fun intFeatures() = intFeatures.flatMap { it.value }.map { it.feature as IntFeature }
    fun nonUniqueFeatures() = nonUniqueFeatures.toSet()
    fun enumDefinitions() = enumFeatures.flatMap { it.value }.map { it as EnumFeatureDefinition }
    fun containsBooleanFeatures() = booleanFeatures.isNotEmpty()
    fun containsEnumFeatures() = enumFeatures.isNotEmpty()
    fun containsIntFeatures() = intFeatures.isNotEmpty()
    fun containsVersionedBooleanFeatures() = booleanFeatures().any { it.versioned }

    private fun checkVisibilityCollisions(module: Module, definitions: List<AnyFeatureDef>, state: CompilerState) {
        val visibleFeatures = getVisibleFeaturesInConnectedModules(module, definitions)
        visibleFeatures.map { it.module.fullName }.distinct()
            .forEach { state.addWarning("Feature also defined in module: $it") }
    }

    private fun addDefinitionToMap(
        module: Module,
        definition: PrlFeatureDefinition,
        slicingProperties: MutableMap<String, AnySlicingPropertyDefinition>,
        map: MutableMap<String, MutableList<AnyFeatureDef>>,
        isGroup: Boolean,
        state: CompilerState
    ) {
        val defToAdd = FeatureDefinition.fromPrlModule(module, definition)
        val existingDefinitions = getAllMatchingDefinitions(definition.code)
        val hasUniqueSlices = existingDefinitions.all { existing ->
            existing.module.fullName != module.fullName || uniqueSlices(
                defToAdd.properties,
                existing.properties,
                slicingProperties.keys
            )
        }
        if (existingDefinitions.any { existing -> existing.module.fullName != module.fullName }) {
            nonUniqueFeatures.add(definition.code)
        }
        val added = hasUniqueSlices && addDefinition(
            defToAdd,
            map.computeIfAbsent(definition.code) { mutableListOf() },
            isGroup
        )
        if (!added) {
            state.addError("Duplicate feature definition")
        }
    }

    private fun addDefinition(definition: AnyFeatureDef, list: MutableList<AnyFeatureDef>, isGroup: Boolean): Boolean {
        if (isGroup) groups.add(definition.feature as BooleanFeature)
        return list.add(definition)
    }

    private fun getAllMatchingDefinitions(featureCode: String) =
        booleanFeatures.getOrDefault(featureCode, listOf()) +
                enumFeatures.getOrDefault(featureCode, listOf()) +
                intFeatures.getOrDefault(featureCode, listOf())

    private fun getVisibleFeaturesInConnectedModules(module: Module, definitions: List<AnyFeatureDef>) =
        getAllVisibleFeatures(module, definitions).filter { module != it.module }.toList()

    private fun getAllVisibleFeatures(module: Module, definitions: List<AnyFeatureDef>) =
        definitions.filter { isFeatureVisible(it, module) }.toList()

    private fun isFeatureVisible(feature: AnyFeatureDef, currentModule: Module) =
        sameModule(feature, currentModule) || visibleFromAncestor(
            feature,
            currentModule
        ) || visibleFromImported(feature, currentModule)

    private fun sameModule(feature: AnyFeatureDef, currentModule: Module) = currentModule == feature.module

    private fun visibleFromAncestor(definition: AnyFeatureDef, currentModule: Module) =
        definition.module.isAncestorOf(currentModule) && (definition.visibility == Visibility.PUBLIC ||
                definition.visibility == Visibility.INTERNAL)

    private fun visibleFromImported(definition: AnyFeatureDef, currentModule: Module) =
        currentModule.hasImport(definition.module) && definition.visibility == Visibility.PUBLIC

    private fun mapForType(definition: PrlFeatureDefinition) = when (definition) {
        is PrlBooleanFeatureDefinition -> booleanFeatures
        is PrlEnumFeatureDefinition -> enumFeatures
        is PrlIntFeatureDefinition -> intFeatures
    }
}

private fun PrlFeature.splitFullQualifiedFeatureName() = QualifiedFeature(this)

private class QualifiedFeature(feature: PrlFeature) {
    val moduleName = feature.featureCode.substringBeforeLast(Module.MODULE_SEPARATOR, "")
    val featureCode = feature.featureCode.substringAfterLast(Module.MODULE_SEPARATOR, feature.featureCode)
    val isQualified by lazy { moduleName.isNotEmpty() }
}

