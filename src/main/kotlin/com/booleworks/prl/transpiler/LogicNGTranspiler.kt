// SPDX-License-Identifier: MIT
// Copyright 2023 BooleWorks GmbH

package com.booleworks.prl.transpiler

import com.booleworks.logicng.datastructures.Substitution
import com.booleworks.logicng.formulas.Formula
import com.booleworks.logicng.formulas.FormulaFactory
import com.booleworks.logicng.formulas.Variable
import com.booleworks.prl.compiler.IntegerStore
import com.booleworks.prl.model.AnyFeatureDef
import com.booleworks.prl.model.BooleanFeatureDefinition
import com.booleworks.prl.model.EnumFeatureDefinition
import com.booleworks.prl.model.IntFeatureDefinition
import com.booleworks.prl.model.PrlModel
import com.booleworks.prl.model.constraints.Amo
import com.booleworks.prl.model.constraints.And
import com.booleworks.prl.model.constraints.BooleanFeature
import com.booleworks.prl.model.constraints.Constant
import com.booleworks.prl.model.constraints.Constraint
import com.booleworks.prl.model.constraints.EnumComparisonPredicate
import com.booleworks.prl.model.constraints.EnumInPredicate
import com.booleworks.prl.model.constraints.Equivalence
import com.booleworks.prl.model.constraints.Exo
import com.booleworks.prl.model.constraints.Feature
import com.booleworks.prl.model.constraints.Implication
import com.booleworks.prl.model.constraints.IntComparisonPredicate
import com.booleworks.prl.model.constraints.IntInPredicate
import com.booleworks.prl.model.constraints.Not
import com.booleworks.prl.model.constraints.Or
import com.booleworks.prl.model.constraints.VersionPredicate
import com.booleworks.prl.model.rules.AnyRule
import com.booleworks.prl.model.rules.ConstraintRule
import com.booleworks.prl.model.rules.DefinitionRule
import com.booleworks.prl.model.rules.ExclusionRule
import com.booleworks.prl.model.rules.ForbiddenFeatureRule
import com.booleworks.prl.model.rules.GroupRule
import com.booleworks.prl.model.rules.GroupType
import com.booleworks.prl.model.rules.IfThenElseRule
import com.booleworks.prl.model.rules.InclusionRule
import com.booleworks.prl.model.rules.MandatoryFeatureRule
import com.booleworks.prl.model.slices.AnySliceSelection
import com.booleworks.prl.model.slices.MAXIMUM_NUMBER_OF_SLICES
import com.booleworks.prl.model.slices.SliceSet
import com.booleworks.prl.model.slices.computeAllSlices
import com.booleworks.prl.model.slices.computeSliceSets
import com.booleworks.prl.transpiler.RuleType.ENUM_FEATURE_CONSTRAINT
import com.booleworks.prl.transpiler.RuleType.INT_FEATURE_CONSTRAINT
import com.booleworks.prl.transpiler.RuleType.INT_OUT_OF_BOUNDS_CONSTRAINT
import kotlin.math.abs

const val S = "_"
const val SLICE_SELECTOR_PREFIX = "@SL"
const val ENUM_FEATURE_PREFIX = "@ENUM"
const val INT_FEATURE_PREFIX = "@INT"

fun transpileModel(
    f: FormulaFactory,
    model: PrlModel,
    selectors: List<AnySliceSelection>,
    maxNumberOfSlices: Int = MAXIMUM_NUMBER_OF_SLICES
): ModelTranslation {
    val allSlices = computeAllSlices(selectors, model.propertyStore.allDefinitions(), maxNumberOfSlices)
    val sliceSets = computeSliceSets(allSlices, model)
    return ModelTranslation(sliceSets.map { transpileSliceSet(f, it) })
}

fun transpileConstraint(f: FormulaFactory, constraint: Constraint, info: TranspilerCoreInfo): Formula =
    when (constraint) {
        is Constant -> f.constant(constraint.value)
        is BooleanFeature ->
            if (info.booleanVariables.contains(f.variable(constraint.fullName))) {
                f.variable(constraint.fullName)
            } else {
                f.falsum()
            }
        is Not -> f.not(transpileConstraint(f, constraint.operand, info))
        is Implication -> f.implication(
            transpileConstraint(f, constraint.left, info),
            transpileConstraint(f, constraint.right, info)
        )
        is Equivalence -> f.equivalence(
            transpileConstraint(f, constraint.left, info),
            transpileConstraint(f, constraint.right, info)
        )
        is And -> f.and(constraint.operands.map { transpileConstraint(f, it, info) })
        is Or -> f.or(constraint.operands.map { transpileConstraint(f, it, info) })
        is Amo -> f.amo(filterFeatures(f, constraint.features, info))
        is Exo -> f.exo(filterFeatures(f, constraint.features, info))
        is EnumComparisonPredicate -> info.translateEnumComparison(f, constraint)
        is EnumInPredicate -> info.translateEnumIn(f, constraint)
        is IntComparisonPredicate -> info.translateIntComparison(f, constraint)
        is IntInPredicate -> info.translateIntIn(f, constraint)
        is VersionPredicate -> TODO()
    }


fun mergeSlices(f: FormulaFactory, slices: List<SliceTranslation>): MergedSliceTranslation {
    val knownVariables = slices.flatMap { it.knownVariables }.toSortedSet()
    val sliceSelectors = mutableMapOf<String, SliceTranslation>()
    val propositions = mutableListOf<PrlProposition>()
    val enumMapping = mutableMapOf<String, MutableMap<String, Variable>>()
    val intMapping = mutableMapOf<String, MutableMap<Int, Variable>>()
    val unknownFeatures = slices[0].unknownFeatures.toMutableSet()
    val booleanVariables = mutableSetOf<Variable>()

    var count = 0
    slices.forEach { slice ->
        val selector = "$SLICE_SELECTOR_PREFIX${count++}"
        sliceSelectors[selector] = slice
        val substitution = Substitution()
        knownVariables.forEach { kVar ->
            val sVar = f.variable("${selector}_${kVar.name()}")
            propositions.add(
                PrlProposition(
                    RuleInformation(RuleType.FEATURE_EQUIVALENCE_OVER_SLICES, slice.sliceSet),
                    f.equivalence(kVar, sVar)
                )
            )
            if (kVar in slice.knownVariables) {
                substitution.addMapping(kVar, sVar)
            } else {
                propositions.add(
                    PrlProposition(
                        RuleInformation(RuleType.UNKNOWN_FEATURE_IN_SLICE, slice.sliceSet),
                        sVar.negate(f)
                    )
                )
            }
        }
        booleanVariables.addAll(slice.info.booleanVariables)
        unknownFeatures.retainAll(slice.unknownFeatures)
        slice.enumMapping.forEach { (feature, varMap) ->
            enumMapping.computeIfAbsent(feature) { mutableMapOf() }.putAll(varMap)
        }
        slice.propositions.forEach { propositions.add(it.substitute(f, substitution)) }
    }
    return MergedSliceTranslation(
        sliceSelectors,
        TranslationInfo(propositions, knownVariables, booleanVariables, enumMapping, intMapping, unknownFeatures)
    )
}

fun transpileSliceSet(f: FormulaFactory, sliceSet: SliceSet): SliceTranslation {
    val intStore = if (sliceSet.hasIntFeatures()) initIntegerStore(sliceSet.rules) else null
    val state = initState(f, sliceSet, intStore)
    val propositions = sliceSet.rules.map { transpileRule(f, it, sliceSet, state) }.toMutableList()
    propositions += enumPropositions(f, state, sliceSet)
    propositions += intPropositions(f, state, sliceSet)
    return SliceTranslation(sliceSet, state.toTranslationInfo(propositions))
}

private fun enumPropositions(f: FormulaFactory, state: TranspilerState, sliceSet: SliceSet) =
    state.enumMapping.values.map {
        PrlProposition(
            RuleInformation(ENUM_FEATURE_CONSTRAINT, sliceSet),
            f.exo(it.values)
        )
    }

private fun intPropositions(f: FormulaFactory, state: TranspilerState, sliceSet: SliceSet): List<PrlProposition> {
    val props = mutableListOf<PrlProposition>()
    sliceSet.definitions
        .filterIsInstance<IntFeatureDefinition>()
        .forEach { ft ->
            val range = ft.feature.domain
            val validFeatures = mutableListOf<Variable>()
            //TODO feature kommt nicht vor
            state.intMapping[ft.feature.fullName]!!.forEach { (value, variable) ->
                if (range.contains(value)) {
                    validFeatures.add(variable)
                } else {
                    props.add(
                        PrlProposition(
                            RuleInformation(INT_OUT_OF_BOUNDS_CONSTRAINT, sliceSet),
                            variable.negate(f)
                        )
                    )
                }
            }
            props.add(PrlProposition(RuleInformation(INT_FEATURE_CONSTRAINT, sliceSet), f.exo(validFeatures)))
        }
    return props
}

private fun initState(f: FormulaFactory, sliceSet: SliceSet, intStore: IntegerStore?) = TranspilerState().apply {
    val allDefinitions = sliceSet.definitions.associateBy { it.feature.featureCode }
    allDefinitions.values.forEach { featureMap[it.feature.fullName] = it }
    sliceSet.rules.flatMap { it.features() }.filter { allDefinitions[it.featureCode] == null }
        .forEach { unknownFeatures.add(it) }
    booleanVariables.addAll(
        featureMap.values.filterIsInstance<BooleanFeatureDefinition>().map { f.variable(it.feature.fullName) })
    featureMap.values.filterIsInstance<EnumFeatureDefinition>()
        .forEach { def ->
            enumMapping[def.feature.fullName] =
                def.values.associateWith { enumFeature(f, def.feature.fullName, it) }
        }
    intStore?.getSimpleFeatures()?.forEach { usage ->
        intMapping[usage.feature.fullName] =
            intStore.relevantValues(usage.feature).associateWith { intFeature(f, usage.feature.fullName, it) }
    }
}

private fun enumFeature(f: FormulaFactory, feature: String, value: String) = f.variable(
    "$ENUM_FEATURE_PREFIX$S${feature.replace(" ", S).replace(".", "#")}" + "$S${value.replace(" ", S)}"
)

private fun intFeature(f: FormulaFactory, feature: String, value: Int): Variable {
    val valueString = if (value >= 0) "$value" else "m${abs(value)}"
    return f.variable("$INT_FEATURE_PREFIX$S${feature.replace(" ", S).replace(".", "#")}" + "$S${valueString}")
}

private fun transpileRule(
    f: FormulaFactory,
    r: AnyRule,
    sliceSet: SliceSet,
    state: TranspilerState
): PrlProposition =
    when (r) {
        is ConstraintRule -> transpileConstraint(f, r.constraint, state)
        is DefinitionRule -> f.equivalence(
            transpileConstraint(f, r.feature, state),
            transpileConstraint(f, r.definition, state)
        )
        is ExclusionRule -> f.implication(
            transpileConstraint(f, r.ifConstraint, state),
            transpileConstraint(f, r.thenNotConstraint, state).negate(f)
        )
        is ForbiddenFeatureRule -> transpileConstraint(f, r.constraint, state)
        is MandatoryFeatureRule -> transpileConstraint(f, r.constraint, state)
        is GroupRule -> transpileGroupRule(f, r, state)
        is InclusionRule -> f.implication(
            transpileConstraint(f, r.ifConstraint, state),
            transpileConstraint(f, r.thenConstraint, state)
        )
        is IfThenElseRule -> transpileConstraint(f, r.ifConstraint, state).let { ifPart ->
            f.or(
                f.and(ifPart, transpileConstraint(f, r.thenConstraint, state)),
                f.and(ifPart.negate(f), transpileConstraint(f, r.elseConstraint, state))
            )
        }
    }.let { PrlProposition(RuleInformation(r, sliceSet), it) }

private fun transpileGroupRule(f: FormulaFactory, rule: GroupRule, state: TranspilerState): Formula {
    val content = filterFeatures(f, rule.content, state)
    val group =
        if (state.featureMap.containsKey(rule.group.fullName)) f.variable(rule.group.fullName) else f.falsum()
    val cc = if (rule.type == GroupType.MANDATORY) f.exo(content) else f.amo(content)
    return f.and(cc, f.equivalence(group, f.or(content)))
}

private fun filterFeatures(f: FormulaFactory, fs: Collection<BooleanFeature>, info: TranspilerCoreInfo) =
    fs.filter { info.booleanVariables.contains(f.variable(it.fullName)) }.map { f.variable(it.fullName) }

private fun initIntegerStore(rules: List<AnyRule>): IntegerStore {
    val intStore = IntegerStore()
    rules.forEach { addRuleToIntStore(it, intStore) }
    return intStore
}

fun addRuleToIntStore(rule: AnyRule, intStore: IntegerStore) {
    when (rule) {
        is ConstraintRule -> addContraintsToIntStore(intStore, rule.constraint)
        is DefinitionRule -> addContraintsToIntStore(intStore, rule.definition)
        is ExclusionRule -> addContraintsToIntStore(intStore, rule.ifConstraint, rule.thenNotConstraint)
        is ForbiddenFeatureRule -> addContraintsToIntStore(intStore, rule.constraint)
        is MandatoryFeatureRule -> addContraintsToIntStore(intStore, rule.constraint)
        is IfThenElseRule -> addContraintsToIntStore(
            intStore, rule.ifConstraint, rule.thenConstraint, rule.elseConstraint
        )
        is InclusionRule -> addContraintsToIntStore(intStore, rule.ifConstraint, rule.thenConstraint)
        is GroupRule -> {}
    }
}

fun addContraintsToIntStore(intStore: IntegerStore, vararg constraints: Constraint) {
    constraints.forEach { addContraintToIntStore(intStore, it) }
}

fun addContraintToIntStore(intStore: IntegerStore, constraint: Constraint) {
    when (constraint) {
        is Not -> addContraintsToIntStore(intStore, constraint.operand)
        is Equivalence -> addContraintsToIntStore(intStore, constraint.left, constraint.right)
        is Implication -> addContraintsToIntStore(intStore, constraint.left, constraint.right)
        is And -> addContraintsToIntStore(intStore, *constraint.operands.toTypedArray())
        is Or -> addContraintsToIntStore(intStore, *constraint.operands.toTypedArray())
        is IntComparisonPredicate -> intStore.addUsage(constraint)
        is IntInPredicate -> intStore.addUsage(constraint)
        else -> {}
    }
}

/**
 * Internal transpiler state for a single slice.
 * @property featureMap a mapping from a feature's full name to its feature
 *                      definition in the current slice
 * @property unknownFeatures a list of features not defined in the current slice
 *                           but used in a rule
 * @property booleanVariables the boolean variables with their original name
 * @property enumMapping a mapping from enum feature to its values mapped to
 *                       their respective variable
 * @property intMapping a mapping from int feature to its values mapped to
 *                       their respective variable
 */
data class TranspilerState(
    val featureMap: MutableMap<String, AnyFeatureDef> = mutableMapOf(),
    override val unknownFeatures: MutableSet<Feature> = mutableSetOf(),
    override val booleanVariables: MutableSet<Variable> = mutableSetOf(),
    override val enumMapping: MutableMap<String, Map<String, Variable>> = mutableMapOf(),
    override val intMapping: MutableMap<String, Map<Int, Variable>> = mutableMapOf()
) : TranspilerCoreInfo {
    private fun knownVariables() = (booleanVariables + enumMapping.values.flatMap { it.values }).toSortedSet()
    fun toTranslationInfo(propositions: List<PrlProposition>) =
        TranslationInfo(propositions, knownVariables(), booleanVariables, enumMapping, intMapping, unknownFeatures)
}

