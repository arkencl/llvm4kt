package dev.supergrecko.vexe.llvm.ir.instructions

import dev.supergrecko.vexe.llvm.internal.util.wrap
import dev.supergrecko.vexe.llvm.ir.BasicBlock
import dev.supergrecko.vexe.llvm.ir.Instruction
import dev.supergrecko.vexe.llvm.ir.Value
import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM

public class PhiInstruction internal constructor() : Instruction() {
    public constructor(llvmRef: LLVMValueRef) : this() {
        ref = llvmRef
    }

    //region Core::Instructions::PHINodes
    /**
     * Add an incoming value to the phi node
     *
     * @see LLVM.LLVMAddIncoming
     */
    public fun addIncoming(values: List<Value>, blocks: List<BasicBlock>) {
        require(values.size == blocks.size)

        val valuePtr = PointerPointer(*values.map { it.ref }.toTypedArray())
        val blockPtr = PointerPointer(*blocks.map { it.ref }.toTypedArray())

        values.mapIndexed { i, v -> valuePtr.put(i.toLong(), v.ref) }
        blocks.mapIndexed { i, v -> blockPtr.put(i.toLong(), v.ref) }

        LLVM.LLVMAddIncoming(ref, valuePtr, blockPtr, values.size)
    }

    /**
     * Count how many incoming values this phi node has
     *
     * The block size is always equal to the value size
     *
     * @see LLVM.LLVMCountIncoming
     */
    public fun getIncomingCount(): Int {
        return LLVM.LLVMCountIncoming(ref)
    }

    /**
     * Get an incoming value at [index]
     *
     * @see LLVM.LLVMGetIncomingValue
     */
    public fun getIncomingValue(index: Int): Value? {
        val value = LLVM.LLVMGetIncomingValue(ref, index)

        return wrap(value) { Value(it) }
    }

    /**
     * Get an incoming block at [index]
     *
     * @see LLVM.LLVMGetIncomingBlock
     */
    public fun getIncomingBlock(index: Int): BasicBlock? {
        val block = LLVM.LLVMGetIncomingBlock(ref, index)

        return wrap(block) { BasicBlock(it) }
    }
    //endregion Core::Instructions::PHINodes
}
