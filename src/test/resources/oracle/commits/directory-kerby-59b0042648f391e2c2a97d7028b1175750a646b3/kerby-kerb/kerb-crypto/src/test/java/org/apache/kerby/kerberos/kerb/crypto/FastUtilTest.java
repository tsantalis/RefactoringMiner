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

import org.apache.kerby.kerberos.kerb.crypto.fast.FastUtil;
import org.apache.kerby.kerberos.kerb.spec.base.EncryptionKey;
import org.apache.kerby.kerberos.kerb.spec.base.EncryptionType;
import org.apache.kerby.util.HexUtil;
import org.junit.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.fail;

public class FastUtilTest {
    static class TestCase {
        EncryptionType encType;
        String keyData1;
        String keyData2;
        String pepper1;
        String pepper2;
        String answer;
        TestCase(EncryptionType encType, String keyData1, String keyData2,
                 String pepper1, String pepper2, String answer) {
            this.encType = encType;
            this.keyData1 = keyData1;
            this.keyData2 = keyData2;
            this.pepper1 = pepper1;
            this.pepper2 = pepper2;
            this.answer = answer;
        }
    }

    @Test
    public void testFastUtil_DES_CBC_CRC() throws Exception {
        performTest(new TestCase(
                EncryptionType.DES_CBC_CRC,
                "key1",
                "key2",
                "a",
                "b",
                "43bae3738c9467e6"
        ));
    }


    @Test
    public void testFastUtil_DES_CBC_MD4() throws Exception {
        performTest(new TestCase(
                EncryptionType.DES_CBC_MD4,
                "key1",
                "key2",
                "a",
                "b",
                "43bae3738c9467e6"
        ));
    }

    @Test
    public void testFastUtil_DES_CBC_MD5() throws Exception {
        performTest(new TestCase(
                EncryptionType.DES_CBC_MD5,
                "key1",
                "key2",
                "a",
                "b",
                "43bae3738c9467e6"
        ));
    }


    @Test
    public void testFastUtil_CAMELLIA128_CTS_CMAC() throws Exception {
        performTest(new TestCase(
                EncryptionType.CAMELLIA128_CTS_CMAC,
                "key1",
                "key2",
                "a",
                "b",
                "403e44c30ee42525b8b4c8c379a4573c"
        ));
    }

    @Test
    public void testFastUtil_CAMELLIA256_CTS_CMAC() throws Exception {
        performTest(new TestCase(
                EncryptionType.CAMELLIA256_CTS_CMAC,
                "key1",
                "key2",
                "a",
                "b",
                "e0595b675a8b082b11b28c2ab9a94988fbc7ddc7ea29ecb5637ea25aff5134db"
        ));
    }

    @Test
    public void testFastUtil_AES128_CTS_HMAC_SHA1() throws Exception {
        performTest(new TestCase(
                EncryptionType.AES128_CTS_HMAC_SHA1_96,
                "key1",
                "key2",
                "a",
                "b",
                "97df97e4b798b29eb31ed7280287a92a"
        ));
    }

    @Test
    public void testFastUtil_AES256_CTS_HMAC_SHA1() throws Exception {
        performTest(new TestCase(
                EncryptionType.AES256_CTS_HMAC_SHA1_96,
                "key1",
                "key2",
                "a",
                "b",
                "4d6ca4e629785c1f01baf55e2e548566b9617ae3a96868c337cb93b5e72b1c7b"
        ));
    }


    @Test
    public void testFastUtil_DES3_CBC_SHA1() throws Exception {
        performTest(new TestCase(
                EncryptionType.DES3_CBC_SHA1,
                "key1",
                "key2",
                "a",
                "b",
                "e58f9eb643862c13ad38e529313462a7f73e62834fe54a01"
        ));
    }

    @Test
    public void testFastUtil_RC4_HMAC() throws Exception {
        performTest(new TestCase(
                EncryptionType.RC4_HMAC,
                "key1",
                "key2",
                "a",
                "b",
                "24d7f6b6bae4e5c00d2082c5ebab3672"
        ));
    }

    @Test
    public void testFastUtil_RC4_HMAC_EXP() throws Exception {
        performTest(new TestCase(
                EncryptionType.RC4_HMAC_EXP,
                "key1",
                "key2",
                "a",
                "b",
                "24d7f6b6bae4e5c00d2082c5ebab3672"
        ));
    }


    private static void performTest(TestCase testCase) throws Exception {
        EncryptionKey key, key1, key2;
        byte[] keyData1, keyData2;
        String pepper1, pepper2, answer;
        keyData1 = EncryptionHandler.getEncHandler(testCase.encType).str2key(testCase.keyData1, testCase.keyData1, null);
        key1 = new EncryptionKey(testCase.encType, keyData1);
        keyData2 = EncryptionHandler.getEncHandler(testCase.encType).str2key(testCase.keyData2, testCase.keyData2, null);
        key2 = new EncryptionKey(testCase.encType, keyData2);
        pepper1 = testCase.pepper1;
        pepper2 = testCase.pepper2;
        answer = testCase.answer;
        key = FastUtil.cf2(key1, pepper1, key2, pepper2);
        if (! Arrays.equals(key.getKeyData(), HexUtil.hex2bytes(answer))) {
            System.err.println("Failed with:");
            System.err.println("outKey:" + HexUtil.bytesToHex(key.getKeyData()));
            System.err.println("answer:" + testCase.answer);
            fail("CF2Test failed for " + testCase.encType.getName());
        }
    }
}
