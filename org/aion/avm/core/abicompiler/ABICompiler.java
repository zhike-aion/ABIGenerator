package org.aion.avm.core.abicompiler;

import org.objectweb.asm.*;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

public class ABICompiler {
    private static final int MAX_CLASS_BYTES = 1024 * 1024;

    static public void main(String[] args) {
        String jarPath = "abicompiler.jar";
        byte[] mainClass = null;
        try {
            mainClass = safeLoadFromBytes(new FileInputStream(jarPath));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SizeException e) {
            e.printStackTrace();
        }
        ClassReader reader = new ClassReader(mainClass);
        ClassVisitor classVisitor = new ABICompilerClassVisitor();
        reader.accept(classVisitor, 0);


    }

    private static byte[] safeLoadFromBytes(InputStream byteReader) throws IOException, SizeException {
        Map<String, byte[]> classBytesByQualifiedNames = new HashMap<>();
        String mainClassName = null;

        boolean verify = true;
        try (JarInputStream jarReader = new JarInputStream(byteReader, verify)) {

            Manifest manifest = jarReader.getManifest();
            if (null != manifest) {
                Attributes mainAttributes = manifest.getMainAttributes();
                if (null != mainAttributes) {
                    mainClassName = mainAttributes.getValue(Attributes.Name.MAIN_CLASS);
                }
            }

            JarEntry entry = null;
            byte[] tempReadingBuffer = new byte[MAX_CLASS_BYTES];
            while (null != (entry = jarReader.getNextJarEntry())) {
                String name = entry.getName();
                // We already ready the manifest so now we only want to work on classes and not any of the special modularity ones.
                if (name.endsWith(".class")
                        && !name.equals("package-info.class")
                        && !name.equals("module-info.class")
                ) {
                    // replaceAll gives us the regex so we use "$".
                    String internalClassName = name.replaceAll(".class$", "");
                    String qualifiedClassName = internalNameToFulllyQualifiedName(internalClassName);
                    int readSize = jarReader.readNBytes(tempReadingBuffer, 0, tempReadingBuffer.length);
                    // Now, copy this part of the array as a correctly-sized classBytes.
                    byte[] classBytes = new byte[readSize];
                    if (0 != jarReader.available()) {
                        // This entry is too big.
                        throw new SizeException(name);
                    }
                    System.arraycopy(tempReadingBuffer, 0, classBytes, 0, readSize);
                    classBytesByQualifiedNames.put(qualifiedClassName, classBytes);
                }
            }
        }
        return classBytesByQualifiedNames.get(mainClassName);
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