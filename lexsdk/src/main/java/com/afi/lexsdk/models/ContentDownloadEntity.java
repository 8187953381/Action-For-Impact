package com.afi.lexsdk.models;

import com.afi.lexsdk.persistence.entity.ContentEntity;
import com.afi.lexsdk.persistence.entity.DownloadStatusEntity;

import java.util.ArrayList;

public class ContentDownloadEntity {
    public ArrayList<ContentEntity> contentEntities;
    public ArrayList<DownloadStatusEntity> downloadStatusEntities;

    public ContentDownloadEntity() {
        this.contentEntities = new ArrayList<>();
        this.downloadStatusEntities = new ArrayList<>();
    }
}
