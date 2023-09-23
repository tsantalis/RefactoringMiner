package eu.siacs.conversations.entities;

import android.content.ContentValues;
import android.database.Cursor;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

import eu.siacs.conversations.Config;
import eu.siacs.conversations.crypto.axolotl.AxolotlService;
import eu.siacs.conversations.utils.GeoHelper;
import eu.siacs.conversations.utils.MimeUtils;
import eu.siacs.conversations.utils.UIHelper;
import eu.siacs.conversations.xmpp.jid.InvalidJidException;
import eu.siacs.conversations.xmpp.jid.Jid;

public class Message extends AbstractEntity {

	public static final String TABLENAME = "messages";

	public static final String MERGE_SEPARATOR = " \u200B\n\n";

	public static final int STATUS_RECEIVED = 0;
	public static final int STATUS_UNSEND = 1;
	public static final int STATUS_SEND = 2;
	public static final int STATUS_SEND_FAILED = 3;
	public static final int STATUS_WAITING = 5;
	public static final int STATUS_OFFERED = 6;
	public static final int STATUS_SEND_RECEIVED = 7;
	public static final int STATUS_SEND_DISPLAYED = 8;

	public static final int ENCRYPTION_NONE = 0;
	public static final int ENCRYPTION_PGP = 1;
	public static final int ENCRYPTION_OTR = 2;
	public static final int ENCRYPTION_DECRYPTED = 3;
	public static final int ENCRYPTION_DECRYPTION_FAILED = 4;
	public static final int ENCRYPTION_AXOLOTL = 5;

	public static final int TYPE_TEXT = 0;
	public static final int TYPE_IMAGE = 1;
	public static final int TYPE_FILE = 2;
	public static final int TYPE_STATUS = 3;
	public static final int TYPE_PRIVATE = 4;

	public static final String CONVERSATION = "conversationUuid";
	public static final String COUNTERPART = "counterpart";
	public static final String TRUE_COUNTERPART = "trueCounterpart";
	public static final String BODY = "body";
	public static final String TIME_SENT = "timeSent";
	public static final String ENCRYPTION = "encryption";
	public static final String STATUS = "status";
	public static final String TYPE = "type";
	public static final String REMOTE_MSG_ID = "remoteMsgId";
	public static final String SERVER_MSG_ID = "serverMsgId";
	public static final String RELATIVE_FILE_PATH = "relativeFilePath";
	public static final String FINGERPRINT = "axolotl_fingerprint";
	public static final String ME_COMMAND = "/me ";


	public boolean markable = false;
	protected String conversationUuid;
	protected Jid counterpart;
	protected Jid trueCounterpart;
	protected String body;
	protected String encryptedBody;
	protected long timeSent;
	protected int encryption;
	protected int status;
	protected int type;
	protected String relativeFilePath;
	protected boolean read = true;
	protected String remoteMsgId = null;
	protected String serverMsgId = null;
	protected Conversation conversation = null;
	protected Downloadable downloadable = null;
	private Message mNextMessage = null;
	private Message mPreviousMessage = null;
	private String axolotlFingerprint = null;

	private Message() {

	}

	public Message(Conversation conversation, String body, int encryption) {
		this(conversation, body, encryption, STATUS_UNSEND);
	}

	public Message(Conversation conversation, String body, int encryption, int status) {
		this(java.util.UUID.randomUUID().toString(),
				conversation.getUuid(),
				conversation.getJid() == null ? null : conversation.getJid().toBareJid(),
				null,
				body,
				System.currentTimeMillis(),
				encryption,
				status,
				TYPE_TEXT,
				null,
				null,
				null,
				null);
		this.conversation = conversation;
	}

	private Message(final String uuid, final String conversationUUid, final Jid counterpart,
					final Jid trueCounterpart, final String body, final long timeSent,
					final int encryption, final int status, final int type, final String remoteMsgId,
					final String relativeFilePath, final String serverMsgId, final String fingerprint) {
		this.uuid = uuid;
		this.conversationUuid = conversationUUid;
		this.counterpart = counterpart;
		this.trueCounterpart = trueCounterpart;
		this.body = body;
		this.timeSent = timeSent;
		this.encryption = encryption;
		this.status = status;
		this.type = type;
		this.remoteMsgId = remoteMsgId;
		this.relativeFilePath = relativeFilePath;
		this.serverMsgId = serverMsgId;
		this.axolotlFingerprint = fingerprint;
	}

	public static Message fromCursor(Cursor cursor) {
		Jid jid;
		try {
			String value = cursor.getString(cursor.getColumnIndex(COUNTERPART));
			if (value != null) {
				jid = Jid.fromString(value, true);
			} else {
				jid = null;
			}
		} catch (InvalidJidException e) {
			jid = null;
		}
		Jid trueCounterpart;
		try {
			String value = cursor.getString(cursor.getColumnIndex(TRUE_COUNTERPART));
			if (value != null) {
				trueCounterpart = Jid.fromString(value, true);
			} else {
				trueCounterpart = null;
			}
		} catch (InvalidJidException e) {
			trueCounterpart = null;
		}
		return new Message(cursor.getString(cursor.getColumnIndex(UUID)),
				cursor.getString(cursor.getColumnIndex(CONVERSATION)),
				jid,
				trueCounterpart,
				cursor.getString(cursor.getColumnIndex(BODY)),
				cursor.getLong(cursor.getColumnIndex(TIME_SENT)),
				cursor.getInt(cursor.getColumnIndex(ENCRYPTION)),
				cursor.getInt(cursor.getColumnIndex(STATUS)),
				cursor.getInt(cursor.getColumnIndex(TYPE)),
				cursor.getString(cursor.getColumnIndex(REMOTE_MSG_ID)),
				cursor.getString(cursor.getColumnIndex(RELATIVE_FILE_PATH)),
				cursor.getString(cursor.getColumnIndex(SERVER_MSG_ID)),
				cursor.getString(cursor.getColumnIndex(FINGERPRINT)));
	}

	public static Message createStatusMessage(Conversation conversation, String body) {
		Message message = new Message();
		message.setType(Message.TYPE_STATUS);
		message.setConversation(conversation);
		message.setBody(body);
		return message;
	}

	@Override
	public ContentValues getContentValues() {
		ContentValues values = new ContentValues();
		values.put(UUID, uuid);
		values.put(CONVERSATION, conversationUuid);
		if (counterpart == null) {
			values.putNull(COUNTERPART);
		} else {
			values.put(COUNTERPART, counterpart.toString());
		}
		if (trueCounterpart == null) {
			values.putNull(TRUE_COUNTERPART);
		} else {
			values.put(TRUE_COUNTERPART, trueCounterpart.toString());
		}
		values.put(BODY, body);
		values.put(TIME_SENT, timeSent);
		values.put(ENCRYPTION, encryption);
		values.put(STATUS, status);
		values.put(TYPE, type);
		values.put(REMOTE_MSG_ID, remoteMsgId);
		values.put(RELATIVE_FILE_PATH, relativeFilePath);
		values.put(SERVER_MSG_ID, serverMsgId);
		values.put(FINGERPRINT, axolotlFingerprint);
		return values;
	}

	public String getConversationUuid() {
		return conversationUuid;
	}

	public Conversation getConversation() {
		return this.conversation;
	}

	public void setConversation(Conversation conv) {
		this.conversation = conv;
	}

	public Jid getCounterpart() {
		return counterpart;
	}

	public void setCounterpart(final Jid counterpart) {
		this.counterpart = counterpart;
	}

	public Contact getContact() {
		if (this.conversation.getMode() == Conversation.MODE_SINGLE) {
			return this.conversation.getContact();
		} else {
			if (this.trueCounterpart == null) {
				return null;
			} else {
				return this.conversation.getAccount().getRoster()
						.getContactFromRoster(this.trueCounterpart);
			}
		}
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public long getTimeSent() {
		return timeSent;
	}

	public int getEncryption() {
		return encryption;
	}

	public void setEncryption(int encryption) {
		this.encryption = encryption;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getRelativeFilePath() {
		return this.relativeFilePath;
	}

	public void setRelativeFilePath(String path) {
		this.relativeFilePath = path;
	}

	public String getRemoteMsgId() {
		return this.remoteMsgId;
	}

	public void setRemoteMsgId(String id) {
		this.remoteMsgId = id;
	}

	public String getServerMsgId() {
		return this.serverMsgId;
	}

	public void setServerMsgId(String id) {
		this.serverMsgId = id;
	}

	public boolean isRead() {
		return this.read;
	}

	public void markRead() {
		this.read = true;
	}

	public void markUnread() {
		this.read = false;
	}

	public void setTime(long time) {
		this.timeSent = time;
	}

	public String getEncryptedBody() {
		return this.encryptedBody;
	}

	public void setEncryptedBody(String body) {
		this.encryptedBody = body;
	}

	public int getType() {
		return this.type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public void setTrueCounterpart(Jid trueCounterpart) {
		this.trueCounterpart = trueCounterpart;
	}

	public Downloadable getDownloadable() {
		return this.downloadable;
	}

	public void setDownloadable(Downloadable downloadable) {
		this.downloadable = downloadable;
	}

	public boolean equals(Message message) {
		if (this.serverMsgId != null && message.getServerMsgId() != null) {
			return this.serverMsgId.equals(message.getServerMsgId());
		} else if (this.body == null || this.counterpart == null) {
			return false;
		} else if (message.getRemoteMsgId() != null) {
			return (message.getRemoteMsgId().equals(this.remoteMsgId) || message.getRemoteMsgId().equals(this.uuid))
					&& this.counterpart.equals(message.getCounterpart())
					&& this.body.equals(message.getBody());
		} else {
			return this.remoteMsgId == null
					&& this.counterpart.equals(message.getCounterpart())
					&& this.body.equals(message.getBody())
					&& Math.abs(this.getTimeSent() - message.getTimeSent()) < Config.MESSAGE_MERGE_WINDOW * 1000;
		}
	}

	public Message next() {
		synchronized (this.conversation.messages) {
			if (this.mNextMessage == null) {
				int index = this.conversation.messages.indexOf(this);
				if (index < 0 || index >= this.conversation.messages.size() - 1) {
					this.mNextMessage = null;
				} else {
					this.mNextMessage = this.conversation.messages.get(index + 1);
				}
			}
			return this.mNextMessage;
		}
	}

	public Message prev() {
		synchronized (this.conversation.messages) {
			if (this.mPreviousMessage == null) {
				int index = this.conversation.messages.indexOf(this);
				if (index <= 0 || index > this.conversation.messages.size()) {
					this.mPreviousMessage = null;
				} else {
					this.mPreviousMessage = this.conversation.messages.get(index - 1);
				}
			}
			return this.mPreviousMessage;
		}
	}

	public boolean mergeable(final Message message) {
		return message != null &&
				(message.getType() == Message.TYPE_TEXT &&
						this.getDownloadable() == null &&
						message.getDownloadable() == null &&
						message.getEncryption() != Message.ENCRYPTION_PGP &&
						this.getType() == message.getType() &&
						//this.getStatus() == message.getStatus() &&
						isStatusMergeable(this.getStatus(), message.getStatus()) &&
						this.getEncryption() == message.getEncryption() &&
						this.getCounterpart() != null &&
						this.getCounterpart().equals(message.getCounterpart()) &&
						(message.getTimeSent() - this.getTimeSent()) <= (Config.MESSAGE_MERGE_WINDOW * 1000) &&
						!GeoHelper.isGeoUri(message.getBody()) &&
						!GeoHelper.isGeoUri(this.body) &&
						message.treatAsDownloadable() == Decision.NEVER &&
						this.treatAsDownloadable() == Decision.NEVER &&
						!message.getBody().startsWith(ME_COMMAND) &&
						!this.getBody().startsWith(ME_COMMAND) &&
						!this.bodyIsHeart() &&
						!message.bodyIsHeart()
				);
	}

	private static boolean isStatusMergeable(int a, int b) {
		return a == b || (
				(a == Message.STATUS_SEND_RECEIVED && b == Message.STATUS_UNSEND)
						|| (a == Message.STATUS_SEND_RECEIVED && b == Message.STATUS_SEND)
						|| (a == Message.STATUS_UNSEND && b == Message.STATUS_SEND)
						|| (a == Message.STATUS_UNSEND && b == Message.STATUS_SEND_RECEIVED)
						|| (a == Message.STATUS_SEND && b == Message.STATUS_UNSEND)
						|| (a == Message.STATUS_SEND && b == Message.STATUS_SEND_RECEIVED)
		);
	}

	public String getMergedBody() {
		final Message next = this.next();
		if (this.mergeable(next)) {
			return getBody().trim() + MERGE_SEPARATOR + next.getMergedBody();
		}
		return getBody().trim();
	}

	public boolean hasMeCommand() {
		return getMergedBody().startsWith(ME_COMMAND);
	}

	public int getMergedStatus() {
		final Message next = this.next();
		if (this.mergeable(next)) {
			return next.getStatus();
		}
		return getStatus();
	}

	public long getMergedTimeSent() {
		Message next = this.next();
		if (this.mergeable(next)) {
			return next.getMergedTimeSent();
		} else {
			return getTimeSent();
		}
	}

	public boolean wasMergedIntoPrevious() {
		Message prev = this.prev();
		return prev != null && prev.mergeable(this);
	}

	public boolean trusted() {
		Contact contact = this.getContact();
		return (status > STATUS_RECEIVED || (contact != null && contact.trusted()));
	}

	public boolean fixCounterpart() {
		Presences presences = conversation.getContact().getPresences();
		if (counterpart != null && presences.has(counterpart.getResourcepart())) {
			return true;
		} else if (presences.size() >= 1) {
			try {
				counterpart = Jid.fromParts(conversation.getJid().getLocalpart(),
						conversation.getJid().getDomainpart(),
						presences.asStringArray()[0]);
				return true;
			} catch (InvalidJidException e) {
				counterpart = null;
				return false;
			}
		} else {
			counterpart = null;
			return false;
		}
	}

	public enum Decision {
		MUST,
		SHOULD,
		NEVER,
	}

	private static String extractRelevantExtension(URL url) {
		String path = url.getPath();
		if (path == null || path.isEmpty()) {
			return null;
		}
		String filename = path.substring(path.lastIndexOf('/') + 1).toLowerCase();
		String[] extensionParts = filename.split("\\.");
		if (extensionParts.length == 2) {
			return extensionParts[extensionParts.length - 1];
		} else if (extensionParts.length == 3 && Arrays
				.asList(Downloadable.VALID_CRYPTO_EXTENSIONS)
				.contains(extensionParts[extensionParts.length - 1])) {
			return extensionParts[extensionParts.length -2];
		}
		return null;
	}

	public String getMimeType() {
		if (relativeFilePath != null) {
			int start = relativeFilePath.lastIndexOf('.') + 1;
			if (start < relativeFilePath.length()) {
				return MimeUtils.guessMimeTypeFromExtension(relativeFilePath.substring(start));
			} else {
				return null;
			}
		} else {
			try {
				return MimeUtils.guessMimeTypeFromExtension(extractRelevantExtension(new URL(body.trim())));
			} catch (MalformedURLException e) {
				return null;
			}
		}
	}

	public Decision treatAsDownloadable() {
		if (body.trim().contains(" ")) {
			return Decision.NEVER;
		}
		try {
			URL url = new URL(body);
			if (!url.getProtocol().equalsIgnoreCase("http") && !url.getProtocol().equalsIgnoreCase("https")) {
				return Decision.NEVER;
			}
			String extension = extractRelevantExtension(url);
			if (extension == null) {
				return Decision.NEVER;
			}
			String ref = url.getRef();
			boolean encrypted = ref != null && ref.matches("([A-Fa-f0-9]{2}){48}");

			if (encrypted) {
				if (MimeUtils.guessMimeTypeFromExtension(extension) != null) {
					return Decision.MUST;
				} else {
					return Decision.NEVER;
				}
			} else if (Arrays.asList(Downloadable.VALID_IMAGE_EXTENSIONS).contains(extension)
					|| Arrays.asList(Downloadable.WELL_KNOWN_EXTENSIONS).contains(extension)) {
				return Decision.SHOULD;
			} else {
				return Decision.NEVER;
			}

		} catch (MalformedURLException e) {
			return Decision.NEVER;
		}
	}

	public boolean bodyIsHeart() {
		return body != null && UIHelper.HEARTS.contains(body.trim());
	}

	public FileParams getFileParams() {
		FileParams params = getLegacyFileParams();
		if (params != null) {
			return params;
		}
		params = new FileParams();
		if (this.downloadable != null) {
			params.size = this.downloadable.getFileSize();
		}
		if (body == null) {
			return params;
		}
		String parts[] = body.split("\\|");
		switch (parts.length) {
			case 1:
				try {
					params.size = Long.parseLong(parts[0]);
				} catch (NumberFormatException e) {
					try {
						params.url = new URL(parts[0]);
					} catch (MalformedURLException e1) {
						params.url = null;
					}
				}
				break;
			case 2:
			case 4:
				try {
					params.url = new URL(parts[0]);
				} catch (MalformedURLException e1) {
					params.url = null;
				}
				try {
					params.size = Long.parseLong(parts[1]);
				} catch (NumberFormatException e) {
					params.size = 0;
				}
				try {
					params.width = Integer.parseInt(parts[2]);
				} catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
					params.width = 0;
				}
				try {
					params.height = Integer.parseInt(parts[3]);
				} catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
					params.height = 0;
				}
				break;
			case 3:
				try {
					params.size = Long.parseLong(parts[0]);
				} catch (NumberFormatException e) {
					params.size = 0;
				}
				try {
					params.width = Integer.parseInt(parts[1]);
				} catch (NumberFormatException e) {
					params.width = 0;
				}
				try {
					params.height = Integer.parseInt(parts[2]);
				} catch (NumberFormatException e) {
					params.height = 0;
				}
				break;
		}
		return params;
	}

	public FileParams getLegacyFileParams() {
		FileParams params = new FileParams();
		if (body == null) {
			return params;
		}
		String parts[] = body.split(",");
		if (parts.length == 3) {
			try {
				params.size = Long.parseLong(parts[0]);
			} catch (NumberFormatException e) {
				return null;
			}
			try {
				params.width = Integer.parseInt(parts[1]);
			} catch (NumberFormatException e) {
				return null;
			}
			try {
				params.height = Integer.parseInt(parts[2]);
			} catch (NumberFormatException e) {
				return null;
			}
			return params;
		} else {
			return null;
		}
	}

	public void untie() {
		this.mNextMessage = null;
		this.mPreviousMessage = null;
	}

	public boolean isFileOrImage() {
		return type == TYPE_FILE || type == TYPE_IMAGE;
	}

	public boolean hasFileOnRemoteHost() {
		return isFileOrImage() && getFileParams().url != null;
	}

	public boolean needsUploading() {
		return isFileOrImage() && getFileParams().url == null;
	}

	public class FileParams {
		public URL url;
		public long size = 0;
		public int width = 0;
		public int height = 0;
	}

	public void setAxolotlFingerprint(String fingerprint) {
		this.axolotlFingerprint = fingerprint;
	}
}
