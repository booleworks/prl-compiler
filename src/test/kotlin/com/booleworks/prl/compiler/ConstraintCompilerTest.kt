package com.booleworks.prl.compiler

import com.booleworks.prl.model.IntRange
import com.booleworks.prl.model.constraints.ComparisonOperator.EQ
import com.booleworks.prl.model.constraints.FALSE
import com.booleworks.prl.model.constraints.TRUE
import com.booleworks.prl.model.constraints.amo
import com.booleworks.prl.model.constraints.and
import com.booleworks.prl.model.constraints.enumEq
import com.booleworks.prl.model.constraints.enumIn
import com.booleworks.prl.model.constraints.enumNe
import com.booleworks.prl.model.constraints.equiv
import com.booleworks.prl.model.constraints.exo
import com.booleworks.prl.model.constraints.impl
import com.booleworks.prl.model.constraints.intEq
import com.booleworks.prl.model.constraints.intGt
import com.booleworks.prl.model.constraints.intIn
import com.booleworks.prl.model.constraints.intMul
import com.booleworks.prl.model.constraints.intSum
import com.booleworks.prl.model.constraints.intVal
import com.booleworks.prl.model.constraints.not
import com.booleworks.prl.model.constraints.or
import com.booleworks.prl.model.constraints.toIntValue
import com.booleworks.prl.model.constraints.versionEq
import com.booleworks.prl.model.constraints.versionGe
import com.booleworks.prl.parser.PrlAmo
import com.booleworks.prl.parser.PrlComparisonPredicate
import com.booleworks.prl.parser.PrlConstant
import com.booleworks.prl.parser.PrlExo
import com.booleworks.prl.parser.PrlFeature
import com.booleworks.prl.parser.PrlIntAddFunction
import com.booleworks.prl.parser.PrlIntMulFunction
import com.booleworks.prl.parser.PrlIntValue
import com.booleworks.prl.parser.parseConstraint
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class ConstraintCompilerTest {
    val cc = ConstraintCompiler()

    @Test
    fun testCompileConstant() {
        assertThat(cc.compileConstraint(PrlConstant(true), featureMap)).isEqualTo(TRUE)
        assertThat(cc.compileConstraint(PrlConstant(false), featureMap)).isEqualTo(FALSE)
    }

    @Test
    fun testCompileBooleanFeature() {
        assertThat(cc.compileConstraint(PrlFeature("b1"), featureMap)).isEqualTo(b1Definition.feature)
        assertThat(cc.compileConstraint(PrlFeature("v"), featureMap)).isEqualTo(vDefinition.feature)
        assertThatThrownBy { cc.compileConstraint(PrlFeature("x"), featureMap) }
            .isInstanceOf(CoCoException::class.java)
            .hasMessage("Unknown feature: 'x'")
        assertThatThrownBy { cc.compileConstraint(PrlFeature("e1"), featureMap) }
            .isInstanceOf(CoCoException::class.java)
            .hasMessage("Enum feature 'e1' is used as boolean feature")
        assertThatThrownBy { cc.compileConstraint(PrlFeature("i1"), featureMap) }
            .isInstanceOf(CoCoException::class.java)
            .hasMessage("Int feature 'i1' is used as boolean feature")
    }

    @Test
    fun testCompileAmo() {
        assertThat(cc.compileConstraint(PrlAmo(), featureMap)).isEqualTo(amo())
        assertThat(cc.compileConstraint(PrlAmo(PrlFeature("b1")), featureMap)).isEqualTo(amo(b1))
        assertThat(
            cc.compileConstraint(
                PrlAmo(PrlFeature("b1"), PrlFeature("b2"), PrlFeature("b3")),
                featureMap
            )
        ).isEqualTo(amo(b1, b2, b3))
        assertThatThrownBy { cc.compileConstraint(PrlAmo(PrlFeature("b1"), PrlFeature("x")), featureMap) }
            .isInstanceOf(CoCoException::class.java)
            .hasMessage("Unknown feature: 'x'")
        assertThatThrownBy { cc.compileConstraint(PrlAmo(PrlFeature("b1"), PrlFeature("e1")), featureMap) }
            .isInstanceOf(CoCoException::class.java)
            .hasMessage("Enum feature 'e1' is used as boolean feature")
        assertThatThrownBy { cc.compileConstraint(PrlAmo(PrlFeature("b1"), PrlFeature("i1")), featureMap) }
            .isInstanceOf(CoCoException::class.java)
            .hasMessage("Int feature 'i1' is used as boolean feature")
        assertThatThrownBy { cc.compileConstraint(PrlAmo(PrlFeature("b1"), PrlFeature("v")), featureMap) }
            .isInstanceOf(CoCoException::class.java)
            .hasMessage("Versioned boolean feature 'v' is used as boolean feature")
    }

    @Test
    fun testCompileExo() {
        assertThat(cc.compileConstraint(PrlExo(), featureMap)).isEqualTo(exo())
        assertThat(cc.compileConstraint(PrlExo(PrlFeature("b1")), featureMap)).isEqualTo(exo(b1))
        assertThat(
            cc.compileConstraint(
                PrlExo(PrlFeature("b1"), PrlFeature("b2"), PrlFeature("b3")),
                featureMap
            )
        ).isEqualTo(exo(b1, b2, b3))
        assertThatThrownBy { cc.compileConstraint(PrlExo(PrlFeature("b1"), PrlFeature("x")), featureMap) }
            .isInstanceOf(CoCoException::class.java)
            .hasMessage("Unknown feature: 'x'")
        assertThatThrownBy { cc.compileConstraint(PrlExo(PrlFeature("b1"), PrlFeature("e1")), featureMap) }
            .isInstanceOf(CoCoException::class.java)
            .hasMessage("Enum feature 'e1' is used as boolean feature")
        assertThatThrownBy { cc.compileConstraint(PrlExo(PrlFeature("b1"), PrlFeature("i1")), featureMap) }
            .isInstanceOf(CoCoException::class.java)
            .hasMessage("Int feature 'i1' is used as boolean feature")
        assertThatThrownBy { cc.compileConstraint(PrlExo(PrlFeature("b1"), PrlFeature("v")), featureMap) }
            .isInstanceOf(CoCoException::class.java)
            .hasMessage("Versioned boolean feature 'v' is used as boolean feature")
    }

    @Test
    fun testImplication() {
        assertThat(cc.compileConstraint(parseConstraint("b1 => b2/b3"), featureMap)).isEqualTo(impl(b1, or(b2, b3)))
        assertThat(cc.compileConstraint(parseConstraint("-(b1&b2) => true"), featureMap)).isEqualTo(
            impl(
                not(
                    and(
                        b1,
                        b2
                    )
                ), TRUE
            )
        )
        assertThatThrownBy { cc.compileConstraint(parseConstraint("b1 => x"), featureMap) }
            .isInstanceOf(CoCoException::class.java)
            .hasMessage("Unknown feature: 'x'")
        assertThatThrownBy { cc.compileConstraint(parseConstraint("x => b2"), featureMap) }
            .isInstanceOf(CoCoException::class.java)
            .hasMessage("Unknown feature: 'x'")
    }

    @Test
    fun testEquivalence() {
        assertThat(cc.compileConstraint(parseConstraint("b1 <=> b2/b3"), featureMap)).isEqualTo(equiv(b1, or(b2, b3)))
        assertThat(cc.compileConstraint(parseConstraint("-(b1&b2) <=> true"), featureMap)).isEqualTo(
            equiv(
                not(
                    and(
                        b1,
                        b2
                    )
                ), TRUE
            )
        )
        assertThatThrownBy { cc.compileConstraint(parseConstraint("b1 <=> x"), featureMap) }
            .isInstanceOf(CoCoException::class.java)
            .hasMessage("Unknown feature: 'x'")
        assertThatThrownBy { cc.compileConstraint(parseConstraint("x <=> b2"), featureMap) }
            .isInstanceOf(CoCoException::class.java)
            .hasMessage("Unknown feature: 'x'")
    }

    @Test
    fun testAnd() {
        assertThat(cc.compileConstraint(parseConstraint("b1 & b2"), featureMap)).isEqualTo(and(b1, b2))
        assertThat(cc.compileConstraint(parseConstraint("b1 & (b2/b3) & -b2"), featureMap)).isEqualTo(
            and(
                b1,
                or(b2, b3),
                not(b2)
            )
        )
        assertThatThrownBy { cc.compileConstraint(parseConstraint("true & x"), featureMap) }
            .isInstanceOf(CoCoException::class.java)
            .hasMessage("Unknown feature: 'x'")
    }

    @Test
    fun testOr() {
        assertThat(cc.compileConstraint(parseConstraint("b1 / b2"), featureMap)).isEqualTo(or(b1, b2))
        assertThat(cc.compileConstraint(parseConstraint("b1 / b2 & b3 / -b2"), featureMap)).isEqualTo(
            or(
                b1,
                and(b2, b3),
                not(b2)
            )
        )
        assertThatThrownBy { cc.compileConstraint(parseConstraint("true / x"), featureMap) }
            .isInstanceOf(CoCoException::class.java)
            .hasMessage("Unknown feature: 'x'")
    }

    @Test
    fun testNot() {
        assertThat(cc.compileConstraint(parseConstraint("-(b1 / b2)"), featureMap)).isEqualTo(not(or(b1, b2)))
        assertThat(cc.compileConstraint(parseConstraint("-(b1 / b2 & b3 / -b2)"), featureMap)).isEqualTo(
            not(
                or(
                    b1,
                    and(b2, b3),
                    not(b2)
                )
            )
        )
        assertThatThrownBy { cc.compileConstraint(parseConstraint("-x"), featureMap) }
            .isInstanceOf(CoCoException::class.java)
            .hasMessage("Unknown feature: 'x'")
    }

    @Test
    fun testEnumInPredicate() {
        assertThat(cc.compileConstraint(parseConstraint("""[e1 in ["a"]]"""), featureMap)).isEqualTo(
            enumIn(
                e1,
                listOf("a")
            )
        )
        assertThat(
            cc.compileConstraint(
                parseConstraint("""[e1 in ["a", "b", "c"]]"""),
                featureMap
            )
        ).isEqualTo(enumIn(e1, listOf("a", "b", "c")))
        assertThatThrownBy { cc.compileConstraint(parseConstraint("""[x in ["a", "b"]]"""), featureMap) }
            .isInstanceOf(CoCoException::class.java)
            .hasMessage("Unknown feature: 'x'")
        assertThatThrownBy { cc.compileConstraint(parseConstraint("""[b1 in ["a", "b"]]"""), featureMap) }
            .isInstanceOf(CoCoException::class.java)
            .hasMessage("Boolean feature 'b1' is used as enum feature")
    }

    @Test
    fun testIntValue() {
        assertThat(cc.compileIntTerm(PrlIntValue(3), featureMap)).isEqualTo(intVal(3))
    }

    @Test
    fun testIntFeature() {
        assertThat(cc.compileIntTerm(PrlFeature("i1"), featureMap)).isEqualTo(i1)
        assertThatThrownBy { cc.compileIntTerm(PrlFeature("x"), featureMap) }
            .isInstanceOf(CoCoException::class.java)
            .hasMessage("Unknown feature: 'x'")
        assertThatThrownBy { cc.compileIntTerm(PrlFeature("b1"), featureMap) }
            .isInstanceOf(CoCoException::class.java)
            .hasMessage("Boolean feature 'b1' is used as int feature")
    }

    @Test
    fun testIntMul() {
        assertThat(cc.compileIntTerm(PrlIntMulFunction(PrlFeature("i1"), PrlIntValue(3)), featureMap)).isEqualTo(
            intMul(
                3,
                i1
            )
        )
        assertThat(
            cc.compileIntTerm(
                PrlIntMulFunction(PrlIntValue(-3), PrlFeature("i1")),
                featureMap
            )
        ).isEqualTo(intMul(-3, i1))
        assertThatThrownBy { cc.compileIntTerm(PrlIntMulFunction(PrlIntValue(-3), PrlIntValue(3)), featureMap) }
            .isInstanceOf(CoCoException::class.java)
            .hasMessage("Integer multiplication is only allowed between a fixed coefficient and an integer feature")
        assertThatThrownBy {
            cc.compileIntTerm(
                PrlIntMulFunction(
                    PrlIntMulFunction(PrlIntValue(3), PrlFeature("i")),
                    PrlIntValue(3)
                ), featureMap
            )
        }
            .isInstanceOf(CoCoException::class.java)
            .hasMessage("Integer multiplication is only allowed between a fixed coefficient and an integer feature")
        assertThatThrownBy { cc.compileIntTerm(PrlIntMulFunction(PrlIntValue(-3), PrlFeature("x")), featureMap) }
            .isInstanceOf(CoCoException::class.java)
            .hasMessage("Unknown feature: 'x'")
        assertThatThrownBy { cc.compileIntTerm(PrlIntMulFunction(PrlIntValue(-3), PrlFeature("b1")), featureMap) }
            .isInstanceOf(CoCoException::class.java)
            .hasMessage("Boolean feature 'b1' is used as int feature")
    }

    @Test
    fun testIntSum() {
        val mul1 = PrlIntMulFunction(PrlFeature("i1"), PrlIntValue(3))
        val mul2 = PrlIntMulFunction(PrlFeature("i2"), PrlIntValue(2))
        val mul3 = PrlIntMulFunction(PrlFeature("i3"), PrlIntValue(-4))
        assertThat(cc.compileIntTerm(PrlIntAddFunction(PrlIntValue(13), mul1, mul2, mul3), featureMap))
            .isEqualTo(intSum(13, intMul(3, i1), intMul(2, i2), intMul(-4, i3)))
        assertThat(
            cc.compileIntTerm(
                PrlIntAddFunction(PrlIntValue(13), mul1, PrlIntAddFunction(mul2, mul3)),
                featureMap
            )
        )
            .isEqualTo(intSum(13, intMul(3, i1), intMul(2, i2), intMul(-4, i3)))
        assertThat(cc.compileIntTerm(PrlIntAddFunction(PrlIntValue(13), mul1, PrlIntValue(-12), mul3), featureMap))
            .isEqualTo(intSum(1, intMul(3, i1), intMul(-4, i3)))
        assertThatThrownBy {
            cc.compileIntTerm(
                PrlIntAddFunction(
                    PrlIntValue(13),
                    mul1,
                    PrlIntMulFunction(PrlIntValue(3), PrlFeature("x"))
                ), featureMap
            )
        }
            .isInstanceOf(CoCoException::class.java)
            .hasMessage("Unknown feature: 'x'")
        assertThatThrownBy {
            cc.compileIntTerm(
                PrlIntAddFunction(
                    PrlIntValue(13),
                    mul1,
                    PrlIntMulFunction(PrlIntValue(3), PrlFeature("b1"))
                ), featureMap
            )
        }
            .isInstanceOf(CoCoException::class.java)
            .hasMessage("Boolean feature 'b1' is used as int feature")
    }

    @Test
    fun testIntInPredicate() {
        assertThat(cc.compileConstraint(parseConstraint("[i1 in [-1 - 7]]"), featureMap)).isEqualTo(
            intIn(
                i1,
                IntRange.interval(-1, 7)
            )
        )
        assertThat(cc.compileConstraint(parseConstraint("[i2*3 in [-1 - 7]]"), featureMap)).isEqualTo(
            intIn(
                intMul(
                    3,
                    i2
                ), IntRange.interval(-1, 7)
            )
        )
        assertThat(cc.compileConstraint(parseConstraint("[i3*3 + 4 in [-1, 1, 10]]"), featureMap))
            .isEqualTo(intIn(intSum(4, intMul(3, i3)), IntRange.list(-1, 1, 10)))
        assertThatThrownBy { cc.compileConstraint(parseConstraint("[x in [1-5]]"), featureMap) }
            .isInstanceOf(CoCoException::class.java)
            .hasMessage("Unknown feature: 'x'")
        assertThatThrownBy { cc.compileConstraint(parseConstraint("[e1 in [1-5]]"), featureMap) }
            .isInstanceOf(CoCoException::class.java)
            .hasMessage("Enum feature 'e1' is used as int feature")
    }

    @Test
    fun testComparisonErrors() {
        assertThatThrownBy { cc.compileConstraint(parseConstraint("""[e1 = i1]"""), featureMap) }
            .isInstanceOf(CoCoException::class.java)
            .hasMessage("Cannot determine type of predicate, mixed features of type ENUM and INT")
        assertThatThrownBy { cc.compileConstraint(parseConstraint("""[i1 = "text"]"""), featureMap) }
            .isInstanceOf(CoCoException::class.java)
            .hasMessage("Unknown integer term type: PrlEnumValue")
        assertThatThrownBy { cc.compileConstraint(parseConstraint("""[e1 = 7]"""), featureMap) }
            .isInstanceOf(CoCoException::class.java)
            .hasMessage("Enum comparison must compare an enum feature with an enum value")
    }

    @Test
    fun testEnumComparisonPredicate() {
        assertThat(cc.compileConstraint(parseConstraint("""[e1 = "text"]"""), featureMap)).isEqualTo(enumEq(e1, "text"))
        assertThat(cc.compileConstraint(parseConstraint("""["text" != e2]"""), featureMap)).isEqualTo(
            enumNe(
                e2,
                "text"
            )
        )
        assertThatThrownBy { cc.compileConstraint(parseConstraint("""[e1 = e2]"""), featureMap) }
            .isInstanceOf(CoCoException::class.java)
            .hasMessage("Enum comparison must compare an enum feature with an enum value")
        assertThatThrownBy { cc.compileConstraint(parseConstraint("""["t" = "p"]"""), featureMap) }
            .isInstanceOf(CoCoException::class.java)
            .hasMessage("Enum comparison must compare an enum feature with an enum value")
        assertThatThrownBy { cc.compileConstraint(parseConstraint("""[e1 > "p"]"""), featureMap) }
            .isInstanceOf(CoCoException::class.java)
            .hasMessage("Only comparisons with = and != are allowed for enums")
    }

    @Test
    fun testIntComparisonPredicate() {
        assertThat(cc.compileConstraint(parseConstraint("[i1 = 7]"), featureMap)).isEqualTo(intEq(i1, 7))
        assertThat(cc.compileConstraint(parseConstraint("[7 = i1]"), featureMap)).isEqualTo(intEq(7.toIntValue(), i1))
        assertThat(cc.compileConstraint(parseConstraint("[7 = 7]"), featureMap)).isEqualTo(
            intEq(
                7.toIntValue(),
                7.toIntValue()
            )
        )
        assertThat(cc.compileConstraint(parseConstraint("[i3*3 + 47 > 8*i1 + -2*i2]"), featureMap))
            .isEqualTo(intGt(intSum(47, intMul(3, i3)), intSum(intMul(8, i1), intMul(-2, i2))))
        assertThat(cc.compileConstraint(parseConstraint("[i1 = i2]"), featureMap)).isEqualTo(intEq(i1, i2))
    }

    @Test
    fun testVersionPredicate() {
        assertThat(cc.compileConstraint(parseConstraint("v[>=7]"), featureMap)).isEqualTo(versionGe(v, 7))
        assertThat(
            cc.compileConstraint(
                PrlComparisonPredicate(EQ, PrlIntValue(4), PrlFeature("v")),
                featureMap
            )
        ).isEqualTo(versionEq(v, 4))
        assertThat(
            cc.compileConstraint(
                PrlComparisonPredicate(EQ, PrlIntValue(4), PrlFeature("v")),
                featureMap
            )
        ).isEqualTo(versionEq(v, 4))
        assertThatThrownBy {
            cc.compileConstraint(
                PrlComparisonPredicate(EQ, PrlIntValue(-4), PrlFeature("v")),
                featureMap
            )
        }
            .isInstanceOf(CoCoException::class.java)
            .hasMessage("Versions must be > 0")
        assertThatThrownBy {
            cc.compileConstraint(
                PrlComparisonPredicate(EQ, PrlFeature("v"), PrlFeature("v")),
                featureMap
            )
        }
            .isInstanceOf(CoCoException::class.java)
            .hasMessage("Version predicate must compare a versioned boolean feature with a fixed version")
        assertThatThrownBy { cc.compileConstraint(parseConstraint("[b1 >= 7]"), featureMap) }
            .isInstanceOf(CoCoException::class.java)
            .hasMessage("Unversioned feature in version predicate: b1")
    }

    @Test
    fun testLargeExample() {
        val formula =
            """b1 => ([e1 in ["a", "b", "c"]] / -b2) & -(v[>=7] <=> [3*i3 + 47 > 8*i1 + -2*i2] / [-2*i2 + 4 in [-1, 1, 10]])"""
        assertThat(cc.compileConstraint(parseConstraint(formula), featureMap).toString()).isEqualTo(
            formula
        )
    }
}
