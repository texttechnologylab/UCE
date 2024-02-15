package org.texttechnologylab.models.corpus;

/**
 * A class that holds information which we scrape from the goethe bib page like here:
 * https://sammlungen.ub.uni-frankfurt.de/botanik/periodical/titleinfo/3671225
 */
public class GoetheTitleInfo {
    private String scrapedUrl;
    private String title;
    private String published;
    private String pdfUrl;
    private String titleImageUrl;
    private String pageViewStartUrl;

    public String getPageViewOfPage(int pageNumber){
        var start = pageViewStartUrl;
        var splited = start.split("/");
        if(splited.length == 0) return "";
        var id = Integer.parseInt(splited[splited.length - 1]);
        var finalUrl = new StringBuilder();
        for(var i = 0; i < splited.length - 1; i++){
            finalUrl.append(splited[i]).append("/");
        }
        finalUrl.append(id + pageNumber);
        return finalUrl.toString();
    }
    public String getPageViewStartUrl() {
        return pageViewStartUrl == null ? "-" : pageViewStartUrl;
    }

    public void setPageViewStartUrl(String pageViewStartUrl) {
        this.pageViewStartUrl = pageViewStartUrl;
    }

    public String getTitleImageUrl() {
        return titleImageUrl == null ? "-" : titleImageUrl;
    }

    public void setTitleImageUrl(String titleImageUrl) {
        this.titleImageUrl = titleImageUrl;
    }

    public String getPdfUrl() {
        return pdfUrl == null ? "-" : pdfUrl;
    }

    public void setPdfUrl(String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }

    public String getPublished() {
        return published == null ? "-" : published;
    }

    public void setPublished(String published) {
        this.published = published;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getScrapedUrl() {
        return scrapedUrl;
    }

    public void setScrapedUrl(String scrapedUrl) {
        this.scrapedUrl = scrapedUrl;
    }
}