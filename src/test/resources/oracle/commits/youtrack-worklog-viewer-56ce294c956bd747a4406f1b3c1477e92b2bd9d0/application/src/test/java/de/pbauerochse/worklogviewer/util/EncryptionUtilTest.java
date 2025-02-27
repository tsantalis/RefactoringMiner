package de.pbauerochse.worklogviewer.util;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Patrick Bauerochse
 * @since 14.04.15
 */
public class EncryptionUtilTest {

    @Test
    public void performTest() throws Exception {
        String cleartextPassword = "super g€h€ime$ passwört";
        String encryptPassword = EncryptionUtil.encryptCleartextString(cleartextPassword);

        Assert.assertNotEquals("Klartext Passwort und verschlüsseltes Passwort sind identisch", cleartextPassword, encryptPassword);

        String entschluesseltesPasswort = EncryptionUtil.decryptEncryptedString(encryptPassword);

        Assert.assertEquals(cleartextPassword, entschluesseltesPasswort);
    }

}
