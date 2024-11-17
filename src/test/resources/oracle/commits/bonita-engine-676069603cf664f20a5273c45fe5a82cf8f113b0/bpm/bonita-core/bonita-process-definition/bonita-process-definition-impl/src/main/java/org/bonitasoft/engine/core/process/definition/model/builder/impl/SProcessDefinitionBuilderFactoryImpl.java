/**
 * Copyright (C) 2015 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation
 * version 2.1 of the License.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA 02110-1301, USA.
 **/
package org.bonitasoft.engine.core.process.definition.model.builder.impl;

import java.io.InputStream;

import org.bonitasoft.engine.bpm.process.DesignProcessDefinition;
import org.bonitasoft.engine.core.process.definition.model.SProcessDefinition;
import org.bonitasoft.engine.core.process.definition.model.bindings.XMLSProcessDefinition;
import org.bonitasoft.engine.core.process.definition.model.builder.SProcessDefinitionBuilder;
import org.bonitasoft.engine.core.process.definition.model.builder.SProcessDefinitionBuilderFactory;
import org.bonitasoft.engine.core.process.definition.model.impl.SProcessDefinitionImpl;
import org.bonitasoft.engine.xml.ElementBindingsFactory;
import org.bonitasoft.engine.xml.XMLNode;

/**
 * @author Baptiste Mesta
 */
public class SProcessDefinitionBuilderFactoryImpl implements SProcessDefinitionBuilderFactory {

    @Override
    public SProcessDefinitionBuilder createNewInstance(final DesignProcessDefinition processDefinition) {
        final SProcessDefinitionImpl entity = new SProcessDefinitionImpl(processDefinition);
        return new SProcessDefinitionBuilderImpl(entity);
    }

    @Override
    public InputStream getModelSchema() {
        return this.getClass().getResourceAsStream("SProcessDefinition.xsd");
    }

    @Override
    public XMLNode getXMLProcessDefinition(final SProcessDefinition definition) {
        return new XMLSProcessDefinition().getXMLProcessDefinition(definition);
    }

    @Override
    public ElementBindingsFactory getElementsBindings() {
        return new SProcessElementBindingsFactory();
    }

}
