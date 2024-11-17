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
package org.bonitasoft.engine.api.impl.transaction.process;

import org.bonitasoft.engine.commons.exceptions.SBonitaException;
import org.bonitasoft.engine.commons.transaction.TransactionContent;
import org.bonitasoft.engine.core.process.definition.ProcessDefinitionService;
import org.bonitasoft.engine.core.process.definition.model.SProcessDefinition;

/**
 * @author Baptiste Mesta
 */
public final class StoreProcess implements TransactionContent {

    private final ProcessDefinitionService processDefinitionService;

    private final SProcessDefinition sDefinition;

    private final String displayName;

    private final String displayDescription;

    public StoreProcess(final ProcessDefinitionService processDefinitionService, final SProcessDefinition sDefinition, final String displayName,
            final String displayDescription) {
        this.processDefinitionService = processDefinitionService;
        this.sDefinition = sDefinition;
        this.displayName = displayName;
        this.displayDescription = displayDescription;
    }

    @Override
    public void execute() throws SBonitaException {
        processDefinitionService.store(sDefinition, displayName, displayDescription);
    }
}
