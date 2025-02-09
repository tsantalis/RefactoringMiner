/*
 * Copyright 2010, Red Hat, Inc. and individual contributors as indicated by the
 * @author tags. See the copyright.txt file in the distribution for a full
 * listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */
package org.zanata.rest.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.zanata.common.EntityStatus;
import org.zanata.common.Namespaces;
import org.zanata.rest.MediaTypes;
import org.zanata.rest.MediaTypes.Format;

@XmlType(name = "projectIterationType", propOrder = { "links", "status",
        "projectType" })
@XmlRootElement(name = "project-iteration")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(include = Inclusion.NON_NULL)
@JsonPropertyOrder({ "id", "links", "status", "projectType" })
public class ProjectIteration implements Serializable,
        HasCollectionSample<ProjectIteration>, HasMediaType {

    /**
    *
    */
    private static final long serialVersionUID = 1L;
    private String id;
    private Links links;
    private EntityStatus status;
    private String projectType;

    public ProjectIteration() {
    }

    public ProjectIteration(String id) {
        this.id = id;
    }

    @XmlAttribute(name = "id", required = true)
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * Set of links managed by this resource
     *
     * This field is ignored in PUT/POST operations
     *
     * @return set of Links managed by this resource
     */
    @XmlElement(name = "link", required = false,
            namespace = Namespaces.ZANATA_OLD)
    public Links getLinks() {
        return links;
    }

    public void setLinks(Links links) {
        this.links = links;
    }

    public Links getLinks(boolean createIfNull) {
        if (createIfNull && links == null)
            links = new Links();
        return links;
    }

    @XmlElement(name = "status", required = false,
            namespace = Namespaces.ZANATA_OLD)
    public EntityStatus getStatus() {
        return status;
    }

    public void setStatus(EntityStatus status) {
        this.status = status;
    }

    @XmlElement(name = "projectType", required = false,
            namespace = Namespaces.ZANATA_OLD)
    public String getProjectType() {
        return projectType;
    }

    public void setProjectType(String projectType) {
        this.projectType = projectType;
    }

    @Override
    public ProjectIteration createSample() {
        ProjectIteration entity = new ProjectIteration("sample-iteration");
        return entity;
    }

    @Override
    public Collection<ProjectIteration> createSamples() {
        Collection<ProjectIteration> entities =
                new ArrayList<ProjectIteration>();
        entities.add(createSample());
        ProjectIteration entity = new ProjectIteration("another-iteration");
        entities.add(entity);
        return entities;
    }

    @Override
    public String getMediaType(Format format) {
        return MediaTypes.APPLICATION_ZANATA_PROJECT_ITERATION + format;
    }

    @Override
    public String toString() {
        return DTOUtil.toXML(this);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((links == null) ? 0 : links.hashCode());
        result = prime * result + ((status == null) ? 0 : status.hashCode());
        result =
                prime * result
                        + ((projectType == null) ? 0 : projectType.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ProjectIteration)) {
            return false;
        }
        ProjectIteration other = (ProjectIteration) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        if (links == null) {
            if (other.links != null) {
                return false;
            }
        } else if (!links.equals(other.links)) {
            return false;
        }
        if (status != other.status) {
            return false;
        }
        if (projectType == null) {
            if (other.projectType != null) {
                return false;
            }
        } else if (!projectType.equals(other.projectType)) {
            return false;
        }
        return true;
    }

}
