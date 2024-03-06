package org.texttechnologylab.services;

import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import org.texttechnologylab.config.CommonConfig;
import org.texttechnologylab.models.corpus.MetadataTitleInfo;

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
    public MetadataTitleInfo scrapeDocumentTitleInfo(String documentId){

        var metadataTitleInfo = new MetadataTitleInfo();

        try{
            var baseUrl = config.getUniversityBotanikBaseUrl();
            var url = baseUrl.replace("{ID}", documentId);
            metadataTitleInfo.setScrapedUrl(url);

            var doc = Jsoup.connect(url).get();
            var titleInfo = doc.select("table[id='titleInfoMetadata']");

            var title = titleInfo.select(".value, .title").select(".valueDiv").html();
            metadataTitleInfo.setTitle(title);

            var titleImageUrl = doc.select("td[id='td-titleInfoImage']").select("img").attr("src");
            metadataTitleInfo.setTitleImageUrl(config.getUniversityCollectionBaseUrl() + titleImageUrl);

            var pageViewStartUrl = doc.select("td[id='td-titleInfoImage']").select("a").attr("href");
            metadataTitleInfo.setPageViewStartUrl(config.getUniversityCollectionBaseUrl() + pageViewStartUrl);

            var pdfUrl = doc.select("table[id='titleInfoLinkActions'] #titleinfoDownloads").select("a").attr("href");
            metadataTitleInfo.setPdfUrl(config.getUniversityCollectionBaseUrl() + pdfUrl);

            var published = titleInfo.select("#mods_originInfoNotEditionElectronicEdition .value").select("a").html();
            metadataTitleInfo.setPublished(published);

        }catch (Exception ex){
            // TODO: Logging!
        }

        return metadataTitleInfo;
    }

}
