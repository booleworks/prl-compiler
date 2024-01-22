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
        usedValues.computeIfAbsent(feature) { IntegerUsage(feature) }.values.add(IntRange.list(value))
    }

    fun addUsage(predicate: IntPredicate) {
        when (predicate) {
            is IntComparisonPredicate -> addComparisonPredicate(predicate)
            is IntInPredicate -> addInPredicate(predicate)
        }
    }

    fun getSimpleFeatures() = usedValues.filter { !isUsedInArEx(it.key, sortedSetOf()) }.map { it.value }

    fun getArithFeatures() = usedValues.filter { isUsedInArEx(it.key, sortedSetOf()) }.map { it.value }

    fun relevantValues(feature: IntFeature): SortedSet<Int> {
        return relevantValues(feature, sortedSetOf())
    }

    private fun relevantValues(feature: IntFeature, seen: SortedSet<IntFeature>): SortedSet<Int> {
        val result = TreeSet<Int>()
        val usage = usedValues[feature] ?: return sortedSetOf()
        usage.values.forEach {
            if (it.isDiscrete()) {
                result.addAll(it.allValues())
            } else {
                result.add(it.first())
                result.add(it.last())
            }
        }
        seen.add(feature)
        usage.otherFeatures.filter { it !in seen }.forEach { result.addAll(relevantValues(it, seen)) }
        return result
    }

    private fun isUsedInArEx(feature: IntFeature, seen: SortedSet<IntFeature>): Boolean {
        val usage = usedValues[feature]
        return if (usage == null || feature in seen) {
            false
        } else if (usage.usedInArEx) {
            true
        } else {
            seen.add(feature)
            usage.otherFeatures.any { isUsedInArEx(it, seen) }
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
            usedValues.computeIfAbsent(left) { IntegerUsage(left) }.otherFeatures.add(right)
            usedValues.computeIfAbsent(right) { IntegerUsage(right) }.otherFeatures.add(left)
        } else {
            val feature = if (left is IntFeature) left else right as IntFeature
            val value = if (left is IntValue) left else right as IntValue
            usedValues.computeIfAbsent(feature) { IntegerUsage(feature) }.values.add(IntRange.list(value.value))
        }
    }

    private fun addInPredicate(predicate: IntInPredicate) {
        when (predicate.term) {
            is IntFeature -> usedValues.computeIfAbsent(predicate.term) { IntegerUsage(predicate.term) }
                .values.add(predicate.range)
            is IntMul -> addArithmeticExpression(predicate.term)
            is IntSum -> addArithmeticExpression(predicate.term)
            is IntValue -> {} // constant statement, nothing to do
        }
    }

    private fun addArithmeticExpression(term: IntTerm) {
        term.features().forEach { feature ->
            val entry = usedValues.computeIfAbsent(feature) { IntegerUsage(feature) }
            entry.usedInArEx = true
            entry.values.clear()
        }
    }
}

data class IntegerUsage(
    val feature: IntFeature,
    val values: SortedSet<IntRange> = TreeSet(),
    val otherFeatures: SortedSet<IntFeature> = TreeSet(),
    var usedInArEx: Boolean = false
) {
}
