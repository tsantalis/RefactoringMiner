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
package org.bonitasoft.engine.core.process.definition;

import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.bpm.process.ActivationState;
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfoCriterion;
import org.bonitasoft.engine.core.process.definition.exception.SDeletingEnabledProcessException;
import org.bonitasoft.engine.core.process.definition.exception.SProcessDefinitionException;
import org.bonitasoft.engine.core.process.definition.exception.SProcessDefinitionNotFoundException;
import org.bonitasoft.engine.core.process.definition.exception.SProcessDefinitionReadException;
import org.bonitasoft.engine.core.process.definition.exception.SProcessDeletionException;
import org.bonitasoft.engine.core.process.definition.exception.SProcessDeploymentInfoUpdateException;
import org.bonitasoft.engine.core.process.definition.exception.SProcessDisablementException;
import org.bonitasoft.engine.core.process.definition.exception.SProcessEnablementException;
import org.bonitasoft.engine.core.process.definition.model.SFlowNodeDefinition;
import org.bonitasoft.engine.core.process.definition.model.SProcessDefinition;
import org.bonitasoft.engine.core.process.definition.model.SProcessDefinitionDeployInfo;
import org.bonitasoft.engine.identity.model.SUser;
import org.bonitasoft.engine.persistence.OrderByType;
import org.bonitasoft.engine.persistence.QueryOptions;
import org.bonitasoft.engine.persistence.SBonitaReadException;
import org.bonitasoft.engine.recorder.model.EntityUpdateDescriptor;

/**
 * @author Matthieu Chaffotte
 * @author Yanyan Liu
 * @author Celine Souchet
 * @author Arthur Freycon
 * @since 6.0
 */
public interface ProcessDefinitionService {

    String PROCESSDEFINITION = "PROCESSDEFINITION";

    String PROCESSDEFINITION_IS_ENABLED = "PROCESSDEFINITION_IS_ENABLED";

    String PROCESSDEFINITION_IS_DISABLED = "PROCESSDEFINITION_IS_DISABLED";

    String PROCESSDEFINITION_DEPLOY_INFO = "PROCESSDEFINITION_DEPLOY_INFO";

    String PROCESSDEFINITION_IS_RESOLVED = "PROCESSDEFINITION_IS_RESOLVED";

    String PROCESSDEFINITION_IS_UNRESOLVED = "PROCESSDEFINITION_IS_UNRESOLVED";

    String PROCESS_CACHE_NAME = "_PROCESSDEF";

    String UNCATEGORIZED_SUFFIX = "Uncategorized";

    String UNCATEGORIZED_SUPERVISED_BY_SUFFIX = "UncategorizedAndWithSupervisor";

    String UNCATEGORIZED_USERCANSTART_SUFFIX = "UncategorizedUserCanStart";

    String WHOCANSTART_PROCESS_SUFFIX = "WhoCanStartProcess";

    String STARTED_BY_SUFFIX = "StartedBy";

    String PROCESS_DEFINITION_ID = "processId";

    String USER_ID = "userId";

    String ROLE_ID = "roleId";

    String GROUP_ID = "groupId";

    /**
     * Store the processDefinition to file system and its deploy info to DB.
     * 
     * @param definition
     *            the processDefinition will be stored
     * @param displayName
     *            display name of the process
     * @param displayDescription
     *            display description of the process
     * @return the definition will an id
     * @throws SProcessDefinitionException
     */
    SProcessDefinition store(SProcessDefinition definition, String displayName, String displayDescription) throws SProcessDefinitionException;

    /**
     * Get processDefinition by its id
     * 
     * @param processDefinitionId
     *            identifier of processDefinition
     * @return the processDefinition corresponding to the parameter processId
     * @throws SProcessDefinitionNotFoundException
     *             error thrown if no process definition found
     * @throws SProcessDefinitionReadException
     */
    SProcessDefinition getProcessDefinition(long processDefinitionId) throws SProcessDefinitionNotFoundException, SProcessDefinitionReadException;

    /**
     * Get processDefinition by its id, if it's enabled.
     *
     * @param processDefinitionId
     *        The identifier of processDefinition
     * @return The processDefinition corresponding to the parameter processId
     * @throws SProcessDefinitionReadException
     * @throws SProcessDefinitionException
     * @since 6.4.0
     */
    SProcessDefinition getProcessDefinitionIfIsEnabled(long processDefinitionId) throws SProcessDefinitionReadException, SProcessDefinitionException;

    /**
     * Get deployment info of the process definition having the id given in parameter
     * 
     * @param processId
     *            id of the process definition on which we want deployment information
     * @return an SProcessDefinitionDeployInfo object to the process definition
     * @throws SProcessDefinitionNotFoundException
     *             error thrown if no process definition found
     * @throws SProcessDefinitionReadException
     */
    SProcessDefinitionDeployInfo getProcessDeploymentInfo(long processId) throws SProcessDefinitionNotFoundException, SProcessDefinitionReadException;

    /**
     * Delete the id specified process definition and its deploy info
     * 
     * @param processId
     *            identifier of processDefinition
     * @throws SProcessDefinitionNotFoundException
     *             error thrown if no process definition found
     * @throws SProcessDeletionException
     * @throws SDeletingEnabledProcessException
     *             error throw if the process still enabled
     */
    void delete(long processId) throws SProcessDefinitionNotFoundException, SProcessDeletionException, SDeletingEnabledProcessException;

    /**
     * Get process definition deploy info in a specific interval with order, this can be used for pagination
     * 
     * @param fromIndex
     *            Index of the record to be retrieved from. First record has index 0
     * @param numberPerPage
     *            Number of result we want to get. Maximum number of result returned
     * @param field
     *            the field user to do order
     * @param order
     *            ASC or DESC
     * @return a list of SProcessDefinitionDeployInfo object
     * @throws SProcessDefinitionReadException
     */
    List<SProcessDefinitionDeployInfo> getProcessDeploymentInfos(int fromIndex, int numberPerPage, String field, OrderByType order)
            throws SProcessDefinitionReadException;

    /**
     * Enable the specific process definition, set the process as ENABLED when it's in RESOLVED state
     * 
     * @param processId
     *            identifier of processDefinition
     * @throws SProcessDefinitionNotFoundException
     *             error thrown if no process definition found for the given processId
     * @throws SProcessEnablementException
     */
    void enableProcessDeploymentInfo(long processId) throws SProcessDefinitionNotFoundException, SProcessEnablementException;

    /**
     * set the process as RESOLVED when it's in ENABLED state
     * 
     * @param processId
     *            identifier of process definition
     * @throws SProcessDefinitionNotFoundException
     *             error thrown if no process definition found for the given processId
     * @throws SProcessDisablementException
     */
    void disableProcessDeploymentInfo(long processId) throws SProcessDefinitionNotFoundException, SProcessDisablementException;

    /**
     * set the process as RESOLVED when it's in UNRESOLVED state
     * 
     * @param processId
     *            identifier of process definition
     * @throws SProcessDefinitionNotFoundException
     *             error thrown if no process definition found for the given processId
     * @throws SProcessDisablementException
     */
    void resolveProcess(long processId) throws SProcessDefinitionNotFoundException, SProcessDisablementException;

    /**
     * Gets how many processes are in the given state.
     * 
     * @param activationState
     *            the activation state
     * @return number of processes are in the given state or 0;
     * @throws SProcessDefinitionReadException
     */
    long getNumberOfProcessDeploymentInfosByActivationState(ActivationState activationState) throws SProcessDefinitionReadException;

    /**
     * Gets how many processes are defined.
     * 
     * @return the number of process definitions;
     * @throws SProcessDefinitionReadException
     *             occurs when an exception is thrown during method execution
     */
    long getNumberOfProcessDeploymentInfos() throws SProcessDefinitionReadException;

    /**
     * Get the process definition identifiers in the given state.
     * 
     * @param activationState
     *            the activation state
     * @param fromIndex
     *            Index of the record to be retrieved from. First record has index 0
     * @param numberOfResult
     *            Number of result we want to get. Maximum number of result returned
     * @return the paginated list of process definition identifiers or an empty list
     * @throws SProcessDefinitionReadException
     */
    List<Long> getProcessDefinitionIds(ActivationState activationState, int fromIndex, int numberOfResult) throws SProcessDefinitionReadException;

    /**
     * Get the process definition identifiers.
     * * @param fromIndex
     * Index of the record to be retrieved from. First record has index 0
     * 
     * @param numberOfResult
     *            Number of result we want to get. Maximum number of result returned
     * @return the paginated list of process definition identifiers or an empty list
     * @throws SProcessDefinitionReadException
     */
    List<Long> getProcessDefinitionIds(int fromIndex, int numberOfResult) throws SProcessDefinitionReadException;

    /**
     * Get target flow node for the given source flow node in the specific process
     * 
     * @param definition
     *            the process definition containing source flow node
     * @param source
     *            a flow node in process definition
     * @return target flow node of the given source
     */
    SFlowNodeDefinition getNextFlowNode(SProcessDefinition definition, String source);

    /**
     * get sub set of processDefinitionDeployInfos in specific order
     * 
     * @param processIds
     *            identifiers of process definition
     * @param fromIndex
     *            Index of the record to be retrieved from. First record has index 0
     * @param numberOfProcesses
     *            Number of result we want to get. Maximum number of result returned
     * @param field
     *            filed user to do order
     * @param order
     *            ASC or DESC
     * @return a list of SProcessDefinitionDeployInfo objects
     * @throws SProcessDefinitionNotFoundException
     * @throws SProcessDefinitionReadException
     */
    List<SProcessDefinitionDeployInfo> getProcessDeploymentInfos(List<Long> processIds, int fromIndex, int numberOfProcesses, String field, OrderByType order)
            throws SProcessDefinitionNotFoundException, SProcessDefinitionReadException;

    List<SProcessDefinitionDeployInfo> getProcessDeploymentInfos(List<Long> processIds) throws SProcessDefinitionNotFoundException,
            SProcessDefinitionReadException;

    /**
     * Get the processDefinitionId of the most recent version of the process
     * 
     * @param processName
     *            name of process definition
     * @return the latest process definition
     * @throws SProcessDefinitionReadException
     */
    long getLatestProcessDefinitionId(String processName) throws SProcessDefinitionReadException, SProcessDefinitionNotFoundException;

    /**
     * Get the processDefinitionId by name and version
     * 
     * @param name
     *            name of process definition
     * @param version
     *            version or process definition
     * @return identifier of process definition
     * @throws SProcessDefinitionReadException
     * @throws SProcessDefinitionNotFoundException
     */
    long getProcessDefinitionId(String name, String version) throws SProcessDefinitionReadException, SProcessDefinitionNotFoundException;

    /**
     * Update deployment info of the process definition having the id given in parameter
     * 
     * @param processId
     *            identifier of process deploy info
     * @param descriptor
     *            update description
     * @throws SProcessDefinitionNotFoundException
     *             error thrown when no process deploy info found with the give processId
     * @throws SProcessDeploymentInfoUpdateException
     */
    SProcessDefinitionDeployInfo updateProcessDefinitionDeployInfo(long processId, EntityUpdateDescriptor descriptor)
            throws SProcessDefinitionNotFoundException,
            SProcessDeploymentInfoUpdateException;

    /**
     * Search all process deploy info started by the specific user
     * 
     * @param startedBy
     *            the name of user who started the process
     * @param searchOptions
     *            a QueryOptions object containing some query conditions
     * @return a list of SProcessDefinitionDeployInfo objects
     * @throws SBonitaReadException
     */
    List<SProcessDefinitionDeployInfo> searchProcessDeploymentInfosStartedBy(long startedBy, QueryOptions searchOptions) throws SBonitaReadException;

    /**
     * Get number of all process deploy info started by the specific user
     * 
     * @param startedBy
     *            the name of user who started the process
     * @param countOptions
     *            a QueryOptions object containing some query conditions
     * @return number of all process deploy info to the criteria
     * @throws SBonitaReadException
     */
    long getNumberOfProcessDeploymentInfosStartedBy(long startedBy, QueryOptions countOptions) throws SBonitaReadException;

    /**
     * Search all process definition deploy infos according to the specific search criteria
     * 
     * @param searchOptions
     *            a QueryOptions object containing search criteria
     * @return a list of SProcessDefinitionDeployInfo object
     * @throws SBonitaReadException
     */
    List<SProcessDefinitionDeployInfo> searchProcessDeploymentInfos(QueryOptions searchOptions) throws SBonitaReadException;

    /**
     * Get number of all process definition deploy infos according to the specific search criteria
     * 
     * @param countOptions
     *            a QueryOptions object containing query criteria
     * @return number of all process definition deploy infos corresponding to the criteria
     * @throws SBonitaReadException
     */
    long getNumberOfProcessDeploymentInfos(QueryOptions countOptions) throws SBonitaReadException;

    /**
     * Get total number of uncategorized process definitions by given query criteria
     * 
     * @param countOptions
     *            a QueryOptions object containing query criteria
     * @return total number of uncategorized process definitions suit to query criteria
     * @throws SBonitaReadException
     */
    long getNumberOfUncategorizedProcessDeploymentInfos(QueryOptions countOptions) throws SBonitaReadException;

    /**
     * Get total number of uncategorized process definitions by given query criteria for specific supervisor
     * 
     * @param userId
     *            identifier of a supervisor user
     * @param countOptions
     *            a QueryOptions object containing query criteria
     * @return number of uncategorized process definitions managed by the specific supervisor
     * @throws SBonitaReadException
     */
    long getNumberOfUncategorizedProcessDeploymentInfosSupervisedBy(long userId, QueryOptions countOptions) throws SBonitaReadException;

    /**
     * Search all uncategorized process definitions according to the search criteria.
     * 
     * @param searchOptions
     *            a QueryOptions object containing query criteria
     * @return a list of SProcessDefinitionDeployInfo objects
     * @throws SBonitaReadException
     */
    List<SProcessDefinitionDeployInfo> searchUncategorizedProcessDeploymentInfos(QueryOptions searchOptions) throws SBonitaReadException;

    /**
     * Search all process definitions for a specific category.
     * 
     * @param categoryId
     *            Identifier of the category
     * @param queryOptions
     *            a QueryOptions object containing query criteria
     * @return a list of SProcessDefinitionDeployInfo objects
     * @throws SBonitaReadException
     */
    List<SProcessDefinitionDeployInfo> searchProcessDeploymentInfosOfCategory(long categoryId, QueryOptions queryOptions) throws SBonitaReadException;

    /**
     * Search all uncategorized process definitions by given query criteria for specific supervisor
     * 
     * @param userId
     *            identifier of a supervisor user
     * @param searchOptions
     *            a QueryOptions object containing query criteria
     * @return a list of SProcessDefinitionDeployInfo object
     * @throws SBonitaReadException
     */
    List<SProcessDefinitionDeployInfo> searchUncategorizedProcessDeploymentInfosSupervisedBy(long userId, QueryOptions searchOptions)
            throws SBonitaReadException;

    /**
     * Search all process definitions for the specific user who can start
     * 
     * @param userId
     *            identifier of user
     * @param searchOptions
     *            a QueryOptions object containing query criteria
     * @return a list of SProcessDefinitionDeployInfo objects
     * @throws SBonitaReadException
     */
    List<SProcessDefinitionDeployInfo> searchProcessDeploymentInfosCanBeStartedBy(long userId, QueryOptions searchOptions) throws SBonitaReadException;

    /**
     * Get number of all process definitions for the specific user who can start
     * 
     * @param userId
     *            identifier of user
     * @param countOptions
     *            a QueryOptions object containing query criteria
     * @return number of all process definitions for the specific user who can start
     * @throws SBonitaReadException
     */
    long getNumberOfProcessDeploymentInfosCanBeStartedBy(long userId, QueryOptions countOptions) throws SBonitaReadException;

    /**
     * Search all process definitions for the users managed by specific manager, or manager who can start
     * 
     * @param managerUserId
     *            identifier of manager
     * @param searchOptions
     *            a QueryOptions object containing query criteria
     * @return a list of SProcessDefinitionDeployInfo objects
     * @throws SBonitaReadException
     */
    List<SProcessDefinitionDeployInfo> searchProcessDeploymentInfosCanBeStartedByUsersManagedBy(long managerUserId, QueryOptions searchOptions)
            throws SBonitaReadException;

    /**
     * Get number of all process definitions for the users managed by specific manager, or manager who can start
     * 
     * @param managerUserId
     *            identifier of manager
     * @param countOptions
     *            a QueryOptions object containing query criteria
     * @return Number of all process definitions for the users managed by specific manager, or manager who can start
     * @throws SBonitaReadException
     */
    long getNumberOfProcessDeploymentInfosCanBeStartedByUsersManagedBy(long managerUserId, QueryOptions countOptions) throws SBonitaReadException;

    /**
     * Search all process definitions for the specific user who can perform the "querySuffix" specified action
     * 
     * @param userId
     *            identifier of user
     * @param searchOptions
     *            a QueryOptions object containing query criteria
     * @param querySuffix
     *            query suffix to specify the thing the user can do, it can be "UserSupervised" or "UserCanStart"
     * @return a list of SProcessDefinitionDeployInfo object
     * @throws SBonitaReadException
     */
    List<SProcessDefinitionDeployInfo> searchProcessDeploymentInfos(long userId, QueryOptions searchOptions, String querySuffix) throws SBonitaReadException;

    /**
     * Get total number of process definitions for the specific user who can perform the "querySuffix" specified action
     * 
     * @param userId
     *            identifier of user
     * @param countOptions
     *            a QueryOptions object containing query criteria
     * @param querySuffix
     *            query suffix to specify the thing the user can do, it can be "UserSupervised" or "UserCanStart"
     * @return number of process definitions for the specific user with specific action
     * @throws SBonitaReadException
     */
    long getNumberOfProcessDeploymentInfos(long userId, QueryOptions countOptions, String querySuffix) throws SBonitaReadException;

    /**
     * Search all uncategorized process definitions for the specific user who can start
     * 
     * @param userId
     *            identifier of user
     * @param searchOptions
     *            a QueryOptions object containing query criteria
     * @return a list of SProcessDefinitionDeployInfo object
     * @throws SBonitaReadException
     */
    List<SProcessDefinitionDeployInfo> searchUncategorizedProcessDeploymentInfosCanBeStartedBy(long userId, QueryOptions searchOptions)
            throws SBonitaReadException;

    /**
     * Get total number of uncategorized process definitions for the specific user who can start
     * 
     * @param userId
     *            identifier of user
     * @param countOptions
     *            a QueryOptions object containing query criteria
     * @return number of uncategorized process definitions for the specific user who can start
     * @throws SBonitaReadException
     */
    long getNumberOfUncategorizedProcessDeploymentInfosCanBeStartedBy(long userId, QueryOptions countOptions) throws SBonitaReadException;

    /**
     * A list of SProcessDefinitionDeployInfos for the specific processInstances
     * 
     * @param processInstanceIds
     *            identifier of process instances
     * @return a map containing identifiers of process instance and the corresponding SProcessDefinitionDeployInfo object
     * @throws SBonitaReadException
     */
    Map<Long, SProcessDefinitionDeployInfo> getProcessDeploymentInfosFromProcessInstanceIds(List<Long> processInstanceIds) throws SBonitaReadException;

    /**
     * Get A list of SProcessDefinitionDeployInfos for the specific archived processInstances
     * 
     * @param archivedProcessInstantsIds
     *            identifiers of archived processInstance
     * @return a map containing identifiers of archived process instance and the corresponding SProcessDefinitionDeployInfo object
     * @throws SProcessDefinitionReadException
     */
    Map<Long, SProcessDefinitionDeployInfo> getProcessDeploymentInfosFromArchivedProcessInstanceIds(List<Long> archivedProcessInstantsIds)
            throws SProcessDefinitionReadException;

    /**
     * Get A list of SProcessDefinitionDeployInfos unrelated to the specific category
     * 
     * @param categoryId
     * @param pagingCriterion
     * @param numberPerPage
     * @param pageIndex
     * @return A list of SProcessDefinitionDeployInfos unrelated to the specific category
     * @throws SProcessDefinitionReadException
     */
    List<SProcessDefinitionDeployInfo> getProcessDeploymentInfosUnrelatedToCategory(long categoryId, int pageIndex, int numberPerPage,
            ProcessDeploymentInfoCriterion pagingCriterion) throws SProcessDefinitionReadException;

    /**
     * Get number of SProcessDefinitionDeployInfos unrelated to the specific category
     * 
     * @param categoryId
     * @return Number of SProcessDefinitionDeployInfos unrelated to the specific category
     * @throws SProcessDefinitionReadException
     */
    Long getNumberOfProcessDeploymentInfosUnrelatedToCategory(long categoryId) throws SProcessDefinitionReadException;

    /**
     * Get process definition deploy info in a specific interval with order, this can be used for pagination
     * 
     * @param queryOptions
     *            object containing query criteria
     * @return a list of SProcessDefinitionDeployInfo corresponding to the criteria
     * @throws SProcessDefinitionReadException
     */
    List<SProcessDefinitionDeployInfo> getProcessDeploymentInfos(QueryOptions queryOptions) throws SProcessDefinitionReadException;

    /**
     * List all processes that contain at least one task which actor is mapped only to the specified group.
     * 
     * @param groupId
     *            the Id of the group from which to retrieve the processes with tasks only it can do.
     * @param queryOptions
     *            object containing query criteria
     * @return the list of matching processes, as a List of <code>SProcessDefinitionDeployInfo</code>
     * @throws SProcessDefinitionReadException
     *             in case a read problem occurs
     */
    List<SProcessDefinitionDeployInfo> getProcessDeploymentInfosWithActorOnlyForGroup(long groupId, QueryOptions queryOptions)
            throws SProcessDefinitionReadException;

    /**
     * List all processes that contain at least one task which actor is mapped only to the specified groups.
     * 
     * @param groupIds
     *            the Ids of the groups from which to retrieve the processes with tasks only they can do.
     * @param queryOptions
     *            object containing query criteria
     * @return the list of matching processes, as a List of <code>SProcessDefinitionDeployInfo</code>
     * @throws SProcessDefinitionReadException
     *             in case a read problem occurs
     */
    List<SProcessDefinitionDeployInfo> getProcessDeploymentInfosWithActorOnlyForGroups(List<Long> groupIds, QueryOptions queryOptions)
            throws SProcessDefinitionReadException;

    /**
     * List all processes that contain at least one task which actor is mapped only to the specified role.
     * 
     * @param roleId
     *            the Id of the role from which to retrieve the processes with tasks only it can do.
     * @param queryOptions
     *            object containing query criteria
     * @return the list of matching processes, as a List of <code>SProcessDefinitionDeployInfo</code>
     * @throws SProcessDefinitionReadException
     *             in case a read problem occurs
     */
    List<SProcessDefinitionDeployInfo> getProcessDeploymentInfosWithActorOnlyForRole(long roleId, QueryOptions queryOptions)
            throws SProcessDefinitionReadException;

    /**
     * List all processes that contain at least one task which actor is mapped only to the specified roles.
     * 
     * @param roleIds
     *            the Ids of the roles from which to retrieve the processes with tasks only they can do.
     * @param queryOptions
     *            object containing query criteria
     * @return the list of matching processes, as a List of <code>SProcessDefinitionDeployInfo</code>
     * @throws SProcessDefinitionReadException
     *             in case a read problem occurs
     */
    List<SProcessDefinitionDeployInfo> getProcessDeploymentInfosWithActorOnlyForRoles(List<Long> roleIds, QueryOptions queryOptions)
            throws SProcessDefinitionReadException;

    /**
     * List all processes that contain at least one task which actor is mapped only to the specified user.
     * 
     * @param userId
     *            the Id of the user from which to retrieve the processes with tasks only he / she can do.
     * @param queryOptions
     *            object containing query criteria
     * @return the list of matching processes, as a List of <code>SProcessDefinitionDeployInfo</code>
     * @throws SProcessDefinitionReadException
     *             in case a read problem occurs
     */
    List<SProcessDefinitionDeployInfo> getProcessDeploymentInfosWithActorOnlyForUser(long userId, QueryOptions queryOptions)
            throws SProcessDefinitionReadException;

    /**
     * List all processes that contain at least one task which actor is mapped only to the specified users.
     * 
     * @param userIds
     *            the Ids of the users from which to retrieve the processes with tasks only they can do.
     * @param queryOptions
     *            object containing query criteria
     * @return the list of matching processes, as a List of <code>SProcessDefinitionDeployInfo</code>
     * @throws SProcessDefinitionReadException
     *             in case a read problem occurs
     */
    List<SProcessDefinitionDeployInfo> getProcessDeploymentInfosWithActorOnlyForUsers(List<Long> userIds, QueryOptions queryOptions)
            throws SProcessDefinitionReadException;

    /**
     * Get total number of users according to specific query options, and who can start the given process definition
     * 
     * @param processDefinitionId
     *            Identifier of the process definition
     * @param queryOptions
     *            The QueryOptions object containing some query conditions
     * @return
     * @throws SBonitaReadException
     */
    long getNumberOfUsersWhoCanStartProcessDeploymentInfo(long processDefinitionId, QueryOptions queryOptions) throws SBonitaReadException;

    /**
     * Search users according to specific query options, and who can start the given process definition
     * 
     * @param processDefinitionId
     *            Identifier of the process definition
     * @param searchOptions
     *            The QueryOptions object containing some query conditions
     * @return
     * @throws SBonitaReadException
     */
    List<SUser> searchUsersWhoCanStartProcessDeploymentInfo(long processDefinitionId, QueryOptions queryOptions) throws SBonitaReadException;

    /**
     * Get the total number of the process definitions that have one or more human tasks assigned/pending for a specific user.
     * The tasks are in stable state, not in terminal/executing state.
     * 
     * @param userId
     *            The identifier of the user.
     * @param queryOptions
     *            The QueryOptions object containing some query conditions
     * @return The number of the process definition
     * @throws SBonitaReadException
     */
    long getNumberOfProcessDeploymentInfosWithAssignedOrPendingHumanTasksFor(long userId, QueryOptions queryOptions) throws SBonitaReadException;

    /**
     * Search all process definitions that have one or more human tasks assigned/pending for a specific user.
     * The tasks are in stable state, not in terminal/executing state.
     * 
     * @param userId
     *            The identifier of the user.
     * @param queryOptions
     *            The QueryOptions object containing some query conditions
     * @return The list of process definitions
     * @throws SBonitaReadException
     */
    List<SProcessDefinitionDeployInfo> searchProcessDeploymentInfosWithAssignedOrPendingHumanTasksFor(long userId, QueryOptions queryOptions)
            throws SBonitaReadException;

    /**
     * Get the total number of the process definitions supervised by a specific user, that have instances with one or more human tasks assigned/pending.
     * The tasks are in stable state, not in terminal/executing state.
     * 
     * @param userId
     *            The identifier of the user.
     * @param queryOptions
     *            The QueryOptions object containing some query conditions
     * @return The number of the process definition
     * @throws SBonitaReadException
     *             if an exception occurs when getting the process deployment information.
     * @since 6.3.3
     */
    long getNumberOfProcessDeploymentInfosWithAssignedOrPendingHumanTasksSupervisedBy(long userId, QueryOptions queryOptions) throws SBonitaReadException;

    /**
     * Search all process definitions supervised by a specific user, that have instances with one or more human tasks assigned/pending.
     * The tasks are in stable state, not in terminal/executing state.
     * 
     * @param userId
     *            The identifier of the user.
     * @param queryOptions
     *            The QueryOptions object containing some query conditions
     * @return The list of process definitions
     * @throws SBonitaReadException
     *             if an exception occurs when getting the process deployment information.
     * @since 6.3.3
     */
    List<SProcessDefinitionDeployInfo> searchProcessDeploymentInfosWithAssignedOrPendingHumanTasksSupervisedBy(long userId, QueryOptions queryOptions)
            throws SBonitaReadException;

    /**
     * Get the total number of the process definitions that have instances with one or more human tasks assigned/pending.
     * The tasks are in stable state, not in terminal/executing state.
     * 
     * @param queryOptions
     *            The QueryOptions object containing some query conditions
     * @return The number of the process definition
     * @throws SBonitaReadException
     *             if an exception occurs when getting the process deployment information.
     * @since 6.3.3
     */
    long getNumberOfProcessDeploymentInfosWithAssignedOrPendingHumanTasks(QueryOptions queryOptions) throws SBonitaReadException;

    /**
     * Search all process definitions that have instances with one or more human tasks assigned/pending.
     * The tasks are in stable state, not in terminal/executing state.
     * 
     * @param queryOptions
     *            The QueryOptions object containing some query conditions
     * @return The list of process definitions
     * @throws SBonitaReadException
     *             if an exception occurs when getting the process deployment information.
     * @since 6.3.3
     */
    List<SProcessDefinitionDeployInfo> searchProcessDeploymentInfosWithAssignedOrPendingHumanTasks(QueryOptions queryOptions) throws SBonitaReadException;

}
