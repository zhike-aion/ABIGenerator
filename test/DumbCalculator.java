import org.aion.abigenerator.Callable;

public class DumbCalculator {
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
