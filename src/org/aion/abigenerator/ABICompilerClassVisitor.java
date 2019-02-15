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
            if (mv.isCallable()) {

                signatures.add(this.className + ": " + mv.getSignature());
            }
        }
        return signatures;
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
            methodVisitor.visitLineNumber(8, label0);
            methodVisitor.visitLdcInsn(
                    Type.getType("L" + this.className.replaceAll("/", ".") + ";"));
            methodVisitor.visitMethodInsn(
                    INVOKESTATIC, "org/aion/avm/api/BlockchainRuntime", "getData", "()[B", false);
            methodVisitor.visitMethodInsn(
                    INVOKESTATIC,
                    "org/aion/avm/api/ABIDecoder",
                    "decodeAndRunWithClass",
                    "(Ljava/lang/Class;[B)[B",
                    false);
            methodVisitor.visitInsn(ARETURN);
            methodVisitor.visitMaxs(2, 0);
            methodVisitor.visitEnd();
        }
        if (!isMain && hasMain) {
            throw new IllegalMainMethodsException("Non-main class can't have main() method!");
        }
        super.visitEnd();
    }
}
