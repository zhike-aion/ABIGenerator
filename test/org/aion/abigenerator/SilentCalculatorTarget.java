package org.aion.abigenerator;

public class SilentCalculatorTarget {
    @Callable()
    public static boolean greaterThan(int a, int b) {
        return a > b;
    }

    private static boolean greaterThanEq(int a, int b) {
        return a >= b;
    }

    @Callable()
    public static boolean lesserThan(int a, int b) {
        return !(greaterThanEq(a, b));
    }
}
