package org.refactoringminer.astDiff.utils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.gumtreediff.actions.model.*;
import com.github.gumtreediff.matchers.Mapping;
import org.apache.commons.io.FileUtils;
import org.refactoringminer.astDiff.models.ASTDiff;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/* Created by pourya on 2023-01-29 8:52 p.m. */
public class MappingExportModel implements Serializable {
    String firstType, secondType, firstLabel, secondLabel, firstParentType, secondParentType;
    int firstPos, secondPos, firstEndPos, secondEndPos;
    @JsonIgnore
    int firstHash, secondHash;

    MappingExportModel(){};

    public MappingExportModel(String firstType, String firstLabel, int firstPos, int firstEndPos, int firstHash, String firstParentType,
                              String secondType, String secondLabel, int secondPos, int secondEndPos, int secondHash, String secondParentType) {
        this.firstType = firstType;
        this.secondType = secondType;
        this.firstLabel = firstLabel;
        this.secondLabel = secondLabel;
        this.firstPos = firstPos;
        this.secondPos = secondPos;
        this.firstEndPos = firstEndPos;
        this.secondEndPos = secondEndPos;
        this.firstHash = firstHash;
        this.secondHash = secondHash;
        this.firstParentType = firstParentType;
        this.secondParentType = secondParentType;
    }

    public int getFirstHash() {
        return firstHash;
    }

    public int getSecondHash() {
        return secondHash;
    }

    public void setFirstHash(int firstHash) {
        this.firstHash = firstHash;
    }

    public void setSecondHash(int secondHash) {
        this.secondHash = secondHash;
    }

    public String getFirstType() {
        return firstType;
    }

    public void setFirstType(String firstType) {
        this.firstType = firstType;
    }

    public String getSecondType() {
        return secondType;
    }

    public void setSecondType(String secondType) {
        this.secondType = secondType;
    }

    public String getFirstLabel() {
        return firstLabel;
    }

    public void setFirstLabel(String firstLabel) {
        this.firstLabel = firstLabel;
    }

    public String getSecondLabel() {
        return secondLabel;
    }

    public void setSecondLabel(String secondLabel) {
        this.secondLabel = secondLabel;
    }

    public int getFirstPos() {
        return firstPos;
    }

    public void setFirstPos(int firstPos) {
        this.firstPos = firstPos;
    }

    public int getSecondPos() {
        return secondPos;
    }

    public void setSecondPos(int secondPos) {
        this.secondPos = secondPos;
    }

    public int getFirstEndPos() {
        return firstEndPos;
    }

    public void setFirstEndPos(int firstEndPos) {
        this.firstEndPos = firstEndPos;
    }

    public int getSecondEndPos() {
        return secondEndPos;
    }

    public void setSecondEndPos(int secondEndPos) {
        this.secondEndPos = secondEndPos;
    }

    public String getFirstParentType() {
        return firstParentType;
    }

    public void setFirstParentType(String firstParentType) {
        this.firstParentType = firstParentType;
    }

    public String getSecondParentType() {
        return secondParentType;
    }

    public void setSecondParentType(String secondParentType) {
        this.secondParentType = secondParentType;
    }

    @Override
    public String toString() {
        return "MappingExportModel{" +
                "firstType='" + firstType + '\'' +
                ", secondType='" + secondType + '\'' +
                ", firstLabel='" + firstLabel + '\'' +
                ", secondLabel='" + secondLabel + '\'' +
                ", firstParentType='" + firstParentType + '\'' +
                ", secondParentType='" + secondParentType + '\'' +
                ", firstPos=" + firstPos +
                ", secondPos=" + secondPos +
                ", firstEndPos=" + firstEndPos +
                ", secondEndPos=" + secondEndPos +
                ", firstHash=" + firstHash +
                ", secondHash=" + secondHash +
                '}';
    }
    public static List<MappingExportModel> exportModelList(Iterable<Mapping> mappings) {
        List<MappingExportModel> exportList = new ArrayList<>();
        for (Mapping mapping : mappings) {
            MappingExportModel mappingExportModel = new MappingExportModel(
                    mapping.first.getType().name,
                    mapping.first.getLabel(),
                    mapping.first.getPos(),
                    mapping.first.getEndPos(),
                    mapping.first.hashCode(),
                    (mapping.first.getParent() == null) ? "" : mapping.first.getParent().getType().name,
                    mapping.second.getType().name,
                    mapping.second.getLabel(),
                    mapping.second.getPos(),
                    mapping.second.getEndPos(),
                    mapping.second.hashCode(),
                    (mapping.second.getParent() == null) ? "" : mapping.second.getParent().getType().name
            );
            exportList.add(mappingExportModel);
        }
        exportList.sort(
                Comparator.comparing(
                                MappingExportModel::getFirstPos)
                        .thenComparing(exportModel -> -1 * exportModel.getFirstEndPos())
                        .thenComparing(MappingExportModel::getFirstType)
                        .thenComparing(MappingExportModel::getSecondPos)
                        .thenComparing(MappingExportModel::getFirstParentType)
                        .thenComparing(MappingExportModel::getSecondParentType)
        );
        return exportList;
    }

    public static String exportString(Iterable<Mapping> mappings) throws JsonProcessingException {
        List<MappingExportModel> mappingExportModels = exportModelList(mappings);
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(mappingExportModels);
    }

    public static void exportToFile(File outputFile, Iterable<Mapping> mappings) throws IOException {
        List<MappingExportModel> mappingExportModels = exportModelList(mappings);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, mappingExportModels);
    }

    public static void exportActions(File outputFile, ASTDiff astDiff) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (Action a : astDiff.editScript) {
            sb.append(a);
        }
        FileUtils.write(outputFile, sb.toString());
    }
}