// Generated by the protocol buffer compiler. DO NOT EDIT!
// source: model_modules.proto

// Generated files should ignore deprecation warnings
@file:Suppress("DEPRECATION")
package com.booleworks.prl.model.protobuf;

@kotlin.jvm.JvmName("-initializepbModule")
public inline fun pbModule(block: com.booleworks.prl.model.protobuf.PbModuleKt.Dsl.() -> kotlin.Unit): com.booleworks.prl.model.protobuf.ProtoBufModules.PbModule =
  com.booleworks.prl.model.protobuf.PbModuleKt.Dsl._create(com.booleworks.prl.model.protobuf.ProtoBufModules.PbModule.newBuilder()).apply { block() }._build()
/**
 * Protobuf type `boolerules.modules.PbModule`
 */
public object PbModuleKt {
  @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
  @com.google.protobuf.kotlin.ProtoDslMarker
  public class Dsl private constructor(
    private val _builder: com.booleworks.prl.model.protobuf.ProtoBufModules.PbModule.Builder
  ) {
    public companion object {
      @kotlin.jvm.JvmSynthetic
      @kotlin.PublishedApi
      internal fun _create(builder: com.booleworks.prl.model.protobuf.ProtoBufModules.PbModule.Builder): Dsl = Dsl(builder)
    }

    @kotlin.jvm.JvmSynthetic
    @kotlin.PublishedApi
    internal fun _build(): com.booleworks.prl.model.protobuf.ProtoBufModules.PbModule = _builder.build()

    /**
     * `string fullName = 1;`
     */
    public var fullName: kotlin.String
      @JvmName("getFullName")
      get() = _builder.getFullName()
      @JvmName("setFullName")
      set(value) {
        _builder.setFullName(value)
      }
    /**
     * `string fullName = 1;`
     */
    public fun clearFullName() {
      _builder.clearFullName()
    }

    /**
     * An uninstantiable, behaviorless type to represent the field in
     * generics.
     */
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    public class ImportsProxy private constructor() : com.google.protobuf.kotlin.DslProxy()
    /**
     * `repeated .boolerules.modules.PbModule imports = 2;`
     */
     public val imports: com.google.protobuf.kotlin.DslList<com.booleworks.prl.model.protobuf.ProtoBufModules.PbModule, ImportsProxy>
      @kotlin.jvm.JvmSynthetic
      get() = com.google.protobuf.kotlin.DslList(
        _builder.getImportsList()
      )
    /**
     * `repeated .boolerules.modules.PbModule imports = 2;`
     * @param value The imports to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addImports")
    public fun com.google.protobuf.kotlin.DslList<com.booleworks.prl.model.protobuf.ProtoBufModules.PbModule, ImportsProxy>.add(value: com.booleworks.prl.model.protobuf.ProtoBufModules.PbModule) {
      _builder.addImports(value)
    }
    /**
     * `repeated .boolerules.modules.PbModule imports = 2;`
     * @param value The imports to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignImports")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<com.booleworks.prl.model.protobuf.ProtoBufModules.PbModule, ImportsProxy>.plusAssign(value: com.booleworks.prl.model.protobuf.ProtoBufModules.PbModule) {
      add(value)
    }
    /**
     * `repeated .boolerules.modules.PbModule imports = 2;`
     * @param values The imports to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addAllImports")
    public fun com.google.protobuf.kotlin.DslList<com.booleworks.prl.model.protobuf.ProtoBufModules.PbModule, ImportsProxy>.addAll(values: kotlin.collections.Iterable<com.booleworks.prl.model.protobuf.ProtoBufModules.PbModule>) {
      _builder.addAllImports(values)
    }
    /**
     * `repeated .boolerules.modules.PbModule imports = 2;`
     * @param values The imports to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignAllImports")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<com.booleworks.prl.model.protobuf.ProtoBufModules.PbModule, ImportsProxy>.plusAssign(values: kotlin.collections.Iterable<com.booleworks.prl.model.protobuf.ProtoBufModules.PbModule>) {
      addAll(values)
    }
    /**
     * `repeated .boolerules.modules.PbModule imports = 2;`
     * @param index The index to set the value at.
     * @param value The imports to set.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("setImports")
    public operator fun com.google.protobuf.kotlin.DslList<com.booleworks.prl.model.protobuf.ProtoBufModules.PbModule, ImportsProxy>.set(index: kotlin.Int, value: com.booleworks.prl.model.protobuf.ProtoBufModules.PbModule) {
      _builder.setImports(index, value)
    }
    /**
     * `repeated .boolerules.modules.PbModule imports = 2;`
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("clearImports")
    public fun com.google.protobuf.kotlin.DslList<com.booleworks.prl.model.protobuf.ProtoBufModules.PbModule, ImportsProxy>.clear() {
      _builder.clearImports()
    }


    /**
     * `optional int32 lineNumber = 3;`
     */
    public var lineNumber: kotlin.Int
      @JvmName("getLineNumber")
      get() = _builder.getLineNumber()
      @JvmName("setLineNumber")
      set(value) {
        _builder.setLineNumber(value)
      }
    /**
     * `optional int32 lineNumber = 3;`
     */
    public fun clearLineNumber() {
      _builder.clearLineNumber()
    }
    /**
     * `optional int32 lineNumber = 3;`
     * @return Whether the lineNumber field is set.
     */
    public fun hasLineNumber(): kotlin.Boolean {
      return _builder.hasLineNumber()
    }
  }
}
@kotlin.jvm.JvmSynthetic
public inline fun com.booleworks.prl.model.protobuf.ProtoBufModules.PbModule.copy(block: `com.booleworks.prl.model.protobuf`.PbModuleKt.Dsl.() -> kotlin.Unit): com.booleworks.prl.model.protobuf.ProtoBufModules.PbModule =
  `com.booleworks.prl.model.protobuf`.PbModuleKt.Dsl._create(this.toBuilder()).apply { block() }._build()
