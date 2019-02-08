package org.aion.avm.core.abicompiler;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ABICompilerClassVisitor extends ClassVisitor {
    public ABICompilerClassVisitor() {
        super(Opcodes.ASM6);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        return new org.aion.avm.core.abicompiler.ABICompilerMethodVisitor(access, name, descriptor);
    }
}
