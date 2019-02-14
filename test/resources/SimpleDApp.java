package resources;

import org.aion.abigenerator.Callable;
import org.aion.avm.api.ABIDecoder;
import org.aion.avm.api.BlockchainRuntime;

public class SimpleDApp {
    public static byte[] main() {
        return ABIDecoder.decodeAndRunWithClass(SimpleDApp.class, BlockchainRuntime.getData());
    }

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
