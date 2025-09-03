package org.texttechnologylab.uce.common.states;

import org.texttechnologylab.uce.common.models.corpus.Document;
import org.texttechnologylab.uce.common.utils.Pair;
import org.texttechnologylab.uce.common.utils.RegexUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class KeywordInContextState {

    private List<Pair<String, List<KeywordContext>>> contexts;

    /**
     * The state will change depending on the given documents.
     */
    public void recalculate(List<Document> currentDocuments,
                            List<String> searchTokens){
        contexts = new ArrayList<>();

        // We make a context view foreach search token
        for(var searchToken:searchTokens){

            var keywordContexts = new ArrayList<KeywordContext>();
            for(var document:currentDocuments){
                var matches = RegexUtils.ExtractOccurrences(document.getFullText(),
                        searchToken, 12, 12, 7);
                for(var context:matches){
                    var left = context[0];
                    var mid = context[1];
                    var right = context[2];

                    var keywordContext = new KeywordContext();
                    keywordContext.setDocument_id(document.getId());
                    keywordContext.setBefore(Arrays.stream(left.split(" ")).toList());
                    keywordContext.setAfter(Arrays.stream(right.split(" ")).toList());
                    keywordContext.setKeyword(mid);
                    keywordContexts.add(keywordContext);
                }
            }
            contexts.add(new Pair<>(searchToken, keywordContexts));
        }
    }

    public List<Pair<String, List<KeywordContext>>> getContexts(){
        return contexts;
    }

}
