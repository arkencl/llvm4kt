package dev.supergrecko.kllvm.core.type

import dev.supergrecko.kllvm.utils.iterateIntoType
import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.LLVMTypeRef
import org.bytedeco.llvm.global.LLVM

public class LLVMArrayType internal constructor(llvmType: LLVMTypeRef) : LLVMType(llvmType) {
    public fun getLength(): Int {
        return LLVM.LLVMGetArrayLength(llvmType)
    }

    /**
     * TODO: Learn how to test this
     */
    public fun getSubtypes(): List<LLVMType> {
        val dest = PointerPointer<LLVMTypeRef>(getLength().toLong())
        LLVM.LLVMGetSubtypes(llvmType, dest)

        return dest.iterateIntoType { LLVMType(it) }
    }

    public fun getElementType(): LLVMType {
        return LLVMType(LLVM.LLVMGetElementType(llvmType))
    }
}