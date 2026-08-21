package app.revanced.patches.all.misc.transformation

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.extensions.replaceInstruction
import app.revanced.util.fiveRegisterInstructionRegistersAsString
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

typealias Instruction35cInfo = Triple<IMethodCall, Instruction35c, Int>

interface IMethodCall {
    val definedClassName: String
    val methodName: String
    val methodParams: Array<String>
    val methodReturnType: String

    /**
     * Replaces an invoke-virtual instruction with an invoke-static instruction,
     * which calls a static replacement method in the respective extension class.
     * The method definition in the extension class is expected to be the same,
     * except that the method should be static and take as a first parameter
     * an instance of the class, in which the original method was defined in.
     *
     * Example:
     *
     * original method: Window#setFlags(int, int)
     *
     * replacement method: Extension#setFlags(Window, int, int)
     */
    fun replaceInvokeVirtualWithExtension(
        definingClassDescriptor: String,
        method: MutableMethod,
        instructionIndex: Int,
    ) {
        val registers = method.fiveRegisterInstructionRegistersAsString(instructionIndex)
        val replacementMethod =
            "$methodName(${definedClassName}${methodParams.joinToString(separator = "")})$methodReturnType"

        method.replaceInstruction(
            instructionIndex,
            "invoke-static { $registers }, $definingClassDescriptor->$replacementMethod",
        )
    }
}

inline fun <reified E> fromMethodReference(methodReference: MethodReference)
        where E : Enum<E>, E : IMethodCall =
    enumValues<E>().firstOrNull { search ->
        search.definedClassName == methodReference.definingClass &&
            search.methodName == methodReference.name &&
            methodReference.parameterTypes.toTypedArray().contentEquals(search.methodParams) &&
            search.methodReturnType == methodReference.returnType
    }

inline fun <reified E> filterMapInstruction35c(
    extensionClassDescriptorPrefix: String,
    classDef: ClassDef,
    instruction: Instruction,
    instructionIndex: Int,
): Instruction35cInfo? where E : Enum<E>, E : IMethodCall {
    if (classDef.startsWith(extensionClassDescriptorPrefix)) {
        // avoid infinite recursion
        return null
    }

    if (instruction.opcode != Opcode.INVOKE_VIRTUAL) {
        return null
    }

    val invokeInstruction = instruction as Instruction35c
    val methodRef = invokeInstruction.reference as MethodReference
    val methodCall = fromMethodReference<E>(methodRef) ?: return null

    return Instruction35cInfo(methodCall, invokeInstruction, instructionIndex)
}
