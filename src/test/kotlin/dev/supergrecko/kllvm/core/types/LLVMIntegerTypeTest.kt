package dev.supergrecko.kllvm.core.types

import dev.supergrecko.kllvm.core.LLVMContext
import dev.supergrecko.kllvm.core.LLVMType
import dev.supergrecko.kllvm.utils.runAll
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LLVMIntegerTypeTest {
    @Test
    fun `global module values equate to module values`() {
        val ctx = LLVMContext.create()

        runAll(1, 8, 16, 32, 64, 128) {
            val contextType = ctx.integerType(LLVMTypeKind.Integer.LLVM_INT_TYPE, it)
            val globalType = LLVMType.makeInteger(LLVMTypeKind.Integer.LLVM_INT_TYPE, it)

            assertEquals(contextType.typeWidth(), globalType.typeWidth())
        }
    }

    @Test
    fun `it actually grabs types instead of null pointers`() {
        val ctx = LLVMContext.create()

        runAll(*LLVMTypeKind.Integer.values()) {
            val type = LLVMType.makeInteger(it, 1024, ctx.llvmCtx)

            assertTrue { !type.llvmType.isNull }
        }
    }
}