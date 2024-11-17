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
package org.bonitasoft.engine.core.process.definition.model;

import org.bonitasoft.engine.persistence.PersistentObject;

/**
 * @author Baptiste Mesta
 * @author Yanyan Liu
 * @author Hongwen Zang
 * @author Celine Souchet
 */
public interface SProcessDefinitionDeployInfo extends PersistentObject {

    long getProcessId();

    String getName();

    String getVersion();

    String getDescription();

    long getDeploymentDate();

    long getDeployedBy();

    long getMigrationDate();

    long getSupervisorId();

    String getDisplayName();

    /**
     * Get the dates of the last time execute update statement
     * 
     * @return date of the last time execute update statement
     */
    long getLastUpdateDate();

    String getIconPath();

    String getDisplayDescription();

    /**
     * @return the Configuration State
     */
    String getConfigurationState();

    /**
     * @return Activation State
     */
    String getActivationState();
}
