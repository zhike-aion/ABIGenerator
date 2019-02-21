import org.aion.abigenerator.ABICompiler;
import org.aion.abigenerator.CallableMismatchNonPublicException;
import org.aion.abigenerator.CallableMismatchNonStaticException;
import org.aion.avm.core.dappreading.JarBuilder;
import org.junit.Before;
import org.junit.Test;

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
        assertTrue(callables.get(0).equals("SimpleDApp: public static boolean test1(boolean)"));
        assertTrue(callables.get(1).equals("SimpleDApp: public static boolean test2(int, java.lang.String, long[])"));
    }

    @Test(expected = CallableMismatchNonPublicException.class)
    public void testGetAnnotationsWrongCallableForNonPublic() {
        byte[] jar = JarBuilder.buildJarForMainAndClasses(SimpleDAppWrongCallable.class);
        compiler.compile(new ByteArrayInputStream(jar));
    }

    @Test(expected = CallableMismatchNonStaticException.class)
    public void testGetAnnotationsWrongCallableForNonStatic() {
        byte[] jar = JarBuilder.buildJarForMainAndClasses(SimpleDAppWrongCallable1.class);
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
        assertEquals(2, callables.size());
        assertTrue(callables.get(0).indexOf("test1") > 0);
        assertTrue(callables.get(1).indexOf("test2") > 0);
    }
}
