package resources;

import org.aion.avm.api.ABIDecoder;
import org.aion.avm.api.BlockchainRuntime;

public class SimpleDApp1 {
    public static byte[] main() {
        return ABIDecoder.decodeAndRunWithClass(SimpleDApp.class, BlockchainRuntime.getData());
    }

    @org.aion.avm.core.abicompiler.Callable(first = "Zhike1", last = "Zhang2")
    public static boolean test1(boolean b) {
        return true;
    }

    @org.aion.avm.core.abicompiler.Callable(first = "Zhike2", last = "Zhang2")
    public boolean test2(int i, String s, long[] l) {
        return true;
    }

    @Deprecated
    public static boolean test3(int i, String s, long[] l) {
        return true;
    }

    @org.aion.avm.core.abicompiler.Callable(first = "Zhike4", last = "Zhang4")
    protected boolean test4(int i, String s, long[] l) {
        return true;
    }
}
