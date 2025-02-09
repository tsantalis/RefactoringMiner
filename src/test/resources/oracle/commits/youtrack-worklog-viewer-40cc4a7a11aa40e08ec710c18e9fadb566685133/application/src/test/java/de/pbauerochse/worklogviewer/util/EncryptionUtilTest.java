package de.pbauerochse.worklogviewer.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class EncryptionUtilTest {

    @Test
    void performTest() throws Exception {
        String cleartextPassword = "super g€h€ime$ passwört";

        String encryptPassword = EncryptionUtil.encryptCleartextString(cleartextPassword);
        assertNotEquals("Klartext Passwort und verschlüsseltes Passwort sind identisch", cleartextPassword, encryptPassword);

        String entschluesseltesPasswort = EncryptionUtil.decryptEncryptedString(encryptPassword);
        assertEquals(cleartextPassword, entschluesseltesPasswort);
    }

}
