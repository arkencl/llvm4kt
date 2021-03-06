package dev.supergrecko.vexe.llvm.`object`

import dev.supergrecko.vexe.llvm.internal.contracts.Disposable
import org.bytedeco.llvm.LLVM.LLVMBinaryRef
import org.bytedeco.llvm.global.LLVM

public class Binary internal constructor() : Disposable {
    public lateinit var ref: LLVMBinaryRef
        internal set
    public override var valid: Boolean = true

    public constructor(llvmRef: LLVMBinaryRef) : this() {
        ref = llvmRef
    }

    override fun dispose() {
        require(valid) { "Cannot dispose object twice" }

        valid = false

        LLVM.LLVMDisposeBinary(ref)
    }
}
