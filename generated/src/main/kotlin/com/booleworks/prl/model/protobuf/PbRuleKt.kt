// Generated by the protocol buffer compiler. DO NOT EDIT!
// source: model_rules.proto

// Generated files should ignore deprecation warnings
@file:Suppress("DEPRECATION")
package com.booleworks.prl.model.protobuf;

@kotlin.jvm.JvmName("-initializepbRule")
public inline fun pbRule(block: com.booleworks.prl.model.protobuf.PbRuleKt.Dsl.() -> kotlin.Unit): com.booleworks.prl.model.protobuf.ProtoBufRules.PbRule =
  com.booleworks.prl.model.protobuf.PbRuleKt.Dsl._create(com.booleworks.prl.model.protobuf.ProtoBufRules.PbRule.newBuilder()).apply { block() }._build()
/**
 * Protobuf type `boolerules.rules.PbRule`
 */
public object PbRuleKt {
  @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
  @com.google.protobuf.kotlin.ProtoDslMarker
  public class Dsl private constructor(
    private val _builder: com.booleworks.prl.model.protobuf.ProtoBufRules.PbRule.Builder
  ) {
    public companion object {
      @kotlin.jvm.JvmSynthetic
      @kotlin.PublishedApi
      internal fun _create(builder: com.booleworks.prl.model.protobuf.ProtoBufRules.PbRule.Builder): Dsl = Dsl(builder)
    }

    @kotlin.jvm.JvmSynthetic
    @kotlin.PublishedApi
    internal fun _build(): com.booleworks.prl.model.protobuf.ProtoBufRules.PbRule = _builder.build()

    /**
     * `optional string id = 2;`
     */
    public var id: kotlin.String
      @JvmName("getId")
      get() = _builder.getId()
      @JvmName("setId")
      set(value) {
        _builder.setId(value)
      }
    /**
     * `optional string id = 2;`
     */
    public fun clearId() {
      _builder.clearId()
    }
    /**
     * `optional string id = 2;`
     * @return Whether the id field is set.
     */
    public fun hasId(): kotlin.Boolean {
      return _builder.hasId()
    }

    /**
     * `optional string description = 3;`
     */
    public var description: kotlin.String
      @JvmName("getDescription")
      get() = _builder.getDescription()
      @JvmName("setDescription")
      set(value) {
        _builder.setDescription(value)
      }
    /**
     * `optional string description = 3;`
     */
    public fun clearDescription() {
      _builder.clearDescription()
    }
    /**
     * `optional string description = 3;`
     * @return Whether the description field is set.
     */
    public fun hasDescription(): kotlin.Boolean {
      return _builder.hasDescription()
    }

    /**
     * An uninstantiable, behaviorless type to represent the field in
     * generics.
     */
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    public class PropertiesProxy private constructor() : com.google.protobuf.kotlin.DslProxy()
    /**
     * `repeated .boolerules.properties.PbProperty properties = 4;`
     */
     public val properties: com.google.protobuf.kotlin.DslList<com.booleworks.prl.model.protobuf.ProtoBufProperties.PbProperty, PropertiesProxy>
      @kotlin.jvm.JvmSynthetic
      get() = com.google.protobuf.kotlin.DslList(
        _builder.getPropertiesList()
      )
    /**
     * `repeated .boolerules.properties.PbProperty properties = 4;`
     * @param value The properties to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addProperties")
    public fun com.google.protobuf.kotlin.DslList<com.booleworks.prl.model.protobuf.ProtoBufProperties.PbProperty, PropertiesProxy>.add(value: com.booleworks.prl.model.protobuf.ProtoBufProperties.PbProperty) {
      _builder.addProperties(value)
    }
    /**
     * `repeated .boolerules.properties.PbProperty properties = 4;`
     * @param value The properties to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignProperties")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<com.booleworks.prl.model.protobuf.ProtoBufProperties.PbProperty, PropertiesProxy>.plusAssign(value: com.booleworks.prl.model.protobuf.ProtoBufProperties.PbProperty) {
      add(value)
    }
    /**
     * `repeated .boolerules.properties.PbProperty properties = 4;`
     * @param values The properties to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addAllProperties")
    public fun com.google.protobuf.kotlin.DslList<com.booleworks.prl.model.protobuf.ProtoBufProperties.PbProperty, PropertiesProxy>.addAll(values: kotlin.collections.Iterable<com.booleworks.prl.model.protobuf.ProtoBufProperties.PbProperty>) {
      _builder.addAllProperties(values)
    }
    /**
     * `repeated .boolerules.properties.PbProperty properties = 4;`
     * @param values The properties to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignAllProperties")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<com.booleworks.prl.model.protobuf.ProtoBufProperties.PbProperty, PropertiesProxy>.plusAssign(values: kotlin.collections.Iterable<com.booleworks.prl.model.protobuf.ProtoBufProperties.PbProperty>) {
      addAll(values)
    }
    /**
     * `repeated .boolerules.properties.PbProperty properties = 4;`
     * @param index The index to set the value at.
     * @param value The properties to set.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("setProperties")
    public operator fun com.google.protobuf.kotlin.DslList<com.booleworks.prl.model.protobuf.ProtoBufProperties.PbProperty, PropertiesProxy>.set(index: kotlin.Int, value: com.booleworks.prl.model.protobuf.ProtoBufProperties.PbProperty) {
      _builder.setProperties(index, value)
    }
    /**
     * `repeated .boolerules.properties.PbProperty properties = 4;`
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("clearProperties")
    public fun com.google.protobuf.kotlin.DslList<com.booleworks.prl.model.protobuf.ProtoBufProperties.PbProperty, PropertiesProxy>.clear() {
      _builder.clearProperties()
    }


    /**
     * `optional int32 lineNumber = 5;`
     */
    public var lineNumber: kotlin.Int
      @JvmName("getLineNumber")
      get() = _builder.getLineNumber()
      @JvmName("setLineNumber")
      set(value) {
        _builder.setLineNumber(value)
      }
    /**
     * `optional int32 lineNumber = 5;`
     */
    public fun clearLineNumber() {
      _builder.clearLineNumber()
    }
    /**
     * `optional int32 lineNumber = 5;`
     * @return Whether the lineNumber field is set.
     */
    public fun hasLineNumber(): kotlin.Boolean {
      return _builder.hasLineNumber()
    }

    /**
     * ```
     * constraint rule
     * ```
     *
     * `optional .boolerules.constraints.PbConstraint constraint = 6;`
     */
    public var constraint: com.booleworks.prl.model.protobuf.ProtoBufConstraints.PbConstraint
      @JvmName("getConstraint")
      get() = _builder.getConstraint()
      @JvmName("setConstraint")
      set(value) {
        _builder.setConstraint(value)
      }
    /**
     * ```
     * constraint rule
     * ```
     *
     * `optional .boolerules.constraints.PbConstraint constraint = 6;`
     */
    public fun clearConstraint() {
      _builder.clearConstraint()
    }
    /**
     * ```
     * constraint rule
     * ```
     *
     * `optional .boolerules.constraints.PbConstraint constraint = 6;`
     * @return Whether the constraint field is set.
     */
    public fun hasConstraint(): kotlin.Boolean {
      return _builder.hasConstraint()
    }
    public val PbRuleKt.Dsl.constraintOrNull: com.booleworks.prl.model.protobuf.ProtoBufConstraints.PbConstraint?
      get() = _builder.constraintOrNull

    /**
     * ```
     * definition rule
     * ```
     *
     * `optional int32 feature = 7;`
     */
    public var feature: kotlin.Int
      @JvmName("getFeature")
      get() = _builder.getFeature()
      @JvmName("setFeature")
      set(value) {
        _builder.setFeature(value)
      }
    /**
     * ```
     * definition rule
     * ```
     *
     * `optional int32 feature = 7;`
     */
    public fun clearFeature() {
      _builder.clearFeature()
    }
    /**
     * ```
     * definition rule
     * ```
     *
     * `optional int32 feature = 7;`
     * @return Whether the feature field is set.
     */
    public fun hasFeature(): kotlin.Boolean {
      return _builder.hasFeature()
    }

    /**
     * `optional .boolerules.constraints.PbConstraint ifPart = 8;`
     */
    public var ifPart: com.booleworks.prl.model.protobuf.ProtoBufConstraints.PbConstraint
      @JvmName("getIfPart")
      get() = _builder.getIfPart()
      @JvmName("setIfPart")
      set(value) {
        _builder.setIfPart(value)
      }
    /**
     * `optional .boolerules.constraints.PbConstraint ifPart = 8;`
     */
    public fun clearIfPart() {
      _builder.clearIfPart()
    }
    /**
     * `optional .boolerules.constraints.PbConstraint ifPart = 8;`
     * @return Whether the ifPart field is set.
     */
    public fun hasIfPart(): kotlin.Boolean {
      return _builder.hasIfPart()
    }
    public val PbRuleKt.Dsl.ifPartOrNull: com.booleworks.prl.model.protobuf.ProtoBufConstraints.PbConstraint?
      get() = _builder.ifPartOrNull

    /**
     * `optional .boolerules.constraints.PbConstraint thenPart = 9;`
     */
    public var thenPart: com.booleworks.prl.model.protobuf.ProtoBufConstraints.PbConstraint
      @JvmName("getThenPart")
      get() = _builder.getThenPart()
      @JvmName("setThenPart")
      set(value) {
        _builder.setThenPart(value)
      }
    /**
     * `optional .boolerules.constraints.PbConstraint thenPart = 9;`
     */
    public fun clearThenPart() {
      _builder.clearThenPart()
    }
    /**
     * `optional .boolerules.constraints.PbConstraint thenPart = 9;`
     * @return Whether the thenPart field is set.
     */
    public fun hasThenPart(): kotlin.Boolean {
      return _builder.hasThenPart()
    }
    public val PbRuleKt.Dsl.thenPartOrNull: com.booleworks.prl.model.protobuf.ProtoBufConstraints.PbConstraint?
      get() = _builder.thenPartOrNull

    /**
     * ```
     * exclusion
     * ```
     *
     * `optional .boolerules.constraints.PbConstraint thenNotPart = 10;`
     */
    public var thenNotPart: com.booleworks.prl.model.protobuf.ProtoBufConstraints.PbConstraint
      @JvmName("getThenNotPart")
      get() = _builder.getThenNotPart()
      @JvmName("setThenNotPart")
      set(value) {
        _builder.setThenNotPart(value)
      }
    /**
     * ```
     * exclusion
     * ```
     *
     * `optional .boolerules.constraints.PbConstraint thenNotPart = 10;`
     */
    public fun clearThenNotPart() {
      _builder.clearThenNotPart()
    }
    /**
     * ```
     * exclusion
     * ```
     *
     * `optional .boolerules.constraints.PbConstraint thenNotPart = 10;`
     * @return Whether the thenNotPart field is set.
     */
    public fun hasThenNotPart(): kotlin.Boolean {
      return _builder.hasThenNotPart()
    }
    public val PbRuleKt.Dsl.thenNotPartOrNull: com.booleworks.prl.model.protobuf.ProtoBufConstraints.PbConstraint?
      get() = _builder.thenNotPartOrNull

    /**
     * ```
     * if-then-else
     * ```
     *
     * `optional .boolerules.constraints.PbConstraint elsePart = 11;`
     */
    public var elsePart: com.booleworks.prl.model.protobuf.ProtoBufConstraints.PbConstraint
      @JvmName("getElsePart")
      get() = _builder.getElsePart()
      @JvmName("setElsePart")
      set(value) {
        _builder.setElsePart(value)
      }
    /**
     * ```
     * if-then-else
     * ```
     *
     * `optional .boolerules.constraints.PbConstraint elsePart = 11;`
     */
    public fun clearElsePart() {
      _builder.clearElsePart()
    }
    /**
     * ```
     * if-then-else
     * ```
     *
     * `optional .boolerules.constraints.PbConstraint elsePart = 11;`
     * @return Whether the elsePart field is set.
     */
    public fun hasElsePart(): kotlin.Boolean {
      return _builder.hasElsePart()
    }
    public val PbRuleKt.Dsl.elsePartOrNull: com.booleworks.prl.model.protobuf.ProtoBufConstraints.PbConstraint?
      get() = _builder.elsePartOrNull

    /**
     * ```
     * feature rule
     * ```
     *
     * `optional bool isForbidden = 12;`
     */
    public var isForbidden: kotlin.Boolean
      @JvmName("getIsForbidden")
      get() = _builder.getIsForbidden()
      @JvmName("setIsForbidden")
      set(value) {
        _builder.setIsForbidden(value)
      }
    /**
     * ```
     * feature rule
     * ```
     *
     * `optional bool isForbidden = 12;`
     */
    public fun clearIsForbidden() {
      _builder.clearIsForbidden()
    }
    /**
     * ```
     * feature rule
     * ```
     *
     * `optional bool isForbidden = 12;`
     * @return Whether the isForbidden field is set.
     */
    public fun hasIsForbidden(): kotlin.Boolean {
      return _builder.hasIsForbidden()
    }

    /**
     * `optional string enumValue = 13;`
     */
    public var enumValue: kotlin.String
      @JvmName("getEnumValue")
      get() = _builder.getEnumValue()
      @JvmName("setEnumValue")
      set(value) {
        _builder.setEnumValue(value)
      }
    /**
     * `optional string enumValue = 13;`
     */
    public fun clearEnumValue() {
      _builder.clearEnumValue()
    }
    /**
     * `optional string enumValue = 13;`
     * @return Whether the enumValue field is set.
     */
    public fun hasEnumValue(): kotlin.Boolean {
      return _builder.hasEnumValue()
    }

    /**
     * `optional int32 intValueOrVersion = 14;`
     */
    public var intValueOrVersion: kotlin.Int
      @JvmName("getIntValueOrVersion")
      get() = _builder.getIntValueOrVersion()
      @JvmName("setIntValueOrVersion")
      set(value) {
        _builder.setIntValueOrVersion(value)
      }
    /**
     * `optional int32 intValueOrVersion = 14;`
     */
    public fun clearIntValueOrVersion() {
      _builder.clearIntValueOrVersion()
    }
    /**
     * `optional int32 intValueOrVersion = 14;`
     * @return Whether the intValueOrVersion field is set.
     */
    public fun hasIntValueOrVersion(): kotlin.Boolean {
      return _builder.hasIntValueOrVersion()
    }

    /**
     * ```
     * group rule
     * ```
     *
     * `optional bool isAmo = 15;`
     */
    public var isAmo: kotlin.Boolean
      @JvmName("getIsAmo")
      get() = _builder.getIsAmo()
      @JvmName("setIsAmo")
      set(value) {
        _builder.setIsAmo(value)
      }
    /**
     * ```
     * group rule
     * ```
     *
     * `optional bool isAmo = 15;`
     */
    public fun clearIsAmo() {
      _builder.clearIsAmo()
    }
    /**
     * ```
     * group rule
     * ```
     *
     * `optional bool isAmo = 15;`
     * @return Whether the isAmo field is set.
     */
    public fun hasIsAmo(): kotlin.Boolean {
      return _builder.hasIsAmo()
    }

    /**
     * An uninstantiable, behaviorless type to represent the field in
     * generics.
     */
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    public class GroupFeaturesProxy private constructor() : com.google.protobuf.kotlin.DslProxy()
    /**
     * ```
     * group rule
     * ```
     *
     * `repeated int32 groupFeatures = 16;`
     */
     public val groupFeatures: com.google.protobuf.kotlin.DslList<kotlin.Int, GroupFeaturesProxy>
      @kotlin.jvm.JvmSynthetic
      get() = com.google.protobuf.kotlin.DslList(
        _builder.getGroupFeaturesList()
      )
    /**
     * ```
     * group rule
     * ```
     *
     * `repeated int32 groupFeatures = 16;`
     * @param value The groupFeatures to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addGroupFeatures")
    public fun com.google.protobuf.kotlin.DslList<kotlin.Int, GroupFeaturesProxy>.add(value: kotlin.Int) {
      _builder.addGroupFeatures(value)
    }/**
     * ```
     * group rule
     * ```
     *
     * `repeated int32 groupFeatures = 16;`
     * @param value The groupFeatures to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignGroupFeatures")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<kotlin.Int, GroupFeaturesProxy>.plusAssign(value: kotlin.Int) {
      add(value)
    }/**
     * ```
     * group rule
     * ```
     *
     * `repeated int32 groupFeatures = 16;`
     * @param values The groupFeatures to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addAllGroupFeatures")
    public fun com.google.protobuf.kotlin.DslList<kotlin.Int, GroupFeaturesProxy>.addAll(values: kotlin.collections.Iterable<kotlin.Int>) {
      _builder.addAllGroupFeatures(values)
    }/**
     * ```
     * group rule
     * ```
     *
     * `repeated int32 groupFeatures = 16;`
     * @param values The groupFeatures to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignAllGroupFeatures")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<kotlin.Int, GroupFeaturesProxy>.plusAssign(values: kotlin.collections.Iterable<kotlin.Int>) {
      addAll(values)
    }/**
     * ```
     * group rule
     * ```
     *
     * `repeated int32 groupFeatures = 16;`
     * @param index The index to set the value at.
     * @param value The groupFeatures to set.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("setGroupFeatures")
    public operator fun com.google.protobuf.kotlin.DslList<kotlin.Int, GroupFeaturesProxy>.set(index: kotlin.Int, value: kotlin.Int) {
      _builder.setGroupFeatures(index, value)
    }/**
     * ```
     * group rule
     * ```
     *
     * `repeated int32 groupFeatures = 16;`
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("clearGroupFeatures")
    public fun com.google.protobuf.kotlin.DslList<kotlin.Int, GroupFeaturesProxy>.clear() {
      _builder.clearGroupFeatures()
    }}
}
@kotlin.jvm.JvmSynthetic
public inline fun com.booleworks.prl.model.protobuf.ProtoBufRules.PbRule.copy(block: `com.booleworks.prl.model.protobuf`.PbRuleKt.Dsl.() -> kotlin.Unit): com.booleworks.prl.model.protobuf.ProtoBufRules.PbRule =
  `com.booleworks.prl.model.protobuf`.PbRuleKt.Dsl._create(this.toBuilder()).apply { block() }._build()

public val com.booleworks.prl.model.protobuf.ProtoBufRules.PbRuleOrBuilder.constraintOrNull: com.booleworks.prl.model.protobuf.ProtoBufConstraints.PbConstraint?
  get() = if (hasConstraint()) getConstraint() else null

public val com.booleworks.prl.model.protobuf.ProtoBufRules.PbRuleOrBuilder.ifPartOrNull: com.booleworks.prl.model.protobuf.ProtoBufConstraints.PbConstraint?
  get() = if (hasIfPart()) getIfPart() else null

public val com.booleworks.prl.model.protobuf.ProtoBufRules.PbRuleOrBuilder.thenPartOrNull: com.booleworks.prl.model.protobuf.ProtoBufConstraints.PbConstraint?
  get() = if (hasThenPart()) getThenPart() else null

public val com.booleworks.prl.model.protobuf.ProtoBufRules.PbRuleOrBuilder.thenNotPartOrNull: com.booleworks.prl.model.protobuf.ProtoBufConstraints.PbConstraint?
  get() = if (hasThenNotPart()) getThenNotPart() else null

public val com.booleworks.prl.model.protobuf.ProtoBufRules.PbRuleOrBuilder.elsePartOrNull: com.booleworks.prl.model.protobuf.ProtoBufConstraints.PbConstraint?
  get() = if (hasElsePart()) getElsePart() else null

