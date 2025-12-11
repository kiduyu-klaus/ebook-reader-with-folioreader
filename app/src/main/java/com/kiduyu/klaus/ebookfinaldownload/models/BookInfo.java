package com.kiduyu.klaus.ebookfinaldownload.models;

import java.util.ArrayList;
import java.util.List;

public class BookInfo {
    private String bookUrl;
    private String title = "Unknown";
    private String author = "Unknown";
    private String language = "Unknown";
    private String epubSize;
    private String pdfSize;
    private String downlink;
    private List<DownloadLink> downloadLinks = new ArrayList<>();

    public BookInfo() {
    }

    public String getBookUrl() {
        return bookUrl;
    }

    public void setBookUrl(String bookUrl) {
        this.bookUrl = bookUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getDownlink() {
        return downlink;
    }

    public void setDownlink(String downlink) {
        this.downlink = downlink;
    }

    public String getEpubSize() {
        return epubSize;
    }

    public void setEpubSize(String epubSize) {
        this.epubSize = epubSize;
    }

    public String getPdfSize() {
        return pdfSize;
    }

    public void setPdfSize(String pdfSize) {
        this.pdfSize = pdfSize;
    }

    public List<DownloadLink> getDownloadLinks() {
        return downloadLinks;
    }

    public void setDownloadLinks(List<DownloadLink> downloadLinks) {
        this.downloadLinks = downloadLinks;
    }

    public void addDownloadLink(DownloadLink link) {
        this.downloadLinks.add(link);
    }

    public void addDownloadLink(String url) {
        DownloadLink link = new DownloadLink();
        link.setDownlink(url);

        this.downloadLinks.add(link);
    }
}
