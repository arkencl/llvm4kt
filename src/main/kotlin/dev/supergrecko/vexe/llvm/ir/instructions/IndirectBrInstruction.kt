package dev.supergrecko.vexe.llvm.ir.instructions

import dev.supergrecko.vexe.llvm.ir.BasicBlock
import dev.supergrecko.vexe.llvm.ir.Instruction
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM

public class IndirectBrInstruction internal constructor() : Instruction() {
    public constructor(llvmRef: LLVMValueRef) : this() {
        ref = llvmRef
    }

    //region InstructionBuilders
    /**
     * Append a destination to this indirect branch
     *
     * @see LLVM.LLVMAddDestination
     */
    public fun addDestination(handler: BasicBlock) {
        LLVM.LLVMAddDestination(ref, handler.ref)
    }
    //endregion InstructionBuilders
}
