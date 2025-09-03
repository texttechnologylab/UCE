package org.texttechnologylab.uce.common.services;

import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import org.texttechnologylab.uce.common.config.CommonConfig;
import org.texttechnologylab.uce.common.models.corpus.MetadataTitleInfo;
import org.texttechnologylab.uce.common.models.util.HealthStatus;
import org.texttechnologylab.uce.common.utils.SystemStatus;

import java.io.IOException;

@Service
public class GoetheUniversityService {
    private CommonConfig config;
    public GoetheUniversityService() {
        try{
            config = new CommonConfig();
            SystemStatus.GoetheUniversityServiceStatus = new HealthStatus(true, "", null);
        } catch (Exception ex){
            SystemStatus.GoetheUniversityServiceStatus = new HealthStatus(false, "Couldn't init the GoetheUniversityService", ex);
        }
    }

    /**
     * Scrapes data from the titleinfo of a given document, e.g. here:
     * https://sammlungen.ub.uni-frankfurt.de/botanik/periodical/titleinfo/3671225
     */
    public MetadataTitleInfo scrapeDocumentTitleInfo(String documentId) throws IOException {

        var metadataTitleInfo = new MetadataTitleInfo();

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

        return metadataTitleInfo;
    }
}
