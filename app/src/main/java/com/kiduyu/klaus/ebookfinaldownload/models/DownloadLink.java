package com.kiduyu.klaus.ebookfinaldownload.models;

public class DownloadLink {
    private String id;
    private String filename;

    private String format;

    private String downlink;
    public DownloadLink() {
    }

    public DownloadLink(String id, String filename, String format, String downlink) {
        this.id = id;
        this.filename = filename;
        this.format = format;
        this.downlink = downlink;
    }

    public String getDownlink() {
        return downlink;
    }

    public void setDownlink(String downlink) {
        this.downlink = downlink;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }
}
