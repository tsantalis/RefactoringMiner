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
package org.apache.kerby.kerberos.kerb.crypto;

import org.apache.kerby.kerberos.kerb.spec.base.EncryptionType;
import org.apache.kerby.util.HexUtil;
import org.junit.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.fail;

public class PrfTest {
    private static void performTest(TestCase testCase) throws Exception {
        byte[] keyData = EncryptionHandler.getEncHandler(testCase.encType).str2key(testCase.keyData, testCase.keyData, null);
        byte[] seed = HexUtil.hex2bytes(testCase.seed);
        byte[] answer = HexUtil.hex2bytes(testCase.answer);
        byte[] outkey = EncryptionHandler.getEncHandler(testCase.encType).prf(keyData, seed);

        if (! Arrays.equals(answer, outkey)) {
            System.err.println("failed with:");
            System.err.println("outKey:" + HexUtil.bytesToHex(outkey));
            System.err.println("answer:" + testCase.answer);
            fail("KeyDerive test failed for " + testCase.encType.getName());
        }
    }

    @Test
    public void testPrf_DES_CBC_CRC() throws Exception {
        performTest(new TestCase(
                EncryptionType.DES_CBC_CRC,
                "key1",
                "0161",
                "e91cff96b939270009308b073b66313e"
        ));
    }

    @Test
    public void testPrf_DES_CBC_MD4() throws Exception {
        performTest(new TestCase(
                EncryptionType.DES_CBC_MD4,
                "key1",
                "0161",
                "e91cff96b939270009308b073b66313e"
        ));
    }

    @Test
    public void testPrf_DES_CBC_MD5() throws Exception {
        performTest(new TestCase(
                EncryptionType.DES_CBC_MD5,
                "key1",
                "0161",
                "e91cff96b939270009308b073b66313e"
        ));
    }

    @Test
    public void testPrf_AES128_CTS_HMAC_SHA1() throws Exception {
        performTest(new TestCase(
                EncryptionType.AES128_CTS_HMAC_SHA1_96,
                "key1",
                "0161",
                "77b39a37a868920f2a51f9dd150c5717"
        ));
    }

    @Test
    public void testPrf_AES256_CTS_HMAC_SHA1() throws Exception {
        if(!EncryptionHandler.isAES256Enabled()) {
            System.out.println("AES256 is not supported in your jdk, pls update local_policy.jar,US_export_policy.jar from http://docs.oracle.com/javase/");
            return;
        }
        performTest(new TestCase(
                EncryptionType.AES256_CTS_HMAC_SHA1_96,
                "key1",
                "0161",
                "b2628c788e2e9c4a9bb4644678c29f2f"
        ));
    }

    @Test
    public void testPrf_DES3_CBC_SHA1() throws Exception {
        performTest(new TestCase(
                EncryptionType.DES3_CBC_SHA1,
                "key1",
                "0161",
                "bb6f4a7caa25fce1ee9baef36f1f9ee7"
        ));
    }

    @Test
    public void testPrf_CAMELLIA128_CTS_CMAC() throws Exception {
        performTest(new TestCase(
                EncryptionType.CAMELLIA128_CTS_CMAC,
                "key1",
                "0161",
                "e9bfccec1ec08740efcfdb020b48cf17"
        ));
    }

    @Test
    public void testPrf_CAMELLIA256_CTS_CMAC() throws Exception {
        performTest(new TestCase(
                EncryptionType.CAMELLIA256_CTS_CMAC,
                "key1",
                "0161",
                "d0bb1a19fd311388dc2eeb67268ff90b"
        ));
    }

    @Test
    public void testPrf_RC4_HMAC() throws Exception {
        performTest(new TestCase(
                EncryptionType.RC4_HMAC,
                "key1",
                "0161",
                "c882f5310c3b65e4b99c19709d986dedc154f234"
        ));
    }

    @Test
    public void testPrf_RC4_HMAC_EXP() throws Exception {
        performTest(new TestCase(
                EncryptionType.RC4_HMAC_EXP,
                "key1",
                "0161",
                "c882f5310c3b65e4b99c19709d986dedc154f234"
        ));
    }

    static class TestCase {
        EncryptionType encType;
        String keyData;
        String seed;
        String answer;
        TestCase(EncryptionType encType, String keyData,
                 String seed, String answer) {
            this.encType = encType;
            this.keyData = keyData;
            this.seed = seed;
            this.answer = answer;
        }

    }
}
