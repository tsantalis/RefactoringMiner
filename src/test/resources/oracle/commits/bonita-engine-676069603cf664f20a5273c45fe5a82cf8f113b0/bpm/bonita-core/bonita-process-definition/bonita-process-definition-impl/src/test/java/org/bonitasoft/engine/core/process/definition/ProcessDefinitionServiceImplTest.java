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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.bpm.process.ActivationState;
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfoCriterion;
import org.bonitasoft.engine.builder.BuilderFactory;
import org.bonitasoft.engine.cache.CacheService;
import org.bonitasoft.engine.core.process.definition.exception.SProcessDefinitionNotFoundException;
import org.bonitasoft.engine.core.process.definition.exception.SProcessDefinitionReadException;
import org.bonitasoft.engine.core.process.definition.exception.SProcessDeploymentInfoUpdateException;
import org.bonitasoft.engine.core.process.definition.model.SProcessDefinitionDeployInfo;
import org.bonitasoft.engine.core.process.definition.model.builder.SProcessDefinitionDeployInfoUpdateBuilder;
import org.bonitasoft.engine.core.process.definition.model.builder.SProcessDefinitionDeployInfoUpdateBuilderFactory;
import org.bonitasoft.engine.dependency.DependencyService;
import org.bonitasoft.engine.events.EventActionType;
import org.bonitasoft.engine.events.EventService;
import org.bonitasoft.engine.events.model.SUpdateEvent;
import org.bonitasoft.engine.identity.model.SUser;
import org.bonitasoft.engine.persistence.OrderByType;
import org.bonitasoft.engine.persistence.QueryOptions;
import org.bonitasoft.engine.persistence.ReadPersistenceService;
import org.bonitasoft.engine.persistence.SBonitaReadException;
import org.bonitasoft.engine.persistence.SelectListDescriptor;
import org.bonitasoft.engine.persistence.SelectOneDescriptor;
import org.bonitasoft.engine.queriablelogger.model.SQueriableLogSeverity;
import org.bonitasoft.engine.recorder.Recorder;
import org.bonitasoft.engine.recorder.SRecorderException;
import org.bonitasoft.engine.recorder.model.UpdateRecord;
import org.bonitasoft.engine.services.QueriableLoggerService;
import org.bonitasoft.engine.session.SessionService;
import org.bonitasoft.engine.sessionaccessor.ReadSessionAccessor;
import org.bonitasoft.engine.xml.ElementBindingsFactory;
import org.bonitasoft.engine.xml.Parser;
import org.bonitasoft.engine.xml.ParserFactory;
import org.bonitasoft.engine.xml.XMLWriter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author Celine Souchet
 */
@RunWith(MockitoJUnitRunner.class)
public class ProcessDefinitionServiceImplTest {

    @Mock
    private CacheService cacheService;

    @Mock
    private DependencyService dependencyService;

    @Mock
    private EventService eventService;

    @Mock
    private ParserFactory parserFactory;

    @Mock
    private ReadPersistenceService persistenceService;

    @Mock
    private QueriableLoggerService queriableLoggerService;

    @Mock
    private Recorder recorder;

    @Mock
    private ReadSessionAccessor sessionAccessor;

    @Mock
    private SessionService sessionService;

    @Mock
    private XMLWriter xmlWriter;

    private ProcessDefinitionServiceImpl processDefinitionServiceImpl;

    @Before
    public void before() {
        final Parser parser = mock(Parser.class);
        doReturn(parser).when(parserFactory).createParser(Matchers.<ElementBindingsFactory> any());

        processDefinitionServiceImpl = new ProcessDefinitionServiceImpl(cacheService, recorder, persistenceService, eventService, sessionService,
                sessionAccessor, parserFactory, xmlWriter, queriableLoggerService, dependencyService);
    }

    /**
     * Test method for
     * {@link org.bonitasoft.engine.core.process.definition.ProcessDefinitionServiceImpl#getProcessDeploymentInfos(int, int, java.lang.String, org.bonitasoft.engine.persistence.OrderByType)}
     * .
     */
    @Test
    public void getProcessDeploymentInfos() throws Exception {
        // Given
        final List<SProcessDefinitionDeployInfo> sProcessDefinitionDeployInfos = new ArrayList<SProcessDefinitionDeployInfo>(3);
        doReturn(sProcessDefinitionDeployInfos).when(persistenceService).selectList(Matchers.<SelectListDescriptor<SProcessDefinitionDeployInfo>> any());

        // When
        final List<SProcessDefinitionDeployInfo> processDeploymentInfos = processDefinitionServiceImpl.getProcessDeploymentInfos(0, 10, "id", OrderByType.ASC);

        // Then
        assertEquals(sProcessDefinitionDeployInfos, processDeploymentInfos);
    }

    @Test(expected = SProcessDefinitionReadException.class)
    public void getProcessDeploymentInfosThrowException() throws Exception {
        // Given
        doThrow(new SBonitaReadException("plop")).when(persistenceService).selectList(Matchers.<SelectListDescriptor<SProcessDefinitionDeployInfo>> any());

        // When
        processDefinitionServiceImpl.getProcessDeploymentInfos(0, 10, "id", OrderByType.ASC);
    }

    /**
     * Test method for {@link org.bonitasoft.engine.core.process.definition.ProcessDefinitionServiceImpl#getNumberOfProcessDeploymentInfos()}.
     */
    @Test
    public void getNumberOfProcessDeploymentInfos() throws Exception {
        // Given
        final long numberOfProcessDeploymentInfos = 9;
        doReturn(numberOfProcessDeploymentInfos).when(persistenceService).selectOne(Matchers.<SelectOneDescriptor<Long>> any());

        // When
        final long result = processDefinitionServiceImpl.getNumberOfProcessDeploymentInfos();

        // Then
        assertEquals(numberOfProcessDeploymentInfos, result);
    }

    @Test(expected = SProcessDefinitionReadException.class)
    public void getNumberOfProcessDeploymentInfosThrowException() throws Exception {
        // Given
        doThrow(new SBonitaReadException("plop")).when(persistenceService).selectOne(Matchers.<SelectOneDescriptor<Long>> any());

        // When
        processDefinitionServiceImpl.getNumberOfProcessDeploymentInfos();
    }

    /**
     * Test method for {@link org.bonitasoft.engine.core.process.definition.ProcessDefinitionServiceImpl#getProcessDeploymentInfo(long)}.
     */
    @Test
    public void getProcessDeploymentInfoById() throws Exception {
        // Given
        final SProcessDefinitionDeployInfo sProcessDefinitionDeployInfo = mock(SProcessDefinitionDeployInfo.class);
        doReturn(sProcessDefinitionDeployInfo).when(persistenceService).selectOne(Matchers.<SelectOneDescriptor<SProcessDefinitionDeployInfo>> any());

        // When
        final SProcessDefinitionDeployInfo result = processDefinitionServiceImpl.getProcessDeploymentInfo(2);

        // Then
        assertEquals(sProcessDefinitionDeployInfo, result);
    }

    @Test(expected = SProcessDefinitionReadException.class)
    public void getProcessDeploymentInfoByIdThrowException() throws Exception {
        // Given
        doThrow(new SBonitaReadException("plop")).when(persistenceService).selectOne(Matchers.<SelectOneDescriptor<SProcessDefinitionDeployInfo>> any());

        // When
        processDefinitionServiceImpl.getProcessDeploymentInfo(2);
    }

    /**
     * Test method for
     * {@link org.bonitasoft.engine.core.process.definition.ProcessDefinitionServiceImpl#getNumberOfProcessDeploymentInfosByActivationState(org.bonitasoft.engine.bpm.process.ActivationState)}
     * .
     */
    @Test
    public void getNumberOfProcessDeploymentInfosByActivationState() throws Exception {
        // Given
        final long numberOfProcessDeploymentInfos = 9;
        doReturn(numberOfProcessDeploymentInfos).when(persistenceService).selectOne(Matchers.<SelectOneDescriptor<Long>> any());

        // When
        final long result = processDefinitionServiceImpl.getNumberOfProcessDeploymentInfosByActivationState(ActivationState.DISABLED);

        // Then
        assertEquals(numberOfProcessDeploymentInfos, result);
    }

    @Test(expected = SProcessDefinitionReadException.class)
    public void getNumberOfProcessDeploymentInfosByActivationStateThrowException() throws Exception {
        // Given
        doThrow(new SBonitaReadException("plop")).when(persistenceService).selectOne(Matchers.<SelectOneDescriptor<Long>> any());

        // When
        processDefinitionServiceImpl.getNumberOfProcessDeploymentInfosByActivationState(ActivationState.DISABLED);
    }

    /**
     * Test method for
     * {@link org.bonitasoft.engine.core.process.definition.ProcessDefinitionServiceImpl#getProcessDefinitionIds(org.bonitasoft.engine.bpm.process.ActivationState, int, int)}
     * .
     */
    @Test
    public void getProcessDefinitionIdsByActivationState() throws Exception {
        // Given
        final List<Long> processDefinitionIds = Arrays.asList(3L);
        doReturn(processDefinitionIds).when(persistenceService).selectList(Matchers.<SelectListDescriptor<Long>> any());

        // When
        final List<Long> result = processDefinitionServiceImpl.getProcessDefinitionIds(ActivationState.DISABLED, 0, 10);

        // Then
        assertEquals(processDefinitionIds, result);
    }

    @Test(expected = SProcessDefinitionReadException.class)
    public void getProcessDefinitionIdsByActivationStateThrowException() throws Exception {
        // Given
        doThrow(new SBonitaReadException("plop")).when(persistenceService).selectList(Matchers.<SelectListDescriptor<Long>> any());

        // When
        processDefinitionServiceImpl.getProcessDefinitionIds(ActivationState.DISABLED, 0, 10);
    }

    /**
     * Test method for {@link org.bonitasoft.engine.core.process.definition.ProcessDefinitionServiceImpl#getProcessDefinitionIds(int, int)}.
     */
    @Test
    public void getProcessDefinitionIds() throws Exception {
        // Given
        final List<Long> processDefinitionIds = Arrays.asList(3L);
        doReturn(processDefinitionIds).when(persistenceService).selectList(Matchers.<SelectListDescriptor<Long>> any());

        // When
        final List<Long> result = processDefinitionServiceImpl.getProcessDefinitionIds(0, 10);

        // Then
        assertEquals(processDefinitionIds, result);
    }

    @Test(expected = SProcessDefinitionReadException.class)
    public void getProcessDefinitionIdsThrowException() throws Exception {
        // Given
        doThrow(new SBonitaReadException("plop")).when(persistenceService).selectList(Matchers.<SelectListDescriptor<Long>> any());

        // When
        processDefinitionServiceImpl.getProcessDefinitionIds(0, 10);
    }

    /**
     * Test method for {@link org.bonitasoft.engine.core.process.definition.ProcessDefinitionServiceImpl#getLatestProcessDefinitionId(java.lang.String)}.
     */
    @Test
    public void getLatestProcessDefinitionId() throws Exception {
        // Given
        final SProcessDefinitionDeployInfo sProcessDefinitionDeployInfo = mock(SProcessDefinitionDeployInfo.class);
        doReturn(6L).when(sProcessDefinitionDeployInfo).getProcessId();
        final List<SProcessDefinitionDeployInfo> sProcessDefinitionDeployInfos = Arrays.asList(sProcessDefinitionDeployInfo);
        doReturn(sProcessDefinitionDeployInfos).when(persistenceService).selectList(Matchers.<SelectListDescriptor<SProcessDefinitionDeployInfo>> any());

        // When
        final long processDeploymentInfoId = processDefinitionServiceImpl.getLatestProcessDefinitionId("name");

        // Then
        assertEquals(sProcessDefinitionDeployInfos.get(0).getProcessId(), processDeploymentInfoId);
    }

    @Test(expected = SProcessDefinitionReadException.class)
    public void getLatestProcessDefinitionIdThrowException() throws Exception {
        // Given
        doThrow(new SBonitaReadException("plop")).when(persistenceService).selectList(Matchers.<SelectListDescriptor<SProcessDefinitionDeployInfo>> any());

        // When
        processDefinitionServiceImpl.getLatestProcessDefinitionId("name");
    }

    /**
     * Test method for
     * {@link ProcessDefinitionService#getProcessDefinitionId(String, String)}.
     */
    @Test
    public void getProcessDefinitionId_should_return_id_of_process_definition_with_given_name_and_version() throws Exception {
        // Given
        final Map<String, Object> parameters = new HashMap<String, Object>();
        String name = "proc";
        String version = "1.0";
        parameters.put("name", name);
        parameters.put("version", version);
        SelectOneDescriptor<Long> selectOneDescriptor = new SelectOneDescriptor<Long>("getProcessDefinitionIdByNameAndVersion", parameters,
                SProcessDefinitionDeployInfo.class, Long.class);

        final long processId = 9;
        doReturn(processId).when(persistenceService).selectOne(selectOneDescriptor);

        // When
        final long result = processDefinitionServiceImpl.getProcessDefinitionId(name, version);

        // Then
        assertEquals(processId, result);
    }

    @Test(expected = SProcessDefinitionNotFoundException.class)
    public void getProcessDefinitionId_should_return_throw_SProcessDefinitionNotFoundException_when_persistenceSservice_returns_null() throws Exception {
        // Given
        final Map<String, Object> parameters = new HashMap<String, Object>();
        String name = "proc";
        String version = "1.0";
        parameters.put("name", name);
        parameters.put("version", version);
        SelectOneDescriptor<Long> selectOneDescriptor = new SelectOneDescriptor<Long>("getProcessDefinitionIdByNameAndVersion", parameters,
                SProcessDefinitionDeployInfo.class, Long.class);

        doReturn(null).when(persistenceService).selectOne(selectOneDescriptor);

        // When
        processDefinitionServiceImpl.getProcessDefinitionId("name", "version");

        // Then exception
    }

    @Test(expected = SProcessDefinitionReadException.class)
    public void getProcessDefinitionId_should_throw_SProcessDefinitionReadException_when_persistenceSservice_throws_exception() throws Exception {
        // Given
        doThrow(new SBonitaReadException("plop")).when(persistenceService).selectOne(Matchers.<SelectOneDescriptor<Long>> any());

        // When
        processDefinitionServiceImpl.getProcessDefinitionId("name", "version");
    }

    /**
     * Test method for
     * {@link org.bonitasoft.engine.core.process.definition.ProcessDefinitionServiceImpl#updateProcessDefinitionDeployInfo(long, org.bonitasoft.engine.recorder.model.EntityUpdateDescriptor)}
     * .
     */
    @Test
    public void updateProcessDefinitionDeployInfo() throws Exception {
        // Given
        final SProcessDefinitionDeployInfo sProcessDefinitionDeployInfo = mock(SProcessDefinitionDeployInfo.class);
        doReturn(3L).when(sProcessDefinitionDeployInfo).getId();

        final SProcessDefinitionDeployInfoUpdateBuilder updateBuilder = BuilderFactory.get(SProcessDefinitionDeployInfoUpdateBuilderFactory.class)
                .createNewInstance();
        updateBuilder.updateDisplayName("newDisplayName");

        doReturn(sProcessDefinitionDeployInfo).when(persistenceService).selectOne(Matchers.<SelectOneDescriptor<SProcessDefinitionDeployInfo>> any());
        doReturn(false).when(eventService).hasHandlers(anyString(), any(EventActionType.class));
        doReturn(false).when(queriableLoggerService).isLoggable(anyString(), any(SQueriableLogSeverity.class));

        // When
        final SProcessDefinitionDeployInfo result = processDefinitionServiceImpl.updateProcessDefinitionDeployInfo(3, updateBuilder.done());

        // Then
        assertNotNull(result);
        assertEquals(sProcessDefinitionDeployInfo, result);
    }

    @Test(expected = SProcessDefinitionNotFoundException.class)
    public final void updateProcessDefinitionDeployInfoNotExists() throws Exception {
        // Given
        final SProcessDefinitionDeployInfoUpdateBuilder updateBuilder = BuilderFactory.get(SProcessDefinitionDeployInfoUpdateBuilderFactory.class)
                .createNewInstance();
        doReturn(null).when(persistenceService).selectOne(Matchers.<SelectOneDescriptor<SProcessDefinitionDeployInfo>> any());

        // When
        processDefinitionServiceImpl.updateProcessDefinitionDeployInfo(4, updateBuilder.done());
    }

    @Test(expected = SProcessDeploymentInfoUpdateException.class)
    public final void updateProcessDefinitionDeployInfoThrowException() throws Exception {
        // Given
        final SProcessDefinitionDeployInfo sProcessDefinitionDeployInfo = mock(SProcessDefinitionDeployInfo.class);
        doReturn(3L).when(sProcessDefinitionDeployInfo).getId();

        final SProcessDefinitionDeployInfoUpdateBuilder updateBuilder = BuilderFactory.get(SProcessDefinitionDeployInfoUpdateBuilderFactory.class)
                .createNewInstance();
        updateBuilder.updateDisplayName("newDisplayName");

        doReturn(sProcessDefinitionDeployInfo).when(persistenceService).selectOne(Matchers.<SelectOneDescriptor<SProcessDefinitionDeployInfo>> any());
        doThrow(new SRecorderException("plop")).when(recorder).recordUpdate(any(UpdateRecord.class), any(SUpdateEvent.class));

        // When
        processDefinitionServiceImpl.updateProcessDefinitionDeployInfo(3, updateBuilder.done());
    }

    /**
     * Test method for
     * {@link org.bonitasoft.engine.core.process.definition.ProcessDefinitionServiceImpl#searchProcessDeploymentInfosStartedBy(long, org.bonitasoft.engine.persistence.QueryOptions)}
     * .
     */
    @Test
    public void searchProcessDeploymentInfosStartedBy() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        final long startedBy = 9;
        when(persistenceService.searchEntity(SProcessDefinitionDeployInfo.class, "StartedBy", options,
                Collections.singletonMap("startedBy", (Object) startedBy))).thenReturn(new ArrayList<SProcessDefinitionDeployInfo>());

        // When
        final List<SProcessDefinitionDeployInfo> result = processDefinitionServiceImpl.searchProcessDeploymentInfosStartedBy(startedBy, options);

        // Then
        assertNotNull(result);
    }

    @Test(expected = SBonitaReadException.class)
    public void searchProcessDeploymentInfosStartedByThrowException() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        final long startedBy = 9;
        when(persistenceService.searchEntity(SProcessDefinitionDeployInfo.class, "StartedBy", options,
                Collections.singletonMap("startedBy", (Object) startedBy))).thenThrow(new SBonitaReadException(""));

        // When
        processDefinitionServiceImpl.searchProcessDeploymentInfosStartedBy(startedBy, options);
    }

    /**
     * Test method for
     * {@link org.bonitasoft.engine.core.process.definition.ProcessDefinitionServiceImpl#getNumberOfProcessDeploymentInfosStartedBy(long, org.bonitasoft.engine.persistence.QueryOptions)}
     * .
     */
    @Test
    public void getNumberOfProcessDeploymentInfosStartedBy() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        final long startedBy = 6;
        when(persistenceService.getNumberOfEntities(SProcessDefinitionDeployInfo.class, "StartedBy", options,
                Collections.singletonMap("startedBy", (Object) startedBy))).thenReturn(1L);

        // When
        final long result = processDefinitionServiceImpl.getNumberOfProcessDeploymentInfosStartedBy(startedBy, options);

        // Then
        assertEquals(1L, result);
    }

    @Test(expected = SBonitaReadException.class)
    public void getNumberOfProcessDeploymentInfosStartedByThrowException() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        final long startedBy = 6;
        when(persistenceService.getNumberOfEntities(SProcessDefinitionDeployInfo.class, "StartedBy", options,
                Collections.singletonMap("startedBy", (Object) startedBy))).thenThrow(new SBonitaReadException(""));

        // When
        processDefinitionServiceImpl.getNumberOfProcessDeploymentInfosStartedBy(startedBy, options);
    }

    /**
     * Test method for
     * {@link org.bonitasoft.engine.core.process.definition.ProcessDefinitionServiceImpl#searchProcessDeploymentInfos(org.bonitasoft.engine.persistence.QueryOptions)}
     * .
     */
    @Test
    public void searchProcessDeploymentInfos() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        when(persistenceService.searchEntity(SProcessDefinitionDeployInfo.class, options, null)).thenReturn(new ArrayList<SProcessDefinitionDeployInfo>());

        // When
        final List<SProcessDefinitionDeployInfo> result = processDefinitionServiceImpl.searchProcessDeploymentInfos(options);

        // Then
        assertNotNull(result);
    }

    @Test(expected = SBonitaReadException.class)
    public void searchProcessDeploymentInfosThrowException() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        when(persistenceService.searchEntity(SProcessDefinitionDeployInfo.class, options, null)).thenThrow(new SBonitaReadException(""));

        // When
        processDefinitionServiceImpl.searchProcessDeploymentInfos(options);
    }

    /**
     * Test method for
     * {@link org.bonitasoft.engine.core.process.definition.ProcessDefinitionServiceImpl#getNumberOfProcessDeploymentInfos(org.bonitasoft.engine.persistence.QueryOptions)}
     * .
     */
    @Test
    public void getNumberOfProcessDeploymentInfosByOptions() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        when(persistenceService.getNumberOfEntities(SProcessDefinitionDeployInfo.class, options, null)).thenReturn(1L);

        // When
        final long result = processDefinitionServiceImpl.getNumberOfProcessDeploymentInfos(options);

        // Then
        assertEquals(1L, result);
    }

    @Test(expected = SBonitaReadException.class)
    public void getNumberOfProcessDeploymentInfosByOptionsThrowException() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        when(persistenceService.getNumberOfEntities(SProcessDefinitionDeployInfo.class, options, null)).thenThrow(new SBonitaReadException(""));

        // When
        processDefinitionServiceImpl.getNumberOfProcessDeploymentInfos(options);
    }

    /**
     * Test method for
     * {@link org.bonitasoft.engine.core.process.definition.ProcessDefinitionServiceImpl#searchProcessDeploymentInfosCanBeStartedBy(long, org.bonitasoft.engine.persistence.QueryOptions)}
     * .
     */
    @Test
    public void searchProcessDeploymentInfosCanBeStartedBy() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        final long userId = 9;
        when(persistenceService.searchEntity(SProcessDefinitionDeployInfo.class, "UserCanStart", options,
                Collections.singletonMap("userId", (Object) userId))).thenReturn(new ArrayList<SProcessDefinitionDeployInfo>());

        // When
        final List<SProcessDefinitionDeployInfo> result = processDefinitionServiceImpl.searchProcessDeploymentInfosCanBeStartedBy(userId, options);

        // Then
        assertNotNull(result);
    }

    @Test(expected = SBonitaReadException.class)
    public void searchProcessDeploymentInfosCanBeStartedByThrowException() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        final long userId = 9;
        when(persistenceService.searchEntity(SProcessDefinitionDeployInfo.class, "UserCanStart", options,
                Collections.singletonMap("userId", (Object) userId))).thenThrow(new SBonitaReadException(""));

        // When
        processDefinitionServiceImpl.searchProcessDeploymentInfosCanBeStartedBy(userId, options);
    }

    /**
     * Test method for
     * {@link org.bonitasoft.engine.core.process.definition.ProcessDefinitionServiceImpl#getNumberOfProcessDeploymentInfosCanBeStartedBy(long, org.bonitasoft.engine.persistence.QueryOptions)}
     * .
     */
    @Test
    public void getNumberOfProcessDeploymentInfosCanBeStartedBy() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        final long userId = 9;
        when(persistenceService.getNumberOfEntities(SProcessDefinitionDeployInfo.class, "UserCanStart", options,
                Collections.singletonMap("userId", (Object) userId))).thenReturn(1L);

        // When
        final long result = processDefinitionServiceImpl.getNumberOfProcessDeploymentInfosCanBeStartedBy(userId, options);

        // Then
        assertEquals(1L, result);
    }

    @Test(expected = SBonitaReadException.class)
    public void getNumberOfProcessDeploymentInfosCanBeStartedByThrowException() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        final long userId = 9;
        when(persistenceService.getNumberOfEntities(SProcessDefinitionDeployInfo.class, "UserCanStart", options,
                Collections.singletonMap("userId", (Object) userId))).thenThrow(new SBonitaReadException(""));

        // When
        processDefinitionServiceImpl.getNumberOfProcessDeploymentInfosCanBeStartedBy(userId, options);
    }

    /**
     * Test method for
     * {@link org.bonitasoft.engine.core.process.definition.ProcessDefinitionServiceImpl#searchProcessDeploymentInfosCanBeStartedByUsersManagedBy(long, org.bonitasoft.engine.persistence.QueryOptions)}
     * .
     */
    @Test
    public void searchProcessDeploymentInfosCanStartForUsersManagedBy() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        final long userId = 9;
        when(persistenceService.searchEntity(SProcessDefinitionDeployInfo.class, "UsersManagedByCanStart", options,
                Collections.singletonMap("managerUserId", (Object) userId))).thenReturn(new ArrayList<SProcessDefinitionDeployInfo>());

        // When
        final List<SProcessDefinitionDeployInfo> result = processDefinitionServiceImpl
                .searchProcessDeploymentInfosCanBeStartedByUsersManagedBy(userId, options);

        // Then
        assertNotNull(result);
    }

    @Test(expected = SBonitaReadException.class)
    public void searchProcessDeploymentInfosCanBeStartedByUsersManagedByThrowException() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        final long userId = 9;
        when(persistenceService.searchEntity(SProcessDefinitionDeployInfo.class, "UsersManagedByCanStart", options,
                Collections.singletonMap("managerUserId", (Object) userId))).thenThrow(new SBonitaReadException(""));

        // When
        processDefinitionServiceImpl.searchProcessDeploymentInfosCanBeStartedByUsersManagedBy(userId, options);
    }

    /**
     * Test method for
     * {@link org.bonitasoft.engine.core.process.definition.ProcessDefinitionServiceImpl#getNumberOfProcessDeploymentInfosCanBeStartedByUsersManagedBy(long, org.bonitasoft.engine.persistence.QueryOptions)}
     * .
     */
    @Test
    public void getNumberOfProcessDeploymentInfosCanBeStartedByUsersManagedBy() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        final long userId = 9;
        when(persistenceService.getNumberOfEntities(SProcessDefinitionDeployInfo.class, "UsersManagedByCanStart", options,
                Collections.singletonMap("managerUserId", (Object) userId))).thenReturn(1L);

        // When
        final long result = processDefinitionServiceImpl.getNumberOfProcessDeploymentInfosCanBeStartedByUsersManagedBy(userId, options);

        // Then
        assertEquals(1L, result);
    }

    @Test(expected = SBonitaReadException.class)
    public void getNumberOfProcessDeploymentInfosCanBeStartedByUsersManagedByThrowException() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        final long userId = 9;
        when(persistenceService.getNumberOfEntities(SProcessDefinitionDeployInfo.class, "UsersManagedByCanStart", options,
                Collections.singletonMap("managerUserId", (Object) userId))).thenThrow(new SBonitaReadException(""));

        // When
        processDefinitionServiceImpl.getNumberOfProcessDeploymentInfosCanBeStartedByUsersManagedBy(userId, options);
    }

    /**
     * Test method for
     * {@link org.bonitasoft.engine.core.process.definition.ProcessDefinitionServiceImpl#searchProcessDeploymentInfos(long, org.bonitasoft.engine.persistence.QueryOptions, java.lang.String)}
     * .
     */
    @Test
    public void searchProcessDeploymentInfosWithParameters() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        final long userId = 9;
        final String querySuffix = "suffix";
        when(persistenceService.searchEntity(SProcessDefinitionDeployInfo.class, querySuffix, options,
                Collections.singletonMap("userId", (Object) userId))).thenReturn(new ArrayList<SProcessDefinitionDeployInfo>());

        // When
        final List<SProcessDefinitionDeployInfo> result = processDefinitionServiceImpl.searchProcessDeploymentInfos(userId, options, querySuffix);

        // Then
        assertNotNull(result);
    }

    @Test(expected = SBonitaReadException.class)
    public void searchProcessDeploymentInfosWithParametersThrowException() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        final long userId = 9;
        final String querySuffix = "suffix";
        when(persistenceService.searchEntity(SProcessDefinitionDeployInfo.class, querySuffix, options,
                Collections.singletonMap("userId", (Object) userId))).thenThrow(new SBonitaReadException(""));

        // When
        processDefinitionServiceImpl.searchProcessDeploymentInfos(userId, options, querySuffix);
    }

    /**
     * Test method for
     * {@link org.bonitasoft.engine.core.process.definition.ProcessDefinitionServiceImpl#getNumberOfProcessDeploymentInfos(long, org.bonitasoft.engine.persistence.QueryOptions, java.lang.String)}
     * .
     */
    @Test
    public void getNumberOfProcessDeploymentInfosWithParameters() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        final long userId = 9;
        final String querySuffix = "suffix";
        when(persistenceService.getNumberOfEntities(SProcessDefinitionDeployInfo.class, querySuffix, options,
                Collections.singletonMap("userId", (Object) userId))).thenReturn(1L);

        // When
        final long result = processDefinitionServiceImpl.getNumberOfProcessDeploymentInfos(userId, options, querySuffix);

        // Then
        assertEquals(1L, result);
    }

    @Test(expected = SBonitaReadException.class)
    public void getNumberOfProcessDeploymentInfosWithParametersThrowException() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        final long userId = 9;
        final String querySuffix = "suffix";
        when(persistenceService.getNumberOfEntities(SProcessDefinitionDeployInfo.class, querySuffix, options,
                Collections.singletonMap("userId", (Object) userId))).thenThrow(new SBonitaReadException(""));

        // When
        processDefinitionServiceImpl.getNumberOfProcessDeploymentInfos(userId, options, querySuffix);
    }

    /**
     * Test method for
     * {@link org.bonitasoft.engine.core.process.definition.ProcessDefinitionServiceImpl#searchUncategorizedProcessDeploymentInfos(org.bonitasoft.engine.persistence.QueryOptions)}
     * .
     */
    @Test
    public void searchUncategorizedProcessDeploymentInfos() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        when(persistenceService.searchEntity(SProcessDefinitionDeployInfo.class, "Uncategorized", options, null)).thenReturn(
                new ArrayList<SProcessDefinitionDeployInfo>());

        // When
        final List<SProcessDefinitionDeployInfo> result = processDefinitionServiceImpl.searchUncategorizedProcessDeploymentInfos(options);

        // Then
        assertNotNull(result);
    }

    @Test(expected = SBonitaReadException.class)
    public void searchUncategorizedProcessDeploymentInfosThrowException() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        when(persistenceService.searchEntity(SProcessDefinitionDeployInfo.class, "Uncategorized", options, null)).thenThrow(new SBonitaReadException(""));

        // When
        processDefinitionServiceImpl.searchUncategorizedProcessDeploymentInfos(options);
    }

    /**
     * Test method for
     * {@link org.bonitasoft.engine.core.process.definition.ProcessDefinitionServiceImpl#getNumberOfUncategorizedProcessDeploymentInfos(org.bonitasoft.engine.persistence.QueryOptions)}
     * .
     */
    @Test
    public void getNumberOfUncategorizedProcessDeploymentInfos() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        when(persistenceService.getNumberOfEntities(SProcessDefinitionDeployInfo.class, "Uncategorized", options, null)).thenReturn(1L);

        // When
        final long result = processDefinitionServiceImpl.getNumberOfUncategorizedProcessDeploymentInfos(options);

        // Then
        assertEquals(1L, result);
    }

    @Test(expected = SBonitaReadException.class)
    public void getNumberOfUncategorizedProcessDeploymentInfosThrowException() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        when(persistenceService.getNumberOfEntities(SProcessDefinitionDeployInfo.class, "Uncategorized", options, null))
                .thenThrow(new SBonitaReadException(""));

        // When
        processDefinitionServiceImpl.getNumberOfUncategorizedProcessDeploymentInfos(options);
    }

    /**
     * Test method for
     * {@link org.bonitasoft.engine.core.process.definition.ProcessDefinitionServiceImpl#searchUncategorizedProcessDeploymentInfosSupervisedBy(long, org.bonitasoft.engine.persistence.QueryOptions)}
     * .
     */
    @Test
    public void searchUncategorizedProcessDeploymentInfosSupervisedBy() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        final long userId = 9;
        when(persistenceService.searchEntity(SProcessDefinitionDeployInfo.class, "UncategorizedAndWithSupervisor", options,
                Collections.singletonMap("userId", (Object) userId))).thenReturn(new ArrayList<SProcessDefinitionDeployInfo>());

        // When
        final List<SProcessDefinitionDeployInfo> result = processDefinitionServiceImpl.searchUncategorizedProcessDeploymentInfosSupervisedBy(userId, options);

        // Then
        assertNotNull(result);
    }

    @Test(expected = SBonitaReadException.class)
    public void searchUncategorizedProcessDeploymentInfosSupervisedByThrowException() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        final long userId = 9;
        when(persistenceService.searchEntity(SProcessDefinitionDeployInfo.class, "UncategorizedAndWithSupervisor", options,
                Collections.singletonMap("userId", (Object) userId))).thenThrow(new SBonitaReadException(""));

        // When
        processDefinitionServiceImpl.searchUncategorizedProcessDeploymentInfosSupervisedBy(userId, options);
    }

    /**
     * Test method for
     * {@link org.bonitasoft.engine.core.process.definition.ProcessDefinitionServiceImpl#getNumberOfUncategorizedProcessDeploymentInfosSupervisedBy(long, org.bonitasoft.engine.persistence.QueryOptions)}
     * .
     */
    @Test
    public void getNumberOfUncategorizedProcessDeploymentInfosSupervisedBy() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        final long userId = 9;
        when(persistenceService.getNumberOfEntities(SProcessDefinitionDeployInfo.class, "UncategorizedAndWithSupervisor", options,
                Collections.singletonMap("userId", (Object) userId))).thenReturn(1L);

        // When
        final long result = processDefinitionServiceImpl.getNumberOfUncategorizedProcessDeploymentInfosSupervisedBy(userId, options);

        // Then
        assertEquals(1L, result);
    }

    @Test(expected = SBonitaReadException.class)
    public void getNumberOfUncategorizedProcessDeploymentInfosSupervisedByThrowException() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        final long userId = 9;
        when(persistenceService.getNumberOfEntities(SProcessDefinitionDeployInfo.class, "UncategorizedAndWithSupervisor", options,
                Collections.singletonMap("userId", (Object) userId))).thenThrow(new SBonitaReadException(""));

        // When
        processDefinitionServiceImpl.getNumberOfUncategorizedProcessDeploymentInfosSupervisedBy(userId, options);
    }

    /**
     * Test method for
     * {@link org.bonitasoft.engine.core.process.definition.ProcessDefinitionServiceImpl#searchUncategorizedProcessDeploymentInfosCanBeStartedBy(long, org.bonitasoft.engine.persistence.QueryOptions)}
     * .
     */
    @Test
    public void searchUncategorizedProcessDeploymentInfosCanBeStartedBy() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        final long userId = 9;
        when(persistenceService.searchEntity(SProcessDefinitionDeployInfo.class, "UncategorizedUserCanStart", options,
                Collections.singletonMap("userId", (Object) userId))).thenReturn(new ArrayList<SProcessDefinitionDeployInfo>());

        // When
        final List<SProcessDefinitionDeployInfo> result = processDefinitionServiceImpl.searchUncategorizedProcessDeploymentInfosCanBeStartedBy(userId, options);

        // Then
        assertNotNull(result);
    }

    @Test(expected = SBonitaReadException.class)
    public void searchUncategorizedProcessDeploymentInfosCanBeStartedByThrowException() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        final long userId = 9;
        when(persistenceService.searchEntity(SProcessDefinitionDeployInfo.class, "UncategorizedUserCanStart", options,
                Collections.singletonMap("userId", (Object) userId))).thenThrow(new SBonitaReadException(""));

        // When
        processDefinitionServiceImpl.searchUncategorizedProcessDeploymentInfosCanBeStartedBy(userId, options);
    }

    /**
     * Test method for
     * {@link org.bonitasoft.engine.core.process.definition.ProcessDefinitionServiceImpl#getNumberOfUncategorizedProcessDeploymentInfosCanBeStartedBy(long, org.bonitasoft.engine.persistence.QueryOptions)}
     * .
     */
    @Test
    public void getNumberOfUncategorizedProcessDeploymentInfosCanBeStartedBy() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        final long userId = 9;
        when(persistenceService.getNumberOfEntities(SProcessDefinitionDeployInfo.class, "UncategorizedUserCanStart", options,
                Collections.singletonMap("userId", (Object) userId))).thenReturn(1L);

        // When
        final long result = processDefinitionServiceImpl.getNumberOfUncategorizedProcessDeploymentInfosCanBeStartedBy(userId, options);

        // Then
        assertEquals(1L, result);
    }

    @Test(expected = SBonitaReadException.class)
    public void getNumberOfUncategorizedProcessDeploymentInfosCanBeStartedByThrowException() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        final long userId = 9;
        when(persistenceService.getNumberOfEntities(SProcessDefinitionDeployInfo.class, "UncategorizedUserCanStart", options,
                Collections.singletonMap("userId", (Object) userId))).thenThrow(new SBonitaReadException(""));

        // When
        processDefinitionServiceImpl.getNumberOfUncategorizedProcessDeploymentInfosCanBeStartedBy(userId, options);
    }

    /**
     * Test method for
     * {@link org.bonitasoft.engine.core.process.definition.ProcessDefinitionServiceImpl#getProcessDeploymentInfosUnrelatedToCategory(long, int, int, org.bonitasoft.engine.bpm.process.ProcessDeploymentInfoCriterion)}
     * .
     */
    @Test
    public void getProcessDeploymentInfosUnrelatedToCategory() throws Exception {
        // Given
        final List<SProcessDefinitionDeployInfo> sProcessDefinitionDeployInfos = new ArrayList<SProcessDefinitionDeployInfo>(3);
        doReturn(sProcessDefinitionDeployInfos).when(persistenceService).selectList(Matchers.<SelectListDescriptor<SProcessDefinitionDeployInfo>> any());

        // When
        final List<SProcessDefinitionDeployInfo> processDeploymentInfos = processDefinitionServiceImpl.getProcessDeploymentInfosUnrelatedToCategory(9, 0, 10,
                ProcessDeploymentInfoCriterion.ACTIVATION_STATE_ASC);

        // Then
        assertEquals(sProcessDefinitionDeployInfos, processDeploymentInfos);
    }

    @Test(expected = SProcessDefinitionReadException.class)
    public void getProcessDeploymentInfosUnrelatedToCategoryThrowException() throws Exception {
        // Given
        doThrow(new SBonitaReadException("plop")).when(persistenceService).selectList(Matchers.<SelectListDescriptor<SProcessDefinitionDeployInfo>> any());

        // When
        processDefinitionServiceImpl.getProcessDeploymentInfosUnrelatedToCategory(9, 0, 10, ProcessDeploymentInfoCriterion.ACTIVATION_STATE_ASC);
    }

    /**
     * Test method for
     * {@link org.bonitasoft.engine.core.process.definition.ProcessDefinitionServiceImpl#getNumberOfProcessDeploymentInfosUnrelatedToCategory(long)}.
     */
    @Test
    public void getNumberOfProcessDeploymentInfosUnrelatedToCategory() throws Exception {
        // Given
        final long numberOfProcessDeploymentInfos = 9;
        doReturn(numberOfProcessDeploymentInfos).when(persistenceService).selectOne(Matchers.<SelectOneDescriptor<Long>> any());

        // When
        final long result = processDefinitionServiceImpl.getNumberOfProcessDeploymentInfosUnrelatedToCategory(9);

        // Then
        assertEquals(numberOfProcessDeploymentInfos, result);
    }

    @Test(expected = SProcessDefinitionReadException.class)
    public void getNumberOfProcessDeploymentInfosUnrelatedToCategoryThrowException() throws Exception {
        // Given
        doThrow(new SBonitaReadException("plop")).when(persistenceService).selectOne(Matchers.<SelectOneDescriptor<Long>> any());

        // When
        processDefinitionServiceImpl.getNumberOfProcessDeploymentInfosUnrelatedToCategory(9);
    }

    /**
     * Test method for
     * {@link org.bonitasoft.engine.core.process.definition.ProcessDefinitionServiceImpl#searchProcessDeploymentInfosOfCategory(long, org.bonitasoft.engine.persistence.QueryOptions)}
     * .
     */
    @Test
    public void searchProcessDeploymentInfosOfCategory() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        final List<SProcessDefinitionDeployInfo> sProcessDefinitionDeployInfos = new ArrayList<SProcessDefinitionDeployInfo>(3);
        doReturn(sProcessDefinitionDeployInfos).when(persistenceService).selectList(Matchers.<SelectListDescriptor<SProcessDefinitionDeployInfo>> any());

        // When
        final List<SProcessDefinitionDeployInfo> processDeploymentInfos = processDefinitionServiceImpl.searchProcessDeploymentInfosOfCategory(9, options);

        // Then
        assertEquals(sProcessDefinitionDeployInfos, processDeploymentInfos);
    }

    @Test(expected = SBonitaReadException.class)
    public void searchProcessDeploymentInfosOfCategoryThrowException() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        doThrow(new SBonitaReadException("plop")).when(persistenceService).selectList(Matchers.<SelectListDescriptor<SProcessDefinitionDeployInfo>> any());

        // When
        processDefinitionServiceImpl.searchProcessDeploymentInfosOfCategory(9, options);
    }

    /**
     * Test method for
     * {@link org.bonitasoft.engine.core.process.definition.ProcessDefinitionServiceImpl#getProcessDeploymentInfos(org.bonitasoft.engine.persistence.QueryOptions)}
     * .
     */
    @Test
    public void getProcessDeploymentInfosWithOptions() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        final List<SProcessDefinitionDeployInfo> sProcessDefinitionDeployInfos = new ArrayList<SProcessDefinitionDeployInfo>(3);
        doReturn(sProcessDefinitionDeployInfos).when(persistenceService).selectList(Matchers.<SelectListDescriptor<SProcessDefinitionDeployInfo>> any());

        // When
        final List<SProcessDefinitionDeployInfo> processDeploymentInfos = processDefinitionServiceImpl.getProcessDeploymentInfos(options);

        // Then
        assertEquals(sProcessDefinitionDeployInfos, processDeploymentInfos);
    }

    @Test(expected = SProcessDefinitionReadException.class)
    public void getProcessDeploymentInfosWithOptionsThrowException() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        doThrow(new SBonitaReadException("plop")).when(persistenceService).selectList(Matchers.<SelectListDescriptor<SProcessDefinitionDeployInfo>> any());

        // When
        processDefinitionServiceImpl.getProcessDeploymentInfos(options);
    }

    /**
     * Test method for
     * {@link org.bonitasoft.engine.core.process.definition.ProcessDefinitionServiceImpl#getProcessDeploymentInfosWithActorOnlyForGroup(long, org.bonitasoft.engine.persistence.QueryOptions)}
     * .
     */
    @Test
    public void getProcessDeploymentInfosWithActorOnlyForGroup() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        final List<SProcessDefinitionDeployInfo> sProcessDefinitionDeployInfos = new ArrayList<SProcessDefinitionDeployInfo>(3);
        doReturn(sProcessDefinitionDeployInfos).when(persistenceService).selectList(Matchers.<SelectListDescriptor<SProcessDefinitionDeployInfo>> any());

        // When
        final List<SProcessDefinitionDeployInfo> processDeploymentInfos = processDefinitionServiceImpl.getProcessDeploymentInfosWithActorOnlyForGroup(9,
                options);

        // Then
        assertEquals(sProcessDefinitionDeployInfos, processDeploymentInfos);
    }

    @Test(expected = SProcessDefinitionReadException.class)
    public void getProcessDeploymentInfosWithActorOnlyForGroupThrowException() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        doThrow(new SBonitaReadException("plop")).when(persistenceService).selectList(Matchers.<SelectListDescriptor<SProcessDefinitionDeployInfo>> any());

        // When
        processDefinitionServiceImpl.getProcessDeploymentInfosWithActorOnlyForGroup(9, options);
    }

    /**
     * Test method for
     * {@link org.bonitasoft.engine.core.process.definition.ProcessDefinitionServiceImpl#getProcessDeploymentInfosWithActorOnlyForGroups(java.util.List, org.bonitasoft.engine.persistence.QueryOptions)}
     * .
     */
    @Test
    public void getProcessDeploymentInfosWithActorOnlyForGroups() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        final List<SProcessDefinitionDeployInfo> sProcessDefinitionDeployInfos = new ArrayList<SProcessDefinitionDeployInfo>(3);
        doReturn(sProcessDefinitionDeployInfos).when(persistenceService).selectList(Matchers.<SelectListDescriptor<SProcessDefinitionDeployInfo>> any());

        // When
        final List<SProcessDefinitionDeployInfo> processDeploymentInfos = processDefinitionServiceImpl.getProcessDeploymentInfosWithActorOnlyForGroups(
                Arrays.asList(9L), options);

        // Then
        assertEquals(sProcessDefinitionDeployInfos, processDeploymentInfos);
    }

    @Test(expected = SProcessDefinitionReadException.class)
    public void getProcessDeploymentInfosWithActorOnlyForGroupsThrowException() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        doThrow(new SBonitaReadException("plop")).when(persistenceService).selectList(Matchers.<SelectListDescriptor<SProcessDefinitionDeployInfo>> any());

        // When
        processDefinitionServiceImpl.getProcessDeploymentInfosWithActorOnlyForGroups(Arrays.asList(9L), options);
    }

    /**
     * Test method for
     * {@link org.bonitasoft.engine.core.process.definition.ProcessDefinitionServiceImpl#getProcessDeploymentInfosWithActorOnlyForRole(long, org.bonitasoft.engine.persistence.QueryOptions)}
     * .
     */
    @Test
    public void getProcessDeploymentInfosWithActorOnlyForRole() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        final List<SProcessDefinitionDeployInfo> sProcessDefinitionDeployInfos = new ArrayList<SProcessDefinitionDeployInfo>(3);
        doReturn(sProcessDefinitionDeployInfos).when(persistenceService).selectList(Matchers.<SelectListDescriptor<SProcessDefinitionDeployInfo>> any());

        // When
        final List<SProcessDefinitionDeployInfo> processDeploymentInfos = processDefinitionServiceImpl.getProcessDeploymentInfosWithActorOnlyForRole(9,
                options);

        // Then
        assertEquals(sProcessDefinitionDeployInfos, processDeploymentInfos);
    }

    @Test(expected = SProcessDefinitionReadException.class)
    public void getProcessDeploymentInfosWithActorOnlyForRoleThrowException() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        doThrow(new SBonitaReadException("plop")).when(persistenceService).selectList(Matchers.<SelectListDescriptor<SProcessDefinitionDeployInfo>> any());

        // When
        processDefinitionServiceImpl.getProcessDeploymentInfosWithActorOnlyForRole(9, options);
    }

    /**
     * Test method for
     * {@link org.bonitasoft.engine.core.process.definition.ProcessDefinitionServiceImpl#getProcessDeploymentInfosWithActorOnlyForRoles(java.util.List, org.bonitasoft.engine.persistence.QueryOptions)}
     * .
     */
    @Test
    public void getProcessDeploymentInfosWithActorOnlyForRoles() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        final List<SProcessDefinitionDeployInfo> sProcessDefinitionDeployInfos = new ArrayList<SProcessDefinitionDeployInfo>(3);
        doReturn(sProcessDefinitionDeployInfos).when(persistenceService).selectList(Matchers.<SelectListDescriptor<SProcessDefinitionDeployInfo>> any());

        // When
        final List<SProcessDefinitionDeployInfo> processDeploymentInfos = processDefinitionServiceImpl.getProcessDeploymentInfosWithActorOnlyForRoles(
                Arrays.asList(9L), options);

        // Then
        assertEquals(sProcessDefinitionDeployInfos, processDeploymentInfos);
    }

    @Test(expected = SProcessDefinitionReadException.class)
    public void getProcessDeploymentInfosWithActorOnlyForRolesThrowException() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        doThrow(new SBonitaReadException("plop")).when(persistenceService).selectList(Matchers.<SelectListDescriptor<SProcessDefinitionDeployInfo>> any());

        // When
        processDefinitionServiceImpl.getProcessDeploymentInfosWithActorOnlyForRoles(Arrays.asList(9L), options);
    }

    /**
     * Test method for
     * {@link org.bonitasoft.engine.core.process.definition.ProcessDefinitionServiceImpl#getProcessDeploymentInfosWithActorOnlyForUser(long, org.bonitasoft.engine.persistence.QueryOptions)}
     * .
     */
    @Test
    public void getProcessDeploymentInfosWithActorOnlyForUser() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        final List<SProcessDefinitionDeployInfo> sProcessDefinitionDeployInfos = new ArrayList<SProcessDefinitionDeployInfo>(3);
        doReturn(sProcessDefinitionDeployInfos).when(persistenceService).selectList(Matchers.<SelectListDescriptor<SProcessDefinitionDeployInfo>> any());

        // When
        final List<SProcessDefinitionDeployInfo> processDeploymentInfos = processDefinitionServiceImpl.getProcessDeploymentInfosWithActorOnlyForUser(9,
                options);

        // Then
        assertEquals(sProcessDefinitionDeployInfos, processDeploymentInfos);
    }

    @Test(expected = SProcessDefinitionReadException.class)
    public void getProcessDeploymentInfosWithActorOnlyForUserThrowException() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        doThrow(new SBonitaReadException("plop")).when(persistenceService).selectList(Matchers.<SelectListDescriptor<SProcessDefinitionDeployInfo>> any());

        // When
        processDefinitionServiceImpl.getProcessDeploymentInfosWithActorOnlyForUser(9, options);
    }

    /**
     * Test method for
     * {@link org.bonitasoft.engine.core.process.definition.ProcessDefinitionServiceImpl#getProcessDeploymentInfosWithActorOnlyForUsers(java.util.List, org.bonitasoft.engine.persistence.QueryOptions)}
     * .
     */
    @Test
    public void getProcessDeploymentInfosWithActorOnlyForUsers() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        final List<SProcessDefinitionDeployInfo> sProcessDefinitionDeployInfos = new ArrayList<SProcessDefinitionDeployInfo>(3);
        doReturn(sProcessDefinitionDeployInfos).when(persistenceService).selectList(Matchers.<SelectListDescriptor<SProcessDefinitionDeployInfo>> any());

        // When
        final List<SProcessDefinitionDeployInfo> processDeploymentInfos = processDefinitionServiceImpl.getProcessDeploymentInfosWithActorOnlyForUsers(
                Arrays.asList(9L), options);

        // Then
        assertEquals(sProcessDefinitionDeployInfos, processDeploymentInfos);
    }

    @Test(expected = SProcessDefinitionReadException.class)
    public void getProcessDeploymentInfosWithActorOnlyForUsersThrowException() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        doThrow(new SBonitaReadException("plop")).when(persistenceService).selectList(Matchers.<SelectListDescriptor<SProcessDefinitionDeployInfo>> any());

        // When
        processDefinitionServiceImpl.getProcessDeploymentInfosWithActorOnlyForUsers(Arrays.asList(9L), options);
    }

    /**
     * Test method for
     * {@link org.bonitasoft.engine.core.process.definition.ProcessDefinitionServiceImpl#searchUsersWhoCanStartProcessDeploymentInfo(long, org.bonitasoft.engine.persistence.QueryOptions)}
     * .
     */
    @Test
    public void searchUsersWhoCanStartProcessDeploymentInfo() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        final long processDefinitionId = 9;
        when(persistenceService.searchEntity(SUser.class, "WhoCanStartProcess", options,
                Collections.singletonMap("processId", (Object) processDefinitionId))).thenReturn(new ArrayList<SUser>());

        // When
        final List<SUser> result = processDefinitionServiceImpl.searchUsersWhoCanStartProcessDeploymentInfo(processDefinitionId, options);

        // Then
        assertNotNull(result);
    }

    @Test(expected = SBonitaReadException.class)
    public void searchUsersWhoCanStartProcessDeploymentInfoThrowException() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        final long processDefinitionId = 9;
        when(persistenceService.searchEntity(SUser.class, "WhoCanStartProcess", options,
                Collections.singletonMap("processId", (Object) processDefinitionId))).thenThrow(new SBonitaReadException(""));

        // When
        processDefinitionServiceImpl.searchUsersWhoCanStartProcessDeploymentInfo(processDefinitionId, options);
    }

    /**
     * Test method for
     * {@link org.bonitasoft.engine.core.process.definition.ProcessDefinitionServiceImpl#getNumberOfUsersWhoCanStartProcessDeploymentInfo(long, org.bonitasoft.engine.persistence.QueryOptions)}
     * .
     */
    @Test
    public void getNumberOfUsersWhoCanStartProcessDeploymentInfo() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        final long processDefinitionId = 9;
        when(persistenceService.getNumberOfEntities(SUser.class, "WhoCanStartProcess", options,
                Collections.singletonMap("processId", (Object) processDefinitionId))).thenReturn(1L);

        // When
        final long result = processDefinitionServiceImpl.getNumberOfUsersWhoCanStartProcessDeploymentInfo(processDefinitionId, options);

        // Then
        assertEquals(1L, result);
    }

    @Test(expected = SBonitaReadException.class)
    public void getNumberOfUsersWhoCanStartProcessDeploymentInfoThrowException() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        final long processDefinitionId = 9;
        when(persistenceService.getNumberOfEntities(SUser.class, "WhoCanStartProcess", options,
                Collections.singletonMap("processId", (Object) processDefinitionId))).thenThrow(new SBonitaReadException(""));

        // When
        processDefinitionServiceImpl.getNumberOfUsersWhoCanStartProcessDeploymentInfo(processDefinitionId, options);
    }

    /**
     * Test method for
     * {@link org.bonitasoft.engine.core.process.definition.ProcessDefinitionServiceImpl#searchProcessDeploymentInfosWithAssignedOrPendingHumanTasksFor(long, org.bonitasoft.engine.persistence.QueryOptions)}
     * .
     */
    @Test
    public void searchProcessDeploymentInfosWithAssignedOrPendingHumanTasksFor() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        final long userId = 9;
        when(persistenceService.searchEntity(SProcessDefinitionDeployInfo.class, "WithAssignedOrPendingHumanTasksFor", options,
                Collections.singletonMap("userId", (Object) userId))).thenReturn(new ArrayList<SProcessDefinitionDeployInfo>());

        // When
        final List<SProcessDefinitionDeployInfo> result = processDefinitionServiceImpl.searchProcessDeploymentInfosWithAssignedOrPendingHumanTasksFor(
                userId,
                options);

        // Then
        assertNotNull(result);
    }

    @Test(expected = SBonitaReadException.class)
    public void searchProcessDeploymentInfosWithAssignedOrPendingHumanTasksForThrowException() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        final long userId = 9;
        when(persistenceService.searchEntity(SProcessDefinitionDeployInfo.class, "WithAssignedOrPendingHumanTasksFor", options,
                Collections.singletonMap("userId", (Object) userId))).thenThrow(new SBonitaReadException(""));

        // When
        processDefinitionServiceImpl.searchProcessDeploymentInfosWithAssignedOrPendingHumanTasksFor(userId, options);
    }

    /**
     * Test method for
     * {@link org.bonitasoft.engine.core.process.definition.ProcessDefinitionServiceImpl#getNumberOfProcessDeploymentInfosWithAssignedOrPendingHumanTasksFor(long, org.bonitasoft.engine.persistence.QueryOptions)}
     * .
     */
    @Test
    public void getNumberOfProcessDeploymentInfosWithAssignedOrPendingHumanTasksFor() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        final long userId = 9;
        when(persistenceService.getNumberOfEntities(SProcessDefinitionDeployInfo.class, "WithAssignedOrPendingHumanTasksFor", options,
                Collections.singletonMap("userId", (Object) userId))).thenReturn(1L);

        // When
        final long result = processDefinitionServiceImpl.getNumberOfProcessDeploymentInfosWithAssignedOrPendingHumanTasksFor(userId, options);

        // Then
        assertEquals(1L, result);
    }

    @Test(expected = SBonitaReadException.class)
    public void getNumberOfProcessDeploymentInfosWithAssignedOrPendingHumanTasksForThrowException() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        final long userId = 9;
        when(persistenceService.getNumberOfEntities(SProcessDefinitionDeployInfo.class, "WithAssignedOrPendingHumanTasksFor", options,
                Collections.singletonMap("userId", (Object) userId))).thenThrow(new SBonitaReadException(""));

        // When
        processDefinitionServiceImpl.getNumberOfProcessDeploymentInfosWithAssignedOrPendingHumanTasksFor(userId, options);
    }

    /**
     * Test method for
     * {@link org.bonitasoft.engine.core.process.definition.ProcessDefinitionServiceImpl#searchProcessDeploymentInfosWithAssignedOrPendingHumanTasksSupervisedBy(long, org.bonitasoft.engine.persistence.QueryOptions)}
     * .
     */
    @Test
    public void searchProcessDeploymentInfosWithAssignedOrPendingHumanTasksSupervisedBy() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        final long userId = 9;
        when(persistenceService.searchEntity(SProcessDefinitionDeployInfo.class, "WithAssignedOrPendingHumanTasksSupervisedBy", options,
                Collections.singletonMap("userId", (Object) userId))).thenReturn(new ArrayList<SProcessDefinitionDeployInfo>());

        // When
        final List<SProcessDefinitionDeployInfo> result = processDefinitionServiceImpl.searchProcessDeploymentInfosWithAssignedOrPendingHumanTasksSupervisedBy(
                userId,
                options);

        // Then
        assertNotNull(result);
    }

    @Test(expected = SBonitaReadException.class)
    public void searchProcessDeploymentInfosWithAssignedOrPendingHumanTasksSupervisedByThrowException() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        final long userId = 9;
        when(persistenceService.searchEntity(SProcessDefinitionDeployInfo.class, "WithAssignedOrPendingHumanTasksSupervisedBy", options,
                Collections.singletonMap("userId", (Object) userId))).thenThrow(new SBonitaReadException(""));

        // When
        processDefinitionServiceImpl.searchProcessDeploymentInfosWithAssignedOrPendingHumanTasksSupervisedBy(userId, options);
    }

    /**
     * Test method for
     * {@link org.bonitasoft.engine.core.process.definition.ProcessDefinitionServiceImpl#getNumberOfProcessDeploymentInfosWithAssignedOrPendingHumanTasksSupervisedBy(long, org.bonitasoft.engine.persistence.QueryOptions)}
     * .
     */
    @Test
    public void getNumberOfProcessDeploymentInfosWithAssignedOrPendingHumanTasksSupervisedBy() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        final long userId = 9;
        when(persistenceService.getNumberOfEntities(SProcessDefinitionDeployInfo.class, "WithAssignedOrPendingHumanTasksSupervisedBy", options,
                Collections.singletonMap("userId", (Object) userId))).thenReturn(1L);

        // When
        final long result = processDefinitionServiceImpl.getNumberOfProcessDeploymentInfosWithAssignedOrPendingHumanTasksSupervisedBy(userId, options);

        // Then
        assertEquals(1L, result);
    }

    @Test(expected = SBonitaReadException.class)
    public void getNumberOfProcessDeploymentInfosWithAssignedOrPendingHumanTasksSupervisedByThrowException() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        final long userId = 9;
        when(persistenceService.getNumberOfEntities(SProcessDefinitionDeployInfo.class, "WithAssignedOrPendingHumanTasksSupervisedBy", options,
                Collections.singletonMap("userId", (Object) userId))).thenThrow(new SBonitaReadException(""));

        // When
        processDefinitionServiceImpl.getNumberOfProcessDeploymentInfosWithAssignedOrPendingHumanTasksSupervisedBy(userId, options);
    }

    /**
     * Test method for
     * {@link org.bonitasoft.engine.core.process.definition.ProcessDefinitionServiceImpl#searchProcessDeploymentInfosWithAssignedOrPendingHumanTasks(org.bonitasoft.engine.persistence.QueryOptions)}
     * .
     */
    @Test
    public void searchProcessDeploymentInfosWithAssignedOrPendingHumanTasks() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        when(persistenceService.searchEntity(SProcessDefinitionDeployInfo.class, "WithAssignedOrPendingHumanTasks", options, null)).thenReturn(
                new ArrayList<SProcessDefinitionDeployInfo>());

        // When
        final List<SProcessDefinitionDeployInfo> result = processDefinitionServiceImpl.searchProcessDeploymentInfosWithAssignedOrPendingHumanTasks(options);

        // Then
        assertNotNull(result);
    }

    @Test(expected = SBonitaReadException.class)
    public void searchProcessDeploymentInfosWithAssignedOrPendingHumanTasksThrowException() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        when(persistenceService.searchEntity(SProcessDefinitionDeployInfo.class, "WithAssignedOrPendingHumanTasks", options, null)).thenThrow(
                new SBonitaReadException(""));

        // When
        processDefinitionServiceImpl.searchProcessDeploymentInfosWithAssignedOrPendingHumanTasks(options);
    }

    /**
     * Test method for
     * {@link org.bonitasoft.engine.core.process.definition.ProcessDefinitionServiceImpl#getNumberOfProcessDeploymentInfosWithAssignedOrPendingHumanTasks(org.bonitasoft.engine.persistence.QueryOptions)}
     * .
     */
    @Test
    public void getNumberOfProcessDeploymentInfosWithAssignedOrPendingHumanTasks() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        when(persistenceService.getNumberOfEntities(SProcessDefinitionDeployInfo.class, "WithAssignedOrPendingHumanTasks", options, null)).thenReturn(1L);

        // When
        final long result = processDefinitionServiceImpl.getNumberOfProcessDeploymentInfosWithAssignedOrPendingHumanTasks(options);

        // Then
        assertEquals(1L, result);
    }

    @Test(expected = SBonitaReadException.class)
    public void getNumberOfProcessDeploymentInfosWithAssignedOrPendingHumanTasksThrowException() throws Exception {
        // Given
        final QueryOptions options = new QueryOptions(0, 10);
        when(persistenceService.getNumberOfEntities(SProcessDefinitionDeployInfo.class, "WithAssignedOrPendingHumanTasks", options, null)).thenThrow(
                new SBonitaReadException(""));

        // When
        processDefinitionServiceImpl.getNumberOfProcessDeploymentInfosWithAssignedOrPendingHumanTasks(options);
    }
}
