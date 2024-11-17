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
package org.bonitasoft.engine.core.process.definition.model.impl;

import org.bonitasoft.engine.core.process.definition.model.SProcessDefinitionDeployInfo;

/**
 * @author Baptiste Mesta
 * @author Yanyan Liu
 * @author Hongwen Zang
 * @author Celine Souchet
 * @author Matthieu Chaffotte
 */
public class SProcessDefinitionDeployInfoImpl implements SProcessDefinitionDeployInfo {

    private static final long serialVersionUID = -2351527528911983740L;

    private String name;

    private Long id;

    private long deploymentDate;

    private long deployedBy;

    private String version;

    private String description;

    private String configurationState;

    private String activationState;

    private long tenantId;

    private long processId;

    private long migrationDate;

    private long supervisorId;

    private String displayName;

    private long lastUpdateDate;

    private String iconPath;

    private String displayDescription;

    public SProcessDefinitionDeployInfoImpl() {
        super();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public void setId(final long id) {
        this.id = id;
    }

    @Override
    public long getDeploymentDate() {
        return deploymentDate;
    }

    @Override
    public long getDeployedBy() {
        return deployedBy;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setDeploymentDate(final long deploymentDate) {
        this.deploymentDate = deploymentDate;
    }

    public void setDeployedBy(final long deployedBy) {
        this.deployedBy = deployedBy;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    @Override
    public String getDiscriminator() {
        return this.getClass().getName();
    }

    public long getTenantId() {
        return tenantId;
    }

    @Override
    public void setTenantId(final long tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public long getProcessId() {
        return processId;
    }

    public void setProcessId(final long processId) {
        this.processId = processId;
    }

    @Override
    public long getMigrationDate() {
        return migrationDate;
    }

    public void setMigrationDate(final long migrationDate) {
        this.migrationDate = migrationDate;
    }

    public void setSupervisorId(final long supervisorId) {
        this.supervisorId = supervisorId;
    }

    @Override
    public long getSupervisorId() {
        return supervisorId;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    @Override
    public long getLastUpdateDate() {
        return lastUpdateDate;
    }

    public void setLastUpdateDate(final long now) {
        lastUpdateDate = now;
    }

    @Override
    public String getIconPath() {
        return iconPath;
    }

    public void setIconPath(final String iconPath) {
        this.iconPath = iconPath;
    }

    public void setDisplayDescription(final String displayDescription) {
        this.displayDescription = displayDescription;
    }

    @Override
    public String getDisplayDescription() {
        return displayDescription;
    }

    @Override
    public String getConfigurationState() {
        return configurationState;
    }

    public void setConfigurationState(final String configurationState) {
        this.configurationState = configurationState;
    }

    @Override
    public String getActivationState() {
        return activationState;
    }

    public void setActivationState(final String activationState) {
        this.activationState = activationState;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (deployedBy ^ deployedBy >>> 32);
        result = prime * result + (int) (deploymentDate ^ deploymentDate >>> 32);
        result = prime * result + (description == null ? 0 : description.hashCode());
        result = prime * result + (displayDescription == null ? 0 : displayDescription.hashCode());
        result = prime * result + (displayName == null ? 0 : displayName.hashCode());
        result = prime * result + (iconPath == null ? 0 : iconPath.hashCode());
        result = prime * result + (id == null ? 0 : id.hashCode());
        result = prime * result + (int) (lastUpdateDate ^ lastUpdateDate >>> 32);
        result = prime * result + (int) (migrationDate ^ migrationDate >>> 32);
        result = prime * result + (name == null ? 0 : name.hashCode());
        result = prime * result + (int) (processId ^ processId >>> 32);
        result = prime * result + (activationState == null ? 0 : activationState.hashCode());
        result = prime * result + (configurationState == null ? 0 : configurationState.hashCode());
        result = prime * result + (int) (supervisorId ^ supervisorId >>> 32);
        result = prime * result + (int) (tenantId ^ tenantId >>> 32);
        result = prime * result + (version == null ? 0 : version.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SProcessDefinitionDeployInfoImpl other = (SProcessDefinitionDeployInfoImpl) obj;
        if (deployedBy != other.processId) {
            return false;
        }
        if (deploymentDate != other.deploymentDate) {
            return false;
        }
        if (description == null) {
            if (other.description != null) {
                return false;
            }
        } else if (!description.equals(other.description)) {
            return false;
        }
        if (displayDescription == null) {
            if (other.displayDescription != null) {
                return false;
            }
        } else if (!displayDescription.equals(other.displayDescription)) {
            return false;
        }
        if (displayName == null) {
            if (other.displayName != null) {
                return false;
            }
        } else if (!displayName.equals(other.displayName)) {
            return false;
        }
        if (iconPath == null) {
            if (other.iconPath != null) {
                return false;
            }
        } else if (!iconPath.equals(other.iconPath)) {
            return false;
        }
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        if (lastUpdateDate != other.lastUpdateDate) {
            return false;
        }
        if (migrationDate != other.migrationDate) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (processId != other.processId) {
            return false;
        }
        if (activationState == null) {
            if (other.activationState != null) {
                return false;
            }
        } else if (!activationState.equals(other.activationState)) {
            return false;
        }
        if (configurationState == null) {
            if (other.configurationState != null) {
                return false;
            }
        } else if (!configurationState.equals(other.configurationState)) {
            return false;
        }
        if (supervisorId != other.supervisorId) {
            return false;
        }
        if (tenantId != other.tenantId) {
            return false;
        }
        if (version == null) {
            if (other.version != null) {
                return false;
            }
        } else if (!version.equals(other.version)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "SProcessDefinitionDeployInfoImpl [name=" + name + ", id=" + id + ", deploymentDate=" + deploymentDate + ", deployedBy=" + deployedBy
                + ", version=" + version + ", description=" + description + ", activationState =" + activationState + ", configurationState ="
                + configurationState + ", tenantId=" + tenantId + ", processId=" + processId + ", migrationDate=" + migrationDate + ", supervisorId="
                + supervisorId + ", displayName=" + displayName + ", lastUpdateDate=" + lastUpdateDate + ", iconPath=" + iconPath + ", displayDescription="
                + displayDescription + "]";
    }

}
