package org.cloudfoundry.multiapps.controller.core.helpers;

import java.util.UUID;

import org.cloudfoundry.multiapps.controller.core.util.ApplicationURI;
import org.cloudfoundry.multiapps.controller.persistence.model.CloudTarget;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.domain.CloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.CloudOrganization;
import com.sap.cloudfoundry.client.facade.domain.CloudSpace;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudOrganization;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudSpace;

class ClientHelperTest {

    private static final String ORG_NAME = "some-organization";
    private static final String SPACE_NAME = "custom-space";
    private static final UUID GUID = UUID.randomUUID();
    private static final String SPACE_ID = "8819b12c-6dde-4338-8530-93b2fba56df6";

    @Mock
    private CloudControllerClient client;

    private ClientHelper clientHelper;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        clientHelper = new ClientHelper(client);
    }

    @Test
    void testDeleteRoute() {
        String uri = "https://some-route.next.domain";
        clientHelper.deleteRoute(uri);
        ApplicationURI route = new ApplicationURI(uri);
        Mockito.verify(client)
               .deleteRoute(route.getHost(), route.getDomain(), route.getPath());
    }

    @Test
    void testComputeSpaceId() {
        Mockito.when(client.getSpace(ORG_NAME, SPACE_NAME, false))
               .thenReturn(createCloudSpace(GUID, SPACE_NAME, ORG_NAME));
        String spaceId = clientHelper.computeSpaceId(ORG_NAME, SPACE_NAME);
        Assertions.assertEquals(GUID.toString(), spaceId);
    }

    @Test
    void testComputeSpaceIdIfSpaceIsNull() {
        Assertions.assertNull(clientHelper.computeSpaceId(ORG_NAME, SPACE_NAME));
    }

    @Test
    void testComputeTarget() {
        Mockito.when(client.getSpace(Matchers.any(UUID.class)))
               .thenReturn(createCloudSpace(GUID, SPACE_NAME, ORG_NAME));
        CloudTarget target = clientHelper.computeTarget(SPACE_ID);
        Assertions.assertEquals(ORG_NAME, target.getOrganizationName());
        Assertions.assertEquals(SPACE_NAME, target.getSpaceName());
    }

    @Test
    void testComputeTargetCloudOperationExceptionForbiddenThrown() {
        Mockito.when(client.getSpace(Matchers.any(UUID.class)))
               .thenThrow(new CloudOperationException(HttpStatus.FORBIDDEN));
        Assertions.assertNull(clientHelper.computeTarget(SPACE_ID));
    }

    @Test
    void testComputeTargetCloudOperationExceptionNotFoundThrown() {
        Mockito.when(client.getSpace(Matchers.any(UUID.class)))
               .thenThrow(new CloudOperationException(HttpStatus.NOT_FOUND));
        Assertions.assertNull(clientHelper.computeTarget(SPACE_ID));
    }

    @Test
    void testComputeTargetCloudOperationExceptionBadRequestThrown() {
        Mockito.when(client.getSpace(Matchers.any(UUID.class)))
               .thenThrow(new CloudOperationException(HttpStatus.BAD_REQUEST));
        CloudOperationException cloudOperationException = Assertions.assertThrows(CloudOperationException.class,
                                                                                  () -> clientHelper.computeTarget(SPACE_ID));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, cloudOperationException.getStatusCode());
    }

    private CloudSpace createCloudSpace(UUID guid, String spaceName, String organizationName) {
        return ImmutableCloudSpace.builder()
                                  .name(spaceName)
                                  .organization(createCloudOrganization(organizationName))
                                  .metadata(createCloudMetadata(guid))
                                  .build();
    }

    private CloudOrganization createCloudOrganization(String organizationName) {
        return ImmutableCloudOrganization.builder()
                                         .name(organizationName)
                                         .build();
    }

    private CloudMetadata createCloudMetadata(UUID guid) {
        return ImmutableCloudMetadata.builder()
                                     .guid(guid)
                                     .build();
    }
}
