package sortpom.parameter;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import sortpom.exception.FailureException;
import sortpom.util.SortPomImplUtil;

public class BackupFileExtensionParameterTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public final void emptyBackupFileExtensionShouldNotWork() throws Exception {
        thrown.expect(FailureException.class);
        thrown.expectMessage("Could not create backup file, extension name was empty");

        SortPomImplUtil.create()
                .backupFileExtension("")
                .defaultOrderFileName("difforder/differentOrder.xml")
                .lineSeparator("\n")
                .testFiles("/full_unsorted_input.xml", "/sortOrderFiles/sorted_differentOrder.xml");
    }

}
