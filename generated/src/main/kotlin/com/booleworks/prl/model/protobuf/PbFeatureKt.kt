// Generated by the protocol buffer compiler. DO NOT EDIT!
// source: model_featurestore.proto

// Generated files should ignore deprecation warnings
@file:Suppress("DEPRECATION")
package com.booleworks.prl.model.protobuf;

@kotlin.jvm.JvmName("-initializepbFeature")
public inline fun pbFeature(block: com.booleworks.prl.model.protobuf.PbFeatureKt.Dsl.() -> kotlin.Unit): com.booleworks.prl.model.protobuf.ProtoBufFeatureStore.PbFeature =
  com.booleworks.prl.model.protobuf.PbFeatureKt.Dsl._create(com.booleworks.prl.model.protobuf.ProtoBufFeatureStore.PbFeature.newBuilder()).apply { block() }._build()
/**
 * Protobuf type `boolerules.features.PbFeature`
 */
public object PbFeatureKt {
  @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
  @com.google.protobuf.kotlin.ProtoDslMarker
  public class Dsl private constructor(
    private val _builder: com.booleworks.prl.model.protobuf.ProtoBufFeatureStore.PbFeature.Builder
  ) {
    public companion object {
      @kotlin.jvm.JvmSynthetic
      @kotlin.PublishedApi
      internal fun _create(builder: com.booleworks.prl.model.protobuf.ProtoBufFeatureStore.PbFeature.Builder): Dsl = Dsl(builder)
    }

    @kotlin.jvm.JvmSynthetic
    @kotlin.PublishedApi
    internal fun _build(): com.booleworks.prl.model.protobuf.ProtoBufFeatureStore.PbFeature = _builder.build()

    /**
     * `int32 id = 1;`
     */
    public var id: kotlin.Int
      @JvmName("getId")
      get() = _builder.getId()
      @JvmName("setId")
      set(value) {
        _builder.setId(value)
      }
    /**
     * `int32 id = 1;`
     */
    public fun clearId() {
      _builder.clearId()
    }
  }
}
@kotlin.jvm.JvmSynthetic
public inline fun com.booleworks.prl.model.protobuf.ProtoBufFeatureStore.PbFeature.copy(block: `com.booleworks.prl.model.protobuf`.PbFeatureKt.Dsl.() -> kotlin.Unit): com.booleworks.prl.model.protobuf.ProtoBufFeatureStore.PbFeature =
  `com.booleworks.prl.model.protobuf`.PbFeatureKt.Dsl._create(this.toBuilder()).apply { block() }._build()

