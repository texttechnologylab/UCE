package org.texttechnologylab.services;

import org.springframework.stereotype.Service;
import org.texttechnologylab.config.CommonConfig;
import org.texttechnologylab.models.UIMAAnnotation;
import org.texttechnologylab.models.biofid.BiofidTaxon;
import org.texttechnologylab.models.corpus.Lemma;
import org.texttechnologylab.models.corpus.NamedEntity;
import org.texttechnologylab.models.corpus.Taxon;
import org.texttechnologylab.models.corpus.Time;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class LexiconService {

    private final PostgresqlDataInterface_Impl db;
    private final CommonConfig commonConfig;

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

    public LexiconService(PostgresqlDataInterface_Impl db){
        this.db = db;
        this.commonConfig = new CommonConfig();
    }



}
