// SPDX-License-Identifier: MIT
// Copyright 2023 BooleWorks GmbH

package com.booleworks.prl.transpiler

import com.booleworks.logicng.csp.CspFactory
import com.booleworks.logicng.csp.IntegerDomain
import com.booleworks.logicng.csp.IntegerRangeDomain
import com.booleworks.logicng.csp.IntegerSetDomain
import com.booleworks.logicng.csp.encodings.CspEncodingContext
import com.booleworks.logicng.csp.predicates.ComparisonPredicate
import com.booleworks.logicng.csp.terms.IntegerVariable
import com.booleworks.logicng.csp.terms.Term
import com.booleworks.logicng.datastructures.Substitution
import com.booleworks.logicng.formulas.Formula
import com.booleworks.logicng.formulas.FormulaFactory
import com.booleworks.logicng.formulas.Variable
import com.booleworks.prl.compiler.FeatureStore
import com.booleworks.prl.model.BooleanFeatureDefinition
import com.booleworks.prl.model.EmptyIntRange
import com.booleworks.prl.model.EnumFeatureDefinition
import com.booleworks.prl.model.IntFeatureDefinition
import com.booleworks.prl.model.IntInterval
import com.booleworks.prl.model.IntList
import com.booleworks.prl.model.PrlModel
import com.booleworks.prl.model.PropertyRange
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
import com.booleworks.prl.transpiler.RuleType.UNKNOWN_FEATURE_IN_SLICE

const val S = "_"
const val SLICE_SELECTOR_PREFIX = "@SL"
const val ENUM_FEATURE_PREFIX = "@ENUM"
const val PREDICATE_PREFIX = "@PREDICATE"
const val FEATURE_DEF_PREFIX = "@DEF"
const val VERSION_FEATURE_PREFIX = "@VER"

fun transpileModel(
    cf: CspFactory,
    model: PrlModel,
    selectors: List<AnySliceSelection>,
    maxNumberOfSlices: Int = MAXIMUM_NUMBER_OF_SLICES
): ModelTranslation {
    val allSlices = computeAllSlices(selectors, model.propertyStore.allDefinitions(), maxNumberOfSlices)
    val sliceSets = computeSliceSets(allSlices, model)
    val context = CspEncodingContext()
    val intVarDefinitions = encodeIntFeatures(context, cf, model.featureStore)
    return ModelTranslation(sliceSets.map { transpileSliceSet(context, cf, intVarDefinitions, it) })
}

fun encodeIntFeatures(
    context: CspEncodingContext,
    cf: CspFactory,
    store: FeatureStore
): IntFeatureEncodingStore = IntFeatureEncodingStore(store.intFeatures.values.associate { defs ->
    val reference = defs.first().code
    val map1 = defs.mapIndexed { index, d ->
        val def = (d as IntFeatureDefinition)
        Pair(
            def,
            LngIntVariable(
                def.feature.featureCode,
                cf.variable(
                    "$FEATURE_DEF_PREFIX$index$S${def.feature.featureCode}",
                    transpileDomain(def.domain)
                )
            )
        )
    }.toMap().toMutableMap()
    val map2 = map1.values.associateBy(LngIntVariable::variable) { v ->
        val clauses = cf.encodeVariable(v.variable, context)
        cf.formulaFactory().and(clauses)
    }.toMutableMap()
    Pair(reference, IntFeatureEncodingInfo(map1, map2))
})

fun transpileConstraint(
    cf: CspFactory,
    constraint: Constraint,
    info: TranspilerCoreInfo,
    integerEncodings: IntFeatureEncodingStore
): Formula =
    when (constraint) {
        is Constant -> cf.formulaFactory().constant(constraint.value)
        is BooleanFeature ->
            if (info.booleanVariables.contains(cf.formulaFactory().variable(constraint.featureCode))) {
                cf.formulaFactory().variable(constraint.featureCode)
            } else {
                cf.formulaFactory().falsum()
            }
        is Not -> cf.formulaFactory().not(transpileConstraint(cf, constraint.operand, info, integerEncodings))
        is Implication -> cf.formulaFactory().implication(
            transpileConstraint(cf, constraint.left, info, integerEncodings),
            transpileConstraint(cf, constraint.right, info, integerEncodings)
        )
        is Equivalence -> cf.formulaFactory().equivalence(
            transpileConstraint(cf, constraint.left, info, integerEncodings),
            transpileConstraint(cf, constraint.right, info, integerEncodings)
        )
        is And -> cf.formulaFactory()
            .and(constraint.operands.map { transpileConstraint(cf, it, info, integerEncodings) })
        is Or -> cf.formulaFactory().or(constraint.operands.map { transpileConstraint(cf, it, info, integerEncodings) })
        is Amo -> cf.formulaFactory().amo(filterFeatures(cf.formulaFactory(), constraint.features, info))
        is Exo -> cf.formulaFactory().exo(filterFeatures(cf.formulaFactory(), constraint.features, info))
        is EnumComparisonPredicate -> info.translateEnumComparison(cf.formulaFactory(), constraint)
        is EnumInPredicate -> info.translateEnumIn(cf.formulaFactory(), constraint)
        is IntComparisonPredicate -> info.intPredicateMapping[constraint]
            ?: cf.formulaFactory().and(
                cf.encodeConstraint(
                    transpileIntComparisonPredicate(cf, integerEncodings, info.featureInstantiations, constraint),
                    info.encodingContext
                )
            )
        is IntInPredicate -> info.intPredicateMapping[constraint]
            ?: cf.formulaFactory().and(
                cf.encodeConstraint(
                    transpileIntInPredicate(cf, integerEncodings, info.featureInstantiations, constraint),
                    info.encodingContext,
                )
            )
        is VersionPredicate -> translateVersionComparison(cf, constraint)
    }


fun mergeSlices(cf: CspFactory, slices: List<SliceTranslation>): MergedSliceTranslation {
    val f = cf.formulaFactory()
    val knownVariables = slices.flatMap { it.knownVariables }.toSortedSet()
    val sliceSelectors = mutableMapOf<String, SliceTranslation>()
    val propositions = mutableListOf<PrlProposition>()
    val enumMapping = mutableMapOf<String, MutableMap<String, Variable>>()
    val unknownFeatures = slices[0].unknownFeatures.toMutableSet()
    val booleanVariables = mutableSetOf<Variable>()
    val intPredicateMapping = mutableMapOf<IntPredicate, Variable>()
    val encodingContext = CspEncodingContext(slices[0].info.encodingContext)
    val integerEncodings = slices[0].info.integerEncodings.clone()

    val instantiation = mergeFeatureInstantiations(slices)
    instantiation.integerFeatures.values.forEach {
        integerEncodings.getInfo(it.code)!!.addDefinition(it, encodingContext, cf)
    }
    val mergedVarMap = instantiation.integerFeatures.mapValues { (_, v) -> integerEncodings.getVariable(v)!! }
    val integerVariables = mergedVarMap.values.toSet()
    integerVariables.forEach {
        propositions.add(PrlProposition(RuleInformation(INTEGER_VARIABLE), integerEncodings.getEncoding(it)!!))
    }

    var count = 0
    slices.forEach { slice ->
        val selector = "$SLICE_SELECTOR_PREFIX${count++}"
        sliceSelectors[selector] = slice
        val substitution = Substitution()
        knownVariables.forEach { kVar ->
            val sVar = f.variable("${selector}_${kVar.name()}")
            propositions.add(
                PrlProposition(
                    RuleInformation(FEATURE_EQUIVALENCE_OVER_SLICES, slice.sliceSet),
                    f.equivalence(kVar, sVar)
                )
            )
            if (kVar in slice.knownVariables) {
                substitution.addMapping(kVar, sVar)
            } else {
                propositions.add(
                    PrlProposition(
                        RuleInformation(UNKNOWN_FEATURE_IN_SLICE, slice.sliceSet),
                        sVar.negate(f)
                    )
                )
            }
        }
        propositions += slice.integerVariables.map {
            createIntVariableEquivalence(
                it.variable,
                mergedVarMap[it.feature]!!.variable,
                encodingContext,
                f
            )
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
        slice.propositions.forEach { propositions.add(it.substitute(f, substitution)) }
    }

    return MergedSliceTranslation(
        sliceSelectors,
        TranslationInfo(
            propositions,
            knownVariables,
            integerEncodings,
            instantiation,
            booleanVariables,
            integerVariables,
            enumMapping,
            unknownFeatures,
            intPredicateMapping,
            encodingContext,
            mapOf() //TODO merged version features
        )
    )
}

fun mergeFeatureInstantiations(slices: List<SliceTranslation>): FeatureInstantiation {
    val booleanFeatureDefs = mutableMapOf<String, MutableList<BooleanFeatureDefinition>>()
    slices.flatMap { it.info.featureInstantiations.booleanFeatures.entries }
        .groupByTo(booleanFeatureDefs, { it.key }, { it.value })
    val booleanFeatureInstantiations =
        booleanFeatureDefs.mapValues { (_, v) -> BooleanFeatureDefinition.merge(v) }.toMutableMap()

    val enumFeatureDefs = mutableMapOf<String, MutableList<EnumFeatureDefinition>>()
    slices.flatMap { it.info.featureInstantiations.enumFeatures.entries }
        .groupByTo(enumFeatureDefs, { it.key }, { it.value })
    val enumFeatureInstantiations =
        enumFeatureDefs.mapValues { (_, v) -> EnumFeatureDefinition.merge(v) }.toMutableMap()

    val intFeatureDefs = mutableMapOf<String, MutableList<IntFeatureDefinition>>()
    slices.flatMap { it.info.featureInstantiations.integerFeatures.entries }
        .groupByTo(intFeatureDefs, { it.key }, { it.value })
    val intFeatureInstantiations = intFeatureDefs.mapValues { (_, v) -> IntFeatureDefinition.merge(v) }.toMutableMap()

    return FeatureInstantiation(booleanFeatureInstantiations, enumFeatureInstantiations, intFeatureInstantiations)
}

fun createIntVariableEquivalence(
    sliceVariable: IntegerVariable,
    mergedVar: IntegerVariable,
    encodingContext: CspEncodingContext,
    f: FormulaFactory
): PrlProposition {
    val constraints = mutableListOf<Formula>();
    val mergedDomain = mergedVar.domain;
    val sliceDomain = sliceVariable.domain;
    var mergedCounter = 0
    var sliceCounter = 0
    var c = sliceDomain.lb();
    var lastGeneralVar: Variable? = null;
    while (c < sliceDomain.ub()) {
        if (sliceDomain.contains(c)) {
            val vEncodedVar = encodingContext.variableMap[sliceVariable]!![sliceCounter]!!
            if (c < mergedDomain.lb()) {
                constraints.add(vEncodedVar.negate(f))
            } else if (c >= mergedDomain.ub()) {
                constraints.add(vEncodedVar)
            } else if (mergedDomain.contains(c)) {
                val generalEncodedVar = encodingContext.variableMap[mergedVar]!![mergedCounter]!!
                constraints.add(f.equivalence(generalEncodedVar, vEncodedVar))
                lastGeneralVar = generalEncodedVar
                ++mergedCounter
            } else {
                if (lastGeneralVar == null) {
                    constraints.add(vEncodedVar.negate(f))
                } else {
                    constraints.add(f.implication(vEncodedVar, lastGeneralVar))
                }
            }
            ++sliceCounter
        }
        ++c
    }
    return PrlProposition(RuleInformation(FEATURE_EQUIVALENCE_OVER_SLICES), f.and(constraints))
}

fun transpileSliceSet(
    context: CspEncodingContext,
    cf: CspFactory,
    integerEncodings: IntFeatureEncodingStore,
    sliceSet: SliceSet
): SliceTranslation {
    val f = cf.formulaFactory()
    val versionStore = if (sliceSet.hasVersionFeatures()) initVersionStore(sliceSet.rules) else null
    val state = initState(context, cf, sliceSet, integerEncodings, versionStore)
    val propositions = sliceSet.rules.map { transpileRule(cf, it, sliceSet, state, integerEncodings) }.toMutableList()
    propositions += state.enumMapping.values.map {
        PrlProposition(
            RuleInformation(ENUM_FEATURE_CONSTRAINT, sliceSet),
            f.exo(it.values)
        )
    }
    propositions += state.intPredicateMapping.entries.map {
        val lngPredicate = transpileIntPredicate(cf, integerEncodings, state.featureInstantiations, it.key)
        val clauses = cf.encodeConstraint(lngPredicate, context)
        val formula = f.equivalence(it.value, f.and(clauses))
        PrlProposition(RuleInformation(PREDICATE_DEFINITION, sliceSet), formula)
    }
    propositions += state.integerVariables.map {
        PrlProposition(
            RuleInformation(INTEGER_VARIABLE),
            integerEncodings.getEncoding(it)!!
        )
    }
    if (versionStore != null) {
        propositions += versionPropositions(f, sliceSet, versionStore)
    }
    return SliceTranslation(sliceSet, state.toTranslationInfo(propositions, integerEncodings))
}

private fun initState(
    context: CspEncodingContext, cf: CspFactory, sliceSet: SliceSet, integerEncodings:
    IntFeatureEncodingStore, versionStore: VersionStore?
) =
    TranspilerState(
        featureInstantiations = getFeatureInstantiations(sliceSet),
        intPredicateMapping = getAllIntPredicates(cf.formulaFactory(), sliceSet),
        encodingContext = context
    ).apply {
        sliceSet.rules.flatMap { it.features() }.filter { featureInstantiations[it] == null }
            .forEach { unknownFeatures.add(it) }
        booleanVariables.addAll(
            featureInstantiations.booleanFeatures.values.map { cf.formulaFactory().variable(it.feature.featureCode) })
        featureInstantiations.enumFeatures.values
            .forEach { def ->
                enumMapping[def.feature.featureCode] =
                    def.values.associateWith { enumFeature(cf.formulaFactory(), def.feature.featureCode, it) }
            }
        integerVariables.addAll(
            featureInstantiations.integerFeatures.values
                .map { integerEncodings.getVariable(featureInstantiations[it.feature]!!)!! }
        )
        versionStore?.usedValues?.forEach { (fea, maxVer) ->
            versionMapping[fea.featureCode] = (1..maxVer).associateWith { installed(cf.formulaFactory(), fea, it) }
        }
    }

fun getFeatureInstantiations(sliceSet: SliceSet): FeatureInstantiation {
    val booleanMap = sliceSet.definitions.filterIsInstance<BooleanFeatureDefinition>().associateBy { it.code }
    val enumMap = sliceSet.definitions.filterIsInstance<EnumFeatureDefinition>().associateBy { it.code }
    val intMap = sliceSet.definitions.filterIsInstance<IntFeatureDefinition>().associateBy { it.code }
    return FeatureInstantiation(booleanMap, enumMap, intMap)
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
        is VersionPredicate -> {}
    }
}

private fun enumFeature(f: FormulaFactory, feature: String, value: String) = f.variable(
    "$ENUM_FEATURE_PREFIX$S${feature.replace(" ", S).replace(".", "#")}" + "$S${value.replace(" ", S)}"
)

private fun transpileRule(
    cf: CspFactory,
    r: AnyRule,
    sliceSet: SliceSet,
    state: TranspilerState,
    integerEncodings: IntFeatureEncodingStore
): PrlProposition =
    when (r) {
        is ConstraintRule -> transpileConstraint(cf, r.constraint, state, integerEncodings)
        is DefinitionRule -> cf.formulaFactory().equivalence(
            transpileConstraint(cf, r.feature, state, integerEncodings),
            transpileConstraint(cf, r.definition, state, integerEncodings)
        )
        is ExclusionRule -> cf.formulaFactory().implication(
            transpileConstraint(cf, r.ifConstraint, state, integerEncodings),
            transpileConstraint(cf, r.thenNotConstraint, state, integerEncodings).negate(cf.formulaFactory())
        )
        is ForbiddenFeatureRule -> transpileConstraint(cf, r.constraint, state, integerEncodings)
        is MandatoryFeatureRule -> transpileConstraint(cf, r.constraint, state, integerEncodings)
        is GroupRule -> transpileGroupRule(cf.formulaFactory(), r, state)
        is InclusionRule -> cf.formulaFactory().implication(
            transpileConstraint(cf, r.ifConstraint, state, integerEncodings),
            transpileConstraint(cf, r.thenConstraint, state, integerEncodings)
        )
        is IfThenElseRule -> transpileConstraint(cf, r.ifConstraint, state, integerEncodings).let { ifPart ->
            cf.formulaFactory().or(
                cf.formulaFactory().and(ifPart, transpileConstraint(cf, r.thenConstraint, state, integerEncodings)),
                cf.formulaFactory().and(
                    ifPart.negate(cf.formulaFactory()),
                    transpileConstraint(cf, r.elseConstraint, state, integerEncodings)
                )
            )
        }
    }.let { PrlProposition(RuleInformation(r, sliceSet), it) }

private fun transpileGroupRule(f: FormulaFactory, rule: GroupRule, state: TranspilerState): Formula {
    val content = filterFeatures(f, rule.content, state)
    val group = if (state.featureInstantiations.booleanFeatures.containsKey(rule.group.featureCode)) {
        f.variable(rule.group.featureCode)
    } else {
        f.falsum()
    }
    val cc = if (rule.type == GroupType.MANDATORY) f.exo(content) else f.amo(content)
    return f.and(cc, f.equivalence(group, f.or(content)))
}

fun transpileIntPredicate(
    cf: CspFactory,
    integerEncodings: IntFeatureEncodingStore,
    instantiation: FeatureInstantiation,
    predicate: IntPredicate
): ComparisonPredicate =
    when (predicate) {
        is IntComparisonPredicate -> transpileIntComparisonPredicate(cf, integerEncodings, instantiation, predicate)
        is IntInPredicate -> transpileIntInPredicate(cf, integerEncodings, instantiation, predicate)
    }

fun transpileIntComparisonPredicate(
    cf: CspFactory,
    integerEncodings: IntFeatureEncodingStore,
    instantiation: FeatureInstantiation,
    predicate: IntComparisonPredicate
): ComparisonPredicate {
    val left = transpileIntTerm(cf, integerEncodings, instantiation, predicate.left)
    val right = transpileIntTerm(cf, integerEncodings, instantiation, predicate.right)
    return when (predicate.comparison) {
        ComparisonOperator.EQ -> cf.eq(left, right)
        ComparisonOperator.GE -> cf.ge(left, right)
        ComparisonOperator.NE -> cf.ne(left, right)
        ComparisonOperator.LT -> cf.lt(left, right)
        ComparisonOperator.LE -> cf.le(left, right)
        ComparisonOperator.GT -> cf.gt(left, right)
    }
}

fun transpileIntInPredicate(
    cf: CspFactory,
    integerEncodings: IntFeatureEncodingStore,
    instantiation: FeatureInstantiation,
    predicate: IntInPredicate
): ComparisonPredicate {
    val term = transpileIntTerm(cf, integerEncodings, instantiation, predicate.term)
    val v = cf.auxVariable(transpileDomain(predicate.range))
    return cf.eq(term, v)
}

fun transpileIntTerm(
    cf: CspFactory,
    integerEncodings: IntFeatureEncodingStore,
    instantiation: FeatureInstantiation,
    term: IntTerm
): Term = when (term) {
    is IntValue -> cf.constant(term.value)
    is IntFeature -> transpileIntFeature(integerEncodings, instantiation, term)
    is IntMul -> cf.mul(term.coefficient, transpileIntFeature(integerEncodings, instantiation, term.feature))
    is IntSum -> cf.add(
        cf.add(term.operands.map { transpileIntMul(cf, integerEncodings, instantiation, it) }),
        cf.constant(term.offset)
    )
}

fun transpileIntMul(
    cf: CspFactory,
    integerEncodings: IntFeatureEncodingStore,
    instantiation: FeatureInstantiation,
    feature: IntMul
): Term =
    cf.mul(feature.coefficient, transpileIntFeature(integerEncodings, instantiation, feature.feature))

fun transpileIntFeature(
    integerEncodings: IntFeatureEncodingStore,
    instantiation: FeatureInstantiation,
    feature: IntFeature
) =
    integerEncodings.getVariable(instantiation[feature]!!)!!.variable

fun transpileDomain(domain: PropertyRange<Int>): IntegerDomain = when (domain) {
    is IntList, EmptyIntRange -> IntegerSetDomain(domain.allValues())
    is IntInterval -> IntegerRangeDomain(domain.first(), domain.last())
    else -> throw IllegalArgumentException("Invalid integer domain ${domain.javaClass}")
}

private fun filterFeatures(f: FormulaFactory, fs: Collection<BooleanFeature>, info: TranspilerCoreInfo) =
    fs.filter { info.booleanVariables.contains(f.variable(it.featureCode)) }.map { f.variable(it.featureCode) }

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

data class TranspilerState(
    override val featureInstantiations: FeatureInstantiation,
    override val unknownFeatures: MutableSet<Feature> = mutableSetOf(),
    override val booleanVariables: MutableSet<Variable> = mutableSetOf(),
    override val integerVariables: MutableSet<LngIntVariable> = mutableSetOf(),
    override val enumMapping: MutableMap<String, Map<String, Variable>> = mutableMapOf(),
    override val intPredicateMapping: MutableMap<IntPredicate, Variable> = mutableMapOf(),
    override val encodingContext: CspEncodingContext,
    override val versionMapping: MutableMap<String, Map<Int, Variable>> = mutableMapOf(),
) : TranspilerCoreInfo {
    private fun knownVariables() = (booleanVariables + enumMapping.values.flatMap { it.values }).toSortedSet()
    fun toTranslationInfo(propositions: List<PrlProposition>, integerEncodings: IntFeatureEncodingStore) =
        TranslationInfo(
            propositions,
            knownVariables(),
            integerEncodings,
            featureInstantiations,
            booleanVariables,
            integerVariables,
            enumMapping,
            unknownFeatures,
            intPredicateMapping,
            encodingContext,
            versionMapping
        )
}

data class IntFeatureEncodingStore(val store: Map<String, IntFeatureEncodingInfo>) : Cloneable {

    fun getEncoding(variable: LngIntVariable) =
        getInfo(variable.feature)?.getEncoding(variable.variable)

    fun getVariable(feature: IntFeatureDefinition) =
        getInfo(feature.feature.featureCode)?.getVariable(feature)

    fun getInfo(feature: String) = store[feature]

    public override fun clone() = IntFeatureEncodingStore(store.mapValues { (_, info) -> info.clone() })

    companion object {
        fun empty() = IntFeatureEncodingStore(mutableMapOf())
    }
}

data class IntFeatureEncodingInfo(
    private val featureToVar: MutableMap<IntFeatureDefinition, LngIntVariable>,
    private val encodedVars: MutableMap<IntegerVariable, Formula>,
) : Cloneable {
    fun contains(variable: IntegerVariable) = encodedVars.containsKey(variable)

    fun getEncoding(variable: IntegerVariable) = encodedVars[variable]

    fun getVariable(definition: IntFeatureDefinition) = featureToVar[definition]

    fun addDefinition(definition: IntFeatureDefinition, encodingContext: CspEncodingContext, cf: CspFactory) {
        if (!featureToVar.containsKey(definition)) {
            val varName = "$FEATURE_DEF_PREFIX${featureToVar.size}$S${definition.code}"
            val domain = transpileDomain(definition.domain)
            val variable = LngIntVariable(definition.code, cf.variable(varName, domain))
            val clauses = cf.encodeVariable(variable.variable, encodingContext)
            val encoded = cf.formulaFactory().and(clauses)
            featureToVar[definition] = variable
            encodedVars[variable.variable] = encoded
        }
    }

    public override fun clone() = IntFeatureEncodingInfo(HashMap(featureToVar), HashMap(encodedVars))

    companion object {
        fun empty() = IntFeatureEncodingInfo(mutableMapOf(), mutableMapOf())
    }
}

data class LngIntVariable(
    val feature: String,
    val variable: IntegerVariable,
)
