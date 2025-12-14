package com.kiduyu.klaus.ebookfinaldownload.models;

public class Genre {
    private String name;
    private String url;
    private int bookCount;

    public Genre() {
    }

    public Genre(String name, String url, int bookCount) {
        this.name = name;
        this.url = url;
        this.bookCount = bookCount;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    @Override
    public String toString() {
        return "Genre{" +
                "name='" + name + '\'' +
                ", url='" + url + '\'' +
                ", bookCount=" + bookCount +
                '}';
    }
}