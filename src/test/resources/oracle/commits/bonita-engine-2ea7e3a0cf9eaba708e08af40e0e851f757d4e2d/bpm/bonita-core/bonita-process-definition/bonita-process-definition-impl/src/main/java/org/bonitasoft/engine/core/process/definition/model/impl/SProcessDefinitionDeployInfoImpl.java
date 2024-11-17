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

import java.util.Objects;

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

    private String content;

    public SProcessDefinitionDeployInfoImpl() {
        super();
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
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

    public void setDeploymentDate(final long deploymentDate) {
        this.deploymentDate = deploymentDate;
    }

    @Override
    public long getDeployedBy() {
        return deployedBy;
    }

    public void setDeployedBy(final long deployedBy) {
        this.deployedBy = deployedBy;
    }

    @Override
    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    @Override
    public String getDescription() {
        return description;
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

    @Override
    public long getSupervisorId() {
        return supervisorId;
    }

    public void setSupervisorId(final long supervisorId) {
        this.supervisorId = supervisorId;
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

    @Override
    public String getDisplayDescription() {
        return displayDescription;
    }

    public void setDisplayDescription(final String displayDescription) {
        this.displayDescription = displayDescription;
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
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        SProcessDefinitionDeployInfoImpl that = (SProcessDefinitionDeployInfoImpl) o;
        return Objects.equals(deploymentDate, that.deploymentDate) &&
                Objects.equals(deployedBy, that.deployedBy) &&
                Objects.equals(tenantId, that.tenantId) &&
                Objects.equals(processId, that.processId) &&
                Objects.equals(migrationDate, that.migrationDate) &&
                Objects.equals(supervisorId, that.supervisorId) &&
                Objects.equals(lastUpdateDate, that.lastUpdateDate) &&
                Objects.equals(name, that.name) &&
                Objects.equals(id, that.id) &&
                Objects.equals(version, that.version) &&
                Objects.equals(description, that.description) &&
                Objects.equals(configurationState, that.configurationState) &&
                Objects.equals(activationState, that.activationState) &&
                Objects.equals(displayName, that.displayName) &&
                Objects.equals(iconPath, that.iconPath) &&
                Objects.equals(displayDescription, that.displayDescription);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, id, deploymentDate, deployedBy, version, description, configurationState, activationState, tenantId, processId,
                migrationDate, supervisorId, displayName, lastUpdateDate, iconPath, displayDescription);
    }

    @Override
    public String toString() {
        return "SProcessDefinitionDeployInfoImpl{" +
                "name='" + name + '\'' +
                ", id=" + id +
                ", deploymentDate=" + deploymentDate +
                ", deployedBy=" + deployedBy +
                ", version='" + version + '\'' +
                ", description='" + description + '\'' +
                ", configurationState='" + configurationState + '\'' +
                ", activationState='" + activationState + '\'' +
                ", tenantId=" + tenantId +
                ", processId=" + processId +
                ", migrationDate=" + migrationDate +
                ", supervisorId=" + supervisorId +
                ", displayName='" + displayName + '\'' +
                ", lastUpdateDate=" + lastUpdateDate +
                ", iconPath='" + iconPath + '\'' +
                ", displayDescription='" + displayDescription + '\'' +
                '}';
    }
}
