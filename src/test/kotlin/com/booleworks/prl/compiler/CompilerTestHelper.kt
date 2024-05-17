package com.booleworks.prl.compiler

import com.booleworks.prl.model.AnyFeatureDef
import com.booleworks.prl.model.BooleanFeatureDefinition
import com.booleworks.prl.model.EnumFeatureDefinition
import com.booleworks.prl.model.IntFeatureDefinition
import com.booleworks.prl.model.IntRange
import com.booleworks.prl.model.constraints.DEFAULT_MODULE
import com.booleworks.prl.parser.PrlFeature

val module = DEFAULT_MODULE
val b1Definition = BooleanFeatureDefinition(module, "b1")
val b2Definition = BooleanFeatureDefinition(module, "b2")
val b3Definition = BooleanFeatureDefinition(module, "b3")
val vDefinition = BooleanFeatureDefinition(module, "v", true)
val e1Definition = EnumFeatureDefinition(module, "e1", setOf("a"))
val e2Definition = EnumFeatureDefinition(module, "e2", setOf("a"))
val i1Definition = IntFeatureDefinition(module, "i1", IntRange.list(0))
val i2Definition = IntFeatureDefinition(module, "i2", IntRange.list(0))
val i3Definition = IntFeatureDefinition(module, "i3", IntRange.list(0))

val b1 = b1Definition.feature
val b2 = b2Definition.feature
val b3 = b3Definition.feature
val e1 = e1Definition.feature
val e2 = e2Definition.feature
val i1 = i1Definition.feature
val i2 = i2Definition.feature
val i3 = i3Definition.feature
val v = vDefinition.feature

val featureMap: Map<PrlFeature, AnyFeatureDef> = mapOf(
    Pair(PrlFeature("b1"), b1Definition),
    Pair(PrlFeature("b2"), b2Definition),
    Pair(PrlFeature("b3"), b3Definition),
    Pair(PrlFeature("v"), vDefinition),
    Pair(PrlFeature("e1"), e1Definition),
    Pair(PrlFeature("e2"), e2Definition),
    Pair(PrlFeature("i1"), i1Definition),
    Pair(PrlFeature("i2"), i2Definition),
    Pair(PrlFeature("i3"), i3Definition),
)
