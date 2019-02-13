import org.aion.avm.core.abicompiler.ABICompiler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.assertEquals;

public class MainTest {
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @Before
    public void setUpStreams() {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @After
    public void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    public void testMain() {
        ABICompiler.main(new String[]{System.getProperty("user.dir") + "/test/resources/dapp/dapp.jar"});
        assertEquals("resources/Comparator: public static boolean greaterThan(int, int)\n" +
                "resources/Comparator: public static boolean lesserThan(int, int)\n" +
                "resources/ChattyCalculator: public static void amIGreater(int, int)\n",
                outContent.toString());
    }
}