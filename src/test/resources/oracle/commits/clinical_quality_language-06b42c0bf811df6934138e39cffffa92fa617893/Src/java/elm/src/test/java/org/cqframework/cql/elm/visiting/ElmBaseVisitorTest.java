package org.cqframework.cql.elm.visiting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.ArrayList;

import javax.xml.namespace.QName;

import org.cqframework.cql.elm.tracking.Trackable;
import org.hl7.cql_annotations.r1.Narrative;
import org.hl7.elm.r1.ByDirection;
import org.hl7.elm.r1.Element;
import org.hl7.elm.r1.Library;
import org.hl7.elm.r1.Sort;
import org.hl7.elm.r1.SortByItem;
import org.hl7.elm.r1.TypeSpecifier;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.ObjenesisObjectFactory;
import org.jeasy.random.api.ExclusionPolicy;
import org.jeasy.random.api.RandomizerContext;
import org.junit.Test;

public class ElmBaseVisitorTest {

    @Test
    public void sortByVisited() {

        // set up visitor that returns true if it visits a SortByItem
        var sortByFinder = new ElmBaseVisitor<Boolean, Void>() {
            @Override
            public Boolean defaultResult(Trackable t, Void context) {
                if (t instanceof SortByItem) {
                    return true;
                }

                return false;
            }

            @Override
            public Boolean aggregateResult(Boolean aggregate, Boolean nextResult) {
                return aggregate || nextResult;
            }
        };

        var sort = new Sort();
        assertFalse(sortByFinder.visitSort(sort, null));

        sort.getBy().add(new ByDirection());
        assertTrue(sortByFinder.visitSort(sort, null));
    }

    @Test
    // This test generates a random ELM graph and verifies that all nodes are
    // visited
    public void allNodesVisited() {
        var elementsGenerated = new ArrayList<Element>();
        var countingObjectFactory = new ObjenesisObjectFactory() {
            @Override
            public <T> T createInstance(Class<T> type, RandomizerContext context) {
                var t = super.createInstance(type, context);
                if (t instanceof Element) {
                    elementsGenerated.add((Element) t);
                }
                return t;
            }
        };

        var randomParams = new EasyRandomParameters()
                .objectFactory(countingObjectFactory)
                .seed(123L)
                .randomizationDepth(10)
                .charset(Charset.forName("UTF-8"))
                .stringLengthRange(5, 50)
                .collectionSizeRange(1, 3)
                .exclusionPolicy(new NoTypeSpecifierRecursionPolicy())
                .scanClasspathForConcreteTypes(true);

        var randomElmGenerator = new EasyRandom(randomParams);
        var randomElm = randomElmGenerator.nextObject(Library.class);

        var elementsGeneratedCount = elementsGenerated.size();
        assertTrue(elementsGeneratedCount > 0); // Sanity check that the elm generator ran

        var elementsVisited = new ArrayList<Element>();
        var elementsDuplicated = new ArrayList<Element>();
        var countingVisitor = new ElmFunctionalVisitor<Integer, ArrayList<Element>>(
                (x, y) -> {
                    if (x instanceof Element) {
                        if (!elementsVisited.contains(x)) {
                            elementsVisited.add((Element) x);
                            return 1;
                        }

                        elementsDuplicated.add((Element)x);
                        return 0;
                    }
                    return 0;
                },
                (a, b) -> a + b);

        var visitorCount = countingVisitor.visitLibrary(randomElm, elementsVisited);

        elementsGenerated.removeAll(elementsVisited);
        // This should be a no-op if all the elements were visited correctly.
        // Otherwise, it helps with debugging.
        elementsGenerated.forEach(x -> System.err.println(x.getClass().getSimpleName()));

        // If we didn't visit all the elements the size won't be zero.
        assertEquals(0, elementsGenerated.size());

        // This ensures we didn't double visit any elements
        assertEquals(0, elementsDuplicated.size());

        // If _did_ visit all the elements and _didn't_ double count any nodes
        // and the counts here are different that indicates that we missed a call
        // to aggregateResult somewhere.
        assertEquals(elementsGeneratedCount, visitorCount.intValue());

        // 70 is the count I get with the current seed and max depth settings.
        // This will change based on the random generation settings.
        assertEquals(70, elementsGeneratedCount);
    }

    class NoTypeSpecifierRecursionPolicy implements ExclusionPolicy {

        // Don't recurse into TypeSpecifier.resultTypeSpecifier
        @Override
        public boolean shouldBeExcluded(Field field, RandomizerContext context) {
            return field.getName().toLowerCase().endsWith("specifier")
                    && TypeSpecifier.class.isAssignableFrom(field.getType());
        }

        @Override
        public boolean shouldBeExcluded(Class<?> type, RandomizerContext context) {
            return type == QName.class || type == Narrative.class;
        }
    }
}