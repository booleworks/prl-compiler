package com.booleworks.prl.compiler

import com.booleworks.prl.model.BooleanFeatureDefinition
import com.booleworks.prl.model.BooleanRange
import com.booleworks.prl.model.DateRange
import com.booleworks.prl.model.EnumFeatureDefinition
import com.booleworks.prl.model.EnumRange
import com.booleworks.prl.model.FeatureDefinition
import com.booleworks.prl.model.IntFeatureDefinition
import com.booleworks.prl.model.IntRange
import com.booleworks.prl.model.Module
import com.booleworks.prl.model.ModuleHierarchy
import com.booleworks.prl.model.Visibility
import com.booleworks.prl.model.deserialize
import com.booleworks.prl.model.serialize
import com.booleworks.prl.parser.PrlBooleanFeatureDefinition
import com.booleworks.prl.parser.PrlBooleanProperty
import com.booleworks.prl.parser.PrlDateProperty
import com.booleworks.prl.parser.PrlEnumFeatureDefinition
import com.booleworks.prl.parser.PrlEnumProperty
import com.booleworks.prl.parser.PrlFeature
import com.booleworks.prl.parser.PrlIntFeatureDefinition
import com.booleworks.prl.parser.PrlIntProperty
import com.booleworks.prl.parser.PrlSlicingDatePropertyDefinition
import com.booleworks.prl.parser.PrlSlicingEnumPropertyDefinition
import com.booleworks.prl.parser.PrlSlicingIntPropertyDefinition
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FutureStoreTest {
    private val s1 = PrlFeature("s1")
    private val bool1FeatureDefinition = PrlBooleanFeatureDefinition("bool1", false)
    private val boolVersioned1FeatureDefinition = PrlBooleanFeatureDefinition("bool1", true)
    private val string1FeatureDefinition = PrlEnumFeatureDefinition(
        "string1", listOf(), "desc", Visibility.PUBLIC, listOf(PrlEnumProperty("prop1", EnumRange.list("val1")))
    )
    private val int1FeatureDefinition = PrlIntFeatureDefinition("int1", IntRange.interval(1, 1), "desc", Visibility.PUBLIC)
    private val bool2FeatureDefinition = PrlBooleanFeatureDefinition("bool2", false)
    private val string2FeatureDefinition = PrlEnumFeatureDefinition("string2", listOf())

    /////////////////////////////////
    // Add Features Tests          //
    /////////////////////////////////
    @Nested
    inner class AddFeaturesTest {
        @Test
        fun testAddSingleBooleanFeature() {
            val state = CompilerState()
            val module = Module("com.test")
            val featureStore = FeatureStore()
            featureStore.addDefinition(module, bool1FeatureDefinition, state)
            assertThat(state.errors + state.warnings).isEmpty()
            assertThat(deserialize(serialize(featureStore, mapOf()), mapOf())).isEqualTo(featureStore)
        }

        @Test
        fun testAddSingleVersionedBooleanFeature() {
            val state = CompilerState()
            val module = Module("com.test")
            val featureStore = FeatureStore()
            featureStore.addDefinition(module, boolVersioned1FeatureDefinition, state)
            assertThat(state.errors + state.warnings).isEmpty()
            assertThat(deserialize(serialize(featureStore, mapOf()), mapOf())).isEqualTo(featureStore)
        }

        @Test
        fun testAddSingleStringFeature() {
            val state = CompilerState()
            val module = Module("com.test")
            val featureStore2 = FeatureStore()
            featureStore2.addDefinition(module, string1FeatureDefinition, state)
            assertThat(state.errors + state.warnings).isEmpty()
            assertThat(deserialize(serialize(featureStore2, mapOf()), mapOf())).isEqualTo(featureStore2)
        }

        @Test
        fun testAddSingleIntFeature() {
            val state = CompilerState()
            val module = Module("com.test")
            val featureStore3 = FeatureStore()
            featureStore3.addDefinition(module, int1FeatureDefinition, state)
            assertThat(state.errors + state.warnings).isEmpty()
            assertThat(deserialize(serialize(featureStore3, mapOf()), mapOf())).isEqualTo(featureStore3)
        }

        @Test
        fun testAddMultipleFeaturesWith_SameNameAndTypeAndModule() {
            val state = CompilerState()
            val module = Module("com.test")
            val featureStore = FeatureStore()
            featureStore.addDefinition(module, bool1FeatureDefinition, state)
            state.context = CompilerContext(module.fullName, bool1FeatureDefinition.code)
            featureStore.addDefinition(module, bool1FeatureDefinition, state)
            assertThat(state.errors).containsExactly("[module=com.test, feature=bool1] Duplicate feature definition")
            assertThat(deserialize(serialize(featureStore, mapOf()), mapOf())).isEqualTo(featureStore)
        }

        @Test
        fun testAddMultipleFeaturesWith_SameNameAndType_DifferentDependantModules() {
            val state = CompilerState()
            val module1 = Module("com.test")
            val module2 = Module("com.test.different")
            val module3 = Module("com.another.one")
            val module4 = Module("com.for.versioned.bool")
            module2.ancestor = module1
            module1.descendants += module2
            module3.imports += module2
            module4.imports += module2
            state.context = CompilerContext()
            val featureStore = FeatureStore()
            featureStore.addDefinition(module1, bool1FeatureDefinition, state)
            state.context.module = module2.fullName
            state.context.feature = bool1FeatureDefinition.code
            featureStore.addDefinition(module2, bool1FeatureDefinition, state)
            assertThat(state.warnings).containsExactly("[module=com.test.different, feature=bool1] Feature also defined in module: com.test")
            state.warnings.clear()
            state.context.module = module3.fullName
            state.context.feature = bool1FeatureDefinition.code
            featureStore.addDefinition(module3, bool1FeatureDefinition, state)
            assertThat(state.warnings).containsExactly("[module=com.another.one, feature=bool1] Feature also defined in module: com.test.different")
            state.warnings.clear()
            state.context.module = module4.fullName
            state.context.feature = boolVersioned1FeatureDefinition.code
            featureStore.addDefinition(module4, boolVersioned1FeatureDefinition, state)
            assertThat(state.warnings).containsExactly("[module=com.for.versioned.bool, feature=bool1] Feature also defined in module: com.test.different")
            // TODO Fix Serialization
//            assertThat(deserialize(serialize(featureStore, mapOf()), mapOf())).isEqualTo(featureStore)
        }

        @Test
        fun testAddMultipleFeaturesWith_SameNameAndType_DifferentIndependentModules() {
            val state = CompilerState()
            val module1 = Module("com.test")
            val module2 = Module("com.independent")
            val featureStore = FeatureStore()
            featureStore.addDefinition(module1, bool1FeatureDefinition, state)
            featureStore.addDefinition(module2, bool1FeatureDefinition, state)
            assertThat(state.errors + state.warnings).isEmpty()
// TODO Fix Serialization
//            assertThat(deserialize(serialize(featureStore, mapOf()), mapOf())).isEqualTo(featureStore)
        }

        @Test
        fun testAddMultipleFeaturesWith_SameNameAndModule_DifferentTypes() {
            val state = CompilerState()
            val module = Module("com.test")
            state.context = CompilerContext()
            state.context.module = module.fullName
            val feature2 = PrlEnumFeatureDefinition("x1", listOf())
            val feature3 = PrlIntFeatureDefinition("x1", IntRange.interval(1, 1))
            val featureStore = FeatureStore()
            featureStore.addDefinition(module, PrlBooleanFeatureDefinition("x1", false), state)
            state.context.feature = feature2.code
            featureStore.addDefinition(module, feature2, state)
            assertThat(state.errors).containsExactly("[module=com.test, feature=x1] Duplicate feature definition")
            state.errors.clear()
            state.context.feature = feature3.code
            featureStore.addDefinition(module, feature3, state)
            assertThat(state.errors).containsExactly("[module=com.test, feature=x1] Duplicate feature definition")
            assertThat(deserialize(serialize(featureStore, mapOf()), mapOf())).isEqualTo(featureStore)
        }

        @Test
        fun testAddMultipleFeaturesWith_SameName_DifferentTypesAndDependentModules() {
            val state = CompilerState()
            val module1 = Module("com.test")
            val module2 = Module("com.different")
            val module3 = Module("com.another.one")
            module2.ancestor = module1
            module1.descendants += module2
            module2.imports += module3
            val feature1 = PrlBooleanFeatureDefinition("x1", false)
            val feature2 = PrlEnumFeatureDefinition("x1", listOf())
            val feature3 = PrlIntFeatureDefinition("x1", IntRange.interval(1, 1))
            val featureStore = FeatureStore()
            state.context = CompilerContext()
            featureStore.addDefinition(module1, feature1, state)
            state.context.module = module2.fullName
            state.context.feature = feature2.code
            featureStore.addDefinition(module2, feature2, state)
            assertThat(state.errors + state.warnings).isEmpty()
            state.context.module = module3.fullName
            state.context.feature = feature3.code
            featureStore.addDefinition(module3, feature3, state)
            assertThat(state.errors + state.warnings).isEmpty()
            // TODO Fix Serialization
//            assertThat(deserialize(serialize(featureStore, mapOf()), mapOf())).isEqualTo(featureStore)
        }

        @Test
        fun testAddMultipleFeaturesWith_SameName_DifferentTypesAndIndependentModules() {
            val state = CompilerState()
            val module1 = Module("com.test")
            val module2 = Module("com.different")
            val module3 = Module("com.another.one")
            val feature1 = PrlBooleanFeatureDefinition("x1", false)
            val feature2 = PrlEnumFeatureDefinition("x1", listOf())
            val feature3 = PrlIntFeatureDefinition("x1", IntRange.interval(1, 1))
            val featureStore = FeatureStore()
            state.context = CompilerContext()
            featureStore.addDefinition(module1, feature1, state)
            state.context.module = module2.fullName
            state.context.feature = feature2.code
            featureStore.addDefinition(module2, feature2, state)
            assertThat(state.errors + state.warnings).isEmpty()
            state.context.module = module3.fullName
            state.context.feature = feature3.code
            featureStore.addDefinition(module3, feature3, state)
            assertThat(state.errors + state.warnings).isEmpty()
            // TODO Fix Serialization
//            assertThat(deserialize(serialize(featureStore, mapOf()), mapOf())).isEqualTo(featureStore)
        }

        @Test
        fun testAddMultipleFeatures_SameName_WithVersionedBooleanFeatures() {
            val state = CompilerState()
            val module = Module("com.test")
            state.context = CompilerContext()
            state.context.module = module.fullName
            val featureBoolVersioned1 = PrlBooleanFeatureDefinition("x1", true)
            val featureBoolVersioned2 = PrlBooleanFeatureDefinition("x1", true)
            val featureBool = PrlBooleanFeatureDefinition("x1", false)
            val featureStore = FeatureStore()
            featureStore.addDefinition(module, featureBoolVersioned1, state)
            state.context.feature = featureBoolVersioned2.code
            featureStore.addDefinition(module, featureBoolVersioned2, state)
            assertThat(state.errors + state.warnings).containsExactly("[module=com.test, feature=x1] Duplicate feature definition")
            state.errors.clear()
            state.context.feature = featureBool.code
            featureStore.addDefinition(module, featureBool, state)
            assertThat(state.errors + state.warnings).containsExactly("[module=com.test, feature=x1] Duplicate feature definition")
            assertThat(deserialize(serialize(featureStore, mapOf()), mapOf())).isEqualTo(featureStore)
        }

        @Test
        fun testAddMultipleFeaturesWith_SameTypeAndModule_DifferentNames() {
            val state = CompilerState()
            val module = Module("com.test")
            state.context = CompilerContext()
            state.context.module = module.fullName
            val feature1 = PrlBooleanFeatureDefinition("x1", false)
            val feature2 = PrlBooleanFeatureDefinition("x2", false)
            val feature3 = PrlBooleanFeatureDefinition("x3", false)
            val feature4Versioned = PrlBooleanFeatureDefinition("x4", true)
            val featureStore = FeatureStore()
            featureStore.addDefinition(module, feature1, state)
            state.context.feature = feature2.code
            featureStore.addDefinition(module, feature2, state)
            assertThat(state.errors + state.warnings).isEmpty()
            state.context.feature = feature3.code
            featureStore.addDefinition(module, feature3, state)
            assertThat(state.errors + state.warnings).isEmpty()
            state.context.feature = feature4Versioned.code
            featureStore.addDefinition(module, feature4Versioned, state)
            assertThat(state.errors + state.warnings).isEmpty()
            assertThat(deserialize(serialize(featureStore, mapOf()), mapOf())).isEqualTo(featureStore)
        }

        @Test
        fun testAddMultipleFeaturesWith_SameType_DifferentNamesAndDependentModules() {
            val state = CompilerState()
            val module1 = Module("com.test")
            val module2 = Module("com.different")
            val module3 = Module("com.another.one")
            val module4 = Module("com.for.versioned.bool")
            module2.ancestor = module1
            module1.descendants += module2
            module3.imports += module2
            module4.imports += module2
            val feature1 = PrlBooleanFeatureDefinition("x1", false)
            val feature2 = PrlBooleanFeatureDefinition("x2", false)
            val feature3 = PrlBooleanFeatureDefinition("x3", false)
            val feature4Versioned = PrlBooleanFeatureDefinition("x4", true)
            val featureStore = FeatureStore()
            state.context = CompilerContext()
            featureStore.addDefinition(module1, feature1, state)
            state.context.module = module2.fullName
            state.context.feature = feature2.code
            featureStore.addDefinition(module2, feature2, state)
            assertThat(state.errors + state.warnings).isEmpty()
            state.context.module = module3.fullName
            state.context.feature = feature3.code
            featureStore.addDefinition(module3, feature3, state)
            assertThat(state.errors + state.warnings).isEmpty()
            state.context.module = module4.fullName
            state.context.feature = feature4Versioned.code
            featureStore.addDefinition(module4, feature4Versioned, state)
            assertThat(state.errors + state.warnings).isEmpty()
            assertThat(deserialize(serialize(featureStore, mapOf()), mapOf())).isEqualTo(featureStore)
        }

        @Test
        fun testAddMultipleFeaturesWith_SameType_DifferentNamesAndIndependentModules() {
            val state = CompilerState()
            val module1 = Module("com.test")
            val module2 = Module("com.different")
            val module3 = Module("com.another.one")
            val module4 = Module("com.for.versioned.bool")
            val feature1 = PrlBooleanFeatureDefinition("x1", false)
            val feature2 = PrlBooleanFeatureDefinition("x2", false)
            val feature3 = PrlBooleanFeatureDefinition("x3", false)
            val feature4Versioned = PrlBooleanFeatureDefinition("x4", true)
            val featureStore = FeatureStore()
            state.context = CompilerContext()
            featureStore.addDefinition(module1, feature1, state)
            state.context.module = module2.fullName
            state.context.feature = feature2.code
            featureStore.addDefinition(module2, feature2, state)
            assertThat(state.errors + state.warnings).isEmpty()
            state.context.module = module3.fullName
            state.context.feature = feature3.code
            featureStore.addDefinition(module3, feature3, state)
            assertThat(state.errors + state.warnings).isEmpty()
            state.context.module = module4.fullName
            state.context.feature = feature4Versioned.code
            featureStore.addDefinition(module4, feature4Versioned, state)
            assertThat(state.errors + state.warnings).isEmpty()
            assertThat(deserialize(serialize(featureStore, mapOf()), mapOf())).isEqualTo(featureStore)
        }

        @Test
        fun testAddMultipleFeaturesWith_SameModule_DifferentNamesAndTypes() {
            val state = CompilerState()
            val module = Module("com.test")
            state.context = CompilerContext()
            state.context.module = module.fullName
            val feature1 = bool1FeatureDefinition
            val feature2 = string1FeatureDefinition
            val feature3 = int1FeatureDefinition
            val featureStore = FeatureStore()
            featureStore.addDefinition(module, feature1, state)
            state.context.feature = feature2.code
            featureStore.addDefinition(module, feature2, state)
            assertThat(state.errors + state.warnings).isEmpty()
            state.context.feature = feature2.code
            featureStore.addDefinition(module, feature3, state)
            assertThat(state.errors + state.warnings).isEmpty()
            assertThat(deserialize(serialize(featureStore, mapOf()), mapOf())).isEqualTo(featureStore)
        }

        @Test
        fun testAddMultipleFeaturesWith_DifferentNamesAndTypesAndDependentModules() {
            val state = CompilerState()
            val module1 = Module("com.test")
            val module2 = Module("com.different")
            module2.ancestor = module1
            module1.descendants += module2
            val module3 = Module("com.another.one")
            module3.imports += module2
            val feature1 = bool1FeatureDefinition
            val feature2 = string1FeatureDefinition
            val feature3 = int1FeatureDefinition
            val featureStore = FeatureStore()
            state.context = CompilerContext()
            featureStore.addDefinition(module1, feature1, state)
            state.context.module = module2.fullName
            state.context.feature = feature2.code
            featureStore.addDefinition(module2, feature2, state)
            assertThat(state.errors + state.warnings).isEmpty()
            state.context.module = module3.fullName
            state.context.feature = feature2.code
            featureStore.addDefinition(module3, feature3, state)
            assertThat(state.errors + state.warnings).isEmpty()
            assertThat(deserialize(serialize(featureStore, mapOf()), mapOf())).isEqualTo(featureStore)
        }

        @Test
        fun testAddMultipleFeaturesWith_DifferentNamesAndTypesAndIndependentModules() {
            val state = CompilerState()
            val module1 = Module("com.test")
            val module2 = Module("com.different")
            val module3 = Module("com.another.one")
            val feature1 = bool1FeatureDefinition
            val feature2 = string1FeatureDefinition
            val feature3 = int1FeatureDefinition
            state.context = CompilerContext()
            val featureStore = FeatureStore()
            featureStore.addDefinition(module1, feature1, state)
            state.context.module = module2.fullName
            state.context.feature = feature2.code
            featureStore.addDefinition(module2, feature2, state)
            assertThat(state.errors + state.warnings).isEmpty()
            state.context.module = module3.fullName
            state.context.feature = feature2.code
            featureStore.addDefinition(module3, feature3, state)
            assertThat(state.errors + state.warnings).isEmpty()
            assertThat(deserialize(serialize(featureStore, mapOf()), mapOf())).isEqualTo(featureStore)
        }

        @Test
        fun testAddMultipleFeaturesWith_SameNameAndTypeAndModule_MultipleFeatures() {
            val state = CompilerState()
            val module = Module("com.test")
            val moduleDiff = Module("com.test.different")
            val moduleDiffIndependent = Module("com.independent")
            moduleDiff.ancestor = module
            module.descendants += moduleDiff
            val feature1 = bool1FeatureDefinition
            val feature1Diff = bool1FeatureDefinition
            val feature1DiffIndependent = bool1FeatureDefinition
            val feature2 = bool1FeatureDefinition
            val featureStore = FeatureStore()
            state.context = CompilerContext()
            featureStore.addDefinition(module, feature1, state)
            state.context.module = moduleDiff.fullName
            state.context.feature = feature1Diff.code
            featureStore.addDefinition(moduleDiff, feature1Diff, state)
            assertThat(state.warnings).containsExactly("[module=com.test.different, feature=bool1] Feature also defined in module: com.test")
            state.warnings.clear()
            state.context.module = moduleDiffIndependent.fullName
            state.context.feature = feature1DiffIndependent.code
            featureStore.addDefinition(moduleDiffIndependent, feature1DiffIndependent, state)
            assertThat(state.errors + state.warnings).isEmpty()
            state.context.module = module.fullName
            state.context.feature = feature2.code
            featureStore.addDefinition(module, feature2, state)
            assertThat(state.errors).containsExactly("[module=com.test, feature=bool1] Duplicate feature definition")
            // TODO Fix Serialization
//            assertThat(deserialize(serialize(featureStore, mapOf()), mapOf())).isEqualTo(featureStore)
        }

        @Test
        fun testAddMultipleFeatures_WithDifferentVisibilities_InDifferentModules() {
            val state = CompilerState()
            val brModuleCurrent = Module("com.booleworks.current")
            state.context = CompilerContext()
            state.context.module = brModuleCurrent.fullName
            val brModuleAncestor = Module("com.booleworks")
            brModuleAncestor.descendants = listOf(brModuleCurrent)
            brModuleCurrent.ancestor = brModuleAncestor
            val brModuleImported = Module("com.completely.different")
            brModuleCurrent.imports += brModuleImported
            val featureAncestorPrivate = PrlBooleanFeatureDefinition("x1", false, visibility = Visibility.PRIVATE)
            val featureImportedInternal = PrlBooleanFeatureDefinition("x1", false, visibility = Visibility.INTERNAL)
            val featureImportedPrivate = PrlBooleanFeatureDefinition("x1", false, visibility = Visibility.PRIVATE)
            val featureCurrentPublic = PrlBooleanFeatureDefinition("x1", false, visibility = Visibility.PUBLIC)

            val featureStorePublic = FeatureStore().apply {
                listOf(featureAncestorPrivate, featureAncestorPrivate, featureAncestorPrivate).forEach {
                    addDefinition(brModuleAncestor, it, state)
                }
            }
            state.errors.clear()
            state.warnings.clear()
            featureStorePublic.addDefinition(brModuleCurrent, featureCurrentPublic, state)
            assertThat(state.errors + state.warnings).isEmpty()

            val featureCurrentInternal = PrlBooleanFeatureDefinition("x1", false, visibility = Visibility.INTERNAL)
            val featureStoreInternal = FeatureStore()
            featureStoreInternal.addDefinition(brModuleAncestor, featureAncestorPrivate, state)
            featureStoreInternal.addDefinition(brModuleImported, featureImportedInternal, state)
            featureStoreInternal.addDefinition(brModuleImported, featureImportedPrivate, state)
            state.errors.clear()
            state.warnings.clear()
            featureStoreInternal.addDefinition(brModuleCurrent, featureCurrentInternal, state)
            assertThat(state.errors + state.warnings).isEmpty()

            val featureCurrentImported = PrlBooleanFeatureDefinition("x1", false, visibility = Visibility.PRIVATE)
            val featureStorePrivate = FeatureStore()
            featureStoreInternal.addDefinition(brModuleAncestor, featureAncestorPrivate, state)
            featureStoreInternal.addDefinition(brModuleImported, featureImportedInternal, state)
            featureStoreInternal.addDefinition(brModuleImported, featureImportedPrivate, state)
            state.errors.clear()
            state.warnings.clear()
            featureStorePrivate.addDefinition(brModuleCurrent, featureCurrentImported, state)
            assertThat(state.errors + state.warnings).isEmpty()
            // TODO Fix Serialization
//            assertThat(deserialize(serialize(featureStoreInternal, mapOf()), mapOf())).isEqualTo(featureStoreInternal)
        }

        @Test
        fun testAddMultipleFeaturesWith_SameNameAndTypeAndModule_DifferentSlicingProperties() {
            val state = CompilerState()
            val module = Module("com.test")
            val featureStore = FeatureStore()
            val propertyStore = PropertyStore()
            propertyStore.addSlicingPropertyDefinition(PrlSlicingEnumPropertyDefinition("p1"), state)
            val properties1 = listOf(PrlBooleanProperty("ns1", BooleanRange.list(true), 11), PrlEnumProperty("p1", "A", 12))
            val bool1FeatureDefinitionSlicing = PrlBooleanFeatureDefinition("bool1", false, "", Visibility.PUBLIC, properties1, 10)
            val properties2 = listOf(PrlBooleanProperty("ns1", BooleanRange.list(true), 21), PrlEnumProperty("p1", "B", 22))
            val bool2FeatureDefinitionSlicing = PrlBooleanFeatureDefinition("bool1", false, "", Visibility.PUBLIC, properties2, 20)
            val properties3 = listOf(PrlBooleanProperty("ns1", BooleanRange.list(true), 31), PrlEnumProperty("p1", "C", 32))
            val bool3FeatureDefinitionSlicing = PrlBooleanFeatureDefinition("bool1", true, "desc", Visibility.INTERNAL, properties3, 30)

            state.context = CompilerContext(module.fullName, bool1FeatureDefinitionSlicing.code, null, bool1FeatureDefinitionSlicing.lineNumber)
            featureStore.addDefinition(module, bool1FeatureDefinitionSlicing, state, propertyStore.slicingPropertyDefinitions)
            state.context = CompilerContext(module.fullName, bool2FeatureDefinitionSlicing.code, null, bool2FeatureDefinitionSlicing.lineNumber)
            featureStore.addDefinition(module, bool2FeatureDefinitionSlicing, state, propertyStore.slicingPropertyDefinitions)
            state.context = CompilerContext(module.fullName, bool3FeatureDefinitionSlicing.code, null, bool3FeatureDefinitionSlicing.lineNumber)
            featureStore.addDefinition(module, bool3FeatureDefinitionSlicing, state, propertyStore.slicingPropertyDefinitions)

            assertThat(state.warnings + state.errors).isEmpty()
            assertThat(featureStore.booleanFeatures.size).isEqualTo(1)
            assertThat(featureStore.booleanFeatures["bool1"]!!.size).isEqualTo(3)
            assertThat(deserialize(serialize(featureStore, mapOf()), mapOf())).isEqualTo(featureStore)
        }

        @Test
        fun testAddMultipleFeaturesWith_SameNameAndTypeAndModule_DuplicateSlicingProperties() {
            val state = CompilerState()
            val module = Module("com.test")
            val featureStore = FeatureStore()
            val propertyStore = PropertyStore()
            propertyStore.addSlicingPropertyDefinition(PrlSlicingEnumPropertyDefinition("p1"), state)
            val properties1 = listOf(PrlBooleanProperty("ns1", BooleanRange.list(true), 11), PrlEnumProperty("p1", "A", 12))
            val bool1FeatureDefinitionSlicing = PrlBooleanFeatureDefinition("bool1", false, "", Visibility.PUBLIC, properties1, 10)
            val properties2 = listOf(PrlBooleanProperty("ns2", BooleanRange.list(true), 21), PrlEnumProperty("p2", "A", 22))
            val bool2FeatureDefinitionSlicing = PrlBooleanFeatureDefinition("bool1", false, "", Visibility.PUBLIC, properties2, 20)
            val properties3 = listOf(PrlBooleanProperty("ns3", BooleanRange.list(true), 31), PrlEnumProperty("p3", "A", 32))
            val stringFeatureDefinitionSlicing = PrlEnumFeatureDefinition("string1", listOf(), "desc", Visibility.PUBLIC, properties3, 30)

            state.context = CompilerContext(module.fullName, bool1FeatureDefinitionSlicing.code, null, bool1FeatureDefinitionSlicing.lineNumber)
            featureStore.addDefinition(module, bool1FeatureDefinitionSlicing, state, propertyStore.slicingPropertyDefinitions)
            state.context = CompilerContext(module.fullName, bool2FeatureDefinitionSlicing.code, null, bool2FeatureDefinitionSlicing.lineNumber)
            featureStore.addDefinition(module, bool2FeatureDefinitionSlicing, state, propertyStore.slicingPropertyDefinitions)

            assertThat(state.warnings).isEmpty()
            assertThat(state.errors).containsExactly("[module=com.test, feature=bool1, lineNumber=20] Duplicate feature definition")

            state.context = CompilerContext(module.fullName, stringFeatureDefinitionSlicing.code, null, stringFeatureDefinitionSlicing.lineNumber)
            featureStore.addDefinition(module, stringFeatureDefinitionSlicing, state, propertyStore.slicingPropertyDefinitions)


            assertThat(deserialize(serialize(featureStore, mapOf()), mapOf())).isEqualTo(featureStore)
        }
    }

    /////////////////////////////////
    // Find Matching Features Tests//
    /////////////////////////////////
    @Nested
    inner class FindMatchingFeaturesTest {
        @Test
        fun testFeatureNotDeclaredInFullFeatureStore() {
            val state = CompilerState()
            state.context = CompilerContext()
            val brModule1 = Module("com.booleworks")
            val brModule2 = Module("com.booleworks.current")
            val brModule3 = Module("com.completely.different")
            brModule1.descendants = listOf(brModule2)
            brModule2.ancestor = brModule1
            val mh = ModuleHierarchy(mapOf(Pair(brModule1.fullName, brModule1), Pair(brModule2.fullName, brModule2), Pair(brModule3.fullName, brModule3)))
            val featureStore = FeatureStore()
            featureStore.addDefinition(brModule1, bool1FeatureDefinition, state)
            featureStore.addDefinition(brModule2, bool1FeatureDefinition, state)
            featureStore.addDefinition(brModule3, bool1FeatureDefinition, state)
            featureStore.addDefinition(brModule1, string1FeatureDefinition, state)
            featureStore.addDefinition(brModule2, string1FeatureDefinition, state)
            featureStore.addDefinition(brModule3, string1FeatureDefinition, state)
            featureStore.addDefinition(brModule1, int1FeatureDefinition, state)
            featureStore.addDefinition(brModule2, int1FeatureDefinition, state)
            featureStore.addDefinition(brModule3, int1FeatureDefinition, state)
            val notMatchingFeature = s1
            state.context.module = brModule1.fullName
            val features = featureStore.findMatchingDefinitions(brModule1, notMatchingFeature, mh)
            assertThat(features).isEmpty()
        }

        @Test
        fun testFindMatchingFeatureDeclaredPublicInDifferentModules() {
            val state = CompilerState()
            state.context = CompilerContext()
            val brModuleAncestor = Module("com.booleworks")
            val brModuleCurrent = Module("com.booleworks.current")
            val brModuleImported = Module("com.completely.different")
            brModuleCurrent.ancestor = brModuleAncestor
            brModuleAncestor.descendants = listOf(brModuleCurrent)
            brModuleCurrent.imports += brModuleImported
            val moduleHierarchy = ModuleHierarchy(
                mapOf(
                    Pair(brModuleAncestor.fullName, brModuleAncestor),
                    Pair(brModuleCurrent.fullName, brModuleCurrent),
                    Pair(brModuleImported.fullName, brModuleImported)
                )
            )
            val featureStore = FeatureStore()
            featureStore.addDefinition(brModuleCurrent, bool1FeatureDefinition, state)
            featureStore.addDefinition(brModuleCurrent, bool2FeatureDefinition, state)
            featureStore.addDefinition(brModuleAncestor, bool2FeatureDefinition, state)
            featureStore.addDefinition(brModuleImported, bool2FeatureDefinition, state)
            featureStore.addDefinition(brModuleAncestor, string1FeatureDefinition, state)
            featureStore.addDefinition(brModuleImported, string1FeatureDefinition, state)
            featureStore.addDefinition(brModuleAncestor, string2FeatureDefinition, state)
            featureStore.addDefinition(brModuleImported, int1FeatureDefinition, state)

            val matchingFeatureInSameModule = PrlFeature(bool1FeatureDefinition.code)
            state.context.module = brModuleCurrent.fullName
            val featuresInSameModule = featureStore.findMatchingDefinitions(brModuleCurrent, matchingFeatureInSameModule, moduleHierarchy)
            assertThat(featuresInSameModule.size).isEqualTo(1)
            assertThat(featuresInSameModule.first()).isEqualTo(BooleanFeatureDefinition(brModuleCurrent, bool1FeatureDefinition))

            val matchingFeatureInSameModule2 = PrlFeature(bool2FeatureDefinition.code)
            val featuresInSameModule2 = featureStore.findMatchingDefinitions(brModuleCurrent, matchingFeatureInSameModule2, moduleHierarchy)
            assertThat(featuresInSameModule2.size).isEqualTo(1)
            assertThat(featuresInSameModule2.first()).isEqualTo(BooleanFeatureDefinition(brModuleCurrent, bool2FeatureDefinition))

            val matchingFeatureInMultipleModules = PrlFeature(string1FeatureDefinition.code)
            val featuresInInMultipleModules = featureStore.findMatchingDefinitions(brModuleCurrent, matchingFeatureInMultipleModules, moduleHierarchy)
            assertThat(featuresInInMultipleModules.size).isEqualTo(2)
            assertThat(featuresInInMultipleModules).containsExactlyInAnyOrder(
                EnumFeatureDefinition(brModuleAncestor, string1FeatureDefinition),
                EnumFeatureDefinition(brModuleImported, string1FeatureDefinition)
            )

            val matchingFeatureInAncestorModule = PrlFeature(string2FeatureDefinition.code)
            val featuresInIAncestorModule = featureStore.findMatchingDefinitions(brModuleCurrent, matchingFeatureInAncestorModule, moduleHierarchy)
            assertThat(featuresInIAncestorModule.first()).isEqualTo(EnumFeatureDefinition(brModuleAncestor, string2FeatureDefinition))

            val matchingFeatureInImportedModule = PrlFeature(int1FeatureDefinition.code)
            val featuresInImportModule = featureStore.findMatchingDefinitions(brModuleCurrent, matchingFeatureInImportedModule, moduleHierarchy)
            assertThat(featuresInImportModule.first()).isEqualTo(IntFeatureDefinition(brModuleImported, int1FeatureDefinition))
        }

        // module a {
        //      public feature f1
        //      # public string feature f1 ["a"] (would have been an Error in feature collection)
        // }
        // module x {
        //      public feature f1       (does not match)
        // }
        // module a.b {
        //      feature f1
        //      rule f1 & a.f1          (exact matching if full qualified)
        // }
        @Test
        fun testFindMatchingFeatureDeclaredPublicInDifferentModules_FeatureFullQualified() {
            val state = CompilerState()
            state.context = CompilerContext()
            val brModuleAncestor = Module("com.booleworks")
            val brModuleCurrent = Module("com.booleworks.current")
            brModuleCurrent.ancestor = brModuleAncestor
            brModuleAncestor.descendants = listOf(brModuleCurrent)
            val moduleHierarchy = ModuleHierarchy(mapOf(Pair(brModuleAncestor.fullName, brModuleAncestor), Pair(brModuleCurrent.fullName, brModuleCurrent)))
            val featureStore = FeatureStore()
            featureStore.addDefinition(brModuleCurrent, bool1FeatureDefinition, state)
            featureStore.addDefinition(brModuleAncestor, bool1FeatureDefinition, state)

            val matchingFeatureFullQualified = PrlFeature("${brModuleAncestor.fullName}.${bool1FeatureDefinition.code}")

            state.context.module = brModuleCurrent.fullName
            state.context.feature = matchingFeatureFullQualified.featureCode
            val featuresInSameModule = featureStore.findMatchingDefinitions(brModuleCurrent, matchingFeatureFullQualified, moduleHierarchy)
            assertThat(featuresInSameModule.size).isEqualTo(1)
            assertThat(featuresInSameModule.first()).isEqualTo(BooleanFeatureDefinition(brModuleAncestor, bool1FeatureDefinition))
        }

        // module a.b {
        //      public feature f1
        //      public string feature s1 ["test", "text"]
        //      public int feature i1 [1-10]
        // }
        // module a.b.c {
        //      boolean feature intern2
        //      rule intern2 & a.b.f1       (exact matching if full qualified)
        //      rule [a.b.s1 != "not text"]
        //      rule [a.b.s1 in ["a", "b", "c"]]
        //      rule [a.b.i1 in [1-7]]
        // }
        @Test
        fun testFindMatchingFeatureDeclaredPublicInDifferentModules_MultipleFeaturesFullQualified() {
            val state = CompilerState()
            state.context = CompilerContext()
            val brModuleAncestor = Module("com.booleworks")
            val brModuleCurrent = Module("com.booleworks.current")
            brModuleCurrent.ancestor = brModuleAncestor
            brModuleAncestor.descendants += brModuleCurrent
            state.context.module = brModuleCurrent.fullName
            val moduleHierarchy = ModuleHierarchy(mapOf(Pair(brModuleAncestor.fullName, brModuleAncestor), Pair(brModuleCurrent.fullName, brModuleCurrent)))

            val featureStore = FeatureStore().apply {
                addDefinition(brModuleAncestor, bool1FeatureDefinition, state)
                addDefinition(brModuleAncestor, string1FeatureDefinition, state)
                addDefinition(brModuleAncestor, int1FeatureDefinition, state)
                addDefinition(brModuleCurrent, bool2FeatureDefinition, state)
            }
            val boolFeatureFullQualified = PrlFeature("${brModuleAncestor.fullName}.${bool1FeatureDefinition.code}")
            val stringFeatureFullQualified = PrlFeature("${brModuleAncestor.fullName}.${string1FeatureDefinition.code}")
            val intFeatureFullQualified = PrlFeature("${brModuleAncestor.fullName}.${int1FeatureDefinition.code}")

            state.context.feature = boolFeatureFullQualified.featureCode
            val boolFeatures = featureStore.findMatchingDefinitions(brModuleCurrent, boolFeatureFullQualified, moduleHierarchy)
            assertThat(boolFeatures.size).isEqualTo(1)
            assertThat(boolFeatures.first()).isEqualTo(BooleanFeatureDefinition(brModuleAncestor, bool1FeatureDefinition))
            state.context.feature = stringFeatureFullQualified.featureCode
            val stringFeatures = featureStore.findMatchingDefinitions(brModuleCurrent, stringFeatureFullQualified, moduleHierarchy)
            assertThat(stringFeatures.size).isEqualTo(1)
            assertThat(stringFeatures.first()).isEqualTo(EnumFeatureDefinition(brModuleAncestor, string1FeatureDefinition))
            state.context.feature = intFeatureFullQualified.featureCode
            val intFeatures = featureStore.findMatchingDefinitions(brModuleCurrent, intFeatureFullQualified, moduleHierarchy)
            assertThat(intFeatures.size).isEqualTo(1)
            assertThat(intFeatures.first()).isEqualTo(IntFeatureDefinition(brModuleAncestor, int1FeatureDefinition))
        }

        // module a { }
        // module a.b {
        //      feature f1
        //      rule f1 & a.f1       (Not Defined a.f1)
        // }
        @Test
        fun testFindMatchingFeatureDeclaredPublicInDifferentModules_FeatureFullQualifiedUndefined() {
            val state = CompilerState()
            state.context = CompilerContext()
            val brModuleAncestor = Module("com.booleworks")
            val brModuleCurrent = Module("com.booleworks.current")
            brModuleCurrent.ancestor = brModuleAncestor
            brModuleAncestor.descendants += brModuleCurrent
            val moduleHierarchy = ModuleHierarchy(mapOf(Pair(brModuleAncestor.fullName, brModuleAncestor), Pair(brModuleCurrent.fullName, brModuleCurrent)))

            val featureStore = FeatureStore().apply {
                addDefinition(brModuleCurrent, bool1FeatureDefinition, state)
            }
            val notDefinedFeatureFullQualified = PrlFeature("${brModuleAncestor.fullName}.${bool1FeatureDefinition.code}")

            state.context.module = brModuleCurrent.fullName
            state.context.feature = notDefinedFeatureFullQualified.featureCode
            val featuresInSameModule = featureStore.findMatchingDefinitions(brModuleCurrent, notDefinedFeatureFullQualified, moduleHierarchy)
            assertThat(featuresInSameModule).isEmpty()
        }

        // module a {
        //      private feature f1
        // }
        // module a.b {
        //      feature f1
        //      rule f1 & a.f1       (Not Defined a.f1)
        // }
        @Test
        fun testFindMatchingFeatureDeclaredPublicInDifferentModules_FeatureFullQualifiedInvisible() {
            val state = CompilerState()
            state.context = CompilerContext()
            val brModuleAncestor = Module("com.booleworks")
            val brModuleCurrent = Module("com.booleworks.current")
            brModuleCurrent.ancestor = brModuleAncestor
            brModuleAncestor.descendants += brModuleCurrent
            val moduleHierarchy = ModuleHierarchy(mapOf(Pair(brModuleAncestor.fullName, brModuleAncestor), Pair(brModuleCurrent.fullName, brModuleCurrent)))

            val featureDefinitionPrivate = PrlBooleanFeatureDefinition(bool1FeatureDefinition.code, false, visibility = Visibility.PRIVATE)
            val featureStore = FeatureStore().apply {
                addDefinition(brModuleCurrent, bool1FeatureDefinition, state)
                addDefinition(brModuleAncestor, featureDefinitionPrivate, state)
            }
            val privateFeatureFullQualified = PrlFeature("${brModuleAncestor.fullName}.${featureDefinitionPrivate.code}")

            state.context.module = brModuleCurrent.fullName
            state.context.feature = privateFeatureFullQualified.featureCode
            val featuresInSameModule = featureStore.findMatchingDefinitions(brModuleCurrent, privateFeatureFullQualified, moduleHierarchy)
            assertThat(featuresInSameModule).isEmpty()
        }

        // module x {
        //      public feature f1
        // }
        // module a.b {
        //      import module x
        //      boolean feature f1
        //      rule x.f1 & f1       (exact matching if full qualified)
        // }
        @Test
        fun testFindMatchingFeatureDeclaredPublicInDifferentModules_FeatureFullQualifiedInImported() {
            val state = CompilerState()
            state.context = CompilerContext()
            val brModuleCurrent = Module("com.booleworks")
            val brModuleImported = Module("com.booleworks.defined")
            brModuleCurrent.imports += brModuleImported
            val moduleHierarchy = ModuleHierarchy(mapOf(Pair(brModuleImported.fullName, brModuleImported), Pair(brModuleCurrent.fullName, brModuleCurrent)))

            val featureStore = FeatureStore().apply {
                addDefinition(brModuleCurrent, bool1FeatureDefinition, state)
                addDefinition(brModuleImported, bool1FeatureDefinition, state)
            }
            val notMatchingFeatureFullQualified = PrlFeature("${brModuleImported.fullName}.${bool1FeatureDefinition.code}")

            state.context.module = brModuleCurrent.fullName
            state.context.feature = notMatchingFeatureFullQualified.featureCode
            val featuresInSameModule = featureStore.findMatchingDefinitions(brModuleCurrent, notMatchingFeatureFullQualified, moduleHierarchy)
            assertThat(featuresInSameModule.size).isEqualTo(1)
            assertThat(featuresInSameModule.first()).isEqualTo(BooleanFeatureDefinition(brModuleImported, bool1FeatureDefinition))
        }

        // module x {
        //      public feature f1
        // }
        // module a.b {
        //      import module x
        //      boolean feature intern2
        //      rule a.f1 & intern2       (exact matching if full qualified)
        // }
        @Test
        fun testFindMatchingFeatureDeclaredPublicInDifferentModules_FeatureFullQualifiedInImported2() {
            val state = CompilerState()
            state.context = CompilerContext()
            val brModuleCurrent = Module("com.booleworks")
            val brModuleImported = Module("com.booleworks.defined")
            brModuleCurrent.imports += brModuleImported
            val moduleHierarchy = ModuleHierarchy(mapOf(Pair(brModuleImported.fullName, brModuleImported), Pair(brModuleCurrent.fullName, brModuleCurrent)))

            val featureStore = FeatureStore().apply {
                addDefinition(brModuleCurrent, string1FeatureDefinition, state)
                addDefinition(brModuleImported, bool1FeatureDefinition, state)
            }
            val notMatchingFeatureFullQualified = PrlFeature("${brModuleImported.fullName}.${bool1FeatureDefinition.code}")

            state.context.module = brModuleCurrent.fullName
            state.context.feature = notMatchingFeatureFullQualified.featureCode
            val featuresInSameModule = featureStore.findMatchingDefinitions(brModuleCurrent, notMatchingFeatureFullQualified, moduleHierarchy)
            assertThat(featuresInSameModule.size).isEqualTo(1)
            assertThat(featuresInSameModule.first()).isEqualTo(BooleanFeatureDefinition(brModuleImported, bool1FeatureDefinition))
        }

        // module y { }
        // module x {
        //      public feature f1
        // }
        // module a.b {
        //      import module y
        //      boolean feature intern2
        //      rule a.f1 & intern2       (Not Defined a.f1)
        // }
        @Test
        fun testFindMatchingFeatureDeclaredPublicInDifferentModules_FeatureFullQualifiedUndefinedInImported() {
            val state = CompilerState()
            state.context = CompilerContext()
            val brModuleCurrent = Module("com.booleworks")
            val brModuleImportedUndefined = Module("com.booleworks.undefined")
            val brModuleNotImportedButDefined = Module("com.booleworks.defined")
            brModuleCurrent.imports += brModuleImportedUndefined

            val moduleHierarchy = ModuleHierarchy(
                mapOf(
                    Pair(brModuleImportedUndefined.fullName, brModuleImportedUndefined),
                    Pair(brModuleCurrent.fullName, brModuleCurrent),
                    Pair(brModuleNotImportedButDefined.fullName, brModuleNotImportedButDefined)
                )
            )
            val featureStore = FeatureStore()
            featureStore.addDefinition(brModuleCurrent, bool1FeatureDefinition, state)
            featureStore.addDefinition(brModuleNotImportedButDefined, bool1FeatureDefinition, state)

            val notMatchingFeatureFullQualified = PrlFeature("${brModuleImportedUndefined.fullName}.${bool1FeatureDefinition.code}")

            state.context.module = brModuleCurrent.fullName
            state.context.feature = notMatchingFeatureFullQualified.featureCode
            val featuresInSameModule = featureStore.findMatchingDefinitions(brModuleCurrent, notMatchingFeatureFullQualified, moduleHierarchy)
            assertThat(featuresInSameModule).isEmpty()
        }
    }

    /////////////////////////////////
    // Feature Uniqueness Tests//
    /////////////////////////////////
    @Nested
    inner class AddNotUniqueFeaturesTest {
        //	int feature f1 [1,2,3] {
        //		version 1
        //		event ["E1", "E2"]
        //	}
        //	int feature f1 [1,2,3,4] {
        //		version 2
        //		event ["E1", "E2"]
        //	}
        //	int feature f1 [1,2,3,5] {
        //		version 1
        //		event ["E3"]
        //	}
        @Test
        fun testAddMultipleFeatureDefinitions_WithUniqueSlices() {
            val state = CompilerState()
            val module = Module("com.booleworks")
            val featureStore = FeatureStore()
            val propertyStore = PropertyStore()
            propertyStore.addSlicingPropertyDefinition(PrlSlicingIntPropertyDefinition("version"), state)
            propertyStore.addSlicingPropertyDefinition(PrlSlicingEnumPropertyDefinition("event"), state)
            val properties1 = listOf(PrlIntProperty("version", IntRange.list(1), 11), PrlEnumProperty("event", EnumRange.list("E1", "E2"), 12))
            val featureDefinition1 = PrlEnumFeatureDefinition("enum1", listOf("a", "b"), "", Visibility.PUBLIC, properties1, 10)
            val properties2 = listOf(PrlIntProperty("version", IntRange.list(2), 21), PrlEnumProperty("event", EnumRange.list("E1", "E2"), 22))
            val featureDefinition2 = PrlEnumFeatureDefinition("enum1", listOf("a", "b", "C"), "", Visibility.PUBLIC, properties2, 20)
            val properties3 = listOf(PrlIntProperty("version", IntRange.list(1), 31), PrlEnumProperty("event", EnumRange.list("E3"), 32))
            val featureDefinition3 = PrlEnumFeatureDefinition("enum1", listOf("a", "b", "D"), "", Visibility.PUBLIC, properties3, 30)

            state.context = CompilerContext(module.fullName, featureDefinition1.code, null, featureDefinition1.lineNumber)
            featureStore.addDefinition(module, featureDefinition1, state, propertyStore.slicingPropertyDefinitions)
            state.context = CompilerContext(module.fullName, featureDefinition2.code, null, featureDefinition2.lineNumber)
            featureStore.addDefinition(module, featureDefinition2, state, propertyStore.slicingPropertyDefinitions)
            state.context = CompilerContext(module.fullName, featureDefinition3.code, null, featureDefinition3.lineNumber)
            featureStore.addDefinition(module, featureDefinition3, state, propertyStore.slicingPropertyDefinitions)

            assertThat(featureStore.enumFeatures.size).isEqualTo(1)
            assertThat(featureStore.enumFeatures[featureDefinition2.code]).containsExactlyInAnyOrder(
                FeatureDefinition.fromPrlModule(module, featureDefinition1),
                FeatureDefinition.fromPrlModule(module, featureDefinition2),
                FeatureDefinition.fromPrlModule(module, featureDefinition3)
            )
        }

        //	int feature f1 [1,2,3] {
        //		version 1
        //		event ["E1", "E2"]
        //	}
        //	int feature f1 [1,2,3,4] {
        //		version 1
        //		event ["E1"]		# Error because of the colling slice in version 1 + event "E1"
        //	}
        @Test
        fun testAddMultipleFeatureDefinitions_WithCollidingSlices() {
            val state = CompilerState()
            val module = Module("com.booleworks")
            val featureStore = FeatureStore()
            val propertyStore = PropertyStore()
            propertyStore.addSlicingPropertyDefinition(PrlSlicingIntPropertyDefinition("version"), state)
            propertyStore.addSlicingPropertyDefinition(PrlSlicingEnumPropertyDefinition("event"), state)
            val properties1 = listOf(PrlIntProperty("version", IntRange.list(1), 11), PrlEnumProperty("event", EnumRange.list("E1", "E2"), 12))
            val featureDefinition1 = PrlEnumFeatureDefinition("enum1", listOf("a", "b"), "", Visibility.PUBLIC, properties1, 10)
            val properties2 = listOf(PrlIntProperty("version", IntRange.list(1), 21), PrlEnumProperty("event", EnumRange.list("E1"), 22))
            val featureDefinition2 = PrlEnumFeatureDefinition("enum1", listOf("a", "b", "C"), "", Visibility.PUBLIC, properties2, 20)

            state.context = CompilerContext(module.fullName, featureDefinition1.code, null, featureDefinition1.lineNumber)
            featureStore.addDefinition(module, featureDefinition1, state, propertyStore.slicingPropertyDefinitions)
            state.context = CompilerContext(module.fullName, featureDefinition2.code, null, featureDefinition2.lineNumber)
            featureStore.addDefinition(module, featureDefinition2, state, propertyStore.slicingPropertyDefinitions)

            assertThat(featureStore.enumFeatures.size).isEqualTo(1)
            assertThat(featureStore.enumFeatures[featureDefinition2.code]).containsExactlyInAnyOrder(
                FeatureDefinition.fromPrlModule(module, featureDefinition1)
            )
            assertThat(state.warnings).isEmpty()
            assertThat(state.errors).containsExactly("[module=com.booleworks, feature=enum1, lineNumber=20] Duplicate feature definition")
        }

        //	int feature f1 [1,2,3] {
        //		version 1
        //		event ["E1", "E2"]
        //	}
        //	int feature f1 [1,2,3,4] {
        //		version 1			# Error, because property event is missing and thus all values of event are valid
        //	}
        @Test
        fun testAddMultipleFeatureDefinitions_WithCollidingSlicesThroughMissingProperty() {
            val state = CompilerState()
            val module = Module("com.booleworks")
            val featureStore = FeatureStore()
            val propertyStore = PropertyStore()
            propertyStore.addSlicingPropertyDefinition(PrlSlicingIntPropertyDefinition("version"), state)
            propertyStore.addSlicingPropertyDefinition(PrlSlicingEnumPropertyDefinition("event"), state)
            val properties1 = listOf(PrlIntProperty("version", IntRange.list(1), 11), PrlEnumProperty("event", EnumRange.list("E1", "E2"), 12))
            val featureDefinition1 = PrlEnumFeatureDefinition("enum1", listOf("a", "b"), "", Visibility.PUBLIC, properties1, 10)
            val properties2 = listOf(PrlIntProperty("version", IntRange.list(1), 21))
            val featureDefinition2 = PrlEnumFeatureDefinition("enum1", listOf("a", "b", "C"), "", Visibility.PUBLIC, properties2, 20)
            val featureDefinition3 = PrlEnumFeatureDefinition("enum1", listOf("a", "b", "D"), "", Visibility.PUBLIC, listOf(), 30)

            state.context = CompilerContext(module.fullName, featureDefinition1.code, null, featureDefinition1.lineNumber)
            featureStore.addDefinition(module, featureDefinition1, state, propertyStore.slicingPropertyDefinitions)
            state.context = CompilerContext(module.fullName, featureDefinition2.code, null, featureDefinition2.lineNumber)
            featureStore.addDefinition(module, featureDefinition2, state, propertyStore.slicingPropertyDefinitions)
            state.context = CompilerContext(module.fullName, featureDefinition3.code, null, featureDefinition3.lineNumber)
            featureStore.addDefinition(module, featureDefinition3, state, propertyStore.slicingPropertyDefinitions)

            assertThat(featureStore.enumFeatures.size).isEqualTo(1)
            assertThat(featureStore.enumFeatures[featureDefinition1.code]).containsExactlyInAnyOrder(
                FeatureDefinition.fromPrlModule(module, featureDefinition1)
            )
            assertThat(state.warnings).isEmpty()
            assertThat(state.errors).containsExactlyInAnyOrder(
                "[module=com.booleworks, feature=enum1, lineNumber=20] Duplicate feature definition",
                "[module=com.booleworks, feature=enum1, lineNumber=30] Duplicate feature definition"
            )
        }


        //	int feature f1 [1 - 100] {
        //		validity [2023-01-01 - 2023-04-01]
        //		event ["E1" - "E3"]
        //	}
        //	int feature f1 [101 - 999] {
        //		validity [2023-05-01 - 2040-12-31]
        //		event "E4"
        //	}
        //	int feature f1 [101 - 200] {
        //		validity [2020-01-01 - 2023-03-01]  # Error: Disjoint slices
        //		event "E1"
        //	}
        //	int feature f1 [201 - 400] {
        //      validity [2023-01-01 - 2023-04-01]
        //		event ["E2" - "E4"]                 # Error: Disjoint slices
        //	}
        @Test
        fun testAddMultipleFeaturesWith_SameNameAndTypeAndModule_DifferentSlicingProperties() {
            val state = CompilerState()
            val module = Module("com.test")
            val featureStore = FeatureStore()
            val propertyStore = PropertyStore()
            propertyStore.addSlicingPropertyDefinition(PrlSlicingDatePropertyDefinition("validity", 5), state)
            propertyStore.addSlicingPropertyDefinition(PrlSlicingEnumPropertyDefinition("event"), state)
            val properties1 = listOf(
                PrlDateProperty("validity", DateRange.interval(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 4, 1)), 11),
                PrlEnumProperty("event", EnumRange.list("E1", "E3"), 12)
            )
            val featureDefinitions1 = PrlIntFeatureDefinition("feature1", IntRange.interval(1, 100), "", Visibility.PUBLIC, properties1, 10)
            val properties2 = listOf(
                PrlDateProperty("validity", DateRange.interval(LocalDate.of(2023, 5, 1), LocalDate.of(2040, 12, 31)), 21),
                PrlEnumProperty("event", EnumRange.list("E4"), 22)
            )
            val featureDefinitions2 = PrlIntFeatureDefinition("feature1", IntRange.interval(101, 999), "", Visibility.PUBLIC, properties2, 20)
            val properties3 = listOf(
                PrlDateProperty("validity", DateRange.interval(LocalDate.of(2020, 1, 1), LocalDate.of(2023, 3, 1)), 31),
                PrlEnumProperty("event", EnumRange.list("E1"), 32)
            )
            val featureDefinitions3 = PrlIntFeatureDefinition("feature1", IntRange.interval(101, 200), "", Visibility.PUBLIC, properties3, 30)
            val properties4 = listOf(
                PrlDateProperty("validity", DateRange.interval(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 4, 1)), 41),
                PrlEnumProperty("event", EnumRange.list("E2", "E4"), 42)
            )
            val featureDefinitions4 = PrlIntFeatureDefinition("feature1", IntRange.interval(201, 400), "", Visibility.PUBLIC, properties4, 40)

            state.context = CompilerContext(module.fullName, featureDefinitions1.code, null, featureDefinitions1.lineNumber)
            featureStore.addDefinition(module, featureDefinitions1, state, propertyStore.slicingPropertyDefinitions)
            state.context = CompilerContext(module.fullName, featureDefinitions2.code, null, featureDefinitions2.lineNumber)
            featureStore.addDefinition(module, featureDefinitions2, state, propertyStore.slicingPropertyDefinitions)
            state.context = CompilerContext(module.fullName, featureDefinitions3.code, null, featureDefinitions3.lineNumber)
            featureStore.addDefinition(module, featureDefinitions3, state, propertyStore.slicingPropertyDefinitions)
            state.context = CompilerContext(module.fullName, featureDefinitions4.code, null, featureDefinitions4.lineNumber)
            featureStore.addDefinition(module, featureDefinitions4, state, propertyStore.slicingPropertyDefinitions)

            assertThat(featureStore.intFeatures.size).isEqualTo(1)
            assertThat(featureStore.intFeatures["feature1"]!!.size).isEqualTo(3)
            assertThat(deserialize(serialize(featureStore, mapOf()), mapOf())).isEqualTo(featureStore)
            assertThat(state.warnings).isEmpty()
            assertThat(state.errors).containsExactlyInAnyOrder(
                "[module=com.test, feature=feature1, lineNumber=30] Duplicate feature definition"
            )
        }

        // module com.booleworks {
        //  public int feature f1 [0 - 2000] {
        //      validity [2023-05-01 - 2023-12-01]
        //      event "E4"
        //  }
        // }
        // module com.booleworks.second {
        //	int feature f1 [1 - 100] {
        //		validity [2023-01-01 - 2023-04-01]
        //		event ["E1" - "E3"]
        //	}
        //	int feature f1 [101 - 999] {
        //		validity [2024-01-01 - 2040-12-01]
        //		event "E4"
        //	}
        // }
        @Test
        fun testAddMultipleFeaturesWith_SameNameAndType_DifferentModuleAndSlicingProperties() {
            val state = CompilerState()
            val module = Module("com.test")
            val moduleSecond = Module("com.test.second")
            val featureStore = FeatureStore()
            val propertyStore = PropertyStore()
            propertyStore.addSlicingPropertyDefinition(PrlSlicingDatePropertyDefinition("validity", 5), state)
            propertyStore.addSlicingPropertyDefinition(PrlSlicingEnumPropertyDefinition("event"), state)
            val properties1 = listOf(
                PrlDateProperty("validity", DateRange.interval(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 4, 1)), 11),
                PrlEnumProperty("event", EnumRange.list("E1", "E3"), 12)
            )
            val featureDefinitions1 = PrlIntFeatureDefinition("feature1", IntRange.interval(1, 100), "", Visibility.PUBLIC, properties1, 10)
            val properties2 = listOf(
                PrlDateProperty("validity", DateRange.interval(LocalDate.of(2024, 1, 1), LocalDate.of(2040, 1, 1)), 21),
                PrlEnumProperty("event", EnumRange.list("E4"), 22)
            )
            val featureDefinitions2 = PrlIntFeatureDefinition("feature1", IntRange.interval(101, 999), "", Visibility.PUBLIC, properties2, 20)
            val propertiesSecond = listOf(
                PrlDateProperty("validity", DateRange.interval(LocalDate.of(2023, 5, 1), LocalDate.of(2023, 12, 1)), 31),
                PrlEnumProperty("event", EnumRange.list("E4"), 32)
            )
            val featureDefinitionsSecond = PrlIntFeatureDefinition("feature1", IntRange.interval(101, 200), "", Visibility.PUBLIC, propertiesSecond, 30)

            state.context = CompilerContext(module.fullName, featureDefinitions1.code, null, featureDefinitions1.lineNumber)
            featureStore.addDefinition(module, featureDefinitions1, state, propertyStore.slicingPropertyDefinitions)
            state.context = CompilerContext(module.fullName, featureDefinitions2.code, null, featureDefinitions2.lineNumber)
            featureStore.addDefinition(module, featureDefinitions2, state, propertyStore.slicingPropertyDefinitions)
            state.context = CompilerContext(moduleSecond.fullName, featureDefinitionsSecond.code, null, featureDefinitionsSecond.lineNumber)
            featureStore.addDefinition(moduleSecond, featureDefinitionsSecond, state, propertyStore.slicingPropertyDefinitions)

            assertThat(featureStore.intFeatures.size).isEqualTo(1)
            assertThat(featureStore.intFeatures["feature1"]!!.size).isEqualTo(3)
            // TODO Fix Serialization
//            assertThat(deserialize(serialize(featureStore, mapOf()), mapOf())).isEqualTo(featureStore)
            assertThat(state.warnings).contains("[module=com.test.second, feature=feature1, lineNumber=30] Feature also defined in module: com.test")
            assertThat(state.errors).isEmpty()
        }

        // TODO Check also different visibilities and with sclicing features
        @Test
        fun testNotUniqueFeatures() {
            val state = CompilerState()
            val module = Module("com.test1")
            val moduleSecond = Module("com.test2")
            val featureStore = FeatureStore()
            val propertyStore = PropertyStore()
            val featureDefinitions1 = PrlBooleanFeatureDefinition("feature1", true, "", Visibility.PRIVATE, listOf(), 10)
            val featureDefinitions2 = PrlBooleanFeatureDefinition("feature1", false, "", Visibility.PRIVATE, listOf(), 20)

            state.context = CompilerContext(module.fullName, featureDefinitions1.code, null, featureDefinitions1.lineNumber)
            featureStore.addDefinition(module, featureDefinitions1, state, propertyStore.slicingPropertyDefinitions)
            state.context = CompilerContext(moduleSecond.fullName, featureDefinitions2.code, null, featureDefinitions2.lineNumber)
            featureStore.addDefinition(moduleSecond, featureDefinitions2, state, propertyStore.slicingPropertyDefinitions)

            assertThat(featureStore.booleanFeatures.size).isEqualTo(1)
            assertThat(featureStore.booleanFeatures["feature1"]!!.size).isEqualTo(2)
            assertThat(featureStore.nonUniqueFeatures()).containsExactly("feature1")
            // TODO Fix Serialization
//            assertThat(deserialize(serialize(featureStore, mapOf()), mapOf())).isEqualTo(featureStore)
            assertThat(state.warnings).isEmpty()
            assertThat(state.errors).isEmpty()
        }

    }
}
