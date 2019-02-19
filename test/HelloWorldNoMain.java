import org.aion.abigenerator.Callable;

public class HelloWorldNoMain {

    @Callable
    public static String returnHelloWorld() {
        return "Hello world";
    }
}
