package org.aion.avm.core.abicompiler;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ABICompilerClassVisitor extends ClassVisitor {
    private boolean hasMain = false;
    private List<ABICompilerMethodVisitor> methodVisitors = new ArrayList<>();

    public ABICompilerClassVisitor() {
        super(Opcodes.ASM6);
    }

    public boolean hasMain() {
        return hasMain;
    }

    public List<String> getCallables() {
        List<String> signatures = new ArrayList<>();
        for (ABICompilerMethodVisitor mv : methodVisitors) {
            if(mv.isCallable()) {
                signatures.add(mv.getSignature());
            }
        }
        return signatures;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        if (name.equals("main") && ((access & Opcodes.ACC_PUBLIC) != 0)) {
            hasMain = true;
        }
        ABICompilerMethodVisitor mv = new ABICompilerMethodVisitor(access, name, descriptor);
        methodVisitors.add(mv);
        return mv;
    }
}
