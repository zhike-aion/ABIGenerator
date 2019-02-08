package org.aion.avm.core.abicompiler;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.StringJoiner;

public class ABICompilerMethodVisitor extends MethodVisitor {
    int access;
    String methodName;
    String methodDescriptor;

    public ABICompilerMethodVisitor(int access, String methodName, String methodDescriptor) {
        super(Opcodes.ASM6);
        this.access = access;
        this.methodName = methodName;
        this.methodDescriptor = methodDescriptor;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        String methodReadable = "";
        boolean isPublic = (this.access & Opcodes.ACC_PUBLIC) != 0;
        if (isPublic && Type.getType(descriptor).getClassName().equals(Callable.class.getName())) {
            StringJoiner arguments = new StringJoiner(", ");
            for (Type type : Type.getArgumentTypes(this.methodDescriptor)) {
                arguments.add(type.getClassName());
            }
            boolean isStatic = (this.access & Opcodes.ACC_STATIC) != 0;
            String returnType = Type.getReturnType(this.methodDescriptor).getClassName();
            methodReadable = (isPublic ? "public " : "")
                    + (isStatic ? "static " : "")
                    + returnType + " "
                    + this.methodName + "("
                    + arguments.toString()
                    + ")";
        }
        System.out.println(methodReadable);
        return null;
    }
}