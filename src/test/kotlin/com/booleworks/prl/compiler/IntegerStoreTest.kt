package com.booleworks.prl.compiler

import com.booleworks.prl.model.IntRange
import com.booleworks.prl.model.constraints.intEq
import com.booleworks.prl.model.constraints.intFt
import com.booleworks.prl.model.constraints.intGt
import com.booleworks.prl.model.constraints.intIn
import com.booleworks.prl.model.constraints.intLe
import com.booleworks.prl.model.constraints.intMul
import com.booleworks.prl.model.constraints.intSum
import com.booleworks.prl.model.constraints.intVal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class IntegerStoreTest {
    private val x = intFt("x")
    private val y = intFt("y")
    private val z = intFt("z")
    private val p = intFt("p")
    private val q = intFt("q")

    @Test
    fun testInPredicate() {
        val store = IntegerStore()

        store.addUsage(intIn(x, IntRange.list(1, 2, 3)))
        store.addUsage(intIn(y, IntRange.interval(-3, 3)))
        assertThat(store.usedValues).hasSize(2)
        assertThat(store.usedValues[x]).isEqualTo(IntegerUsage(values = sortedSetOf(IntRange.list(1, 2, 3))))
        assertThat(store.usedValues[y]).isEqualTo(IntegerUsage(values = sortedSetOf(IntRange.interval(-3, 3))))

        store.addUsage(intIn(x, IntRange.list(4, 5, 6)))
        assertThat(store.usedValues).hasSize(2)
        assertThat(store.usedValues[x]).isEqualTo(
            IntegerUsage(
                values = sortedSetOf(
                    IntRange.list(1, 2, 3),
                    IntRange.list(4, 5, 6)
                )
            )
        )
        store.addUsage(intIn(x, IntRange.list(4, 5, 6)))
        assertThat(store.usedValues).hasSize(2)
        assertThat(store.usedValues[x]).isEqualTo(
            IntegerUsage(
                values = sortedSetOf(
                    IntRange.list(1, 2, 3),
                    IntRange.list(4, 5, 6)
                )
            )
        )

        store.addUsage(intIn(intVal(27), IntRange.list(4, 5, 6)))
        assertThat(store.usedValues).hasSize(2)

        store.addUsage(intIn(intMul(2, z), IntRange.list(4, 5, 6)))
        assertThat(store.usedValues).hasSize(3)
        assertThat(store.usedValues[z]).isEqualTo(IntegerUsage(usedInArEx = true))
    }

    @Test
    fun testCompPredicate() {
        val store = IntegerStore()

        store.addUsage(intEq(x, intVal(7)))
        assertThat(store.usedValues).hasSize(1)
        assertThat(store.usedValues[x]).isEqualTo(IntegerUsage(values = sortedSetOf(IntRange.list(7))))

        store.addUsage(intLe(intVal(3), x))
        assertThat(store.usedValues).hasSize(1)
        assertThat(store.usedValues[x]).isEqualTo(
            IntegerUsage(
                values = sortedSetOf(
                    IntRange.list(7),
                    IntRange.list(3)
                )
            )
        )

        store.addUsage(intLe(intVal(4), intMul(1, x)))
        assertThat(store.usedValues).hasSize(1)
        assertThat(store.usedValues[x]).isEqualTo(
            IntegerUsage(
                values = sortedSetOf(
                    IntRange.list(7),
                    IntRange.list(3),
                    IntRange.list(4)
                )
            )
        )

        store.addUsage(intLe(intMul(4, z), intMul(2, y)))
        assertThat(store.usedValues).hasSize(3)
        assertThat(store.usedValues[y]).isEqualTo(IntegerUsage(usedInArEx = true))
        assertThat(store.usedValues[z]).isEqualTo(IntegerUsage(usedInArEx = true))

        store.addUsage(intGt(intSum(intMul(1, p)), q))
        assertThat(store.usedValues).hasSize(5)
        assertThat(store.usedValues[p]).isEqualTo(IntegerUsage(otherFeatures = sortedSetOf(q)))
        assertThat(store.usedValues[q]).isEqualTo(IntegerUsage(otherFeatures = sortedSetOf(p)))
    }
}
