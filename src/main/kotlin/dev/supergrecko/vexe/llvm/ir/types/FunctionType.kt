package dev.supergrecko.vexe.llvm.ir.types

import dev.supergrecko.vexe.llvm.internal.util.fromLLVMBool
import dev.supergrecko.vexe.llvm.internal.util.map
import dev.supergrecko.vexe.llvm.internal.util.toLLVMBool
import dev.supergrecko.vexe.llvm.ir.Type
import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.LLVMTypeRef
import org.bytedeco.llvm.global.LLVM

public class FunctionType internal constructor() : Type() {
    public constructor(llvmRef: LLVMTypeRef) : this() {
        ref = llvmRef
    }

    //region Core::Types::FunctionTypes
    /**
     * Create a function types
     *
     * This will construct a function types which returns the types provided in
     * [returns] which expects to receive parameters of the types provided in
     * [types]. You can mark a function types as variadic by setting the
     * [variadic] arg to true.
     */
    public constructor(
        returns: Type,
        types: List<Type>,
        variadic: Boolean
    ) : this() {
        val arr = ArrayList(types.map { it.ref }).toTypedArray()

        ref = LLVM.LLVMFunctionType(
            returns.ref,
            PointerPointer(*arr),
            arr.size,
            variadic.toLLVMBool()
        )
    }

    /**
     * Is this function type variadic?
     *
     * @see LLVM.LLVMIsFunctionVarArg
     */
    public fun isVariadic(): Boolean {
        return LLVM.LLVMIsFunctionVarArg(ref).fromLLVMBool()
    }

    /**
     * Get the parameter count
     *
     * @see LLVM.LLVMCountParamTypes
     */
    public fun getParameterCount(): Int {
        return LLVM.LLVMCountParamTypes(ref)
    }

    /**
     * Get the return type
     *
     * @see LLVM.LLVMGetReturnType
     */
    public fun getReturnType(): Type {
        val type = LLVM.LLVMGetReturnType(ref)

        return Type(type)
    }

    /**
     * Get the parameter types
     *
     * @see LLVM.LLVMGetParamTypes
     */
    public fun getParameterTypes(): List<Type> {
        val dest = PointerPointer<LLVMTypeRef>(getParameterCount().toLong())
        LLVM.LLVMGetParamTypes(ref, dest)

        return dest.map { Type(it) }
    }
    //endregion Core::Types::FunctionTypes
}
