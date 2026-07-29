package org.cloudfoundry.multiapps.mta.handlers;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.cloudfoundry.multiapps.mta.handlers.v2.DescriptorHandler;
import org.cloudfoundry.multiapps.mta.handlers.v2.DescriptorParser;
import org.cloudfoundry.multiapps.mta.handlers.v2.DescriptorValidator;
import org.junit.jupiter.api.Test;

class HandlerFactoryTest {

    @Test
    void testGetV2DescriptorHandler() {
        DescriptorHandler handler = new HandlerFactory(2).getDescriptorHandler();
        assertTrue(handler instanceof org.cloudfoundry.multiapps.mta.handlers.v2.DescriptorHandler);
    }

    @Test
    void testGetV3DescriptorHandler() {
        DescriptorHandler handler = new HandlerFactory(3).getDescriptorHandler();
        assertTrue(handler instanceof org.cloudfoundry.multiapps.mta.handlers.v3.DescriptorHandler);
    }

    @Test
    void testGetUnsupportedDescriptorHandler() {
        assertThrows(UnsupportedOperationException.class, () -> new HandlerFactory(128).getDescriptorHandler());
    }

    @Test
    void testGetV2DescriptorValidator() {
        DescriptorValidator handler = new HandlerFactory(2).getDescriptorValidator();
        assertTrue(handler instanceof org.cloudfoundry.multiapps.mta.handlers.v2.DescriptorValidator);
    }

    @Test
    void testGetV3DescriptorValidator() {
        DescriptorValidator handler = new HandlerFactory(3).getDescriptorValidator();
        assertTrue(handler instanceof org.cloudfoundry.multiapps.mta.handlers.v3.DescriptorValidator);
    }

    @Test
    void testGetUnsupportedDescriptorValidator() {
        assertThrows(UnsupportedOperationException.class, () -> new HandlerFactory(128).getDescriptorValidator());
    }

    @Test
    void testGetV2DescriptorParser() {
        DescriptorParser handler = new HandlerFactory(2).getDescriptorParser();
        assertTrue(handler instanceof org.cloudfoundry.multiapps.mta.handlers.v2.DescriptorParser);
    }

    @Test
    void testGetV3DescriptorParser() {
        DescriptorParser handler = new HandlerFactory(3).getDescriptorParser();
        assertTrue(handler instanceof org.cloudfoundry.multiapps.mta.handlers.v3.DescriptorParser);
    }

    @Test
    void testGetUnsupportedDescriptorParser() {
        assertThrows(UnsupportedOperationException.class, () -> new HandlerFactory(128).getDescriptorParser());
    }

}
