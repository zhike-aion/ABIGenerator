package resources;

import org.aion.avm.api.ABIDecoder;
import org.aion.avm.api.BlockchainRuntime;

public class SimpleDAppWrongCallable {
    public static byte[] main() {
        return ABIDecoder.decodeAndRunWithClass(SimpleDApp.class, BlockchainRuntime.getData());
    }

    @org.aion.avm.core.abicompiler.Callable()
    public static boolean test1(boolean b) {
        return true;
    }

    @org.aion.avm.core.abicompiler.Callable()
    public boolean test2(int i, String s, long[] l) {
        return true;
    }

    @Deprecated
    public static boolean test3(int i, String s, long[] l) {
        return true;
    }

    @org.aion.avm.core.abicompiler.Callable()
    protected boolean test4(int i, String s, long[] l) {
        return true;
    }
}

