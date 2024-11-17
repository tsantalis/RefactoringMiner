package com.novoda.downloadmanager.lib;

class BatchInfo {
    private final String title;
    private final String description;
    private final String bigPictureUrl;

    public BatchInfo(String title, String description, String bigPictureUrl) {
        this.title = title;
        this.description = description;
        this.bigPictureUrl = bigPictureUrl;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getBigPictureUrl() {
        return bigPictureUrl;
    }
}
