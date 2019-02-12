package org.aion.avm.core.abicompiler;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

public class ABICompiler {

    private static final int MAX_CLASS_BYTES = 1024 * 1024;

    private String mainClassName;
    private byte[] mainClassBytes;
    private List<String> callables = new ArrayList<>();
    private Map<String, byte[]> classMap = new HashMap<>();

    //    public static void main(String[] args) {
    //        String jarPath = "nomain/nomain.jar";
    //        byte[] mainClass = null;
    //        try {
    //            mainClass = safeLoadFromBytes(new FileInputStream(jarPath));
    //        } catch (IOException e) {
    //            e.printStackTrace();
    //        } catch (SizeException e) {
    //            e.printStackTrace();
    //        }
    //
    //        extractMethods(mainClass);
    //        generateMain();
    //    }

    public void compile(InputStream byteReader) {
        try {
            safeLoadFromBytes(byteReader);
        } catch (IOException | SizeException e) {
            e.printStackTrace();
        }
        for (Map.Entry<String, byte[]> clazz : classMap.entrySet()) {
            ClassReader reader = new ClassReader(clazz.getValue());
            ClassWriter classWriter = new ClassWriter(0);
            ABICompilerClassVisitor classVisitor = new ABICompilerClassVisitor(classWriter) {};
            if (clazz.getKey().equals(mainClassName)) {
                classVisitor.setMain();
            }
            reader.accept(classVisitor, 0);
            callables.addAll(classVisitor.getCallables());
            if (clazz.getKey().equals(mainClassName)) {
                mainClassBytes = classWriter.toByteArray();
            }
        }
    }

    public byte[] getMainClassBytes() {
        return mainClassBytes;
    }

    public List<String> getCallables() {
        return callables;
    }

    private void safeLoadFromBytes(InputStream byteReader) throws IOException, SizeException {
        classMap = new HashMap<>();
        mainClassName = null;

        try (JarInputStream jarReader = new JarInputStream(byteReader, true)) {

            Manifest manifest = jarReader.getManifest();
            if (null != manifest) {
                Attributes mainAttributes = manifest.getMainAttributes();
                if (null != mainAttributes) {
                    mainClassName = mainAttributes.getValue(Attributes.Name.MAIN_CLASS);
                }
            }

            JarEntry entry;
            byte[] tempReadingBuffer = new byte[MAX_CLASS_BYTES];
            while (null != (entry = jarReader.getNextJarEntry())) {
                String name = entry.getName();
                // We already ready the manifest so now we only want to work on classes and not any
                // of the
                // special modularity ones.
                if (name.endsWith(".class")
                        && !name.equals("package-info.class")
                        && !name.equals("module-info.class")) {
                    // replaceAll gives us the regex so we use "$".
                    String internalClassName = name.replaceAll(".class$", "");
                    String qualifiedClassName =
                            internalNameToFulllyQualifiedName(internalClassName);
                    int readSize =
                            jarReader.readNBytes(tempReadingBuffer, 0, tempReadingBuffer.length);
                    // Now, copy this part of the array as a correctly-sized classBytes.
                    byte[] classBytes = new byte[readSize];
                    if (0 != jarReader.available()) {
                        // This entry is too big.
                        throw new SizeException(name);
                    }
                    System.arraycopy(tempReadingBuffer, 0, classBytes, 0, readSize);
                    classMap.put(qualifiedClassName, classBytes);
                }
            }
        }
    }

    private static String internalNameToFulllyQualifiedName(String internalName) {
        return internalName.replaceAll("/", ".");
    }

    private static class SizeException extends Exception {

        private static final long serialVersionUID = 1L;

        public SizeException(String entryName) {
            super("Class file too big: " + entryName);
        }
    }
}
