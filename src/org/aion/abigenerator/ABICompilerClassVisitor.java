package org.aion.abigenerator;

import org.objectweb.asm.*;

import java.util.ArrayList;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;

public class ABICompilerClassVisitor extends ClassVisitor {
    private boolean isMain;
    private boolean hasMain = false;
    private String className;
    private List<ABICompilerMethodVisitor> methodVisitors = new ArrayList<>();

    public ABICompilerClassVisitor(ClassWriter cw) {
        super(Opcodes.ASM6, cw);
    }

    public void setMain() {
        isMain = true;
    }

    public List<String> getCallables() {
        List<String> signatures = new ArrayList<>();
        for (ABICompilerMethodVisitor mv : methodVisitors) {
            signatures.add(this.className + ": " + mv.getSignature());
        }
        return signatures;
    }

    public List<ABICompilerMethodVisitor> getCallableMethodVisitors() {
        List<ABICompilerMethodVisitor> callableMethodVisitors = new ArrayList<>();
        for (ABICompilerMethodVisitor mv : methodVisitors) {
            if (mv.isCallable()) {
                callableMethodVisitors.add(mv);
            }
        }
        return callableMethodVisitors;
    }

    @Override
    public void visit(int version, int access, java.lang.String name, java.lang.String signature, java.lang.String superName, java.lang.String[] interfaces) {
        this.className = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(
            int access, String name, String descriptor, String signature, String[] exceptions) {
        if (name.equals("main") && ((access & Opcodes.ACC_PUBLIC) != 0)) {
            hasMain = true;
        }
        ABICompilerMethodVisitor mv = new ABICompilerMethodVisitor(access, name, descriptor,
                super.visitMethod(access, name, descriptor, signature, exceptions));
        if (isMain) {
            methodVisitors.add(mv);
        }
        return mv;
    }

    @Override
    public void visitEnd() {
        if (isMain && !hasMain) {
            MethodVisitor methodVisitor =
                    super.visitMethod(ACC_PUBLIC | ACC_STATIC, "main", "()[B", null, null);
            methodVisitor.visitCode();
            Label label0 = new Label();
            methodVisitor.visitLabel(label0);
            methodVisitor.visitMethodInsn(INVOKESTATIC, "org/aion/avm/api/BlockchainRuntime", "getData", "()[B", false);
            methodVisitor.visitVarInsn(ASTORE, 0);
            Label label1 = new Label();
            methodVisitor.visitLabel(label1);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitMethodInsn(INVOKESTATIC, "org/aion/avm/api/ABIDecoder", "decodeMethodName", "([B)Ljava/lang/String;", false);
            methodVisitor.visitVarInsn(ASTORE, 1);
            Label label2 = new Label();
            methodVisitor.visitLabel(label2);
            methodVisitor.visitMethodInsn(INVOKESTATIC, "org/aion/avm/api/BlockchainRuntime", "getData", "()[B", false);
            methodVisitor.visitMethodInsn(INVOKESTATIC, "org/aion/avm/api/ABIDecoder", "decodeArguments", "([B)[Ljava/lang/Object;", false);
            methodVisitor.visitVarInsn(ASTORE, 2);

//            for (String callableMethod : this.getCallables()) {
//
//            }

            ABICompilerMethodVisitor callableMethod = this.getCallableMethodVisitors().get(0);

            Label label3 = new Label();
            methodVisitor.visitLabel(label3);
            methodVisitor.visitVarInsn(ALOAD, 1);
            methodVisitor.visitLdcInsn(callableMethod.getMethodName());
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
            Label label4 = new Label();
            methodVisitor.visitJumpInsn(IFEQ, label4);
            Label label5 = new Label();
            methodVisitor.visitLabel(label5);
            methodVisitor.visitMethodInsn(INVOKESTATIC, className, callableMethod.getMethodName(), callableMethod.getDescriptor(), false);
            methodVisitor.visitMethodInsn(INVOKESTATIC, "org/aion/avm/api/ABIEncoder", "encodeOneObject", "(Ljava/lang/Object;)[B", false);
            methodVisitor.visitInsn(ARETURN);
            methodVisitor.visitLabel(label4);
            methodVisitor.visitFrame(Opcodes.F_APPEND, 3, new Object[]{"[B", "java/lang/String", "[Ljava/lang/Object;"}, 0, null);
            methodVisitor.visitInsn(ACONST_NULL);
            methodVisitor.visitInsn(ARETURN);
            Label label6 = new Label();
            methodVisitor.visitLabel(label6);
            methodVisitor.visitLocalVariable("inputBytes", "[B", null, label1, label6, 0);
            methodVisitor.visitLocalVariable("methodName", "Ljava/lang/String;", null, label2, label6, 1);
            methodVisitor.visitLocalVariable("argValues", "[Ljava/lang/Object;", null, label3, label6, 2);
            methodVisitor.visitMaxs(2, 3);
            methodVisitor.visitEnd();
        }
        if (!isMain && hasMain) {
            throw new IllegalMainMethodsException("Non-main class can't have main() method!");
        }
        super.visitEnd();
    }
}
