package org.texttechnologylab.uce.common.utils;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

import java.util.ArrayList;
import java.util.List;


public class TokenUtils {
    public static ArrayList<ArrayList<Token>> findMaximalSpans(List<Token> tokens) {
        ArrayList<ArrayList<Token>> spans = new ArrayList<>();
        if (tokens.isEmpty()) return spans;

        // Sort tokens by start position (optional if already sorted)
        tokens.sort((t1, t2) -> Integer.compare(t1.getBegin(), t2.getEnd()));
        ArrayList<Token> currentSpan = new ArrayList<>();
        currentSpan.add(tokens.getFirst());

        for (int i = 1; i < tokens.size(); i++) {
            Token prev = tokens.get(i - 1);
            Token curr = tokens.get(i);

            // If current token starts right after the previous token (+1 for whitespace), continue the span
            if (curr.getBegin() <= prev.getEnd() + 1) {
                currentSpan.add(curr);
            } else {
                // Otherwise, start a new span
                spans.add(new ArrayList<>(currentSpan));
                currentSpan.clear();
                currentSpan.add(curr);
            }
        }

        // Add the last span
        spans.add(currentSpan);

        return spans;
    }
}
