package dev.supergrecko.vexe.llvm.unit.ir.types

import dev.supergrecko.vexe.llvm.ir.TypeKind
import dev.supergrecko.vexe.llvm.ir.types.FloatType
import dev.supergrecko.vexe.llvm.ir.types.IntType
import dev.supergrecko.vexe.llvm.ir.types.VectorType
import dev.supergrecko.vexe.test.TestSuite
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class VectorTypeTest : TestSuite({
    describe("Creation from user-land constructor") {
        val type = IntType(32)
        val vec = type.toVectorType(1000)

        assertEquals(TypeKind.Vector, vec.getTypeKind())
        assertEquals(1000, vec.getElementCount())
    }

    describe("Creation via LLVM reference") {
        val type = IntType(16).toVectorType(10)
        val second = VectorType(type.ref)

        assertEquals(TypeKind.Vector, second.getTypeKind())
    }

    describe("The type of the elements match the vector type") {
        val type = IntType(32)
        val vec = VectorType(type, 10)

        assertEquals(10, vec.getElementCount())
        assertEquals(type.ref, vec.getElementType().ref)
    }

    describe("The subtypes match") {
        val type = IntType(32)
        val vec = VectorType(type, 10)

        assertEquals(10, vec.getSubtypes().size)
        assertEquals(type.ref, vec.getSubtypes().first().ref)
    }

    describe("Allocating a vector type with negative size fails") {
        val type = FloatType(TypeKind.Float)

        assertFailsWith<IllegalArgumentException> {
            type.toVectorType(-100)
        }

        assertFailsWith<IllegalArgumentException> {
            VectorType(type, -100)
        }
    }
}
)
