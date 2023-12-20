// Generated by the protocol buffer compiler. DO NOT EDIT!
// source: model.proto

// Generated files should ignore deprecation warnings
@file:Suppress("DEPRECATION")
package com.booleworks.prl.model.protobuf;

@kotlin.jvm.JvmName("-initializepbHeader")
public inline fun pbHeader(block: com.booleworks.prl.model.protobuf.PbHeaderKt.Dsl.() -> kotlin.Unit): com.booleworks.prl.model.protobuf.ProtoBufModel.PbHeader =
  com.booleworks.prl.model.protobuf.PbHeaderKt.Dsl._create(com.booleworks.prl.model.protobuf.ProtoBufModel.PbHeader.newBuilder()).apply { block() }._build()
/**
 * Protobuf type `boolerules.model.PbHeader`
 */
public object PbHeaderKt {
  @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
  @com.google.protobuf.kotlin.ProtoDslMarker
  public class Dsl private constructor(
    private val _builder: com.booleworks.prl.model.protobuf.ProtoBufModel.PbHeader.Builder
  ) {
    public companion object {
      @kotlin.jvm.JvmSynthetic
      @kotlin.PublishedApi
      internal fun _create(builder: com.booleworks.prl.model.protobuf.ProtoBufModel.PbHeader.Builder): Dsl = Dsl(builder)
    }

    @kotlin.jvm.JvmSynthetic
    @kotlin.PublishedApi
    internal fun _build(): com.booleworks.prl.model.protobuf.ProtoBufModel.PbHeader = _builder.build()

    /**
     * `int32 major = 1;`
     */
    public var major: kotlin.Int
      @JvmName("getMajor")
      get() = _builder.getMajor()
      @JvmName("setMajor")
      set(value) {
        _builder.setMajor(value)
      }
    /**
     * `int32 major = 1;`
     */
    public fun clearMajor() {
      _builder.clearMajor()
    }

    /**
     * `int32 minor = 2;`
     */
    public var minor: kotlin.Int
      @JvmName("getMinor")
      get() = _builder.getMinor()
      @JvmName("setMinor")
      set(value) {
        _builder.setMinor(value)
      }
    /**
     * `int32 minor = 2;`
     */
    public fun clearMinor() {
      _builder.clearMinor()
    }

    /**
     * An uninstantiable, behaviorless type to represent the field in
     * generics.
     */
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    public class PropertiesProxy private constructor() : com.google.protobuf.kotlin.DslProxy()
    /**
     * `repeated .boolerules.properties.PbProperty properties = 3;`
     */
     public val properties: com.google.protobuf.kotlin.DslList<com.booleworks.prl.model.protobuf.ProtoBufProperties.PbProperty, PropertiesProxy>
      @kotlin.jvm.JvmSynthetic
      get() = com.google.protobuf.kotlin.DslList(
        _builder.getPropertiesList()
      )
    /**
     * `repeated .boolerules.properties.PbProperty properties = 3;`
     * @param value The properties to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addProperties")
    public fun com.google.protobuf.kotlin.DslList<com.booleworks.prl.model.protobuf.ProtoBufProperties.PbProperty, PropertiesProxy>.add(value: com.booleworks.prl.model.protobuf.ProtoBufProperties.PbProperty) {
      _builder.addProperties(value)
    }
    /**
     * `repeated .boolerules.properties.PbProperty properties = 3;`
     * @param value The properties to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignProperties")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<com.booleworks.prl.model.protobuf.ProtoBufProperties.PbProperty, PropertiesProxy>.plusAssign(value: com.booleworks.prl.model.protobuf.ProtoBufProperties.PbProperty) {
      add(value)
    }
    /**
     * `repeated .boolerules.properties.PbProperty properties = 3;`
     * @param values The properties to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addAllProperties")
    public fun com.google.protobuf.kotlin.DslList<com.booleworks.prl.model.protobuf.ProtoBufProperties.PbProperty, PropertiesProxy>.addAll(values: kotlin.collections.Iterable<com.booleworks.prl.model.protobuf.ProtoBufProperties.PbProperty>) {
      _builder.addAllProperties(values)
    }
    /**
     * `repeated .boolerules.properties.PbProperty properties = 3;`
     * @param values The properties to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignAllProperties")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<com.booleworks.prl.model.protobuf.ProtoBufProperties.PbProperty, PropertiesProxy>.plusAssign(values: kotlin.collections.Iterable<com.booleworks.prl.model.protobuf.ProtoBufProperties.PbProperty>) {
      addAll(values)
    }
    /**
     * `repeated .boolerules.properties.PbProperty properties = 3;`
     * @param index The index to set the value at.
     * @param value The properties to set.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("setProperties")
    public operator fun com.google.protobuf.kotlin.DslList<com.booleworks.prl.model.protobuf.ProtoBufProperties.PbProperty, PropertiesProxy>.set(index: kotlin.Int, value: com.booleworks.prl.model.protobuf.ProtoBufProperties.PbProperty) {
      _builder.setProperties(index, value)
    }
    /**
     * `repeated .boolerules.properties.PbProperty properties = 3;`
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("clearProperties")
    public fun com.google.protobuf.kotlin.DslList<com.booleworks.prl.model.protobuf.ProtoBufProperties.PbProperty, PropertiesProxy>.clear() {
      _builder.clearProperties()
    }

  }
}
@kotlin.jvm.JvmSynthetic
public inline fun com.booleworks.prl.model.protobuf.ProtoBufModel.PbHeader.copy(block: `com.booleworks.prl.model.protobuf`.PbHeaderKt.Dsl.() -> kotlin.Unit): com.booleworks.prl.model.protobuf.ProtoBufModel.PbHeader =
  `com.booleworks.prl.model.protobuf`.PbHeaderKt.Dsl._create(this.toBuilder()).apply { block() }._build()

