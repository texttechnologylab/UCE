package org.texttechnologylab.models.corpus;

/**
 * A class that holds information which we scrape from the goethe bib page like here:
 * https://sammlungen.ub.uni-frankfurt.de/botanik/periodical/titleinfo/3671225
 */
public class GoetheTitleInfo {
    private String scrapedUrl;
    private String title;
    private String published;

    public String getPublished() {
        return published.isEmpty() ? "-" : published;
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
