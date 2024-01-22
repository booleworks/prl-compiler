package com.booleworks.prl.compiler

import com.booleworks.prl.model.IntRange
import com.booleworks.prl.model.Module
import com.booleworks.prl.model.ModuleHierarchy
import com.booleworks.prl.model.SlicingBooleanPropertyDefinition
import com.booleworks.prl.model.SlicingDatePropertyDefinition
import com.booleworks.prl.model.SlicingEnumPropertyDefinition
import com.booleworks.prl.model.SlicingIntPropertyDefinition
import com.booleworks.prl.model.constraints.BooleanFeature
import com.booleworks.prl.model.constraints.EnumFeature
import com.booleworks.prl.model.constraints.IntFeature
import com.booleworks.prl.model.constraints.intEq
import com.booleworks.prl.model.constraints.intLe
import com.booleworks.prl.model.constraints.intMul
import com.booleworks.prl.model.constraints.intSum
import com.booleworks.prl.model.deserialize
import com.booleworks.prl.model.rules.ConstraintRule
import com.booleworks.prl.model.serialize
import com.booleworks.prl.parser.PrlFeature
import com.booleworks.prl.parser.parseRuleFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.time.LocalDate

class PrlCompilerTest {

    @Test
    fun testEmptyModuleName() {
        val parsed = parseRuleFile("test-files/prl/parser/empty_module_name.prl")
        val compiler = PrlCompiler()
        val model = compiler.compile(parsed)
        val module = Module("", lineNumber = 5)
        val f1 = model.featureStore.findMatchingDefinitions(
            module,
            PrlFeature("f1"),
            ModuleHierarchy(mapOf())
        )[0].feature as EnumFeature
        assertThat(compiler.errors()).isEmpty()
        assertThat(model.moduleHierarchy.modules()).containsExactly(module)
        assertThat(f1.fullName).isEqualTo("f1")
        assertThat(deserialize(serialize(model))).isEqualTo(model)
    }

    @Test
    fun testIntModule() {
        val parsed = parseRuleFile("test-files/prl/parser/int_module.prl")
        val compiler = PrlCompiler()
        val model = compiler.compile(parsed)

        val module = Module("ints", lineNumber = 5)
        val i1 = model.featureStore.findMatchingDefinitions(
            module,
            PrlFeature("i1"),
            ModuleHierarchy(mapOf())
        )[0].feature as IntFeature
        val i2 = model.featureStore.findMatchingDefinitions(
            module,
            PrlFeature("i2"),
            ModuleHierarchy(mapOf())
        )[0].feature as IntFeature
        val i3 = model.featureStore.findMatchingDefinitions(
            module,
            PrlFeature("i3"),
            ModuleHierarchy(mapOf())
        )[0].feature as IntFeature
        val i4 = model.featureStore.findMatchingDefinitions(
            module,
            PrlFeature("i4"),
            ModuleHierarchy(mapOf())
        )[0].feature as IntFeature
        val sum = model.featureStore.findMatchingDefinitions(
            module,
            PrlFeature("sum"),
            ModuleHierarchy(mapOf())
        )[0].feature as IntFeature

        assertThat(compiler.errors()).containsExactly(
            "Currently linear arithmetic expressions over int features are not supported. Support will be added infuture releases"
        )
        assertThat(model.moduleHierarchy.modules()).containsExactly(module)
        assertThat(model.featureStore.allDefinitions(module).map { it.feature }).containsExactly(i1, i2, i3, i4, sum)
        assertThat(model.rules).hasSize(10)
        assertThat(model.rules[0]).isEqualTo(ConstraintRule(intLe(i1, sum), module))
        assertThat(model.rules[1]).isEqualTo(ConstraintRule(intLe(i1, 48), module))
        assertThat(model.rules[6]).isEqualTo(
            ConstraintRule(
                intEq(
                    intSum(
                        17,
                        intMul(i1),
                        intMul(2, i2),
                        intMul(-4, i3),
                        intMul(i4, false)
                    ), sum
                ), module
            )
        )

        assertThat(deserialize(serialize(model))).isEqualTo(model)

        assertThat(model.intStore.usedValues).hasSize(5)
        assertThat(model.intStore.usedValues[i1]).isEqualTo(
            IntegerUsage(
                i1,
                usedInArEx = true,
                otherFeatures = sortedSetOf(sum)
            )
        )
        assertThat(model.intStore.usedValues[i2]).isEqualTo(IntegerUsage(i2, usedInArEx = true))
        assertThat(model.intStore.usedValues[i3]).isEqualTo(IntegerUsage(i3, usedInArEx = true))
        assertThat(model.intStore.usedValues[i4]).isEqualTo(IntegerUsage(i4, usedInArEx = true))
        assertThat(model.intStore.usedValues[sum]).isEqualTo(
            IntegerUsage(
                sum,
                usedInArEx = true,
                otherFeatures = sortedSetOf(i1)
            )
        )
    }

    @Test
    fun testIntToRuleFile() {
        val parsed = parseRuleFile("test-files/prl/parser/int_module.prl")
        val model = PrlCompiler().compile(parsed)
        val tempFile = Files.createTempFile("temp", "prl")
        tempFile.toFile().writeText(model.toRuleFile().toString())
        val parsedModel = PrlCompiler().compile(parseRuleFile(tempFile))
        Files.deleteIfExists(tempFile)
        assertThat(parsedModel).isEqualTo(model)
    }

    @Test
    fun testInvalidFeatureNames() {
        val parsed = parseRuleFile("test-files/prl/parser/invalid_feature_names.prl")
        val compiler = PrlCompiler()
        val model = compiler.compile(parsed)

        val module = Module("boolerules", lineNumber = 5)
        val f1 = model.featureStore.findMatchingDefinitions(
            module,
            PrlFeature("f1"),
            ModuleHierarchy(mapOf())
        )[0].feature as BooleanFeature
        val i1 = model.featureStore.findMatchingDefinitions(
            module,
            PrlFeature("i1"),
            ModuleHierarchy(mapOf())
        )[0].feature as IntFeature

        assertThat(compiler.errors()).containsExactly(
            "[module=boolerules, feature=com.x1, lineNumber=8] Feature name invalid: com.x1",
            "[module=boolerules, feature=invalid.name, lineNumber=12] Rule name invalid: invalid.name",
        )
        assertThat(model.moduleHierarchy.modules()).containsExactly(module)
        assertThat(model.featureStore.allDefinitions(module).map { it.feature }).containsExactly(f1, i1)

        assertThat(deserialize(serialize(model))).isEqualTo(model)
    }

    @Test
    fun testRealModule() {
        val parsed = parseRuleFile("test-files/prl/real/automotive/automotive_simple_1.prl")
        val compiler = PrlCompiler()
        val model = compiler.compile(parsed)

        assertThat(deserialize(serialize(model))).isEqualTo(model)
    }

    @Test
    fun testRealToRuleFile() {
        val parsed = parseRuleFile("test-files/prl/real/automotive/automotive_simple_1.prl")
        val model = PrlCompiler().compile(parsed)
        val tempFile = Files.createTempFile("temp", "prl")
        tempFile.toFile().writeText(model.toRuleFile().toString())
        val parsedModel = PrlCompiler().compile(parseRuleFile(tempFile))
        Files.deleteIfExists(tempFile)
        assertThat(parsedModel).isEqualTo(model)
    }

    @Test
    fun testInheritance() {
        val parsed = parseRuleFile("test-files/prl/compiler/inheritance.prl")
        val compiler = PrlCompiler()
        val model = compiler.compile(parsed)

        assertThat(compiler.warnings()).containsExactly("[module=top.second.a, feature=f22, lineNumber=32] Feature also defined in module: top.second")
        assertThat(model.rules[0].features().map { it.fullName }).containsExactlyInAnyOrder("top.f1", "top.first.f10")
        assertThat(model.rules[1].features().map { it.fullName }).containsExactlyInAnyOrder("top.f1", "top.second.f20")
        assertThat(model.rules[2].features().map { it.fullName }).containsExactlyInAnyOrder("top.f1", "top.second.f20")
        assertThat(model.rules[3].features().map { it.fullName }).containsExactlyInAnyOrder(
            "top.f1",
            "top.second.f21",
            "top.second.a.f22",
            "top.second.a.f30"
        )
        assertThat(model.rules[4].features().map { it.fullName }).containsExactlyInAnyOrder("top.second.a.f20")
        assertThat(model.rules[5].features().map { it.fullName }).containsExactlyInAnyOrder("top.second.f22")
        assertThat(model.rules[6].features().map { it.fullName }).containsExactlyInAnyOrder(
            "top.second.f22",
            "top.second.a.f22"
        )
        assertThat(model.rules[7].features().map { it.fullName }).containsExactlyInAnyOrder(
            "top.second.f22",
            "top.second.a.f22"
        )

        // TODO Fix Serialization
//        assertThat(deserialize(serialize(model))).isEqualTo(model)
    }

    @Test
    fun testInheritanceToRuleFile() {
        val parsed = parseRuleFile("test-files/prl/compiler/inheritance.prl")
        val model = PrlCompiler().compile(parsed)
        val string = model.toRuleFile().toString()
        assertThat(string).contains("rule top.f1 & f10")
        assertThat(string).contains("rule top.f1 & f20")
        assertThat(string).contains("rule top.f1 & top.second.f21 & f22 & f30")
        assertThat(string).contains("rule f22 / top.second.f22")

        val tempFile = Files.createTempFile("temp", "prl")
        tempFile.toFile().writeText(model.toRuleFile().toString())
        val parsedModel = PrlCompiler().compile(parseRuleFile(tempFile))
        Files.deleteIfExists(tempFile)
        assertThat(parsedModel).isEqualTo(model)
    }

    @Test
    fun testSlicingProperties() {
        val parsed = parseRuleFile("test-files/prl/compiler/slices.prl")
        val compiler = PrlCompiler()
        val original = compiler.compile(parsed)
        val model = deserialize(serialize(original))
        assertThat(model).isEqualTo(original)

        assertThat(compiler.warnings() + compiler.errors()).isEmpty()
        val boolDef = model.propertyStore.definition("active") as SlicingBooleanPropertyDefinition
        assertThat(boolDef.values).containsExactly(false, true)

        val intDef = model.propertyStore.definition("version") as SlicingIntPropertyDefinition
        assertThat(intDef.startValues).containsExactly(1)
        assertThat(intDef.endValues).containsExactly(5)
        assertThat(intDef.singleValues).containsExactly(1, 3)

        val dateDef = model.propertyStore.definition("validity") as SlicingDatePropertyDefinition
        assertThat(dateDef.startValues).containsExactly(LocalDate.of(2022, 1, 1), LocalDate.of(2024, 1, 1))
        assertThat(dateDef.endValues).containsExactly(LocalDate.of(2024, 12, 31), LocalDate.of(2025, 12, 31))
        assertThat(dateDef.singleValues).containsExactly(
            LocalDate.of(2023, 1, 12), LocalDate.of(2024, 1, 12), LocalDate.of(2025, 1, 12), LocalDate.of(2026, 1, 12)
        )

        val enumDef = model.propertyStore.definition("model") as SlicingEnumPropertyDefinition
        assertThat(enumDef.values).containsExactly("M1", "M10", "M2", "M3")
    }

    @Test
    fun testSlicingPropertiesToRuleFile1() {
        val parsed = parseRuleFile("test-files/prl/compiler/slices.prl")
        val model = PrlCompiler().compile(parsed)
        val tempFile = Files.createTempFile("temp", "prl")
        tempFile.toFile().writeText(model.toRuleFile().toString())
        val parsedModel = PrlCompiler().compile(parseRuleFile(tempFile))
        Files.deleteIfExists(tempFile)
        assertThat(parsedModel).isEqualTo(model)
    }

    @Test
    fun testSlicingPropertiesToRuleFile2() {
        val parsed = parseRuleFile("test-files/prl/transpiler/merge3.prl")
        val model = PrlCompiler().compile(parsed)
        val tempFile = Files.createTempFile("temp", "prl")
        tempFile.toFile().writeText(model.toRuleFile().toString())
        val parsedModel = PrlCompiler().compile(parseRuleFile(tempFile))
        Files.deleteIfExists(tempFile)
        assertThat(parsedModel).isEqualTo(model)
        assertThat(deserialize(serialize(model))).isEqualTo(model)
    }

    @Test
    fun testBikeshop() {
        val parsed = parseRuleFile("test-files/prl/real/bike/bikeshop_without_slices.prl")
        val compiler = PrlCompiler()
        val model = compiler.compile(parsed)
        assertThat(compiler.errors()).isEmpty()
        val tempFile = Files.createTempFile("temp", "prl")
        tempFile.toFile().writeText(model.toRuleFile().toString())
        val parsedModel = compiler.compile(parseRuleFile(tempFile))
        Files.deleteIfExists(tempFile)
        assertThat(parsedModel).isEqualTo(model)
        assertThat(deserialize(serialize(model))).isEqualTo(model)
    }

    @Test
    fun testSimpleIntExampleSerialize() {
        val parsed = parseRuleFile("test-files/prl/compiler/simple_int.prl")
        val compiler = PrlCompiler()
        val model = compiler.compile(parsed)
        assertThat(compiler.errors()).isEmpty()
        val tempFile = Files.createTempFile("temp", "prl")
        tempFile.toFile().writeText(model.toRuleFile().toString())
        val parsedModel = compiler.compile(parseRuleFile(tempFile))
        Files.deleteIfExists(tempFile)
        assertThat(parsedModel).isEqualTo(model)
        assertThat(deserialize(serialize(model))).isEqualTo(model)
    }

    @Test
    fun testSimpleIntExampleIntStore() {
        val parsed = parseRuleFile("test-files/prl/compiler/simple_int.prl")
        val compiler = PrlCompiler()
        val model = compiler.compile(parsed)

        val module = Module("ints", lineNumber = 5)
        val i1 = model.featureStore.findMatchingDefinitions(
            module,
            PrlFeature("i1"),
            ModuleHierarchy(mapOf())
        )[0].feature as IntFeature
        val i2 = model.featureStore.findMatchingDefinitions(
            module,
            PrlFeature("i2"),
            ModuleHierarchy(mapOf())
        )[0].feature as IntFeature
        val i3 = model.featureStore.findMatchingDefinitions(
            module,
            PrlFeature("i3"),
            ModuleHierarchy(mapOf())
        )[0].feature as IntFeature
        val i4 = model.featureStore.findMatchingDefinitions(
            module,
            PrlFeature("i4"),
            ModuleHierarchy(mapOf())
        )[0].feature as IntFeature

        assertThat(compiler.errors()).isEmpty()
        assertThat(model.intStore.hasArithmeticExpressions()).isFalse()
        assertThat(model.intStore.getSimpleFeatures()).hasSize(4)
        assertThat(model.intStore.getArithFeatures()).hasSize(0)

        val istore = model.intStore.usedValues
        assertThat(istore).hasSize(4)
        assertThat(istore[i1]).isEqualTo(
            IntegerUsage(
                i1,
                values = sortedSetOf(
                    IntRange.list(10),
                    IntRange.list(20),
                    IntRange.list(50)
                )
            )
        )
        assertThat(model.intStore.relevantValues(i1)).containsExactly(10, 20, 50)
        assertThat(istore[i2]).isEqualTo(
            IntegerUsage(
                i2,
                values = sortedSetOf(
                    IntRange.list(0),
                    IntRange.list(10),
                    IntRange.list(30)
                )
            )
        )
        assertThat(model.intStore.relevantValues(i2)).containsExactly(0, 10, 30)
        assertThat(istore[i3]).isEqualTo(
            IntegerUsage(
                i3,
                values = sortedSetOf(
                    IntRange.list(0),
                    IntRange.list(0, 10, 20),
                ),
                otherFeatures = sortedSetOf(i4)
            )
        )
        assertThat(model.intStore.relevantValues(i3)).containsExactly(-40, -20, 0, 10, 20, 40)
        assertThat(istore[i4]).isEqualTo(
            IntegerUsage(
                i4,
                values = sortedSetOf(
                    IntRange.list(0),
                    IntRange.list(20, 40),
                    IntRange.interval(-40, -20),
                ),
                otherFeatures = sortedSetOf(i3)
            )
        )
        assertThat(model.intStore.relevantValues(i4)).containsExactly(-40, -20, 0, 10, 20, 40)
    }
}
