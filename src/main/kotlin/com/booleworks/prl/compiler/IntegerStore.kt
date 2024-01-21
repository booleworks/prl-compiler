// SPDX-License-Identifier: MIT
// Copyright 2023 BooleWorks GmbH

package com.booleworks.prl.compiler

import com.booleworks.prl.model.IntRange
import com.booleworks.prl.model.constraints.IntComparisonPredicate
import com.booleworks.prl.model.constraints.IntFeature
import com.booleworks.prl.model.constraints.IntInPredicate
import com.booleworks.prl.model.constraints.IntMul
import com.booleworks.prl.model.constraints.IntPredicate
import com.booleworks.prl.model.constraints.IntSum
import com.booleworks.prl.model.constraints.IntTerm
import com.booleworks.prl.model.constraints.IntValue
import java.util.SortedSet
import java.util.TreeSet

data class IntegerStore internal constructor(
    val usedValues: MutableMap<IntFeature, IntegerUsage> = mutableMapOf()
) {
    fun hasArithmeticExpressions() = usedValues.any { it.value.usedInArEx }

    fun addValue(feature: IntFeature, value: Int) {
        usedValues.computeIfAbsent(feature) { IntegerUsage() }.values.add(IntRange.list(value))
    }

    fun addUsage(predicate: IntPredicate) {
        when (predicate) {
            is IntComparisonPredicate -> addComparisonPredicate(predicate)
            is IntInPredicate -> addInPredicate(predicate)
        }
    }

    private fun addComparisonPredicate(predicate: IntComparisonPredicate) {
        if (predicate.left is IntValue && predicate.right is IntValue) {
            return // constant statement, nothing to do
        }
        val left = predicate.left.normalize()
        val right = predicate.right.normalize()
        if (left is IntMul || left is IntSum || right is IntMul || right is IntSum) {
            addArithmeticExpression(left)
            addArithmeticExpression(right)
        } else if (left is IntFeature && right is IntFeature) {
            usedValues.computeIfAbsent(left) { IntegerUsage() }.otherFeatures.add(right)
            usedValues.computeIfAbsent(right) { IntegerUsage() }.otherFeatures.add(left)
        } else {
            val feature = if (left is IntFeature) left else right as IntFeature
            val value = if (left is IntValue) left else right as IntValue
            usedValues.computeIfAbsent(feature) { IntegerUsage() }.values.add(IntRange.list(value.value))
        }
    }

    private fun addInPredicate(predicate: IntInPredicate) {
        when (predicate.term) {
            is IntFeature -> usedValues.computeIfAbsent(predicate.term) { IntegerUsage() }.values.add(predicate.range)
            is IntMul -> addArithmeticExpression(predicate.term)
            is IntSum -> addArithmeticExpression(predicate.term)
            is IntValue -> {} // constant statement, nothing to do
        }
    }

    private fun addArithmeticExpression(term: IntTerm) {
        term.features().forEach { feature ->
            usedValues.computeIfAbsent(feature) { IntegerUsage() }.usedInArEx = true
        }
    }
}

data class IntegerUsage(
    val values: SortedSet<IntRange> = TreeSet(),
    val otherFeatures: SortedSet<IntFeature> = TreeSet(),
    var usedInArEx: Boolean = false
)
