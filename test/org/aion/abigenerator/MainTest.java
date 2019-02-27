package org.aion.abigenerator;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
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
    public void testMainMethodOfABICompiler() {

        ABICompiler.main(new String[] {System.getProperty("user.dir") + "/test/resources/dapp.jar"});
        assertEquals(
                ABICompiler.getVersionNumber()
                        + "\nChattyCalculator: public static java.lang.String amIGreater(int, int)\n",
                outContent.toString());
    }
}
