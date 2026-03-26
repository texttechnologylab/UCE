package org.texttechnologylab.uce.common.models.search.promode;

import java.util.LinkedHashMap;
import java.util.List;
import org.texttechnologylab.uce.common.models.search.EnrichedSearchToken;

public interface ProExpansionResolver {
    ExpansionResult resolveCommand(String command, String value) throws Exception;

    ExpansionResult resolveTaxonTerm(String value) throws Exception;

    record ExpansionResult(
            List<String> flatValues,
            LinkedHashMap<String, List<EnrichedSearchToken>> groupedChildren,
            org.texttechnologylab.uce.common.models.search.EnrichedSearchTokenType tokenType
    ) {}
}
