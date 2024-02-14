package org.texttechnologylab.services;

import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import org.texttechnologylab.config.CommonConfig;
import org.texttechnologylab.models.corpus.GoetheTitleInfo;

@Service
public class GoetheUniversityService {

    private final CommonConfig config;

    public GoetheUniversityService(){
        config = new CommonConfig();
    }

    /**
     * Scrapes data from the titleinfo of a given document, e.g. here:
     * https://sammlungen.ub.uni-frankfurt.de/botanik/periodical/titleinfo/3671225
     */
    public GoetheTitleInfo scrapeDocumentTitleInfo(String documentId){

        var goetheTitleInfo = new GoetheTitleInfo();

        try{
            var baseUrl = config.getUniversityBotanikBaseUrl();
            var url = baseUrl.replace("{ID}", documentId);
            goetheTitleInfo.setScrapedUrl(url);

            var doc = Jsoup.connect(url).get();
            var titleInfo = doc.select("table[id='titleInfoMetadata']");

            var title = titleInfo.select(".value, .title").select(".valueDiv").html();
            goetheTitleInfo.setTitle(title);

            var published = titleInfo.select("#mods_originInfoNotEditionElectronicEdition .value").select("a").html();
            goetheTitleInfo.setPublished(published);

        }catch (Exception ex){
            // TODO: Logging!
        }

        return goetheTitleInfo;
    }

}
