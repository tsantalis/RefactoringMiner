package com.novoda.downloadmanager.lib;

import android.content.ContentValues;

import java.util.ArrayList;
import java.util.List;

public class RequestBatch {

    private final BatchInfo batchInfo;
    private final List<Request> requests;

    RequestBatch(BatchInfo batchInfo, List<Request> requests) {
        this.batchInfo = batchInfo;
        this.requests = requests;
    }

    public String getTitle() {
        return batchInfo.getTitle();
    }

    public String getDescription() {
        return batchInfo.getDescription();
    }

    public String getBigPictureUrl() {
        return batchInfo.getBigPictureUrl();
    }

    List<Request> getRequests() {
        return requests;
    }

    public void addRequest(Request request) {
        requests.add(request);
    }

    ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        values.put(Downloads.Impl.Batches.COLUMN_TITLE, batchInfo.getTitle());
        values.put(Downloads.Impl.Batches.COLUMN_DESCRIPTION, batchInfo.getDescription());
        values.put(Downloads.Impl.Batches.COLUMN_BIG_PICTURE, batchInfo.getBigPictureUrl());
        return values;
    }

    public static class Builder {

        private String title;
        private String description;
        private String bigPictureUrl;

        public Builder withTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder withBigPictureUrl(String bigPictureUrl) {
            this.bigPictureUrl = bigPictureUrl;
            return this;
        }


        public RequestBatch build() {
            BatchInfo batchInfo = new BatchInfo(title, description, bigPictureUrl);
            return new RequestBatch(batchInfo, new ArrayList<Request>());
        }

    }
}
