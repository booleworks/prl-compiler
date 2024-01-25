package com.booleworks.prl.transpiler;

import com.booleworks.logicng.datastructures.Tristate
import com.booleworks.logicng.formulas.FormulaFactory
import com.booleworks.logicng.solvers.MiniSat
import com.booleworks.prl.compiler.PrlCompiler
import com.booleworks.prl.parser.parseRuleFile
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.jupiter.api.Test

class LogicNGTranspilerIntNoSlicesTest {
    private val model = PrlCompiler().compile(parseRuleFile("test-files/prl/compiler/simple_int.prl"))
    private val f = FormulaFactory.caching()
    private val modelTranslation = transpileModel(f, model, listOf())

    @Test
    fun testModel() {
        assertThat(model.rules).hasSize(12)
        assertThat(modelTranslation.numberOfComputations).isEqualTo(1)
    }

    @Test
    fun testIntMapping() {
        val trans = modelTranslation[0]
        assertThat(trans.intMapping).hasSize(4)
        assertThat(trans.intMapping["ints.i1"]).containsExactly(
            entry(0, f.variable("@INT_ints#i1_0")),
            entry(10, f.variable("@INT_ints#i1_10")),
            entry(20, f.variable("@INT_ints#i1_20")),
            entry(50, f.variable("@INT_ints#i1_50")),
            entry(100, f.variable("@INT_ints#i1_100")),
        )
        assertThat(trans.intMapping["ints.i2"]).containsExactly(
            entry(0, f.variable("@INT_ints#i2_0")),
            entry(2, f.variable("@INT_ints#i2_2")),
            entry(4, f.variable("@INT_ints#i2_4")),
            entry(6, f.variable("@INT_ints#i2_6")),
            entry(8, f.variable("@INT_ints#i2_8")),
            entry(10, f.variable("@INT_ints#i2_10")),
            entry(12, f.variable("@INT_ints#i2_12")),
            entry(14, f.variable("@INT_ints#i2_14")),
            entry(16, f.variable("@INT_ints#i2_16")),
            entry(18, f.variable("@INT_ints#i2_18")),
            entry(20, f.variable("@INT_ints#i2_20")),
            entry(30, f.variable("@INT_ints#i2_30")),
        )
        assertThat(trans.intMapping["ints.i3"]).containsExactly(
            entry(-100, f.variable("@INT_ints#i3_m100")),
            entry(-40, f.variable("@INT_ints#i3_m40")),
            entry(-20, f.variable("@INT_ints#i3_m20")),
            entry(0, f.variable("@INT_ints#i3_0")),
            entry(10, f.variable("@INT_ints#i3_10")),
            entry(20, f.variable("@INT_ints#i3_20")),
            entry(40, f.variable("@INT_ints#i3_40")),
            entry(100, f.variable("@INT_ints#i3_100")),
        )
        assertThat(trans.intMapping["ints.i4"]).containsExactly(
            entry(-100, f.variable("@INT_ints#i4_m100")),
            entry(-40, f.variable("@INT_ints#i4_m40")),
            entry(-20, f.variable("@INT_ints#i4_m20")),
            entry(0, f.variable("@INT_ints#i4_0")),
            entry(10, f.variable("@INT_ints#i4_10")),
            entry(20, f.variable("@INT_ints#i4_20")),
            entry(40, f.variable("@INT_ints#i4_40")),
            entry(100, f.variable("@INT_ints#i4_100")),
        )
        assertThat(trans.intVariables).hasSize(33)
    }

    @Test
    fun testIntConstraints() {
        val rules = model.rules
        val trans = modelTranslation[0]
        val sliceSet = trans.sliceSet
        val props = trans.propositions
        assertThat(props).hasSize(12 + 4 + 5)
        assertThat(props).contains(
            PrlProposition(
                RuleInformation(rules[0], sliceSet),
                f.parse("@INT_ints#i1_0 | @INT_ints#i1_10 | @INT_ints#i1_20")
            )
        )
        assertThat(props).contains(
            PrlProposition(
                RuleInformation(rules[1], sliceSet),
                f.parse("@INT_ints#i1_20 => @INT_ints#i2_0")
            )
        )
        assertThat(props).contains(
            PrlProposition(
                RuleInformation(rules[2], sliceSet),
                f.parse("@INT_ints#i1_10 => @INT_ints#i2_10")
            )
        )
        assertThat(props).contains(
            PrlProposition(
                RuleInformation(rules[3], sliceSet),
                f.parse("~@INT_ints#i1_0")
            )
        )
        assertThat(props).contains(
            PrlProposition(
                RuleInformation(rules[4], sliceSet),
                f.parse(
                    "ints.b1 <=> (@INT_ints#i3_0 | @INT_ints#i3_10 | @INT_ints#i3_20) & " +
                            "(@INT_ints#i4_0 | @INT_ints#i4_10 | @INT_ints#i4_20 | @INT_ints#i4_40 | @INT_ints#i4_100)",
                )
            )
        )
        assertThat(props).contains(
            PrlProposition(
                RuleInformation(rules[5], sliceSet),
                f.parse(
                    "ints.b2 <=> @INT_ints#i3_m20 | @INT_ints#i4_m20 | @INT_ints#i4_m40 | @INT_ints#i4_m100",
                )
            )
        )
        assertThat(props).contains(
            PrlProposition(
                RuleInformation(rules[6], sliceSet),
                f.parse(
                    "ints.b1 => ~(@INT_ints#i3_0 | @INT_ints#i3_10 | @INT_ints#i3_20)",
                )
            )
        )
        assertThat(props).contains(
            PrlProposition(
                RuleInformation(rules[7], sliceSet),
                f.parse(
                    "ints.b2 & (@INT_ints#i4_20 | @INT_ints#i4_40) | " +
                            "~ints.b2 & (@INT_ints#i4_m40 | @INT_ints#i4_m20 | @INT_ints#i4_0)",
                )
            )
        )
        assertThat(props).contains(
            PrlProposition(
                RuleInformation(rules[8], sliceSet),
                f.parse(
                    " @INT_ints#i3_m20 & @INT_ints#i4_m100 | @INT_ints#i3_m20 & @INT_ints#i4_m40 | " +
                            "@INT_ints#i3_m20 & @INT_ints#i4_m20 | @INT_ints#i3_0 & @INT_ints#i4_m100 | " +
                            "@INT_ints#i3_0 & @INT_ints#i4_m40 | @INT_ints#i3_0 & @INT_ints#i4_m20 | " +
                            "@INT_ints#i3_0 & @INT_ints#i4_0 | @INT_ints#i3_10 & @INT_ints#i4_m100 | " +
                            "@INT_ints#i3_10 & @INT_ints#i4_m40 | @INT_ints#i3_10 & @INT_ints#i4_m20 | " +
                            "@INT_ints#i3_10 & @INT_ints#i4_0 | @INT_ints#i3_10 & @INT_ints#i4_10 | " +
                            "@INT_ints#i3_20 & @INT_ints#i4_m100 | @INT_ints#i3_20 & @INT_ints#i4_m40 | " +
                            "@INT_ints#i3_20 & @INT_ints#i4_m20 | @INT_ints#i3_20 & @INT_ints#i4_0 | " +
                            "@INT_ints#i3_20 & @INT_ints#i4_10 | @INT_ints#i3_20 & @INT_ints#i4_20 => ints.b1 ",
                )
            )
        )
        assertThat(props).contains(
            PrlProposition(
                RuleInformation(rules[9], sliceSet),
                f.parse(
                    "@INT_ints#i1_0 | @INT_ints#i1_10 | @INT_ints#i1_50 | @INT_ints#i1_100",
                )
            )
        )
        assertThat(props).contains(PrlProposition(RuleInformation(rules[10], sliceSet), f.parse("\$true")))
        assertThat(props).contains(PrlProposition(RuleInformation(rules[11], sliceSet), f.parse("\$true")))
    }

    @Test
    fun testIntMetaPropositionsExo() {
        val trans = modelTranslation[0]
        val sliceSet = trans.sliceSet
        val props = trans.propositions
        assertThat(props).contains(
            PrlProposition(
                RuleInformation(RuleType.INT_FEATURE_CONSTRAINT, sliceSet),
                f.parse("@INT_ints#i1_0 + @INT_ints#i1_10 + @INT_ints#i1_20 + @INT_ints#i1_50 + @INT_ints#i1_100 = 1")
            )
        )
        assertThat(props).contains(
            PrlProposition(
                RuleInformation(RuleType.INT_FEATURE_CONSTRAINT, sliceSet),
                f.parse(
                    "@INT_ints#i2_0 + @INT_ints#i2_2 + @INT_ints#i2_4 + @INT_ints#i2_6 + @INT_ints#i2_8 + " +
                            "@INT_ints#i2_10 + @INT_ints#i2_12 + @INT_ints#i2_14 + @INT_ints#i2_16 + " +
                            "@INT_ints#i2_18 + @INT_ints#i2_20 = 1"
                )
            )
        )
        assertThat(props).contains(
            PrlProposition(
                RuleInformation(RuleType.INT_FEATURE_CONSTRAINT, sliceSet),
                f.parse("@INT_ints#i3_m20 + @INT_ints#i3_0 + @INT_ints#i3_10 + @INT_ints#i3_20 = 1")
            )
        )
        assertThat(props).contains(
            PrlProposition(
                RuleInformation(RuleType.INT_FEATURE_CONSTRAINT, sliceSet),
                f.parse(
                    "@INT_ints#i4_m100 + @INT_ints#i4_m40 + @INT_ints#i4_m20 + @INT_ints#i4_0 +" +
                            "@INT_ints#i4_10 + @INT_ints#i4_20 + @INT_ints#i4_40 + @INT_ints#i4_100 = 1"
                )
            )
        )
    }

    @Test
    fun testIntMetaPropositionsExclusions() {
        val trans = modelTranslation[0]
        val sliceSet = trans.sliceSet
        val props = trans.propositions
        assertThat(props).contains(
            PrlProposition(
                RuleInformation(RuleType.INT_OUT_OF_BOUNDS_CONSTRAINT, sliceSet),
                f.parse("~@INT_ints#i2_30")
            )
        )
        assertThat(props).contains(
            PrlProposition(
                RuleInformation(RuleType.INT_OUT_OF_BOUNDS_CONSTRAINT, sliceSet),
                f.parse("~@INT_ints#i3_m100")
            )
        )
        assertThat(props).contains(
            PrlProposition(
                RuleInformation(RuleType.INT_OUT_OF_BOUNDS_CONSTRAINT, sliceSet),
                f.parse("~@INT_ints#i3_m40")
            )
        )
        assertThat(props).contains(
            PrlProposition(
                RuleInformation(RuleType.INT_OUT_OF_BOUNDS_CONSTRAINT, sliceSet),
                f.parse("~@INT_ints#i3_40")
            )
        )
        assertThat(props).contains(
            PrlProposition(
                RuleInformation(RuleType.INT_OUT_OF_BOUNDS_CONSTRAINT, sliceSet),
                f.parse("~@INT_ints#i3_100")
            )
        )
    }

    @Test
    fun testIntSolving() {
        val trans = modelTranslation[0]
        val solver = MiniSat.miniSat(f)
        solver.addPropositions(trans.propositions)
        assertThat(solver.sat()).isEqualTo(Tristate.TRUE)
        val models = solver.enumerateAllModels(trans.intVariables)
        models.forEach { println(it.positiveVariables()) }
        assertThat(models).hasSize(2)
    }
}

