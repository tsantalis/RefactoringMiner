/**
 * DataCleaner (community edition)
 * Copyright (C) 2014 Neopost - Customer Information Management
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.datacleaner.beans.transform;

import java.util.List;

import junit.framework.TestCase;

import org.datacleaner.api.InputColumn;
import org.datacleaner.api.OutputColumns;
import org.datacleaner.beans.transform.RemoveDictionaryMatchesTransformer.RemovedMatchesType;
import org.datacleaner.configuration.DataCleanerConfigurationImpl;
import org.datacleaner.data.MockInputColumn;
import org.datacleaner.reference.Dictionary;
import org.datacleaner.reference.SimpleDictionary;
import org.junit.Test;

public class RemoveDictionaryMatchesTransformerTest extends TestCase {

    private final InputColumn<String> col = new MockInputColumn<>("Job title");
    private final Dictionary dictionary = new SimpleDictionary("Title adjectives", "Junior", "Senior", "Lead",
            "Principal", "Assistant to");
    private RemoveDictionaryMatchesTransformer transformer;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        transformer = new RemoveDictionaryMatchesTransformer(col, dictionary, new DataCleanerConfigurationImpl());
        transformer.init();
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        transformer.close();
    }

    @Test
    public void testGetOutputColumns() throws Exception {
        transformer._removedMatchesType= RemovedMatchesType.STRING;
        final OutputColumns outputColumns = transformer.getOutputColumns();
        assertEquals("OutputColumns[Job title (Title adjectives removed), Removed matches]", outputColumns.toString());
        assertEquals(String.class, outputColumns.getColumnType(1));
        
        transformer._removedMatchesType= RemovedMatchesType.LIST;
        assertEquals("OutputColumns[Job title (Title adjectives removed), Removed matches]", transformer.getOutputColumns().toString());
        assertEquals(List.class,transformer.getOutputColumns().getColumnType(1));
    }

    public void testJobTitleScenarioRemovedMatchesAsString() throws Throwable {
        
        transformer._removedMatchesType= RemovedMatchesType.STRING;
        
        assertEquals("", transformer.transform("")[0]);
        assertEquals("", transformer.transform("")[1]);
        
        assertEquals("Software Engineer", transformer.transform("Senior Software Engineer")[0]);
        assertEquals("Senior", transformer.transform("Senior Software Engineer")[1]);
        
        assertEquals("    Designer  ", transformer.transform("  Lead   Designer  ")[0]);
        assertEquals("Lead", transformer.transform("  Lead   Designer  ")[1]);
        
        assertEquals("Software  Engineer", transformer.transform("Software  Engineer")[0]);
        assertEquals("", transformer.transform("Software  Engineer")[1]);
        
        assertEquals("Guru of employees", transformer.transform("Principal Senior Lead Guru of Junior employees")[0]);
        assertEquals("Principal Junior Senior Lead", transformer.transform("Principal Senior Lead Guru of Junior employees")[1]);
   
    }
    public void testJobTitleScenarioRemovedMatchesAsList() throws Throwable {
        transformer._removedMatchesType= RemovedMatchesType.LIST;
        
        assertEquals("Software Engineer", transformer.transform("Senior Software Engineer")[0]);
        assertEquals("[Senior]", transformer.transform("Senior Software Engineer")[1].toString());
        
        assertEquals("    Designer  ", transformer.transform("  Lead   Designer  ")[0]);
        assertEquals("[Lead]", transformer.transform("  Lead   Designer  ")[1].toString());
        
        assertEquals("Software  Engineer", transformer.transform("Software  Engineer")[0]);
        assertEquals("[]", transformer.transform("Software  Engineer")[1].toString());
        
        assertEquals("Guru of employees", transformer.transform("Principal Senior Lead Guru of Junior employees")[0]);
        assertEquals("[Principal, Junior, Senior, Lead]", transformer.transform("Principal Senior Lead Guru of Junior employees")[1].toString());
        
        assertEquals("", transformer.transform("")[0]);
        assertEquals("[]", transformer.transform("")[1].toString()); 
    }

    @Test
    public void testRemoveMultiTokenPart() throws Exception {
        // we currently do not support this...

        // assertEquals("CEO", transformer.transform("Assistant to the CEO"));
    }
}
