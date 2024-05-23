package org.refactoringminer.astDiff.actions.editscript;

import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import org.refactoringminer.astDiff.actions.model.MultiMove;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/* Created by pourya on 2024-05-03*/
public class SimplifiedMultiMoveActionGenerator extends MultiMoveActionGenerator {

    private List<Action> actions;

    @Override
    public EditScript computeActions(ExtendedMultiMappingStore mappings, Map<String, TreeContext> parentContextMap, Map<String, TreeContext> childContextMap) {
        actions = toArrayList(super.computeActions(mappings, parentContextMap, childContextMap));
        return simplify();
    }

    private EditScript simplify() {
        removeActionFromMap(actionMapSrc);
        removeActionFromMap(actionMapDst);
        return toEditScript(actions);
    }

    private void removeActionFromMap(Map<Tree, List<MultiMove>> target) {
        for (Tree t : target.keySet()) {
            if (target.containsKey(t.getParent())
                    && target.keySet().containsAll(t.getParent().getDescendants()))
                removeActionsForParticularTreeFromMap(t, target);
            else {
                if (!t.getChildren().isEmpty() && target.keySet().containsAll(t.getDescendants())) {

                }
            }
        }
    }

    private void removeActionsForParticularTreeFromMap(Tree t, Map<Tree, List<MultiMove>> map) {
        List<Action> removable = new ArrayList<>();
        boolean _flag = false;
        for(MultiMove action : map.get(t)) {
            if (action.isUpdated())
            {
                _flag = true;
                break;
            }
            else {
                Tree actionSrc = action.getNode();
                Tree actionDst = action.getParent();
                if (       actionMapSrc.containsKey(actionSrc.getParent())
                        && actionMapSrc.keySet().containsAll(actionSrc.getParent().getDescendants())
                        && actionMapDst.containsKey(actionDst.getParent())
                        && actionMapDst.keySet().containsAll(actionDst.getParent().getDescendants()))
                    removable.add(action);
                else {
                    _flag = true;
                    break;
                }
            }
        }
        if (!_flag)
            actions.removeAll(removable);
    }


    private static List<Action> toArrayList(Iterable<Action> iterable) {
        List<Action> arrayList = new ArrayList<>();
        for (Action action : iterable) {
            arrayList.add(action);
        }
        return arrayList;
    }


}
