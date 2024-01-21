// SPDX-License-Identifier: MIT
// Copyright 2023 BooleWorks GmbH

package com.booleworks.prl.transpiler

import com.booleworks.logicng.datastructures.Substitution
import com.booleworks.logicng.formulas.Formula
import com.booleworks.logicng.formulas.FormulaFactory
import com.booleworks.logicng.formulas.Variable
import com.booleworks.logicng.propositions.ExtendedProposition
import com.booleworks.logicng.propositions.PropositionBackpack
import com.booleworks.prl.model.Module
import com.booleworks.prl.model.constraints.ComparisonOperator
import com.booleworks.prl.model.constraints.Constraint
import com.booleworks.prl.model.constraints.EnumComparisonPredicate
import com.booleworks.prl.model.constraints.EnumInPredicate
import com.booleworks.prl.model.constraints.Feature
import com.booleworks.prl.model.rules.AnyRule
import com.booleworks.prl.model.rules.ConstraintRule
import com.booleworks.prl.model.slices.Slice
import com.booleworks.prl.model.slices.SliceSet
import com.booleworks.prl.model.slices.SliceType.ALL
import com.booleworks.prl.model.slices.SliceType.ANY
import com.booleworks.prl.model.slices.SliceType.SPLIT

typealias PrlProposition = ExtendedProposition<RuleInformation>

enum class RuleType(val description: String) {
    ORIGINAL_RULE("Original rule from the rule file"),
    UNKNOWN_FEATURE_IN_SLICE("Unknown feature in this slice"),
    FEATURE_EQUIVALENCE_OVER_SLICES("Feature equivalence for slice"),
    ENUM_FEATURE_CONSTRAINT("EXO constraint for enum feature values"),
    ADDITIONAL_RESTRICTION("Additional user-provided restriction")
}

data class RuleInformation(val ruleType: RuleType, val rule: AnyRule?, val sliceSet: SliceSet?) : PropositionBackpack {
    constructor(rule: AnyRule, sliceSet: SliceSet) : this(RuleType.ORIGINAL_RULE, rule, sliceSet)
    constructor(ruleType: RuleType, sliceSet: SliceSet) : this(ruleType, null, sliceSet)
    constructor(ruleType: RuleType) : this(ruleType, null, null)

    companion object {
        fun fromAdditionRestriction(constraint: Constraint) =
            RuleInformation(RuleType.ADDITIONAL_RESTRICTION, ConstraintRule(constraint, Module("")), null)
    }
}

fun PrlProposition.substitute(f: FormulaFactory, substitution: Substitution) =
    PrlProposition(backpack(), formula().substitute(f, substitution))

data class SliceTranslation(val sliceSet: SliceSet, val info: TranslationInfo) {
    val propositions = info.propositions
    val knownVariables = info.knownVariables
    val booleanVariables = info.booleanVariables
    val enumVariables = info.enumVariables
    val enumMapping = info.enumMapping
    val unknownFeatures = info.unknownFeatures
}

data class MergedSliceTranslation(val sliceSelectors: Map<String, SliceTranslation>, val info: TranslationInfo) {
    val propositions = info.propositions
    val knownVariables = info.knownVariables
    val booleanVariables = info.booleanVariables
    val enumVariables = info.enumVariables
    val enumMapping = info.enumMapping
    val unknownFeatures = info.unknownFeatures
}

data class ModelTranslation(val computations: List<SliceTranslation>) : Iterable<SliceTranslation> {
    val numberOfComputations = computations.size
    val allSlices = LinkedHashSet(computations.flatMap { it.sliceSet.slices })
    fun sliceMap(): Map<Slice, SliceTranslation> =
        computations.flatMap { it.sliceSet.slices.map { slice -> Pair(slice, it) } }.toMap()

    fun allSplitSlices() = allSlices.map { it.filterProperties(setOf(SPLIT)) }.distinct()
    fun allAnySlices() = allSlices.map { it.filterProperties(setOf(ANY)) }.distinct()
    fun allAnySlices(slice: Slice) =
        allSlices.filter { it.matches(slice) }.map { it.filterProperties(setOf(SPLIT, ANY)) }.distinct()

    fun allAllSlices() = allSlices.map { it.filterProperties(setOf(ALL)) }.distinct()
    fun allAllSlices(slice: Slice) =
        allSlices.filter { it.matches(slice) }.map { it.filterProperties(setOf(SPLIT, ANY, ALL)) }.distinct()

    operator fun get(index: Int) = computations[index]
    override fun iterator() = computations.iterator()
}

interface TranspilerCoreInfo {
    val unknownFeatures: Set<Feature>
    val booleanVariables: Set<Variable>
    val enumMapping: Map<String, Map<String, Variable>>
    val intMapping: Map<String, Map<Int, Variable>>

    fun translateEnumIn(f: FormulaFactory, constraint: EnumInPredicate): Formula =
        enumMapping[constraint.feature.fullName].let { enumMap ->
            if (enumMap == null) f.falsum() else f.or(constraint.values.map { enumMap[it] ?: f.falsum() })
        }

    fun translateEnumComparison(f: FormulaFactory, constraint: EnumComparisonPredicate): Formula =
        enumMapping[constraint.feature.fullName].let { enumMap ->
            if (enumMap == null) f.falsum() else enumMap[constraint.value.value].let { v ->
                if (v == null) f.constant(constraint.comparison != ComparisonOperator.EQ) else f.literal(
                    v.name(),
                    constraint.comparison == ComparisonOperator.EQ
                )
            }
        }
}

data class TranslationInfo(
    val propositions: List<PrlProposition>,
    val knownVariables: Set<Variable>,
    override val booleanVariables: Set<Variable>,
    override val enumMapping: Map<String, Map<String, Variable>>,
    override val intMapping: Map<String, Map<Int, Variable>>,
    override val unknownFeatures: Set<Feature>,
) : TranspilerCoreInfo {
    val enumVariables: Set<Variable> = enumMapping.values.flatMap { it.values }.toSet()
    val intVariables: Set<Variable> = intMapping.values.flatMap { it.values }.toSet()
    private val var2enum = mutableMapOf<Variable, Pair<String, String>>()
    private val var2int = mutableMapOf<Variable, Pair<String, Int>>()

    init {
        enumMapping.forEach { (feature, vs) ->
            vs.forEach { (value, variable) ->
                var2enum[variable] = Pair(feature, value)
            }
        }
        intMapping.forEach { (feature, vs) ->
            vs.forEach { (value, variable) ->
                var2int[variable] = Pair(feature, value)
            }
        }
    }

    fun getFeatureAndValue(v: Variable) = var2enum[v]
    fun getFeatureAndInt(v: Variable) = var2int[v]
}
