package org.aion.abigenerator;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;

import org.aion.avm.api.ABIDecoder;
import org.aion.avm.api.ABIEncoder;
import org.aion.avm.api.Address;
import org.aion.avm.core.dappreading.JarBuilder;
import org.aion.avm.core.util.AvmRule;
import org.aion.avm.core.util.CodeAndArguments;
import org.aion.kernel.AvmTransactionResult;
import org.aion.vm.api.interfaces.TransactionResult;
import org.junit.*;
import org.objectweb.asm.*;

import static org.junit.Assert.assertEquals;
import static org.objectweb.asm.Opcodes.*;

public class RandomTest {
    @Rule
    public AvmRule avmRule = new AvmRule(true);

    private static ABICompiler compiler;

    private static final long ENERGY_LIMIT = 10_000_000L;
    private static final long ENERGY_PRICE = 1L;


    @Before
    public void setup() {
        compiler = new ABICompiler();
    }

    private Address installTestDApp() {

        byte[] jar =
                JarBuilder.buildJarForExplicitClassNamesAndBytecode(
                        compiler.getMainClassName(),
                        compiler.getMainClassBytes(),
                        compiler.getClassMap());
        byte[] txData = new CodeAndArguments(jar, new byte[0]).encodeToBytes();

        // Deploy.
        TransactionResult createResult =
                avmRule.deploy(
                        avmRule.getPreminedAccount(),
                        BigInteger.ZERO,
                        txData,
                        ENERGY_LIMIT,
                        ENERGY_PRICE)
                        .getTransactionResult();
        assertEquals(AvmTransactionResult.Code.SUCCESS, createResult.getResultCode());
        return new Address(createResult.getReturnData());
    }

    private Object callStatic(Address dapp, String methodName, Object... arguments) {
        byte[] argData = ABIEncoder.encodeMethodArguments(methodName, arguments);
        TransactionResult result =
                avmRule.call(
                        avmRule.getPreminedAccount(),
                        dapp,
                        BigInteger.ZERO,
                        argData,
                        ENERGY_LIMIT,
                        ENERGY_PRICE)
                        .getTransactionResult();
        assertEquals(AvmTransactionResult.Code.SUCCESS, result.getResultCode());
        return ABIDecoder.decodeOneObject(result.getReturnData());
    }

    @Test
    public void testArgumentsWithFixedTypesRandomValues() {
        RandomArgumentsGenerator argsGenerator = new RandomArgumentsGenerator();
        argsGenerator.addFixedTypesAndRandomValues(18);

        String className = "RandomClass";

        String methodName = argsGenerator.getRandomString(9);

        System.out.println(String.format("%s.%s%s --- %s arguments", className , methodName,
                        RandomTest.getMethodDescriptor(String.join("", argsGenerator.getArgDescriptors())), argsGenerator.getNumberOfArgs()));

        byte[] jar = JarBuilder.buildJarForExplicitClassNamesAndBytecode(className, createClass(methodName, argsGenerator), new HashMap<>());
        compiler.compile(new ByteArrayInputStream(jar));

        Address dapp = installTestDApp();

        byte[] result = (byte[]) callStatic(dapp, methodName, argsGenerator.getVarArgs());
        byte[] expected = ABIEncoder.encodeMethodArguments("",argsGenerator.getVarArgs());
        Assert.assertTrue(Arrays.equals(expected, result));
    }

    @Test
    public void testArgumentsWithRandomTypesRandomValue() {
        int times = 100;
        for(int i = 0; i < times; ++i) {
            testRandom();
        }
    }

    private void testRandom() {
        RandomArgumentsGenerator argsGenerator = new RandomArgumentsGenerator();
        argsGenerator.addRandomTypeAndRandomValues(50);

        String className = "RandomClass";

        String methodName = argsGenerator.getRandomString(9);

        System.out.println(String.format("%s.%s%s --- %s arguments", className , methodName,
                RandomTest.getMethodDescriptor(String.join("", argsGenerator.getArgDescriptors())), argsGenerator.getNumberOfArgs()));

        byte[] jar = JarBuilder.buildJarForExplicitClassNamesAndBytecode(className, createClass(methodName, argsGenerator), new HashMap<>());
        compiler.compile(new ByteArrayInputStream(jar));

        Address dapp = installTestDApp();

        byte[] result = (byte[]) callStatic(dapp, methodName, argsGenerator.getVarArgs());
        byte[] expected = ABIEncoder.encodeMethodArguments("",argsGenerator.getVarArgs());
        Assert.assertTrue(Arrays.equals(expected, result));
    }

    private byte[] createClass(String methodName, RandomArgumentsGenerator argsGenerator) {
        ClassWriter classWriter = new ClassWriter(0);
        classWriter.visit(V10, ACC_PUBLIC + ACC_SUPER, "RandomClass", null, "java/lang/Object", null);

        MethodVisitor methodVisitor = classWriter.visitMethod(ACC_PUBLIC | ACC_STATIC,
                methodName,
                RandomTest.getMethodDescriptor(String.join("", argsGenerator.getArgDescriptors())),
                null,
                null);
        AnnotationVisitor annotationVisitor = methodVisitor.visitAnnotation("Lorg/aion/abigenerator/Callable;", false);
        annotationVisitor.visitEnd();
        methodVisitor.visitCode();

        Label label0 = new Label();
        methodVisitor.visitLabel(label0);
        methodVisitor.visitLdcInsn("");

        methodVisitor.visitIntInsn(BIPUSH, argsGenerator.getArgTypes().length);
        methodVisitor.visitTypeInsn(ANEWARRAY, "java/lang/Object");
        int index = 0;
        for(int i = 0; i < argsGenerator.getArgDescriptors().length; ++i, ++index) {
            methodVisitor.visitInsn(DUP);
            methodVisitor.visitIntInsn(BIPUSH, i);
            loadLocalVariableToStack(methodVisitor, argsGenerator.getArgTypes()[i], index);
            if(argsGenerator.getArgTypes()[i] == Type.LONG || argsGenerator.getArgTypes()[i] == Type.DOUBLE) index++;
            castArgumentType(methodVisitor, argsGenerator.getArgTypes()[i]);
            methodVisitor.visitInsn(AASTORE);
        }

        methodVisitor.visitMethodInsn(INVOKESTATIC, "org/aion/avm/api/ABIEncoder", "encodeMethodArguments", "(Ljava/lang/String;[Ljava/lang/Object;)[B", false);
        methodVisitor.visitInsn(ARETURN);

        Label label1 = new Label();
        methodVisitor.visitLabel(label1);

        index = 0;
        for(int i = 0; i < argsGenerator.getArgDescriptors().length; ++i, ++index) {
            System.out.println(String.format("%s %s %s", argsGenerator.getArgTypes()[i], getTypeDescriptor(argsGenerator.getArgTypes()[i]), argsGenerator.getVarArgs()[i]));
            methodVisitor.visitLocalVariable("v" + String.valueOf(i+1), getTypeDescriptor(argsGenerator.getArgTypes()[i]), null, label0, label1, index);
            if(argsGenerator.getArgTypes()[i] == Type.LONG || argsGenerator.getArgTypes()[i] == Type.DOUBLE) index++;
        }

        methodVisitor.visitMaxs(index, index);
        methodVisitor.visitEnd();

/*        DataOutputStream dout = null;
        try {
            dout = new DataOutputStream(
                    new FileOutputStream("RandomClass.class"));
            dout.write(classWriter.toByteArray());
            dout.close();
        } catch (Exception e) {
            e.printStackTrace();
        }*/

        return classWriter.toByteArray();
    }

    private void loadLocalVariableToStack(MethodVisitor methodVisitor, Type type, int index) {
        switch (type) {
            case BYTE:
            case BOOLEAN:
            case CHAR:
            case SHORT:
            case INT:
                methodVisitor.visitVarInsn(ILOAD, index);
                break;
            case LONG:
                methodVisitor.visitVarInsn(LLOAD, index);
                break;
            case FLOAT:
                methodVisitor.visitVarInsn(FLOAD, index);
                break;
            case DOUBLE:
                methodVisitor.visitVarInsn(DLOAD, index);
                break;
            default:
                //other
                methodVisitor.visitVarInsn(ALOAD, index);
                break;
        }
    }

    private void castArgumentType(MethodVisitor methodVisitor, Type type) {
        switch (type) {
            case BYTE:
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
                break;
            case BOOLEAN:
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
                break;
            case CHAR:
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
                break;
            case SHORT:
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
                break;
            case INT:
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
                break;
            case LONG:
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
                break;
            case FLOAT:
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
                break;
            case DOUBLE:
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
                break;
            default:
                //other
                break;
        }
    }

    private static String getMethodDescriptor(String argumentsDescriptor) {
        return String.format("(%s)[B", argumentsDescriptor);
    }

    private String getTypeDescriptor(Type type) {
        String t = "";
        switch (type) {
            case BYTE:
                t = "B";
                break;
            case BOOLEAN:
                t = "Z";
                break;
            case CHAR:
                t = "C";
                break;
            case SHORT:
                t = "S";
                break;
            case INT:
                t = "I";
                break;
            case LONG:
                t = "J";
                break;
            case FLOAT:
                t = "F";
                break;
            case DOUBLE:
                t = "D";
                break;
            case BYTE_ARRAY:
                t = "[B";
                break;
            case BOOL_ARRAY:
                t = "[Z";
                break;
            case CHAR_ARRAY:
                t = "[C";
                break;
            case SHORT_ARRAY:
                t = "[S";
                break;
            case INT_ARRAY:
                t = "[I";
                break;
            case LONG_ARRAY:
                t = "[J";
                break;
            case FLOAT_ARRAY:
                t = "[F";
                break;
            case DOUBLE_ARRAY:
                t = "[D";
                break;
            case STRING:
                t = "Ljava/lang/String;";
                break;
            case INT_ARRAY_2D:
                t = "[[I";
                break;
        }
        return t;
    }
}
