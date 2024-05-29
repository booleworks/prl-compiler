// SPDX-License-Identifier: MIT
// Copyright 2023 BooleWorks GmbH

package com.booleworks.prl.transpiler

import com.booleworks.logicng.datastructures.Substitution
import com.booleworks.logicng.formulas.Formula
import com.booleworks.logicng.formulas.FormulaFactory
import com.booleworks.logicng.formulas.Variable
import com.booleworks.prl.model.AnyFeatureDef
import com.booleworks.prl.model.BooleanFeatureDefinition
import com.booleworks.prl.model.EnumFeatureDefinition
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

const val S = "_"
const val SLICE_SELECTOR_PREFIX = "@SL"
const val ENUM_FEATURE_PREFIX = "@ENUM"
const val VERSION_FEATURE_PREFIX = "@VER"

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
        is VersionPredicate -> translateVersionComparison(f, constraint)
        is IntComparisonPredicate -> TODO()
        is IntInPredicate -> TODO()
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
    val versionStore = if (sliceSet.hasVersionFeatures()) initVersionStore(sliceSet.rules) else null
    val state = initState(f, sliceSet, versionStore)
    val propositions = sliceSet.rules.map { transpileRule(f, it, sliceSet, state) }.toMutableList()
    propositions += enumPropositions(f, state, sliceSet)
    if (versionStore != null) {
        propositions += versionPropositions(f, sliceSet, versionStore)
    }
    return SliceTranslation(sliceSet, state.toTranslationInfo(propositions))
}

private fun enumPropositions(f: FormulaFactory, state: TranspilerState, sliceSet: SliceSet) =
    state.enumMapping.values.map {
        PrlProposition(
            RuleInformation(ENUM_FEATURE_CONSTRAINT, sliceSet),
            f.exo(it.values)
        )
    }

private fun initState(f: FormulaFactory, sliceSet: SliceSet, versionStore: VersionStore?) = TranspilerState().apply {
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
    versionStore?.usedValues?.forEach { (fea, maxVer) ->
        versionMapping[fea.fullName] = (1..maxVer).associateWith { installed(f, fea, it) }
    }
}

private fun enumFeature(f: FormulaFactory, feature: String, value: String) = f.variable(
    "$ENUM_FEATURE_PREFIX$S${feature.replace(" ", S).replace(".", "#")}" + "$S${value.replace(" ", S)}"
)

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

private fun initVersionStore(rules: List<AnyRule>): VersionStore {
    val versionStore = VersionStore()
    rules.forEach { addRuleToVersionStore(it, versionStore) }
    return versionStore
}

fun addRuleToVersionStore(rule: AnyRule, versionStore: VersionStore) {
    when (rule) {
        is ConstraintRule -> addContraintsToVersionStore(versionStore, rule.constraint)
        is DefinitionRule -> addContraintsToVersionStore(versionStore, rule.definition)
        is ExclusionRule -> addContraintsToVersionStore(versionStore, rule.ifConstraint, rule.thenNotConstraint)
        is ForbiddenFeatureRule -> addContraintsToVersionStore(versionStore, rule.constraint)
        is MandatoryFeatureRule -> addContraintsToVersionStore(versionStore, rule.constraint)
        is IfThenElseRule -> addContraintsToVersionStore(
            versionStore, rule.ifConstraint, rule.thenConstraint, rule.elseConstraint
        )
        is InclusionRule -> addContraintsToVersionStore(versionStore, rule.ifConstraint, rule.thenConstraint)
        is GroupRule -> {}
    }
}

fun addContraintsToVersionStore(versionStore: VersionStore, vararg constraints: Constraint) {
    constraints.forEach {
        when (it) {
            is Not -> addContraintsToVersionStore(versionStore, it.operand)
            is Equivalence -> addContraintsToVersionStore(versionStore, it.left, it.right)
            is Implication -> addContraintsToVersionStore(versionStore, it.left, it.right)
            is And -> addContraintsToVersionStore(versionStore, *it.operands.toTypedArray<Constraint>())
            is Or -> addContraintsToVersionStore(versionStore, *it.operands.toTypedArray<Constraint>())
            is VersionPredicate -> versionStore.addUsage(it)
            else -> {}
        }
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
 * @property versionMapping a mapping from version feature to its values mapped to
 *                       their respective variable
 */
data class TranspilerState(
    val featureMap: MutableMap<String, AnyFeatureDef> = mutableMapOf(),
    override val unknownFeatures: MutableSet<Feature> = mutableSetOf(),
    override val booleanVariables: MutableSet<Variable> = mutableSetOf(),
    override val enumMapping: MutableMap<String, Map<String, Variable>> = mutableMapOf(),
    override val versionMapping: MutableMap<String, Map<Int, Variable>> = mutableMapOf(),
) : TranspilerCoreInfo {
    private fun knownVariables() = (booleanVariables + enumMapping.values.flatMap { it.values }).toSortedSet()
    fun toTranslationInfo(propositions: List<PrlProposition>) =
        TranslationInfo(propositions, knownVariables(), booleanVariables, enumMapping, versionMapping, unknownFeatures)
}

