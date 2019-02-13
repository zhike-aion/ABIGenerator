import org.aion.avm.core.abicompiler.ABICompiler;
import org.aion.avm.core.abicompiler.IllegalMainMethodsException;
import org.junit.Before;
import org.junit.Test;
import resources.ChattyCalculator;
import resources.Comparator;
import resources.SimpleDApp;
import resources.SimpleDAppNoMain;
import resources.SimpleDAppNoMain1;
import util.JarBuilder;

import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class MainGenerationTest {

    private static ABICompiler compiler;

    @Before
    public void setup() {
        compiler = new ABICompiler();
    }

    @Test
    public void testMainGeneration() {
        try {
            byte[] jar = JarBuilder.buildJarForMainAndClasses(SimpleDAppNoMain.class);
            compiler.compile(new ByteArrayInputStream(jar));
            DataOutputStream dout = null;
            try {
                dout = new DataOutputStream(new FileOutputStream("Main.class"));
                dout.write(compiler.getMainClassBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testOtherClassesGeneration() {
        try {
            byte[] jar =
                JarBuilder.buildJarForMainAndClasses(
                    SimpleDAppNoMain.class, SimpleDAppNoMain1.class);
            compiler.compile(new ByteArrayInputStream(jar));
            List<byte[]> otherClasses = compiler.getOtherClassesBytes();
            for (int i = 0; i < otherClasses.size(); i++) {
                DataOutputStream dout = null;
                try {
                    dout = new DataOutputStream(new FileOutputStream("OtherClass" + i + ".class"));
                    dout.write(otherClasses.get(i));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Test(expected = IllegalMainMethodsException.class)
    public void testGetAnnotationsMultipleMainMethod() {
        byte[] jar = JarBuilder.buildJarForMainAndClasses(SimpleDAppNoMain.class, SimpleDApp.class);
        compiler.compile(new ByteArrayInputStream(jar));
    }

    @Test
    public void testImportedClass() {
        try {
            byte[] jar =
                JarBuilder.buildJarForMainAndClasses(ChattyCalculator.class, Comparator.class);
            compiler.compile(new ByteArrayInputStream(jar));
            List<byte[]> otherClasses = compiler.getOtherClassesBytes();
            for (int i = 0; i < otherClasses.size(); i++) {
                DataOutputStream dout = null;
                try {
                    dout = new DataOutputStream(new FileOutputStream("otherClass" + i + ".class"));
                    dout.write(otherClasses.get(i));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
