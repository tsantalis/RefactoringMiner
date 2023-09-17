package eu.siacs.conversations.http;

import android.app.PendingIntent;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import eu.siacs.conversations.Config;
import eu.siacs.conversations.entities.Account;
import eu.siacs.conversations.entities.Downloadable;
import eu.siacs.conversations.entities.DownloadableFile;
import eu.siacs.conversations.entities.Message;
import eu.siacs.conversations.persistance.FileBackend;
import eu.siacs.conversations.services.XmppConnectionService;
import eu.siacs.conversations.ui.UiCallback;
import eu.siacs.conversations.utils.CryptoHelper;
import eu.siacs.conversations.utils.Xmlns;
import eu.siacs.conversations.xml.Element;
import eu.siacs.conversations.xmpp.OnIqPacketReceived;
import eu.siacs.conversations.xmpp.jid.Jid;
import eu.siacs.conversations.xmpp.stanzas.IqPacket;

public class HttpUploadConnection implements Downloadable {

	private HttpConnectionManager mHttpConnectionManager;
	private XmppConnectionService mXmppConnectionService;

	private boolean canceled = false;
	private Account account;
	private DownloadableFile file;
	private Message message;
	private URL mGetUrl;
	private URL mPutUrl;

	private byte[] key = null;

	private long transmitted = 0;
	private long expected = 1;

	public HttpUploadConnection(HttpConnectionManager httpConnectionManager) {
		this.mHttpConnectionManager = httpConnectionManager;
		this.mXmppConnectionService = httpConnectionManager.getXmppConnectionService();
	}

	@Override
	public boolean start() {
		return false;
	}

	@Override
	public int getStatus() {
		return STATUS_UPLOADING;
	}

	@Override
	public long getFileSize() {
		return this.file.getExpectedSize();
	}

	@Override
	public int getProgress() {
		return (int) ((((double) transmitted) / expected) * 100);
	}

	@Override
	public void cancel() {
		this.canceled = true;
	}

	private void fail() {
		mHttpConnectionManager.finishUploadConnection(this);
		message.setDownloadable(null);
		mXmppConnectionService.markMessage(message,Message.STATUS_SEND_FAILED);
	}

	public void init(Message message) {
		this.message = message;
		message.setDownloadable(this);
		mXmppConnectionService.markMessage(message,Message.STATUS_UNSEND);
		this.account = message.getConversation().getAccount();
		this.file = mXmppConnectionService.getFileBackend().getFile(message, false);
		this.file.setExpectedSize(this.file.getSize());

		if (Config.ENCRYPT_ON_HTTP_UPLOADED) {
			this.key = new byte[48];
			mXmppConnectionService.getRNG().nextBytes(this.key);
			this.file.setKey(this.key);
		}

		Jid host = account.getXmppConnection().findDiscoItemByFeature(Xmlns.HTTP_UPLOAD);
		IqPacket request = mXmppConnectionService.getIqGenerator().requestHttpUploadSlot(host,file);
		mXmppConnectionService.sendIqPacket(account, request, new OnIqPacketReceived() {
			@Override
			public void onIqPacketReceived(Account account, IqPacket packet) {
				if (packet.getType() == IqPacket.TYPE.RESULT) {
					Element slot = packet.findChild("slot",Xmlns.HTTP_UPLOAD);
					if (slot != null) {
						try {
							mGetUrl = new URL(slot.findChildContent("get"));
							mPutUrl = new URL(slot.findChildContent("put"));
							if (!canceled) {
								new Thread(new FileUploader()).start();
							}
						} catch (MalformedURLException e) {
							fail();
						}
					} else {
						fail();
					}
				} else {
					fail();
				}
			}
		});
	}

	private class FileUploader implements Runnable {

		@Override
		public void run() {
			this.upload();
		}

		private void upload() {
			OutputStream os = null;
			InputStream is = null;
			HttpURLConnection connection = null;
			try {
				Log.d(Config.LOGTAG, "uploading to " + mPutUrl.toString());
				connection = (HttpURLConnection) mPutUrl.openConnection();
				if (connection instanceof HttpsURLConnection) {
					mHttpConnectionManager.setupTrustManager((HttpsURLConnection) connection, true);
				}
				connection.setRequestMethod("PUT");
				connection.setFixedLengthStreamingMode((int) file.getExpectedSize());
				connection.setDoOutput(true);
				connection.connect();
				os = connection.getOutputStream();
				is = file.createInputStream();
				transmitted = 0;
				expected = file.getExpectedSize();
				int count = -1;
				byte[] buffer = new byte[4096];
				while (((count = is.read(buffer)) != -1) && !canceled) {
					transmitted += count;
					os.write(buffer, 0, count);
					mXmppConnectionService.updateConversationUi();
				}
				os.flush();
				os.close();
				is.close();
				int code = connection.getResponseCode();
				if (code == 200) {
					Log.d(Config.LOGTAG, "finished uploading file");
					Message.FileParams params = message.getFileParams();
					if (key != null) {
						mGetUrl = new URL(mGetUrl.toString() + "#" + CryptoHelper.bytesToHex(key));
					}
					mXmppConnectionService.getFileBackend().updateFileParams(message, mGetUrl);
					message.setDownloadable(null);
					message.setCounterpart(message.getConversation().getJid().toBareJid());
					if (message.getEncryption() == Message.ENCRYPTION_DECRYPTED) {
						mXmppConnectionService.getPgpEngine().encrypt(message, new UiCallback<Message>() {
							@Override
							public void success(Message message) {
								mXmppConnectionService.resendMessage(message);
							}

							@Override
							public void error(int errorCode, Message object) {
								fail();
							}

							@Override
							public void userInputRequried(PendingIntent pi, Message object) {
								fail();
							}
						});
					} else {
						mXmppConnectionService.resendMessage(message);
					}
				} else {
					fail();
				}
			} catch (IOException e) {
				Log.d(Config.LOGTAG, e.getMessage());
				fail();
			} finally {
				FileBackend.close(is);
				FileBackend.close(os);
				if (connection != null) {
					connection.disconnect();
				}
			}
		}
	}
}
