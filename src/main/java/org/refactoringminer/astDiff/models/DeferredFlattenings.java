package org.refactoringminer.astDiff.models;

import com.github.gumtreediff.tree.Tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The flattenings of the source (Java) and target (Kotlin) trees, which are deferred until all diffs are matched,
 * so that the same tree has the same structure for all the trees it is matched with,
 * i.e., a statement of a method inlined to multiple call sites.
 */
public class DeferredFlattenings {
    private enum Kind {
        //flatten the child, removing all its mappings
        REMOVE_ALL_MAPPINGS,
        //flatten the child, removing only its mappings to the given targets
        REMOVE_MAPPINGS_TO_TARGETS,
        //flatten the child, keeping its mappings
        KEEP_MAPPINGS,
        //flatten the child of a target tree, removing all mappings to it
        REMOVE_ALL_TARGET_MAPPINGS,
        //remove the child of a target tree, removing all mappings to it
        REMOVE_TARGET_CHILD,
        //remove the leaf child of a source tree, removing all its mappings, so that the parent gets its label
        ABSORB_LEAF_CHILD,
        //Java VariableDeclarationFragment -> [name, =, initializer], kept as the leaf matching the Kotlin = operator, or flattened
        VARIABLE_DECLARATION_FRAGMENT
    }

    private static class Flattening {
        private final Kind kind;
        private final Tree parent;
        private final Tree child;
        private final Set<Tree> targets = Collections.newSetFromMap(new IdentityHashMap<>());
        private final String affectationOperatorType;

        private Flattening(Kind kind, Tree parent, Tree child, String affectationOperatorType) {
            this.kind = kind;
            this.parent = parent;
            this.child = child;
            this.affectationOperatorType = affectationOperatorType;
        }
    }

    //in registration order, and keyed by the child to be flattened, so that the same flattening registered by multiple mappings is applied once
    private final List<Flattening> flattenings = new ArrayList<>();
    private final Map<Tree, Flattening> flatteningByChild = new IdentityHashMap<>();

    public void flattenRemovingAllMappings(Tree parent, Tree child) {
        register(Kind.REMOVE_ALL_MAPPINGS, parent, child, null);
    }

    public void flattenRemovingMappingTo(Tree parent, Tree child, Tree target) {
        register(Kind.REMOVE_MAPPINGS_TO_TARGETS, parent, child, null).targets.add(target);
    }

    public void flattenKeepingMappings(Tree parent, Tree child) {
        register(Kind.KEEP_MAPPINGS, parent, child, null);
    }

    public void flattenTargetRemovingAllMappings(Tree parent, Tree child) {
        register(Kind.REMOVE_ALL_TARGET_MAPPINGS, parent, child, null);
    }

    public void removeTargetChild(Tree parent, Tree child) {
        register(Kind.REMOVE_TARGET_CHILD, parent, child, null);
    }

    public void absorbLeafChild(Tree parent, Tree child) {
        register(Kind.ABSORB_LEAF_CHILD, parent, child, null);
    }

    public void alignVariableDeclarationFragment(Tree declaration, Tree fragment, String affectationOperatorType) {
        register(Kind.VARIABLE_DECLARATION_FRAGMENT, declaration, fragment, affectationOperatorType);
    }

    private Flattening register(Kind kind, Tree parent, Tree child, String affectationOperatorType) {
        Flattening flattening = flatteningByChild.get(child);
        if(flattening == null) {
            flattening = new Flattening(kind, parent, child, affectationOperatorType);
            flatteningByChild.put(child, flattening);
            flattenings.add(flattening);
        }
        return flattening;
    }

    public boolean isEmpty() {
        return flattenings.isEmpty();
    }

    //applies the registered flattenings to the trees, removing the mappings of the flattened nodes from all mapping stores
    public void apply(Collection<ExtendedMultiMappingStore> mappingStores) {
        List<Flattening> pending = new ArrayList<>(flattenings);
        flattenings.clear();
        flatteningByChild.clear();
        for(Flattening flattening : pending) {
            Tree parent = flattening.parent;
            Tree child = flattening.child;
            //the tree has been already restructured
            if(child.getParent() != parent || parent.getChildPosition(child) < 0)
                continue;
            switch(flattening.kind) {
                case REMOVE_ALL_MAPPINGS:
                    removeMappings(mappingStores, child, null);
                    flattenChild(parent, child);
                    break;
                case REMOVE_MAPPINGS_TO_TARGETS:
                    removeMappings(mappingStores, child, flattening.targets);
                    flattenChild(parent, child);
                    break;
                case KEEP_MAPPINGS:
                    flattenChild(parent, child);
                    break;
                case REMOVE_ALL_TARGET_MAPPINGS:
                    removeTargetMappings(mappingStores, child);
                    flattenChild(parent, child);
                    break;
                case REMOVE_TARGET_CHILD:
                    removeTargetMappings(mappingStores, child);
                    parent.getChildren().remove(parent.getChildPosition(child));
                    break;
                case ABSORB_LEAF_CHILD:
                    removeMappings(mappingStores, child, null);
                    applyLeafChildAbsorption(parent, child);
                    break;
                case VARIABLE_DECLARATION_FRAGMENT:
                    if(child.getChildren().size() > 1 && isMappedToType(mappingStores, child, flattening.affectationOperatorType)) {
                        //keep the fragment as the leaf matching the = operator, placed between the name and the initializer
                        int index = parent.getChildPosition(child);
                        Tree name = child.getChild(0);
                        List<Tree> rest = new ArrayList<>(child.getChildren().subList(1, child.getChildren().size()));
                        child.getChildren().clear();
                        parent.getChildren().add(index, name);
                        name.setParent(parent);
                        parent.getChildren().addAll(index + 2, rest);
                        for(Tree t : rest)
                            t.setParent(parent);
                    }
                    else {
                        removeMappings(mappingStores, child, null);
                        flattenChild(parent, child);
                    }
                    break;
            }
        }
    }

    private static boolean isMappedToType(Collection<ExtendedMultiMappingStore> mappingStores, Tree src, String type) {
        for(ExtendedMultiMappingStore mappingStore : mappingStores) {
            Set<Tree> dsts = mappingStore.getDsts(src);
            if(dsts != null) {
                for(Tree dst : dsts) {
                    if(dst.getType().name.equals(type))
                        return true;
                }
            }
        }
        return false;
    }

    //targets == null removes all mappings of the source node
    private static void removeMappings(Collection<ExtendedMultiMappingStore> mappingStores, Tree src, Set<Tree> targets) {
        for(ExtendedMultiMappingStore mappingStore : mappingStores) {
            Set<Tree> dsts = mappingStore.getDsts(src);
            if(dsts != null) {
                for(Tree dst : new ArrayList<>(dsts)) {
                    if(targets == null || targets.contains(dst)) {
                        mappingStore.removeMapping(src, dst);
                    }
                }
            }
        }
    }

    private static void removeTargetMappings(Collection<ExtendedMultiMappingStore> mappingStores, Tree dst) {
        for(ExtendedMultiMappingStore mappingStore : mappingStores) {
            Set<Tree> srcs = mappingStore.getSrcs(dst);
            if(srcs != null) {
                for(Tree src : new ArrayList<>(srcs)) {
                    mappingStore.removeMapping(src, dst);
                }
            }
        }
    }

    public static void applyLeafChildAbsorption(Tree parent, Tree child) {
        parent.setLabel(child.getLabel());
        parent.getChildren().remove(parent.getChildPosition(child));
    }

    private static void flattenChild(Tree parent, Tree child) {
        int index = parent.getChildPosition(child);
        parent.getChildren().remove(index);
        parent.getChildren().addAll(index, child.getChildren());
        for(Tree t : child.getChildren())
            t.setParent(parent);
    }
}
