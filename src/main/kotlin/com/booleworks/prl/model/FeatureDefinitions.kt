// SPDX-License-Identifier: MIT
// Copyright 2023 BooleWorks GmbH

package com.booleworks.prl.model

import com.booleworks.prl.model.constraints.BooleanFeature
import com.booleworks.prl.model.constraints.EnumFeature
import com.booleworks.prl.model.constraints.Feature
import com.booleworks.prl.model.constraints.IntFeature
import com.booleworks.prl.model.constraints.boolFt
import com.booleworks.prl.model.constraints.enumFt
import com.booleworks.prl.model.constraints.intFt
import com.booleworks.prl.model.constraints.versionFt
import com.booleworks.prl.model.datastructures.FeatureRenaming
import com.booleworks.prl.model.slices.AnySliceSelection
import com.booleworks.prl.model.slices.evaluateProperties
import com.booleworks.prl.parser.PragmaticRuleLanguage.INDENT
import com.booleworks.prl.parser.PragmaticRuleLanguage.KEYWORD_DESCRIPTION
import com.booleworks.prl.parser.PragmaticRuleLanguage.KEYWORD_ENUM
import com.booleworks.prl.parser.PragmaticRuleLanguage.KEYWORD_FEATURE
import com.booleworks.prl.parser.PragmaticRuleLanguage.KEYWORD_INT
import com.booleworks.prl.parser.PragmaticRuleLanguage.KEYWORD_VERSIONED
import com.booleworks.prl.parser.PragmaticRuleLanguage.SYMBOL_LBRA
import com.booleworks.prl.parser.PragmaticRuleLanguage.SYMBOL_RBRA
import com.booleworks.prl.parser.PragmaticRuleLanguage.identifier
import com.booleworks.prl.parser.PragmaticRuleLanguage.quote
import com.booleworks.prl.parser.PragmaticRuleLanguage.visibilityString
import com.booleworks.prl.parser.PrlBooleanFeatureDefinition
import com.booleworks.prl.parser.PrlEnumFeatureDefinition
import com.booleworks.prl.parser.PrlFeatureDefinition
import com.booleworks.prl.parser.PrlIntFeatureDefinition
import java.util.Objects

/**
 * Feature visibility:
 *
 * PRIVATE: only visible in the module in which it was declared
 * INTERNAL: visible in its own modules and all modules which inherit from it
 * PUBLIC: visible everywhere
 */
enum class Visibility {
    PRIVATE, INTERNAL, PUBLIC
}

typealias AnyFeatureDef = FeatureDefinition<*, *>

/**
 * Super class for features of different types.  A feature always has a code
 * (name) which has to be unique within the module.  By default, a feature
 * has PRIVATE visibility.  Additionally, it can have the following optional
 * parameters:
 *
 *  description: A textual description of the feature
 *  enforces: a list of other features which are directly enforced by this
 *            feature
 *  versioned: a flag whether this feature is versioned or not
 *  visibility: the visibility of the feature
 *  properties: a list of additional properties of the feature
 */
sealed class FeatureDefinition<F : FeatureDefinition<F, *>, T : Feature>(
    open val module: Module,
    open val code: String,
    open val visibility: Visibility,
    open val description: String,
    open val properties: Map<String, AnyProperty>,
    open val lineNumber: Int? = null,
    var used: Boolean = false
) {
    abstract fun rename(renaming: FeatureRenaming): F
    abstract fun stripProperties(): F
    abstract fun stripMetaInfo(): F
    abstract fun stripAll(): F
    abstract val headerLine: String
    abstract val feature: T
    val reference by lazy { FeatureReference(module, code) }

    fun appendString(app: Appendable, depth: Int): Appendable {
        val i = INDENT.repeat(depth)
        val ii = i + INDENT
        if (description.isBlank() && properties.isEmpty()) {
            app.append(i).append(headerLine)
        } else {
            app.append(i).append(headerLine).append(" ").append(SYMBOL_LBRA).append(System.lineSeparator())
            if (description.isNotBlank()) app.append(ii).append(KEYWORD_DESCRIPTION).append(" ")
                .append(quote(description)).append(System.lineSeparator())
            properties.forEach { app.append(ii).append(it.value.toString()).append(System.lineSeparator()) }
            app.append(i).append(SYMBOL_RBRA)
        }
        return app
    }

    fun filter(sliceSelections: List<AnySliceSelection>) = evaluateProperties(properties, sliceSelections)

    override fun toString() = StringBuilder().apply { appendString(this, 0) }.toString()

    override fun hashCode() = Objects.hash(module, code, visibility, description, properties, headerLine, feature)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AnyFeatureDef
        if (module != other.module) return false
        if (code != other.code) return false
        if (visibility != other.visibility) return false
        if (description != other.description) return false
        if (properties != other.properties) return false
        if (headerLine != other.headerLine) return false
        return feature == other.feature
    }

    companion object {
        internal fun fromPrlModule(module: Module, definition: PrlFeatureDefinition): AnyFeatureDef =
            when (definition) {
                is PrlBooleanFeatureDefinition -> BooleanFeatureDefinition(module, definition)
                is PrlEnumFeatureDefinition -> EnumFeatureDefinition(module, definition)
                is PrlIntFeatureDefinition -> IntFeatureDefinition(module, definition)
            }
    }
}

/**
 * A boolean feature.
 *
 * A boolean feature can have two values - false or true - and is the main building block of
 * many rule sets in propositional logic.
 */
class BooleanFeatureDefinition(
    override val module: Module,
    override val code: String,
    val versioned: Boolean = false,
    override val visibility: Visibility = Visibility.PUBLIC,
    override val description: String = "",
    override val properties: Map<String, AnyProperty> = mapOf(),
    override val lineNumber: Int? = null
) : FeatureDefinition<BooleanFeatureDefinition, BooleanFeature>(
    module,
    code,
    visibility,
    description,
    properties,
    lineNumber
) {

    constructor(module: Module, d: PrlBooleanFeatureDefinition) : this(
        module,
        d.code,
        d.versioned,
        d.visibility,
        d.description,
        compileProperties(d.properties).associateBy { it.name },
        d.lineNumber
    )

    override fun stripProperties() =
        BooleanFeatureDefinition(module, code, versioned, visibility, description, mapOf(), lineNumber)

    override fun stripMetaInfo() =
        BooleanFeatureDefinition(module, code, versioned, visibility, "", properties, lineNumber)

    override fun stripAll() = BooleanFeatureDefinition(module, code, versioned, visibility, "", mapOf(), lineNumber)
    override fun rename(renaming: FeatureRenaming): BooleanFeatureDefinition {
        val renamedCode = renaming.rename(feature).featureCode
        return BooleanFeatureDefinition(module, renamedCode, versioned, visibility, description, properties, lineNumber)
    }

    override val headerLine =
        visibilityString(visibility) + (if (versioned) "$KEYWORD_VERSIONED " else "") + "$KEYWORD_FEATURE " +
            identifier(code)

    override val feature = if (versioned) versionFt(code, module) else boolFt(code, module)

    companion object {
        fun merge(definitions: Collection<BooleanFeatureDefinition>): BooleanFeatureDefinition {
            assert(!definitions.isEmpty())
            val module = definitions.first().module
            val code = definitions.first().code
            val version = definitions.first().versioned
            val visibility = definitions.first().visibility
            return BooleanFeatureDefinition(module, code, version, visibility)
        }
    }
}

/**
 * An enum feature definition.
 *
 * An enum feature must be defined with a range of possible values. For any buildable feature combination, the
 * feature is assigned to exactly one value of these values. In constraints, enum features can be compared with
 * fixed enum values (like x = "abc") or with a range (like x in ["a", "b", "c"]).
 */
class EnumFeatureDefinition(
    override val module: Module,
    override val code: String,
    val values: Set<String>,
    override val visibility: Visibility = Visibility.PUBLIC,
    override val description: String = "",
    override val properties: Map<String, AnyProperty> = mapOf(),
    override val lineNumber: Int? = null
) : FeatureDefinition<EnumFeatureDefinition, EnumFeature>(
    module,
    code,
    visibility,
    description,
    properties,
    lineNumber
) {

    constructor(module: Module, d: PrlEnumFeatureDefinition) : this(
        module,
        d.code,
        d.values.toSet(),
        d.visibility,
        d.description,
        compileProperties(d.properties).associateBy { it.name },
        d.lineNumber
    )

    override fun stripProperties() =
        EnumFeatureDefinition(module, code, values, visibility, description, mapOf(), lineNumber)

    override fun stripMetaInfo() = EnumFeatureDefinition(module, code, values, visibility, "", properties, lineNumber)
    override fun stripAll() = EnumFeatureDefinition(module, code, values, visibility, "", mapOf(), lineNumber)
    override fun rename(renaming: FeatureRenaming): EnumFeatureDefinition {
        val renamedCode = renaming.rename(feature).featureCode
        val renamedValues = values.map { renaming.rename(feature, it) }.toSet()
        return EnumFeatureDefinition(
            module,
            renamedCode,
            renamedValues,
            visibility,
            description,
            properties,
            lineNumber
        )
    }

    override val headerLine =
        visibilityString(visibility) + "$KEYWORD_ENUM $KEYWORD_FEATURE " + identifier(code) + " " +
            (if (values.isEmpty()) "e" else "") + quote(values)
    override val feature = enumFt(code, module, values)

    companion object {
        fun merge(definitions: Collection<EnumFeatureDefinition>): EnumFeatureDefinition {
            assert(!definitions.isEmpty())
            val module = definitions.first().module
            val code = definitions.first().code
            val visibility = definitions.first().visibility
            val values = definitions.map { it.values }.reduce { acc, values -> acc.intersect(values) }
            return EnumFeatureDefinition(module, code, values, visibility)
        }
    }
}


/**
 * An integer feature.
 *
 * An integer feature must be defined with a range of possible values (c.f. [IntRange]).
 * For any buildable feature combination, the feature is assigned to exactly one value of this range.
 * In constraints, integer features can be compared with each other, with fixed values (like x &lt;= 7)
 * or with a range (like x in [1-7]).
 */
class IntFeatureDefinition(
    override val module: Module,
    override val code: String,
    val domain: PropertyRange<Int>,
    override val visibility: Visibility = Visibility.PUBLIC,
    override val description: String = "",
    override val properties: Map<String, AnyProperty> = mapOf(),
    override val lineNumber: Int? = null
) : FeatureDefinition<IntFeatureDefinition, IntFeature>(module, code, visibility, description, properties, lineNumber) {

    constructor(module: Module, d: PrlIntFeatureDefinition) : this(
        module,
        d.code,
        d.domain,
        d.visibility,
        d.description,
        compileProperties(d.properties).associateBy { it.name },
        d.lineNumber
    )

    override fun stripProperties() =
        IntFeatureDefinition(module, code, domain, visibility, description, mapOf(), lineNumber)

    override fun stripMetaInfo() = IntFeatureDefinition(module, code, domain, visibility, "", properties, lineNumber)
    override fun stripAll() = IntFeatureDefinition(module, code, domain, visibility, "", mapOf(), lineNumber)
    override fun rename(renaming: FeatureRenaming) =
        IntFeatureDefinition(
            module,
            renaming.rename(feature).featureCode,
            domain,
            visibility,
            description,
            properties,
            lineNumber
        )

    override val headerLine =
        visibilityString(visibility) + "$KEYWORD_INT $KEYWORD_FEATURE " + identifier(code) + " " + domain
    override val feature = intFt(code, module, domain)

    companion object {
        fun merge(definitions: Collection<IntFeatureDefinition>): IntFeatureDefinition {
            assert(!definitions.isEmpty())
            val module = definitions.first().module
            val code = definitions.first().code
            val visibility = definitions.first().visibility
            val domain = definitions.map { it.domain }.reduce { acc, domain -> acc.intersection(domain) }
            return IntFeatureDefinition(module, code, domain, visibility)
        }
    }
}

data class FeatureReference(val module: Module, val featureCode: String) {
    val fullName by lazy { Feature.fullNameOf(featureCode, module.fullName) }
}
