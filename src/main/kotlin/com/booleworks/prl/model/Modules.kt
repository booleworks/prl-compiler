// SPDX-License-Identifier: MIT
// Copyright 2023 BooleWorks GmbH

package com.booleworks.prl.model

import java.util.Objects

class Module(val fullName: String, val imports: MutableList<Module> = mutableListOf(), val lineNumber: Int? = null) {
    companion object {
        const val MODULE_SEPARATOR = "."
    }

    private val modulePath = fullName.split(MODULE_SEPARATOR)
    val name: String = modulePath.last()

    var descendants: List<Module> = listOf()
        internal set

    var ancestor: Module? = null
        internal set

    fun isAncestorOf(module: Module) = modulePath != module.modulePath && modulePath.size < module.modulePath.size
            && modulePath == module.modulePath.subList(0, modulePath.size)

    fun isDescendantOf(module: Module) = modulePath != module.modulePath && modulePath.size > module.modulePath.size
            && module.modulePath == modulePath.subList(0, module.modulePath.size)

    fun allAncestors(): List<Module> = ancestor?.let { it.allAncestors() + it } ?: emptyList()
    fun allDescendants(): List<Module> = descendants + descendants.flatMap { it.allDescendants() }
    fun hasImport(module: Module) = imports.contains(module)

    override fun toString() = fullName
    override fun hashCode() = Objects.hash(fullName, imports)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Module

        if (fullName != other.fullName) return false
        return imports == other.imports
    }
}

data class ModuleHierarchy(internal val modules: Map<String, Module>) {
    fun moduleForName(moduleName: String) = modules[moduleName]
    fun modules() = modules.values
    fun numberOfModules() = modules.size
}
