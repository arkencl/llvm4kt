package dev.supergrecko.vexe.llvm.ir.types

import dev.supergrecko.vexe.llvm.ir.Context
import dev.supergrecko.vexe.llvm.ir.Type
import dev.supergrecko.vexe.llvm.ir.values.constants.ConstantInt
import org.bytedeco.llvm.LLVM.LLVMTypeRef
import org.bytedeco.llvm.global.LLVM

public class IntType internal constructor() : Type() {
    public constructor(llvmRef: LLVMTypeRef) : this() {
        ref = llvmRef
    }

    //region Core::Types::Int
    /**
     * Create an integer types
     *
     * This will create an integer types of the size [size]. If the size matches
     * any of LLVM's preset integer sizes then that size will be returned.
     * Otherwise an arbitrary size int types will be returned.
     */
    public constructor(
        size: Int,
        ctx: Context = Context.getGlobalContext()
    ) : this() {
        ref = when (size) {
            1 -> LLVM.LLVMInt1TypeInContext(ctx.ref)
            8 -> LLVM.LLVMInt8TypeInContext(ctx.ref)
            16 -> LLVM.LLVMInt16TypeInContext(ctx.ref)
            32 -> LLVM.LLVMInt32TypeInContext(ctx.ref)
            64 -> LLVM.LLVMInt64TypeInContext(ctx.ref)
            128 -> LLVM.LLVMInt128TypeInContext(ctx.ref)
            else -> {
                require(size in 1..8388606) {
                    "LLVM only supports integers of 2^23-1 bits size"
                }

                LLVM.LLVMIntTypeInContext(ctx.ref, size)
            }
        }
    }

    /**
     * Get the size of the integer
     *
     * @see LLVM.LLVMGetIntTypeWidth
     */
    public fun getTypeWidth(): Int {
        return LLVM.LLVMGetIntTypeWidth(ref)
    }
    //endregion Core::Types::Int

    //region Core::Values::Constants
    /**
     * Get a constant all 1's for this integer type
     *
     * @see LLVM.LLVMConstAllOnes
     */
    public fun getConstantAllOnes(): ConstantInt {
        val v = LLVM.LLVMConstAllOnes(ref)

        return ConstantInt(v)
    }
    //endregion Core::Values::Constants
}
