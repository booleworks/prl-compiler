// Generated by the protocol buffer compiler. DO NOT EDIT!
// source: model_featurestore.proto

// Generated files should ignore deprecation warnings
@file:Suppress("DEPRECATION")
package com.booleworks.prl.model.protobuf;

@kotlin.jvm.JvmName("-initializepbAllFeatures")
public inline fun pbAllFeatures(block: com.booleworks.prl.model.protobuf.PbAllFeaturesKt.Dsl.() -> kotlin.Unit): com.booleworks.prl.model.protobuf.ProtoBufFeatureStore.PbAllFeatures =
  com.booleworks.prl.model.protobuf.PbAllFeaturesKt.Dsl._create(com.booleworks.prl.model.protobuf.ProtoBufFeatureStore.PbAllFeatures.newBuilder()).apply { block() }._build()
/**
 * Protobuf type `boolerules.features.PbAllFeatures`
 */
public object PbAllFeaturesKt {
  @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
  @com.google.protobuf.kotlin.ProtoDslMarker
  public class Dsl private constructor(
    private val _builder: com.booleworks.prl.model.protobuf.ProtoBufFeatureStore.PbAllFeatures.Builder
  ) {
    public companion object {
      @kotlin.jvm.JvmSynthetic
      @kotlin.PublishedApi
      internal fun _create(builder: com.booleworks.prl.model.protobuf.ProtoBufFeatureStore.PbAllFeatures.Builder): Dsl = Dsl(builder)
    }

    @kotlin.jvm.JvmSynthetic
    @kotlin.PublishedApi
    internal fun _build(): com.booleworks.prl.model.protobuf.ProtoBufFeatureStore.PbAllFeatures = _builder.build()

    /**
     * An uninstantiable, behaviorless type to represent the field in
     * generics.
     */
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    public class FeatureProxy private constructor() : com.google.protobuf.kotlin.DslProxy()
    /**
     * `repeated .boolerules.features.PbFullFeature feature = 1;`
     */
     public val feature: com.google.protobuf.kotlin.DslList<com.booleworks.prl.model.protobuf.ProtoBufFeatureStore.PbFullFeature, FeatureProxy>
      @kotlin.jvm.JvmSynthetic
      get() = com.google.protobuf.kotlin.DslList(
        _builder.getFeatureList()
      )
    /**
     * `repeated .boolerules.features.PbFullFeature feature = 1;`
     * @param value The feature to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addFeature")
    public fun com.google.protobuf.kotlin.DslList<com.booleworks.prl.model.protobuf.ProtoBufFeatureStore.PbFullFeature, FeatureProxy>.add(value: com.booleworks.prl.model.protobuf.ProtoBufFeatureStore.PbFullFeature) {
      _builder.addFeature(value)
    }
    /**
     * `repeated .boolerules.features.PbFullFeature feature = 1;`
     * @param value The feature to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignFeature")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<com.booleworks.prl.model.protobuf.ProtoBufFeatureStore.PbFullFeature, FeatureProxy>.plusAssign(value: com.booleworks.prl.model.protobuf.ProtoBufFeatureStore.PbFullFeature) {
      add(value)
    }
    /**
     * `repeated .boolerules.features.PbFullFeature feature = 1;`
     * @param values The feature to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addAllFeature")
    public fun com.google.protobuf.kotlin.DslList<com.booleworks.prl.model.protobuf.ProtoBufFeatureStore.PbFullFeature, FeatureProxy>.addAll(values: kotlin.collections.Iterable<com.booleworks.prl.model.protobuf.ProtoBufFeatureStore.PbFullFeature>) {
      _builder.addAllFeature(values)
    }
    /**
     * `repeated .boolerules.features.PbFullFeature feature = 1;`
     * @param values The feature to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignAllFeature")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<com.booleworks.prl.model.protobuf.ProtoBufFeatureStore.PbFullFeature, FeatureProxy>.plusAssign(values: kotlin.collections.Iterable<com.booleworks.prl.model.protobuf.ProtoBufFeatureStore.PbFullFeature>) {
      addAll(values)
    }
    /**
     * `repeated .boolerules.features.PbFullFeature feature = 1;`
     * @param index The index to set the value at.
     * @param value The feature to set.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("setFeature")
    public operator fun com.google.protobuf.kotlin.DslList<com.booleworks.prl.model.protobuf.ProtoBufFeatureStore.PbFullFeature, FeatureProxy>.set(index: kotlin.Int, value: com.booleworks.prl.model.protobuf.ProtoBufFeatureStore.PbFullFeature) {
      _builder.setFeature(index, value)
    }
    /**
     * `repeated .boolerules.features.PbFullFeature feature = 1;`
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("clearFeature")
    public fun com.google.protobuf.kotlin.DslList<com.booleworks.prl.model.protobuf.ProtoBufFeatureStore.PbFullFeature, FeatureProxy>.clear() {
      _builder.clearFeature()
    }

  }
}
@kotlin.jvm.JvmSynthetic
public inline fun com.booleworks.prl.model.protobuf.ProtoBufFeatureStore.PbAllFeatures.copy(block: `com.booleworks.prl.model.protobuf`.PbAllFeaturesKt.Dsl.() -> kotlin.Unit): com.booleworks.prl.model.protobuf.ProtoBufFeatureStore.PbAllFeatures =
  `com.booleworks.prl.model.protobuf`.PbAllFeaturesKt.Dsl._create(this.toBuilder()).apply { block() }._build()

