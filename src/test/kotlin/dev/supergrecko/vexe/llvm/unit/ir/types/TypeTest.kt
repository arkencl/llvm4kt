package dev.supergrecko.vexe.llvm.unit.ir.types

import dev.supergrecko.vexe.llvm.ir.Context
import dev.supergrecko.vexe.llvm.ir.types.IntType
import dev.supergrecko.vexe.test.TestSuite
import kotlin.test.assertEquals

internal class TypeTest : TestSuite({
    describe("Casting into the same type will work") {
        val type = IntType(32)
        val ptr = type.toPointerType()
        val underlying = ptr.getElementType()

        assertEquals(type.ref, IntType(underlying.ref).ref)
    }

    describe("The context the type was made in is retrievable") {
        val ctx = Context()
        val type = IntType(32, ctx)

        val typeCtx = type.getContext()

        assertEquals(ctx.ref, typeCtx.ref)
    }

    describe("The name of the type can be retrieved") {
        val type = IntType(32)

        val msg = type.getStringRepresentation()

        // LLVM does apparently not retain bit size for integer types here
        assertEquals("i", msg.getString())

        msg.dispose()
    }
}
)
