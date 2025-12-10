package com.kiduyu.klaus.ebookfinaldownload.models;

public class BookItem {
    private String title;
    private String filePath;
    private String size;
    private String date;

    public BookItem() {
    }

    public BookItem(String title, String filePath, String size, String date) {
        this.title = title;
        this.filePath = filePath;
        this.size = size;
        this.date = date;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}