package com.booleworks.prl.model.rules

import com.booleworks.prl.model.EnumProperty
import com.booleworks.prl.model.EnumRange
import com.booleworks.prl.model.IntProperty
import com.booleworks.prl.model.IntRange
import com.booleworks.prl.model.constraints.DEFAULT_MODULE
import com.booleworks.prl.model.constraints.FALSE
import com.booleworks.prl.model.constraints.TRUE
import com.booleworks.prl.model.constraints.boolFt
import com.booleworks.prl.model.constraints.enumFt
import com.booleworks.prl.model.constraints.intFt
import com.booleworks.prl.model.constraints.versionFt
import com.booleworks.prl.model.datastructures.FeatureAssignment
import com.booleworks.prl.model.datastructures.FeatureRenaming
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MandatoryFeatureRuleTest {
    private val properties = mapOf(Pair("p1", EnumProperty("p1", EnumRange.list("text 1"))), Pair("p2", IntProperty("p2", IntRange.list(42))))
    private val booleanRule: MandatoryFeatureRule = MandatoryFeatureRule(boolFt("bf1"), DEFAULT_MODULE)
    private val vBooleanRule: MandatoryFeatureRule = MandatoryFeatureRule(versionFt("vf1"), 3, DEFAULT_MODULE)
    private val intRule: MandatoryFeatureRule = MandatoryFeatureRule(intFt("if1"), 7, DEFAULT_MODULE)
    private val stringRule: MandatoryFeatureRule = MandatoryFeatureRule(enumFt("sf1"), "te xdf", DEFAULT_MODULE)

    @Test
    fun testDefaultsConstructor() {
        assertThat(booleanRule.feature).isEqualTo(boolFt("bf1"))
        assertThat(booleanRule.enumValue).isNull()
        assertThat(booleanRule.intValue).isNull()
        assertThat(booleanRule.version).isNull()
        assertThat(booleanRule.id).isEqualTo("")
        assertThat(booleanRule.description).isEqualTo("")
        assertThat(booleanRule.properties).isEmpty()
        assertThat(booleanRule.toString(DEFAULT_MODULE)).isEqualTo("rule mandatory feature bf1")
    }

    @Test
    fun testFullConstructor() {
        val rule = MandatoryFeatureRule(boolFt("bf1"), DEFAULT_MODULE, "id string", "text text", properties)
        assertThat(booleanRule.feature).isEqualTo(boolFt("bf1"))
        assertThat(booleanRule.enumValue).isNull()
        assertThat(booleanRule.intValue).isNull()
        assertThat(booleanRule.version).isNull()
        assertThat(rule.id).isEqualTo("id string")
        assertThat(rule.description).isEqualTo("text text")
        assertThat(rule.properties).containsExactlyInAnyOrderEntriesOf(properties)
        assertThat(rule.toString(DEFAULT_MODULE)).isEqualTo(
            "rule mandatory feature bf1 {" + System.lineSeparator() +
                    "  id \"id string\"" + System.lineSeparator() +
                    "  description \"text text\"" + System.lineSeparator() +
                    "  p1 \"text 1\"" + System.lineSeparator() +
                    "  p2 42" + System.lineSeparator() +
                    "}"
        )
    }

    @Test
    fun testEquals() {
        assertThat(booleanRule == MandatoryFeatureRule(boolFt("bf1"), DEFAULT_MODULE)).isTrue
        assertThat(vBooleanRule == MandatoryFeatureRule(versionFt("vf1"), 3, DEFAULT_MODULE)).isTrue
        assertThat(intRule == MandatoryFeatureRule(intFt("if1"), 7, DEFAULT_MODULE)).isTrue
        assertThat(stringRule == MandatoryFeatureRule(enumFt("sf1"), "te xdf", DEFAULT_MODULE)).isTrue
        assertThat(booleanRule == vBooleanRule).isFalse
        assertThat(booleanRule == vBooleanRule).isFalse
        assertThat(booleanRule == intRule).isFalse
    }

    @Test
    fun testHashCode() {
        assertThat(booleanRule).hasSameHashCodeAs(booleanRule)
        assertThat(booleanRule).hasSameHashCodeAs(MandatoryFeatureRule(boolFt("bf1"), DEFAULT_MODULE))
        assertThat(vBooleanRule).hasSameHashCodeAs(vBooleanRule)
        assertThat(vBooleanRule).hasSameHashCodeAs(MandatoryFeatureRule(versionFt("vf1"), 3, DEFAULT_MODULE))
        assertThat(intRule).hasSameHashCodeAs(intRule)
        assertThat(intRule).hasSameHashCodeAs(MandatoryFeatureRule(intFt("if1"), 7, DEFAULT_MODULE))
        assertThat(stringRule).hasSameHashCodeAs(stringRule)
        assertThat(stringRule).hasSameHashCodeAs(MandatoryFeatureRule(enumFt("sf1"), "te xdf", DEFAULT_MODULE))
        assertThat(booleanRule).doesNotHaveSameHashCodeAs(intRule)
        assertThat(booleanRule).doesNotHaveSameHashCodeAs(MandatoryFeatureRule(boolFt("bf2"), DEFAULT_MODULE))
        assertThat(vBooleanRule).doesNotHaveSameHashCodeAs(MandatoryFeatureRule(versionFt("vf1"), 4, DEFAULT_MODULE))
        assertThat(intRule).doesNotHaveSameHashCodeAs(MandatoryFeatureRule(intFt("if1"), 10, DEFAULT_MODULE))
        assertThat(stringRule).doesNotHaveSameHashCodeAs(MandatoryFeatureRule(enumFt("sf3"), "te xdf", DEFAULT_MODULE))
    }

    @Test
    fun testFeatures() {
        assertThat(booleanRule.features().map { it.featureCode }).containsExactly("bf1")
        assertThat(booleanRule.booleanFeatures().map { it.featureCode }).containsExactly("bf1")
        assertThat(booleanRule.intFeatures().map { it.featureCode }).isEmpty()
        assertThat(booleanRule.enumFeatures().map { it.featureCode }).isEmpty()
        assertThat(booleanRule.containsBooleanFeatures()).isTrue
        assertThat(booleanRule.containsEnumFeatures()).isFalse
        assertThat(booleanRule.containsIntFeatures()).isFalse
        assertThat(vBooleanRule.features().map { it.featureCode }).containsExactly("vf1")
        assertThat(vBooleanRule.booleanFeatures().map { it.featureCode }).containsExactly("vf1")
        assertThat(vBooleanRule.intFeatures().map { it.featureCode }).isEmpty()
        assertThat(vBooleanRule.enumFeatures().map { it.featureCode }).isEmpty()
        assertThat(vBooleanRule.containsBooleanFeatures()).isTrue
        assertThat(vBooleanRule.containsEnumFeatures()).isFalse
        assertThat(vBooleanRule.containsIntFeatures()).isFalse
        assertThat(intRule.features().map { it.featureCode }).containsExactly("if1")
        assertThat(intRule.booleanFeatures().map { it.featureCode }).isEmpty()
        assertThat(intRule.intFeatures().map { it.featureCode }).containsExactly("if1")
        assertThat(intRule.enumFeatures().map { it.featureCode }).isEmpty()
        assertThat(intRule.containsBooleanFeatures()).isFalse
        assertThat(intRule.containsEnumFeatures()).isFalse
        assertThat(intRule.containsIntFeatures()).isTrue
        assertThat(stringRule.features().map { it.featureCode }).containsExactly("sf1")
        assertThat(stringRule.booleanFeatures().map { it.featureCode }).isEmpty()
        assertThat(stringRule.intFeatures().map { it.featureCode }).isEmpty()
        assertThat(stringRule.enumFeatures().map { it.featureCode }).containsExactly("sf1")
        assertThat(stringRule.containsBooleanFeatures()).isFalse
        assertThat(stringRule.containsEnumFeatures()).isTrue
        assertThat(stringRule.containsIntFeatures()).isFalse
        assertThat(booleanRule.enumValues()).isEmpty()
        assertThat(vBooleanRule.enumValues()).isEmpty()
        assertThat(intRule.enumValues()).isEmpty()
        assertThat(stringRule.enumValues()).hasSize(1)
        assertThat(stringRule.enumValues()[enumFt("sf1")]).containsExactly("te xdf")
    }

    @Test
    fun testEvaluate() {
        val ass: FeatureAssignment = FeatureAssignment()
            .assign(boolFt("bf1"), true)
            .assign(enumFt("sf1"), "te xdf")
            .assign(intFt("if1"), 7)
            .assign(versionFt("vf1"), 3)
        assertThat(booleanRule.evaluate(ass)).isTrue()
        assertThat(vBooleanRule.evaluate(ass)).isTrue()
        assertThat(intRule.evaluate(ass)).isTrue()
        assertThat(stringRule.evaluate(ass)).isTrue()
        val ass2: FeatureAssignment = FeatureAssignment()
            .assign(boolFt("bf1"), false)
            .assign(enumFt("sf1"), "texdf")
            .assign(intFt("if1"), 2)
            .assign(versionFt("vf1"), 7)
        assertThat(booleanRule.evaluate(ass2)).isFalse()
        assertThat(vBooleanRule.evaluate(ass2)).isFalse()
        assertThat(intRule.evaluate(ass2)).isFalse()
        assertThat(stringRule.evaluate(ass2)).isFalse()
    }

    @Test
    fun testRestriction() {
        val ass: FeatureAssignment = FeatureAssignment()
            .assign(boolFt("bf1"), false)
            .assign(enumFt("sf1"), "texdf")
            .assign(intFt("if1"), 2)
            .assign(versionFt("vf1"), 7)
        assertThat(booleanRule.restrict(ass)).isEqualTo(ConstraintRule(FALSE, DEFAULT_MODULE))
        assertThat(vBooleanRule.restrict(ass)).isEqualTo(ConstraintRule(FALSE, DEFAULT_MODULE))
        assertThat(intRule.restrict(ass)).isEqualTo(ConstraintRule(FALSE, DEFAULT_MODULE))
        assertThat(stringRule.restrict(ass)).isEqualTo(ConstraintRule(FALSE, DEFAULT_MODULE))
        val ass2 = FeatureAssignment()
            .assign(boolFt("bf1"), true)
            .assign(enumFt("sf1"), "te xdf")
            .assign(intFt("if1"), 7)
            .assign(versionFt("vf1"), 3)
        assertThat(booleanRule.restrict(ass2)).isEqualTo(ConstraintRule(TRUE, DEFAULT_MODULE))
        assertThat(vBooleanRule.restrict(ass2)).isEqualTo(ConstraintRule(TRUE, DEFAULT_MODULE))
        assertThat(intRule.restrict(ass2)).isEqualTo(ConstraintRule(TRUE, DEFAULT_MODULE))
        assertThat(stringRule.restrict(ass2)).isEqualTo(ConstraintRule(TRUE, DEFAULT_MODULE))
        val ass3 = FeatureAssignment()
            .assign(boolFt("bf11"), true)
            .assign(enumFt("sf11"), "te xdf")
            .assign(intFt("if11"), 7)
            .assign(versionFt("vf11"), 3)
        assertThat(booleanRule.restrict(ass3)).isEqualTo(booleanRule)
        assertThat(vBooleanRule.restrict(ass3)).isEqualTo(vBooleanRule)
        assertThat(intRule.restrict(ass3)).isEqualTo(intRule)
        assertThat(stringRule.restrict(ass3)).isEqualTo(stringRule)
    }

    @Test
    fun testSyntacticSimplify() {
        assertThat(booleanRule.syntacticSimplify()).isEqualTo(booleanRule)
        assertThat(vBooleanRule.syntacticSimplify()).isEqualTo(vBooleanRule)
        assertThat(vBooleanRule.syntacticSimplify()).isEqualTo(vBooleanRule)
        assertThat(intRule.syntacticSimplify()).isEqualTo(intRule)
        assertThat(stringRule.syntacticSimplify()).isEqualTo(stringRule)
    }

    @Test
    fun testRenaming() {
        val r1 = FeatureRenaming()
            .add(boolFt("bf1"), "bf2")
            .add(enumFt("sf1"), "sf2")
            .add(intFt("if1"), "if2")
            .add(versionFt("vf1"), "vf2")
        assertThat(booleanRule.rename(r1).feature.featureCode).isEqualTo("bf2")
        assertThat(vBooleanRule.rename(r1).feature.featureCode).isEqualTo("vf2")
        assertThat(intRule.rename(r1).feature.featureCode).isEqualTo("if2")
        assertThat(stringRule.rename(r1).feature.featureCode).isEqualTo("sf2")
        val r2 = FeatureRenaming()
            .add(boolFt("bf3"), "bf2")
            .add(enumFt("sf3"), "sf2")
            .add(intFt("if3"), "if2")
            .add(versionFt("vf3"), "vf2")
        assertThat(booleanRule.rename(r2)).isSameAs(booleanRule)
        assertThat(vBooleanRule.rename(r2)).isSameAs(vBooleanRule)
        assertThat(intRule.rename(r2)).isSameAs(intRule)
        assertThat(stringRule.rename(r2)).isSameAs(stringRule)
    }
}
