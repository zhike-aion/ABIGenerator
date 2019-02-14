package resources;

import org.aion.abigenerator.Callable;

public class SimpleDAppNoMain {

    @Callable()
    public static boolean test1(boolean b) {
        return true;
    }

    @Callable()
    public boolean test2(int i, String s, long[] l) {
        return true;
    }

    @Deprecated
    public static boolean test3(int i, String s, long[] l) {
        return true;
    }
}

