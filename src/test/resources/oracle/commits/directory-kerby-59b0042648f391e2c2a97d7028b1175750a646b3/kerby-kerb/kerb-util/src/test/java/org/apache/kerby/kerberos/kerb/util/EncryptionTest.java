/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.kerby.kerberos.kerb.util;

import org.apache.kerby.kerberos.kerb.KrbException;
import org.apache.kerby.kerberos.kerb.ccache.CredentialCache;
import org.apache.kerby.kerberos.kerb.crypto.EncryptionHandler;
import org.apache.kerby.kerberos.kerb.keytab.Keytab;
import org.apache.kerby.kerberos.kerb.spec.base.*;
import org.apache.kerby.kerberos.kerb.spec.ticket.EncTicketPart;
import org.apache.kerby.kerberos.kerb.spec.ticket.Ticket;
import org.apache.kerby.kerberos.kerb.KrbCodec;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/*
The principal keys for krbtgt/SH.INTEL.COM@SH.INTEL.COM

KVNO Principal
---- --------------------------------------------------------------------------
   2 krbtgt/SH.INTEL.COM@SH.INTEL.COM (des-cbc-crc)
   2 krbtgt/SH.INTEL.COM@SH.INTEL.COM (des3-cbc-raw)
   2 krbtgt/SH.INTEL.COM@SH.INTEL.COM (des-hmac-sha1)
   2 krbtgt/SH.INTEL.COM@SH.INTEL.COM (aes256-cts-hmac-sha1-96)
   2 krbtgt/SH.INTEL.COM@SH.INTEL.COM (aes128-cts-hmac-sha1-96)
   2 krbtgt/SH.INTEL.COM@SH.INTEL.COM (arcfour-hmac)
   2 krbtgt/SH.INTEL.COM@SH.INTEL.COM (camellia256-cts-cmac)
   2 krbtgt/SH.INTEL.COM@SH.INTEL.COM (camellia128-cts-cmac)
 */
public class EncryptionTest {

    private Keytab keytab;
    private CredentialCache cc;

    @Before
    public void setUp() throws IOException {
        InputStream kis = EncryptionTest.class.getResourceAsStream("/krbtgt.keytab");
        keytab = Keytab.loadKeytab(kis);
    }

    @Test
    public void testAes128() throws IOException, KrbException {
        testEncWith("aes128-cts-hmac-sha1-96.cc");
    }

    @Test
    public void testAes256() throws IOException, KrbException {
        if(!EncryptionHandler.isAES256Enabled()) {
            return;
        }

        testEncWith("aes256-cts-hmac-sha1-96.cc");
    }

    @Test
    public void testRc4() throws IOException, KrbException {
        testEncWith("arcfour-hmac.cc");
    }

    @Test
    public void testCamellia128() throws IOException, KrbException {
        testEncWith("camellia128-cts-cmac.cc");
    }

    @Test
    public void testCamellia256() throws IOException, KrbException {
        testEncWith("camellia256-cts-cmac.cc");
    }

    @Test
    public void testDesCbcCrc() throws IOException, KrbException {
        testEncWith("des-cbc-crc.cc");
    }

    @Test
    public void testDes3CbcSha1() throws IOException, KrbException {
        testEncWith("des3-cbc-sha1.cc");
    }

    private void testEncWith(String ccFile) throws IOException, KrbException, KrbException {
        InputStream cis = CcacheTest.class.getResourceAsStream("/" + ccFile);
        cc = new CredentialCache();
        cc.load(cis);

        Ticket ticket = getTicket();
        EncryptionType keyType = ticket.getEncryptedEncPart().getEType();
        EncryptionKey key = getServerKey(keyType);
        if (! EncryptionHandler.isImplemented(keyType)) {
            System.err.println("Key type not supported yet: " + keyType.getName());
            return;
        }

        byte[] decrypted = EncryptionHandler.decrypt(
                ticket.getEncryptedEncPart(), key, KeyUsage.KDC_REP_TICKET);
        assertThat(decrypted).isNotNull();

        EncTicketPart encPart = KrbCodec.decode(decrypted, EncTicketPart.class);
        assertThat(encPart).isNotNull();
        ticket.setEncPart(encPart);

        EncryptedData encrypted = EncryptionHandler.encrypt(
                decrypted, key, KeyUsage.KDC_REP_TICKET);

        byte[] decrypted2 = EncryptionHandler.decrypt(
                encrypted, key, KeyUsage.KDC_REP_TICKET);
        if (! Arrays.equals(decrypted, decrypted2)) {
            System.err.println("Encryption checking failed after decryption for key type: "
                    + keyType.getName());
        }
    }

    private EncryptionKey getServerKey(EncryptionType keyType) {
        return keytab.getKey(getServer(), keyType);
    }

    private PrincipalName getServer() {
        // only one, krbtgt/SH.INTEL.COM@SH.INTEL.COM
        List<PrincipalName> principals = keytab.getPrincipals();

        PrincipalName server = principals.get(0);

        return server;
    }

    private Ticket getTicket() {
        return cc.getCredentials().get(0).getTicket();
    }
}
