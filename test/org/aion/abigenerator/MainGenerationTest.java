package org.aion.abigenerator;

import java.io.ByteArrayInputStream;

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
    public void testOneClass() {
        try {
            byte[] jar = JarBuilder.buildJarForMainAndClasses(DAppNoMainWithFallbackTarget.class);
            compiler.compile(new ByteArrayInputStream(jar));

            TestHelpers.saveMainClassInABICompiler(compiler);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testMultiClasses() {
        try {
            byte[] jar =
                    JarBuilder.buildJarForMainAndClasses(
                        DAppNoMainWithFallbackTarget.class, DAppNoMainNoFallbackTarget.class);
            compiler.compile(new ByteArrayInputStream(jar));

            TestHelpers.saveAllClassesInABICompiler(compiler);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testImportedClass() {
        try {
            byte[] jar =
                    JarBuilder.buildJarForMainAndClasses(
                            ChattyCalculatorTarget.class, SilentCalculatorTarget.class);
            compiler.compile(new ByteArrayInputStream(jar));

            TestHelpers.saveAllClassesInABICompiler(compiler);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
