package org.aion.avm.core.abicompiler;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.Type;

public class ABICompilerClassVisitor extends ClassVisitor {
    private boolean isMain;
    private boolean hasMain = false;
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

                signatures.add(mv.getSignature());
            }
        }
        return signatures;
    }

    @Override
    public MethodVisitor visitMethod(
            int access, String name, String descriptor, String signature, String[] exceptions) {
        if (name.equals("main") && ((access & Opcodes.ACC_PUBLIC) != 0)) {
            hasMain = true;
        }
        ABICompilerMethodVisitor mv = new ABICompilerMethodVisitor(access, name, descriptor);
        methodVisitors.add(mv);
        super.visitMethod(access, name, descriptor, signature, exceptions);
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
                    Type.getType("Lorg/aion/avm/core/abicompiler/ABICompilerTestTarget;"));
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
        super.visitEnd();
    }
}
