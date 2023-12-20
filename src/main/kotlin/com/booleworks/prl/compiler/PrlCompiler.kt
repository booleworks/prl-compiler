// SPDX-License-Identifier: MIT
// Copyright 2023 BooleWorks GmbH

package com.booleworks.prl.compiler

import com.booleworks.prl.model.AnyFeatureDef
import com.booleworks.prl.model.BooleanFeatureDefinition
import com.booleworks.prl.model.EnumFeatureDefinition
import com.booleworks.prl.model.IntFeatureDefinition
import com.booleworks.prl.model.Module
import com.booleworks.prl.model.ModuleHierarchy
import com.booleworks.prl.model.PrlModel
import com.booleworks.prl.model.PrlModelHeader
import com.booleworks.prl.model.compileProperties
import com.booleworks.prl.model.compilePropertiesToMap
import com.booleworks.prl.model.rules.AnyRule
import com.booleworks.prl.model.rules.ConstraintRule
import com.booleworks.prl.model.rules.DefinitionRule
import com.booleworks.prl.model.rules.ExclusionRule
import com.booleworks.prl.model.rules.ForbiddenFeatureRule
import com.booleworks.prl.model.rules.GroupRule
import com.booleworks.prl.model.rules.IfThenElseRule
import com.booleworks.prl.model.rules.InclusionRule
import com.booleworks.prl.model.rules.MandatoryFeatureRule
import com.booleworks.prl.parser.PrlBooleanFeatureDefinition
import com.booleworks.prl.parser.PrlConstraintRule
import com.booleworks.prl.parser.PrlDefinitionRule
import com.booleworks.prl.parser.PrlExclusionRule
import com.booleworks.prl.parser.PrlFeature
import com.booleworks.prl.parser.PrlFeatureDefinition
import com.booleworks.prl.parser.PrlFeatureRule
import com.booleworks.prl.parser.PrlForbiddenFeatureRule
import com.booleworks.prl.parser.PrlGroupRule
import com.booleworks.prl.parser.PrlIfThenElseRule
import com.booleworks.prl.parser.PrlInclusionRule
import com.booleworks.prl.parser.PrlMandatoryFeatureRule
import com.booleworks.prl.parser.PrlRule
import com.booleworks.prl.parser.PrlRuleFile
import com.booleworks.prl.parser.PrlRuleSet
import com.booleworks.prl.parser.PrlVersion
import java.util.SortedSet
import java.util.TreeMap
import java.util.TreeSet
import java.util.UUID.randomUUID

class PrlCompiler {
    private val state = CompilerState()
    private val cc = ConstraintCompiler()

    fun compile(ruleFile: PrlRuleFile): PrlModel {
        val version = PrlVersion(ruleFile.header.major, ruleFile.header.minor)
        val header = PrlModelHeader(version, compilePropertiesToMap(ruleFile.header.properties))
        val t1 = System.currentTimeMillis()
        val moduleHierarchy = compileModuleHierarchy(ruleFile)
        val t2 = System.currentTimeMillis()
        val propertyStore = compileSlicingPropertyDefinitionsIntoStore(ruleFile)
        val t3 = System.currentTimeMillis()
        val featureStore = compileFeaturesIntoStore(ruleFile, propertyStore, moduleHierarchy)
        val t4 = System.currentTimeMillis()
        val rules = compileRules(ruleFile, moduleHierarchy, propertyStore, featureStore)
        val t5 = System.currentTimeMillis()
        val numDefs = featureStore.size()
        state.addInfo(
            "Compiled ${moduleHierarchy.numberOfModules()} " +
                    "module${if (moduleHierarchy.numberOfModules() != 1) "s" else ""} in ${t2 - t1} ms."
        )
        state.addInfo(
            "Compiled ${propertyStore.slicingPropertyDefinitions.size} slicing property definition " +
                    "${if (propertyStore.slicingPropertyDefinitions.size != 1) "s" else ""} in ${t3 - t2} ms."
        )
        state.addInfo("Compiled $numDefs feature definition${if (numDefs != 1) "s" else ""} in ${t4 - t3} ms.")
        state.addInfo("Compiled ${rules.size} rule${if (rules.size != 1) "s" else ""} in ${t5 - t4} ms.")

        // TODO limitations in the current developer preview
        if (moduleHierarchy.numberOfModules() > 1) {
            state.addError(
                "Currently only PRL files with one module are supported. Multi-Module support will be " +
                        "added in future releases."
            )
        }
        if (featureStore.containsIntFeatures() || featureStore.containsVersionedBooleanFeatures()) {
            state.addError(
                "Currently integer and versioned Boolean features are not supported. Support will be added in future " +
                        "releases."
            )
        }

        return PrlModel(header, moduleHierarchy, featureStore, rules, propertyStore)
    }

    fun errors() = state.errors
    fun warnings() = state.warnings
    fun infos() = state.infos
    fun hasErrors() = state.hasErrors()

    /////////////////////////////////
    // Compile Modules and imports //
    /////////////////////////////////
    private fun compileModuleHierarchy(ruleFile: PrlRuleFile): ModuleHierarchy {
        compileImports(compileModules(ruleFile.ruleSets), ruleFile)
        state.context.clear()
        return compileModules(ruleFile.ruleSets)
    }

    internal fun compileModules(ruleSets: Collection<PrlRuleSet>): ModuleHierarchy {
        val sortedModuleNames = TreeSet<String>()
        ruleSets.filterNot { sortedModuleNames.add(it.module.fullName) }.map {
            state.context.module = it.module.fullName
            state.context.lineNumber = it.lineNumber
            state.addError("Duplicate module declaration")
        }
        val lineNumbers = ruleSets.associate { it.module.fullName to it.lineNumber }
        return ModuleHierarchy(compileModules(sortedModuleNames, lineNumbers))
    }

    private fun compileModules(
        sortedModuleNames: SortedSet<String>,
        lineNumbers: Map<String, Int?>
    ): Map<String, Module> {
        val modules = TreeMap<String, Module>()
        sortedModuleNames.forEach { moduleName ->
            val newModule = Module(moduleName, lineNumber = lineNumbers[moduleName])
            modules[moduleName] = newModule
            modules.values.findLast { newModule.isDescendantOf(it) }?.also {
                newModule.ancestor = it
                it.descendants += newModule
            }
        }
        return modules
    }

    private fun compileImports(moduleHierarchy: ModuleHierarchy, ruleFile: PrlRuleFile) {
        ruleFile.ruleSets.flatMap {
            val module = moduleHierarchy.moduleForName(it.module.fullName)!!
            state.context.module = module.fullName
            it.imports.map { importedModule ->
                state.context.lineNumber = importedModule.lineNumber
                val imported = moduleHierarchy.moduleForName(importedModule.module.fullName)
                if (imported != null) {
                    module.imports += imported
                    null
                } else {
                    state.addError("Unknown imported module ${importedModule.module.fullName}")
                }
            }
        }
    }

    ////////////////////////////////
    // Compile Slicing Properties //
    ////////////////////////////////
    private fun compileSlicingPropertyDefinitionsIntoStore(ruleFile: PrlRuleFile): PropertyStore =
        PropertyStore().apply {
            state.context.module = ""
            ruleFile.slicingPropertyDefinitions.forEach { addSlicingPropertyDefinition(it, state) }
            state.context.clear()
        }

    //////////////////////
    // Compile Features //
    //////////////////////
    private fun compileFeaturesIntoStore(
        ruleFile: PrlRuleFile,
        propertyStore: PropertyStore,
        moduleHierarchy: ModuleHierarchy
    ) =
        FeatureStore().apply {
            ruleFile.ruleSets.forEach {
                compileFeaturesIntoStore(
                    moduleHierarchy.moduleForName(it.module.fullName)!!,
                    it.featureDefinitions,
                    it.rules,
                    propertyStore,
                    this
                )
            }
            state.context.clear()
        }

    private fun compileFeaturesIntoStore(
        module: Module,
        features: List<PrlFeatureDefinition>,
        rules: List<PrlRule>,
        propertyStore: PropertyStore,
        featureStore: FeatureStore
    ) {
        state.context.module = module.fullName
        features.forEach {
            state.context.feature = it.code
            state.context.lineNumber = it.lineNumber
            if (hasInvalidFeatureName(it.code)) {
                state.addError("Feature name invalid: ${it.code}")
                return@forEach
            }
            propertyStore.addProperties(it, state)
            if (!state.hasErrors()) {
                featureStore.addDefinition(module, it, state, propertyStore.slicingPropertyDefinitions, false)
            }
        }
        rules.filterIsInstance<PrlGroupRule>().forEach {
            state.context.feature = it.group.featureCode
            state.context.lineNumber = it.lineNumber
            if (hasInvalidFeatureName(it.group.featureCode)) {
                state.addError("Rule name invalid: ${it.group.featureCode}")
                return@forEach
            }
            propertyStore.checkPropertiesCorrect(it.properties, compileProperties(it.propsMap()), state)
            if (!state.hasErrors()) {
                featureStore.addDefinition(
                    module,
                    createFeatureDefinitionForGroup(it),
                    state,
                    propertyStore.slicingPropertyDefinitions,
                    true
                )
            }
        }
    }

    private fun hasInvalidFeatureName(featureName: String) = featureName.contains(".")


    private fun createFeatureDefinitionForGroup(rule: PrlGroupRule) =
        PrlBooleanFeatureDefinition(
            rule.group.featureCode,
            false,
            rule.description,
            rule.visibility,
            rule.properties,
            rule.lineNumber
        )

    ///////////////////
    // Compile Rules //
    ///////////////////
    private fun compileRules(
        ruleFile: PrlRuleFile,
        moduleHierarchy: ModuleHierarchy,
        propertyStore: PropertyStore,
        featureStore: FeatureStore
    ) =
        if (!state.hasErrors()) {
            mutableListOf<AnyRule>().apply {
                ruleFile.ruleSets.forEach {
                    this.addAll(
                        compileRules(
                            it,
                            moduleHierarchy,
                            propertyStore,
                            featureStore
                        )
                    )
                }
                state.context.clear()
            }
        } else {
            emptyList()
        }


    private fun compileRules(
        ruleSet: PrlRuleSet,
        mh: ModuleHierarchy,
        propertyStore: PropertyStore,
        featureStore: FeatureStore
    ): List<AnyRule> {
        val module = mh.moduleForName(ruleSet.module.fullName)!!
        state.context.module = module.fullName
        val featureMap = featureStore.generateDefinitionMap(ruleSet.features(), module, mh, state)
        if (state.hasErrors()) return listOf()
        return ruleSet.rules.mapNotNull {
            state.context.ruleId = it.id.ifEmpty { randomUUID().toString() }
            propertyStore.addProperties(it, state)
            if (state.hasErrors()) {
                null
            } else {
                state.context.lineNumber = it.lineNumber
                compileRule(it, module, featureMap)
            }
        }
    }

    internal fun compileRule(prlRule: PrlRule, module: Module, map: Map<PrlFeature, AnyFeatureDef>): AnyRule? {
        return try {
            when (prlRule) {
                is PrlConstraintRule -> rule(prlRule, module, map)
                is PrlDefinitionRule -> rule(prlRule, module, map)
                is PrlExclusionRule -> rule(prlRule, module, map)
                is PrlForbiddenFeatureRule -> rule(prlRule, module, map)
                is PrlGroupRule -> rule(prlRule, module, map)
                is PrlIfThenElseRule -> rule(prlRule, module, map)
                is PrlInclusionRule -> rule(prlRule, module, map)
                is PrlMandatoryFeatureRule -> rule(prlRule, module, map)
                is PrlFeatureRule -> rule(prlRule, module, map)
            }
        } catch (e: com.booleworks.prl.compiler.CoCoException) {
            state.addError(e.message!!)
            null
        }
    }

    private fun rule(prl: PrlConstraintRule, module: Module, map: Map<PrlFeature, AnyFeatureDef>) =
        ConstraintRule(
            cc.compileConstraint(prl.constraint, map),
            module,
            prl.id,
            prl.description,
            compileProperties(prl.propsMap()),
            prl.lineNumber
        )

    private fun rule(prl: PrlInclusionRule, module: Module, map: Map<PrlFeature, AnyFeatureDef>) = InclusionRule(
        cc.compileConstraint(prl.ifPart, map),
        cc.compileConstraint(prl.thenPart, map),
        module,
        prl.id,
        prl.description,
        compileProperties(prl.propsMap()),
        prl.lineNumber
    )

    private fun rule(prl: PrlExclusionRule, module: Module, map: Map<PrlFeature, AnyFeatureDef>) = ExclusionRule(
        cc.compileConstraint(prl.ifPart, map),
        cc.compileConstraint(prl.thenNotPart, map),
        module,
        prl.id,
        prl.description,
        compileProperties(prl.propsMap()),
        prl.lineNumber
    )

    private fun rule(prl: PrlDefinitionRule, module: Module, map: Map<PrlFeature, AnyFeatureDef>) = DefinitionRule(
        cc.compileUnversionedBooleanFeature(prl.feature, map),
        cc.compileConstraint(prl.definition, map),
        module,
        prl.id,
        prl.description,
        compileProperties(prl.propsMap()),
        prl.lineNumber
    )

    private fun rule(prl: PrlIfThenElseRule, module: Module, map: Map<PrlFeature, AnyFeatureDef>) = IfThenElseRule(
        cc.compileConstraint(prl.ifPart, map),
        cc.compileConstraint(prl.thenPart, map),
        cc.compileConstraint(prl.elsePart, map),
        module,
        prl.id,
        prl.description,
        compileProperties(prl.propsMap()),
        prl.lineNumber
    )

    private fun rule(prl: PrlGroupRule, module: Module, map: Map<PrlFeature, AnyFeatureDef>) = GroupRule(
        prl.type,
        cc.compileUnversionedBooleanFeature(prl.group, map),
        cc.compileBooleanFeatures(prl.content, map).toSet(),
        prl.visibility,
        module,
        prl.id,
        prl.description,
        compileProperties(prl.propsMap()),
        prl.lineNumber
    )

    private fun rule(prl: PrlFeatureRule, module: Module, map: Map<PrlFeature, AnyFeatureDef>) =
        if (prl is PrlForbiddenFeatureRule) {
            ForbiddenFeatureRule(
                validateFeature(prl, map),
                prl.enumValue,
                prl.intValueOrVersion,
                module,
                prl.id,
                prl.description,
                compileProperties(prl.propsMap()),
                prl.lineNumber
            )
        } else {
            MandatoryFeatureRule(
                validateFeature(prl, map),
                prl.enumValue,
                prl.intValueOrVersion,
                module,
                prl.id,
                prl.description,
                compileProperties(prl.propsMap()),
                prl.lineNumber
            )
        }

    private fun validateFeature(prl: PrlFeatureRule, map: Map<PrlFeature, AnyFeatureDef>) =
        when (val def = map[prl.feature]) {
            is BooleanFeatureDefinition ->
                if (!def.versioned && (prl.enumValue != null || prl.intValueOrVersion != null)) {
                    invalidBoolean()
                } else if (def.versioned && prl.intValueOrVersion == null) {
                    invalidVersioned()
                } else {
                    def.feature
                }

            is EnumFeatureDefinition -> if (prl.enumValue == null) invalidEnum() else def.feature
            is IntFeatureDefinition -> if (prl.intValueOrVersion == null) invalidInt() else def.feature
            null -> cc.unknownFeature(prl.feature)
        }

    private fun invalidBoolean(): Nothing =
        throw CoCoException("Cannot assign an unversioned boolean feature to an int or enum value")

    private fun invalidVersioned(): Nothing =
        throw CoCoException("Cannot assign a versioned boolean feature to anything else than an int version")

    private fun invalidInt(): Nothing =
        throw CoCoException("Cannot assign an int feature to anything else than an int value")

    private fun invalidEnum(): Nothing =
        throw CoCoException("Cannot assign an enum feature to anything else than an enum value")
}

internal class CompilerState {
    internal val errors = mutableListOf<String>()
    internal val warnings = mutableListOf<String>()
    internal val infos = mutableListOf<String>()
    internal var context: CompilerContext = CompilerContext()

    internal fun hasErrors() = errors.isNotEmpty()
    internal fun addError(message: String) = errors.add("${contextString()}$message")
    internal fun addWarning(message: String) = warnings.add("${contextString()}$message")
    internal fun addInfo(message: String) = infos.add("${contextString()}$message")
    private fun contextString() = if (context.isEmpty()) "" else "$context "
}

internal class CompilerContext(
    var module: String? = null,
    var feature: String? = null,
    var ruleId: String? = null,
    var lineNumber: Int? = null
) {
    fun clear() {
        module = null
        feature = null
        ruleId = null
        lineNumber = null
    }

    fun isEmpty() = module == null && feature == null && ruleId == null && lineNumber == null

    override fun toString(): String {
        val moduleString = if (module == null) null else "module=$module"
        val featureString = if (feature == null) null else "feature=$feature"
        val ruleIdString = if (ruleId == null) null else "ruleId=$ruleId"
        val lineNumberString = if (lineNumber == null) null else "lineNumber=$lineNumber"
        return listOfNotNull(moduleString, featureString, ruleIdString, lineNumberString).joinToString(", ", "[", "]")
    }
}
