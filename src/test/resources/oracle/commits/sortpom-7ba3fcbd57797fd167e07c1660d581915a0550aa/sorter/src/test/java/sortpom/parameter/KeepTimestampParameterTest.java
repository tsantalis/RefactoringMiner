package sortpom.parameter;

import org.junit.Test;

import sortpom.util.SortPomImplUtil;

public class KeepTimestampParameterTest {

    @Test
    public final void whenKeepTimestampNotSetTimestampsShouldDiffer() throws Exception {
        SortPomImplUtil.create()
            .defaultOrderFileName("difforder/differentOrder.xml")
            .lineSeparator("\n")
            .keepTimestamp(false)
            .testFilesWithTimestamp("/full_unsorted_input.xml", "/sortOrderFiles/sorted_differentOrder.xml");
    }


    @Test
    public final void whenKeepTimestampIsSetTimestampsShouldRemain() throws Exception {
    	SortPomImplUtil.create()
	    	.defaultOrderFileName("difforder/differentOrder.xml")
	    	.lineSeparator("\n")
	    	.keepTimestamp(true)
	    	.testFilesWithTimestamp("/full_unsorted_input.xml", "/sortOrderFiles/sorted_differentOrder.xml");
    }
    
}
