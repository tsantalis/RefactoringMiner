package sortpom.parameter;

import org.junit.jupiter.api.Test;
import sortpom.util.SortPomImplUtil;

class KeepTimestampParameterTest {

    @Test
    final void whenKeepTimestampNotSetTimestampsShouldDiffer() throws Exception {
        SortPomImplUtil.create()
            .defaultOrderFileName("difforder/differentOrder.xml")
            .lineSeparator("\n")
            .keepTimestamp(false)
            .testFilesWithTimestamp("/full_unsorted_input.xml", "/sortOrderFiles/sorted_differentOrder.xml");
    }

    @Test
    final void whenKeepTimestampIsSetTimestampsShouldRemain() throws Exception {
    	SortPomImplUtil.create()
	    	.defaultOrderFileName("difforder/differentOrder.xml")
	    	.lineSeparator("\n")
	    	.keepTimestamp(true)
	    	.testFilesWithTimestamp("/full_unsorted_input.xml", "/sortOrderFiles/sorted_differentOrder.xml");
    }

}
