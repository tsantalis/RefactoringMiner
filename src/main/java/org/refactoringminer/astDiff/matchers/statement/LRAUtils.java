package org.refactoringminer.astDiff.matchers.statement;

import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.utils.Pair;

import java.util.List;

//LeftRightAware
public class LRAUtils {
    //LeftRightAware + ify

    //Technically it should not be necessarily to the pass the existing match to LRAify,
    //However breaking the whole tree, into left and right, prevents some matches (i.e. the entire Assignment statement)
    //For such instances, match can be passed and further optimized during the LRAify process.

    public static List<Pair<Tree, Tree>> LRAify(Tree src, Tree dst, MappingStore match) {
        for (LRAScenarios value : LRAScenarios.values()) {
            if (value.condition.test(src, dst)) {
                return value.makePairs(src, dst, match);
            }
        }
        return List.of(new Pair<>(src, dst)); // Fallback case if no scenario matches
    }
}

