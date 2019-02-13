package resources;

public class ChattyCalculator {

    @org.aion.avm.core.abicompiler.Callable()
    public static void amIGreater(int a, int b) {
        if (Comparator.greaterThan(a, b)) {
            System.out.println("Yes, " + a + ", you are greater than " + b);
        } else {
            System.out.println("No, " + a + ", you are NOT greater than " + b);
        }
    }
}
