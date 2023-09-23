package eu.siacs.conversations.crypto.axolotl;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.Log;

import org.whispersystems.libaxolotl.AxolotlAddress;
import org.whispersystems.libaxolotl.DuplicateMessageException;
import org.whispersystems.libaxolotl.IdentityKey;
import org.whispersystems.libaxolotl.IdentityKeyPair;
import org.whispersystems.libaxolotl.InvalidKeyException;
import org.whispersystems.libaxolotl.InvalidKeyIdException;
import org.whispersystems.libaxolotl.InvalidMessageException;
import org.whispersystems.libaxolotl.InvalidVersionException;
import org.whispersystems.libaxolotl.LegacyMessageException;
import org.whispersystems.libaxolotl.NoSessionException;
import org.whispersystems.libaxolotl.SessionBuilder;
import org.whispersystems.libaxolotl.SessionCipher;
import org.whispersystems.libaxolotl.UntrustedIdentityException;
import org.whispersystems.libaxolotl.ecc.Curve;
import org.whispersystems.libaxolotl.ecc.ECKeyPair;
import org.whispersystems.libaxolotl.ecc.ECPublicKey;
import org.whispersystems.libaxolotl.protocol.CiphertextMessage;
import org.whispersystems.libaxolotl.protocol.PreKeyWhisperMessage;
import org.whispersystems.libaxolotl.protocol.WhisperMessage;
import org.whispersystems.libaxolotl.state.AxolotlStore;
import org.whispersystems.libaxolotl.state.PreKeyBundle;
import org.whispersystems.libaxolotl.state.PreKeyRecord;
import org.whispersystems.libaxolotl.state.SessionRecord;
import org.whispersystems.libaxolotl.state.SignedPreKeyRecord;
import org.whispersystems.libaxolotl.util.KeyHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import eu.siacs.conversations.Config;
import eu.siacs.conversations.entities.Account;
import eu.siacs.conversations.entities.Contact;
import eu.siacs.conversations.entities.Conversation;
import eu.siacs.conversations.entities.Message;
import eu.siacs.conversations.parser.IqParser;
import eu.siacs.conversations.services.XmppConnectionService;
import eu.siacs.conversations.utils.SerialSingleThreadExecutor;
import eu.siacs.conversations.xml.Element;
import eu.siacs.conversations.xmpp.OnIqPacketReceived;
import eu.siacs.conversations.xmpp.jid.InvalidJidException;
import eu.siacs.conversations.xmpp.jid.Jid;
import eu.siacs.conversations.xmpp.stanzas.IqPacket;
import eu.siacs.conversations.xmpp.stanzas.MessagePacket;

public class AxolotlService {

	public static final String PEP_PREFIX = "eu.siacs.conversations.axolotl";
	public static final String PEP_DEVICE_LIST = PEP_PREFIX + ".devicelist";
	public static final String PEP_BUNDLES = PEP_PREFIX + ".bundles";

	public static final String LOGPREFIX = "AxolotlService";

	public static final int NUM_KEYS_TO_PUBLISH = 10;

	private final Account account;
	private final XmppConnectionService mXmppConnectionService;
	private final SQLiteAxolotlStore axolotlStore;
	private final SessionMap sessions;
	private final Map<Jid, Set<Integer>> deviceIds;
	private final Map<String, MessagePacket> messageCache;
	private final FetchStatusMap fetchStatusMap;
	private final SerialSingleThreadExecutor executor;
	private int ownDeviceId;

	public static class SQLiteAxolotlStore implements AxolotlStore {

		public static final String PREKEY_TABLENAME = "prekeys";
		public static final String SIGNED_PREKEY_TABLENAME = "signed_prekeys";
		public static final String SESSION_TABLENAME = "sessions";
		public static final String IDENTITIES_TABLENAME = "identities";
		public static final String ACCOUNT = "account";
		public static final String DEVICE_ID = "device_id";
		public static final String ID = "id";
		public static final String KEY = "key";
		public static final String FINGERPRINT = "fingerprint";
		public static final String NAME = "name";
		public static final String TRUSTED = "trusted";
		public static final String OWN = "ownkey";

		public static final String JSONKEY_REGISTRATION_ID = "axolotl_reg_id";
		public static final String JSONKEY_CURRENT_PREKEY_ID = "axolotl_cur_prekey_id";

		private final Account account;
		private final XmppConnectionService mXmppConnectionService;

		private IdentityKeyPair identityKeyPair;
		private final int localRegistrationId;
		private int currentPreKeyId = 0;

		public enum Trust {
			UNDECIDED, // 0
			TRUSTED,
			UNTRUSTED;

			public String toString() {
				switch(this){
					case UNDECIDED:
						return "Trust undecided";
					case TRUSTED:
						return "Trusted";
					case UNTRUSTED:
					default:
						return "Untrusted";
				}
			}
		};

		private static IdentityKeyPair generateIdentityKeyPair() {
			Log.i(Config.LOGTAG, AxolotlService.LOGPREFIX+" : "+"Generating axolotl IdentityKeyPair...");
			ECKeyPair identityKeyPairKeys = Curve.generateKeyPair();
			IdentityKeyPair ownKey = new IdentityKeyPair(new IdentityKey(identityKeyPairKeys.getPublicKey()),
					identityKeyPairKeys.getPrivateKey());
			return ownKey;
		}

		private static int generateRegistrationId() {
			Log.i(Config.LOGTAG, AxolotlService.LOGPREFIX+" : "+"Generating axolotl registration ID...");
			int reg_id = KeyHelper.generateRegistrationId(true);
			return reg_id;
		}

		public SQLiteAxolotlStore(Account account, XmppConnectionService service) {
			this.account = account;
			this.mXmppConnectionService = service;
			this.localRegistrationId = loadRegistrationId();
			this.currentPreKeyId = loadCurrentPreKeyId();
			for (SignedPreKeyRecord record : loadSignedPreKeys()) {
				Log.d(Config.LOGTAG, AxolotlService.getLogprefix(account)+"Got Axolotl signed prekey record:" + record.getId());
			}
		}

		public int getCurrentPreKeyId() {
			return currentPreKeyId;
		}

		// --------------------------------------
		// IdentityKeyStore
		// --------------------------------------

		private IdentityKeyPair loadIdentityKeyPair() {
			String ownName = account.getJid().toBareJid().toString();
			IdentityKeyPair ownKey = mXmppConnectionService.databaseBackend.loadOwnIdentityKeyPair(account,
					ownName);

			if (ownKey != null) {
				return ownKey;
			} else {
				Log.i(Config.LOGTAG, AxolotlService.getLogprefix(account)+"Could not retrieve axolotl key for account " + ownName);
				ownKey = generateIdentityKeyPair();
				mXmppConnectionService.databaseBackend.storeOwnIdentityKeyPair(account, ownName, ownKey);
			}
			return ownKey;
		}

		private int loadRegistrationId() {
			String regIdString = this.account.getKey(JSONKEY_REGISTRATION_ID);
			int reg_id;
			if (regIdString != null) {
				reg_id = Integer.valueOf(regIdString);
			} else {
				Log.i(Config.LOGTAG, AxolotlService.getLogprefix(account)+"Could not retrieve axolotl registration id for account " + account.getJid());
				reg_id = generateRegistrationId();
				boolean success = this.account.setKey(JSONKEY_REGISTRATION_ID, Integer.toString(reg_id));
				if (success) {
					mXmppConnectionService.databaseBackend.updateAccount(account);
				} else {
					Log.e(Config.LOGTAG, AxolotlService.getLogprefix(account)+"Failed to write new key to the database!");
				}
			}
			return reg_id;
		}

		private int loadCurrentPreKeyId() {
			String regIdString = this.account.getKey(JSONKEY_CURRENT_PREKEY_ID);
			int reg_id;
			if (regIdString != null) {
				reg_id = Integer.valueOf(regIdString);
			} else {
				Log.w(Config.LOGTAG, AxolotlService.getLogprefix(account)+"Could not retrieve current prekey id for account " + account.getJid());
				reg_id = 0;
			}
			return reg_id;
		}

		public void regenerate() {
			mXmppConnectionService.databaseBackend.wipeAxolotlDb(account);
			account.setKey(JSONKEY_CURRENT_PREKEY_ID, Integer.toString(0));
			identityKeyPair = loadIdentityKeyPair();
			currentPreKeyId = 0;
			mXmppConnectionService.updateAccountUi();
		}

		/**
		 * Get the local client's identity key pair.
		 *
		 * @return The local client's persistent identity key pair.
		 */
		@Override
		public IdentityKeyPair getIdentityKeyPair() {
			if(identityKeyPair == null) {
				identityKeyPair = loadIdentityKeyPair();
			}
			return identityKeyPair;
		}

		/**
		 * Return the local client's registration ID.
		 * <p/>
		 * Clients should maintain a registration ID, a random number
		 * between 1 and 16380 that's generated once at install time.
		 *
		 * @return the local client's registration ID.
		 */
		@Override
		public int getLocalRegistrationId() {
			return localRegistrationId;
		}

		/**
		 * Save a remote client's identity key
		 * <p/>
		 * Store a remote client's identity key as trusted.
		 *
		 * @param name        The name of the remote client.
		 * @param identityKey The remote client's identity key.
		 */
		@Override
		public void saveIdentity(String name, IdentityKey identityKey) {
			if(!mXmppConnectionService.databaseBackend.loadIdentityKeys(account, name).contains(identityKey)) {
				mXmppConnectionService.databaseBackend.storeIdentityKey(account, name, identityKey);
			}
		}

		/**
		 * Verify a remote client's identity key.
		 * <p/>
		 * Determine whether a remote client's identity is trusted.  Convention is
		 * that the TextSecure protocol is 'trust on first use.'  This means that
		 * an identity key is considered 'trusted' if there is no entry for the recipient
		 * in the local store, or if it matches the saved key for a recipient in the local
		 * store.  Only if it mismatches an entry in the local store is it considered
		 * 'untrusted.'
		 *
		 * @param name        The name of the remote client.
		 * @param identityKey The identity key to verify.
		 * @return true if trusted, false if untrusted.
		 */
		@Override
		public boolean isTrustedIdentity(String name, IdentityKey identityKey) {
			return true;
		}

		public Trust getFingerprintTrust(String name, String fingerprint) {
			return mXmppConnectionService.databaseBackend.isIdentityKeyTrusted(account, name, fingerprint);
		}

		public void setFingerprintTrust(String name, String fingerprint, Trust trust) {
			mXmppConnectionService.databaseBackend.setIdentityKeyTrust(account, name, fingerprint, trust);
		}

		// --------------------------------------
		// SessionStore
		// --------------------------------------

		/**
		 * Returns a copy of the {@link SessionRecord} corresponding to the recipientId + deviceId tuple,
		 * or a new SessionRecord if one does not currently exist.
		 * <p/>
		 * It is important that implementations return a copy of the current durable information.  The
		 * returned SessionRecord may be modified, but those changes should not have an effect on the
		 * durable session state (what is returned by subsequent calls to this method) without the
		 * store method being called here first.
		 *
		 * @param address The name and device ID of the remote client.
		 * @return a copy of the SessionRecord corresponding to the recipientId + deviceId tuple, or
		 * a new SessionRecord if one does not currently exist.
		 */
		@Override
		public SessionRecord loadSession(AxolotlAddress address) {
			SessionRecord session = mXmppConnectionService.databaseBackend.loadSession(this.account, address);
			return (session != null) ? session : new SessionRecord();
		}

		/**
		 * Returns all known devices with active sessions for a recipient
		 *
		 * @param name the name of the client.
		 * @return all known sub-devices with active sessions.
		 */
		@Override
		public List<Integer> getSubDeviceSessions(String name) {
			return mXmppConnectionService.databaseBackend.getSubDeviceSessions(account,
					new AxolotlAddress(name, 0));
		}

		/**
		 * Commit to storage the {@link SessionRecord} for a given recipientId + deviceId tuple.
		 *
		 * @param address the address of the remote client.
		 * @param record  the current SessionRecord for the remote client.
		 */
		@Override
		public void storeSession(AxolotlAddress address, SessionRecord record) {
			mXmppConnectionService.databaseBackend.storeSession(account, address, record);
		}

		/**
		 * Determine whether there is a committed {@link SessionRecord} for a recipientId + deviceId tuple.
		 *
		 * @param address the address of the remote client.
		 * @return true if a {@link SessionRecord} exists, false otherwise.
		 */
		@Override
		public boolean containsSession(AxolotlAddress address) {
			return mXmppConnectionService.databaseBackend.containsSession(account, address);
		}

		/**
		 * Remove a {@link SessionRecord} for a recipientId + deviceId tuple.
		 *
		 * @param address the address of the remote client.
		 */
		@Override
		public void deleteSession(AxolotlAddress address) {
			mXmppConnectionService.databaseBackend.deleteSession(account, address);
		}

		/**
		 * Remove the {@link SessionRecord}s corresponding to all devices of a recipientId.
		 *
		 * @param name the name of the remote client.
		 */
		@Override
		public void deleteAllSessions(String name) {
			mXmppConnectionService.databaseBackend.deleteAllSessions(account,
					new AxolotlAddress(name, 0));
		}

		// --------------------------------------
		// PreKeyStore
		// --------------------------------------

		/**
		 * Load a local PreKeyRecord.
		 *
		 * @param preKeyId the ID of the local PreKeyRecord.
		 * @return the corresponding PreKeyRecord.
		 * @throws InvalidKeyIdException when there is no corresponding PreKeyRecord.
		 */
		@Override
		public PreKeyRecord loadPreKey(int preKeyId) throws InvalidKeyIdException {
			PreKeyRecord record = mXmppConnectionService.databaseBackend.loadPreKey(account, preKeyId);
			if (record == null) {
				throw new InvalidKeyIdException("No such PreKeyRecord: " + preKeyId);
			}
			return record;
		}

		/**
		 * Store a local PreKeyRecord.
		 *
		 * @param preKeyId the ID of the PreKeyRecord to store.
		 * @param record   the PreKeyRecord.
		 */
		@Override
		public void storePreKey(int preKeyId, PreKeyRecord record) {
			mXmppConnectionService.databaseBackend.storePreKey(account, record);
			currentPreKeyId = preKeyId;
			boolean success = this.account.setKey(JSONKEY_CURRENT_PREKEY_ID, Integer.toString(preKeyId));
			if (success) {
				mXmppConnectionService.databaseBackend.updateAccount(account);
			} else {
				Log.e(Config.LOGTAG, AxolotlService.getLogprefix(account)+"Failed to write new prekey id to the database!");
			}
		}

		/**
		 * @param preKeyId A PreKeyRecord ID.
		 * @return true if the store has a record for the preKeyId, otherwise false.
		 */
		@Override
		public boolean containsPreKey(int preKeyId) {
			return mXmppConnectionService.databaseBackend.containsPreKey(account, preKeyId);
		}

		/**
		 * Delete a PreKeyRecord from local storage.
		 *
		 * @param preKeyId The ID of the PreKeyRecord to remove.
		 */
		@Override
		public void removePreKey(int preKeyId) {
			mXmppConnectionService.databaseBackend.deletePreKey(account, preKeyId);
		}

		// --------------------------------------
		// SignedPreKeyStore
		// --------------------------------------

		/**
		 * Load a local SignedPreKeyRecord.
		 *
		 * @param signedPreKeyId the ID of the local SignedPreKeyRecord.
		 * @return the corresponding SignedPreKeyRecord.
		 * @throws InvalidKeyIdException when there is no corresponding SignedPreKeyRecord.
		 */
		@Override
		public SignedPreKeyRecord loadSignedPreKey(int signedPreKeyId) throws InvalidKeyIdException {
			SignedPreKeyRecord record = mXmppConnectionService.databaseBackend.loadSignedPreKey(account, signedPreKeyId);
			if (record == null) {
				throw new InvalidKeyIdException("No such SignedPreKeyRecord: " + signedPreKeyId);
			}
			return record;
		}

		/**
		 * Load all local SignedPreKeyRecords.
		 *
		 * @return All stored SignedPreKeyRecords.
		 */
		@Override
		public List<SignedPreKeyRecord> loadSignedPreKeys() {
			return mXmppConnectionService.databaseBackend.loadSignedPreKeys(account);
		}

		/**
		 * Store a local SignedPreKeyRecord.
		 *
		 * @param signedPreKeyId the ID of the SignedPreKeyRecord to store.
		 * @param record         the SignedPreKeyRecord.
		 */
		@Override
		public void storeSignedPreKey(int signedPreKeyId, SignedPreKeyRecord record) {
			mXmppConnectionService.databaseBackend.storeSignedPreKey(account, record);
		}

		/**
		 * @param signedPreKeyId A SignedPreKeyRecord ID.
		 * @return true if the store has a record for the signedPreKeyId, otherwise false.
		 */
		@Override
		public boolean containsSignedPreKey(int signedPreKeyId) {
			return mXmppConnectionService.databaseBackend.containsSignedPreKey(account, signedPreKeyId);
		}

		/**
		 * Delete a SignedPreKeyRecord from local storage.
		 *
		 * @param signedPreKeyId The ID of the SignedPreKeyRecord to remove.
		 */
		@Override
		public void removeSignedPreKey(int signedPreKeyId) {
			mXmppConnectionService.databaseBackend.deleteSignedPreKey(account, signedPreKeyId);
		}
	}

	public static class XmppAxolotlSession {
		private final SessionCipher cipher;
		private Integer preKeyId = null;
		private final SQLiteAxolotlStore sqLiteAxolotlStore;
		private final AxolotlAddress remoteAddress;
		private final Account account;
		private String fingerprint = null;

		public XmppAxolotlSession(Account account, SQLiteAxolotlStore store, AxolotlAddress remoteAddress, String fingerprint) {
			this(account, store, remoteAddress);
			this.fingerprint = fingerprint;
		}

		public XmppAxolotlSession(Account account, SQLiteAxolotlStore store, AxolotlAddress remoteAddress) {
			this.cipher = new SessionCipher(store, remoteAddress);
			this.remoteAddress = remoteAddress;
			this.sqLiteAxolotlStore = store;
			this.account = account;
		}

		public Integer getPreKeyId() {
			return preKeyId;
		}

		public void resetPreKeyId() {

			preKeyId = null;
		}

		public String getFingerprint() {
			return fingerprint;
		}

		public byte[] processReceiving(XmppAxolotlMessage.XmppAxolotlMessageHeader incomingHeader) {
			byte[] plaintext = null;
			try {
				try {
					PreKeyWhisperMessage message = new PreKeyWhisperMessage(incomingHeader.getContents());
					Log.i(Config.LOGTAG, AxolotlService.getLogprefix(account)+"PreKeyWhisperMessage received, new session ID:" + message.getSignedPreKeyId() + "/" + message.getPreKeyId());
					String fingerprint = message.getIdentityKey().getFingerprint().replaceAll("\\s", "");
					if (this.fingerprint != null && !this.fingerprint.equals(fingerprint)) {
						Log.e(Config.LOGTAG, AxolotlService.getLogprefix(account)+"Had session with fingerprint "+ this.fingerprint+", received message with fingerprint "+fingerprint);
					} else {
						this.fingerprint = fingerprint;
						plaintext = cipher.decrypt(message);
						if (message.getPreKeyId().isPresent()) {
							preKeyId = message.getPreKeyId().get();
						}
					}
				} catch (InvalidMessageException | InvalidVersionException e) {
					Log.i(Config.LOGTAG, AxolotlService.getLogprefix(account)+"WhisperMessage received");
					WhisperMessage message = new WhisperMessage(incomingHeader.getContents());
					plaintext = cipher.decrypt(message);
				} catch (InvalidKeyException | InvalidKeyIdException | UntrustedIdentityException e) {
					Log.w(Config.LOGTAG, AxolotlService.getLogprefix(account)+"Error decrypting axolotl header, "+e.getClass().getName()+": " + e.getMessage());
				}
			} catch (LegacyMessageException | InvalidMessageException | DuplicateMessageException | NoSessionException  e) {
				Log.w(Config.LOGTAG, AxolotlService.getLogprefix(account)+"Error decrypting axolotl header, "+e.getClass().getName()+": " + e.getMessage());
			}
			return plaintext;
		}

		public XmppAxolotlMessage.XmppAxolotlMessageHeader processSending(byte[] outgoingMessage) {
			CiphertextMessage ciphertextMessage = cipher.encrypt(outgoingMessage);
			XmppAxolotlMessage.XmppAxolotlMessageHeader header =
					new XmppAxolotlMessage.XmppAxolotlMessageHeader(remoteAddress.getDeviceId(),
							ciphertextMessage.serialize());
			return header;
		}
	}

	private static class AxolotlAddressMap<T> {
		protected Map<String, Map<Integer, T>> map;
		protected final Object MAP_LOCK = new Object();

		public AxolotlAddressMap() {
			this.map = new HashMap<>();
		}

		public void put(AxolotlAddress address, T value) {
			synchronized (MAP_LOCK) {
				Map<Integer, T> devices = map.get(address.getName());
				if (devices == null) {
					devices = new HashMap<>();
					map.put(address.getName(), devices);
				}
				devices.put(address.getDeviceId(), value);
			}
		}

		public T get(AxolotlAddress address) {
			synchronized (MAP_LOCK) {
				Map<Integer, T> devices = map.get(address.getName());
				if (devices == null) {
					return null;
				}
				return devices.get(address.getDeviceId());
			}
		}

		public Map<Integer, T> getAll(AxolotlAddress address) {
			synchronized (MAP_LOCK) {
				Map<Integer, T> devices = map.get(address.getName());
				if (devices == null) {
					return new HashMap<>();
				}
				return devices;
			}
		}

		public boolean hasAny(AxolotlAddress address) {
			synchronized (MAP_LOCK) {
				Map<Integer, T> devices = map.get(address.getName());
				return devices != null && !devices.isEmpty();
			}
		}


	}

	private static class SessionMap extends AxolotlAddressMap<XmppAxolotlSession> {

		public SessionMap(SQLiteAxolotlStore store, Account account) {
			super();
			this.fillMap(store, account);
		}

		private void fillMap(SQLiteAxolotlStore store, Account account) {
			for (Contact contact : account.getRoster().getContacts()) {
				Jid bareJid = contact.getJid().toBareJid();
				if (bareJid == null) {
					continue; // FIXME: handle this?
				}
				String address = bareJid.toString();
				List<Integer> deviceIDs = store.getSubDeviceSessions(address);
				for (Integer deviceId : deviceIDs) {
					AxolotlAddress axolotlAddress = new AxolotlAddress(address, deviceId);
					Log.d(Config.LOGTAG, AxolotlService.getLogprefix(account)+"Building session for remote address: "+axolotlAddress.toString());
					String fingerprint = store.loadSession(axolotlAddress).getSessionState().getRemoteIdentityKey().getFingerprint().replaceAll("\\s", "");
					this.put(axolotlAddress, new XmppAxolotlSession(account, store, axolotlAddress, fingerprint));
				}
			}
		}

	}

	private static enum FetchStatus {
		PENDING,
		SUCCESS,
		ERROR
	}

	private static class FetchStatusMap extends AxolotlAddressMap<FetchStatus> {

	}
	
	public static String getLogprefix(Account account) {
		return LOGPREFIX+" ("+account.getJid().toBareJid().toString()+"): ";
	}

	public AxolotlService(Account account, XmppConnectionService connectionService) {
		this.mXmppConnectionService = connectionService;
		this.account = account;
		this.axolotlStore = new SQLiteAxolotlStore(this.account, this.mXmppConnectionService);
		this.deviceIds = new HashMap<>();
		this.messageCache = new HashMap<>();
		this.sessions = new SessionMap(axolotlStore, account);
		this.fetchStatusMap = new FetchStatusMap();
		this.executor = new SerialSingleThreadExecutor();
		this.ownDeviceId = axolotlStore.getLocalRegistrationId();
	}

	public IdentityKey getOwnPublicKey() {
		return axolotlStore.getIdentityKeyPair().getPublicKey();
	}

	private AxolotlAddress getAddressForJid(Jid jid) {
		return new AxolotlAddress(jid.toString(), 0);
	}

	private Set<XmppAxolotlSession> findOwnSessions() {
		AxolotlAddress ownAddress = getAddressForJid(account.getJid().toBareJid());
		Set<XmppAxolotlSession> ownDeviceSessions = new HashSet<>(this.sessions.getAll(ownAddress).values());
		return ownDeviceSessions;
	}

	private Set<XmppAxolotlSession> findSessionsforContact(Contact contact) {
		AxolotlAddress contactAddress = getAddressForJid(contact.getJid());
		Set<XmppAxolotlSession> sessions = new HashSet<>(this.sessions.getAll(contactAddress).values());
		return sessions;
	}

	private boolean hasAny(Contact contact) {
		AxolotlAddress contactAddress = getAddressForJid(contact.getJid());
		return sessions.hasAny(contactAddress);
	}

	public void regenerateKeys() {
		axolotlStore.regenerate();
		publishBundlesIfNeeded();
	}

	public int getOwnDeviceId() {
		return ownDeviceId;
	}

	public Set<Integer> getOwnDeviceIds() {
		return this.deviceIds.get(account.getJid().toBareJid());
	}

	public void registerDevices(final Jid jid, @NonNull final Set<Integer> deviceIds) {
		if(deviceIds.contains(getOwnDeviceId())) {
			Log.d(Config.LOGTAG, AxolotlService.getLogprefix(account)+"Skipping own Device ID:"+ jid + ":"+getOwnDeviceId());
			deviceIds.remove(getOwnDeviceId());
		}
		for(Integer i:deviceIds) {
			Log.d(Config.LOGTAG, AxolotlService.getLogprefix(account)+"Adding Device ID:"+ jid + ":"+i);
		}
		this.deviceIds.put(jid, deviceIds);
		publishOwnDeviceIdIfNeeded();
	}

	public void wipeOtherPepDevices() {
		Set<Integer> deviceIds = new HashSet<>();
		deviceIds.add(getOwnDeviceId());
		IqPacket publish = mXmppConnectionService.getIqGenerator().publishDeviceIds(deviceIds);
		Log.d(Config.LOGTAG, AxolotlService.getLogprefix(account)+"Wiping all other devices from Pep:" + publish);
		mXmppConnectionService.sendIqPacket(account, publish, new OnIqPacketReceived() {
			@Override
			public void onIqPacketReceived(Account account, IqPacket packet) {
				// TODO: implement this!
			}
		});
	}

	public void publishOwnDeviceIdIfNeeded() {
		IqPacket packet = mXmppConnectionService.getIqGenerator().retrieveDeviceIds(account.getJid().toBareJid());
		mXmppConnectionService.sendIqPacket(account, packet, new OnIqPacketReceived() {
			@Override
			public void onIqPacketReceived(Account account, IqPacket packet) {
				Element item = mXmppConnectionService.getIqParser().getItem(packet);
				Set<Integer> deviceIds = mXmppConnectionService.getIqParser().deviceIds(item);
				if (deviceIds == null) {
					deviceIds = new HashSet<Integer>();
				}
				if (!deviceIds.contains(getOwnDeviceId())) {
					deviceIds.add(getOwnDeviceId());
					IqPacket publish = mXmppConnectionService.getIqGenerator().publishDeviceIds(deviceIds);
					Log.d(Config.LOGTAG, AxolotlService.getLogprefix(account)+"Own device " + getOwnDeviceId() + " not in PEP devicelist. Publishing: " + publish);
					mXmppConnectionService.sendIqPacket(account, publish, new OnIqPacketReceived() {
						@Override
						public void onIqPacketReceived(Account account, IqPacket packet) {
							// TODO: implement this!
						}
					});
				}
			}
		});
	}

	public void publishBundlesIfNeeded() {
		IqPacket packet = mXmppConnectionService.getIqGenerator().retrieveBundlesForDevice(account.getJid().toBareJid(), ownDeviceId);
		mXmppConnectionService.sendIqPacket(account, packet, new OnIqPacketReceived() {
			@Override
			public void onIqPacketReceived(Account account, IqPacket packet) {
				PreKeyBundle bundle = mXmppConnectionService.getIqParser().bundle(packet);
				Map<Integer, ECPublicKey> keys = mXmppConnectionService.getIqParser().preKeyPublics(packet);
				boolean flush = false;
				if (bundle == null) {
					Log.w(Config.LOGTAG, AxolotlService.getLogprefix(account)+"Received invalid bundle:" + packet);
					bundle = new PreKeyBundle(-1, -1, -1 , null, -1, null, null, null);
					flush = true;
				}
				if (keys == null) {
					Log.w(Config.LOGTAG, AxolotlService.getLogprefix(account)+"Received invalid prekeys:" + packet);
				}
				try {
					boolean changed = false;
					// Validate IdentityKey
					IdentityKeyPair identityKeyPair = axolotlStore.getIdentityKeyPair();
					if (flush || !identityKeyPair.getPublicKey().equals(bundle.getIdentityKey())) {
						Log.i(Config.LOGTAG, AxolotlService.getLogprefix(account)+"Adding own IdentityKey " + identityKeyPair.getPublicKey() + " to PEP.");
						changed = true;
					}

					// Validate signedPreKeyRecord + ID
					SignedPreKeyRecord signedPreKeyRecord;
					int numSignedPreKeys = axolotlStore.loadSignedPreKeys().size();
					try {
						signedPreKeyRecord = axolotlStore.loadSignedPreKey(bundle.getSignedPreKeyId());
						if ( flush
								||!bundle.getSignedPreKey().equals(signedPreKeyRecord.getKeyPair().getPublicKey())
								|| !Arrays.equals(bundle.getSignedPreKeySignature(), signedPreKeyRecord.getSignature())) {
							Log.i(Config.LOGTAG, AxolotlService.getLogprefix(account)+"Adding new signedPreKey with ID " + (numSignedPreKeys + 1) + " to PEP.");
							signedPreKeyRecord = KeyHelper.generateSignedPreKey(identityKeyPair, numSignedPreKeys + 1);
							axolotlStore.storeSignedPreKey(signedPreKeyRecord.getId(), signedPreKeyRecord);
							changed = true;
						}
					} catch (InvalidKeyIdException e) {
						Log.i(Config.LOGTAG, AxolotlService.getLogprefix(account)+"Adding new signedPreKey with ID " + (numSignedPreKeys + 1) + " to PEP.");
						signedPreKeyRecord = KeyHelper.generateSignedPreKey(identityKeyPair, numSignedPreKeys + 1);
						axolotlStore.storeSignedPreKey(signedPreKeyRecord.getId(), signedPreKeyRecord);
						changed = true;
					}

					// Validate PreKeys
					Set<PreKeyRecord> preKeyRecords = new HashSet<>();
					if (keys != null) {
						for (Integer id : keys.keySet()) {
							try {
								PreKeyRecord preKeyRecord = axolotlStore.loadPreKey(id);
								if (preKeyRecord.getKeyPair().getPublicKey().equals(keys.get(id))) {
									preKeyRecords.add(preKeyRecord);
								}
							} catch (InvalidKeyIdException ignored) {
							}
						}
					}
					int newKeys = NUM_KEYS_TO_PUBLISH - preKeyRecords.size();
					if (newKeys > 0) {
						List<PreKeyRecord> newRecords = KeyHelper.generatePreKeys(
								axolotlStore.getCurrentPreKeyId()+1, newKeys);
						preKeyRecords.addAll(newRecords);
						for (PreKeyRecord record : newRecords) {
							axolotlStore.storePreKey(record.getId(), record);
						}
						changed = true;
						Log.i(Config.LOGTAG, AxolotlService.getLogprefix(account)+"Adding " + newKeys + " new preKeys to PEP.");
					}


					if(changed) {
						IqPacket publish = mXmppConnectionService.getIqGenerator().publishBundles(
								signedPreKeyRecord, axolotlStore.getIdentityKeyPair().getPublicKey(),
								preKeyRecords, ownDeviceId);
						Log.d(Config.LOGTAG, AxolotlService.getLogprefix(account)+ ": Bundle " + getOwnDeviceId() + " in PEP not current. Publishing: " + publish);
						mXmppConnectionService.sendIqPacket(account, publish, new OnIqPacketReceived() {
							@Override
							public void onIqPacketReceived(Account account, IqPacket packet) {
								// TODO: implement this!
								Log.d(Config.LOGTAG, AxolotlService.getLogprefix(account)+"Published bundle, got: " + packet);
							}
						});
					}
				} catch (InvalidKeyException e) {
						Log.e(Config.LOGTAG, AxolotlService.getLogprefix(account)+"Failed to publish bundle " + getOwnDeviceId() + ", reason: " + e.getMessage());
						return;
				}
			}
		});
	}

	public boolean isContactAxolotlCapable(Contact contact) {

		Jid jid = contact.getJid().toBareJid();
		AxolotlAddress address = new AxolotlAddress(jid.toString(), 0);
		return sessions.hasAny(address) ||
				( deviceIds.containsKey(jid) && !deviceIds.get(jid).isEmpty());
	}
	public SQLiteAxolotlStore.Trust getFingerprintTrust(String name, String fingerprint) {
		return axolotlStore.getFingerprintTrust(name, fingerprint);
	}

	public void setFingerprintTrust(String name, String fingerprint, SQLiteAxolotlStore.Trust trust) {
		axolotlStore.setFingerprintTrust(name, fingerprint, trust);
	}

	private void buildSessionFromPEP(final Conversation conversation, final AxolotlAddress address) {
		Log.i(Config.LOGTAG, AxolotlService.getLogprefix(account)+"Building new sesstion for " + address.getDeviceId());

		try {
			IqPacket bundlesPacket = mXmppConnectionService.getIqGenerator().retrieveBundlesForDevice(
					Jid.fromString(address.getName()), address.getDeviceId());
			Log.d(Config.LOGTAG, AxolotlService.getLogprefix(account)+"Retrieving bundle: " + bundlesPacket);
			mXmppConnectionService.sendIqPacket(account, bundlesPacket, new OnIqPacketReceived() {
				@Override
				public void onIqPacketReceived(Account account, IqPacket packet) {
					Log.d(Config.LOGTAG, AxolotlService.getLogprefix(account)+"Received preKey IQ packet, processing...");
					final IqParser parser = mXmppConnectionService.getIqParser();
					final List<PreKeyBundle> preKeyBundleList = parser.preKeys(packet);
					final PreKeyBundle bundle = parser.bundle(packet);
					if (preKeyBundleList.isEmpty() || bundle == null) {
						Log.e(Config.LOGTAG, AxolotlService.getLogprefix(account)+"preKey IQ packet invalid: " + packet);
						fetchStatusMap.put(address, FetchStatus.ERROR);
						return;
					}
					Random random = new Random();
					final PreKeyBundle preKey = preKeyBundleList.get(random.nextInt(preKeyBundleList.size()));
					if (preKey == null) {
						//should never happen
						fetchStatusMap.put(address, FetchStatus.ERROR);
						return;
					}

					final PreKeyBundle preKeyBundle = new PreKeyBundle(0, address.getDeviceId(),
							preKey.getPreKeyId(), preKey.getPreKey(),
							bundle.getSignedPreKeyId(), bundle.getSignedPreKey(),
							bundle.getSignedPreKeySignature(), bundle.getIdentityKey());

					axolotlStore.saveIdentity(address.getName(), bundle.getIdentityKey());

					try {
						SessionBuilder builder = new SessionBuilder(axolotlStore, address);
						builder.process(preKeyBundle);
						XmppAxolotlSession session = new XmppAxolotlSession(account, axolotlStore, address, bundle.getIdentityKey().getFingerprint().replaceAll("\\s", ""));
						sessions.put(address, session);
						fetchStatusMap.put(address, FetchStatus.SUCCESS);
					} catch (UntrustedIdentityException|InvalidKeyException e) {
						Log.e(Config.LOGTAG, AxolotlService.getLogprefix(account)+"Error building session for " + address + ": "
								+ e.getClass().getName() + ", " + e.getMessage());
						fetchStatusMap.put(address, FetchStatus.ERROR);
					}

					AxolotlAddress ownAddress = new AxolotlAddress(conversation.getAccount().getJid().toBareJid().toString(),0);
					AxolotlAddress foreignAddress = new AxolotlAddress(conversation.getJid().toBareJid().toString(),0);
					if (!fetchStatusMap.getAll(ownAddress).containsValue(FetchStatus.PENDING)
							&& !fetchStatusMap.getAll(foreignAddress).containsValue(FetchStatus.PENDING)) {
						conversation.findUnsentMessagesWithEncryption(Message.ENCRYPTION_AXOLOTL,
								new Conversation.OnMessageFound() {
									@Override
									public void onMessageFound(Message message) {
										processSending(message);
									}
								});
					}
				}
			});
		} catch (InvalidJidException e) {
			Log.e(Config.LOGTAG, AxolotlService.getLogprefix(account)+"Got address with invalid jid: " + address.getName());
		}
	}

	private boolean createSessionsIfNeeded(Conversation conversation) {
		boolean newSessions = false;
		Log.i(Config.LOGTAG, AxolotlService.getLogprefix(account)+"Creating axolotl sessions if needed...");
		Jid contactJid = conversation.getContact().getJid().toBareJid();
		Set<AxolotlAddress> addresses = new HashSet<>();
		if(deviceIds.get(contactJid) != null) {
			for(Integer foreignId:this.deviceIds.get(contactJid)) {
				Log.d(Config.LOGTAG, AxolotlService.getLogprefix(account)+"Found device "+account.getJid().toBareJid()+":"+foreignId);
				addresses.add(new AxolotlAddress(contactJid.toString(), foreignId));
			}
		} else {
			Log.w(Config.LOGTAG, AxolotlService.getLogprefix(account) + "Have no target devices in PEP!");
		}
		Log.d(Config.LOGTAG, AxolotlService.getLogprefix(account)+"Checking own account "+account.getJid().toBareJid());
		if(deviceIds.get(account.getJid().toBareJid()) != null) {
			for(Integer ownId:this.deviceIds.get(account.getJid().toBareJid())) {
				Log.d(Config.LOGTAG, AxolotlService.getLogprefix(account)+"Found device "+account.getJid().toBareJid()+":"+ownId);
				addresses.add(new AxolotlAddress(account.getJid().toBareJid().toString(), ownId));
			}
		}
		for (AxolotlAddress address : addresses) {
			Log.d(Config.LOGTAG, AxolotlService.getLogprefix(account)+"Processing device: " + address.toString());
			FetchStatus status = fetchStatusMap.get(address);
			XmppAxolotlSession session = sessions.get(address);
			if ( session == null && ( status == null || status == FetchStatus.ERROR) ) {
				fetchStatusMap.put(address, FetchStatus.PENDING);
				this.buildSessionFromPEP(conversation,  address);
				newSessions = true;
			} else {
				Log.d(Config.LOGTAG, AxolotlService.getLogprefix(account)+"Already have session for " +  address.toString());
			}
		}
		return newSessions;
	}

	@Nullable
	public XmppAxolotlMessage encrypt(Message message ){
		final XmppAxolotlMessage axolotlMessage = new XmppAxolotlMessage(message.getContact().getJid().toBareJid(),
				ownDeviceId, message.getBody());

		if(findSessionsforContact(message.getContact()).isEmpty()) {
			return null;
		}
		Log.d(Config.LOGTAG, AxolotlService.getLogprefix(account)+"Building axolotl foreign headers...");
		for (XmppAxolotlSession session : findSessionsforContact(message.getContact())) {
			Log.v(Config.LOGTAG, AxolotlService.getLogprefix(account)+session.remoteAddress.toString());
			//if(!session.isTrusted()) {
			// TODO: handle this properly
			//              continue;
			//        }
			axolotlMessage.addHeader(session.processSending(axolotlMessage.getInnerKey()));
		}
		Log.d(Config.LOGTAG, AxolotlService.getLogprefix(account)+"Building axolotl own headers...");
		for (XmppAxolotlSession session : findOwnSessions()) {
			Log.v(Config.LOGTAG, AxolotlService.getLogprefix(account)+session.remoteAddress.toString());
			//        if(!session.isTrusted()) {
			// TODO: handle this properly
			//          continue;
			//    }
			axolotlMessage.addHeader(session.processSending(axolotlMessage.getInnerKey()));
		}

		return axolotlMessage;
	}

	private void processSending(final Message message) {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				MessagePacket packet = mXmppConnectionService.getMessageGenerator()
						.generateAxolotlChat(message);
				if (packet == null) {
					mXmppConnectionService.markMessage(message, Message.STATUS_SEND_FAILED);
					//mXmppConnectionService.updateConversationUi();
				} else {
					Log.d(Config.LOGTAG, AxolotlService.getLogprefix(account)+"Generated message, caching: " + message.getUuid());
					messageCache.put(message.getUuid(), packet);
					mXmppConnectionService.resendMessage(message);
				}
			}
		});
	}

	public void prepareMessage(Message message) {
		if (!messageCache.containsKey(message.getUuid())) {
			boolean newSessions = createSessionsIfNeeded(message.getConversation());

			if (!newSessions) {
				this.processSending(message);
			}
		}
	}

	public MessagePacket fetchPacketFromCache(Message message) {
		MessagePacket packet = messageCache.get(message.getUuid());
		if (packet != null) {
			Log.d(Config.LOGTAG, AxolotlService.getLogprefix(account)+"Cache hit: " + message.getUuid());
			messageCache.remove(message.getUuid());
		} else {
			Log.d(Config.LOGTAG, AxolotlService.getLogprefix(account)+"Cache miss: " + message.getUuid());
		}
		return packet;
	}

	public XmppAxolotlMessage.XmppAxolotlPlaintextMessage processReceiving(XmppAxolotlMessage message) {
		XmppAxolotlMessage.XmppAxolotlPlaintextMessage plaintextMessage = null;
		AxolotlAddress senderAddress = new AxolotlAddress(message.getFrom().toString(),
				message.getSenderDeviceId());

		boolean newSession = false;
		XmppAxolotlSession session = sessions.get(senderAddress);
		if (session == null) {
			Log.d(Config.LOGTAG, AxolotlService.getLogprefix(account)+"Account: "+account.getJid()+" No axolotl session found while parsing received message " + message);
			// TODO: handle this properly
			session = new XmppAxolotlSession(account, axolotlStore, senderAddress);
			newSession = true;
		}

		for (XmppAxolotlMessage.XmppAxolotlMessageHeader header : message.getHeaders()) {
			if (header.getRecipientDeviceId() == ownDeviceId) {
				Log.d(Config.LOGTAG, AxolotlService.getLogprefix(account)+"Found axolotl header matching own device ID, processing...");
				byte[] payloadKey = session.processReceiving(header);
				if (payloadKey != null) {
					Log.d(Config.LOGTAG, AxolotlService.getLogprefix(account)+"Got payload key from axolotl header. Decrypting message...");
					plaintextMessage = message.decrypt(session, payloadKey, session.getFingerprint());
				}
				Integer preKeyId = session.getPreKeyId();
				if (preKeyId != null) {
					publishBundlesIfNeeded();
					session.resetPreKeyId();
				}
				break;
			}
		}

		if (newSession && plaintextMessage != null) {
			sessions.put(senderAddress,session);
		}

		return plaintextMessage;
	}
}
