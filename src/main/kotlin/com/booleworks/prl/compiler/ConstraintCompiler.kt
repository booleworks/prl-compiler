// SPDX-License-Identifier: MIT
// Copyright 2023 BooleWorks GmbH

package com.booleworks.prl.compiler

import com.booleworks.prl.compiler.ConstraintCompiler.PredicateType.BOOLEAN
import com.booleworks.prl.compiler.ConstraintCompiler.PredicateType.ENUM
import com.booleworks.prl.compiler.ConstraintCompiler.PredicateType.INT
import com.booleworks.prl.model.AnyFeatureDef
import com.booleworks.prl.model.BooleanFeatureDefinition
import com.booleworks.prl.model.EnumFeatureDefinition
import com.booleworks.prl.model.IntFeatureDefinition
import com.booleworks.prl.model.constraints.BooleanFeature
import com.booleworks.prl.model.constraints.ComparisonOperator
import com.booleworks.prl.model.constraints.Constraint
import com.booleworks.prl.model.constraints.EnumFeature
import com.booleworks.prl.model.constraints.IntFeature
import com.booleworks.prl.model.constraints.IntMul
import com.booleworks.prl.model.constraints.IntTerm
import com.booleworks.prl.model.constraints.amo
import com.booleworks.prl.model.constraints.and
import com.booleworks.prl.model.constraints.constant
import com.booleworks.prl.model.constraints.enumComparison
import com.booleworks.prl.model.constraints.enumIn
import com.booleworks.prl.model.constraints.enumVal
import com.booleworks.prl.model.constraints.equiv
import com.booleworks.prl.model.constraints.exo
import com.booleworks.prl.model.constraints.impl
import com.booleworks.prl.model.constraints.intComparison
import com.booleworks.prl.model.constraints.intIn
import com.booleworks.prl.model.constraints.intMul
import com.booleworks.prl.model.constraints.intSum
import com.booleworks.prl.model.constraints.intVal
import com.booleworks.prl.model.constraints.not
import com.booleworks.prl.model.constraints.or
import com.booleworks.prl.model.constraints.versionComparison
import com.booleworks.prl.parser.PrlAmo
import com.booleworks.prl.parser.PrlAnd
import com.booleworks.prl.parser.PrlComparisonPredicate
import com.booleworks.prl.parser.PrlConstant
import com.booleworks.prl.parser.PrlConstraint
import com.booleworks.prl.parser.PrlEnumValue
import com.booleworks.prl.parser.PrlEquivalence
import com.booleworks.prl.parser.PrlExo
import com.booleworks.prl.parser.PrlFeature
import com.booleworks.prl.parser.PrlImplication
import com.booleworks.prl.parser.PrlInEnumsPredicate
import com.booleworks.prl.parser.PrlInIntRangePredicate
import com.booleworks.prl.parser.PrlIntAddFunction
import com.booleworks.prl.parser.PrlIntMulFunction
import com.booleworks.prl.parser.PrlIntValue
import com.booleworks.prl.parser.PrlNot
import com.booleworks.prl.parser.PrlOr
import com.booleworks.prl.parser.PrlTerm

class CoCoException(message: String) : Exception(message)

class ConstraintCompiler {
    fun compileConstraint(constraint: PrlConstraint, featureMap: Fmap, intStore: IntegerStore): Constraint =
        when (constraint) {
            is PrlConstant -> constant(constraint.value)
            is PrlAmo -> amo(compileBooleanFeatures(constraint.features, featureMap))
            is PrlAnd -> and(constraint.operands.map { compileConstraint(it, featureMap, intStore) })
            is PrlComparisonPredicate -> compileComparison(constraint, featureMap, intStore)
            is PrlEquivalence -> equiv(
                compileConstraint(constraint.left, featureMap, intStore),
                compileConstraint(constraint.right, featureMap, intStore)
            )
            is PrlExo -> exo(compileBooleanFeatures(constraint.features, featureMap))
            is PrlFeature -> compileBooleanFeature(constraint, featureMap)
            is PrlImplication -> impl(
                compileConstraint(constraint.left, featureMap, intStore),
                compileConstraint(constraint.right, featureMap, intStore)
            )
            is PrlInEnumsPredicate -> compileEnumInPredicate(constraint, featureMap)
            is PrlInIntRangePredicate -> compileIntInPredicate(constraint, featureMap, intStore)
            is PrlNot -> not(compileConstraint(constraint.operand, featureMap, intStore))
            is PrlOr -> or(constraint.operands.map { compileConstraint(it, featureMap, intStore) })
        }

    internal fun compileUnversionedBooleanFeature(feature: PrlFeature, featureMap: Fmap) =
        compileBooleanFeature(feature, featureMap).also {
            if (it.versioned) throw CoCoException("Feature '${feature.featureCode}' is a versioned feature")
        }

    private fun compileBooleanFeature(feature: PrlFeature, featureMap: Fmap) =
        when (val def = featureMap[feature]) {
            null -> unknownFeature(feature)
            is BooleanFeatureDefinition -> def.feature
            else -> wrongFeatureType(feature, def, "boolean")
        }

    internal fun compileBooleanFeatures(
        features: Collection<PrlFeature>,
        featuresMap: Fmap
    ): Collection<BooleanFeature> =
        mutableListOf<BooleanFeature>().apply {
            features.forEach {
                when (val definition = featuresMap[it]) {
                    null -> unknownFeature(it)
                    is BooleanFeatureDefinition ->
                        if (!definition.versioned) {
                            add(definition.feature)
                        } else {
                            wrongFeatureType(it, definition, "boolean")
                        }
                    else -> wrongFeatureType(it, definition, "boolean")
                }
            }
        }

    private fun compileIntFeature(feature: PrlFeature, featureMap: Fmap): IntFeature =
        when (val def = featureMap[feature]) {
            null -> unknownFeature(feature)
            is IntFeatureDefinition -> def.feature
            else -> wrongFeatureType(feature, def, "int")
        }

    private fun compileEnumFeature(feature: PrlFeature, featureMap: Fmap): EnumFeature =
        when (val def = featureMap[feature]) {
            null -> unknownFeature(feature)
            is EnumFeatureDefinition -> def.feature
            else -> wrongFeatureType(feature, def, "enum")
        }

    private fun compileEnumInPredicate(
        predicate: PrlInEnumsPredicate,
        featureMap: Fmap
    ): Constraint {
        if (predicate.term !is PrlFeature) {
            throw CoCoException("Left-hand side of an enum 'in' predicate must be an enum feature")
        }
        return enumIn(compileEnumFeature(predicate.term, featureMap), predicate.values)
    }

    private fun compileIntInPredicate(
        predicate: PrlInIntRangePredicate,
        featureMap: Fmap,
        intStore: IntegerStore
    ): Constraint {
        val pred = intIn(compileIntTerm(predicate.term, featureMap), predicate.range)
        intStore.addUsage(pred)
        return pred
    }


    internal fun compileIntTerm(term: PrlTerm, featureMap: Fmap): IntTerm = when (term) {
        is PrlFeature -> compileIntFeature(term, featureMap)
        is PrlIntValue -> intVal(term.value)
        is PrlIntMulFunction -> compileIntMulFunction(term, featureMap)
        is PrlIntAddFunction -> compileIntAddFunction(term, featureMap)
        else -> throw CoCoException("Unknown integer term type: ${term.javaClass.simpleName}")
    }

    private fun compileIntMulFunction(term: PrlIntMulFunction, featureMap: Fmap): IntMul {
        if (term.left is PrlIntMulFunction || term.left is PrlIntAddFunction ||
            term.right is PrlIntMulFunction || term.right is PrlIntAddFunction ||
            term.left is PrlIntValue && term.right is PrlIntValue
        ) {
            throw CoCoException(
                "Integer multiplication is only allowed between a fixed coefficient and an integer feature"
            )
        }
        val feature = if (term.left is PrlFeature) term.left else term.right as PrlFeature
        val coefficient = if (term.left is PrlIntValue) term.left.value else (term.right as PrlIntValue).value
        return intMul(coefficient, compileIntFeature(feature, featureMap))
    }

    private fun compileIntAddFunction(term: PrlIntAddFunction, featureMap: Fmap): IntTerm {
        var coefficient = 0
        val condensed = mutableListOf<PrlTerm>().apply {
            term.operands.forEach { if (it is PrlIntAddFunction) addAll(it.operands) else add(it) }
        }
        val operands = mutableListOf<IntMul>()
        condensed.forEach {
            when (it) {
                is PrlFeature -> operands.add(intMul(compileIntFeature(it, featureMap)))
                is PrlIntValue -> coefficient += it.value
                is PrlIntMulFunction -> operands.add(compileIntMulFunction(it, featureMap))
                else -> throw CoCoException("Unknown integer term type ${term.javaClass}")
            }
        }
        return intSum(coefficient, operands)
    }

    private fun compileComparison(
        predicate: PrlComparisonPredicate,
        featureMap: Fmap,
        intStore: IntegerStore
    ): Constraint =
        when (determineType(predicate, featureMap)) {
            BOOLEAN -> compileVersionPredicate(predicate, featureMap)
            ENUM -> compileEnumComparison(predicate, featureMap)
            INT -> {
                val comp = intComparison(
                    compileIntTerm(predicate.left, featureMap),
                    compileIntTerm(predicate.right, featureMap),
                    predicate.comparison
                )
                intStore.addUsage(comp)
                comp
            }
        }

    private fun compileEnumComparison(
        predicate: PrlComparisonPredicate,
        featureMap: Fmap
    ): Constraint {
        val value =
            if (predicate.left is PrlEnumValue) {
                predicate.left.value
            } else if (predicate.right is PrlEnumValue) {
                predicate.right.value
            } else {
                null
            }
        val feature =
            if (predicate.left is PrlFeature) {
                predicate.left
            } else if (predicate.right is PrlFeature) {
                predicate.right
            } else {
                null
            }
        if (value == null || feature == null) {
            throw CoCoException("Enum comparison must compare an enum feature with an enum value")
        }
        if (predicate.comparison != ComparisonOperator.EQ && predicate.comparison != ComparisonOperator.NE) {
            throw CoCoException("Only comparisons with = and != are allowed for enums")
        }
        return enumComparison(compileEnumFeature(feature, featureMap), enumVal(value), predicate.comparison)
    }

    private fun compileVersionPredicate(
        predicate: PrlComparisonPredicate,
        featureMap: Fmap
    ): Constraint {
        val version =
            if (predicate.left is PrlIntValue) {
                predicate.left.value
            } else if (predicate.right is PrlIntValue) {
                predicate.right.value
            } else {
                null
            }
        val feature =
            if (predicate.left is PrlFeature) {
                predicate.left
            } else if (predicate.right is PrlFeature) {
                predicate.right
            } else {
                null
            }
        if (version == null || feature == null) {
            throw CoCoException("Version predicate must compare a versioned boolean feature with a fixed version")
        }
        if (version <= 0) {
            throw CoCoException("Versions must be > 0")
        }
        val compiledFeature = compileBooleanFeature(feature, featureMap)
        if (!compiledFeature.versioned) {
            throw CoCoException("Unversioned feature in version predicate: " + compiledFeature.featureCode)
        }
        return versionComparison(compiledFeature, predicate.comparison, version)
    }

    private fun determineType(
        predicate: PrlComparisonPredicate,
        featureMap: Fmap
    ): PredicateType {
        var type: PredicateType? = null
        predicate.features().forEach { feature ->
            featureMap[feature].let {
                when {
                    it == null -> unknownFeature(feature)
                    type == null -> type = it.type()
                    type != it.type() -> throw CoCoException(
                        "Cannot determine type of predicate, mixed features of type $type and ${it.type()}"
                    )
                }
            }
        }
        return when {
            type != null -> type!!
            isInt(predicate.left) && isInt(predicate.right) -> INT
            isEnum(predicate.left) && isEnum(predicate.right) -> ENUM
            else -> throw CoCoException("Cannot determine type of predicate")
        }
    }

    internal fun unknownFeature(feature: PrlFeature): Nothing =
        throw CoCoException("Unknown feature: '${feature.featureCode}'")

    private fun wrongFeatureType(feature: PrlFeature, definition: AnyFeatureDef, expected: String): Nothing =
        throw CoCoException("${featureType(definition)} feature '${feature.featureCode}' is used as $expected feature")

    private fun featureType(definition: AnyFeatureDef) = when (definition) {
        is EnumFeatureDefinition -> "Enum"
        is IntFeatureDefinition -> "Int"
        is BooleanFeatureDefinition -> if (definition.versioned) "Versioned boolean" else "Boolean"
    }

    private enum class PredicateType { ENUM, INT, BOOLEAN }

    private fun AnyFeatureDef.type() = when (this) {
        is BooleanFeatureDefinition -> BOOLEAN
        is EnumFeatureDefinition -> ENUM
        is IntFeatureDefinition -> INT
    }

    private fun isInt(term: PrlTerm) = term is PrlIntValue || term is PrlIntMulFunction || term is PrlIntAddFunction
    private fun isEnum(term: PrlTerm) = term is PrlEnumValue
}
