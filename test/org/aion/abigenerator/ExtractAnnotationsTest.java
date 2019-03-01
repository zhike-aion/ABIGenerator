package org.aion.abigenerator;

import org.aion.abigenerator.ABICompiler;
import org.aion.abigenerator.AnnotationException;
import org.aion.avm.core.dappreading.JarBuilder;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class ExtractAnnotationsTest {

    private static ABICompiler compiler;

    @Before
    public void setup() {
        compiler = new ABICompiler();
    }

    @Test
    public void testOneClass() {
        List<String> callables = new ArrayList<>();
        try {
            byte[] jar = JarBuilder.buildJarForMainAndClasses(SimpleDAppWithMain.class);

            compiler.compile(new ByteArrayInputStream(jar));
            callables = compiler.getCallables();

        } catch (Throwable e) {
            e.printStackTrace();
        }
        assertEquals(2, callables.size());
        assertTrue(callables.get(0).equals("org/aion/abigenerator/SimpleDAppWithMain: public static boolean test1(boolean)"));
        assertTrue(callables.get(1).equals("org/aion/abigenerator/SimpleDAppWithMain: public static boolean test2(int, java.lang.String, long[])"));
    }

    @Test(expected = AnnotationException.class)
    public void testNonPublicCallable() {
        byte[] jar = JarBuilder.buildJarForMainAndClasses(SimpleDAppWrongCallable.class);
        try {
            compiler.compile(new ByteArrayInputStream(jar));
        } catch(AnnotationException e) {
            assertTrue(e.getMessage().contains("test4"));
            throw e;
        }
    }

    @Test(expected = AnnotationException.class)
        public void testNonStaticCallable() {
        byte[] jar = JarBuilder.buildJarForMainAndClasses(SimpleDAppWrongCallable1.class);
        try {
            compiler.compile(new ByteArrayInputStream(jar));
        } catch(AnnotationException e) {
            assertTrue(e.getMessage().contains("test2"));
            throw e;
        }
    }

    @Test
    public void testMultiClasses() {
        List<String> callables = new ArrayList<>();
        try {
            byte[] jar = JarBuilder.buildJarForMainAndClasses(SimpleDAppWithMain.class, SimpleDAppNoMain.class);

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
