package org.refactoringminer;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;

public class RefactoringMinerHttpsServer extends RefactoringMinerHttpServer {

	// Constructor required to match the superclass
	RefactoringMinerHttpsServer(String url, String sha, int timeout, String token) {
		super(url, sha, timeout, token);
	}

	public static void main(String[] args) throws Exception {
		Properties prop = getProperties();
		SSLContext sslContext = setupSSL(prop);
		HttpsServer server = HttpsServer.create(setupSocket(prop), 0);
		server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
			public void configure(HttpsParameters params) {
				try {
					// initialize the SSL context
					SSLContext context = getSSLContext();
					SSLEngine engine = context.createSSLEngine();
					params.setNeedClientAuth(false);
					params.setCipherSuites(engine.getEnabledCipherSuites());
					params.setProtocols(engine.getEnabledProtocols());

					// Set the SSL parameters
					SSLParameters sslParameters = context.getSupportedSSLParameters();
					params.setSSLParameters(sslParameters);

				} catch (Exception ex) {
					System.out.println("Failed to create HTTPS port");
				}
			}
		});
		
		server.createContext("/RefactoringMiner", (HttpExchange exchange) -> {
			exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
			RefactoringMinerHttpServer miner = RefactoringMinerHttpServer.from(exchange);
			miner.prepareResponse();
			miner.sendResponse(exchange);
		});

		server.setExecutor(new ThreadPoolExecutor(4, 8, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100), new ThreadPoolExecutor.CallerRunsPolicy()));
		server.start();
		System.out.println(InetAddress.getLocalHost());
	}

	@Nonnull
	private static SSLContext setupSSL(Properties prop) throws NoSuchAlgorithmException, KeyStoreException, IOException, CertificateException, UnrecoverableKeyException, KeyManagementException {
		SSLContext sslContext = SSLContext.getInstance("TLS");
		// initialize the keystore
		String keystore = prop.getProperty("keystore", System.getenv("keystore"));
		String keyStorePass = prop.getProperty("keystore-password", System.getenv("keystore-password"));
		char[] password = keyStorePass.toCharArray();
		KeyStore ks = KeyStore.getInstance("JKS");
		FileInputStream fis = new FileInputStream(keystore);
		ks.load(fis, password);

		// setup the key manager factory
		KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
		kmf.init(ks, password);

		// setup the trust manager factory
		TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
		tmf.init(ks);

		// setup the HTTPS context and parameters
		sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
		return sslContext;
	}
}