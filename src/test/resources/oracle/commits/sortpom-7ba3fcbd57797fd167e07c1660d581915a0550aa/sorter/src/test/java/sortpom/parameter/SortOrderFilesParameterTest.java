package sortpom.parameter;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import sortpom.util.SortPomImplUtil;

import static org.hamcrest.Matchers.endsWith;

public class SortOrderFilesParameterTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public final void incorrectCustomSortOrderShouldThrowException() throws Exception {
        thrown.expect(RuntimeException.class);
        thrown.expectMessage(endsWith("VERYdifferentOrder.xml in classpath"));
        SortPomImplUtil.create()
                .defaultOrderFileName("difforder/VERYdifferentOrder.xml")
                .testFiles("/full_unsorted_input.xml", "/sortOrderFiles/sorted_differentOrder.xml");
    }

    @Test
    public final void incorrectPredefinedSortOrderShouldThrowException() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Cannot find abbie_normal_brain.xml among the predefined plugin resources");
        SortPomImplUtil.create()
                .predefinedSortOrder("abbie_normal_brain")
                .lineSeparator("\n")
                .testFiles("/full_unsorted_input.xml",
                        "/sortOrderFiles/sorted_default_0_4_0.xml");
    }

    @Test
    public final void incorrectCustomSortOrderShouldThrowException2() throws Exception {
        thrown.expect(RuntimeException.class);
        thrown.expectMessage(endsWith("VERYdifferentOrder.xml in classpath"));
        SortPomImplUtil.create()
                .defaultOrderFileName("difforder/VERYdifferentOrder.xml")
                .testVerifyXmlIsOrdered("/sortOrderFiles/sorted_differentOrder.xml");
    }

    @Test
    public final void incorrectPredefinedSortOrderShouldThrowException2() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Cannot find abbie_normal_brain.xml among the predefined plugin resources");
        SortPomImplUtil.create()
                .predefinedSortOrder("abbie_normal_brain")
                .lineSeparator("\n")
                .testVerifyXmlIsOrdered("/sortOrderFiles/sorted_default_0_4_0.xml");
    }

}
