package org.refactoringminer.astDiff.models;

import gr.uom.java.xmi.diff.UMLModelDiff;

import java.util.Map;

public class ProjectData {

    UMLModelDiff umlModelDiff;
    Map<String, String> fileContentsBefore;
    Map<String, String> fileContentsCurrent;


    public Map<String, String> getFileContentsBefore() {
        return fileContentsBefore;
    }

    public Map<String, String> getFileContentsCurrent() {
        return fileContentsCurrent;
    }


    public UMLModelDiff getUmlModelDiff() {
        return umlModelDiff;
    }

    public void setUmlModelDiff(UMLModelDiff umlModelDiff) {
        this.umlModelDiff = umlModelDiff;
    }

    public void setFileContentsBefore(Map<String, String> fileContentsBefore) {
        this.fileContentsBefore = fileContentsBefore;
    }

    public void setFileContentsCurrent(Map<String, String> fileContentsCurrent) {
        this.fileContentsCurrent = fileContentsCurrent;
    }

}
