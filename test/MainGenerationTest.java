import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import org.aion.abigenerator.ABICompiler;
import org.aion.abigenerator.IllegalMainMethodsException;
import org.aion.avm.core.dappreading.JarBuilder;
import org.junit.Before;
import org.junit.Test;

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
            DataOutputStream dout =
                    new DataOutputStream(
                            new FileOutputStream(compiler.getMainClassName() + ".class"));
            dout.write(compiler.getMainClassBytes());
            dout.close();

            for (Map.Entry<String, byte[]> entry : compiler.getClassMap().entrySet()) {
                dout = new DataOutputStream(new FileOutputStream(entry.getKey() + ".class"));
                dout.write(entry.getValue());
                dout.close();
            }
        } catch (IOException e) {
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
                    JarBuilder.buildJarForMainAndClasses(
                            ChattyCalculator.class, DumbCalculator.class);
            compiler.compile(new ByteArrayInputStream(jar));
            DataOutputStream dout =
                    new DataOutputStream(
                            new FileOutputStream(compiler.getMainClassName() + ".class"));
            dout.write(compiler.getMainClassBytes());
            dout.close();

            for (Map.Entry<String, byte[]> entry : compiler.getClassMap().entrySet()) {
                dout = new DataOutputStream(new FileOutputStream(entry.getKey() + ".class"));
                dout.write(entry.getValue());
                dout.close();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
