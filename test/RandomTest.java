
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

import org.aion.abigenerator.ABICompiler;
import org.aion.avm.abi.internal.ABICodec;
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
        setupRandomArgumentsEnv();
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
    public void testFixedArguments() {
        String className = "RandomClass";
        String methodName = getRandomString(9);
        methodName = "test";

        for (int i = 0; i < numberOfArgs; i++) {
            addRandomType(i+1, i);
        }

        byte[] jar = JarBuilder.buildJarForExplicitClassNamesAndBytecode(className, createClass(methodName), new HashMap<>());
        compiler.compile(new ByteArrayInputStream(jar));

        Address dapp = installTestDApp();

        byte[] result = (byte[]) callStatic(dapp, methodName, this.varArgs);
        byte[] expected = ABIEncoder.encodeMethodArguments("",this.varArgs);
        Assert.assertTrue(Arrays.equals(expected, result));
    }

    @Test
    public void testRandoms() {
        int times = 10;
        for(int i = 0; i < times; ++i) {
            testRandom();
        }
    }

    private void testRandom() {
        String className = "RandomClass";
        String methodName = getRandomString(9);
        methodName = "test";

        for (int i = 0; i < numberOfArgs; i++) {
            addRandomType(random.nextInt(18) + 1, i);
        }

        byte[] jar = JarBuilder.buildJarForExplicitClassNamesAndBytecode(className, createClass(methodName), new HashMap<>());
        compiler.compile(new ByteArrayInputStream(jar));

        Address dapp = installTestDApp();

        byte[] result = (byte[]) callStatic(dapp, methodName, this.varArgs);
        byte[] expected = ABIEncoder.encodeMethodArguments("",this.varArgs);
        Assert.assertTrue(Arrays.equals(expected, result));
    }

    private byte[] createClass(String methodName) {
        ClassWriter classWriter = new ClassWriter(0);
        classWriter.visit(V10, ACC_PUBLIC + ACC_SUPER, "RandomClass", null, "java/lang/Object", null);

        MethodVisitor methodVisitor = classWriter.visitMethod(ACC_PUBLIC | ACC_STATIC,
                methodName,
                RandomTest.getMethodDescriptor(String.join("", this.argDescriptors)),
                null,
                null);
        AnnotationVisitor annotationVisitor = methodVisitor.visitAnnotation("Lorg/aion/abigenerator/Callable;", false);
        annotationVisitor.visitEnd();
        methodVisitor.visitCode();

        Label label0 = new Label();
        methodVisitor.visitLabel(label0);
        methodVisitor.visitLdcInsn("");

        methodVisitor.visitIntInsn(BIPUSH, this.argTypes.length);
        methodVisitor.visitTypeInsn(ANEWARRAY, "java/lang/Object");
        int index = 0;
        for(int i = 0; i < this.argDescriptors.length; ++i, ++index) {
            methodVisitor.visitInsn(DUP);
            methodVisitor.visitIntInsn(BIPUSH, i);
            loadLocalVariableToStack(methodVisitor, this.argTypes[i], index);
            if(this.argTypes[i] == 6 || this.argTypes[i] == 8) index++;
            castArgumentType(methodVisitor, this.argTypes[i]);
            methodVisitor.visitInsn(AASTORE);
        }

        methodVisitor.visitMethodInsn(INVOKESTATIC, "org/aion/avm/api/ABIEncoder", "encodeMethodArguments", "(Ljava/lang/String;[Ljava/lang/Object;)[B", false);
        methodVisitor.visitInsn(ARETURN);

        Label label1 = new Label();
        methodVisitor.visitLabel(label1);

        index = 0;
        for(int i = 0; i < this.argDescriptors.length; ++i, ++index) {
            System.out.println(String.format("%d %s %s", this.argTypes[i], getTypeDescriptor(this.argTypes[i]), this.varArgs[i].toString()));
            methodVisitor.visitLocalVariable("v" + String.valueOf(i+1), getTypeDescriptor(this.argTypes[i]), null, label0, label1, index);
            if(this.argTypes[i] == 6 || this.argTypes[i] == 8) index++;
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

    private void loadLocalVariableToStack(MethodVisitor methodVisitor, int type, int index) {
        switch (type) {
            case 1: //byte
            case 2: //boolean
            case 3: //char
            case 4: //short
            case 5: //int
                methodVisitor.visitVarInsn(ILOAD, index);
                break;
            case 6:
                //long
                methodVisitor.visitVarInsn(LLOAD, index);
                break;
            case 7:
                //float
                methodVisitor.visitVarInsn(FLOAD, index);
                break;
            case 8:
                //double
                methodVisitor.visitVarInsn(DLOAD, index);
                break;
            default:
                //other
                methodVisitor.visitVarInsn(ALOAD, index);
                break;
        }
    }

    private void castArgumentType(MethodVisitor methodVisitor, int type) {
        switch (type) {
            case 1:
                //byte
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
                break;
            case 2:
                //boolean
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
                break;
            case 3:
                //char
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
                break;
            case 4:
                //short
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
                break;
            case 5:
                //int
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
                break;
            case 6:
                //long
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
                break;
            case 7:
                //float
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
                break;
            case 8:
                //double
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
                break;
            default:
                break;
        }
    }

    private static String getMethodDescriptor(String argumentsDescriptor) {
        return String.format("(%s)[B", argumentsDescriptor);
    }

    private String getRandomArgumentsDescriptor(int argumentsNumber) {

        Random random = new Random();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\"(");
        for (int i = 0; i < argumentsNumber; ++i) {
            stringBuilder.append(getTypeDescriptor(random.nextInt(18)));
        }
        stringBuilder.append(")Z");
        System.out.println(stringBuilder.toString());
        return stringBuilder.toString();
    }

    private String getTypeDescriptor(int type) {
        String t = "";
        switch (type) {
            case 1:
                //byte
                t = "B";
                break;
            case 2:
                //bool
                t = "Z";
                break;
            case 3:
                //char
                t = "C";
                break;
            case 4:
                //short
                t = "S";
                break;
            case 5:
                //int
                t = "I";
                break;
            case 6:
                //long
                t = "J";
                break;
            case 7:
                //float
                t = "F";
                break;
            case 8:
                //double
                t = "D";
                break;
            case 9:
                //byte array
                t = "[B";
                break;
            case 10:
                //bool array
                t = "[Z";
                break;
            case 11:
                //char array
                t = "[C";
                break;
            case 12:
                //short array
                t = "[S";
                break;
            case 13:
                //int array
                t = "[I";
                break;
            case 14:
                //long array
                t = "[J";
                break;
            case 15:
                //float array
                t = "[F";
                break;
            case 16:
                //double array
                t = "[D";
                break;
            case 17:
                //string
                t = "Ljava/lang/String;";
                break;
            case 18:
                //int array 2d
                t = "[[I";
                break;
        }
        return t;
    }

    private static Random random;

    private int numberOfArgs;
    private Object[] varArgs;
    private ABICodec.Tuple[] argTuples;
    private int[] argTypes;
    private String[] argDescriptors;
    private static final int upperBoundOfNumOfArgs = 10;

    public void setupRandomArgumentsEnv() {
        random = new Random();
        long seed = random.nextLong();
        System.out.println("Test seed is " + seed);
        random.setSeed(seed);

        numberOfArgs = random.nextInt(RandomTest.upperBoundOfNumOfArgs);
        varArgs = new Object[numberOfArgs];
        argTuples = new ABICodec.Tuple[numberOfArgs];
        argTypes = new int[numberOfArgs];
        argDescriptors = new String[numberOfArgs];
    }

    private static String getRandomString(int max) {
        int length = random.nextInt(max) + 5;
        String allChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890";

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(allChars.charAt(random.nextInt(62)));
        }
        return sb.toString();
    }

    private void addByte(int i) {
        byte b = (byte) random.nextInt();
        varArgs[i] = b;
        argTuples[i] = new ABICodec.Tuple(Byte.class, b);
    }

    private void addBool(int i) {
        boolean b = random.nextBoolean();
        varArgs[i] = b;
        argTuples[i] = new ABICodec.Tuple(Boolean.class, b);
    }

    private void addChar(int i) {
        char b = (char) random.nextInt();
        varArgs[i] = b;
        argTuples[i] = new ABICodec.Tuple(Character.class, b);
    }

    private void addShort(int i) {
        short b = (short) random.nextInt();
        varArgs[i] = b;
        argTuples[i] = new ABICodec.Tuple(Short.class, b);
    }

    private void addInt(int i) {
        int b = random.nextInt();
        varArgs[i] = b;
        argTuples[i] = new ABICodec.Tuple(Integer.class, b);
    }

    private void addLong(int i) {
        long b = random.nextLong();
        varArgs[i] = b;
        argTuples[i] = new ABICodec.Tuple(Long.class, b);
    }

    private void addFloat(int i) {
        float b = random.nextFloat();
        varArgs[i] = b;
        argTuples[i] = new ABICodec.Tuple(Float.class, b);
    }

    private void addDouble(int i) {
        double b = random.nextDouble();
        varArgs[i] = b;
        argTuples[i] = new ABICodec.Tuple(Double.class, b);
    }

    private void addByteArray(int i) {
        byte[] b = new byte[random.nextInt(50)];
        random.nextBytes(b);
        varArgs[i] = b;
        argTuples[i] = new ABICodec.Tuple(byte[].class, b);
    }

    private void addBoolArray(int i) {
        int len = random.nextInt(50);
        boolean[] b = new boolean[len];
        for (int j = 0; j < len; j++) {
            b[j] = random.nextBoolean();
        }
        varArgs[i] = b;
        argTuples[i] = new ABICodec.Tuple(boolean[].class, b);
    }

    private void addCharArray(int i) {
        int len = random.nextInt(50);
        char[] b = new char[len];
        for (int j = 0; j < len; j++) {
            b[j] = (char) random.nextInt();
        }
        varArgs[i] = b;
        argTuples[i] = new ABICodec.Tuple(char[].class, b);
    }

    private void addShortArray(int i) {
        int len = random.nextInt(50);
        short[] b = new short[len];
        for (int j = 0; j < len; j++) {
            b[j] = (short) random.nextInt();
        }
        varArgs[i] = b;
        argTuples[i] = new ABICodec.Tuple(short[].class, b);
    }

    private void addIntArray(int i) {
        int[] b = getRandomIntArray();
        varArgs[i] = b;
        argTuples[i] = new ABICodec.Tuple(int[].class, b);
    }

    private void addLongArray(int i) {
        int len = random.nextInt(50);
        long[] b = new long[len];
        for (int j = 0; j < len; j++) {
            b[j] = random.nextLong();
        }
        varArgs[i] = b;
        argTuples[i] = new ABICodec.Tuple(long[].class, b);
    }

    private void addFloatArray(int i) {
        int len = random.nextInt(50);
        float[] b = new float[len];
        for (int j = 0; j < len; j++) {
            b[j] = random.nextFloat();
        }
        varArgs[i] = b;
        argTuples[i] = new ABICodec.Tuple(float[].class, b);
    }

    private void addDoubleArray(int i) {
        int len = random.nextInt(50);
        double[] b = new double[len];
        for (int j = 0; j < len; j++) {
            b[j] = random.nextDouble();
        }
        varArgs[i] = b;
        argTuples[i] = new ABICodec.Tuple(double[].class, b);
    }

    private void addString(int i) {
        String b = getRandomString(20);
        varArgs[i] = b;
        argTuples[i] = new ABICodec.Tuple(String.class, b);
    }

    private int[] getRandomIntArray() {
        int len = random.nextInt(50);
        int[] b = new int[len];
        for (int j = 0; j < len; j++) {
            if (random.nextInt(10) < 8) {
                b[j] = random.nextInt();
            }
        }
        return b;
    }

    private void addIntArray2D(int i) {
        int len = random.nextInt(50);
        int[][] b = new int[len][];

        for (int j = 0; j < len; j++) {
            b[j] = getRandomIntArray();
        }

        varArgs[i] = b;
        argTuples[i] = new ABICodec.Tuple(int[][].class, b);
    }

    private void addRandomType(int type, int index) {
        switch (type) {
            case 1:
                addByte(index);
                break;
            case 2:
                addBool(index);
                break;
            case 3:
                addChar(index);
                break;
            case 4:
                addShort(index);
                break;
            case 5:
                addInt(index);
                break;
            case 6:
                addLong(index);
                break;
            case 7:
                addFloat(index);
                break;
            case 8:
                addDouble(index);
                break;
            case 9:
                addByteArray(index);
                break;
            case 10:
                addBoolArray(index);
                break;
            case 11:
                addCharArray(index);
                break;
            case 12:
                addShortArray(index);
                break;
            case 13:
                addIntArray(index);
                break;
            case 14:
                addLongArray(index);
                break;
            case 15:
                addFloatArray(index);
                break;
            case 16:
                addDoubleArray(index);
                break;
            case 17:
                addString(index);
                break;
            case 18:
                addIntArray2D(index);
                break;
        }
        argTypes[index] = type;
        argDescriptors[index] = getTypeDescriptor(type);
    }

    private void assertArray(Object actual, int index) {
        switch (argTypes[index]) {
            case 9:
                Assert.assertArrayEquals((byte[]) argTuples[index].value, (byte[]) actual);
                break;
            case 10:
                Assert.assertArrayEquals((boolean[]) argTuples[index].value, (boolean[]) actual);
                break;
            case 11:
                Assert.assertArrayEquals((char[]) argTuples[index].value, (char[]) actual);
                break;
            case 12:
                Assert.assertArrayEquals((short[]) argTuples[index].value, (short[]) actual);
                break;
            case 13:
                Assert.assertArrayEquals((int[]) argTuples[index].value, (int[]) actual);
                break;
            case 14:
                Assert.assertArrayEquals((long[]) argTuples[index].value, (long[]) actual);
                break;
            case 15:
                Assert.assertTrue(Arrays.equals((float[]) argTuples[index].value, (float[]) actual));
                break;
            case 16:
                Assert.assertArrayEquals((double[]) argTuples[index].value, (double[]) actual, 0.1);
                break;
            case 18:
                Assert.assertArrayEquals((int[][]) argTuples[index].value, (int[][]) actual);
                break;
            default:
                Assert.fail("Not an array, test failed");
        }
    }
}
