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
package org.bonitasoft.engine.bpm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.bonitasoft.engine.actor.mapping.ActorMappingService;
import org.bonitasoft.engine.bpm.process.ActivationState;
import org.bonitasoft.engine.bpm.process.ConfigurationState;
import org.bonitasoft.engine.builder.BuilderFactory;
import org.bonitasoft.engine.core.process.definition.ProcessDefinitionService;
import org.bonitasoft.engine.core.process.definition.ProcessDefinitionServiceImpl;
import org.bonitasoft.engine.core.process.definition.exception.SProcessDefinitionNotFoundException;
import org.bonitasoft.engine.core.process.definition.model.SProcessDefinition;
import org.bonitasoft.engine.core.process.definition.model.SProcessDefinitionDeployInfo;
import org.bonitasoft.engine.core.process.definition.model.builder.SProcessDefinitionDeployInfoUpdateBuilderFactory;
import org.bonitasoft.engine.data.model.LightEmployee;
import org.bonitasoft.engine.identity.IdentityService;
import org.bonitasoft.engine.identity.UserSearchDescriptor;
import org.bonitasoft.engine.identity.model.SUser;
import org.bonitasoft.engine.persistence.FilterOption;
import org.bonitasoft.engine.persistence.OrderByOption;
import org.bonitasoft.engine.persistence.OrderByType;
import org.bonitasoft.engine.persistence.QueryOptions;
import org.bonitasoft.engine.recorder.model.EntityUpdateDescriptor;
import org.bonitasoft.engine.session.SessionService;
import org.bonitasoft.engine.test.annotation.Cover;
import org.bonitasoft.engine.test.annotation.Cover.BPMNConcept;
import org.junit.Test;

/**
 * @author Celine Souchet
 */
public class ProcessDefinitionServiceIntegrationTest extends CommonBPMServicesTest {

    private final ProcessDefinitionService processDefinitionService;

    private final ActorMappingService actorMappingService;

    public ProcessDefinitionServiceIntegrationTest() {
        processDefinitionService = getTenantAccessor().getProcessDefinitionService();
        actorMappingService = getTenantAccessor().getActorMappingService();
    }

    private SessionService getSessionService() {
        return getTenantAccessor().getSessionService();
    }

    @Test(expected = IllegalArgumentException.class)
    public void storeNullProcessDefinition() throws Exception {
        getTransactionService().begin();
        processDefinitionService.store(null, "", "");
        getTransactionService().complete();
    }

    @Test
    public void addedDisplayName() throws Exception {
        final SProcessDefinition sProcessDefinition = createSProcessDefinition("myProcessName", "1.0");

        getTransactionService().begin();
        final SProcessDefinitionDeployInfo processDefinitionDeployInfo = processDefinitionService.getProcessDeploymentInfo(sProcessDefinition.getId());
        assertEquals("myProcessName", processDefinitionDeployInfo.getName());
        assertEquals("myProcessName", processDefinitionDeployInfo.getDisplayName()); // display name should be the same as name
        assertEquals("1.0", processDefinitionDeployInfo.getVersion());
        assertEquals(ConfigurationState.RESOLVED.name(), processDefinitionDeployInfo.getConfigurationState());
        assertEquals(ActivationState.ENABLED.name(), processDefinitionDeployInfo.getActivationState());
        getTransactionService().complete();

        // clean-up
        deleteSProcessDefinition(sProcessDefinition);
    }

    @Test
    public void updateProcessDefinitionDeployInfo() throws Exception {
        // create process definition
        final SProcessDefinition sProcessDefinition = createSProcessDefinition("myProcessName", "1.0");
        final Long processId = sProcessDefinition.getId();
        final String updatedDisplayName = "updateDisplayName";

        getTransactionService().begin();
        // update processDefinitionDeployInfo
        final EntityUpdateDescriptor updateDescriptor = BuilderFactory.get(SProcessDefinitionDeployInfoUpdateBuilderFactory.class).createNewInstance().updateDisplayName(updatedDisplayName)
                .updateActivationState(ActivationState.ENABLED).done();
        processDefinitionService.updateProcessDefinitionDeployInfo(processId, updateDescriptor);

        // check and do assert
        final SProcessDefinitionDeployInfo processDefinitionDeployInfo = processDefinitionService.getProcessDeploymentInfo(processId);
        assertEquals("myProcessName", processDefinitionDeployInfo.getName());
        assertEquals(updatedDisplayName, processDefinitionDeployInfo.getDisplayName());
        assertNotNull(processDefinitionDeployInfo.getLastUpdateDate());
        assertEquals(ConfigurationState.RESOLVED.name(), processDefinitionDeployInfo.getConfigurationState());
        assertEquals(ActivationState.ENABLED.name(), processDefinitionDeployInfo.getActivationState());
        getTransactionService().complete();

        // clean-up
        deleteSProcessDefinition(sProcessDefinition);
    }

    @Test(expected = SProcessDefinitionNotFoundException.class)
    public void updateProcessDefinitionDeployInfoThrowException() throws Exception {
        final String updatedDisplayName = "updateDisplayName";

        // create process definition
        final SProcessDefinition sProcessDefinition = createSProcessDefinition("myProcessName", "1.0");

        getTransactionService().begin();
        // update processDefinitionDeployInfo with wrong processId

        final EntityUpdateDescriptor updateDescriptor = BuilderFactory.get(SProcessDefinitionDeployInfoUpdateBuilderFactory.class).createNewInstance().updateDisplayName(updatedDisplayName).done();
        try {
            processDefinitionService.updateProcessDefinitionDeployInfo(sProcessDefinition.getId() + 1, updateDescriptor);
        } finally {
            getTransactionService().complete();
            // clean-up
            deleteSProcessDefinition(sProcessDefinition);
        }
    }

    @Test
    public void ifDeployedByIsTheUserId() throws Exception {
        final SProcessDefinition sProcessDefinition = createSProcessDefinition("testIfDeployedByIsTheUserId", "1.0");

        getTransactionService().begin();
        final Long processId = sProcessDefinition.getId();
        final SProcessDefinitionDeployInfo processDefinitionDeployInfo = processDefinitionService.getProcessDeploymentInfo(processId);
        assertEquals(processDefinitionDeployInfo.getDeployedBy(), getSessionService().getSession(getAPISession().getId()).getUserId());
        getTransactionService().complete();

        assertEquals(processDefinitionDeployInfo.getDeployedBy(), getSessionService().getSession(getAPISession().getId()).getUserId());

        getTransactionService().begin();
        actorMappingService.deleteActors(processId);
        processDefinitionService.disableProcessDeploymentInfo(processId);
        processDefinitionService.delete(processId);
        getTransactionService().complete();
    }

    @Cover(classes = { ProcessDefinitionServiceImpl.class }, concept = BPMNConcept.PROCESS, keywords = { "Pagination" }, story = "Implementation of the pagination for getProcessDefinitionIds", jira = "ENGINE-448")
    @Test
    public void getProcessDefIds() throws Exception {
        final List<SProcessDefinition> sProcessDefinitions = createSProcessDefinitions(25, "testGetProcessDefIds", "0.0");

        getTransactionService().begin();
        List<Long> processDefIds = processDefinitionService.getProcessDefinitionIds(0, 10);
        assertEquals(10, processDefIds.size());
        processDefIds = processDefinitionService.getProcessDefinitionIds(0, 20);
        assertEquals(20, processDefIds.size());
        processDefIds = processDefinitionService.getProcessDefinitionIds(0, 25);
        assertEquals(25, processDefIds.size());
        getTransactionService().complete();

        // clean-up
        deleteSProcessDefinitions(sProcessDefinitions);
    }

    @Cover(classes = { IdentityService.class }, concept = BPMNConcept.ACTOR, keywords = { "Get number", "User", "Can start process", "Actor  initiator" }, jira = "ENGINE-815")
    @Test
    public void getNumberOfUsersWhoCanStartProcessWithActorInitiator() throws Exception {
        final SUser sUser1 = createEnabledSUser("firstname1", "lastname1", "pwd1");
        final SUser sUser2 = createEnabledSUser("firstname2", "lastname2", "pwd2");
        final SProcessDefinition sProcessDefinition = createSProcessDefinitionWithSActor("process1", "1.0", "actor1", true, Arrays.asList(sUser1, sUser2));

        getTransactionService().begin();
        final QueryOptions searchOptions = new QueryOptions(0, 5);
        final long result = processDefinitionService.getNumberOfUsersWhoCanStartProcessDeploymentInfo(sProcessDefinition.getId(), searchOptions);
        assertEquals(2, result);
        getTransactionService().complete();

        // clean-up
        deleteSProcessDefinition(sProcessDefinition);
        deleteSUsers(sUser1, sUser2);
    }

    @Cover(classes = { IdentityService.class }, concept = BPMNConcept.ACTOR, keywords = { "Get number", "User", "Can start process", "Actor not initiator" }, jira = "ENGINE-815")
    @Test
    public void getNumberOfUsersWhoCanStartProcessWithActorNotInitiator() throws Exception {
        final SUser sUser1 = createEnabledSUser("firstname1", "lastname1", "pwd1");
        final SUser sUser2 = createEnabledSUser("firstname2", "lastname2", "pwd2");
        final SProcessDefinition sProcessDefinition = createSProcessDefinitionWithSActor("process1", "1.0", "actor1", false, Arrays.asList(sUser1, sUser2));

        getTransactionService().begin();
        final QueryOptions searchOptions = new QueryOptions(0, 5);
        final long result = processDefinitionService.getNumberOfUsersWhoCanStartProcessDeploymentInfo(sProcessDefinition.getId(), searchOptions);
        assertEquals(0, result);
        getTransactionService().complete();

        // clean-up
        deleteSProcessDefinition(sProcessDefinition);
        deleteSUsers(sUser1, sUser2);
    }

    @Cover(classes = { IdentityService.class }, concept = BPMNConcept.ACTOR, keywords = { "Get number", "User", "Can start process", "Actor initiator",
            "Managed by" }, jira = "ENGINE-815")
    @Test
    public void getNumberOfUsersWhoCanStartProcessWithActorInitiatorAndFilterManagedBy() throws Exception {
        final SUser sUser1 = createEnabledSUser("firstname1", "lastname1", "pwd1");
        final SUser sUser2 = createEnabledSUser("firstname2", "lastname2", "pwd2", sUser1.getId());
        final SUser sUser3 = createEnabledSUser("firstname3", "lastname3", "pwd3", sUser2.getId());
        final SUser sUser4 = createEnabledSUser("firstname4", "lastname4", "pwd4");
        final SProcessDefinition sProcessDefinition = createSProcessDefinitionWithSActor("process1", "1.0", "actor1", true,
                Arrays.asList(sUser2, sUser3, sUser4));

        getTransactionService().begin();
        final FilterOption filterManagedBy = new FilterOption(SUser.class, UserSearchDescriptor.MANAGER_USER_ID, sUser1.getId());
        final QueryOptions searchOptions = new QueryOptions(0, 5, null, Arrays.asList(filterManagedBy), null);
        final long result = processDefinitionService.getNumberOfUsersWhoCanStartProcessDeploymentInfo(sProcessDefinition.getId(), searchOptions);
        assertEquals(1, result);
        getTransactionService().complete();

        // clean-up
        deleteSProcessDefinition(sProcessDefinition);
        deleteSUsers(sUser1, sUser2);
    }

    @Cover(classes = { IdentityService.class }, concept = BPMNConcept.ACTOR, keywords = { "Search", "User", "Can start process", "Actor initiator" }, jira = "ENGINE-815")
    @Test
    public void searchUsersWhoCanStartProcessWithActorInitiator() throws Exception {
        final SUser sUser1 = createEnabledSUser("firstname1", "lastname1", "pwd1");
        final SUser sUser2 = createEnabledSUser("firstname2", "lastname2", "pwd2");
        final SProcessDefinition sProcessDefinition = createSProcessDefinitionWithSActor("process1", "1.0", "actor1", true, Arrays.asList(sUser1, sUser2));

        getTransactionService().begin();
        final QueryOptions searchOptions = new QueryOptions(0, 5, SUser.class, UserSearchDescriptor.FIRST_NAME, OrderByType.ASC);
        final List<SUser> result = processDefinitionService.searchUsersWhoCanStartProcessDeploymentInfo(sProcessDefinition.getId(), searchOptions);
        assertEquals(sUser1, result.get(0));
        assertEquals(sUser2, result.get(1));
        getTransactionService().complete();

        // clean-up
        deleteSProcessDefinition(sProcessDefinition);
        deleteSUsers(sUser1, sUser2);
    }

    @Cover(classes = { IdentityService.class }, concept = BPMNConcept.ACTOR, keywords = { "Search", "User", "Can start process", "Actor not initiator" }, jira = "ENGINE-815")
    @Test
    public void searchUsersWhoCanStartProcessWithActorNotInitiator() throws Exception {
        final SUser sUser1 = createEnabledSUser("firstname1", "lastname1", "pwd1");
        final SUser sUser2 = createEnabledSUser("firstname2", "lastname2", "pwd2");
        final SProcessDefinition sProcessDefinition = createSProcessDefinitionWithSActor("process1", "1.0", "actor1", false, Arrays.asList(sUser1, sUser2));

        getTransactionService().begin();
        final QueryOptions searchOptions = new QueryOptions(0, 5, SUser.class, UserSearchDescriptor.FIRST_NAME, OrderByType.ASC);
        final List<SUser> result = processDefinitionService.searchUsersWhoCanStartProcessDeploymentInfo(sProcessDefinition.getId(), searchOptions);
        assertTrue("Users are added to a actor that isn't initiator", result.isEmpty());
        getTransactionService().complete();

        // clean-up
        deleteSProcessDefinition(sProcessDefinition);
        deleteSUsers(sUser1, sUser2);
    }

    @Cover(classes = { IdentityService.class }, concept = BPMNConcept.ACTOR, keywords = { "Search", "User", "Can start process", "Actor initiator",
            "Managed by" }, jira = "ENGINE-815")
    @Test
    public void searchUsersWhoCanStartProcessWithActorInitiatorAndFilterManagedBy() throws Exception {
        final SUser sUser1 = createEnabledSUser("firstname1", "lastname1", "pwd1");
        final SUser sUser2 = createEnabledSUser("firstname2", "lastname2", "pwd2", sUser1.getId());
        final SUser sUser3 = createEnabledSUser("firstname3", "lastname3", "pwd3", sUser2.getId());
        final SUser sUser4 = createEnabledSUser("firstname4", "lastname4", "pwd4");
        final SProcessDefinition sProcessDefinition = createSProcessDefinitionWithSActor("process1", "1.0", "actor1", true,
                Arrays.asList(sUser2, sUser3, sUser4));

        getTransactionService().begin();
        final FilterOption filterManagedBy = new FilterOption(SUser.class, UserSearchDescriptor.MANAGER_USER_ID, sUser1.getId());
        final OrderByOption orderByFirstName = new OrderByOption(SUser.class, UserSearchDescriptor.FIRST_NAME, OrderByType.ASC);
        final QueryOptions searchOptions = new QueryOptions(0, 5, Arrays.asList(orderByFirstName), Arrays.asList(filterManagedBy), null);
        final List<SUser> result = processDefinitionService.searchUsersWhoCanStartProcessDeploymentInfo(sProcessDefinition.getId(), searchOptions);
        getTransactionService().complete();
        assertEquals(sUser2, result.get(0));

        // clean-up
        deleteSProcessDefinition(sProcessDefinition);
        deleteSUsers(sUser1, sUser2);
    }
}
