package org.texttechnologylab.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.texttechnologylab.config.CommonConfig;
import org.texttechnologylab.exceptions.DatabaseOperationException;
import org.texttechnologylab.exceptions.ExceptionUtils;
import org.texttechnologylab.models.UIMAAnnotation;
import org.texttechnologylab.models.biofid.BiofidTaxon;
import org.texttechnologylab.models.corpus.*;
import org.texttechnologylab.utils.SystemStatus;

import javax.persistence.Table;
import java.sql.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class LexiconService {

    private final PostgresqlDataInterface_Impl db;
    private final CommonConfig commonConfig;
    private static final Logger logger = LogManager.getLogger(LexiconService.class);

    /**
     * Holds the UIMAAnnotation models that we want to lexiconize. Add here further annotations if you wish to
     * lexiconize them.
     */
    public static List<Class<? extends UIMAAnnotation>> lexiconizableAnnotations =
            new ArrayList<>(List.of(
                    NamedEntity.class,
                    Lemma.class,
                    Time.class,
                    Taxon.class,
                    BiofidTaxon.class));

    public LexiconService(PostgresqlDataInterface_Impl db) {
        this.db = db;
        this.commonConfig = new CommonConfig();
    }

    /**
     * Checks and updates the 'lexicon' for new entries or annotations that we can cache.
     */
    public int updateLexicon() {
        var tables = new ArrayList<String>();

        for (var annotation : lexiconizableAnnotations) {
            if (annotation.isAnnotationPresent(Table.class)) {
                tables.add(annotation.getAnnotation(Table.class).name().toLowerCase());
            }
        }

        var insertedLex = ExceptionUtils.tryCatchLog(()->db.callLexiconRefresh(tables),
                (ex) -> logger.error("Error updating the lexicon: ", ex));
        return insertedLex == null ? -1 : insertedLex;
    }

    public List<LexiconEntry> getEntries(int skip, int take) throws DatabaseOperationException {
        var entries = db.getManyLexiconEntries(skip, take);
        return entries;
    }

}
