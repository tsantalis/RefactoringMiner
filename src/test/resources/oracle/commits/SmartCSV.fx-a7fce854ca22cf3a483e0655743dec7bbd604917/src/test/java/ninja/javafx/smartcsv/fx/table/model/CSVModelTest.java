/*
   The MIT License (MIT)
   -----------------------------------------------------------------------------

   Copyright (c) 2015-2019 javafx.ninja <info@javafx.ninja>

   Permission is hereby granted, free of charge, to any person obtaining a copy
   of this software and associated documentation files (the "Software"), to deal
   in the Software without restriction, including without limitation the rights
   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
   copies of the Software, and to permit persons to whom the Software is
   furnished to do so, subject to the following conditions:

   The above copyright notice and this permission notice shall be included in
   all copies or substantial portions of the Software.

   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
   AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
   LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
   THE SOFTWARE.

*/

package ninja.javafx.smartcsv.fx.table.model;

import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

/**
 * unit test for the csv model
 */
public class CSVModelTest {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // constants
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    static final String TESTHEADER = "TESTHEADER";
    static final String TESTVALUE = "TESTVALUE";
    static final String FILEPATH = "filepath";

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // subject under test
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    CSVModel sut = new CSVModel();

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // tests
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @Test
    public void fresh_model_has_empty_rows() {
        // assertion
        assertThat(sut.getRows(), empty());
    }

    @Test
    public void adds_a_new_row_into_row_list() {
        // execution
        CSVRow newRow = sut.addRow();

        // assertion
        assertThat(sut.getRows(), contains(newRow));
    }

    @Test
    public void new_row_has_last_index_of_list_as_rownumber() {
        // execution
        CSVRow newRow = sut.addRow();

        // assertion
        assertThat(sut.getRows().indexOf(newRow), is(newRow.getRowNumber()));
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // private methods
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void setup_model_with_one_row_one_column_and_value() {
        sut.setHeader(new String[] {TESTHEADER});
        sut.addRow().addValue(TESTHEADER, TESTVALUE);
    }

}