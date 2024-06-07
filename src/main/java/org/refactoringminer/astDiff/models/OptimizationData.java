package org.refactoringminer.astDiff.models;

import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;

import java.util.List;

public class OptimizationData {
    private final List<AbstractCodeMapping> lastStepMappings;
    private final ExtendedMultiMappingStore subtreeMappings;

    public List<AbstractCodeMapping> getLastStepMappings() {
        return lastStepMappings;
    }

    public ExtendedMultiMappingStore getSubtreeMappings() {
        return subtreeMappings;
    }

    public OptimizationData(List<AbstractCodeMapping> lastStepMappings, ExtendedMultiMappingStore subtreeMappings) {
        this.lastStepMappings = lastStepMappings;
        this.subtreeMappings = subtreeMappings;
    }

}
