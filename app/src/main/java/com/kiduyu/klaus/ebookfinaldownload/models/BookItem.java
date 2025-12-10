package com.kiduyu.klaus.ebookfinaldownload.models;

public class BookItem {
    private String title;
    private String filePath;
    private String size;
    private String date;
    private String coverImagePath;

    public BookItem() {
    }

    public BookItem(String title, String filePath, String size, String date, String coverImagePath) {
        this.title = title;
        this.filePath = filePath;
        this.size = size;
        this.date = date;
        this.coverImagePath = coverImagePath;
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

    public String getCoverImagePath() {
        return coverImagePath;
    }

    public void setCoverImagePath(String coverImagePath) {
        this.coverImagePath = coverImagePath;
    }
}