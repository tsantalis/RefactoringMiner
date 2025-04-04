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

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.metamodel.util.HasName;
import org.datacleaner.api.Alias;
import org.datacleaner.api.Categorized;
import org.datacleaner.api.Close;
import org.datacleaner.api.Configured;
import org.datacleaner.api.Description;
import org.datacleaner.api.ExternalDocumentation;
import org.datacleaner.api.ExternalDocumentation.DocumentationLink;
import org.datacleaner.api.ExternalDocumentation.DocumentationType;
import org.datacleaner.api.HasLabelAdvice;
import org.datacleaner.api.Initialize;
import org.datacleaner.api.InputColumn;
import org.datacleaner.api.InputRow;
import org.datacleaner.api.OutputColumns;
import org.datacleaner.api.Provided;
import org.datacleaner.api.Transformer;
import org.datacleaner.components.categories.ImproveSuperCategory;
import org.datacleaner.components.categories.ReferenceDataCategory;
import org.datacleaner.configuration.DataCleanerConfiguration;
import org.datacleaner.reference.SynonymCatalog;
import org.datacleaner.reference.SynonymCatalogConnection;

import com.google.common.base.Joiner;

/**
 * A simple transformer that uses a synonym catalog to replace a synonym with
 * it's master term.
 */
@Named("Synonym lookup")
@Alias("Synonym replacement")
@Description("Replaces strings with their synonyms")
@ExternalDocumentation({
        @DocumentationLink(title = "Segmenting customers on messy data", url = "https://www.youtube.com/watch?v=iy-j5s-uHz4", type = DocumentationType.VIDEO, version = "4.0"),
        @DocumentationLink(title = "Understanding and using Synonyms", url = "https://www.youtube.com/watch?v=_YiPaA8bFt4", type = DocumentationType.VIDEO, version = "2.0") })
@Categorized(superCategory = ImproveSuperCategory.class, value = ReferenceDataCategory.class)
public class SynonymLookupTransformer implements Transformer, HasLabelAdvice {
    public enum ReplacedSynonymsType implements HasName {
        STRING("String"), LIST("List");

        private final String _name;

        ReplacedSynonymsType(String name) {
            _name = name;
        }

        @Override
        public String getName() {
            return _name;
        }
    }

    @Configured
    InputColumn<String> column;

    @Configured
    SynonymCatalog synonymCatalog;

    @Configured
    @Description("Retain original value in case no synonym is found (otherwise null)")
    boolean retainOriginalValue = true;

    @Configured
    @Description("Tokenize and look up every token of the input, rather than looking up the complete input string?")
    boolean lookUpEveryToken = false;

    @Inject
    @Configured
    @Description("How should the synonyms and the master terms that replaced them be returned?" +" As a concatenated String or as a List.")
    ReplacedSynonymsType replacedSynonymsType = ReplacedSynonymsType.STRING;

    @Provided
    DataCleanerConfiguration configuration;

    private SynonymCatalogConnection synonymCatalogConnection;

    public SynonymLookupTransformer() {
    }

    public SynonymLookupTransformer(InputColumn<String> column, SynonymCatalog synonymCatalog,
            boolean retainOriginalValue, DataCleanerConfiguration configuration) {
        this();
        this.column = column;
        this.synonymCatalog = synonymCatalog;
        this.retainOriginalValue = retainOriginalValue;
        this.configuration = configuration;
    }

    @Override
    public OutputColumns getOutputColumns() {
        if (lookUpEveryToken) {
            final Class[] columnTypes;
            if( replacedSynonymsType == ReplacedSynonymsType.STRING) {
                columnTypes = new Class[] { String.class, String.class, String.class };
            } else {
                columnTypes = new Class[] { String.class, List.class, List.class };
            }

            return new OutputColumns(
                    new String[] { column.getName() + " (synonyms replaced)", column.getName() + " (synonyms found)",
                            column.getName() + " (master terms found)" }, columnTypes);
        } else {
            return new OutputColumns(String.class,
                    new String[] { column.getName() + " (synonyms replaced)" });
        }
    }

    @Override
    public String getSuggestedLabel() {
        if (synonymCatalog == null) {
            return null;
        }
        return "Lookup: " + synonymCatalog.getName();
    }

    @Initialize
    public void init() {
        synonymCatalogConnection = synonymCatalog.openConnection(configuration);
    }

    @Close
    public void close() {
        if (synonymCatalogConnection != null) {
            synonymCatalogConnection.close();
            synonymCatalogConnection = null;
        }
    }

    @Override
    public Object[] transform(InputRow inputRow) {
        final String originalValue = inputRow.getValue(column);

        if (originalValue == null) {
            return new String[1];
        }

        if (lookUpEveryToken) {
            final SynonymCatalogConnection.Replacement replacement = synonymCatalogConnection.replaceInline(originalValue);
            if (replacedSynonymsType == ReplacedSynonymsType.STRING) {
                return new Object[] { replacement.getReplacedString(), Joiner.on(' ').join(replacement.getSynonyms()),
                        Joiner.on(' ').join(replacement.getMasterTerms()) };
            } else {
                return new Object[] { replacement.getReplacedString(), replacement.getSynonyms(),
                        replacement.getMasterTerms() };
            }
        } else {
            final String replacedValue = lookup(originalValue);
            return new String[] { replacedValue };
        }
    }

    private String lookup(String originalValue) {
        final String replacedValue = synonymCatalogConnection.getMasterTerm(originalValue);
        if (retainOriginalValue && replacedValue == null) {
            return originalValue;
        }
        return replacedValue;
    }
}
