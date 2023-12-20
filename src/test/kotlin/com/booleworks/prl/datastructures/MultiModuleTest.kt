package com.booleworks.prl.datastructures

import com.booleworks.prl.compiler.PrlCompiler
import com.booleworks.prl.model.constraints.BooleanFeature
import com.booleworks.prl.model.datastructures.FeatureAssignment
import com.booleworks.prl.parser.parseRuleFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MultiModuleTest {

    private val model = PrlCompiler().compile(parseRuleFile("test-files/prl/compiler/inheritance.prl"))

    private val f1 = model.getFeature<BooleanFeature>("f1", "top")
    private val f10 = model.getFeature<BooleanFeature>("f10", "top.first")
    private val f11 = model.getFeature<BooleanFeature>("f11", "top.first")
    private val f20second = model.getFeature<BooleanFeature>("f20", "top.second")
    private val f21 = model.getFeature<BooleanFeature>("f21", "top.second")
    private val f22second = model.getFeature<BooleanFeature>("f22", "top.second")
    private val f20a = model.getFeature<BooleanFeature>("f20", "top.second.a")
    private val f22a = model.getFeature<BooleanFeature>("f22", "top.second.a")
    private val f30 = model.getFeature<BooleanFeature>("f30", "top.second.a")

    private val rule1 = model.rules[0]
    private val rule2 = model.rules[1]
    private val rule3 = model.rules[2]
    private val rule4 = model.rules[3]
    private val rule5 = model.rules[4]
    private val rule6 = model.rules[5]
    private val rule7 = model.rules[6]
    private val rule8 = model.rules[7]

    @Test
    fun testRules() {
        assertThat(model.rules).hasSize(8)
        assertThat(rule1.toString(model.moduleHierarchy.moduleForName("top.first")!!)).startsWith("rule top.f1 & f10")
        assertThat(rule2.toString(model.moduleHierarchy.moduleForName("top.second")!!)).startsWith("rule top.f1 & f20")
        assertThat(rule3.toString(model.moduleHierarchy.moduleForName("top.second")!!)).startsWith("rule top.f1 & f20")
    }

    @Test
    fun testFeatures() {
        assertThat(f1.fullName).isEqualTo("top.f1")
        assertThat(f10.fullName).isEqualTo("top.first.f10")
        assertThat(f11.fullName).isEqualTo("top.first.f11")
        assertThat(f20second.fullName).isEqualTo("top.second.f20")
        assertThat(f21.fullName).isEqualTo("top.second.f21")
        assertThat(f22second.fullName).isEqualTo("top.second.f22")
        assertThat(f20a.fullName).isEqualTo("top.second.a.f20")
        assertThat(f22a.fullName).isEqualTo("top.second.a.f22")
        assertThat(f30.fullName).isEqualTo("top.second.a.f30")
    }

    @Test
    fun testEvaluate() {
        val ass = FeatureAssignment().assign(f1, true).assign(f10, true)
        assertThat(rule1.evaluate(ass)).isTrue
        assertThat(rule2.evaluate(ass)).isFalse
        assertThat(rule3.evaluate(ass)).isFalse
        ass.assign(f20second, true)
        assertThat(rule2.evaluate(ass)).isTrue
        assertThat(rule3.evaluate(ass)).isTrue
        ass.assign(f21, true).assign(f30, true)
        assertThat(rule4.evaluate(ass)).isFalse
        ass.assign(f22second, true)
        assertThat(rule4.evaluate(ass)).isFalse
        ass.assign(f22a, true)
        assertThat(rule4.evaluate(ass)).isTrue
        assertThat(rule5.evaluate(ass)).isFalse
        assertThat(rule6.evaluate(ass)).isTrue
        ass.assign(f22a, false)
        assertThat(rule6.evaluate(ass)).isTrue
        assertThat(rule7.evaluate(ass)).isTrue
        assertThat(rule8.evaluate(ass)).isTrue
    }

    @Test
    fun testEvaluateModel() {
        val ass = FeatureAssignment().assign(f1, true).assign(f10, true)
        assertThat(model.evaluate(ass)).isFalse
        assertThat(model.evaluateEachRule(ass).filter { it.value }.map { it.key }).containsExactlyInAnyOrder(rule1)
        ass.assign(f20second, true)
        assertThat(model.evaluate(ass)).isFalse
        assertThat(model.evaluateEachRule(ass).filter { it.value }.map { it.key }).containsExactlyInAnyOrder(rule1, rule2, rule3)
        ass.assign(f20a, true)
        assertThat(model.evaluate(ass)).isFalse
        assertThat(model.evaluateEachRule(ass).filter { it.value }.map { it.key }).containsExactlyInAnyOrder(rule1, rule2, rule3, rule5)
        ass.assign(f22a, true).assign(f22second, true)
        assertThat(model.evaluate(ass)).isFalse
        assertThat(model.evaluateEachRule(ass).filter { it.value }.map { it.key }).containsExactlyInAnyOrder(rule1, rule2, rule3, rule5, rule6, rule7, rule8)
        ass.assign(f21, true).assign(f30, true)
        assertThat(model.evaluate(ass)).isTrue
        assertThat(model.evaluateEachRule(ass).filter { it.value }.map { it.key }).hasSize(8)
    }
}
