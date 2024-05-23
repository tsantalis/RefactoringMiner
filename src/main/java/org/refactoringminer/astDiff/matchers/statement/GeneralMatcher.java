package org.refactoringminer.astDiff.matchers.statement;

import com.github.gumtreediff.tree.Tree;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.AbstractExpression;
import gr.uom.java.xmi.decomposition.CompositeStatementObject;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.matchers.TreeMatcher;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

/** Use this matcher when two code fragments must be matched. <br>
 * If you know that both fragments are composite, use {@link CompositeMatcher} instead. <br>
 * @author  Pourya Alikhani Fard pouryafard75@gmail.com
*/
public class GeneralMatcher extends BasicTreeMatcher implements TreeMatcher {
    AbstractCodeFragment st1;
    AbstractCodeFragment st2;

    public GeneralMatcher(AbstractCodeFragment st1, AbstractCodeFragment st2) {
        this.st1 = st1;
        this.st2 = st2;
    }
    @Override
    public void match(Tree src, Tree dst, ExtendedMultiMappingStore mappingStore) {
        if ((st1 instanceof CompositeStatementObject) &&  (st2 instanceof CompositeStatementObject)) {
            new CompositeMatcher((CompositeStatementObject) st1, (CompositeStatementObject) st2)
                    .match(src, dst, mappingStore);
            return;
        }
        //Corner cases;
        if (!(st1 instanceof CompositeStatementObject) &&
                (st2 instanceof CompositeStatementObject)) {
            CompositeStatementObject fragment2 = (CompositeStatementObject) st2;
            for (AbstractExpression expression : fragment2.getExpressions()) {
                Tree dstExpTree = TreeUtilFunctions.findByLocationInfo(dst, expression.getLocationInfo());
                new LeafMatcher().match(src,dstExpTree,mappingStore);
            }
        } else if ((st1 instanceof CompositeStatementObject) &&
                !(st2 instanceof CompositeStatementObject)) {
            CompositeStatementObject fragment1 = (CompositeStatementObject) st1;
            for (AbstractExpression expression : fragment1.getExpressions()) {
                Tree srcExpTree = TreeUtilFunctions.findByLocationInfo(src, expression.getLocationInfo());
                new LeafMatcher().match(srcExpTree,dst,mappingStore);
            }
        }
        else {
            new LeafMatcher().match(src,dst,mappingStore);
        }

    }

}
