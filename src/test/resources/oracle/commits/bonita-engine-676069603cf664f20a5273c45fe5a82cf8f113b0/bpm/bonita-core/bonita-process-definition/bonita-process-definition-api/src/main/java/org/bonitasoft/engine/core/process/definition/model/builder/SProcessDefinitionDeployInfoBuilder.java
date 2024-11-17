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
package org.bonitasoft.engine.core.process.definition.model.builder;

import org.bonitasoft.engine.core.process.definition.model.SProcessDefinitionDeployInfo;

/**
 * @author Baptiste Mesta
 * @author Yanyan Liu
 * @author Hongwen Zang
 * @author Celine Souchet
 * @author Matthieu Chaffotte
 */
public interface SProcessDefinitionDeployInfoBuilder {

    SProcessDefinitionDeployInfoBuilder setProcessId(long processId);

    SProcessDefinitionDeployInfoBuilder setDescription(String description);

    SProcessDefinitionDeployInfoBuilder setDeploymentDate(long deploymentDate);

    SProcessDefinitionDeployInfoBuilder setDeployedBy(long deployedBy);

    SProcessDefinitionDeployInfoBuilder setConfigurationState(String configurationState);

    SProcessDefinitionDeployInfoBuilder setActivationState(String activationState);

    SProcessDefinitionDeployInfoBuilder setDisplayName(String displayName);

    SProcessDefinitionDeployInfoBuilder setDisplayDescription(String displayDescription);

    SProcessDefinitionDeployInfoBuilder setLastUpdateDate(long lastUpdateDate);

    SProcessDefinitionDeployInfoBuilder setId(long id);

    SProcessDefinitionDeployInfoBuilder setIconPath(String iconPath);

    SProcessDefinitionDeployInfo done();

}
