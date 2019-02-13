import org.aion.avm.core.abicompiler.ABICompiler;
import org.aion.avm.core.abicompiler.CallableMismatchException;
import org.junit.Before;
import org.junit.Test;
import resources.SimpleDApp;
import resources.SimpleDAppNoMain;
import resources.SimpleDAppWrongCallable;
import util.JarBuilder;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class AnnotationsTest {

    private static ABICompiler compiler;

    @Before
    public void setup() {
        compiler = new ABICompiler();
    }

    @Test
    public void testGetAnnotations() {
        List<String> callables = new ArrayList<>();
        try {
            byte[] jar = JarBuilder.buildJarForMainAndClasses(SimpleDApp.class);

            compiler.compile(new ByteArrayInputStream(jar));
            callables = compiler.getCallables();

        } catch (Throwable e) {
            e.printStackTrace();
        }
        assertEquals(2, callables.size());
        assertTrue(callables.get(0).indexOf("test1") > 0);
        assertTrue(callables.get(1).indexOf("test2") > 0);
    }

    @Test(expected = CallableMismatchException.class)
    public void testGetAnnotationsWrongCallable() {
        byte[] jar = JarBuilder.buildJarForMainAndClasses(SimpleDAppWrongCallable.class);
        compiler.compile(new ByteArrayInputStream(jar));
    }

    @Test
    public void testGetAnnotationsFromMultiClasses() {
        List<String> callables = new ArrayList<>();
        try {
            byte[] jar = JarBuilder.buildJarForMainAndClasses(SimpleDApp.class, SimpleDAppNoMain.class);

            compiler.compile(new ByteArrayInputStream(jar));
            callables = compiler.getCallables();

        } catch (Throwable e) {
            e.printStackTrace();
        }
        assertEquals(4, callables.size());
        assertTrue(callables.get(0).indexOf("test1") > 0);
        assertTrue(callables.get(1).indexOf("test2") > 0);
        assertTrue(callables.get(2).indexOf("test1") > 0);
        assertTrue(callables.get(3).indexOf("test2") > 0);
    }
}
