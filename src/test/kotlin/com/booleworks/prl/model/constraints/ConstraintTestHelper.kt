package com.booleworks.prl.model.constraints

import com.booleworks.prl.model.BooleanFeatureDefinition
import com.booleworks.prl.model.EnumFeatureDefinition
import com.booleworks.prl.model.IntFeatureDefinition
import com.booleworks.prl.model.IntRange
import com.booleworks.prl.model.Module

val DEFAULT_MODULE: Module = Module("com.booleworks")

fun boolFt(featureCode: String) = BooleanFeatureDefinition(DEFAULT_MODULE, featureCode, false).feature
fun enumFt(featureCode: String) = EnumFeatureDefinition(DEFAULT_MODULE, featureCode, listOf()).feature
fun enumFt(featureCode: String, values: Collection<String>): EnumFeature = EnumFeatureDefinition(DEFAULT_MODULE, featureCode, values.toList()).feature
fun intFt(featureCode: String) = IntFeatureDefinition(DEFAULT_MODULE, featureCode, IntRange.interval(Int.MIN_VALUE, Int.MAX_VALUE)).feature
fun versionFt(featureCode: String) = BooleanFeatureDefinition(DEFAULT_MODULE, featureCode, true).feature

fun ftMap(vararg constraints: Constraint): Pair<Map<Feature, Int>, Map<Int, Feature>> {
    val map1 = mutableMapOf<Feature, Int>()
    val map2 = mutableMapOf<Int, Feature>()
    constraints.forEach { constraint ->
        constraint.features().forEach {
            if (!map1.containsKey(it)) {
                map1[it] = map1.size
                map2[map2.size] = it
            }
        }
    }
    return Pair(map1, map2)
}
