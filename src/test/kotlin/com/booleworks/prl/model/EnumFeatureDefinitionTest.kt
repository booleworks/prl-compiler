package com.booleworks.prl.model

import com.booleworks.prl.model.datastructures.FeatureRenaming
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EnumFeatureDefinitionTest {
    private val module = Module("module")
    private val properties = mapOf(Pair("prop1", EnumProperty("prop1", EnumRange.list("val1"))))
    private val testDefinition = EnumFeatureDefinition(module, "ft1", listOf("v1", "v2"), Visibility.INTERNAL, "desc 1", properties)

    @Test
    fun testDefaults() {
        val definition = EnumFeatureDefinition(module, "ft1", listOf("v1", "v2"))
        assertThat(definition.code).isEqualTo("ft1")
        assertThat(definition.values).isEqualTo(listOf("v1", "v2"))
        assertThat(definition.description).isEqualTo("")
        assertThat(definition.visibility).isEqualTo(Visibility.PUBLIC)
        assertThat(definition.properties).isEmpty()
    }

    @Test
    fun testFull() {
        assertThat(testDefinition.code).isEqualTo("ft1")
        assertThat(testDefinition.values).isEqualTo(listOf("v1", "v2"))
        assertThat(testDefinition.description).isEqualTo("desc 1")
        assertThat(testDefinition.visibility).isEqualTo(Visibility.INTERNAL)
        assertThat(testDefinition.properties).isEqualTo(properties)
    }

    @Test
    fun testToString() {
        assertThat(testDefinition.toString()).isEqualTo(
            "internal enum feature ft1 [\"v1\", \"v2\"] {" + System.lineSeparator() +
                    "  description \"desc 1\"" + System.lineSeparator() +
                    "  prop1 \"val1\"" + System.lineSeparator() +
                    "}"
        )
    }

    @Test
    fun testRenaming() {
        val f = testDefinition.rename(FeatureRenaming().add(testDefinition.feature, "x"))
        assertThat(f.code).isEqualTo("x")
        assertThat(f.values).isEqualTo(listOf("v1", "v2"))
        assertThat(f.description).isEqualTo("desc 1")
        assertThat(f.visibility).isEqualTo(Visibility.INTERNAL)
        assertThat(f.properties).isEqualTo(properties)
    }

    @Test
    fun testEquals() {
        val f1 = EnumFeatureDefinition(module, "ft1", listOf("a", "b"))
        val f2 = EnumFeatureDefinition(module, "ft2", listOf("a", "b"), Visibility.INTERNAL, "description", properties)
        assertThat(f1 == EnumFeatureDefinition(module, "ft1", listOf("a", "b"))).isTrue
        assertThat(f1 == EnumFeatureDefinition(Module("module2"), "ft1", listOf("a", "b"))).isFalse
        assertThat(f1.equals(null)).isFalse
        assertThat(f1.equals("foo")).isFalse
        assertThat(f1 == f2).isFalse
        assertThat(f2 == f1).isFalse
        assertThat(f1 == EnumFeatureDefinition(module, "ft1", listOf("a", "b", "c"))).isFalse
        assertThat(f1 == EnumFeatureDefinition(module, "ft1", listOf("a", "b"), Visibility.INTERNAL, "", properties)).isFalse
    }

    @Test
    fun testHashCode() {
        val f1 = EnumFeatureDefinition(module, "ft1", listOf("a", "b"))
        val f2 = EnumFeatureDefinition(module, "ft2", listOf("a", "b"), Visibility.INTERNAL, "description", properties)
        assertThat(f1).hasSameHashCodeAs(f1)
        assertThat(f1).hasSameHashCodeAs(EnumFeatureDefinition(module, "ft1", listOf("a", "b")))
        assertThat(f1).doesNotHaveSameHashCodeAs(f2)
        assertThat(f1).doesNotHaveSameHashCodeAs(EnumFeatureDefinition(module, "ft1", listOf("a", "b", "c")))
        assertThat(f1).doesNotHaveSameHashCodeAs(EnumFeatureDefinition(module, "ft1", listOf("a", "b"), Visibility.INTERNAL, "", properties))
    }
}
