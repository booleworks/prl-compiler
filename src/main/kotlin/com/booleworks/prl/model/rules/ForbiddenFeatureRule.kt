// SPDX-License-Identifier: MIT
// Copyright 2023 BooleWorks GmbH

package com.booleworks.prl.model.rules

import com.booleworks.prl.model.AnyProperty
import com.booleworks.prl.model.Module
import com.booleworks.prl.model.constraints.BooleanFeature
import com.booleworks.prl.model.constraints.EnumFeature
import com.booleworks.prl.model.constraints.Feature
import com.booleworks.prl.model.constraints.IntFeature
import com.booleworks.prl.model.constraints.enumNe
import com.booleworks.prl.model.constraints.enumVal
import com.booleworks.prl.model.constraints.intNe
import com.booleworks.prl.model.constraints.not
import com.booleworks.prl.model.constraints.versionNe
import com.booleworks.prl.model.datastructures.FeatureRenaming
import java.util.Objects

class ForbiddenFeatureRule internal constructor(
    override val feature: Feature,
    override val enumValue: String?,
    override val intValueOrVersion: Int?,
    override val module: Module,
    override val id: String,
    override val description: String,
    override val properties: Map<String, AnyProperty> = mapOf(),
    override val lineNumber: Int? = null
) : FeatureRule<ForbiddenFeatureRule>(
    feature,
    enumValue,
    intValueOrVersion,
    module,
    id,
    description,
    properties,
    lineNumber
) {

    private constructor(rule: AnyRule, feature: Feature, enumValue: String?, intValueOrVersion: Int?) : this(
        feature,
        enumValue,
        intValueOrVersion,
        rule.module,
        rule.id,
        rule.description,
        rule.properties,
        rule.lineNumber
    )

    constructor(
        feature: BooleanFeature,
        module: Module,
        id: String = "",
        description: String = "",
        properties: Map<String, AnyProperty> = mapOf(),
        lineNumber: Int? = null
    ) : this(feature, null, null, module, id, description, properties, lineNumber)

    constructor(
        feature: BooleanFeature,
        version: Int,
        module: Module,
        id: String = "",
        description: String = "",
        properties: Map<String, AnyProperty> = mapOf(),
        lineNumber: Int? = null
    ) : this(feature, null, version, module, id, description, properties, lineNumber)

    constructor(
        feature: IntFeature,
        value: Int,
        module: Module,
        id: String = "",
        description: String = "",
        properties: Map<String, AnyProperty> = mapOf(),
        lineNumber: Int? = null
    ) : this(feature, null, value, module, id, description, properties, lineNumber)

    constructor(
        feature: EnumFeature,
        value: String,
        module: Module,
        id: String = "",
        description: String = "",
        properties: Map<String, AnyProperty> = mapOf(),
        lineNumber: Int? = null
    ) : this(feature, value, null, module, id, description, properties, lineNumber)

    override fun rename(renaming: FeatureRenaming) =
        renameFeature(renaming).let {
            if (it != feature) ForbiddenFeatureRule(
                this,
                it,
                enumValue,
                intValueOrVersion
            ) else this
        }

    override fun stripProperties() =
        ForbiddenFeatureRule(feature, enumValue, intValueOrVersion, module, id, description, mapOf(), lineNumber)

    override fun stripMetaInfo() =
        ForbiddenFeatureRule(feature, enumValue, intValueOrVersion, module, "", "", properties, lineNumber)

    override fun stripAll() =
        ForbiddenFeatureRule(feature, enumValue, intValueOrVersion, module, "", "", mapOf(), lineNumber)

    override fun hashCode() = Objects.hash(super.hashCode(), feature, enumValue, intValueOrVersion)
    override fun equals(other: Any?) = super.equals(other) && hasEqualConstraint(other as AnyRule)
    private fun hasEqualConstraint(other: AnyRule) =
        other is ForbiddenFeatureRule && feature == other.feature && enumValue == other.enumValue &&
                intValueOrVersion == other.intValueOrVersion

    override fun generateConstraint(feature: Feature, enumValue: String?, intValueOrVersion: Int?) = when (feature) {
        is BooleanFeature -> if (!feature.versioned) not(feature) else versionNe(feature, intValueOrVersion!!)
        is EnumFeature -> enumNe(feature, enumVal(enumValue!!))
        is IntFeature -> intNe(feature, intValueOrVersion!!)
    }
}
