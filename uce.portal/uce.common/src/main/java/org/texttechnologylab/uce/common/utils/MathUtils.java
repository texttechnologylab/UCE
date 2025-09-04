package org.texttechnologylab.uce.common.utils;

public class MathUtils {

    /**
     * Converts a log10 odds value to a probability between 0 and 1.
     *
     * @param oddsLog10 the log10 odds value
     * @return the corresponding probability (0 to 1)
     */
    public static double log10OddsToProbability(double oddsLog10) {
        return 1.0 / (1.0 + Math.pow(10, -oddsLog10));
    }

}
