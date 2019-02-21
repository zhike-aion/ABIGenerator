package org.aion.abigenerator;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.StringJoiner;

public class ABICompilerMethodVisitor extends MethodVisitor {
    private int access;
    private String methodName;
    private String methodDescriptor;
    private boolean callable = false;

    public boolean isCallable() {
        return callable;
    }

    // Should only be called on public methods
    public String getSignature() {
        String signature = "";

        StringJoiner arguments = new StringJoiner(", ");
        for (Type type : Type.getArgumentTypes(this.methodDescriptor)) {
            arguments.add(type.getClassName());
        }
        String returnType = Type.getReturnType(this.methodDescriptor).getClassName();
        signature = ("public ")
                + ("static ")
                + returnType + " "
                + this.methodName + "("
                + arguments.toString()
                + ")";
        return signature;
    }

    public ABICompilerMethodVisitor(int access, String methodName, String methodDescriptor, MethodVisitor mv) {
        super(Opcodes.ASM6, mv);
        this.access = access;
        this.methodName = methodName;
        this.methodDescriptor = methodDescriptor;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        boolean isPublic = (this.access & Opcodes.ACC_PUBLIC) != 0;
        boolean isStatic = (this.access & Opcodes.ACC_STATIC) != 0;
        if (!isPublic && !isStatic && Type.getType(descriptor).getClassName().equals(Callable.class.getName())) {
            throw new CallableMismatchException("Annotation 'Callable' mismatches non-public && non-static access modifiers(protected/private)!");
        }
        if (isPublic && isStatic && Type.getType(descriptor).getClassName().equals(Callable.class.getName())) {
            callable = true;
        }
        return null;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getDescriptor() {
        return methodDescriptor;
    }
}