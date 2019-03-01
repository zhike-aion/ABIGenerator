package org.aion.abigenerator;

public class ChattyCalculator {

    @Callable()
    public static String amIGreater(int a, int b) {
        if (DumbCalculator.greaterThan(a, b)) {
            return("Yes, " + a + ", you are greater than " + b);
        } else {
            return("No, " + a + ", you are NOT greater than " + b);
        }
    }
}
