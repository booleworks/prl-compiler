package com.booleworks.prl.compiler

import com.booleworks.prl.model.IntRange
import com.booleworks.prl.model.Visibility
import com.booleworks.prl.model.constraints.and
import com.booleworks.prl.model.constraints.enumEq
import com.booleworks.prl.model.constraints.equiv
import com.booleworks.prl.model.constraints.impl
import com.booleworks.prl.model.constraints.intIn
import com.booleworks.prl.model.constraints.intLe
import com.booleworks.prl.model.constraints.intLt
import com.booleworks.prl.model.rules.ConstraintRule
import com.booleworks.prl.model.rules.DefinitionRule
import com.booleworks.prl.model.rules.ExclusionRule
import com.booleworks.prl.model.rules.ForbiddenFeatureRule
import com.booleworks.prl.model.rules.GroupRule
import com.booleworks.prl.model.rules.GroupType
import com.booleworks.prl.model.rules.IfThenElseRule
import com.booleworks.prl.model.rules.InclusionRule
import com.booleworks.prl.model.rules.MandatoryFeatureRule
import com.booleworks.prl.parser.parseRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CompilerRulesTest {

    @Test
    fun testConstraintRule() {
        val rule = PrlCompiler().compileRule(parseRule("rule (b1 & b2) <=> [i1 < 7]"), module, featureMap)
        assertThat(rule).isEqualTo(ConstraintRule(equiv(and(b1, b2), intLt(i1, 7)), module))

        PrlCompiler().let {
            assertThat(it.compileRule(parseRule("rule (b1 & b2) <=> [e1 < 7]"), module, featureMap)).isNull()
            assertThat(it.errors()).containsExactly("Enum comparison must compare an enum feature with an enum value")
        }
    }

    @Test
    fun testDefinitionRule() {
        val rule = PrlCompiler().compileRule(parseRule("""rule b1 is b2 => [e1 = "text"]"""), module, featureMap)
        assertThat(rule).isEqualTo(DefinitionRule(b1, impl(b2, enumEq(e1, "text")), module))

        PrlCompiler().let {
            assertThat(it.compileRule(parseRule("""rule v is b2 => [e1 = "text"]"""), module, featureMap)).isNull()
            assertThat(it.errors()).containsExactly("Feature 'v' is a versioned feature")
        }
        PrlCompiler().let {
            assertThat(it.compileRule(parseRule("""rule i1 is b2 => [e1 = "text"]"""), module, featureMap)).isNull()
            assertThat(it.errors()).containsExactly("Int feature 'i1' is used as boolean feature")
        }
        PrlCompiler().let {
            assertThat(it.compileRule(parseRule("""rule b1 is x => [e1 = "text"]"""), module, featureMap)).isNull()
            assertThat(it.errors()).containsExactly("Unknown feature: 'x'")
        }
    }

    @Test
    fun testExclusionRule() {
        val rule = PrlCompiler().compileRule(parseRule("""rule if b1 thenNot [i1 in [1-10]]"""), module, featureMap)
        assertThat(rule).isEqualTo(ExclusionRule(b1, intIn(i1, IntRange.interval(1, 10)), module))

        PrlCompiler().let {
            assertThat(it.compileRule(parseRule("""rule if x thenNot [i1 in [1-10]]"""), module, featureMap)).isNull()
            assertThat(it.errors()).containsExactly("Unknown feature: 'x'")
        }
        PrlCompiler().let {
            assertThat(it.compileRule(parseRule("""rule if b1 thenNot [i5 in [1-10]]"""), module, featureMap)).isNull()
            assertThat(it.errors()).containsExactly("Unknown feature: 'i5'")
        }
    }

    @Test
    fun testForbiddenFeatureRule() {
        val rule1 = PrlCompiler().compileRule(parseRule("""rule forbidden feature b1"""), module, featureMap)
        assertThat(rule1).isEqualTo(ForbiddenFeatureRule(b1, module))
        val rule2 = PrlCompiler().compileRule(parseRule("""rule forbidden feature v[=2]"""), module, featureMap)
        assertThat(rule2).isEqualTo(ForbiddenFeatureRule(v, 2, module))
        val rule3 = PrlCompiler().compileRule(parseRule("""rule forbidden feature e1 = "text""""), module, featureMap)
        assertThat(rule3).isEqualTo(ForbiddenFeatureRule(e1, "text", module))
        val rule4 = PrlCompiler().compileRule(parseRule("""rule forbidden feature i1 = 8"""), module, featureMap)
        assertThat(rule4).isEqualTo(ForbiddenFeatureRule(i1, 8, module))

        PrlCompiler().let {
            assertThat(it.compileRule(parseRule("""rule forbidden feature b1 = 2"""), module, featureMap)).isNull()
            assertThat(it.errors()).containsExactly("Cannot assign an unversioned boolean feature to an int or enum value")
        }
        PrlCompiler().let {
            assertThat(it.compileRule(parseRule("""rule forbidden feature b1 = "text" """), module, featureMap)).isNull()
            assertThat(it.errors()).containsExactly("Cannot assign an unversioned boolean feature to an int or enum value")
        }
        PrlCompiler().let {
            assertThat(it.compileRule(parseRule("""rule forbidden feature v"""), module, featureMap)).isNull()
            assertThat(it.errors()).containsExactly("Cannot assign a versioned boolean feature to anything else than an int version")
        }
        PrlCompiler().let {
            assertThat(it.compileRule(parseRule("""rule forbidden feature e1 = 7"""), module, featureMap)).isNull()
            assertThat(it.errors()).containsExactly("Cannot assign an enum feature to anything else than an enum value")
        }
        PrlCompiler().let {
            assertThat(it.compileRule(parseRule("""rule forbidden feature e1"""), module, featureMap)).isNull()
            assertThat(it.errors()).containsExactly("Cannot assign an enum feature to anything else than an enum value")
        }
        PrlCompiler().let {
            assertThat(it.compileRule(parseRule("""rule forbidden feature i1 = "text" """), module, featureMap)).isNull()
            assertThat(it.errors()).containsExactly("Cannot assign an int feature to anything else than an int value")
        }
        PrlCompiler().let {
            assertThat(it.compileRule(parseRule("""rule forbidden feature i1"""), module, featureMap)).isNull()
            assertThat(it.errors()).containsExactly("Cannot assign an int feature to anything else than an int value")
        }
    }

    @Test
    fun testGroupRule() {
        val rule1 = PrlCompiler().compileRule(parseRule("""optional group b1 contains [b2, b3]"""), module, featureMap)
        assertThat(rule1).isEqualTo(GroupRule(GroupType.OPTIONAL, b1, setOf(b2, b3), Visibility.PUBLIC, module))
        val rule2 = PrlCompiler().compileRule(parseRule("""mandatory internal group b1 contains [b2, b3]"""), module, featureMap)
        assertThat(rule2).isEqualTo(GroupRule(GroupType.MANDATORY, b1, setOf(b2, b3), Visibility.INTERNAL, module))

        PrlCompiler().let {
            assertThat(it.compileRule(parseRule("""optional group v contains [b2, b3]"""), module, featureMap)).isNull()
            assertThat(it.errors()).containsExactly("Feature 'v' is a versioned feature")
        }
        PrlCompiler().let {
            assertThat(it.compileRule(parseRule("""optional group b1 contains [b2, b3, e1]"""), module, featureMap)).isNull()
            assertThat(it.errors()).containsExactly("Enum feature 'e1' is used as boolean feature")
        }
    }

    @Test
    fun testIfThenElseRule() {
        val rule1 = PrlCompiler().compileRule(parseRule("""rule if b1 then b2 else [i1 <= -10]"""), module, featureMap)
        assertThat(rule1).isEqualTo(IfThenElseRule(b1, b2, intLe(i1, -10), module))

        PrlCompiler().let {
            assertThat(it.compileRule(parseRule("""rule if y then b2 else [i1 <= -10]"""), module, featureMap)).isNull()
            assertThat(it.errors()).containsExactly("Unknown feature: 'y'")
        }
        PrlCompiler().let {
            assertThat(it.compileRule(parseRule("""rule if b1 then y else [i1 <= -10]"""), module, featureMap)).isNull()
            assertThat(it.errors()).containsExactly("Unknown feature: 'y'")
        }
        PrlCompiler().let {
            assertThat(it.compileRule(parseRule("""rule if b1 then b2 else [e1 <= -10]"""), module, featureMap)).isNull()
            assertThat(it.errors()).containsExactly("Enum comparison must compare an enum feature with an enum value")
        }
    }

    @Test
    fun testInclusionRule() {
        val rule = PrlCompiler().compileRule(parseRule("""rule if b1 then [i1 in [1-10]]"""), module, featureMap)
        assertThat(rule).isEqualTo(InclusionRule(b1, intIn(i1, IntRange.interval(1, 10)), module))

        PrlCompiler().let {
            assertThat(it.compileRule(parseRule("""rule if x then [i1 in [1-10]]"""), module, featureMap)).isNull()
            assertThat(it.errors()).containsExactly("Unknown feature: 'x'")
        }
        PrlCompiler().let {
            assertThat(it.compileRule(parseRule("""rule if b1 then [i5 in [1-10]]"""), module, featureMap)).isNull()
            assertThat(it.errors()).containsExactly("Unknown feature: 'i5'")
        }
    }

    @Test
    fun testMandatoryFeatureRule() {
        val rule1 = PrlCompiler().compileRule(parseRule("""rule mandatory feature b1"""), module, featureMap)
        assertThat(rule1).isEqualTo(MandatoryFeatureRule(b1, module))
        val rule2 = PrlCompiler().compileRule(parseRule("""rule mandatory feature v[=2]"""), module, featureMap)
        assertThat(rule2).isEqualTo(MandatoryFeatureRule(v, 2, module))
        val rule3 = PrlCompiler().compileRule(parseRule("""rule mandatory feature e1 = "text""""), module, featureMap)
        assertThat(rule3).isEqualTo(MandatoryFeatureRule(e1, "text", module))
        val rule4 = PrlCompiler().compileRule(parseRule("""rule mandatory feature i1 = 8"""), module, featureMap)
        assertThat(rule4).isEqualTo(MandatoryFeatureRule(i1, 8, module))

        PrlCompiler().let {
            assertThat(it.compileRule(parseRule("""rule mandatory feature b1 = 2"""), module, featureMap)).isNull()
            assertThat(it.errors()).containsExactly("Cannot assign an unversioned boolean feature to an int or enum value")
        }
        PrlCompiler().let {
            assertThat(it.compileRule(parseRule("""rule mandatory feature b1 = "text" """), module, featureMap)).isNull()
            assertThat(it.errors()).containsExactly("Cannot assign an unversioned boolean feature to an int or enum value")
        }
        PrlCompiler().let {
            assertThat(it.compileRule(parseRule("""rule mandatory feature v"""), module, featureMap)).isNull()
            assertThat(it.errors()).containsExactly("Cannot assign a versioned boolean feature to anything else than an int version")
        }
        PrlCompiler().let {
            assertThat(it.compileRule(parseRule("""rule mandatory feature e1 = 7"""), module, featureMap)).isNull()
            assertThat(it.errors()).containsExactly("Cannot assign an enum feature to anything else than an enum value")
        }
        PrlCompiler().let {
            assertThat(it.compileRule(parseRule("""rule mandatory feature e1"""), module, featureMap)).isNull()
            assertThat(it.errors()).containsExactly("Cannot assign an enum feature to anything else than an enum value")
        }
        PrlCompiler().let {
            assertThat(it.compileRule(parseRule("""rule mandatory feature i1 = "text" """), module, featureMap)).isNull()
            assertThat(it.errors()).containsExactly("Cannot assign an int feature to anything else than an int value")
        }
        PrlCompiler().let {
            assertThat(it.compileRule(parseRule("""rule mandatory feature i1"""), module, featureMap)).isNull()
            assertThat(it.errors()).containsExactly("Cannot assign an int feature to anything else than an int value")
        }
    }
}
