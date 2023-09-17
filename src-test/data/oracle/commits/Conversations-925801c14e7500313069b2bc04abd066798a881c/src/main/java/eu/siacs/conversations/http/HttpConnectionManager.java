package eu.siacs.conversations.http;

import org.apache.http.conn.ssl.StrictHostnameVerifier;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import eu.siacs.conversations.entities.Message;
import eu.siacs.conversations.services.AbstractConnectionManager;
import eu.siacs.conversations.services.XmppConnectionService;
import eu.siacs.conversations.utils.CryptoHelper;

public class HttpConnectionManager extends AbstractConnectionManager {

	public HttpConnectionManager(XmppConnectionService service) {
		super(service);
	}

	private List<HttpConnection> connections = new CopyOnWriteArrayList<>();
	private List<HttpUploadConnection> uploadConnections = new CopyOnWriteArrayList<>();

	public HttpConnection createNewConnection(Message message) {
		return this.createNewConnection(message,false);
	}

	public HttpConnection createNewConnection(Message message,boolean interactive) {
		HttpConnection connection = new HttpConnection(this);
		connection.init(message,interactive);
		this.connections.add(connection);
		return connection;
	}

	public HttpUploadConnection createNewUploadConnection(Message message) {
		HttpUploadConnection connection = new HttpUploadConnection(this);
		connection.init(message);
		this.uploadConnections.add(connection);
		return connection;
	}

	public void finishConnection(HttpConnection connection) {
		this.connections.remove(connection);
	}

	public void finishUploadConnection(HttpUploadConnection httpUploadConnection) {
		this.uploadConnections.remove(httpUploadConnection);
	}

	public void setupTrustManager(final HttpsURLConnection connection, final boolean interactive) {
		final X509TrustManager trustManager;
		final HostnameVerifier hostnameVerifier;
		if (interactive) {
			trustManager = mXmppConnectionService.getMemorizingTrustManager();
			hostnameVerifier = mXmppConnectionService
					.getMemorizingTrustManager().wrapHostnameVerifier(
							new StrictHostnameVerifier());
		} else {
			trustManager = mXmppConnectionService.getMemorizingTrustManager()
					.getNonInteractive();
			hostnameVerifier = mXmppConnectionService
					.getMemorizingTrustManager()
					.wrapHostnameVerifierNonInteractive(
							new StrictHostnameVerifier());
		}
		try {
			final SSLContext sc = SSLContext.getInstance("TLS");
			sc.init(null, new X509TrustManager[]{trustManager},
					mXmppConnectionService.getRNG());

			final SSLSocketFactory sf = sc.getSocketFactory();
			final String[] cipherSuites = CryptoHelper.getOrderedCipherSuites(
					sf.getSupportedCipherSuites());
			if (cipherSuites.length > 0) {
				sc.getDefaultSSLParameters().setCipherSuites(cipherSuites);

			}

			connection.setSSLSocketFactory(sf);
			connection.setHostnameVerifier(hostnameVerifier);
		} catch (final KeyManagementException | NoSuchAlgorithmException ignored) {
		}
	}
}
