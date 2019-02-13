package resources;

public class Comparator {
    @org.aion.avm.core.abicompiler.Callable()
    public static boolean greaterThan(int a, int b) {
        return a > b;
    }

    private static boolean greaterThanEq(int a, int b) {
        return a >= b;
    }

    @org.aion.avm.core.abicompiler.Callable()
    public static boolean lesserThan(int a, int b) {
        return !(greaterThanEq(a, b));
    }
}
