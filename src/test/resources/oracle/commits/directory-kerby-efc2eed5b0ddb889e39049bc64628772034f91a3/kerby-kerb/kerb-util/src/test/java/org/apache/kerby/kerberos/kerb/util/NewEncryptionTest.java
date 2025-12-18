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
import org.apache.kerby.kerberos.kerb.crypto.EncryptionHandler;
import org.apache.kerby.kerberos.kerb.spec.base.EncryptedData;
import org.apache.kerby.kerberos.kerb.spec.base.EncryptionKey;
import org.apache.kerby.kerberos.kerb.spec.base.EncryptionType;
import org.apache.kerby.kerberos.kerb.spec.base.KeyUsage;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assume.assumeTrue;

public class NewEncryptionTest {

    @Test
    public void testDesCbcCrc() throws IOException, KrbException {
        testEncWith(EncryptionType.DES_CBC_CRC);
    }

    @Test
    public void testDesCbcMd4() throws IOException, KrbException {
        testEncWith(EncryptionType.DES_CBC_MD4);
    }

    @Test
    public void testDesCbcMd5() throws IOException, KrbException {
        testEncWith(EncryptionType.DES_CBC_MD5);
    }

    @Test
    public void testCamellia128CtsCmac() throws IOException, KrbException {
        testEncWith(EncryptionType.CAMELLIA128_CTS_CMAC);
    }

    @Test
    public void testCamellia256CtsCmac() throws IOException, KrbException {
        testEncWith(EncryptionType.CAMELLIA256_CTS_CMAC);
    }

    @Test
    public void testAes128CtsHmacSha1() throws IOException, KrbException {
        testEncWith(EncryptionType.AES128_CTS_HMAC_SHA1_96);
    }

    @Test
    public void testAes256CtsHmacSha1() throws IOException, KrbException {
        assumeTrue(EncryptionHandler.isAES256Enabled());

        testEncWith(EncryptionType.AES256_CTS_HMAC_SHA1_96);
    }

    @Test
    public void testDes3CbcSha1() throws IOException, KrbException {
        testEncWith(EncryptionType.DES3_CBC_SHA1);
    }

    @Test
    public void testRc4Hmac() throws IOException, KrbException {
        testEncWith(EncryptionType.RC4_HMAC);
    }

    @Test
    public void testRc4HmacExp() throws IOException, KrbException {
        testEncWith(EncryptionType.RC4_HMAC_EXP);
    }

    /**
     * Decryption can leave a little trailing cruft. For the current cryptosystems, this can be up to 7 bytes.
     * @param inData
     * @param outData
     * @return
     */
    private boolean compareResult(byte[] inData, byte[] outData) {
        if (inData.length > outData.length || inData.length + 8 <= outData.length) {
            return false;
        }

        byte[] resultData = Arrays.copyOf(outData, inData.length);

        if (Arrays.equals(inData, resultData)) {
            return true;
        } else {
            return false;
        }
    }

    private void testEncWith(EncryptionType eType) throws KrbException {
        byte[] inData1 = "This is a test.\n".getBytes();
        byte[] inData2 = "This is another test.\n".getBytes();
        EncryptionKey key = EncryptionHandler.random2Key(eType);
        EncryptedData enctryData1 = EncryptionHandler.encrypt(inData1, key, KeyUsage.AD_ITE);
        EncryptedData enctryData2 = EncryptionHandler.encrypt(inData2, key, KeyUsage.AD_ITE);
        byte[] outData1 = EncryptionHandler.decrypt(enctryData1, key, KeyUsage.AD_ITE);
        byte[] outData2 = EncryptionHandler.decrypt(enctryData2, key, KeyUsage.AD_ITE);

        if (!compareResult(inData1, outData1)) {
            fail(eType + ": Failed, inData1 & outData1 are not the same!");
        }

        if (!compareResult(inData2, outData2)) {
            fail(eType + ": Failed, inData2 & outData2 are not the same!");
        }
    }
}
