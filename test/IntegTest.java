import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import org.aion.abigenerator.ABICompiler;
import org.aion.avm.api.ABIDecoder;
import org.aion.avm.api.ABIEncoder;
import org.aion.avm.api.Address;
import org.aion.avm.core.dappreading.JarBuilder;
import org.aion.avm.core.util.AvmRule;
import org.aion.avm.core.util.CodeAndArguments;
import org.aion.kernel.AvmTransactionResult;
import org.aion.vm.api.interfaces.TransactionResult;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class IntegTest {

    @Rule public AvmRule avmRule = new AvmRule(true);

    private static ABICompiler compiler;

    private static final long ENERGY_LIMIT = 10_000_000L;
    private static final long ENERGY_PRICE = 1L;

    @Before
    public void setup() {
        compiler = new ABICompiler();
    }

    private Address installTestDApp() {

        byte[] jar =
                JarBuilder.buildJarForExplicitClassNamesAndBytecode(
                        compiler.getMainClassName(),
                        compiler.getMainClassBytes(),
                        compiler.getClassMap());
        byte[] txData = new CodeAndArguments(jar, new byte[0]).encodeToBytes();

        // Deploy.
        TransactionResult createResult =
                avmRule.deploy(
                                avmRule.getPreminedAccount(),
                                BigInteger.ZERO,
                                txData,
                                ENERGY_LIMIT,
                                ENERGY_PRICE)
                        .getTransactionResult();
        assertEquals(AvmTransactionResult.Code.SUCCESS, createResult.getResultCode());
        return new Address(createResult.getReturnData());
    }

    private Object callStatic(Address dapp, String methodName, Object... arguments) {
        byte[] argData = ABIEncoder.encodeMethodArguments(methodName, arguments);
        TransactionResult result =
                avmRule.call(
                                avmRule.getPreminedAccount(),
                                dapp,
                                BigInteger.ZERO,
                                argData,
                                ENERGY_LIMIT,
                                ENERGY_PRICE)
                        .getTransactionResult();
        assertEquals(AvmTransactionResult.Code.SUCCESS, result.getResultCode());
        return ABIDecoder.decodeOneObject(result.getReturnData());
    }

    @Test
    public void testSimpleDAppNoMain() {

        byte[] jar = JarBuilder.buildJarForMainAndClasses(SimpleDAppNoMain.class);
        compiler.compile(new ByteArrayInputStream(jar));
        Address dapp = installTestDApp();

        boolean ret = (Boolean) callStatic(dapp, "test1", true);
        assertTrue(ret);
    }

    @Test
    public void testChattyCalculator() {

        byte[] jar =
                JarBuilder.buildJarForMainAndClasses(ChattyCalculator.class, DumbCalculator.class);
        compiler.compile(new ByteArrayInputStream(jar));
        Address dapp = installTestDApp();

        String ret = (String) callStatic(dapp, "amIGreater", 3, 4);
        assertEquals("No, 3, you are NOT greater than 4", ret);
        ret = (String) callStatic(dapp, "amIGreater", 5, 4);
        assertEquals("Yes, 5, you are greater than 4", ret);
    }
}
