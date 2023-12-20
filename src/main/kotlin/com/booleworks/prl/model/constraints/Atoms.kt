// SPDX-License-Identifier: MIT
// Copyright 2023 BooleWorks GmbH

package com.booleworks.prl.model.constraints

import com.booleworks.prl.model.Module
import com.booleworks.prl.parser.PragmaticRuleLanguage
import com.booleworks.prl.parser.PragmaticRuleLanguage.SYMBOL_EQ
import com.booleworks.prl.parser.PragmaticRuleLanguage.SYMBOL_GE
import com.booleworks.prl.parser.PragmaticRuleLanguage.SYMBOL_GT
import com.booleworks.prl.parser.PragmaticRuleLanguage.SYMBOL_LE
import com.booleworks.prl.parser.PragmaticRuleLanguage.SYMBOL_LT
import com.booleworks.prl.parser.PragmaticRuleLanguage.SYMBOL_NE

sealed interface AtomicConstraint : Constraint {
    override fun isAtom() = true
}

sealed interface Predicate : AtomicConstraint

sealed class Feature(open val featureCode: String, open val module: Module) : Comparable<Feature> {
    val fullName by lazy { fullNameOf(featureCode, module.fullName) }

    companion object {
        fun fullNameOf(featureCode: String, moduleName: String): String {
            return if (moduleName.isNotEmpty()) {
                moduleName + Module.MODULE_SEPARATOR + featureCode
            } else {
                featureCode
            }
        }
    }

    fun toString(currentModule: Module) =
        PragmaticRuleLanguage.identifier(if (module == currentModule) featureCode else fullName)

    override fun compareTo(other: Feature) = compareValuesBy(this, other, { it.fullName }, { it.fullName })

    override fun hashCode() = fullName.hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return fullName == (other as Feature).fullName
    }
}

enum class ComparisonOperator(val symbol: String) {
    EQ(SYMBOL_EQ),
    NE(SYMBOL_NE),
    LT(SYMBOL_LT),
    LE(SYMBOL_LE),
    GT(SYMBOL_GT),
    GE(SYMBOL_GE);

    fun evaluate(v1: Int, v2: Int) = when (this) {
        EQ -> v1 == v2
        NE -> v1 != v2
        LT -> v1 < v2
        LE -> v1 <= v2
        GT -> v1 > v2
        GE -> v1 >= v2
    }

    fun reverse() = when (this) {
        EQ -> EQ
        NE -> NE
        LT -> GT
        LE -> GE
        GT -> LT
        GE -> LE
    }
}
