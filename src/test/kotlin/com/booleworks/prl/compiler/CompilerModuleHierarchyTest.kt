package com.booleworks.prl.compiler

import com.booleworks.prl.TestUtil.moduleInputFrom
import com.booleworks.prl.model.Module
import com.booleworks.prl.model.deserialize
import com.booleworks.prl.model.serialize
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class CompilerModuleHierarchyTest {

    @Test
    fun testModule() {
        val simple = Module("simple")
        val simpleEven = Module("simple.even")
        val simpleEvenMore = Module("simple.even.more")

        assertThat(simple.name).isEqualTo("simple")
        assertThat(simpleEven.name).isEqualTo("even")
        assertThat(simpleEvenMore.name).isEqualTo("more")

        assertThat(simple.isAncestorOf(simpleEven)).isTrue
        assertThat(simple.isAncestorOf(simpleEvenMore)).isTrue
        assertThat(simple.isAncestorOf(simple)).isFalse
        assertThat(simpleEven.isAncestorOf(simple)).isFalse
        assertThat(simpleEven.isAncestorOf(simpleEvenMore)).isTrue
        assertThat(simpleEven.isAncestorOf(simpleEven)).isFalse
        assertThat(simpleEvenMore.isAncestorOf(simple)).isFalse
        assertThat(simpleEvenMore.isAncestorOf(simpleEven)).isFalse
        assertThat(simpleEvenMore.isAncestorOf(simpleEvenMore)).isFalse

        assertThat(simple.isDescendantOf(simpleEven)).isFalse
        assertThat(simple.isDescendantOf(simpleEvenMore)).isFalse
        assertThat(simple.isDescendantOf(simple)).isFalse
        assertThat(simpleEven.isDescendantOf(simple)).isTrue
        assertThat(simpleEven.isDescendantOf(simpleEvenMore)).isFalse
        assertThat(simpleEven.isDescendantOf(simpleEven)).isFalse
        assertThat(simpleEvenMore.isDescendantOf(simple)).isTrue
        assertThat(simpleEvenMore.isDescendantOf(simpleEven)).isTrue
        assertThat(simpleEvenMore.isDescendantOf(simpleEvenMore)).isFalse
    }

    @Test
    fun testModuleHierarchySingle() {
        val mh = PrlCompiler().compileModules(moduleInputFrom(listOf("simplest")))
        assertThat(mh.numberOfModules()).isEqualTo(1)

        assertThat(mh.moduleForName("simplest")?.name).isEqualTo("simplest")
        assertThat(mh.moduleForName("simplest")?.ancestor).isNull()
        assertThat(mh.moduleForName("simplest")?.descendants).isEmpty()
        assertThat(mh.moduleForName("simplest")?.allAncestors()).isEmpty()
        assertThat(mh.moduleForName("simplest")?.allDescendants()).isEmpty()

        assertThat(deserialize(serialize(mh))).isEqualTo(mh)
    }

    @Test
    fun testModuleHierarchyTwoModules() {
        val mh = PrlCompiler().compileModules(moduleInputFrom(listOf("simplest", "simple")))

        assertThat(mh.numberOfModules()).isEqualTo(2)

        assertThat(mh.moduleForName("simplest")?.name).isEqualTo("simplest")
        assertThat(mh.moduleForName("simplest")?.ancestor).isNull()
        assertThat(mh.moduleForName("simplest")?.descendants).isEmpty()
        assertThat(mh.moduleForName("simplest")?.allAncestors()).isEmpty()
        assertThat(mh.moduleForName("simplest")?.allDescendants()).isEmpty()

        assertThat(mh.moduleForName("simple")?.name).isEqualTo("simple")
        assertThat(mh.moduleForName("simple")?.ancestor).isNull()
        assertThat(mh.moduleForName("simple")?.descendants).isEmpty()
        assertThat(mh.moduleForName("simple")?.allAncestors()).isEmpty()
        assertThat(mh.moduleForName("simple")?.allDescendants()).isEmpty()

        assertThat(deserialize(serialize(mh))).isEqualTo(mh)
    }

    @Test
    fun testModuleHierarchyTree() {
        val mh = PrlCompiler().compileModules(
            moduleInputFrom(
                listOf(
                    "com.br",
                    "com.br.abc",
                    "com.br.ln.f",
                    "com.br.ln.f.p.g",
                    "com.br.abc.def",
                    "com.br.ln",
                    "com.br.ln.f.p.h"
                )
            )
        )

        assertThat(mh.numberOfModules()).isEqualTo(7)

        val br = mh.moduleForName("com.br")
        val abc = mh.moduleForName("com.br.abc")
        val def = mh.moduleForName("com.br.abc.def")
        val ln = mh.moduleForName("com.br.ln")
        val f = mh.moduleForName("com.br.ln.f")
        val g = mh.moduleForName("com.br.ln.f.p.g")
        val h = mh.moduleForName("com.br.ln.f.p.h")

        assertThat(br?.ancestor).isNull()
        assertThat(br?.descendants).containsExactlyInAnyOrder(abc, ln)
        assertThat(br?.allAncestors()).isEmpty()
        assertThat(br?.allDescendants()).containsExactlyInAnyOrder(abc, def, ln, f, g, h)

        assertThat(abc?.ancestor).isEqualTo(br)
        assertThat(abc?.descendants).containsExactlyInAnyOrder(def)
        assertThat(abc?.allAncestors()).containsExactlyInAnyOrder(br)
        assertThat(abc?.allDescendants()).containsExactlyInAnyOrder(def)

        assertThat(def?.ancestor).isEqualTo(abc)
        assertThat(def?.descendants).isEmpty()
        assertThat(def?.allAncestors()).containsExactlyInAnyOrder(br, abc)
        assertThat(def?.allDescendants()).isEmpty()

        assertThat(ln?.ancestor).isEqualTo(br)
        assertThat(ln?.descendants).containsExactlyInAnyOrder(f)
        assertThat(ln?.allAncestors()).containsExactlyInAnyOrder(br)
        assertThat(ln?.allDescendants()).containsExactlyInAnyOrder(f, g, h)

        assertThat(f?.ancestor).isEqualTo(ln)
        assertThat(f?.descendants).containsExactlyInAnyOrder(g, h)
        assertThat(f?.allAncestors()).containsExactlyInAnyOrder(br, ln)
        assertThat(f?.allDescendants()).containsExactlyInAnyOrder(g, h)

        assertThat(g?.ancestor).isEqualTo(f)
        assertThat(g?.descendants).isEmpty()
        assertThat(g?.allAncestors()).containsExactlyInAnyOrder(br, ln, f)
        assertThat(g?.allDescendants()).isEmpty()

        assertThat(h?.ancestor).isEqualTo(f)
        assertThat(h?.descendants).isEmpty()
        assertThat(h?.allAncestors()).containsExactlyInAnyOrder(br, ln, f)
        assertThat(h?.allDescendants()).isEmpty()

        assertThat(deserialize(serialize(mh))).isEqualTo(mh)
    }

    @Test
    fun testModuleHierarchyForest() {
        val mh = PrlCompiler().compileModules(
            moduleInputFrom(
                listOf(
                    "com.br",
                    "com.br.abc",
                    "org.br.ln.f",
                    "org.br.ln.f.p.g",
                    "com.br.abc.def",
                    "org.br.ln",
                    "org.br.ln.f.p.h"
                )
            )
        )

        assertThat(mh.numberOfModules()).isEqualTo(7)

        val br = mh.moduleForName("com.br")
        val abc = mh.moduleForName("com.br.abc")
        val def = mh.moduleForName("com.br.abc.def")
        val ln = mh.moduleForName("org.br.ln")
        val f = mh.moduleForName("org.br.ln.f")
        val g = mh.moduleForName("org.br.ln.f.p.g")
        val h = mh.moduleForName("org.br.ln.f.p.h")

        assertThat(br?.ancestor).isNull()
        assertThat(br?.descendants).containsExactlyInAnyOrder(abc)
        assertThat(br?.allAncestors()).isEmpty()
        assertThat(br?.allDescendants()).containsExactlyInAnyOrder(abc, def)

        assertThat(abc?.ancestor).isEqualTo(br)
        assertThat(abc?.descendants).containsExactlyInAnyOrder(def)
        assertThat(abc?.allAncestors()).containsExactlyInAnyOrder(br)
        assertThat(abc?.allDescendants()).containsExactlyInAnyOrder(def)

        assertThat(def?.ancestor).isEqualTo(abc)
        assertThat(def?.descendants).isEmpty()
        assertThat(def?.allAncestors()).containsExactlyInAnyOrder(br, abc)
        assertThat(def?.allDescendants()).isEmpty()

        assertThat(ln?.ancestor).isNull()
        assertThat(ln?.descendants).containsExactlyInAnyOrder(f)
        assertThat(ln?.allAncestors()).isEmpty()
        assertThat(ln?.allDescendants()).containsExactlyInAnyOrder(f, g, h)

        assertThat(f?.ancestor).isEqualTo(ln)
        assertThat(f?.descendants).containsExactlyInAnyOrder(g, h)
        assertThat(f?.allAncestors()).containsExactlyInAnyOrder(ln)
        assertThat(f?.allDescendants()).containsExactlyInAnyOrder(g, h)

        assertThat(g?.ancestor).isEqualTo(f)
        assertThat(g?.descendants).isEmpty()
        assertThat(g?.allAncestors()).containsExactlyInAnyOrder(ln, f)
        assertThat(g?.allDescendants()).isEmpty()

        assertThat(h?.ancestor).isEqualTo(f)
        assertThat(h?.descendants).isEmpty()
        assertThat(h?.allAncestors()).containsExactlyInAnyOrder(ln, f)
        assertThat(h?.allDescendants()).isEmpty()

        assertThat(deserialize(serialize(mh))).isEqualTo(mh)
    }

    @Test
    fun testDuplicateModule() {
        val compiler = PrlCompiler()
        compiler.compileModules(moduleInputFrom(listOf("com.br", "simple", "com.br")))
        assertThat(compiler.hasErrors()).isTrue
        assertThat(compiler.errors()).containsExactly("[module=com.br, lineNumber=0] Duplicate module declaration")
    }

    @Test
    fun testDuplicateEmptyModule() {
        val compiler = PrlCompiler()
        compiler.compileModules(moduleInputFrom(listOf("", "com.br", "simple", "")))
        assertThat(compiler.hasErrors()).isTrue
        assertThat(compiler.errors()).containsExactly("[module=, lineNumber=0] Duplicate module declaration")
    }
}

