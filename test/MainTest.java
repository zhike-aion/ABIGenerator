import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.aion.abigenerator.ABICompiler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MainTest {
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @Before
    public void setUpStreams() {
        System.setOut(new PrintStream(outContent));
    }

    @After
    public void restoreStreams() {
        System.setOut(originalOut);
    }

    @Test
    public void testMain() {

        ABICompiler.main(new String[]{System.getProperty("user.dir") + "/test/dapp.jar"});
        assertEquals("DumbCalculator: public static boolean greaterThan(int, int)\n" +
                "DumbCalculator: public static boolean lesserThan(int, int)\n" +
                "ChattyCalculator: public static java.lang.String amIGreater(int, int)\n",
                outContent.toString());
    }
}