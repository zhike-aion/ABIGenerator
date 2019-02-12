import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import org.aion.avm.core.abicompiler.ABICompiler;
import org.junit.Before;
import org.junit.Test;
import resources.SimpleDApp;
import resources.SimpleDApp1;
import resources.SimpleDAppNoMain;
import util.JarBuilder;


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
            DataOutputStream dout= null;
            try {
                dout = new DataOutputStream(new FileOutputStream("ClassModificationDemoNoMain.class"));
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
            byte[] jar = JarBuilder.buildJarForMainAndClasses(SimpleDAppNoMain.class, SimpleDApp.class, SimpleDApp1.class);
            compiler.compile(new ByteArrayInputStream(jar));
            List<byte[]> otherClasses = compiler.getOtherClasses();
            for (int i = 0; i < otherClasses.size(); i++) {
                DataOutputStream dout = null;
                try {
                    dout = new DataOutputStream(new FileOutputStream("otherClass" + i + ".class"));
                    dout.write(otherClasses.get(i));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch(Throwable e){
            e.printStackTrace();
        }
    }
}
