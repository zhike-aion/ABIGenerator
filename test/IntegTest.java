import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.objectweb.asm.Opcodes.*;

import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

import org.aion.abigenerator.ABICompiler;
import org.aion.avm.api.ABIDecoder;
import org.aion.avm.api.ABIEncoder;
import org.aion.avm.api.Address;
import org.aion.avm.core.dappreading.JarBuilder;
import org.aion.avm.core.util.AvmRule;
import org.aion.avm.core.util.CodeAndArguments;
import org.aion.kernel.AvmTransactionResult;
import org.aion.vm.api.interfaces.TransactionResult;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.objectweb.asm.*;

public class IntegTest {

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
        if (result.getReturnData() != null) {
            return ABIDecoder.decodeOneObject(result.getReturnData());
        } else {
            return null;
        }
    }

    @Test
    public void testSimpleDAppNoMain() {

        byte[] jar = JarBuilder.buildJarForMainAndClasses(SimpleDAppNoMain.class);
        compiler.compile(new ByteArrayInputStream(jar));
        Address dapp = installTestDApp();

        boolean ret = (Boolean) callStatic(dapp, "test1", true);
        assertTrue(ret);

        ret = (Boolean) callStatic(dapp, "test2", 1, "test2", new long[]{1, 2, 3});
        assertTrue(ret);
    }

    @Test
    public void testChattyCalculator() {

        byte[] jar =
                JarBuilder.buildJarForMainAndClasses(ChattyCalculator.class, DumbCalculator.class);
        compiler.compile(new ByteArrayInputStream(jar));
        Address dapp = installTestDApp();

        String ret = (String) callStatic(dapp, "amIGreater", 3, 4);
        assertEquals("No, 3, you are NOT greater than 4", ret);
        ret = (String) callStatic(dapp, "amIGreater", 5, 4);
        assertEquals("Yes, 5, you are greater than 4", ret);
    }

    @Test
    public void testGenerateMainAndCallMethod() {

        byte[] jar = JarBuilder.buildJarForMainAndClasses(HelloWorldNoMain.class);
        compiler.compile(new ByteArrayInputStream(jar));

/*               DataOutputStream dout = null;
        try {
            dout = new DataOutputStream(
                    new FileOutputStream(compiler.getMainClassName() + ".class"));
            dout.write(compiler.getMainClassBytes());
            dout.close();
        } catch (Exception e) {
            e.printStackTrace();
        }*/

        Address dapp = installTestDApp();

        String ret = (String) callStatic(dapp, "returnHelloWorld");
        assertEquals("Hello world", ret);

        ret = (String) callStatic(dapp, "returnGoodbyeWorld");
        assertEquals("Goodbye world", ret);

        ret = (String) callStatic(dapp, "returnEcho", "Code meets world");
        assertEquals("Code meets world", ret);

        ret = (String) callStatic(dapp, "returnAppended", "alpha", "bet");
        assertEquals("alphabet", ret);

        ret = (String) callStatic(dapp, "returnAppendedMultiTypes", "alpha", "bet", false, 123);
        assertEquals("alphabetfalse123", ret);

        int[] intArray = (int[]) callStatic(dapp, "returnArrayOfInt", 1, 2, 3);
        assertArrayEquals(new int[]{1, 2, 3}, intArray);

        String[] strArray = (String[]) callStatic(dapp, "returnArrayOfString", "hello", "world", "!");
        assertArrayEquals(new String[]{"hello", "world", "!"}, strArray);
    }


    @Test
    public void testFallback() {
        byte[] jar =
            JarBuilder.buildJarForMainAndClasses(SimpleDAppNoMain.class);
        compiler.compile(new ByteArrayInputStream(jar));
        Address dapp = installTestDApp();

        int oldVal = (Integer) callStatic(dapp, "getValue");
        callStatic(dapp, "garbageMethod", 7);
        int newVal = (Integer) callStatic(dapp, "getValue");

        assertEquals(oldVal + 10, newVal);
        callStatic(dapp, "", 7);

        newVal = (Integer) callStatic(dapp, "getValue");
        assertEquals(oldVal + 20, newVal);
    }

    @Test
    public void testRandom() {
        byte[] jar = JarBuilder.buildJarForExplicitClassNamesAndBytecode("RandomClass", createClass(), new HashMap<>());
        compiler.compile(new ByteArrayInputStream(jar));

        Address dapp = installTestDApp();

        boolean ret = (boolean) callStatic(dapp, "test",
                true, (byte) 1, 'a', (short) 2, (int) 3, (long) 4, (float) 5, (double) 6, "7", new int[]{8, 9});
        assertEquals(true, ret);
    }

    private byte[] createClass() {
        ClassWriter classWriter = new ClassWriter(0);
        classWriter.visit(V10, ACC_PUBLIC + ACC_SUPER, "RandomClass", null, "java/lang/Object", null);
        MethodVisitor methodVisitor = classWriter.visitMethod(ACC_PUBLIC | ACC_STATIC,
                "test",
                //getRandomArgumentsDescriptor(10), //TODO
                "(ZBCSIJFDLjava/lang/String;[I)Z",
                null,
                null);
        AnnotationVisitor annotationVisitor = methodVisitor.visitAnnotation("Lorg/aion/abigenerator/Callable;", false);
        annotationVisitor.visitEnd();
        methodVisitor.visitCode();
        methodVisitor.visitInsn(ICONST_1);
        methodVisitor.visitInsn(IRETURN);
        //TODO: return an object array
        methodVisitor.visitMaxs(1, 12);
        methodVisitor.visitEnd();
        classWriter.visitEnd();

/*        DataOutputStream dout = null;
        try {
            dout = new DataOutputStream(
                    new FileOutputStream("testrandom.class"));
            dout.write(classWriter.toByteArray());
            dout.close();
        } catch (Exception e) {
            e.printStackTrace();
        }*/

        return classWriter.toByteArray();
    }

    private String getRandomArgumentsDescriptor(int argumentsNumber) {

        Random random = new Random();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\"(");
        for (int i = 0; i < argumentsNumber; ++i) {
            stringBuilder.append(getRandomType(random.nextInt(18)));
        }
        stringBuilder.append(")Z");
        System.out.println(stringBuilder.toString());
        return stringBuilder.toString();
    }

    private String getRandomType(int type) {
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
                t = "[L";
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

}
