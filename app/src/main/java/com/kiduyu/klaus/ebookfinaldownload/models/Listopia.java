package com.kiduyu.klaus.ebookfinaldownload.models;

public class Listopia {
    private String title;
    private String url;
    private int bookCount;
    private String thumbnailUrl;

    public Listopia() {
    }

    public Listopia(String title, String url, int bookCount, String thumbnailUrl) {
        this.title = title;
        this.url = url;
        this.bookCount = bookCount;
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getBookCount() {
        return bookCount;
    }

    public void setBookCount(int bookCount) {
        this.bookCount = bookCount;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }
}