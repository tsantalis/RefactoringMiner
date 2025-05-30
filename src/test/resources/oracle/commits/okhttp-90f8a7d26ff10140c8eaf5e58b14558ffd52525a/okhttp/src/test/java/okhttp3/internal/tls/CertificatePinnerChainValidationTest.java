/*
 * Copyright (C) 2016 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package okhttp3.internal.tls;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Collections;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.SocketPolicy;
import okhttp3.Call;
import okhttp3.CertificatePinner;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClientTestRule;
import okhttp3.RecordingHostnameVerifier;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.platform.Platform;
import okhttp3.testing.Flaky;
import okhttp3.testing.PlatformRule;
import okhttp3.tls.HandshakeCertificates;
import okhttp3.tls.HeldCertificate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static okhttp3.tls.internal.TlsUtil.newKeyManager;
import static okhttp3.tls.internal.TlsUtil.newTrustManager;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public final class CertificatePinnerChainValidationTest {
  @RegisterExtension PlatformRule platform = new PlatformRule();
  @RegisterExtension OkHttpClientTestRule clientTestRule = new OkHttpClientTestRule();

  private MockWebServer server;

  @BeforeEach
  public void setup(MockWebServer server) {
    this.server = server;
    platform.assumeNotBouncyCastle();
  }

  /** The pinner should pull the root certificate from the trust manager. */
  @Test public void pinRootNotPresentInChain() throws Exception {
    // Fails on 11.0.1 https://github.com/square/okhttp/issues/4703

    HeldCertificate rootCa = new HeldCertificate.Builder()
        .serialNumber(1L)
        .certificateAuthority(1)
        .commonName("root")
        .build();
    HeldCertificate intermediateCa = new HeldCertificate.Builder()
        .signedBy(rootCa)
        .certificateAuthority(0)
        .serialNumber(2L)
        .commonName("intermediate_ca")
        .build();
    HeldCertificate certificate = new HeldCertificate.Builder()
        .signedBy(intermediateCa)
        .serialNumber(3L)
        .commonName(server.getHostName())
        .build();
    CertificatePinner certificatePinner = new CertificatePinner.Builder()
        .add(server.getHostName(), CertificatePinner.pin(rootCa.certificate()))
        .build();
    HandshakeCertificates handshakeCertificates = new HandshakeCertificates.Builder()
        .addTrustedCertificate(rootCa.certificate())
        .build();
    OkHttpClient client = clientTestRule.newClientBuilder()
        .sslSocketFactory(
            handshakeCertificates.sslSocketFactory(), handshakeCertificates.trustManager())
        .hostnameVerifier(new RecordingHostnameVerifier())
        .certificatePinner(certificatePinner)
        .build();

    HandshakeCertificates serverHandshakeCertificates = new HandshakeCertificates.Builder()
        .heldCertificate(certificate, intermediateCa.certificate())
        .build();
    server.useHttps(serverHandshakeCertificates.sslSocketFactory(), false);

    // The request should complete successfully.
    server.enqueue(new MockResponse()
        .setBody("abc"));
    Call call1 = client.newCall(new Request.Builder()
        .url(server.url("/"))
        .build());
    Response response1 = call1.execute();
    assertThat(response1.body().string()).isEqualTo("abc");
  }

  /** The pinner should accept an intermediate from the server's chain. */
  @Test public void pinIntermediatePresentInChain() throws Exception {
    // Fails on 11.0.1 https://github.com/square/okhttp/issues/4703

    HeldCertificate rootCa = new HeldCertificate.Builder()
        .serialNumber(1L)
        .certificateAuthority(1)
        .commonName("root")
        .build();
    HeldCertificate intermediateCa = new HeldCertificate.Builder()
        .signedBy(rootCa)
        .certificateAuthority(0)
        .serialNumber(2L)
        .commonName("intermediate_ca")
        .build();
    HeldCertificate certificate = new HeldCertificate.Builder()
        .signedBy(intermediateCa)
        .serialNumber(3L)
        .commonName(server.getHostName())
        .build();
    CertificatePinner certificatePinner = new CertificatePinner.Builder()
        .add(server.getHostName(), CertificatePinner.pin(intermediateCa.certificate()))
        .build();
    HandshakeCertificates handshakeCertificates = new HandshakeCertificates.Builder()
        .addTrustedCertificate(rootCa.certificate())
        .build();
    OkHttpClient client = clientTestRule.newClientBuilder()
        .sslSocketFactory(
            handshakeCertificates.sslSocketFactory(), handshakeCertificates.trustManager())
        .hostnameVerifier(new RecordingHostnameVerifier())
        .certificatePinner(certificatePinner)
        .build();

    HandshakeCertificates serverHandshakeCertificates = new HandshakeCertificates.Builder()
        .heldCertificate(certificate, intermediateCa.certificate())
        .build();
    server.useHttps(serverHandshakeCertificates.sslSocketFactory(), false);

    // The request should complete successfully.
    server.enqueue(new MockResponse()
        .setBody("abc")
        .setSocketPolicy(SocketPolicy.DISCONNECT_AT_END));
    Call call1 = client.newCall(new Request.Builder()
        .url(server.url("/"))
        .build());
    Response response1 = call1.execute();
    assertThat(response1.body().string()).isEqualTo("abc");
    response1.close();

    // Force a fresh connection for the next request.
    client.connectionPool().evictAll();

    // Confirm that a second request also succeeds. This should detect caching problems.
    server.enqueue(new MockResponse()
        .setBody("def")
        .setSocketPolicy(SocketPolicy.DISCONNECT_AT_END));
    Call call2 = client.newCall(new Request.Builder()
        .url(server.url("/"))
        .build());
    Response response2 = call2.execute();
    assertThat(response2.body().string()).isEqualTo("def");
    response2.close();
  }

  @Test public void unrelatedPinnedLeafCertificateInChain() throws Exception {
    // https://github.com/square/okhttp/issues/4729
    platform.expectFailureOnConscryptPlatform();
    platform.expectFailureOnCorrettoPlatform();

    // Start with a trusted root CA certificate.
    HeldCertificate rootCa = new HeldCertificate.Builder()
        .serialNumber(1L)
        .certificateAuthority(1)
        .commonName("root")
        .build();

    // Add a good intermediate CA, and have that issue a good certificate to localhost. Prepare an
    // SSL context for an HTTP client under attack. It includes the trusted CA and a pinned
    // certificate.
    HeldCertificate goodIntermediateCa = new HeldCertificate.Builder()
        .signedBy(rootCa)
        .certificateAuthority(0)
        .serialNumber(2L)
        .commonName("good_intermediate_ca")
        .build();
    HeldCertificate goodCertificate = new HeldCertificate.Builder()
        .signedBy(goodIntermediateCa)
        .serialNumber(3L)
        .commonName(server.getHostName())
        .build();
    CertificatePinner certificatePinner = new CertificatePinner.Builder()
        .add(server.getHostName(), CertificatePinner.pin(goodCertificate.certificate()))
        .build();
    HandshakeCertificates handshakeCertificates = new HandshakeCertificates.Builder()
        .addTrustedCertificate(rootCa.certificate())
        .build();
    OkHttpClient client = clientTestRule.newClientBuilder()
        .sslSocketFactory(
            handshakeCertificates.sslSocketFactory(), handshakeCertificates.trustManager())
        .hostnameVerifier(new RecordingHostnameVerifier())
        .certificatePinner(certificatePinner)
        .build();

    // Add a bad intermediate CA and have that issue a rogue certificate for localhost. Prepare
    // an SSL context for an attacking webserver. It includes both these rogue certificates plus the
    // trusted good certificate above. The attack is that by including the good certificate in the
    // chain, we may trick the certificate pinner into accepting the rouge certificate.
    HeldCertificate compromisedIntermediateCa = new HeldCertificate.Builder()
        .signedBy(rootCa)
        .certificateAuthority(0)
        .serialNumber(4L)
        .commonName("bad_intermediate_ca")
        .build();
    HeldCertificate rogueCertificate = new HeldCertificate.Builder()
        .serialNumber(5L)
        .signedBy(compromisedIntermediateCa)
        .commonName(server.getHostName())
        .build();

    SSLSocketFactory socketFactory = newServerSocketFactory(rogueCertificate,
        compromisedIntermediateCa.certificate(), goodCertificate.certificate());

    server.useHttps(socketFactory, false);
    server.enqueue(new MockResponse()
        .setBody("abc")
        .addHeader("Content-Type: text/plain"));

    // Make a request from client to server. It should succeed certificate checks (unfortunately the
    // rogue CA is trusted) but it should fail certificate pinning.
    Request request = new Request.Builder()
        .url(server.url("/"))
        .build();
    Call call = client.newCall(request);
    try {
      call.execute();
      fail();
    } catch (SSLPeerUnverifiedException expected) {
      // Certificate pinning fails!
      String message = expected.getMessage();
      assertThat(message).startsWith("Certificate pinning failure!");
    }
  }

  @Test public void unrelatedPinnedIntermediateCertificateInChain() throws Exception {
    // https://github.com/square/okhttp/issues/4729
    platform.expectFailureOnConscryptPlatform();
    platform.expectFailureOnCorrettoPlatform();

    // Start with two root CA certificates, one is good and the other is compromised.
    HeldCertificate rootCa = new HeldCertificate.Builder()
        .serialNumber(1L)
        .certificateAuthority(1)
        .commonName("root")
        .build();
    HeldCertificate compromisedRootCa = new HeldCertificate.Builder()
        .serialNumber(2L)
        .certificateAuthority(1)
        .commonName("compromised_root")
        .build();

    // Add a good intermediate CA, and have that issue a good certificate to localhost. Prepare an
    // SSL context for an HTTP client under attack. It includes the trusted CA and a pinned
    // certificate.
    HeldCertificate goodIntermediateCa = new HeldCertificate.Builder()
        .signedBy(rootCa)
        .certificateAuthority(0)
        .serialNumber(3L)
        .commonName("intermediate_ca")
        .build();
    CertificatePinner certificatePinner = new CertificatePinner.Builder()
        .add(server.getHostName(), CertificatePinner.pin(goodIntermediateCa.certificate()))
        .build();
    HandshakeCertificates handshakeCertificates = new HandshakeCertificates.Builder()
        .addTrustedCertificate(rootCa.certificate())
        .addTrustedCertificate(compromisedRootCa.certificate())
        .build();
    OkHttpClient client = clientTestRule.newClientBuilder()
        .sslSocketFactory(
            handshakeCertificates.sslSocketFactory(), handshakeCertificates.trustManager())
        .hostnameVerifier(new RecordingHostnameVerifier())
        .certificatePinner(certificatePinner)
        .build();

    // The attacker compromises the root CA, issues an intermediate with the same common name
    // "intermediate_ca" as the good CA. This signs a rogue certificate for localhost. The server
    // serves the good CAs certificate in the chain, which means the certificate pinner sees a
    // different set of certificates than the SSL verifier.
    HeldCertificate compromisedIntermediateCa = new HeldCertificate.Builder()
        .signedBy(compromisedRootCa)
        .certificateAuthority(0)
        .serialNumber(4L)
        .commonName("intermediate_ca")
        .build();
    HeldCertificate rogueCertificate = new HeldCertificate.Builder()
        .serialNumber(5L)
        .signedBy(compromisedIntermediateCa)
        .commonName(server.getHostName())
        .build();

    SSLSocketFactory socketFactory = newServerSocketFactory(rogueCertificate,
        goodIntermediateCa.certificate(), compromisedIntermediateCa.certificate());
    server.useHttps(socketFactory, false);
    server.enqueue(new MockResponse()
        .setBody("abc")
        .addHeader("Content-Type: text/plain"));

    // Make a request from client to server. It should succeed certificate checks (unfortunately the
    // rogue CA is trusted) but it should fail certificate pinning.
    Request request = new Request.Builder()
        .url(server.url("/"))
        .build();
    Call call = client.newCall(request);
    try {
      call.execute();
      fail();
    } catch (SSLHandshakeException expected) {
      // On Android, the handshake fails before the certificate pinner runs.
      String message = expected.getMessage();
      assertThat(message).contains("Could not validate certificate");
    } catch (SSLPeerUnverifiedException expected) {
      // On OpenJDK, the handshake succeeds but the certificate pinner fails.
      String message = expected.getMessage();
      assertThat(message).startsWith("Certificate pinning failure!");
    }
  }

  /**
   * Not checking the CA bit created a vulnerability in old OkHttp releases. It is exploited by
   * triggering different chains to be discovered by the TLS engine and our chain cleaner. In this
   * attack there's several different chains.
   *
   * <p>The victim's gets a non-CA certificate signed by a CA, and pins the CA root and/or
   * intermediate. This is business as usual.
   *
   * <pre>{@code
   *
   *   pinnedRoot (trusted by CertificatePinner)
   *     -> pinnedIntermediate (trusted by CertificatePinner)
   *       -> realVictim
   *
   * }</pre>
   *
   * <p>The attacker compromises a CA. They take the public key from an intermediate certificate
   * signed by the compromised CA's certificate and uses it in a non-CA certificate. They ask the
   * pinned CA above to sign it for non-certificate-authority uses:
   *
   * <pre>{@code
   *
   *   pinnedRoot (trusted by CertificatePinner)
   *     -> pinnedIntermediate (trusted by CertificatePinner)
   *         -> attackerSwitch
   *
   * }</pre>
   *
   * <p>The attacker serves a set of certificates that yields a too-long chain in our certificate
   * pinner. The served certificates (incorrectly) formed a single chain to the pinner:
   *
   * <pre>{@code
   *
   *   attackerCa
   *     -> attackerIntermediate
   *         -> pinnedRoot (trusted by CertificatePinner)
   *             -> pinnedIntermediate (trusted by CertificatePinner)
   *                 -> attackerSwitch (not a CA certificate!)
   *                     -> phonyVictim
   *
   * }</pre>
   *
   * But this chain is wrong because the attackerSwitch certificate is being used in a CA role even
   * though it is not a CA certificate. There are pinned certificates in the chain! The correct
   * chain is much shorter because it skips the non-CA certificate.
   *
   * <pre>{@code
   *
   *   attackerCa
   *     -> attackerIntermediate
   *         -> phonyVictim
   *
   * }</pre>
   *
   * Some implementations fail the TLS handshake when they see the long chain, and don't give
   * CertificatePinner the opportunity to produce a different chain from their own. This includes
   * the OpenJDK 11 TLS implementation, which itself fails the handshake when it encounters a non-CA
   * certificate.
   */
  @Test public void signersMustHaveCaBitSet() throws Exception {
    // TODO failing on conscrypt
    platform.assumeNotConscrypt();

    HeldCertificate attackerCa = new HeldCertificate.Builder()
        .serialNumber(1L)
        .certificateAuthority(4)
        .commonName("attacker ca")
        .build();
    HeldCertificate attackerIntermediate = new HeldCertificate.Builder()
        .serialNumber(2L)
        .certificateAuthority(3)
        .commonName("attacker")
        .signedBy(attackerCa)
        .build();
    HeldCertificate pinnedRoot = new HeldCertificate.Builder()
        .serialNumber(3L)
        .certificateAuthority(2)
        .commonName("pinned root")
        .signedBy(attackerIntermediate)
        .build();
    HeldCertificate pinnedIntermediate = new HeldCertificate.Builder()
        .serialNumber(4L)
        .certificateAuthority(1)
        .commonName("pinned intermediate")
        .signedBy(pinnedRoot)
        .build();
    HeldCertificate attackerSwitch = new HeldCertificate.Builder()
        .serialNumber(5L)
        .keyPair(attackerIntermediate.keyPair()) // share keys between compromised CA and leaf!
        .commonName("attacker")
        .addSubjectAlternativeName("attacker.com")
        .signedBy(pinnedIntermediate)
        .build();
    HeldCertificate phonyVictim = new HeldCertificate.Builder()
        .serialNumber(6L)
        .signedBy(attackerSwitch)
        .addSubjectAlternativeName("victim.com")
        .commonName("victim")
        .build();

    CertificatePinner certificatePinner = new CertificatePinner.Builder()
        .add(server.getHostName(), CertificatePinner.pin(pinnedRoot.certificate()))
        .build();
    HandshakeCertificates handshakeCertificates = new HandshakeCertificates.Builder()
        .addTrustedCertificate(pinnedRoot.certificate())
        .addTrustedCertificate(attackerCa.certificate())
        .build();
    OkHttpClient client = clientTestRule.newClientBuilder()
        .sslSocketFactory(
            handshakeCertificates.sslSocketFactory(), handshakeCertificates.trustManager())
        .hostnameVerifier(new RecordingHostnameVerifier())
        .certificatePinner(certificatePinner)
        .build();

    HandshakeCertificates serverHandshakeCertificates = new HandshakeCertificates.Builder()
        .heldCertificate(
            phonyVictim,
            attackerSwitch.certificate(),
            pinnedIntermediate.certificate(),
            pinnedRoot.certificate(),
            attackerIntermediate.certificate()
        )
        .build();
    server.useHttps(serverHandshakeCertificates.sslSocketFactory(), false);

    server.enqueue(new MockResponse());

    // Make a request from client to server. It should succeed certificate checks (unfortunately the
    // rogue CA is trusted) but it should fail certificate pinning.
    Request request = new Request.Builder()
        .url(server.url("/"))
        .build();
    Call call = client.newCall(request);
    try (Response response = call.execute()) {
      fail("expected connection failure but got " + response);
    } catch (SSLPeerUnverifiedException expected) {
      // Certificate pinning fails!
      String message = expected.getMessage();
      assertThat(message).startsWith("Certificate pinning failure!");
    } catch (SSLHandshakeException expected) {
      // We didn't have the opportunity to do certificate pinning because the handshake failed.
      assertThat(expected).hasMessageContaining("this is not a CA certificate");
    }
  }

  private SSLSocketFactory newServerSocketFactory(HeldCertificate heldCertificate,
      X509Certificate... intermediates) throws GeneralSecurityException {
    // Test setup fails on JDK9
    // java.security.KeyStoreException: Certificate chain is not valid
    // at sun.security.pkcs12.PKCS12KeyStore.setKeyEntry
    // http://openjdk.java.net/jeps/229
    // http://hg.openjdk.java.net/jdk9/jdk9/jdk/file/2c1c21d11e58/src/share/classes/sun/security/pkcs12/PKCS12KeyStore.java#l596
    String keystoreType = platform.isJdk9() ? "JKS" : null;
    X509KeyManager x509KeyManager = newKeyManager(keystoreType, heldCertificate, intermediates);
    X509TrustManager trustManager = newTrustManager(
        keystoreType, Collections.emptyList(), Collections.emptyList());
    SSLContext sslContext = Platform.get().newSSLContext();
    sslContext.init(new KeyManager[] {x509KeyManager}, new TrustManager[] {trustManager},
        new SecureRandom());
    return sslContext.getSocketFactory();
  }
}
