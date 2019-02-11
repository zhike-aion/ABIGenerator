package util;

import java.io.*;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;


/**
 * A utility to build in-memory JAR representations for tests and examples.
 * <p>
 * This is kept purely private and only the top-level factory method operates on the instances since they are stateful in ways which
 * would be complicated to communicate (streams are closed when reading the bytes, for example).
 */
public class JarBuilder {
    /**
     * Creates the in-memory representation of a JAR with the given main class and other classes.
     *
     * @param mainClass    The main class to include and list in manifest (can be null).
     * @param otherClasses The other classes to include (main is already included).
     * @return The bytes representing this JAR.
     */
    public static byte[] buildJarForMainAndClasses(Class<?> mainClass, Class<?>... otherClasses) {
        JarBuilder builder = new JarBuilder(mainClass, null);
        for (Class<?> clazz : otherClasses) {
            builder.addClassAndInners(clazz);
        }
        return builder.toBytes();
    }

    /**
     * Creates the in-memory representation of a JAR with the given classes and explicit main class name.
     * NOTE:  This method is really just used to build invalid JARs (main class might not be included).
     *
     * @param mainClassName The name of the main class to reference in the manifest (cannot be null).
     * @param otherClasses  The other classes to include (main is already included).
     * @return The bytes representing this JAR.
     */
    public static byte[] buildJarForExplicitMainAndClasses(String mainClassName, Class<?>... otherClasses) {
        JarBuilder builder = new JarBuilder(null, mainClassName);
        for (Class<?> clazz : otherClasses) {
            builder.addClassAndInners(clazz);
        }
        return builder.toBytes();
    }

    /**
     * Creates the in-memory representation of a JAR with the given class names and direct bytes.
     * NOTE:  This method is really just used to build invalid JARs (given classes may be corrupt/invalid).
     *
     * @return The bytes representing this JAR.
     */
    public static byte[] buildJarForExplicitClassNameAndBytecode(String mainClassName, byte[] mainClassBytes) {
        JarBuilder builder = new JarBuilder(null, mainClassName);
        try {
            builder.saveClassToStream(mainClassName, mainClassBytes);
        } catch (IOException e) {
            // Can't happen - in-memory.
            e.printStackTrace();
        }
        return builder.toBytes();
    }


    private final ByteArrayOutputStream byteStream;
    private final JarOutputStream jarStream;
    private final Set<String> entriesInJar;

    private JarBuilder(Class<?> mainClass, String mainClassName) {
        // Build the manifest.
        Manifest manifest = new Manifest();
        Attributes mainAttributes = manifest.getMainAttributes();
        // Note that the manifest version seems to be required.  If it isn't specified, we don't see the main class.
        mainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        // The main class is technically optional (we mostly use a null main for testing cases).
        if (null != mainClass) {
            mainAttributes.put(Attributes.Name.MAIN_CLASS, mainClass.getName());
        } else if (null != mainClassName) {
            mainAttributes.put(Attributes.Name.MAIN_CLASS, mainClassName);
        }

        // Create the underlying byte stream (hold onto this for serialization).
        this.byteStream = new ByteArrayOutputStream();
        JarOutputStream stream = null;
        try {
            // We always write the manifest into the high-level JAR stream.
            stream = new JarOutputStream(this.byteStream, manifest);
        } catch (IOException e) {
            // We are using a byte array so this can't happen.
            e.printStackTrace();
        }
        this.jarStream = stream;
        this.entriesInJar = new HashSet<>();

        // Finally, add this class.
        if (null != mainClass) {
            addClassAndInners(mainClass);
        }
    }

    /**
     * Loads the given class, any declared classes (named inner classes), and any anonymous inner classes.
     *
     * @param clazz The class to load.
     * @return this, for easy chaining.
     */
    public JarBuilder addClassAndInners(Class<?> clazz) {
        try {
            // Load everything related to this class.
            loadClassAndAnonymous(clazz);
            // Now, include any declared classes.
            for (Class<?> one : clazz.getDeclaredClasses()) {
                addClassAndInners(one);
            }
        } catch (IOException e) {
            // We are serializing to a byte array so this is unexpected.
            e.printStackTrace();
        }
        return this;
    }

    private void loadClassAndAnonymous(Class<?> clazz) throws IOException {
        // Start with the fully-qualified class name, since we use that for addressing it.
        String className = clazz.getName();
        byte[] bytes = loadRequiredResourceAsBytes(fulllyQualifiedNameToInternalName(className) + ".class");
        assert (null != bytes);
        saveClassToStream(className, bytes);

        // Load any inner classes which might exist (these are just decimal suffixes, starting at 1.
        int i = 1;
        String innerName = className + "$" + Integer.toString(i);
        byte[] innerBytes = loadRequiredResourceAsBytes(fulllyQualifiedNameToInternalName(innerName) + ".class");
        while (null != innerBytes) {
            saveClassToStream(innerName, innerBytes);

            i += 1;
            innerName = className + "$" + Integer.toString(i);
            innerBytes = loadRequiredResourceAsBytes(fulllyQualifiedNameToInternalName(innerName) + ".class");
        }
    }

    private void saveClassToStream(String qualifiedClassName, byte[] bytes) throws IOException {
        // Convert this fully-qualified name into an internal name, since that is the serialized name it needs.
        String internalName = fulllyQualifiedNameToInternalName(qualifiedClassName);
        assert (!this.entriesInJar.contains(internalName));
        JarEntry entry = new JarEntry(internalName + ".class");
        this.jarStream.putNextEntry(entry);
        this.jarStream.write(bytes);
        this.jarStream.closeEntry();
        this.entriesInJar.add(internalName);
    }

    public byte[] toBytes() {
        try {
            this.jarStream.finish();
            this.jarStream.close();
            this.byteStream.close();
        } catch (IOException e) {
            // We are using a byte array so this can't happen.
            e.printStackTrace();
        }
        return this.byteStream.toByteArray();
    }


    /**
     * A helper which will attempt to load the given resource path as bytes.
     * Returns null if the resource could not be found.
     *
     * @param resourcePath The path to this resource, within the parent class loader.
     * @return The resource as bytes, or null if not found.
     */
    public static byte[] loadRequiredResourceAsBytes(String resourcePath) {
        InputStream stream = JarBuilder.class.getClassLoader().getResourceAsStream(resourcePath);
        byte[] raw = null;
        if (null != stream) {
            try {
                raw = stream.readAllBytes();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return raw;
    }


    /**
     * Converts a fully qualified class name into it's JVM internal form.
     *
     * @param fullyQualifiedName
     * @return
     */
    public static String fulllyQualifiedNameToInternalName(String fullyQualifiedName) {
        return fullyQualifiedName.replaceAll("\\.", "/");
    }


    /**
     * Writes the given bytes to the file at the given path.
     * This is effective for dumping re-written bytecode to file for offline analysis.
     *
     * @param bytes The bytes to write.
     * @param path  The path where the file should be written.
     */
    public static void writeBytesToFile(byte[] bytes, String path) {
        File f = new File(path);
        f.getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(f)) {
            fos.write(bytes);
        } catch (IOException e) {
            // This is for tests so we aren't expecting the failure.
            e.printStackTrace();
        }
    }

}
