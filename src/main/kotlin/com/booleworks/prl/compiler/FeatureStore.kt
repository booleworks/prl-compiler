// SPDX-License-Identifier: MIT
// Copyright 2023 BooleWorks GmbH

package com.booleworks.prl.compiler

import com.booleworks.prl.compiler.PropertyStore.Companion.uniqueSlices
import com.booleworks.prl.model.AnyFeatureDef
import com.booleworks.prl.model.AnySlicingPropertyDefinition
import com.booleworks.prl.model.EnumFeatureDefinition
import com.booleworks.prl.model.FeatureDefinition
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
     * Adds a given feature definition to the feature store.
     * Is the definition already present, a compiler error is returned.
     *
     * A feature definition is uniquely determined by its name -
     * its feature type is not relevant for uniqueness.
     */
    internal fun addDefinition(
        definition: PrlFeatureDefinition,
        state: CompilerState,
        slicingProperties: MutableMap<String, AnySlicingPropertyDefinition> = mutableMapOf(),
        isGroup: Boolean = false
    ) {
        val map = mapForType(definition)
        addDefinitionToMap(definition, slicingProperties, map, isGroup, state)
    }

    internal fun generateDefinitionMap(
        features: Collection<PrlFeature>,
        state: CompilerState
    ): Map<PrlFeature, AnyFeatureDef> {
        val map = mutableMapOf<PrlFeature, AnyFeatureDef>()
        for (feature in features) {
            val definitions = findMatchingDefinitions(feature.featureCode)
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

    fun findMatchingDefinitions(featureCode: String): List<AnyFeatureDef> {
        return booleanFeatures.getOrDefault(featureCode, listOf()) +
                enumFeatures.getOrDefault(featureCode, listOf()) +
                intFeatures.getOrDefault(featureCode, listOf())
    }

    fun allDefinitions() =
        booleanFeatures.values.flatten() + enumFeatures.values.flatten() + intFeatures.values.flatten()

    fun allDefinitionMaps(): Map<String, List<AnyFeatureDef>> = booleanFeatures + enumFeatures + intFeatures

    fun booleanFeatures() = booleanFeatures.flatMap { it.value }.map { it.feature as BooleanFeature }
    fun enumFeatures() = enumFeatures.flatMap { it.value }.map { it.feature as EnumFeature }
    fun intFeatures() = intFeatures.flatMap { it.value }.map { it.feature as IntFeature }
    fun nonUniqueFeatures() = nonUniqueFeatures.toSet()
    fun enumDefinitions() = enumFeatures.flatMap { it.value }.map { it as EnumFeatureDefinition }
    fun containsBooleanFeatures() = booleanFeatures.isNotEmpty()
    fun containsEnumFeatures() = enumFeatures.isNotEmpty()
    fun containsIntFeatures() = intFeatures.isNotEmpty()
    fun containsVersionedBooleanFeatures() = booleanFeatures().any { it.versioned }

    private fun addDefinitionToMap(
        definition: PrlFeatureDefinition,
        slicingProperties: MutableMap<String, AnySlicingPropertyDefinition>,
        map: MutableMap<String, MutableList<AnyFeatureDef>>,
        isGroup: Boolean,
        state: CompilerState
    ) {
        val defToAdd = FeatureDefinition.fromPrlDef(definition)
        val existingDefinitions = findMatchingDefinitions(definition.code)
        val hasUniqueSlices = existingDefinitions.all { existing ->
            uniqueSlices(defToAdd.properties, existing.properties, slicingProperties.keys)
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

    private fun mapForType(definition: PrlFeatureDefinition) = when (definition) {
        is PrlBooleanFeatureDefinition -> booleanFeatures
        is PrlEnumFeatureDefinition -> enumFeatures
        is PrlIntFeatureDefinition -> intFeatures
    }
}

