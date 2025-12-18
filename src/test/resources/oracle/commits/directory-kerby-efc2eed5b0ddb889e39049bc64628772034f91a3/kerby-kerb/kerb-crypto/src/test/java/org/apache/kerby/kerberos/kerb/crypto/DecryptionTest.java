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

import org.apache.kerby.kerberos.kerb.spec.base.EncryptionKey;
import org.apache.kerby.kerberos.kerb.spec.base.EncryptionType;
import org.apache.kerby.kerberos.kerb.spec.base.KeyUsage;
import org.apache.kerby.util.HexUtil;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assume.assumeTrue;

/**
 * Decryption test with known ciphertexts.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DecryptionTest {
    /**
     * The class used to store the test values
     */
    private static class TestCase {
        EncryptionType encType;
        String plainText;
        int keyUsage;
        String key;
        String cipher;

        TestCase(EncryptionType encType, String plainText,
                 int keyUsage, String key, String cipher) {
            this.encType = encType;
            this.plainText = plainText;
            this.keyUsage = keyUsage;
            this.key = key;
            this.cipher = cipher;
        }
    }

    /**
     * Actually do the test
     */
    private boolean testDecrypt(TestCase testCase) throws Exception {
        KeyUsage ku = KeyUsage.fromValue(testCase.keyUsage);

        byte[] cipherBytes = HexUtil.hex2bytes(testCase.cipher);
        byte[] keyBytes = HexUtil.hex2bytes(testCase.key);

        EncryptionKey encKey = new EncryptionKey(testCase.encType, keyBytes);
        byte[] decrypted = EncryptionHandler.decrypt(cipherBytes, encKey, ku);
        String plainText = new String(decrypted);

        return plainText.startsWith(testCase.plainText);
    }

    /**
     * Perform all the checks for a testcase
     */
    private void performTestDecrypt(TestCase testCase) {
        //assertThat(EncryptionHandler.isImplemented(testCase.encType)).isTrue();
        if (! EncryptionHandler.isImplemented(testCase.encType)) {
            System.err.println("Not implemented yet: " + testCase.encType.getDisplayName());
            return;
        }

        try {
            assertThat(testDecrypt(testCase)).isTrue();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
     * Test for DES_CBC_CRC encryption type, with 0 byte
     */
    @Test
    public void testDecryptDES_CBC_CRC_0() {
        TestCase testCase = new TestCase(
                EncryptionType.DES_CBC_CRC,
                "", 0,
                "45E6087CDF138FB5",
                "28F6B09A012BCCF72FB05122B2839E6E");

        performTestDecrypt(testCase);
    }


    /**
     * Test for DES_CBC_CRC encryption type, with 1 byte
     */
    @Test
    public void testDecryptDES_CBC_CRC_1() {
        TestCase testCase = new TestCase(
                EncryptionType.DES_CBC_CRC,
                "1", 1,
                "92A7155810586B2F",
                "B4C871C2F3E7BF7605EFD62F2EEEC205");

        performTestDecrypt(testCase);
    }


    /**
     * Test for DES_CBC_CRC encryption type, with 9 bytes
     */
    @Test
    public void testDecryptDES_CBC_CRC_9() {
        TestCase testCase = new TestCase(
                EncryptionType.DES_CBC_CRC,
                "9 bytesss", 2,
                "A4B9514A61646423",
                "5F14C35178D33D7CDE0EC169C623CC83" +
                        "21B7B8BD34EA7EFE");

        performTestDecrypt(testCase);
    }


    /**
     * Test for DES_CBC_CRC encryption type, with 13 bytes
     */
    @Test
    public void testDecryptDES_CBC_CRC_13() {
        TestCase testCase = new TestCase(
                EncryptionType.DES_CBC_CRC,
                "13 bytes byte", 3,
                "2F16A2A7FDB05768",
                "0B588E38D971433C9D86D8BAEBF63E4C" +
                        "1A01666E76D8A54A3293F72679ED88C9");

        performTestDecrypt(testCase);
    }


    /**
     * Test for DES_CBC_CRC encryption type, with 30 bytes
     */
    @Test
    public void testDecryptDES_CBC_CRC_30() {
        TestCase testCase = new TestCase(
                EncryptionType.DES_CBC_CRC,
                "30 bytes bytes bytes bytes byt", 4,
                "BC8F70FD2097D67C",
                "38D632D2C20A7C2EA250FC8ECE42938E" +
                        "92A9F5D302502665C1A33729C1050DC2" +
                        "056298FBFB1682CEEB65E59204FDA7DF");

        performTestDecrypt(testCase);
    }


    /**
     * Test for DES_CBC_MD4 encryption type, with 0 byte
     */
    @Test
    public void testDecryptDES_CBC_MD4_0() {
        TestCase testCase = new TestCase(
                EncryptionType.DES_CBC_MD4,
                "", 0,
                "13EF45D0D6D9A15D",
                "1FB202BF07AF3047FB7801E588568686" +
                        "BA63D78BE3E87DC7");

        performTestDecrypt(testCase);
    }


    /**
     * Test for DES_CBC_MD4 encryption type, with 1 byte
     */
    @Test
    public void testDecryptDES_CBC_MD4_1() {
        TestCase testCase = new TestCase(
                EncryptionType.DES_CBC_MD4,
                "1", 1,
                "64688654DC269E67",
                "1F6CB9CECB73F755ABFDB3D565BD31D5" +
                        "A2E64BFE44C491E20EEBE5BD20E4D2A9");

        performTestDecrypt(testCase);
    }


    /**
     * Test for DES_CBC_MD4 encryption type, with 9 bytes
     */
    @Test
    public void testDecryptDES_CBC_MD4_9() {
        TestCase testCase = new TestCase(
                EncryptionType.DES_CBC_MD4,
                "9 bytesss", 2,
                "6804FB26DF8A4C32",
                "08A53D62FEC3338AD1D218E60DBDD3B2" +
                        "12940679D125E0621B3BAB4680CE0367" +
                        "6A2C420E9BE784EB");

        performTestDecrypt(testCase);
    }


    /**
     * Test for DES_CBC_MD4 encryption type, with 13 bytes
     */
    @Test
    public void testDecryptDES_CBC_MD4_13() {
        TestCase testCase = new TestCase(
                EncryptionType.DES_CBC_MD4,
                "13 bytes byte", 3,
                "234A436EC72FA80B",
                "17CD45E14FF06B2840A6036E9AA7A414" +
                        "4E29768144A0C1827D8C4BC7C9906E72" +
                        "CD4DC328F6648C99");

        performTestDecrypt(testCase);
    }


    /**
     * Test for DES_CBC_MD4 encryption type, with 30 bytes
     */
    @Test
    public void testDecryptDES_CBC_MD4_30()
    {
        TestCase testCase = new TestCase(
                EncryptionType.DES_CBC_MD4,
                "30 bytes bytes bytes bytes byt", 4,
                "1FD5F74334C4FB8C",
                "51134CD8951E9D57C0A36053E04CE03E" +
                        "CB8422488FDDC5C074C4D85E60A2AE42" +
                        "3C3C701201314F362CB07448091679C6" +
                        "A496C11D7B93C71B");

        performTestDecrypt(testCase);
    }


    /**
     * Test for DES_CBC_MD5 encryption type, with 0 byte
     */
    @Test
    public void testDecryptDES_CBC_MD5_0()
    {
        TestCase testCase = new TestCase(
                EncryptionType.DES_CBC_MD5,
                "", 0,
                "4A545E0BF7A22631",
                "784CD81591A034BE82556F56DCA3224B" +
                        "62D9956FA90B1B93");

        performTestDecrypt(testCase);
    }


    /**
     * Test for DES_CBC_MD5 encryption type, with 1 byte
     */
    @Test
    public void testDecryptDES_CBC_MD5_1()
    {
        TestCase testCase = new TestCase(
                EncryptionType.DES_CBC_MD5,
                "1", 1,
                "D5804A269DC4E645",
                "FFA25C7BE287596BFE58126E90AAA0F1" +
                        "2D9A82A0D86DF6D5F9074B6B399E7FF1");

        performTestDecrypt(testCase);
    }


    /**
     * Test for DES_CBC_MD5 encryption type, with 9 bytes
     */
    @Test
    public void testDecryptDES_CBC_MD5_9()
    {
        TestCase testCase = new TestCase(
                EncryptionType.DES_CBC_MD5,
                "9 bytesss", 2,
                "C8312F7F83EA4640",
                "E7850337F2CC5E3F35CE3D69E2C32986" +
                        "38A7AA44B878031E39851E47C15B5D0E" +
                        "E7E7AC54DE111D80");

        performTestDecrypt(testCase);
    }


    /**
     * Test for DES_CBC_MD5 encryption type, with 13 bytes
     */
    @Test
    public void testDecryptDES_CBC_MD5_13()
    {
        TestCase testCase = new TestCase(
                EncryptionType.DES_CBC_MD5,
                "13 bytes byte", 3,
                "7FDA3E62AD8AF18C",
                "D7A8032E19994C928777506595FBDA98" +
                        "83158A8514548E296E911C29F465C672" +
                        "366000558BFC2E88");

        performTestDecrypt(testCase);
    }


    /**
     * Test for DES_CBC_MD5 encryption type, with 30 bytes
     */
    @Test
    public void testDecryptDES_CBC_MD5_30()
    {
        TestCase testCase = new TestCase(
                EncryptionType.DES_CBC_MD5,
                "30 bytes bytes bytes bytes byt", 4,
                "D3D6832970A73752",
                "8A48166A4C6FEAE607A8CF68B381C075" +
                        "5E402B19DBC0F81A7D7CA19A25E05223" +
                        "F6064409BF5A4F50ACD826639FFA7673" +
                        "FD324EC19E429502");

        performTestDecrypt(testCase);
    }


    /**
     * Test for DES_CBC_SHA1 encryption type, with 0 byte
     */
    @Test
    public void testDecryptDES_CBC_SHA1_0()
    {
        TestCase testCase = new TestCase(
                EncryptionType.DES3_CBC_SHA1,
                "", 0,
                "7A25DF8992296DCEDA0E135BC4046E23" +
                        "75B3C14C98FBC162",
                "548AF4D504F7D723303F12175FE8386B" +
                        "7B5335A967BAD61F3BF0B143");

        performTestDecrypt(testCase);
    }


    /**
     * Test for DES_CBC_SHA1 encryption type, with 1 byte
     */
    @Test
    public void testDecryptDES_CBC_SHA1_1()
    {
        TestCase testCase = new TestCase(
                EncryptionType.DES3_CBC_SHA1,
                "1", 1,
                "BC0783891513D5CE57BC138FD3C11AE6" +
                        "40452385322962B6",
                "9C3C1DBA4747D85AF2916E4745F2DCE3" +
                        "8046796E5104BCCDFB669A91D44BC356" +
                        "660945C7");

        performTestDecrypt(testCase);
    }


    /**
     * Test for DES_CBC_SHA1 encryption type, with 9 bytes
     */
    @Test
    public void testDecryptDES_CBC_SHA1_9()
    {
        TestCase testCase = new TestCase(
                EncryptionType.DES3_CBC_SHA1,
                "9 bytesss", 2,
                "2FD0F725CE04100D2FC8A18098831F85" +
                        "0B45D9EF850BD920",
                "CF9144EBC8697981075A8BAD8D74E5D7" +
                        "D591EB7D9770C7ADA25EE8C5B3D69444" +
                        "DFEC79A5B7A01482D9AF74E6");

        performTestDecrypt(testCase);
    }


    /**
     * Test for DES_CBC_SHA1 encryption type, with 13 bytes
     */
    @Test
    public void testDecryptDES_CBC_SHA1_13()
    {
        TestCase testCase = new TestCase(
                EncryptionType.DES3_CBC_SHA1,
                "13 bytes byte", 3,
                "0DD52094E0F41CECCB5BE510A764B351" +
                        "76E3981332F1E598",
                "839A17081ECBAFBCDC91B88C6955DD3C" +
                        "4514023CF177B77BF0D0177A16F705E8" +
                        "49CB7781D76A316B193F8D30");

        performTestDecrypt(testCase);
    }


    /**
     * Test for DES_CBC_SHA1 encryption type, with 30 bytes
     */
    @Test
    public void testDecryptDES_CBC_SHA1_30()
    {
        TestCase testCase = new TestCase(
                EncryptionType.DES3_CBC_SHA1,
                "30 bytes bytes bytes bytes byt", 4,
                "F11686CBBC9E23EA54FECD2A3DCDFB20" +
                        "B6FE98BF2645C4C4",
                "89433E83FD0EA3666CFFCD18D8DEEBC5" +
                        "3B9A34EDBEB159D9F667C6C2B9A96440" +
                        "1D55E7E9C68D648D65C3AA84FFA3790C" +
                        "14A864DA8073A9A95C4BA2BC");

        performTestDecrypt(testCase);
    }


    /**
     * Test for ARCFOUR_HMAC encryption type, with 0 byte
     */
    @Test
    public void testDecryptARC_FOUR_0()
    {
        TestCase testCase = new TestCase(
                EncryptionType.ARCFOUR_HMAC,
                "", 0,
                "F81FEC39255F5784E850C4377C88BD85",
                "02C1EB15586144122EC717763DD348BF" +
                        "00434DDC6585954C"
        );

        performTestDecrypt(testCase);
    }


    /**
     * Test for ARCFOUR_HMAC encryption type, with 1 byte
     */
    @Test
    public void testDecryptARC_FOUR_1()
    {
        TestCase testCase = new TestCase(
                EncryptionType.ARCFOUR_HMAC,
                "1", 1,
                "67D1300D281223867F9647FF48721273",
                "6156E0CC04E0A0874F9FDA008F498A7A" +
                        "DBBC80B70B14DDDBC0"
        );

        performTestDecrypt(testCase);
    }


    /**
     * Test for ARCFOUR_HMAC encryption type, with 9 bytes
     */
    @Test
    public void testDecryptARC_FOUR_9()
    {
        TestCase testCase = new TestCase(
                EncryptionType.ARCFOUR_HMAC,
                "9 bytesss", 2,
                "3E40AB6093695281B3AC1A9304224D98",
                "0F9AD121D99D4A09448E4F1F718C4F5C" +
                        "BE6096262C66F29DF232A87C9F98755D" +
                        "55"
        );

        performTestDecrypt(testCase);
    }


    /**
     * Test for ARCFOUR_HMAC encryption type, with 13 bytes
     */
    @Test
    public void testDecryptARC_FOUR_13()
    {
        TestCase testCase = new TestCase(
                EncryptionType.ARCFOUR_HMAC,
                "13 bytes byte", 3,
                "4BA2FBF0379FAED87A254D3B353D5A7E",
                "612C57568B17A70352BAE8CF26FB9459" +
                        "A6F3353CD35FD439DB3107CBEC765D32" +
                        "6DFC04C1DD"
        );

        performTestDecrypt(testCase);
    }


    /**
     * Test for ARCFOUR_HMAC encryption type, with 30 bytes
     */
    @Test
    public void testDecryptARC_FOUR_30()
    {
        TestCase testCase = new TestCase(
                EncryptionType.ARCFOUR_HMAC,
                "30 bytes bytes bytes bytes byt", 4,
                "68F263DB3FCE15D031C9EAB02D67107A",
                "95F9047C3AD75891C2E9B04B16566DC8" +
                        "B6EB9CE4231AFB2542EF87A7B5A0F260" +
                        "A99F0460508DE0CECC632D07C354124E" +
                        "46C5D2234EB8"
        );

        performTestDecrypt(testCase);
    }


    /**
     * Test for ARCFOUR_HMAC_EXP encryption type, with 0 byte
     */
    @Test
    public void testDecryptARCFOUR_HMAC_EXP_0() {
        TestCase testCase = new TestCase(
                EncryptionType.ARCFOUR_HMAC_EXP,
                "", 0,
                "F7D3A155AF5E238A0B7A871A96BA2AB2",
                "2827F0E90F62E7460C4E2FB39F9657BA" +
                        "8BFAA991D7FDADFF"
        );

        performTestDecrypt(testCase);
    }


    /**
     * Test for ARCFOUR_HMAC encryption type, with 1 byte
     */
    @Test
    public void testDecryptARCFOUR_HMAC_EXP_1() {
        TestCase testCase = new TestCase(
                EncryptionType.ARCFOUR_HMAC_EXP,
                "1", 1,
                "DEEAA0607DB799E2FDD6DB2986BB8D65",
                "3DDA392E2E275A4D75183FA6328A0A4E" +
                        "6B752DF6CD2A25FA4E"
        );

        performTestDecrypt(testCase);
    }


    /**
     * Test for ARCFOUR_HMAC encryption type, with 9 bytes
     */
    @Test
    public void testDecryptARCFOUR_HMAC_EXP_9() {
        TestCase testCase = new TestCase(
                EncryptionType.ARCFOUR_HMAC_EXP,
                "9 bytesss", 2,
                "33AD7FC2678615569B2B09836E0A3AB6",
                "09D136AC485D92644EC6701D6A0D03E8" +
                        "982D7A3CA7EFD0F8F4F83660EF4277BB" +
                        "81"
        );

        performTestDecrypt(testCase);
    }


    /**
     * Test for ARCFOUR_HMAC encryption type, with 13 bytes
     */
    @Test
    public void testDecryptARCFOUR_HMAC_EXP_13() {
        TestCase testCase = new TestCase(
                EncryptionType.ARCFOUR_HMAC_EXP,
                "13 bytes byte", 3,
                "39F25CD4F0D41B2B2D9D300FCB2981CB",
                "912388D7C07612819E3B640FF5CECDAF" +
                        "72E5A59DF10F1091A6BEC39CAAD748AF" +
                        "9BD2D8D546"
        );

        performTestDecrypt(testCase);
    }


    /**
     * Test for ARCFOUR_HMAC encryption type, with 30 bytes
     */
    @Test
    public void testDecryptARCFOUR_HMAC_EXP_30() {
        TestCase testCase = new TestCase(
                EncryptionType.ARCFOUR_HMAC_EXP,
                "30 bytes bytes bytes bytes byt", 4,
                "9F725542D9F72AA1F386CBE7896984FC",
                "78B35A08B08BE265AEB4145F076513B6" +
                        "B56EFED3F7526574AF74F7D2F9BAE96E" +
                        "ABB76F2D87386D2E93E3A77B99919F1D" +
                        "976490E2BD45"
        );

        performTestDecrypt(testCase);
    }


    /**
     * Test for AES128_CTS_HMAC_SHA1_96 encryption type, with 0 byte
     */
    @Test
    public void testDecryptAES128_CTS_HMAC_SHA1_96_0() {
        TestCase testCase = new TestCase(
                EncryptionType.AES128_CTS_HMAC_SHA1_96,
                "", 0,
                "5A5C0F0BA54F3828B2195E66CA24A289",
                "49FF8E11C173D9583A3254FBE7B1F1DF" +
                        "36C538E8416784A1672E6676"
        );

        performTestDecrypt(testCase);
    }


    /**
     * Test for AES128_CTS_HMAC_SHA1_96 encryption type, with 1 byte
     */
    @Test
    public void testDecryptAES128_CTS_HMAC_SHA1_96_1() {
        TestCase testCase = new TestCase(
                EncryptionType.AES128_CTS_HMAC_SHA1_96,
                "1", 1,
                "98450E3F3BAA13F5C99BEB936981B06F",
                "F86742F537B35DC2174A4DBAA920FAF9" +
                        "042090B065E1EBB1CAD9A65394"
        );

        performTestDecrypt(testCase);
    }


    /**
     * Test for AES128_CTS_HMAC_SHA1_96 encryption type, with 9 bytes
     */
    @Test
    public void testDecryptAES128_CTS_HMAC_SHA1_96_9() {
        TestCase testCase = new TestCase(
                EncryptionType.AES128_CTS_HMAC_SHA1_96,
                "9 bytesss", 2,
                "9062430C8CDA3388922E6D6A509F5B7A",
                "68FB9679601F45C78857B2BF820FD6E5" +
                        "3ECA8D42FD4B1D7024A09205ABB7CD2E" +
                        "C26C355D2F"
        );

        performTestDecrypt(testCase);
    }


    /**
     * Test for AES128_CTS_HMAC_SHA1_96 encryption type, with 13 bytes
     */
    @Test
    public void testDecryptAES128_CTS_HMAC_SHA1_96_13() {
        TestCase testCase = new TestCase(
                EncryptionType.AES128_CTS_HMAC_SHA1_96,
                "13 bytes byte", 3,
                "033EE6502C54FD23E27791E987983827",
                "EC366D0327A933BF49330E650E49BC6B" +
                        "974637FE80BF532FE51795B4809718E6" +
                        "194724DB948D1FD637"
        );

        performTestDecrypt(testCase);
    }


    /**
     * Test for AES128_CTS_HMAC_SHA1_96 encryption type, with 30 bytes
     */
    @Test
    public void testDecryptAES128_CTS_HMAC_SHA1_96_30() {
        TestCase testCase = new TestCase(
                EncryptionType.AES128_CTS_HMAC_SHA1_96,
                "30 bytes bytes bytes bytes byt", 4,
                "DCEEB70B3DE76562E689226C76429148",
                "C96081032D5D8EEB7E32B4089F789D0F" +
                        "AA481DEA74C0F97CBF3146DDFCF8E800" +
                        "156ECB532FC203E30FF600B63B350939" +
                        "FECE510F02D7FF1E7BAC"
        );

        performTestDecrypt(testCase);
    }


    /**
     * Test for AES256_CTS_HMAC_SHA1_96 encryption type, with 0 byte
     */
    @Test
    public void testDecryptAES256_CTS_HMAC_SHA1_96_0() {
        assumeTrue(EncryptionHandler.isAES256Enabled());

        TestCase testCase = new TestCase(
                EncryptionType.AES256_CTS_HMAC_SHA1_96,
                "", 0,
                "17F275F2954F2ED1F90C377BA7F4D6A3" +
                        "69AA0136E0BF0C927AD6133C693759A9",
                "E5094C55EE7B38262E2B044280B06937" +
                        "9A95BF95BD8376FB3281B435"
        );

        performTestDecrypt(testCase);
    }


    /**
     * Test for AES256_CTS_HMAC_SHA1_96 encryption type, with 1 byte
     */
    @Test
    public void testDecryptAES256_CTS_HMAC_SHA1_96_1() {
        assumeTrue(EncryptionHandler.isAES256Enabled());

        TestCase testCase = new TestCase(
                EncryptionType.AES256_CTS_HMAC_SHA1_96,
                "1", 1,
                "B9477E1FF0329C0050E20CE6C72D2DFF" +
                        "27E8FE541AB0954429A9CB5B4F7B1E2A",
                "406150B97AEB76D43B36B62CC1ECDFBE" +
                        "6F40E95755E0BEB5C27825F3A4"
        );

        performTestDecrypt(testCase);
    }


    /**
     * Test for AES256_CTS_HMAC_SHA1_96 encryption type, with 9 bytes
     */
    @Test
    public void testDecryptAES256_CTS_HMAC_SHA1_96_9() {
        assumeTrue(EncryptionHandler.isAES256Enabled());

        TestCase testCase = new TestCase(
                EncryptionType.AES256_CTS_HMAC_SHA1_96,
                "9 bytesss", 2,
                "B1AE4CD8462AFF1677053CC9279AAC30" +
                        "B796FB81CE21474DD3DDBCFEA4EC76D7",
                "09957AA25FCAF88F7B39E4406E633012" +
                        "D5FEA21853F6478DA7065CAEF41FD454" +
                        "A40824EEC5"
        );

        performTestDecrypt(testCase);
    }


    /**
     * Test for AES256_CTS_HMAC_SHA1_96 encryption type, with 13 bytes
     */
    @Test
    public void testDecryptAES256_CTS_HMAC_SHA1_96_13() {
        assumeTrue(EncryptionHandler.isAES256Enabled());

        TestCase testCase = new TestCase(
                EncryptionType.AES256_CTS_HMAC_SHA1_96,
                "13 bytes byte", 3,
                "E5A72BE9B7926C1225BAFEF9C1872E7B" +
                        "A4CDB2B17893D84ABD90ACDD8764D966",
                "D8F1AAFEEC84587CC3E700A774E56651" +
                        "A6D693E174EC4473B5E6D96F80297A65" +
                        "3FB818AD893E719F96"
        );

        performTestDecrypt(testCase);
    }


    /**
     * Test for AES256_CTS_HMAC_SHA1_96 encryption type, with 30 bytes
     */
    @Test
    public void testDecryptAES256_CTS_HMAC_SHA1_96_30() {
        assumeTrue(EncryptionHandler.isAES256Enabled());

        TestCase testCase = new TestCase(
                EncryptionType.AES256_CTS_HMAC_SHA1_96,
                "30 bytes bytes bytes bytes byt", 4,
                "F1C795E9248A09338D82C3F8D5B56704" +
                        "0B0110736845041347235B1404231398",
                "D1137A4D634CFECE924DBC3BF6790648" +
                        "BD5CFF7DE0E7B99460211D0DAEF3D79A" +
                        "295C688858F3B34B9CBD6EEBAE81DAF6" +
                        "B734D4D498B6714F1C1D"
        );

        performTestDecrypt(testCase);
    }


    /**
     * Test for CAMELLIA128_CTS_CMAC encryption type, with 0 byte
     */
    @Test
    public void testDecryptCAMELIA128_CTS_CMAC_0() {
        TestCase testCase = new TestCase(
                EncryptionType.CAMELLIA128_CTS_CMAC,
                "", 0,
                "1DC46A8D763F4F93742BCBA3387576C3",
                "C466F1871069921EDB7C6FDE244A52DB" +
                        "0BA10EDC197BDB8006658CA3CCCE6EB8"
        );

        performTestDecrypt(testCase);
    }


    /**
     * Test for CAMELLIA128_CTS_CMAC encryption type, with 1 byte
     */
    @Test
    public void testDecryptCAMELIA128_CTS_CMAC_1() {
        TestCase testCase = new TestCase(
                EncryptionType.CAMELLIA128_CTS_CMAC,
                "1", 1,
                "5027BC231D0F3A9D23333F1CA6FDBE7C",
                "842D21FD950311C0DD464A3F4BE8D6DA" +
                        "88A56D559C9B47D3F9A85067AF661559" +
                        "B8"
        );

        performTestDecrypt(testCase);
    }


    /**
     * Test for CAMELLIA128_CTS_CMAC encryption type, with 9 bytes
     */
    @Test
    public void testDecryptCAMELIA128_CTS_CMAC_9() {
        TestCase testCase = new TestCase(
                EncryptionType.CAMELLIA128_CTS_CMAC,
                "9 bytesss", 2,
                "A1BB61E805F9BA6DDE8FDBDDC05CDEA0",
                "619FF072E36286FF0A28DEB3A352EC0D" +
                        "0EDF5C5160D663C901758CCF9D1ED33D" +
                        "71DB8F23AABF8348A0"
        );

        performTestDecrypt(testCase);
    }


    /**
     * Test for CAMELLIA128_CTS_CMAC encryption type, with 13 bytes
     */
    @Test
    public void testDecryptCAMELIA128_CTS_CMAC_13() {
        TestCase testCase = new TestCase(
                EncryptionType.CAMELLIA128_CTS_CMAC,
                "13 bytes byte", 3,
                "2CA27A5FAF5532244506434E1CEF6676",
                "B8ECA3167AE6315512E59F98A7C50020" +
                        "5E5F63FF3BB389AF1C41A21D640D8615" +
                        "C9ED3FBEB05AB6ACB67689B5EA"
        );

        performTestDecrypt(testCase);
    }


    /**
     * Test for CAMELLIA128_CTS_CMAC encryption type, with 30 bytes
     */
    @Test
    public void testDecryptCAMELIA128_CTS_CMAC_30() {
        TestCase testCase = new TestCase(
                EncryptionType.CAMELLIA128_CTS_CMAC,
                "30 bytes bytes bytes bytes byt", 4,
                "7824F8C16F83FF354C6BF7515B973F43",
                "A26A3905A4FFD5816B7B1E27380D0809" +
                        "0C8EC1F304496E1ABDCD2BDCD1DFFC66" +
                        "0989E117A713DDBB57A4146C1587CBA4" +
                        "356665591D2240282F5842B105A5"
        );

        performTestDecrypt(testCase);
    }


    /**
     * Test for CAMELLIA256_CTS_CMAC encryption type, with 0 byte
     */
    @Test
    public void testDecryptCAMELIA256_CTS_CMAC_0() {
        TestCase testCase = new TestCase(
                EncryptionType.CAMELLIA256_CTS_CMAC,
                "", 0,
                "B61C86CC4E5D2757545AD423399FB703" +
                        "1ECAB913CBB900BD7A3C6DD8BF92015B",
                "03886D03310B47A6D8F06D7B94D1DD83" +
                        "7ECCE315EF652AFF620859D94A259266"
        );

        performTestDecrypt(testCase);
    }


    /**
     * Test for CAMELLIA256_CTS_CMAC encryption type, with 1 byte
     */
    @Test
    public void testDecryptCAMELIA256_CTS_CMAC_1() {
        TestCase testCase = new TestCase(
                EncryptionType.CAMELLIA256_CTS_CMAC,
                "1", 1,
                "1B97FE0A190E2021EB30753E1B6E1E77" +
                        "B0754B1D684610355864104963463833",
                "2C9C1570133C99BF6A34BC1B0212002F" +
                        "D194338749DB4135497A347CFCD9D18A12"
        );

        performTestDecrypt(testCase);
    }


    /**
     * Test for CAMELLIA256_CTS_CMAC encryption type, with 9 bytes
     */
    @Test
    public void testDecryptCAMELIA256_CTS_CMAC_9() {
        TestCase testCase = new TestCase(
                EncryptionType.CAMELLIA256_CTS_CMAC,
                "9 bytesss", 2,
                "32164C5B434D1D1538E4CFD9BE8040FE" +
                        "8C4AC7ACC4B93D3314D2133668147A05",
                "9C6DE75F812DE7ED0D28B2963557A115" +
                        "640998275B0AF5152709913FF52A2A9C" +
                        "8E63B872F92E64C839"
        );

        performTestDecrypt(testCase);
    }


    /**
     * Test for CAMELLIA256_CTS_CMAC encryption type, with 13 bytes
     */
    @Test
    public void testDecryptCAMELIA256_CTS_CMAC_13() {
        TestCase testCase = new TestCase(
                EncryptionType.CAMELLIA256_CTS_CMAC,
                "13 bytes byte", 3,
                "B038B132CD8E06612267FAB7170066D8" +
                        "8AECCBA0B744BFC60DC89BCA182D0715",
                "EEEC85A9813CDC536772AB9B42DEFC57" +
                        "06F726E975DDE05A87EB5406EA324CA1" +
                        "85C9986B42AABE794B84821BEE"
        );

        performTestDecrypt(testCase);
    }


    /**
     * Test for CAMELLIA256_CTS_CMAC encryption type, with 30 bytes
     */
    @Test
    public void testDecryptCAMELIA256_CTS_CMAC_30() {
        TestCase testCase = new TestCase(
                EncryptionType.CAMELLIA256_CTS_CMAC,
                "30 bytes bytes bytes bytes byt", 4,
                "CCFCD349BF4C6677E86E4B02B8EAB924" +
                        "A546AC731CF9BF6989B996E7D6BFBBA7",
                "0E44680985855F2D1F1812529CA83BFD" +
                        "8E349DE6FD9ADA0BAAA048D68E265FEB" +
                        "F34AD1255A344999AD37146887A6C684" +
                        "5731AC7F46376A0504CD06571474"
        );

        performTestDecrypt(testCase);
    }
}
