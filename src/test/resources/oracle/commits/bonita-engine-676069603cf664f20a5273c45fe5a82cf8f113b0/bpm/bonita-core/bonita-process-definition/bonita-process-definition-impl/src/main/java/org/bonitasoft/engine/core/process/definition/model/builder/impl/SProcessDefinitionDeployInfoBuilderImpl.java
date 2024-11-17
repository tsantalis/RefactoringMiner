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

import org.bonitasoft.engine.core.process.definition.model.SProcessDefinitionDeployInfo;
import org.bonitasoft.engine.core.process.definition.model.builder.SProcessDefinitionDeployInfoBuilder;
import org.bonitasoft.engine.core.process.definition.model.impl.SProcessDefinitionDeployInfoImpl;

/**
 * @author Baptiste Mesta
 * @author Yanyan Liu
 * @author Hongwen Zang
 * @author Celine Souchet
 */
public class SProcessDefinitionDeployInfoBuilderImpl implements SProcessDefinitionDeployInfoBuilder {

    private final SProcessDefinitionDeployInfoImpl entity;

    public SProcessDefinitionDeployInfoBuilderImpl(final SProcessDefinitionDeployInfoImpl entity) {
        super();
        this.entity = entity;
    }

    @Override
    public SProcessDefinitionDeployInfoBuilder setProcessId(final long processId) {
        entity.setProcessId(processId);
        return this;
    }

    @Override
    public SProcessDefinitionDeployInfoBuilder setDescription(final String description) {
        entity.setDescription(description);
        return this;
    }

    @Override
    public SProcessDefinitionDeployInfoBuilder setDeploymentDate(final long deploymentDate) {
        entity.setDeploymentDate(deploymentDate);
        return this;
    }

    @Override
    public SProcessDefinitionDeployInfoBuilder setDeployedBy(final long deployedBy) {
        entity.setDeployedBy(deployedBy);
        return this;
    }

    @Override
    public SProcessDefinitionDeployInfoBuilder setConfigurationState(final String state) {
        entity.setConfigurationState(state);
        return this;
    }

    @Override
    public SProcessDefinitionDeployInfoBuilder setActivationState(final String state) {
        entity.setActivationState(state);
        return this;
    }

    @Override
    public SProcessDefinitionDeployInfoBuilder setDisplayName(final String displayName) {
        entity.setDisplayName(displayName);
        return this;
    }

    @Override
    public SProcessDefinitionDeployInfoBuilder setLastUpdateDate(final long lastUpdateDate) {
        entity.setLastUpdateDate(lastUpdateDate);
        return this;
    }

    @Override
    public SProcessDefinitionDeployInfoBuilder setDisplayDescription(final String displayDescription) {
        entity.setDisplayDescription(displayDescription);
        return this;
    }

    @Override
    public SProcessDefinitionDeployInfoBuilder setId(final long id) {
        entity.setId(id);
        return this;
    }

    @Override
    public SProcessDefinitionDeployInfoBuilder setIconPath(final String iconPath) {
        entity.setIconPath(iconPath);
        return this;
    }

    @Override
    public SProcessDefinitionDeployInfo done() {
        return entity;
    }
}
