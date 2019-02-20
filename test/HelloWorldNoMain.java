import org.aion.abigenerator.Callable;

public class HelloWorldNoMain {

    @Callable
    public static String returnHelloWorld() {
        return "Hello world";
    }

    @Callable
    public static String returnGoodbyeWorld() {
        return "Goodbye world";
    }

    @Callable
    public static String returnEcho(String s) {
        return s;
    }

    @Callable
    public static String returnAppended(String s1, String s2) {
        return s1 + s2;
    }

    @Callable
    public static String returnAppendedMultiTypes(String s1, String s2, Boolean b, Integer l) {
        return s1 + s2 + b + l;
    }
}
