package org.aion.abigenerator;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

public class ABICompiler {

    private static final int MAX_CLASS_BYTES = 1024 * 1024;

    private String mainClassName;
    private byte[] mainClassBytes;
    private List<String> callables = new ArrayList<>();
    private Map<String, byte[]> inputClassMap = new HashMap<>();
    private Map<String, byte[]> outputClassMap = new HashMap<>();

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Invalid parameters!");
            usage();
            System.exit(1);
        }

        String jarPath = args[0];
        ABICompiler compiler = new ABICompiler();
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(jarPath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        }

        compiler.compile(fileInputStream);

        List<String> callables = compiler.getCallables();
        for (String s : callables) System.out.println(s);

        try {
            DataOutputStream dout =
                    new DataOutputStream(
                            new FileOutputStream(compiler.getMainClassName() + ".class"));
            dout.write(compiler.getMainClassBytes());
            dout.close();

            for (HashMap.Entry<String, byte[]> entry : compiler.outputClassMap.entrySet()) {
                dout = new DataOutputStream(new FileOutputStream(entry.getKey() + ".class"));
                dout.write(entry.getValue());
                dout.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void usage() {
        System.out.println("Usage: ABICompiler <DApp jar path>");
    }

    public void compile(InputStream byteReader) {
        try {
            safeLoadFromBytes(byteReader);
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (Map.Entry<String, byte[]> clazz : inputClassMap.entrySet()) {
            ClassReader reader = new ClassReader(clazz.getValue());
            ClassWriter classWriter = new ClassWriter(0);
            ABICompilerClassVisitor classVisitor = new ABICompilerClassVisitor(classWriter) {};
            if (clazz.getKey().equals(mainClassName)) {
                classVisitor.setMain();
            }
            try {
                reader.accept(classVisitor, 0);
            } catch (Exception e) {
                throw e;
            }
            callables.addAll(classVisitor.getCallables());
            if (clazz.getKey().equals(mainClassName)) {
                mainClassBytes = classWriter.toByteArray();
            } else {
                outputClassMap.put(clazz.getKey(), classWriter.toByteArray());
            }
        }
    }

    private void safeLoadFromBytes(InputStream byteReader) throws Exception {
        inputClassMap = new HashMap<>();
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
                        throw new Exception("Class file too big: " + name);
                    }
                    System.arraycopy(tempReadingBuffer, 0, classBytes, 0, readSize);
                    inputClassMap.put(qualifiedClassName, classBytes);
                }
            }
        }
    }

    private static String internalNameToFulllyQualifiedName(String internalName) {
        return internalName.replaceAll("/", ".");
    }

    public List<String> getCallables() {
        return callables;
    }

    public byte[] getMainClassBytes() {
        return mainClassBytes;
    }

    public String getMainClassName() {
        return mainClassName;
    }

    public Map<String, byte[]> getClassMap() {
        return outputClassMap;
    }
}
