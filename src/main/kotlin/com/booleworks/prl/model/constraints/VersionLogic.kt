// SPDX-License-Identifier: MIT
// Copyright 2023 BooleWorks GmbH

package com.booleworks.prl.model.constraints

import com.booleworks.prl.model.Module
import com.booleworks.prl.model.constraints.ComparisonOperator.EQ
import com.booleworks.prl.model.constraints.ComparisonOperator.GE
import com.booleworks.prl.model.constraints.ComparisonOperator.GT
import com.booleworks.prl.model.constraints.ComparisonOperator.LE
import com.booleworks.prl.model.constraints.ComparisonOperator.LT
import com.booleworks.prl.model.constraints.ComparisonOperator.NE
import com.booleworks.prl.model.datastructures.FeatureAssignment
import com.booleworks.prl.model.datastructures.FeatureRenaming
import com.booleworks.prl.parser.PragmaticRuleLanguage.SYMBOL_LSQB
import com.booleworks.prl.parser.PragmaticRuleLanguage.SYMBOL_RSQB

fun versionFt(featureCode: String, module: Module) = BooleanFeature(featureCode, true, module)
fun versionEq(feature: BooleanFeature, version: Int) = VersionPredicate(feature, EQ, version)
fun versionNe(feature: BooleanFeature, version: Int) = VersionPredicate(feature, NE, version)
fun versionGt(feature: BooleanFeature, version: Int) = VersionPredicate(feature, GT, version)
fun versionGe(feature: BooleanFeature, version: Int) = VersionPredicate(feature, GE, version)
fun versionLt(feature: BooleanFeature, version: Int) = VersionPredicate(feature, LT, version)
fun versionLe(feature: BooleanFeature, version: Int) = VersionPredicate(feature, LE, version)
fun versionComparison(feature: BooleanFeature, comparison: ComparisonOperator, version: Int) =
    VersionPredicate(feature, comparison, version)

data class VersionPredicate internal constructor(
    val feature: BooleanFeature,
    val comparison: ComparisonOperator,
    val version: Int
) :
    AtomicConstraint, Predicate {
    init {
        require(feature.versioned) { "Cannot generate a version predicate with an unversioned feature" }
    }

    override val type = ConstraintType.ATOM
    override fun features() = setOf(feature)
    override fun booleanFeatures() = setOf<BooleanFeature>()
    override fun enumFeatures() = setOf<EnumFeature>()
    override fun enumValues() = mapOf<EnumFeature, Set<String>>()
    override fun intFeatures() = setOf<IntFeature>()
    override fun evaluate(assignment: FeatureAssignment) =
        assignment.getVersion(feature).let { it != null && it >= 0 && comparison.evaluate(it, version) }

    override fun rename(renaming: FeatureRenaming) = VersionPredicate(renaming.rename(feature), comparison, version)
    override fun syntacticSimplify() = if (comparison == LT && version == 0) FALSE else this
    override fun restrict(assignment: FeatureAssignment) = assignment.getVersion(feature).let {
        if (it == null) feature.restrict(assignment) else Constant(comparison.evaluate(it, version))
    }

    override fun toString(currentModule: Module) =
        feature.toString(currentModule) + SYMBOL_LSQB + comparison.symbol + version + SYMBOL_RSQB
}
