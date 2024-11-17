/*
 * This project is licensed under the open source MPL V2.
 * See https://github.com/openMF/android-client/blob/master/LICENSE.md
 */

package com.mifos.objects;

/**
 * Created by ishankhanna on 14/02/14.
 */
public class SearchedEntity {

    private int entityId;
    private String entityAccountNo;
    private String entityName;
    private String entityType;
    private int parentId;
    private String parentName;

    public int getEntityId() {
        return entityId;
    }

    public void setEntityId(int entityId) {
        this.entityId = entityId;
    }

    public SearchedEntity withEntityId(int entityId) {
        this.entityId = entityId;
        return this;
    }

    public String getEntityAccountNo() {
        return entityAccountNo;
    }

    public void setEntityAccountNo(String entityAccountNo) {
        this.entityAccountNo = entityAccountNo;
    }

    public SearchedEntity withEntityAccountNo(String entityAccountNo) {
        this.entityAccountNo = entityAccountNo;
        return this;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public SearchedEntity withEntityName(String entityName) {
        this.entityName = entityName;
        return this;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public SearchedEntity withEntityType(String entityType) {
        this.entityType = entityType;
        return this;
    }

    public int getParentId() {
        return parentId;
    }

    public void setParentId(int parentId) {
        this.parentId = parentId;
    }

    public SearchedEntity withParentId(int parentId) {
        this.parentId = parentId;
        return this;
    }

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    public SearchedEntity withParentName(String parentName) {
        this.parentName = parentName;
        return this;
    }

    public String getDescription() {
        return "#" + getEntityId() + " - " + getEntityName();
    }

}
