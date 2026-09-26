package org.refactoringminer.astDiff.models;

import gr.uom.java.xmi.decomposition.AbstractCodeMapping;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

public class OptimizationData {
    private final List<AbstractCodeMapping> lastStepMappings;
    private final ExtendedMultiMappingStore subtreeMappings;
    //the same leaf mapping may belong to more than one body mapper (e.g., a refactoring body mapper and the enclosing method body mapper)
    private final Set<AbstractCodeMapping> processedLeafMappings = Collections.newSetFromMap(new IdentityHashMap<>());

    public List<AbstractCodeMapping> getLastStepMappings() {
        return lastStepMappings;
    }

    public ExtendedMultiMappingStore getSubtreeMappings() {
        return subtreeMappings;
    }

    public Set<AbstractCodeMapping> getProcessedLeafMappings() {
        return processedLeafMappings;
    }

    public OptimizationData(List<AbstractCodeMapping> lastStepMappings, ExtendedMultiMappingStore subtreeMappings) {
        this.lastStepMappings = lastStepMappings;
        this.subtreeMappings = subtreeMappings;
    }

}
