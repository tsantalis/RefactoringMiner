package org.cloudfoundry.multiapps.controller.web.api.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.common.ConflictException;
import org.cloudfoundry.multiapps.common.NotFoundException;
import org.cloudfoundry.multiapps.common.test.TestUtil;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.api.model.Metadata;
import org.cloudfoundry.multiapps.controller.api.model.Module;
import org.cloudfoundry.multiapps.controller.api.model.Mta;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientProvider;
import org.cloudfoundry.multiapps.controller.core.cf.detect.DeployedMtaDetector;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.ImmutableMtaMetadata;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadata;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaApplication;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaService;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDeployedMtaApplication;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDeployedMtaService;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationURI;
import org.cloudfoundry.multiapps.controller.core.util.UserInfo;
import org.cloudfoundry.multiapps.mta.model.Version;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudRouteSummary;

class MtasApiServiceImplTest {

    @Mock
    private CloudControllerClientProvider clientProvider;
    @Mock
    private CloudControllerClient client;

    @Mock
    private DeployedMtaDetector deployedMtaDetector;

    @InjectMocks
    private MtasApiServiceImpl testedClass;

    private List<CloudApplication> apps;
    private List<Mta> mtas;

    private static final String USER_NAME = "someUser";
    private static final String SPACE_GUID = UUID.randomUUID()
                                                 .toString();

    @BeforeEach
    void initialize() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        apps = parseApps();
        mtas = parseMtas();
        mockClient();
    }

    private List<CloudApplication> parseApps() {
        String appsJson = TestUtil.getResourceAsString("apps-01.json", getClass());
        return JsonUtil.fromJson(appsJson, new TypeReference<>() {
        });
    }

    private List<Mta> parseMtas() {
        String appsJson = TestUtil.getResourceAsString("mtas-01.json", getClass());
        return JsonUtil.fromJson(appsJson, new TypeReference<>() {
        });
    }

    @Test
    void testGetMtas() {
        Mockito.when(deployedMtaDetector.detectDeployedMtasWithoutNamespace(Mockito.any()))
               .thenReturn(getDeployedMtas(mtas));

        ResponseEntity<List<Mta>> response = testedClass.getMtas(SPACE_GUID);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Mta> responseMtas = response.getBody();
        assertEquals(mtas, responseMtas);
    }

    @Test
    void testGetAllMtas() {
        Mockito.when(deployedMtaDetector.detectDeployedMtas(Mockito.any()))
               .thenReturn(getDeployedMtas(mtas));

        ResponseEntity<List<Mta>> response = testedClass.getMtas(SPACE_GUID, null, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Mta> responseMtas = response.getBody();
        assertEquals(mtas, responseMtas);
    }

    @Test
    void testGetMtasByName() {
        Mta mtaToGet = mtas.get(1);
        Mockito.when(deployedMtaDetector.detectDeployedMtasByName(mtaToGet.getMetadata()
                                                                          .getId(),
                                                                  client))
               .thenReturn(List.of(getDeployedMta(mtaToGet)));

        ResponseEntity<List<Mta>> response = testedClass.getMtas(SPACE_GUID, null, mtaToGet.getMetadata()
                                                                                           .getId());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Mta> responseMtas = response.getBody();
        assertEquals(List.of(mtaToGet), responseMtas);
    }

    @Test
    void testGetMtasByNamespace() {
        Mta mtaToGet = mtas.get(0);
        Mockito.when(deployedMtaDetector.detectDeployedMtasByNamespace(mtaToGet.getMetadata()
                                                                               .getNamespace(),
                                                                       client))
               .thenReturn(List.of(getDeployedMta(mtaToGet)));

        ResponseEntity<List<Mta>> response = testedClass.getMtas(SPACE_GUID, mtaToGet.getMetadata()
                                                                                     .getNamespace(),
                                                                 null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Mta> responseMtas = response.getBody();
        assertEquals(List.of(mtaToGet), responseMtas);
    }

    @Test
    void testGetMtasByNameAndNamespace() {
        Mta mtaToGet = mtas.get(0);
        Mockito.when(deployedMtaDetector.detectDeployedMtaByNameAndNamespace(mtaToGet.getMetadata()
                                                                                     .getId(),
                                                                             mtaToGet.getMetadata()
                                                                                     .getNamespace(),
                                                                             client))
               .thenReturn(Optional.of(getDeployedMta(mtaToGet)));

        ResponseEntity<List<Mta>> response = testedClass.getMtas(SPACE_GUID, mtaToGet.getMetadata()
                                                                                     .getNamespace(),
                                                                 mtaToGet.getMetadata()
                                                                         .getId());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Mta> responseMtas = response.getBody();
        assertEquals(List.of(mtaToGet), responseMtas);
    }

    @Test
    void testGetMta() {
        Mta mtaToGet = mtas.get(1);
        Mockito.when(deployedMtaDetector.detectDeployedMtasByName(mtaToGet.getMetadata()
                                                                          .getId(),
                                                                  client))
               .thenReturn(List.of(getDeployedMta(mtaToGet)));

        ResponseEntity<Mta> response = testedClass.getMta(SPACE_GUID, mtaToGet.getMetadata()
                                                                              .getId());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Mta responseMta = response.getBody();
        assertEquals(mtaToGet, responseMta);
    }

    @Test
    void testGetMtaNotFound() {
        Assertions.assertThrows(NotFoundException.class, () -> testedClass.getMta(SPACE_GUID, "not_a_real_mta"));
    }

    @Test
    void testGetMtaNotUniqueByName() {
        Mockito.when(deployedMtaDetector.detectDeployedMtasByName("name_thats_not_unique", client))
               .thenReturn(getDeployedMtas(mtas));

        Assertions.assertThrows(ConflictException.class, () -> testedClass.getMta(SPACE_GUID, "name_thats_not_unique"));
    }

    private void mockClient() {
        UserInfo userInfo = new UserInfo(null, USER_NAME, null);
        Authentication auth = Mockito.mock(Authentication.class);
        Mockito.when(auth.getPrincipal())
               .thenReturn(userInfo);
        SecurityContext securityContextMock = Mockito.mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContextMock);
        Mockito.when(securityContextMock.getAuthentication())
               .thenReturn(auth);
        Mockito.when(clientProvider.getControllerClient(Mockito.anyString(), Mockito.anyString()))
               .thenReturn(client);
        Mockito.when(client.getApplications())
               .thenReturn(apps);
    }

    private List<DeployedMta> getDeployedMtas(List<Mta> mtas) {
        return mtas.stream()
                   .map(this::getDeployedMta)
                   .collect(Collectors.toList());
    }

    private DeployedMta getDeployedMta(Mta mta) {
        return ImmutableDeployedMta.builder()
                                   .metadata(getMtaMetadata(mta.getMetadata()))
                                   .applications(getDeployedMtaApplications(mta.getModules()))
                                   .services(mta.getServices()
                                                .stream()
                                                .map(this::getDeployedMtaService)
                                                .collect(Collectors.toList()))
                                   .build();
    }

    private List<DeployedMtaApplication> getDeployedMtaApplications(List<Module> modules) {
        return modules.stream()
                      .map(this::getDeployedMtaApplication)
                      .collect(Collectors.toList());
    }

    private DeployedMtaApplication getDeployedMtaApplication(Module module) {
        List<String> services = module.getServices();
        return ImmutableDeployedMtaApplication.builder()
                                              .name(module.getAppName())
                                              .moduleName(module.getModuleName())
                                              .providedDependencyNames(module.getProvidedDendencyNames())
                                              .routes(parseUrisAsRoutes(module.getUris()))
                                              .boundMtaServices(services)
                                              .build();
    }
    
    private Set<CloudRouteSummary> parseUrisAsRoutes(List<String> uris) {
        return uris.stream()
                   .map(uri -> new ApplicationURI(uri, false).toCloudRouteSummary())
                   .collect(Collectors.toSet());
    }

    private DeployedMtaService getDeployedMtaService(String service) {
        return ImmutableDeployedMtaService.builder()
                                          .name(service)
                                          .resourceName(service)
                                          .build();
    }

    private MtaMetadata getMtaMetadata(Metadata metadata) {
        return ImmutableMtaMetadata.builder()
                                   .id(metadata.getId())
                                   .version(Version.parseVersion(metadata.getVersion()))
                                   .namespace(metadata.getNamespace())
                                   .build();
    }

}
