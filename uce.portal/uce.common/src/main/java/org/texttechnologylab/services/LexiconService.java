package org.texttechnologylab.services;

import io.micrometer.common.lang.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.texttechnologylab.config.CommonConfig;
import org.texttechnologylab.exceptions.DatabaseOperationException;
import org.texttechnologylab.exceptions.ExceptionUtils;
import org.texttechnologylab.models.UIMAAnnotation;
import org.texttechnologylab.models.biofid.BiofidTaxon;
import org.texttechnologylab.models.biofid.GazetteerTaxon;
import org.texttechnologylab.models.biofid.GnFinderTaxon;
import org.texttechnologylab.models.corpus.*;
import org.texttechnologylab.models.negation.*;
import org.texttechnologylab.models.offensiveSpeech.OffensiveSpeech;
import org.texttechnologylab.models.topic.UnifiedTopic;
import org.texttechnologylab.models.viewModels.lexicon.LexiconOccurrenceViewModel;
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
                    GeoName.class,
                    Lemma.class,
                    Time.class,
                    GazetteerTaxon.class,
                    GnFinderTaxon.class,
                    CompleteNegation.class,
                    Focus.class,
                    Cue.class,
                    Scope.class,
                    XScope.class,
                    UnifiedTopic.class,
                    OffensiveSpeech.class));

    public LexiconService(PostgresqlDataInterface_Impl db) {
        this.db = db;
        this.commonConfig = new CommonConfig();
    }

    /**
     * Counts the amount of entries in the lexicon
     */
    public long countLexiconEntries(){
        var count = ExceptionUtils.tryCatchLog(
                db::countLexiconEntries,
                (ex) -> logger.error("Error counting the entries count in the lexicon.", ex));
        if(count == null) return -1;
        return count;
    }

    /**
     * Method that firstly checks if a lexicon update might be required and, if so determined, updates it.
     */
    public int checkForUpdates(){
        var inserts = updateLexicon(false);
        // The lexicon should be updated and calculated by the importer. This is more of a sanity check
        var entriesCount = countLexiconEntries();
        if(entriesCount == 0) return updateLexicon(true);
        return inserts;
    }

    /**
     * Checks and updates the 'lexicon' for new entries or annotations that we can cache.
     */
    public int updateLexicon(boolean forceRecalculate) {
        var tables = new ArrayList<String>();

        // Here we gather all table names of the annotations we have lexiconized.
        for (var annotation : lexiconizableAnnotations) {
            if (annotation.isAnnotationPresent(Table.class)) {
                tables.add(annotation.getAnnotation(Table.class).name().toLowerCase());
            }
        }

        var insertedLex = ExceptionUtils.tryCatchLog(()->db.callLexiconRefresh(tables, forceRecalculate),
                (ex) -> logger.error("Error updating the lexicon: ", ex));
        return insertedLex == null ? -1 : insertedLex;
    }

    /**
     * Gets occurrences of a lexicon entry, meaning the respective annotations and where they occur.
     */
    public List<LexiconOccurrenceViewModel> getOccurrenceViewModelsOfEntry(String coveredText, String type, int skip, int take){
        // Based on the simple class name in lower text, we get the actual clazz and get the occurrences generically that way.
        var annotationClass = lexiconizableAnnotations.stream().filter(l -> l.getSimpleName().toLowerCase().equals(type)).findFirst();
        var annotations = ExceptionUtils.tryCatchLog(
                () ->db.getManyUIMAAnnotationsByCoveredText(coveredText, annotationClass.get(), skip, take),
                (ex) -> logger.error("Couldn't fetch occurrences of a lexicon entry", ex));
        if(annotations == null) annotations = new ArrayList<>();

        // We build viewmodels from the UIMAAnnotations as these are used within a view/UI
        var viewModels = new ArrayList<LexiconOccurrenceViewModel>();
        for(var anno:annotations){
            var page = ExceptionUtils.tryCatchLog(
                    () -> db.getPageByDocumentIdAndBeginEnd(anno.getDocumentId(), anno.getBegin(), anno.getEnd(), false),
                    (ex) -> logger.error("Error getting a page by its documentid, begin and end.", ex));
            viewModels.add(new LexiconOccurrenceViewModel(anno, page));
        }
        return viewModels;
    }

    /**
     * Gets a paginated list of lexicon entries depending on the parameters.
     */
    public List<LexiconEntry> getEntries(int skip,
                                         int take,
                                         List<String> alphabet,
                                         List<String> annotationFilters,
                                         String sortColumn,
                                         String sortOrder,
                                         String searchInput) throws DatabaseOperationException {
        var entries = db.getManyLexiconEntries(skip, take, alphabet, annotationFilters, sortColumn, sortOrder, searchInput);
        return entries;
    }

}
