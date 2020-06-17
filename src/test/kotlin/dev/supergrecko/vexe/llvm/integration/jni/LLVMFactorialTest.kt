package dev.supergrecko.vexe.llvm.integration.jni

import dev.supergrecko.vexe.llvm.ir.Builder
import dev.supergrecko.vexe.llvm.ir.CallConvention
import dev.supergrecko.vexe.llvm.ir.IntPredicate
import dev.supergrecko.vexe.llvm.ir.Module
import dev.supergrecko.vexe.llvm.ir.PassManager
import dev.supergrecko.vexe.llvm.ir.types.FunctionType
import dev.supergrecko.vexe.llvm.ir.types.IntType
import dev.supergrecko.vexe.llvm.ir.values.constants.ConstantInt
import dev.supergrecko.vexe.llvm.support.VerifierFailureAction
import dev.supergrecko.vexe.test.TestSuite
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.llvm.LLVM.LLVMExecutionEngineRef
import org.bytedeco.llvm.global.LLVM
import kotlin.system.exitProcess
import kotlin.test.assertEquals

internal class LLVMFactorialTest : TestSuite({
    LLVM.LLVMLinkInMCJIT()
    LLVM.LLVMInitializeNativeAsmPrinter()
    LLVM.LLVMInitializeNativeAsmParser()
    LLVM.LLVMInitializeNativeDisassembler()
    LLVM.LLVMInitializeNativeTarget()

    val i32 = IntType(32)

    describe("Factorial function and execution") {
        val module = Module("Factorial")
        val ee = LLVMExecutionEngineRef()
        val pm = PassManager()
        val builder = Builder()
        val factorial = module.addFunction(
            "factorial",
            FunctionType(i32, listOf(i32), false)
        ).apply {
            setCallConvention(CallConvention.CCall)
        }

        onTearDown = {
            pm.dispose()
            builder.dispose()
            LLVM.LLVMDisposeExecutionEngine(ee)
        }

        val number = factorial.getParameter(0)
        val entry = factorial.createBlock("Entry")
        val then = factorial.createBlock("Then")
        val otherwise = factorial.createBlock("Otherwise")
        val end = factorial.createBlock("End")

        builder.apply {
            positionAtEnd(entry)

            getInstructionBuilder().apply {
                val cond = createICmp(
                    number,
                    IntPredicate.EQ,
                    ConstantInt(i32, 0),
                    "cond"
                )

                createCondBr(cond, then, otherwise)
                positionAtEnd(then)
                createBr(end)
                positionAtEnd(otherwise)

                val resTrue = ConstantInt(i32, 1)
                val neg = createSub(number, ConstantInt(i32, 1), "n")
                val call = createCall(factorial, listOf(neg), "fac(n - 1)")
                val resFalse = createMul(number, call, "n * fac(n - 1)")
                createBr(end)

                positionAtEnd(end)

                val res = createPhi(i32, "result")
                res.addIncoming(
                    listOf(resTrue, resFalse),
                    listOf(then, otherwise)
                )
                createRet(res)
            }
        }

        module.dump()
        module.verify(VerifierFailureAction.AbortProcess)

        LLVM.LLVMAddConstantPropagationPass(pm.ref)
        LLVM.LLVMAddInstructionCombiningPass(pm.ref)
        LLVM.LLVMAddPromoteMemoryToRegisterPass(pm.ref)
        LLVM.LLVMAddGVNPass(pm.ref)
        LLVM.LLVMAddCFGSimplificationPass(pm.ref)
        LLVM.LLVMRunPassManager(pm.ref, module.ref)

        if (LLVM.LLVMCreateJITCompilerForModule(ee, module.ref, 2,
                BytePointer(0L))
            != 0) {
            exitProcess(-1)
        }

        val exec = LLVM.LLVMRunFunction(
            ee, factorial.ref, 1,
            LLVM.LLVMCreateGenericValueOfInt(i32.ref, 10, 0)
        )
        val res = LLVM.LLVMGenericValueToInt(exec, 0)

        assertEquals(3628800, res)
    }
})