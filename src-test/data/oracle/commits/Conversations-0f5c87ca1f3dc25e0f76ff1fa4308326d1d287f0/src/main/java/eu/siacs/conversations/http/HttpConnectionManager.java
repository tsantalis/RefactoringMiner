package eu.siacs.conversations.http;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import eu.siacs.conversations.entities.Message;
import eu.siacs.conversations.services.AbstractConnectionManager;
import eu.siacs.conversations.services.XmppConnectionService;

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
}
