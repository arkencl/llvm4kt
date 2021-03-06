package dev.supergrecko.vexe.llvm.ir

import dev.supergrecko.vexe.llvm.internal.contracts.ContainsReference
import dev.supergrecko.vexe.llvm.internal.contracts.Disposable
import dev.supergrecko.vexe.llvm.internal.util.toLLVMBool
import dev.supergrecko.vexe.llvm.internal.util.wrap
import dev.supergrecko.vexe.llvm.ir.instructions.AShrInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.AddInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.AddrSpaceCastInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.AllocaInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.AndInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.AtomicCmpXchgInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.AtomicRMWInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.BitCastInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.BrInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.CallInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.CatchPadInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.CatchRetInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.CatchSwitchInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.CleanupPadInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.CleanupRetInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.ExtractElementInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.ExtractValueInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.FAddInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.FCmpInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.FDivInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.FMulInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.FNegInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.FPExtInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.FPToSIInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.FPToUIInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.FPTruncInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.FRemInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.FSubInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.FenceInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.GetElementPtrInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.ICmpInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.IndirectBrInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.InsertElementInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.InsertValueInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.IntToPtrInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.InvokeInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.LShrInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.LandingPadInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.LoadInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.MulInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.OrInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.PhiInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.PtrToIntInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.ResumeInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.RetInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.SDivInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.SExtInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.SIToFPInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.SRemInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.SelectInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.ShlInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.ShuffleVectorInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.StoreInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.SubInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.SwitchInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.TruncInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.UDivInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.UIToFPInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.URemInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.UnreachableInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.VAArgInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.XorInstruction
import dev.supergrecko.vexe.llvm.ir.instructions.ZExtInstruction
import dev.supergrecko.vexe.llvm.ir.types.FloatType
import dev.supergrecko.vexe.llvm.ir.types.FunctionType
import dev.supergrecko.vexe.llvm.ir.types.IntType
import dev.supergrecko.vexe.llvm.ir.types.PointerType
import dev.supergrecko.vexe.llvm.ir.values.FunctionValue
import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.LLVMBuilderRef
import org.bytedeco.llvm.global.LLVM

public class Builder public constructor(
    context: Context = Context.getGlobalContext()
) : Disposable, ContainsReference<LLVMBuilderRef> {
    public override var ref: LLVMBuilderRef = LLVM.LLVMCreateBuilderInContext(
        context.ref
    )
        internal set
    public override var valid: Boolean = true
    private val builder: InstructionBuilder = InstructionBuilder()

    public constructor(llvmRef: LLVMBuilderRef) : this() {
        ref = llvmRef
    }

    //region InstructionBuilders
    /**
     * Position the builder at the [block]
     *
     * By default, this positions the end of the [block]. If [instruction] is
     * passed and [instruction] is inside the given [block], the builder will
     * be positioned after this instruction
     *
     * @see LLVM.LLVMPositionBuilder
     */
    public fun setPosition(block: BasicBlock, instruction: Instruction) {
        LLVM.LLVMPositionBuilder(ref, block.ref, instruction.ref)
    }

    /**
     * Position the builder before the given [instruction]
     *
     * @see LLVM.LLVMPositionBuilderBefore
     */
    public fun setPositionBefore(instruction: Instruction) {
        LLVM.LLVMPositionBuilderBefore(ref, instruction.ref)
    }

    /**
     * Position the builder at the end of the [block]
     *
     * @see LLVM.LLVMPositionBuilderAtEnd
     */
    public fun setPositionAtEnd(block: BasicBlock) {
        LLVM.LLVMPositionBuilderAtEnd(ref, block.ref)
    }

    /**
     * Clear the insertion point
     *
     * This erases the current point of insertion which means the user has to
     * set a new insertion point.
     *
     * @see LLVM.LLVMClearInsertionPosition
     */
    public fun clear() {
        LLVM.LLVMClearInsertionPosition(ref)
    }

    /**
     * Get the block the builder is currently inside
     *
     * This returns null if the builder isn't positioned anywhere, for
     * example cleared by [clear]
     *
     * @see LLVM.LLVMGetInsertBlock
     */
    public fun getInsertionBlock(): BasicBlock? {
        val bb = LLVM.LLVMGetInsertBlock(ref)

        return wrap(bb) { BasicBlock(it) }
    }

    /**
     * Insert an instruction into the current insertion point
     *
     * If [name] is passed, [LLVM.LLVMInsertIntoBuilderWithName] is used.
     *
     * @see LLVM.LLVMInsertIntoBuilder
     */
    public fun insert(instruction: Instruction, name: String?) {
        if (name != null) {
            LLVM.LLVMInsertIntoBuilderWithName(ref, instruction.ref, name)
        } else {
            LLVM.LLVMInsertIntoBuilder(ref, instruction.ref)
        }
    }

    /**
     * Get the current debug location for this builder if it exists
     *
     * @see LLVM.LLVMGetCurrentDebugLocation2
     */
    public fun getCurrentDebugLocation(): Metadata? {
        val loc = LLVM.LLVMGetCurrentDebugLocation2(ref)

        return wrap(loc) { Metadata(it) }
    }

    /**
     * Set the current debug location for this builder
     *
     * @see LLVM.LLVMSetCurrentDebugLocation2
     */
    public fun setCurrentDebugLocation(location: Metadata) {
        LLVM.LLVMSetCurrentDebugLocation2(ref, location.ref)
    }

    /**
     * Attempt to use the builder's debug location to set the [instruction]'s
     * debug location
     *
     * If the builder doesn't have a debug location, nothing happens.
     *
     * @see LLVM.LLVMSetInstDebugLocation
     */
    public fun setInstructionDebugLocation(instruction: Instruction) {
        LLVM.LLVMSetInstDebugLocation(ref, instruction.ref)
    }

    /**
     * Get the default fp math metadata for the builder if it exists
     *
     * @see LLVM.LLVMBuilderGetDefaultFPMathTag
     */
    public fun getDefaultFPMathTag(): Metadata? {
        val tag = LLVM.LLVMBuilderGetDefaultFPMathTag(ref)

        return wrap(tag) { Metadata(it) }
    }

    /**
     * Set the default fp math metadata for the builder
     *
     * @see LLVM.LLVMBuilderSetDefaultFPMathTag
     */
    public fun setDefaultFPMathTag(tag: Metadata) {
        LLVM.LLVMBuilderSetDefaultFPMathTag(ref, tag.ref)
    }

    /**
     * Create a global string and return its value
     *
     * If [asPointer] is true, this will build a pointer to the string.
     *
     * @see LLVM.LLVMBuildGlobalString
     */
    public fun buildGlobalString(
        string: String,
        globalName: String,
        asPointer: Boolean = false
    ): Value {
        val value = if (asPointer) {
            LLVM.LLVMBuildGlobalStringPtr(ref, string, globalName)
        } else {
            LLVM.LLVMBuildGlobalString(ref, string, globalName)
        }

        return Value(value)
    }

    /**
     * Get the singleton instruction builder
     *
     * Returns a per-class singleton instance of [InstructionBuilder]
     */
    public fun build(): InstructionBuilder = builder

    /**
     * An instruction builder is a wrapper class for building instructions
     * for a builder.
     *
     * To prevent polluting autocomplete for the [Builder] all the
     * instruction creation functions are declared in here.
     *
     * Each [Builder] has one of these, retrievable by [build]
     */
    public inner class InstructionBuilder {
        /**
         * Build a return instruction
         *
         * Returns the given [value]
         *
         * @see LLVM.LLVMBuildRet
         */
        public fun createRet(
            value: Value
        ): RetInstruction {
            val inst = LLVM.LLVMBuildRet(ref, value.ref)

            return RetInstruction(inst)
        }

        /**
         * Build a return instruction
         *
         * This instruction returns void instead of a value
         *
         * @see LLVM.LLVMBuildRetVoid
         */
        public fun createRetVoid(): RetInstruction {
            val inst = LLVM.LLVMBuildRetVoid(ref)

            return RetInstruction(inst)
        }

        /**
         * Build a return instruction
         *
         * Create a sequence of N insertvalue instructions, with one Value
         * from the [values] array each, that build a aggregate
         * return value one value at a time, and a ret instruction to return
         * the resulting aggregate value.
         *
         * For this call to work, the builder must be placed inside a
         * function as it verifies the return type with the aggregate values.
         * Failing to fulfill this requirement crashes the JVM
         *
         * @see LLVM.LLVMBuildAggregateRet
         */
        public fun createAggregateRet(
            values: List<Value>
        ): RetInstruction {
            val ptr = PointerPointer(*values.map { it.ref }.toTypedArray())
            val inst = LLVM.LLVMBuildAggregateRet(ref, ptr, values.size)

            return RetInstruction(inst)
        }

        /**
         * Build a branch instruction
         *
         * This is an unconditional branch. Use [createCondBr]
         *
         * @see LLVM.LLVMBuildBr
         */
        public fun createBr(
            destination: BasicBlock
        ): BrInstruction {
            val inst = LLVM.LLVMBuildBr(ref, destination.ref)

            return BrInstruction(inst)
        }

        /**
         * Build a branch instruction
         *
         * Evaluates [condition], if it's truthy it will branch to [truthy]
         * otherwise it will branch to [falsy]
         *
         * @see LLVM.LLVMBuildCondBr
         */
        public fun createCondBr(
            condition: Value,
            truthy: BasicBlock,
            falsy: BasicBlock
        ): BrInstruction {
            val inst = LLVM.LLVMBuildCondBr(
                ref,
                condition.ref,
                truthy.ref,
                falsy.ref
            )

            return BrInstruction(inst)
        }

        /**
         * Build a switch instruction
         *
         * A switch is a glorified branch, it takes a single integer
         * [condition] and a [default] case which will be branched if none of
         * the other cases match.
         *
         * You may add cases to the instruction after construction. You may
         * hint at how many cases the switch expects with [expectedCases]
         *
         * @see LLVM.LLVMBuildSwitch
         */
        public fun createSwitch(
            condition: Value,
            default: BasicBlock,
            expectedCases: Int = 10
        ): SwitchInstruction {
            val inst = LLVM.LLVMBuildSwitch(
                ref,
                condition.ref,
                default.ref,
                expectedCases
            )

            return SwitchInstruction(inst)
        }

        /**
         * Build an indirect branch instruction
         *
         * Create an indirect break to the [destination] address. Each
         * destination added to this instruction are destinations the
         * [destination] address may point to.
         *
         * Destinations can be added on the returned instruction
         *
         * @see LLVM.LLVMBuildIndirectBr
         */
        public fun createIndirectBr(
            destination: Value,
            expectedCases: Int = 10
        ): IndirectBrInstruction {
            val inst = LLVM.LLVMBuildIndirectBr(
                ref,
                destination.ref,
                expectedCases
            )

            return IndirectBrInstruction(inst)
        }

        /**
         * Build an invoke instruction
         *
         * Invoke moves control to the target [function] and continues with
         * either a [normal] or a [catch] label. If the [function] returns,
         * then control is moved to [normal]. If the callee, or any indirect
         * callees returns via resume then control is moved to [catch].
         *
         * If the callee did return, it will be moved into [variable]
         *
         * @see LLVM.LLVMBuildInvoke2
         */
        public fun createInvoke(
            functionType: FunctionType,
            function: FunctionValue,
            arguments: List<Value>,
            normal: BasicBlock,
            catch: BasicBlock,
            variable: String
        ): InvokeInstruction {
            val args = PointerPointer(*arguments.map { it.ref }.toTypedArray())
            val inst = LLVM.LLVMBuildInvoke2(
                ref,
                functionType.ref,
                function.ref,
                args,
                arguments.size,
                normal.ref,
                catch.ref,
                variable
            )

            return InvokeInstruction(inst)
        }

        /**
         * Build an unreachable instruction
         *
         * Unreachable instructions inform the optimizer that code below the
         * instruction are not reachable. This can be used to implement noreturn
         *
         * @see LLVM.LLVMBuildUnreachable
         */
        public fun createUnreachable(): UnreachableInstruction {
            val inst = LLVM.LLVMBuildUnreachable(ref)

            return UnreachableInstruction(inst)
        }

        /**
         * Build a resume instruction
         *
         * Resume resumes the propagation of an in-flight exception which was
         * unwinded at a landingpad
         *
         * @see LLVM.LLVMBuildResume
         */
        public fun createResume(
            value: Value
        ): ResumeInstruction {
            val inst = LLVM.LLVMBuildResume(ref, value.ref)

            return ResumeInstruction(inst)
        }

        /**
         * Build a landing pad instruction
         *
         * A landing pad is a catch clause for LLVM's exception handling
         * system. The result of the landingpad is stored in [variable]
         *
         * Clauses can be added on the returned instructions
         *
         * @see LLVM.LLVMBuildLandingPad
         */
        public fun createLandingPad(
            catchesType: Type,
            personalityFunction: FunctionValue,
            expectedClauses: Int = 10,
            variable: String
        ): LandingPadInstruction {
            val inst = LLVM.LLVMBuildLandingPad(
                ref,
                catchesType.ref,
                personalityFunction.ref,
                expectedClauses,
                variable
            )

            return LandingPadInstruction(inst)
        }

        /**
         * Build a cleanup return instruction
         *
         * A cleanup ret exits an existing cleanup pad instruction, namely
         * [cleanup]. It also has an optional [successor] which will be the
         * basic block to move to after. This must begin with either a
         * cleanuppad or a catchswitch instruction
         *
         * @see LLVM.LLVMBuildCleanupRet
         */
        public fun createCleanupRet(
            cleanup: CleanupPadInstruction,
            successor: BasicBlock? = null
        ): CleanupRetInstruction {
            val inst = LLVM.LLVMBuildCleanupRet(
                ref,
                cleanup.ref,
                successor?.ref
            )

            return CleanupRetInstruction(inst)
        }

        /**
         * Build a catch return instruction
         *
         * Catch ret ends and existing in-flight exception whose unwinding
         * was interuppted with a [catchpad] instruction. gets a chance to
         * execute arbitrary code to, for example, destroy the active
         * exception. Control then transfers to [successor].
         *
         * @see LLVM.LLVMBuildCatchRet
         */
        public fun createCatchRet(
            catchpad: CatchPadInstruction,
            successor: BasicBlock
        ): CatchRetInstruction {
            val inst = LLVM.LLVMBuildCatchRet(ref, catchpad.ref, successor.ref)

            return CatchRetInstruction(inst)
        }

        /**
         * Build a catch pad instruction
         *
         * The [parent] operand must always be a token produced by a
         * catchswitch instruction in a predecessor block. This ensures that
         * each catchpad has exactly one predecessor block, and it always
         * terminates in a catchswitch.
         *
         * The [arguments] correspond to whatever information the personality
         * routine requires to know if this is an appropriate handler for the
         * exception. Control will transfer to the catchpad if this is the
         * first appropriate handler for the exception.
         *
         * The [variable] has the type token and is used to match the catchpad
         * to corresponding catchrets and other nested exception handling pads.
         *
         * @see LLVM.LLVMBuildCatchPad
         */
        public fun createCatchPad(
            parent: Value,
            arguments: List<Value>,
            variable: String
        ): CatchPadInstruction {
            val args = PointerPointer(*arguments.map { it.ref }.toTypedArray())
            val inst = LLVM.LLVMBuildCatchPad(
                ref,
                parent.ref,
                args,
                arguments.size,
                variable
            )

            return CatchPadInstruction(inst)
        }

        /**
         * Build a cleanup pad instruction
         *
         * A cleanup pad specifies that a basic block is a cleanup block.
         *
         * The [arguments] correspond to whatever additional information the
         * personality function requires to execute the cleanup.
         *
         * The [variable] has the type token and is used to match the cleanuppad
         * to corresponding cleanuprets.
         *
         * @see LLVM.LLVMBuildCleanupPad
         */
        public fun createCleanupPad(
            parent: Value,
            arguments: List<Value>,
            variable: String
        ): CleanupPadInstruction {
            val args = PointerPointer(*arguments.map { it.ref }.toTypedArray())
            val inst = LLVM.LLVMBuildCleanupPad(
                ref,
                parent.ref,
                args,
                arguments.size,
                variable
            )

            return CleanupPadInstruction(inst)
        }

        /**
         * Build a catch switch instruction
         *
         * The catch switch is used to describe a set of exception handlers.
         * The [unwind] argument is another basic block which begins with
         * either a cleanuppad or a catchswitch.
         *
         * [handlers] is the amount added handlers expected. Handlers
         * are added on the returned instruction
         *
         * @see LLVM.LLVMBuildCatchSwitch
         */
        public fun createCatchSwitch(
            parent: Value,
            unwind: BasicBlock,
            handlers: Int = 10,
            variable: String
        ): CatchSwitchInstruction {
            val inst = LLVM.LLVMBuildCatchSwitch(
                ref,
                parent.ref,
                unwind.ref,
                handlers,
                variable
            )

            return CatchSwitchInstruction(inst)
        }

        /**
         * Build an add instruction
         *
         * Add returns the sum of two integers or vectors of integers, [lhs]
         * and [rhs]. You can apply the [nuw] and [nsw] flags via the [nuw] and
         * [nsw] arguments
         *
         * The result is stored in [variable]
         *
         * @see LLVM.LLVMBuildAdd
         */
        public fun createAdd(
            lhs: Value,
            rhs: Value,
            variable: String,
            nsw: Boolean = false,
            nuw: Boolean = false
        ): AddInstruction {
            require(!(nsw && nuw)) {
                "Instruction can not declare both NUW & " +
                        "NSW"
            }

            val inst = when {
                nsw -> LLVM.LLVMBuildNSWAdd(ref, lhs.ref, rhs.ref, variable)
                nuw -> LLVM.LLVMBuildNUWAdd(ref, lhs.ref, rhs.ref, variable)
                else -> LLVM.LLVMBuildAdd(ref, lhs.ref, rhs.ref, variable)
            }

            return AddInstruction(inst)
        }

        /**
         * Build a fadd instruction
         *
         * Fadd returns the sum of two floats or vectors of floats. The
         * result is stored in [variable]
         *
         * @see LLVM.LLVMBuildFAdd
         */
        public fun createFAdd(
            lhs: Value,
            rhs: Value,
            variable: String
        ): FAddInstruction {
            val inst = LLVM.LLVMBuildFAdd(ref, lhs.ref, rhs.ref, variable)

            return FAddInstruction(inst)
        }

        /**
         * Build a sub instruction
         *
         * Sub returns the difference between two integers or vectors of
         * integers, [lhs] and [rhs]. You can apply the [nuw] and [nsw] flags
         * via the [nuw] and [nsw] arguments
         *
         * The result is stored in [variable]
         *
         * @see LLVM.LLVMBuildSub
         */
        public fun createSub(
            lhs: Value,
            rhs: Value,
            variable: String,
            nsw: Boolean = false,
            nuw: Boolean = false
        ): SubInstruction {
            require(!(nsw && nuw)) {
                "Instruction can not declare both NUW & " +
                        "NSW"
            }

            val inst = when {
                nsw -> LLVM.LLVMBuildNSWSub(ref, lhs.ref, rhs.ref, variable)
                nuw -> LLVM.LLVMBuildNUWSub(ref, lhs.ref, rhs.ref, variable)
                else -> LLVM.LLVMBuildSub(ref, lhs.ref, rhs.ref, variable)
            }

            return SubInstruction(inst)
        }

        /**
         * Build a fsub instruction
         *
         * fsub returns the difference of two floats or vectors of floats. The
         * result is stored in [variable]
         *
         * @see LLVM.LLVMBuildFSub
         */
        public fun createFSub(
            lhs: Value,
            rhs: Value,
            variable: String
        ): FSubInstruction {
            val inst = LLVM.LLVMBuildFSub(ref, lhs.ref, rhs.ref, variable)

            return FSubInstruction(inst)
        }

        /**
         * Build a mul instruction
         *
         * Mul returns the product of two integers or vectors of integers,
         * [lhs] and [rhs]. You can apply the [nuw] and [nsw] flags via the
         * [nuw] and [nsw] arguments
         *
         * The result is stored in [variable]
         *
         * @see LLVM.LLVMBuildMul
         */
        public fun createMul(
            lhs: Value,
            rhs: Value,
            variable: String,
            nsw: Boolean = false,
            nuw: Boolean = false
        ): MulInstruction {
            require(!(nsw && nuw)) {
                "Instruction can not declare both NUW & " +
                        "NSW"
            }

            val inst = when {
                nsw -> LLVM.LLVMBuildNSWMul(ref, lhs.ref, rhs.ref, variable)
                nuw -> LLVM.LLVMBuildNUWMul(ref, lhs.ref, rhs.ref, variable)
                else -> LLVM.LLVMBuildMul(ref, lhs.ref, rhs.ref, variable)
            }

            return MulInstruction(inst)
        }

        /**
         * Build a fmul instruction
         *
         * FMul returns the product of two floats or vectors of floats. The
         * result is stored in [variable]
         */
        public fun createFMul(
            lhs: Value,
            rhs: Value,
            variable: String
        ): FMulInstruction {
            val inst = LLVM.LLVMBuildMul(ref, lhs.ref, rhs.ref, variable)

            return FMulInstruction(inst)
        }

        /**
         * Build a sdiv instruction
         *
         * Sdiv returns the signed integer quotient of [lhs] and [rhs]. If
         * [exact] is applied and [lhs] is not an exact multiple of [rhs], a
         * poison value will be returned.
         *
         * The returned value is stored in [variable]
         *
         * @see LLVM.LLVMBuildSDiv
         */
        public fun createSDiv(
            lhs: Value,
            rhs: Value,
            variable: String,
            exact: Boolean = false
        ): SDivInstruction {
            val inst = if (exact) {
                LLVM.LLVMBuildExactSDiv(ref, lhs.ref, rhs.ref, variable)
            } else {
                LLVM.LLVMBuildSDiv(ref, lhs.ref, rhs.ref, variable)
            }

            return SDivInstruction(inst)
        }

        /**
         * Build an udiv instruction
         *
         * Udiv returns the unsigned integer quotient of [lhs] and [rhs]. If
         * [exact] is applied and [lhs] is not an exact multiple of [rhs], a
         * poison value will be returned.
         *
         * The returned value is stored in [variable]
         *
         * @see LLVM.LLVMBuildUDiv
         */
        public fun createUDiv(
            lhs: Value,
            rhs: Value,
            variable: String,
            exact: Boolean = false
        ): UDivInstruction {
            val inst = if (exact) {
                LLVM.LLVMBuildExactUDiv(ref, lhs.ref, rhs.ref, variable)
            } else {
                LLVM.LLVMBuildUDiv(ref, lhs.ref, rhs.ref, variable)
            }

            return UDivInstruction(inst)
        }

        /**
         * Build a fdiv instruction
         *
         * Fdiv returns the floating point quotient of [lhs] and [rhs]. The
         * returned value is stored in [variable]
         *
         * @see LLVM.LLVMBuildFDiv
         */
        public fun createFDiv(
            lhs: Value,
            rhs: Value,
            variable: String
        ): FDivInstruction {
            val inst = LLVM.LLVMBuildFDiv(ref, lhs.ref, rhs.ref, variable)

            return FDivInstruction(inst)
        }

        /**
         * Build a urem instruction
         *
         * Urem takes the unsigned value of the remainder of [lhs] and [rhs].
         * The returned value is stored in [variable]
         *
         * @see LLVM.LLVMBuildURem
         */
        public fun createURem(
            lhs: Value,
            rhs: Value,
            variable: String
        ): URemInstruction {
            val inst = LLVM.LLVMBuildURem(ref, lhs.ref, rhs.ref, variable)

            return URemInstruction(inst)
        }

        /**
         * Build a srem instruction
         *
         * Srem takes the signed value of the remainder of [lhs] and [rhs].
         * The returned value is stored in [variable]
         *
         * @see LLVM.LLVMBuildURem
         */
        public fun createSRem(
            lhs: Value,
            rhs: Value,
            variable: String
        ): SRemInstruction {
            val inst = LLVM.LLVMBuildSRem(ref, lhs.ref, rhs.ref, variable)

            return SRemInstruction(inst)
        }

        /**
         * Build a frem instruction
         *
         * Urem takes the floating value of the remainder of [lhs] and [rhs],
         * both which are floating point values
         *
         * The returned value is stored in [variable]
         *
         * @see LLVM.LLVMBuildURem
         */
        public fun createFRem(
            lhs: Value,
            rhs: Value,
            variable: String
        ): FRemInstruction {
            val inst = LLVM.LLVMBuildFRem(ref, lhs.ref, rhs.ref, variable)

            return FRemInstruction(inst)
        }

        /**
         * Build a shl instruction
         *
         * Shl returns [lhs] shifted [rhs] bits to the left. The returned
         * value is stored in [variable]
         *
         * @see LLVM.LLVMBuildShl
         */
        public fun createShl(
            lhs: Value,
            rhs: Value,
            variable: String
        ): ShlInstruction {
            val inst = LLVM.LLVMBuildShl(ref, lhs.ref, rhs.ref, variable)

            return ShlInstruction(inst)
        }

        /**
         * Build a lshr instruction
         *
         * Lshr returns [lhs] shifted [rhs] bits to the left with zero fill.
         * The returned value is stored in [variable]
         *
         * @see LLVM.LLVMBuildLShr
         */
        public fun createLShr(
            lhs: Value,
            rhs: Value,
            variable: String
        ): LShrInstruction {
            val inst = LLVM.LLVMBuildLShr(ref, rhs.ref, lhs.ref, variable)

            return LShrInstruction(inst)
        }

        /**
         * Build an ashr instruction
         *
         * Ashr returns [lhs] shifted [rhs] bits to the right. The returned
         * value is stored in [variable]
         *
         * @see LLVM.LLVMBuildAShr
         */
        public fun createAShr(
            lhs: Value,
            rhs: Value,
            variable: String
        ): AShrInstruction {
            val inst = LLVM.LLVMBuildAShr(ref, rhs.ref, lhs.ref, variable)

            return AShrInstruction(inst)
        }

        /**
         * Build an and instruction
         *
         * And returns the bitwise logical and of [lhs] and [rhs]. The
         * returned value is stored in [variable]
         *
         * @see LLVM.LLVMBuildAnd
         */
        public fun createAnd(
            lhs: Value,
            rhs: Value,
            variable: String
        ): AndInstruction {
            val inst = LLVM.LLVMBuildAnd(ref, rhs.ref, lhs.ref, variable)

            return AndInstruction(inst)
        }

        /**
         * Build an or instruction
         *
         * Or returns the bitwise logical or of [lhs] and [rhs]. The returned
         * value is stored in [variable]
         *
         * @see LLVM.LLVMBuildOr
         */
        public fun createOr(
            lhs: Value,
            rhs: Value,
            variable: String
        ): OrInstruction {
            val inst = LLVM.LLVMBuildOr(ref, lhs.ref, rhs.ref, variable)

            return OrInstruction(inst)
        }

        /**
         * Build a xor instruction
         *
         * Xor returns the bitwise logical exclusive of [lhs] and [rhs]. The
         * returned value is stored in [variable]
         *
         * @see LLVM.LLVMBuildXor
         */
        public fun createXor(
            lhs: Value,
            rhs: Value,
            variable: String
        ): XorInstruction {
            val inst = LLVM.LLVMBuildXor(ref, lhs.ref, rhs.ref, variable)

            return XorInstruction(inst)
        }

        /**
         * Build a neg instruction
         *
         * LLVM implements neg with sub. Sub returns the negation of [value].
         * The returned value is stored in [variable]
         *
         * @see LLVM.LLVMBuildNeg
         */
        public fun buildNeg(
            value: Value,
            variable: String,
            nuw: Boolean = false,
            nsw: Boolean = false
        ): SubInstruction {
            require(!(nsw && nuw)) {
                "Instruction can not declare both NUW & " +
                        "NSW"
            }

            val inst = when {
                nsw -> LLVM.LLVMBuildNSWNeg(ref, value.ref, variable)
                nuw -> LLVM.LLVMBuildNUWNeg(ref, value.ref, variable)
                else -> LLVM.LLVMBuildNeg(ref, value.ref, variable)
            }

            return SubInstruction(inst)
        }

        /**
         * Build a fneg instruction
         *
         * Fneg returns the negation of [value]. The returned value is stored
         * in [variable]
         *
         * @see LLVM.LLVMBuildFNeg
         */
        public fun createFNeg(value: Value, variable: String): FNegInstruction {
            val inst = LLVM.LLVMBuildFNeg(ref, value.ref, variable)

            return FNegInstruction(inst)
        }

        /**
         * Build a not instruction
         *
         * LLVM implements not with xor. Xor returns the bitwise logical
         * exclusive of its two operands
         *
         * @see LLVM.LLVMBuildNot
         */
        public fun createNot(
            value: Value,
            variable: String
        ): XorInstruction {
            val inst = LLVM.LLVMBuildNot(ref, value.ref, variable)

            return XorInstruction(inst)
        }

        /**
         * Create a call to the built-in malloc function
         *
         * If you want to allocate an array/multiple items, pass an
         * [elementCount] which specifies how many items you want
         *
         * @see LLVM.LLVMBuildMalloc
         */
        public fun createMalloc(
            type: Type,
            name: String,
            elementCount: Value? = null
        ): CallInstruction {
            val inst = if (elementCount != null) {
                LLVM.LLVMBuildArrayMalloc(ref, type.ref, elementCount.ref, name)
            } else {
                LLVM.LLVMBuildMalloc(ref, type.ref, name)
            }

            return CallInstruction(inst)
        }

        /**
         * Create a call to the built-in memset function
         *
         * @see LLVM.LLVMBuildMemSet
         */
        public fun createMemSet(
            ptr: Value,
            value: Value,
            length: Value,
            align: Int
        ): CallInstruction {
            val inst = LLVM.LLVMBuildMemSet(
                ref,
                ptr.ref,
                value.ref,
                length.ref,
                align
            )

            return CallInstruction(inst)
        }

        /**
         * Create a call to the built-in memcpy function
         *
         * @see LLVM.LLVMBuildMemCpy
         */
        public fun createMemCpy(
            destination: Value,
            destinationAlignment: Int,
            source: Value,
            sourceAlignment: Int,
            size: Value
        ): CallInstruction {
            val inst = LLVM.LLVMBuildMemCpy(
                ref,
                destination.ref,
                destinationAlignment,
                source.ref,
                sourceAlignment,
                size.ref
            )

            return CallInstruction(inst)
        }

        /**
         * Create a call to the built-in memmove function
         *
         * @see LLVM.LLVMBuildMemMove
         */
        public fun createMemMove(
            destination: Value,
            destinationAlignment: Int,
            source: Value,
            sourceAlignment: Int,
            size: Value
        ): CallInstruction {
            val inst = LLVM.LLVMBuildMemMove(
                ref,
                destination.ref,
                destinationAlignment,
                source.ref,
                sourceAlignment,
                size.ref
            )

            return CallInstruction(inst)
        }

        /**
         * Build an alloca instruction
         *
         * The passed [type] must be sized. You may specify an amount of
         * objects to be allocated, default is one. This is done by
         * specifying the [elementCount] argument.
         *
         * @see LLVM.LLVMBuildAlloca
         */
        public fun createAlloca(
            type: Type,
            name: String,
            elementCount: Value? = null
        ): AllocaInstruction {
            val inst = if (elementCount != null) {
                LLVM.LLVMBuildArrayAlloca(ref, type.ref, elementCount.ref, name)
            } else {
                LLVM.LLVMBuildAlloca(ref, type.ref, name)
            }

            return AllocaInstruction(inst)
        }

        /**
         * Create a call to the built-in free function
         *
         * @see LLVM.LLVMBuildFree
         */
        public fun createFree(ptr: Value): CallInstruction {
            val inst = LLVM.LLVMBuildFree(ref, ptr.ref)

            return CallInstruction(inst)
        }

        /**
         * Build a load instruction
         *
         * Loads [dereference] into memory and assigns it the type [type].
         * The result is stored in a new variable in the IR named [variable]
         *
         * @see LLVM.LLVMBuildLoad2
         */
        public fun createLoad(
            type: Type,
            dereference: Value,
            variable: String
        ): LoadInstruction {
            val inst = LLVM.LLVMBuildLoad2(
                ref,
                type.ref,
                dereference.ref,
                variable
            )

            return LoadInstruction(inst)
        }

        /**
         * Build a store instruction
         *
         * Writes [value] into memory at [destination]
         */
        public fun createStore(
            value: Value,
            destination: Value
        ): StoreInstruction {
            val inst = LLVM.LLVMBuildStore(ref, value.ref, destination.ref)

            return StoreInstruction(inst)
        }

        /**
         * Build a GEP instruction
         *
         * A GetElementPtr instruction accesses an element of an aggregate
         * data structure.
         *
         * The returned value is stored in [variable]
         *
         * @see LLVM.LLVMBuildGEP
         */
        public fun createGEP(
            ptr: Value,
            ptrType: Type,
            indices: List<Value>,
            inBounds: Boolean,
            variable: String
        ): GetElementPtrInstruction {
            val args = PointerPointer(*indices.map { it.ref }.toTypedArray())
            val inst = if (inBounds) {
                LLVM.LLVMBuildInBoundsGEP2(
                    ref,
                    ptrType.ref,
                    ptr.ref,
                    args,
                    indices.size,
                    variable
                )
            } else {
                LLVM.LLVMBuildGEP2(
                    ref,
                    ptrType.ref,
                    ptr.ref,
                    args,
                    indices.size,
                    variable
                )
            }

            return GetElementPtrInstruction(inst)
        }

        /**
         * Build a trunc instruction
         *
         * Trunc takes an integer [value] and truncates it to the [target]
         * integer type.
         *
         * The returned value is stored in [variable]
         *
         * @see LLVM.LLVMBuildTrunc
         */
        public fun createTrunc(
            value: Value,
            target: IntType,
            variable: String
        ): TruncInstruction {
            val inst = LLVM.LLVMBuildTrunc(ref, value.ref, target.ref, variable)

            return TruncInstruction(inst)
        }

        /**
         * Build a zext instruction
         *
         * Zext takes an integer [value] and zero extends it to the [target]
         * integer type
         *
         * The returned value is stored in [variable]
         *
         * @see LLVM.LLVMBuildZExt
         */
        public fun createZExt(
            value: Value,
            target: IntType,
            variable: String
        ): ZExtInstruction {
            val inst = LLVM.LLVMBuildZExt(ref, value.ref, target.ref, variable)

            return ZExtInstruction(inst)
        }

        /**
         * Build a sext instruction
         *
         * Sext takes an integer [value] and sign extends it to the [target]
         * integer value
         *
         * The returned value is stored in [variable]
         *
         * @see LLVM.LLVMBuildSExt
         */
        public fun createSExt(
            value: Value,
            target: IntType,
            variable: String
        ): SExtInstruction {
            val inst = LLVM.LLVMBuildSExt(ref, value.ref, target.ref, variable)

            return SExtInstruction(inst)
        }

        /**
         * Build a fptoui instruction
         *
         * FP to UI takes a floating point [value] and casts it to an
         * unsigned integer [target] type
         *
         * The returned value is stored in [variable]
         *
         * @see LLVM.LLVMBuildFPToUI
         */
        public fun createFPToUI(
            value: Value,
            target: IntType,
            variable: String
        ): FPToUIInstruction {
            val inst = LLVM.LLVMBuildFPToUI(
                ref,
                value.ref,
                target.ref,
                variable
            )

            return FPToUIInstruction(inst)
        }

        /**
         * Build a fptosi instruction
         *
         * FP to SI takes a floating point [value] and casts it to a signed
         * integer [target] type
         *
         * The returned value is stored in [variable]
         *
         * @see LLVM.LLVMBuildFPToSI
         */
        public fun createFPToSI(
            value: Value,
            target: IntType,
            variable: String
        ): FPToSIInstruction {
            val inst = LLVM.LLVMBuildFPToSI(
                ref,
                value.ref,
                target.ref,
                variable
            )

            return FPToSIInstruction(inst)
        }

        /**
         * Build a uitofp instruction
         *
         * UI to FP takes an unsigned integer [value] and casts it to the
         * [target] floating point type
         *
         * The returned value is stored in [variable]
         *
         * @see LLVM.LLVMBuildUIToFP
         */
        public fun createUIToFP(
            value: Value,
            target: FloatType,
            variable: String
        ): UIToFPInstruction {
            val inst = LLVM.LLVMBuildUIToFP(
                ref,
                value.ref,
                target.ref,
                variable
            )

            return UIToFPInstruction(inst)
        }

        /**
         * Build a sitofp instruction
         *
         * SI to FP takes a signed integer [value] and casts it to the
         * [target] floating point type
         *
         * The returned value is stored in [variable]
         *
         * @see LLVM.LLVMBuildSIToFP
         */
        public fun createSIToFP(
            value: Value,
            target: IntType,
            variable: String
        ): SIToFPInstruction {
            val inst = LLVM.LLVMBuildSIToFP(
                ref,
                value.ref,
                target.ref,
                variable
            )

            return SIToFPInstruction(inst)
        }

        /**
         * Build a fptrunc instruction
         *
         * FPTrunc takes a floating point [value] and truncates it to the
         * [target] floating point type
         *
         * The returned value is stored in [variable]
         *
         * @see LLVM.LLVMBuildFPTrunc
         */
        public fun createFPTrunc(
            value: Value,
            target: FloatType,
            variable: String
        ): FPTruncInstruction {
            val inst = LLVM.LLVMBuildFPTrunc(
                ref,
                value.ref,
                target.ref,
                variable
            )

            return FPTruncInstruction(inst)
        }

        /**
         * Build a fpext instruction
         *
         * FPExt takes a floating point [value] and truncates it to the
         * [target] floating point type
         *
         * The returned value is stored in [variable]
         *
         * @see LLVM.LLVMBuildFPExt
         */
        public fun createFPExt(
            value: Value,
            target: FloatType,
            variable: String
        ): FPExtInstruction {
            val inst = LLVM.LLVMBuildFPExt(
                ref,
                value.ref,
                target.ref,
                variable
            )

            return FPExtInstruction(inst)
        }

        /**
         * Build a ptrtoint instruction
         *
         * Ptr to Int interprets the pointer [value] as an integer and
         * truncates/zero-extends the valuer to the [target] type
         *
         * The returned value is stored in [variable]
         *
         * @see LLVM.LLVMBuildPtrToInt
         */
        public fun createPtrToInt(
            value: Value,
            target: IntType,
            variable: String
        ): PtrToIntInstruction {
            val inst = LLVM.LLVMBuildPtrToInt(
                ref,
                value.ref,
                target.ref,
                variable
            )

            return PtrToIntInstruction(inst)
        }

        /**
         * Build an inttoptr instruction
         *
         * Int to Ptr takes an integer [value] and converts it to the
         * [target] pointer type
         *
         * The returned value is stored in [variable]
         *
         * @see LLVM.LLVMBuildIntToPtr
         */
        public fun createIntToPtr(
            value: Value,
            target: PointerType,
            variable: String
        ): IntToPtrInstruction {
            val inst = LLVM.LLVMBuildIntToPtr(
                ref,
                value.ref,
                target.ref,
                variable
            )

            return IntToPtrInstruction(inst)
        }

        /**
         * Build a bitcast instruction
         *
         * Bitcast converts the [value] to the non-aggregate [target] type
         *
         * The returned value is stored in [variable]
         *
         * @see LLVM.LLVMBuildBitCast
         */
        public fun createBitCast(
            value: Value,
            target: Type,
            variable: String
        ): BitCastInstruction {
            val inst = LLVM.LLVMBuildBitCast(
                ref,
                value.ref,
                target.ref,
                variable
            )

            return BitCastInstruction(inst)
        }

        /**
         * Build an addrspacecast instruction
         *
         * Addrspacecast converts the pointer [value] to the [target] type
         *
         * The returned value is stored in [variable]
         *
         * @see LLVM.LLVMBuildAddrSpaceCast
         */
        public fun createAddrSpaceCast(
            value: Value,
            target: IntType,
            variable: String
        ): AddrSpaceCastInstruction {
            val inst = LLVM.LLVMBuildAddrSpaceCast(
                ref,
                value.ref,
                target.ref,
                variable
            )

            return AddrSpaceCastInstruction(inst)
        }

        /**
         * Build an icmp instruction
         *
         * Compares two integers, [lhs] and [rhs] with the given [predicate]
         *
         * The returned value is stored in [variable]
         *
         * @see LLVM.LLVMBuildICmp
         */
        public fun createICmp(
            lhs: Value,
            predicate: IntPredicate,
            rhs: Value,
            variable: String
        ): ICmpInstruction {
            val inst = LLVM.LLVMBuildICmp(
                ref,
                predicate.value,
                lhs.ref,
                rhs.ref,
                variable
            )

            return ICmpInstruction(inst)
        }

        /**
         * Build a fcp instruction
         *
         * Compares two floats, [lhs] and [rhs] with the given [predicate]
         *
         * The returned value is stored in [variable]
         *
         * @see LLVM.LLVMBuildFCmp
         */
        public fun createFCmp(
            lhs: Value,
            predicate: RealPredicate,
            rhs: Value,
            variable: String
        ): FCmpInstruction {
            val inst = LLVM.LLVMBuildFCmp(
                ref,
                predicate.value,
                lhs.ref,
                rhs.ref,
                variable
            )

            return FCmpInstruction(inst)
        }

        /**
         * Build a phi instruction
         *
         * The returned value is stored in [variable]
         *
         * @see LLVM.LLVMBuildPhi
         */
        public fun createPhi(
            incoming: Type,
            variable: String
        ): PhiInstruction {
            val inst = LLVM.LLVMBuildPhi(
                ref,
                incoming.ref,
                variable
            )

            return PhiInstruction(inst)
        }

        /**
         * Build a call instruction
         *
         * This will call [function] with the supplied [arguments]. The
         * result will be bound to the a variable with the name [variable].
         *
         * To prevent binding the result to a variable, either omit the
         * [variable] argument, or set it to `""`
         *
         * @see LLVM.LLVMBuildCall2
         */
        public fun createCall(
            function: FunctionValue,
            arguments: List<Value>,
            variable: String = ""
        ): CallInstruction {
            val args = PointerPointer(*arguments.map { it.ref }.toTypedArray())
            val inst = LLVM.LLVMBuildCall2(
                ref,
                function.getType().ref,
                function.ref,
                args,
                arguments.size,
                variable
            )

            return CallInstruction(inst)
        }

        /**
         * Build a select instruction
         *
         * A select instruction picks a value, either [then] else
         * [otherwise], based on the [condition]
         *
         * The returned value is stored in [variable]
         *
         * @see LLVM.LLVMBuildSelect
         */
        public fun createSelect(
            condition: Value,
            then: Value,
            otherwise: Value,
            variable: String
        ): SelectInstruction {
            val inst = LLVM.LLVMBuildSelect(
                ref,
                condition.ref,
                then.ref,
                otherwise.ref,
                variable
            )

            return SelectInstruction(inst)
        }

        /**
         * Build a va_arg instruction
         *
         * va_arg receives the current value in the [list] which is of type
         * [type] and advances the "iterator"
         *
         * The returned value is stored in [variable]
         *
         * @see LLVM.LLVMBuildVAArg
         */
        public fun createVAArg(
            list: Value,
            type: Type,
            variable: String
        ): VAArgInstruction {
            val inst = LLVM.LLVMBuildVAArg(
                ref,
                list.ref,
                type.ref,
                variable
            )

            return VAArgInstruction(inst)
        }

        /**
         * Build an extractelement instruction
         *
         * Extracts the element at [index] in the [vector]
         *
         * The returned value is stored in [variable]
         *
         * @see LLVM.LLVMBuildExtractElement
         */
        public fun createExtractElement(
            vector: Value,
            index: Value,
            variable: String
        ): ExtractElementInstruction {
            val inst = LLVM.LLVMBuildExtractElement(
                ref,
                vector.ref,
                index.ref,
                variable
            )

            return ExtractElementInstruction(inst)
        }

        /**
         * Build an insertelement instruction
         *
         * Inserts the [element] into [vector] at [index]. The modified
         * vector is returned.
         *
         * The returned value is stored in [variable]
         *
         * @see LLVM.LLVMBuildInsertElement
         */
        public fun createInsertElement(
            vector: Value,
            index: Value,
            element: Value,
            variable: String
        ): InsertElementInstruction {
            val inst = LLVM.LLVMBuildInsertElement(
                ref,
                vector.ref,
                element.ref,
                index.ref,
                variable
            )

            return InsertElementInstruction(inst)
        }

        /**
         * Build a shufflevector instruction
         *
         * The returned value is stored in [variable]
         *
         * @see LLVM.LLVMBuildShuffleVector
         */
        public fun createShuffleVector(
            v1: Value,
            v2: Value,
            mask: Value,
            variable: String
        ): ShuffleVectorInstruction {
            val inst = LLVM.LLVMBuildShuffleVector(
                ref,
                v1.ref,
                v2.ref,
                mask.ref,
                variable
            )

            return ShuffleVectorInstruction(inst)
        }

        /**
         * Build a extractvalue instruction
         *
         * Extract the element at [index] in the [aggregate]
         *
         * The returned value is stored in [variable]
         *
         * @see LLVM.LLVMBuildExtractValue
         */
        public fun createExtractValue(
            aggregate: Value,
            index: Int,
            variable: String
        ): ExtractValueInstruction {
            val inst = LLVM.LLVMBuildExtractValue(
                ref,
                aggregate.ref,
                index,
                variable
            )

            return ExtractValueInstruction(inst)
        }

        /**
         * Build an insertvalue instruction
         *
         * Insert [value] into the [aggregate] at [index]. The modified
         * aggregate is returned.
         *
         * The returned value is stored in [variable]
         *
         * @see LLVM.LLVMBuildInsertElement
         */
        public fun createInsertValue(
            aggregate: Value,
            index: Int,
            value: Value,
            variable: String
        ): InsertValueInstruction {
            val inst = LLVM.LLVMBuildInsertValue(
                ref,
                aggregate.ref,
                value.ref,
                index,
                variable
            )

            return InsertValueInstruction(inst)
        }

        /**
         * Build a fence instruction
         *
         * The returned value is stored in [variable]
         *
         * @see LLVM.LLVMBuildFence
         */
        public fun createFence(
            ordering: AtomicOrdering,
            singleThread: Boolean,
            variable: String
        ): FenceInstruction {
            val inst = LLVM.LLVMBuildFence(
                ref,
                ordering.value,
                singleThread.toLLVMBool(),
                variable
            )

            return FenceInstruction(inst)
        }

        /**
         * Build an atomicrmw instruction
         *
         * @see LLVM.LLVMBuildAtomicRMW
         */
        public fun createAtomicRMW(
            operator: AtomicRMWBinaryOperation,
            pointer: Value,
            value: Value,
            ordering: AtomicOrdering,
            singleThread: Boolean
        ): AtomicRMWInstruction {
            val inst = LLVM.LLVMBuildAtomicRMW(
                ref,
                operator.value,
                pointer.ref,
                value.ref,
                ordering.value,
                singleThread.toLLVMBool()
            )

            return AtomicRMWInstruction(inst)
        }

        /**
         * Build a cmpxchg instruction
         *
         * @see LLVM.LLVMBuildAtomicCmpXchg
         */
        public fun createAtomicCmpXchg(
            pointer: Value,
            compareTo: Value,
            replacement: Value,
            successOrdering: AtomicOrdering,
            failureOrdering: AtomicOrdering,
            singleThread: Boolean
        ): AtomicCmpXchgInstruction {
            val inst = LLVM.LLVMBuildAtomicCmpXchg(
                ref,
                pointer.ref,
                compareTo.ref,
                replacement.ref,
                successOrdering.value,
                failureOrdering.value,
                singleThread.toLLVMBool()
            )

            return AtomicCmpXchgInstruction(inst)
        }
    }
    //endregion InstructionBuilders

    override fun dispose() {
        require(valid) { "Cannot dispose object twice" }

        valid = false

        LLVM.LLVMDisposeBuilder(ref)
    }
}
