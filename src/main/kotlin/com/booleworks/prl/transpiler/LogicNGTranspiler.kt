// SPDX-License-Identifier: MIT
// Copyright 2023 BooleWorks GmbH

package com.booleworks.prl.transpiler

import com.booleworks.logicng.csp.CspFactory
import com.booleworks.logicng.csp.IntegerDomain
import com.booleworks.logicng.csp.IntegerRangeDomain
import com.booleworks.logicng.csp.IntegerSetDomain
import com.booleworks.logicng.csp.encodings.CspEncoder.Algorithm
import com.booleworks.logicng.csp.encodings.CspEncodingContext
import com.booleworks.logicng.csp.predicates.ComparisonPredicate
import com.booleworks.logicng.csp.terms.IntegerVariable
import com.booleworks.logicng.csp.terms.Term
import com.booleworks.logicng.datastructures.Substitution
import com.booleworks.logicng.formulas.Formula
import com.booleworks.logicng.formulas.FormulaFactory
import com.booleworks.logicng.formulas.Variable
import com.booleworks.prl.compiler.FeatureStore
import com.booleworks.prl.model.AnyFeatureDef
import com.booleworks.prl.model.BooleanFeatureDefinition
import com.booleworks.prl.model.EmptyIntRange
import com.booleworks.prl.model.EnumFeatureDefinition
import com.booleworks.prl.model.IntFeatureDefinition
import com.booleworks.prl.model.IntInterval
import com.booleworks.prl.model.IntList
import com.booleworks.prl.model.IntRange
import com.booleworks.prl.model.PrlModel
import com.booleworks.prl.model.constraints.Amo
import com.booleworks.prl.model.constraints.And
import com.booleworks.prl.model.constraints.BooleanFeature
import com.booleworks.prl.model.constraints.ComparisonOperator
import com.booleworks.prl.model.constraints.Constant
import com.booleworks.prl.model.constraints.Constraint
import com.booleworks.prl.model.constraints.EnumComparisonPredicate
import com.booleworks.prl.model.constraints.EnumInPredicate
import com.booleworks.prl.model.constraints.Equivalence
import com.booleworks.prl.model.constraints.Exo
import com.booleworks.prl.model.constraints.Feature
import com.booleworks.prl.model.constraints.Implication
import com.booleworks.prl.model.constraints.IntComparisonPredicate
import com.booleworks.prl.model.constraints.IntFeature
import com.booleworks.prl.model.constraints.IntInPredicate
import com.booleworks.prl.model.constraints.IntMul
import com.booleworks.prl.model.constraints.IntPredicate
import com.booleworks.prl.model.constraints.IntSum
import com.booleworks.prl.model.constraints.IntTerm
import com.booleworks.prl.model.constraints.IntValue
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
import com.booleworks.prl.transpiler.RuleType.FEATURE_EQUIVALENCE_OVER_SLICES
import com.booleworks.prl.transpiler.RuleType.INTEGER_VARIABLE
import com.booleworks.prl.transpiler.RuleType.PREDICATE_DEFINITION
import java.util.TreeMap

const val S = "_"
const val SLICE_SELECTOR_PREFIX = "@SL"
const val ENUM_FEATURE_PREFIX = "@ENUM"
const val PREDICATE_PREFIX = "@PREDICATE"
const val FEATURE_DEF_PREFIX = "@DEF"

fun transpileModel(
    cf: CspFactory,
    model: PrlModel,
    selectors: List<AnySliceSelection>,
    maxNumberOfSlices: Int = MAXIMUM_NUMBER_OF_SLICES
): ModelTranslation {
    val allSlices = computeAllSlices(selectors, model.propertyStore.allDefinitions(), maxNumberOfSlices)
    val sliceSets = computeSliceSets(allSlices, model)
    val context = CspEncodingContext(cf);
    val intVarDefinitions = encodeIntFeatures(context, model.featureStore);
    return ModelTranslation(sliceSets.map { transpileSliceSet(context, intVarDefinitions, it) })
}

fun encodeIntFeatures(
    context: CspEncodingContext,
    store: FeatureStore
): IntFeatureEncodingStore = IntFeatureEncodingStore(store.intFeatures.values.associate { defs ->
    val name = defs.first().feature.fullName
    val map1 = defs.mapIndexed { index, d ->
        val def = (d as IntFeatureDefinition)
        Pair(
            def.feature,
            LngIntVariable(
                def.feature.fullName,
                context.factory().variable(
                    "$FEATURE_DEF_PREFIX$index$S${def.feature.fullName}",
                    transpileDomain(def.feature.domain)
                )
            )
        )
    }.toMap()
    val map2 = map1.values.associateBy(LngIntVariable::variable) { v ->
        val clauses = context.factory().encodeVariable(v.variable, context, Algorithm.Order)
        context.factory().formulaFactory.and(clauses)
    }
    Pair(name, IntFeatureEncodingInfo(map1, map2))
})

fun transpileConstraint(f: FormulaFactory, constraint: Constraint, info: TranspilerCoreInfo, integerEncodings: IntFeatureEncodingStore): Formula =
    when (constraint) {
        is Constant -> f.constant(constraint.value)
        is BooleanFeature ->
            if (info.booleanVariables.contains(f.variable(constraint.fullName))) {
                f.variable(constraint.fullName)
            } else {
                f.falsum()
            }

        is Not -> f.not(transpileConstraint(f, constraint.operand, info, integerEncodings))
        is Implication -> f.implication(
            transpileConstraint(f, constraint.left, info, integerEncodings),
            transpileConstraint(f, constraint.right, info, integerEncodings)
        )

        is Equivalence -> f.equivalence(
            transpileConstraint(f, constraint.left, info, integerEncodings),
            transpileConstraint(f, constraint.right, info, integerEncodings)
        )

        is And -> f.and(constraint.operands.map { transpileConstraint(f, it, info, integerEncodings) })
        is Or -> f.or(constraint.operands.map { transpileConstraint(f, it, info, integerEncodings) })
        is Amo -> f.amo(filterFeatures(f, constraint.features, info))
        is Exo -> f.exo(filterFeatures(f, constraint.features, info))
        is EnumComparisonPredicate -> info.translateEnumComparison(f, constraint)
        is EnumInPredicate -> info.translateEnumIn(f, constraint)
        is IntComparisonPredicate -> info.intPredicateMapping[constraint]
            ?: f.and(
                info.encodingContext.factory().encodeConstraint(
                    transpileIntComparisonPredicate(info.encodingContext.factory(), integerEncodings, constraint),
                    info.encodingContext,
                    Algorithm.Order
                )
            )

        is IntInPredicate -> info.intPredicateMapping[constraint]
            ?: f.and(
                info.encodingContext.factory().encodeConstraint(
                    transpileIntInPredicate(info.encodingContext.factory(), integerEncodings, constraint),
                    info.encodingContext,
                    Algorithm.Order
                )
            )

        is VersionPredicate -> TODO()
    }


fun mergeSlices(cf: CspFactory, slices: List<SliceTranslation>): MergedSliceTranslation {
    val f = cf.formulaFactory;
    val knownVariables = slices.flatMap { it.knownVariables }.toSortedSet()
    val sliceSelectors = mutableMapOf<String, SliceTranslation>()
    val propositions = mutableListOf<PrlProposition>()
    val enumMapping = mutableMapOf<String, MutableMap<String, Variable>>()
    val unknownFeatures = slices[0].unknownFeatures.toMutableSet()
    val booleanVariables = mutableSetOf<Variable>()
    val intPredicateMapping = mutableMapOf<IntPredicate, Variable>()
    val encodingContext = slices[0].info.encodingContext
    val integerEncodings = slices[0].info.integerEncodings

    val mostGeneralIntVariables = TreeMap<String, LngIntVariable>();
    slices
        .flatMap { it.integerVariables.map { v -> v.feature } }
        .distinct()
        .forEach {
            val (variable, encoded) = integerEncodings.getInfo(it)!!.getMostGeneralVariable(it, encodingContext)
            mostGeneralIntVariables[it] = variable;
            propositions.add(PrlProposition(RuleInformation(INTEGER_VARIABLE), encoded))
        }
    val integerVariables = mostGeneralIntVariables.values.toSet()

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
        slice.integerVariables.forEach { v ->
            val constraints = ArrayList<Formula>()
            val generalVar = mostGeneralIntVariables[v.feature]!!.variable
            val generalDomain = generalVar.domain;
            val vDomain = v.variable.domain;
            var generalCounter = 0
            var vCounter = 0
            var c = vDomain.lb();
            var lastGeneralVar: Variable? = null;
            while (c < vDomain.ub()) {
                if (vDomain.contains(c)) {
                    val vEncodedVar = encodingContext.variableMap[v.variable]!![vCounter]!!
                    if (c < generalDomain.lb()) {
                        constraints.add(vEncodedVar.negate(f))
                    } else if (c >= generalDomain.ub()) {
                        constraints.add(vEncodedVar)
                    } else if (generalDomain.contains(c)) {
                        val generalEncodedVar = encodingContext.variableMap[generalVar]!![generalCounter]!!
                        constraints.add(f.equivalence(generalEncodedVar, vEncodedVar))
                        lastGeneralVar = generalEncodedVar
                        ++generalCounter
                    } else {
                        if (lastGeneralVar == null) {
                            constraints.add(vEncodedVar.negate(f))
                        } else {
                            constraints.add(f.implication(vEncodedVar, lastGeneralVar))
                        }
                    }
                    ++vCounter
                }
                ++c
            }
            propositions.add(PrlProposition(RuleInformation(FEATURE_EQUIVALENCE_OVER_SLICES), cf.formulaFactory.and(constraints)))
        }
        slice.info.intPredicateMapping.forEach { (predicate, variable) ->
            val newVar = intPredicateMapping.computeIfAbsent(predicate) {
                variable
            }
            if (newVar != variable) {
                substitution.addMapping(variable, newVar)
            }
        }
        booleanVariables.addAll(slice.info.booleanVariables)
        unknownFeatures.retainAll(slice.unknownFeatures)
        slice.enumMapping.forEach { (feature, varMap) ->
            enumMapping.computeIfAbsent(feature) { mutableMapOf() }.putAll(varMap)
        }
        slice.propositions.forEach { propositions.add(it.substitute(cf.formulaFactory, substitution)) }
    }

    return MergedSliceTranslation(
        sliceSelectors,
        TranslationInfo(propositions, knownVariables, integerEncodings, booleanVariables, integerVariables, enumMapping, unknownFeatures, intPredicateMapping, encodingContext)
    )
}

fun transpileSliceSet(context: CspEncodingContext, integerEncodings: IntFeatureEncodingStore, sliceSet: SliceSet): SliceTranslation {
    val f = context.factory().formulaFactory
    val state = initState(context, sliceSet, integerEncodings)
    val propositions = sliceSet.rules.map { transpileRule(f, it, sliceSet, state, integerEncodings) }.toMutableList()
    propositions += state.enumMapping.values.map {
        PrlProposition(
            RuleInformation(ENUM_FEATURE_CONSTRAINT, sliceSet),
            f.exo(it.values)
        )
    }
    propositions += state.intPredicateMapping.entries.map {
        val lngPredicate = transpileIntPredicate(context.factory(), integerEncodings, it.key)
        val clauses = context.factory().encodeConstraint(lngPredicate, context, Algorithm.Order)
        val formula = f.equivalence(it.value, f.and(clauses))
        PrlProposition(RuleInformation(PREDICATE_DEFINITION, sliceSet), formula)
    }
    propositions += state.integerVariables.map {
        PrlProposition(
            RuleInformation(INTEGER_VARIABLE),
            integerEncodings.getEncoding(it)!!
        )
    }
    return SliceTranslation(sliceSet, state.toTranslationInfo(propositions, integerEncodings))
}

private fun initState(context: CspEncodingContext, sliceSet: SliceSet, integerEncodings: IntFeatureEncodingStore) =
    TranspilerState(
        intPredicateMapping = getAllIntPredicates(context.factory().formulaFactory, sliceSet),
        encodingContext = context
    ).apply {
        val f = context.factory().formulaFactory
        val allDefinitions = sliceSet.definitions.associateBy { it.feature.featureCode }
        allDefinitions.values.forEach { featureMap[it.feature.fullName] = it }
        sliceSet.rules.flatMap { it.features() }.filter { allDefinitions[it.featureCode] == null }
            .forEach { unknownFeatures.add(it) }
        booleanVariables.addAll(
            featureMap.values.filterIsInstance<BooleanFeatureDefinition>().map { f.variable(it.feature.fullName) })
        featureMap.values.filterIsInstance<EnumFeatureDefinition>()
            .forEach { def ->
                enumMapping[def.feature.fullName] = def.values.associateWith { enumFeature(f, def.feature.fullName, it) }
            }
        integerVariables.addAll(
            featureMap.values.filterIsInstance<IntFeatureDefinition>()
                .map { integerEncodings.getVariable(it.feature)!! }
        )
    }

fun getAllIntPredicates(f: FormulaFactory, sliceSet: SliceSet): MutableMap<IntPredicate, Variable> {
    val map = LinkedHashMap<IntPredicate, Variable>()
    sliceSet.rules.forEach { rule ->
        when (rule) {
            is ConstraintRule -> getAllIntPredicates(f, map, rule.constraint)
            is DefinitionRule -> {
                getAllIntPredicates(f, map, rule.feature)
                getAllIntPredicates(f, map, rule.definition)
            }
            is ExclusionRule -> {
                getAllIntPredicates(f, map, rule.ifConstraint)
                getAllIntPredicates(f, map, rule.thenNotConstraint)
            }
            is ForbiddenFeatureRule -> getAllIntPredicates(f, map, rule.constraint)
            is MandatoryFeatureRule -> getAllIntPredicates(f, map, rule.constraint)
            is GroupRule -> {}
            is InclusionRule -> {
                getAllIntPredicates(f, map, rule.ifConstraint)
                getAllIntPredicates(f, map, rule.thenConstraint)
            }
            is IfThenElseRule -> {
                getAllIntPredicates(f, map, rule.ifConstraint)
                getAllIntPredicates(f, map, rule.thenConstraint)
                getAllIntPredicates(f, map, rule.elseConstraint)
            }
        }
    }
    return map
}

fun getAllIntPredicates(f: FormulaFactory, map: MutableMap<IntPredicate, Variable>, constraint: Constraint) {
    when (constraint) {
        is Constant -> {}
        is BooleanFeature -> {}
        is Not -> getAllIntPredicates(f, map, constraint.operand)
        is Implication -> {
            getAllIntPredicates(f, map, constraint.left)
            getAllIntPredicates(f, map, constraint.right)
        }
        is Equivalence -> {
            getAllIntPredicates(f, map, constraint.left)
            getAllIntPredicates(f, map, constraint.right)
        }
        is And -> constraint.operands.forEach { getAllIntPredicates(f, map, it) }
        is Or -> constraint.operands.forEach { getAllIntPredicates(f, map, it) }
        is IntComparisonPredicate, is IntInPredicate -> map.computeIfAbsent(constraint as IntPredicate) { _ ->
            f.variable("${PREDICATE_PREFIX}_${map.size}")
        }
        is Amo, is Exo, is EnumComparisonPredicate, is EnumInPredicate -> {}
        is VersionPredicate -> TODO()
    }
}

private fun enumFeature(f: FormulaFactory, feature: String, value: String) = f.variable(
    "$ENUM_FEATURE_PREFIX$S${feature.replace(" ", S).replace(".", "#")}" + "$S${value.replace(" ", S)}"
)

private fun transpileRule(f: FormulaFactory, r: AnyRule, sliceSet: SliceSet, state: TranspilerState, integerEncodings: IntFeatureEncodingStore): PrlProposition =
    when (r) {
        is ConstraintRule -> transpileConstraint(f, r.constraint, state, integerEncodings)
        is DefinitionRule -> f.equivalence(
            transpileConstraint(f, r.feature, state, integerEncodings),
            transpileConstraint(f, r.definition, state, integerEncodings)
        )
        is ExclusionRule -> f.implication(
            transpileConstraint(f, r.ifConstraint, state, integerEncodings),
            transpileConstraint(f, r.thenNotConstraint, state, integerEncodings).negate(f)
        )
        is ForbiddenFeatureRule -> transpileConstraint(f, r.constraint, state, integerEncodings)
        is MandatoryFeatureRule -> transpileConstraint(f, r.constraint, state, integerEncodings)
        is GroupRule -> transpileGroupRule(f, r, state)
        is InclusionRule -> f.implication(
            transpileConstraint(f, r.ifConstraint, state, integerEncodings),
            transpileConstraint(f, r.thenConstraint, state, integerEncodings)
        )
        is IfThenElseRule -> transpileConstraint(f, r.ifConstraint, state, integerEncodings).let { ifPart ->
            f.or(
                f.and(ifPart, transpileConstraint(f, r.thenConstraint, state, integerEncodings)),
                f.and(ifPart.negate(f), transpileConstraint(f, r.elseConstraint, state, integerEncodings))
            )
        }
    }.let { PrlProposition(RuleInformation(r, sliceSet), it) }

private fun transpileGroupRule(f: FormulaFactory, rule: GroupRule, state: TranspilerState): Formula {
    val content = filterFeatures(f, rule.content, state)
    val group = if (state.featureMap.containsKey(rule.group.fullName)) f.variable(rule.group.fullName) else f.falsum()
    val cc = if (rule.type == GroupType.MANDATORY) f.exo(content) else f.amo(content)
    return f.and(cc, f.equivalence(group, f.or(content)))
}

fun transpileIntPredicate(cf: CspFactory, integerEncodings: IntFeatureEncodingStore, predicate: IntPredicate): ComparisonPredicate =
    when (predicate) {
        is IntComparisonPredicate -> transpileIntComparisonPredicate(cf, integerEncodings, predicate)
        is IntInPredicate -> transpileIntInPredicate(cf, integerEncodings, predicate)
    }

fun transpileIntComparisonPredicate(cf: CspFactory, integerEncodings: IntFeatureEncodingStore, predicate: IntComparisonPredicate): ComparisonPredicate {
    val left = transpileIntTerm(cf, integerEncodings, predicate.left)
    val right = transpileIntTerm(cf, integerEncodings, predicate.right)
    return when (predicate.comparison) {
        ComparisonOperator.EQ -> cf.eq(left, right)
        ComparisonOperator.GE -> cf.ge(left, right)
        ComparisonOperator.NE -> cf.ne(left, right)
        ComparisonOperator.LT -> cf.lt(left, right)
        ComparisonOperator.LE -> cf.le(left, right)
        ComparisonOperator.GT -> cf.gt(left, right)
    }
}

fun transpileIntInPredicate(cf: CspFactory, integerEncodings: IntFeatureEncodingStore, predicate: IntInPredicate): ComparisonPredicate {
    val term = transpileIntTerm(cf, integerEncodings, predicate.term)
    val v = cf.auxVariable(transpileDomain(predicate.range))
    return cf.eq(term, v)
}

fun transpileIntTerm(cf: CspFactory, integerEncodings: IntFeatureEncodingStore, term: IntTerm): Term = when (term) {
    is IntValue -> cf.constant(term.value)
    is IntFeature -> transpileIntFeature(integerEncodings, term)
    is IntMul -> cf.mul(term.coefficient, transpileIntFeature(integerEncodings, term.feature))
    is IntSum -> cf.add(cf.add(term.operands.map { transpileIntMul(cf, integerEncodings, it) }), cf.constant(term.offset))
}

fun transpileIntMul(cf: CspFactory, integerEncodings: IntFeatureEncodingStore, feature: IntMul): Term =
    cf.mul(feature.coefficient, transpileIntFeature(integerEncodings, feature.feature))

fun transpileIntFeature(integerEncodings: IntFeatureEncodingStore, feature: IntFeature) = integerEncodings.getVariable(feature)!!.variable

fun transpileDomain(domain: IntRange): IntegerDomain = when (domain) {
    is IntList, EmptyIntRange -> IntegerSetDomain(domain.allValues())
    is IntInterval -> IntegerRangeDomain(domain.first(), domain.last())
}

private fun filterFeatures(f: FormulaFactory, fs: Collection<BooleanFeature>, info: TranspilerCoreInfo) =
    fs.filter { info.booleanVariables.contains(f.variable(it.fullName)) }.map { f.variable(it.fullName) }

/**
 * Internal transpiler state for a single slice.
 * @property featureMap a mapping from a feature's full name to its feature
 *                      definition in the current slice
 * @property unknownFeatures a list of features not defined in the current slice
 *                           but used in a rule
 * @property booleanVariables the boolean variables with their original name
 * @property enumMapping a mapping from enum feature to its values mapped to
 *                       their respective variable
 */
data class TranspilerState(
    val featureMap: MutableMap<String, AnyFeatureDef> = mutableMapOf(),
    override val unknownFeatures: MutableSet<Feature> = mutableSetOf(),
    override val booleanVariables: MutableSet<Variable> = mutableSetOf(),
    override val integerVariables: MutableSet<LngIntVariable> = mutableSetOf(),
    override val enumMapping: MutableMap<String, Map<String, Variable>> = mutableMapOf(),
    override val intPredicateMapping: MutableMap<IntPredicate, Variable> = mutableMapOf(),
    override val encodingContext: CspEncodingContext
) : TranspilerCoreInfo {
    private fun knownVariables() = (booleanVariables + enumMapping.values.flatMap { it.values }).toSortedSet()
    fun toTranslationInfo(propositions: List<PrlProposition>, integerEncodings: IntFeatureEncodingStore) =
        TranslationInfo(propositions, knownVariables(), integerEncodings, booleanVariables, integerVariables, enumMapping, unknownFeatures, intPredicateMapping, encodingContext)
}

data class IntFeatureEncodingStore(val store: Map<String, IntFeatureEncodingInfo>) {
    fun getEncoding(variable: LngIntVariable) = store[variable.feature]?.encodedVars?.get(variable.variable)
    fun getVariable(feature: IntFeature) = store[feature.fullName]?.featureToVar?.get(feature)
    fun getInfo(featureCode: String) = store[featureCode]
}

data class IntFeatureEncodingInfo(
    val featureToVar: Map<IntFeature, LngIntVariable>,
    val encodedVars: Map<IntegerVariable, Formula>,
) {
    var mostGeneralVariable: Pair<LngIntVariable, Formula>? = null
    fun contains(variable: IntegerVariable) = encodedVars.containsKey(variable)

    fun getMostGeneralVariable(name: String, encodingContext: CspEncodingContext): Pair<LngIntVariable, Formula> {
        if (mostGeneralVariable == null) {
            val domain = encodedVars.keys.map { it.domain }.reduce { a, b -> a.cap(b) }
            val v = LngIntVariable(name, encodingContext.factory().variable(name, domain))
            val clauses = encodingContext.factory().encodeVariable(v.variable, encodingContext, Algorithm.Order)
            val formula = encodingContext.factory().formulaFactory.and(clauses)
            mostGeneralVariable = Pair(v, formula)
        }
        return mostGeneralVariable!!
    }
}

data class LngIntVariable(
    val feature: String,
    val variable: IntegerVariable,
)
