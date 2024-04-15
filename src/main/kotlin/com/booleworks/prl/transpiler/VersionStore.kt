// SPDX-License-Identifier: MIT
// Copyright 2023 BooleWorks GmbH

package com.booleworks.prl.transpiler

import com.booleworks.prl.model.constraints.BooleanFeature
import com.booleworks.prl.model.constraints.VersionPredicate

data class VersionStore internal constructor(
    val usedValues: MutableMap<BooleanFeature, VersionUsage> = mutableMapOf()
) {
    fun addValue(feature: BooleanFeature, value: Int) {
        val currentMax = usedValues.computeIfAbsent(feature) { VersionUsage(feature) }.maxVersion
        if (value > currentMax) {
            usedValues[feature]!!.maxVersion = value
        }
    }

    fun addUsage(predicate: VersionPredicate) {
        addComparisonPredicate(predicate)
    }

    private fun addComparisonPredicate(predicate: VersionPredicate) {
        val feature = predicate.feature
        val version = predicate.version
        val currentMax = usedValues.computeIfAbsent(feature) { VersionUsage(feature) }.maxVersion
        if (version > currentMax) {
            usedValues[feature]!!.maxVersion = version
        }
    }
}

data class VersionUsage(
    val feature: BooleanFeature,
    var maxVersion: Int = 0
)
