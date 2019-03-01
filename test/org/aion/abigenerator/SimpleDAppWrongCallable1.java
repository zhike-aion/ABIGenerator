package org.aion.abigenerator;

import org.aion.avm.api.ABIDecoder;
import org.aion.avm.api.BlockchainRuntime;

public class SimpleDAppWrongCallable1 {
    public static byte[] main() {
        return ABIDecoder.decodeAndRunWithClass(SimpleDAppWithMain.class, BlockchainRuntime.getData());
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
