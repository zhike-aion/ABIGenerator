package org.aion.avm.core.abicompiler;

import org.objectweb.asm.*;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import static org.objectweb.asm.Opcodes.*;

public class ABICompiler {
    private static final int MAX_CLASS_BYTES = 1024 * 1024;

     private static ClassReader reader;
     private byte[] jarBytes;
     private byte[] mainClass;
     List<String> callables = new ArrayList<>();

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

    private void setJarBytes(byte[] bytes) {
        jarBytes = bytes;
    }

    public void extractMethods() {
        reader = new ClassReader(mainClass);
        ABICompilerClassVisitor classVisitor = new org.aion.avm.core.abicompiler.ABICompilerClassVisitor();
        reader.accept(classVisitor, 0);
        callables = classVisitor.getCallables();
    }

    public List<String> getCallables() {
        return callables;
    }

    private static void generateMain() {
        //Generating main()
        ClassWriter classWriter = new ClassWriter(0);
        ClassVisitor mcw = new ClassVisitor(Opcodes.ASM6, classWriter){};
        reader.accept(mcw, 0);

        {
            MethodVisitor methodVisitor = classWriter.visitMethod(ACC_PUBLIC | ACC_STATIC, "main", "()[B", null, null);
            methodVisitor.visitCode();
            Label label0 = new Label();
            methodVisitor.visitLabel(label0);
            methodVisitor.visitLineNumber(8, label0);
            methodVisitor.visitLdcInsn(Type.getType("Lorg/aion/avm/core/abicompiler/ABICompilerTestTarget;"));
            methodVisitor.visitMethodInsn(INVOKESTATIC, "org/aion/avm/api/BlockchainRuntime", "getData", "()[B", false);
            methodVisitor.visitMethodInsn(INVOKESTATIC, "org/aion/avm/api/ABIDecoder", "decodeAndRunWithClass", "(Ljava/lang/Class;[B)[B", false);
            methodVisitor.visitInsn(ARETURN);
            methodVisitor.visitMaxs(2, 0);
            methodVisitor.visitEnd();
        }
        //Write the output to a class file
        DataOutputStream dout= null;
        try {
            dout = new DataOutputStream(new FileOutputStream("ClassModificationDemoNoMain.class"));
            dout.write(classWriter.toByteArray());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void safeLoadFromBytes(InputStream byteReader) throws IOException, SizeException {
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
        mainClass = classBytesByQualifiedNames.get(mainClassName);
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