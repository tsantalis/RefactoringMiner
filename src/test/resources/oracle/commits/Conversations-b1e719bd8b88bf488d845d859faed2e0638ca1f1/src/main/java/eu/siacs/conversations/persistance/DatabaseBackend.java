package eu.siacs.conversations.persistance;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Base64;
import android.util.Log;

import org.whispersystems.libaxolotl.AxolotlAddress;
import org.whispersystems.libaxolotl.IdentityKey;
import org.whispersystems.libaxolotl.IdentityKeyPair;
import org.whispersystems.libaxolotl.InvalidKeyException;
import org.whispersystems.libaxolotl.state.PreKeyRecord;
import org.whispersystems.libaxolotl.state.SessionRecord;
import org.whispersystems.libaxolotl.state.SignedPreKeyRecord;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import eu.siacs.conversations.Config;
import eu.siacs.conversations.crypto.axolotl.AxolotlService;
import eu.siacs.conversations.entities.Account;
import eu.siacs.conversations.entities.Contact;
import eu.siacs.conversations.entities.Conversation;
import eu.siacs.conversations.entities.Message;
import eu.siacs.conversations.entities.Roster;
import eu.siacs.conversations.xmpp.jid.InvalidJidException;
import eu.siacs.conversations.xmpp.jid.Jid;

public class DatabaseBackend extends SQLiteOpenHelper {

	private static DatabaseBackend instance = null;

	private static final String DATABASE_NAME = "history";
	private static final int DATABASE_VERSION = 15;

	private static String CREATE_CONTATCS_STATEMENT = "create table "
			+ Contact.TABLENAME + "(" + Contact.ACCOUNT + " TEXT, "
			+ Contact.SERVERNAME + " TEXT, " + Contact.SYSTEMNAME + " TEXT,"
			+ Contact.JID + " TEXT," + Contact.KEYS + " TEXT,"
			+ Contact.PHOTOURI + " TEXT," + Contact.OPTIONS + " NUMBER,"
			+ Contact.SYSTEMACCOUNT + " NUMBER, " + Contact.AVATAR + " TEXT, "
			+ Contact.LAST_PRESENCE + " TEXT, " + Contact.LAST_TIME + " NUMBER, "
			+ Contact.GROUPS + " TEXT, FOREIGN KEY(" + Contact.ACCOUNT + ") REFERENCES "
			+ Account.TABLENAME + "(" + Account.UUID
			+ ") ON DELETE CASCADE, UNIQUE(" + Contact.ACCOUNT + ", "
			+ Contact.JID + ") ON CONFLICT REPLACE);";

	private static String CREATE_PREKEYS_STATEMENT = "CREATE TABLE "
			+ AxolotlService.SQLiteAxolotlStore.PREKEY_TABLENAME + "("
				+ AxolotlService.SQLiteAxolotlStore.ACCOUNT + " TEXT,  "
				+ AxolotlService.SQLiteAxolotlStore.ID + " INTEGER, "
				+ AxolotlService.SQLiteAxolotlStore.KEY + " TEXT, FOREIGN KEY("
					+ AxolotlService.SQLiteAxolotlStore.ACCOUNT
				+ ") REFERENCES " + Account.TABLENAME + "(" + Account.UUID + ") ON DELETE CASCADE, "
				+ "UNIQUE( " + AxolotlService.SQLiteAxolotlStore.ACCOUNT + ", "
					+ AxolotlService.SQLiteAxolotlStore.ID
				+ ") ON CONFLICT REPLACE"
			+");";

	private static String CREATE_SIGNED_PREKEYS_STATEMENT = "CREATE TABLE "
			+ AxolotlService.SQLiteAxolotlStore.SIGNED_PREKEY_TABLENAME + "("
				+ AxolotlService.SQLiteAxolotlStore.ACCOUNT + " TEXT,  "
				+ AxolotlService.SQLiteAxolotlStore.ID + " INTEGER, "
				+ AxolotlService.SQLiteAxolotlStore.KEY + " TEXT, FOREIGN KEY("
					+ AxolotlService.SQLiteAxolotlStore.ACCOUNT
				+ ") REFERENCES " + Account.TABLENAME + "(" + Account.UUID + ") ON DELETE CASCADE, "
				+ "UNIQUE( " + AxolotlService.SQLiteAxolotlStore.ACCOUNT + ", "
					+ AxolotlService.SQLiteAxolotlStore.ID
				+ ") ON CONFLICT REPLACE"+
			");";

	private static String CREATE_SESSIONS_STATEMENT = "CREATE TABLE "
			+ AxolotlService.SQLiteAxolotlStore.SESSION_TABLENAME + "("
				+ AxolotlService.SQLiteAxolotlStore.ACCOUNT + " TEXT,  "
				+ AxolotlService.SQLiteAxolotlStore.NAME + " TEXT, "
				+ AxolotlService.SQLiteAxolotlStore.DEVICE_ID + " INTEGER, "
				+ AxolotlService.SQLiteAxolotlStore.TRUSTED + " INTEGER, "
				+ AxolotlService.SQLiteAxolotlStore.KEY + " TEXT, FOREIGN KEY("
					+ AxolotlService.SQLiteAxolotlStore.ACCOUNT
				+ ") REFERENCES " + Account.TABLENAME + "(" + Account.UUID + ") ON DELETE CASCADE, "
				+ "UNIQUE( " + AxolotlService.SQLiteAxolotlStore.ACCOUNT + ", "
					+ AxolotlService.SQLiteAxolotlStore.NAME + ", "
					+ AxolotlService.SQLiteAxolotlStore.DEVICE_ID
				+ ") ON CONFLICT REPLACE"
			+");";

	private static String CREATE_IDENTITIES_STATEMENT = "CREATE TABLE "
			+ AxolotlService.SQLiteAxolotlStore.IDENTITIES_TABLENAME + "("
			+ AxolotlService.SQLiteAxolotlStore.ACCOUNT + " TEXT,  "
			+ AxolotlService.SQLiteAxolotlStore.NAME + " TEXT, "
			+ AxolotlService.SQLiteAxolotlStore.OWN + " INTEGER, "
			+ AxolotlService.SQLiteAxolotlStore.KEY + " TEXT, FOREIGN KEY("
			+ AxolotlService.SQLiteAxolotlStore.ACCOUNT
			+ ") REFERENCES " + Account.TABLENAME + "(" + Account.UUID + ") ON DELETE CASCADE "
			+");";

	private DatabaseBackend(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("PRAGMA foreign_keys=ON;");
		db.execSQL("create table " + Account.TABLENAME + "(" + Account.UUID
				+ " TEXT PRIMARY KEY," + Account.USERNAME + " TEXT,"
				+ Account.SERVER + " TEXT," + Account.PASSWORD + " TEXT,"
				+ Account.ROSTERVERSION + " TEXT," + Account.OPTIONS
				+ " NUMBER, " + Account.AVATAR + " TEXT, " + Account.KEYS
				+ " TEXT)");
		db.execSQL("create table " + Conversation.TABLENAME + " ("
				+ Conversation.UUID + " TEXT PRIMARY KEY, " + Conversation.NAME
				+ " TEXT, " + Conversation.CONTACT + " TEXT, "
				+ Conversation.ACCOUNT + " TEXT, " + Conversation.CONTACTJID
				+ " TEXT, " + Conversation.CREATED + " NUMBER, "
				+ Conversation.STATUS + " NUMBER, " + Conversation.MODE
				+ " NUMBER, " + Conversation.ATTRIBUTES + " TEXT, FOREIGN KEY("
				+ Conversation.ACCOUNT + ") REFERENCES " + Account.TABLENAME
				+ "(" + Account.UUID + ") ON DELETE CASCADE);");
		db.execSQL("create table " + Message.TABLENAME + "( " + Message.UUID
				+ " TEXT PRIMARY KEY, " + Message.CONVERSATION + " TEXT, "
				+ Message.TIME_SENT + " NUMBER, " + Message.COUNTERPART
				+ " TEXT, " + Message.TRUE_COUNTERPART + " TEXT,"
				+ Message.BODY + " TEXT, " + Message.ENCRYPTION + " NUMBER, "
				+ Message.STATUS + " NUMBER," + Message.TYPE + " NUMBER, "
				+ Message.RELATIVE_FILE_PATH + " TEXT, "
				+ Message.SERVER_MSG_ID + " TEXT, "
				+ Message.REMOTE_MSG_ID + " TEXT, FOREIGN KEY("
				+ Message.CONVERSATION + ") REFERENCES "
				+ Conversation.TABLENAME + "(" + Conversation.UUID
				+ ") ON DELETE CASCADE);");

		db.execSQL(CREATE_CONTATCS_STATEMENT);
		db.execSQL(CREATE_SESSIONS_STATEMENT);
		db.execSQL(CREATE_PREKEYS_STATEMENT);
		db.execSQL(CREATE_SIGNED_PREKEYS_STATEMENT);
		db.execSQL(CREATE_IDENTITIES_STATEMENT);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion < 2 && newVersion >= 2) {
			db.execSQL("update " + Account.TABLENAME + " set "
					+ Account.OPTIONS + " = " + Account.OPTIONS + " | 8");
		}
		if (oldVersion < 3 && newVersion >= 3) {
			db.execSQL("ALTER TABLE " + Message.TABLENAME + " ADD COLUMN "
					+ Message.TYPE + " NUMBER");
		}
		if (oldVersion < 5 && newVersion >= 5) {
			db.execSQL("DROP TABLE " + Contact.TABLENAME);
			db.execSQL(CREATE_CONTATCS_STATEMENT);
			db.execSQL("UPDATE " + Account.TABLENAME + " SET "
					+ Account.ROSTERVERSION + " = NULL");
		}
		if (oldVersion < 6 && newVersion >= 6) {
			db.execSQL("ALTER TABLE " + Message.TABLENAME + " ADD COLUMN "
					+ Message.TRUE_COUNTERPART + " TEXT");
		}
		if (oldVersion < 7 && newVersion >= 7) {
			db.execSQL("ALTER TABLE " + Message.TABLENAME + " ADD COLUMN "
					+ Message.REMOTE_MSG_ID + " TEXT");
			db.execSQL("ALTER TABLE " + Contact.TABLENAME + " ADD COLUMN "
					+ Contact.AVATAR + " TEXT");
			db.execSQL("ALTER TABLE " + Account.TABLENAME + " ADD COLUMN "
					+ Account.AVATAR + " TEXT");
		}
		if (oldVersion < 8 && newVersion >= 8) {
			db.execSQL("ALTER TABLE " + Conversation.TABLENAME + " ADD COLUMN "
					+ Conversation.ATTRIBUTES + " TEXT");
		}
		if (oldVersion < 9 && newVersion >= 9) {
			db.execSQL("ALTER TABLE " + Contact.TABLENAME + " ADD COLUMN "
					+ Contact.LAST_TIME + " NUMBER");
			db.execSQL("ALTER TABLE " + Contact.TABLENAME + " ADD COLUMN "
					+ Contact.LAST_PRESENCE + " TEXT");
		}
		if (oldVersion < 10 && newVersion >= 10) {
			db.execSQL("ALTER TABLE " + Message.TABLENAME + " ADD COLUMN "
					+ Message.RELATIVE_FILE_PATH + " TEXT");
		}
		if (oldVersion < 11 && newVersion >= 11) {
			db.execSQL("ALTER TABLE " + Contact.TABLENAME + " ADD COLUMN "
					+ Contact.GROUPS + " TEXT");
			db.execSQL("delete from "+Contact.TABLENAME);
			db.execSQL("update "+Account.TABLENAME+" set "+Account.ROSTERVERSION+" = NULL");
		}
		if (oldVersion < 12 && newVersion >= 12) {
			db.execSQL("ALTER TABLE " + Message.TABLENAME + " ADD COLUMN "
					+ Message.SERVER_MSG_ID + " TEXT");
		}
		if (oldVersion < 13 && newVersion >= 13) {
			db.execSQL("delete from "+Contact.TABLENAME);
			db.execSQL("update "+Account.TABLENAME+" set "+Account.ROSTERVERSION+" = NULL");
		}
		if (oldVersion < 14 && newVersion >= 14) {
			// migrate db to new, canonicalized JID domainpart representation

			// Conversation table
			Cursor cursor = db.rawQuery("select * from " + Conversation.TABLENAME, new String[0]);
			while(cursor.moveToNext()) {
				String newJid;
				try {
					newJid = Jid.fromString(
							cursor.getString(cursor.getColumnIndex(Conversation.CONTACTJID))
					).toString();
				} catch (InvalidJidException ignored) {
					Log.e(Config.LOGTAG, "Failed to migrate Conversation CONTACTJID "
							+cursor.getString(cursor.getColumnIndex(Conversation.CONTACTJID))
							+": " + ignored +". Skipping...");
					continue;
				}

				String updateArgs[] = {
						newJid,
						cursor.getString(cursor.getColumnIndex(Conversation.UUID)),
				};
				db.execSQL("update " + Conversation.TABLENAME
						+ " set " + Conversation.CONTACTJID	+ " = ? "
						+ " where " + Conversation.UUID + " = ?", updateArgs);
			}
			cursor.close();

			// Contact table
			cursor = db.rawQuery("select * from " + Contact.TABLENAME, new String[0]);
			while(cursor.moveToNext()) {
				String newJid;
				try {
					newJid = Jid.fromString(
							cursor.getString(cursor.getColumnIndex(Contact.JID))
					).toString();
				} catch (InvalidJidException ignored) {
					Log.e(Config.LOGTAG, "Failed to migrate Contact JID "
							+cursor.getString(cursor.getColumnIndex(Contact.JID))
							+": " + ignored +". Skipping...");
					continue;
				}

				String updateArgs[] = {
						newJid,
						cursor.getString(cursor.getColumnIndex(Contact.ACCOUNT)),
						cursor.getString(cursor.getColumnIndex(Contact.JID)),
				};
				db.execSQL("update " + Contact.TABLENAME
						+ " set " + Contact.JID + " = ? "
						+ " where " + Contact.ACCOUNT + " = ? "
						+ " AND " + Contact.JID + " = ?", updateArgs);
			}
			cursor.close();

			// Account table
			cursor = db.rawQuery("select * from " + Account.TABLENAME, new String[0]);
			while(cursor.moveToNext()) {
				String newServer;
				try {
					newServer = Jid.fromParts(
							cursor.getString(cursor.getColumnIndex(Account.USERNAME)),
							cursor.getString(cursor.getColumnIndex(Account.SERVER)),
							"mobile"
					).getDomainpart();
				} catch (InvalidJidException ignored) {
					Log.e(Config.LOGTAG, "Failed to migrate Account SERVER "
							+cursor.getString(cursor.getColumnIndex(Account.SERVER))
							+": " + ignored +". Skipping...");
					continue;
				}

				String updateArgs[] = {
						newServer,
						cursor.getString(cursor.getColumnIndex(Account.UUID)),
				};
				db.execSQL("update " + Account.TABLENAME
						+ " set " + Account.SERVER + " = ? "
						+ " where " + Account.UUID + " = ?", updateArgs);
			}
			cursor.close();
		}
		if (oldVersion < 15  && newVersion >= 15) {
			recreateAxolotlDb();
		}
	}

	public static synchronized DatabaseBackend getInstance(Context context) {
		if (instance == null) {
			instance = new DatabaseBackend(context);
		}
		return instance;
	}

	public void createConversation(Conversation conversation) {
		SQLiteDatabase db = this.getWritableDatabase();
		db.insert(Conversation.TABLENAME, null, conversation.getContentValues());
	}

	public void createMessage(Message message) {
		SQLiteDatabase db = this.getWritableDatabase();
		db.insert(Message.TABLENAME, null, message.getContentValues());
	}

	public void createAccount(Account account) {
		SQLiteDatabase db = this.getWritableDatabase();
		db.insert(Account.TABLENAME, null, account.getContentValues());
	}

	public void createContact(Contact contact) {
		SQLiteDatabase db = this.getWritableDatabase();
		db.insert(Contact.TABLENAME, null, contact.getContentValues());
	}

	public int getConversationCount() {
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery("select count(uuid) as count from "
				+ Conversation.TABLENAME + " where " + Conversation.STATUS
				+ "=" + Conversation.STATUS_AVAILABLE, null);
		cursor.moveToFirst();
		int count = cursor.getInt(0);
		cursor.close();
		return count;
	}

	public CopyOnWriteArrayList<Conversation> getConversations(int status) {
		CopyOnWriteArrayList<Conversation> list = new CopyOnWriteArrayList<>();
		SQLiteDatabase db = this.getReadableDatabase();
		String[] selectionArgs = { Integer.toString(status) };
		Cursor cursor = db.rawQuery("select * from " + Conversation.TABLENAME
				+ " where " + Conversation.STATUS + " = ? order by "
				+ Conversation.CREATED + " desc", selectionArgs);
		while (cursor.moveToNext()) {
			list.add(Conversation.fromCursor(cursor));
		}
		cursor.close();
		return list;
	}

	public ArrayList<Message> getMessages(Conversation conversations, int limit) {
		return getMessages(conversations, limit, -1);
	}

	public ArrayList<Message> getMessages(Conversation conversation, int limit,
			long timestamp) {
		ArrayList<Message> list = new ArrayList<>();
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor;
		if (timestamp == -1) {
			String[] selectionArgs = { conversation.getUuid() };
			cursor = db.query(Message.TABLENAME, null, Message.CONVERSATION
					+ "=?", selectionArgs, null, null, Message.TIME_SENT
					+ " DESC", String.valueOf(limit));
		} else {
			String[] selectionArgs = { conversation.getUuid(),
					Long.toString(timestamp) };
			cursor = db.query(Message.TABLENAME, null, Message.CONVERSATION
					+ "=? and " + Message.TIME_SENT + "<?", selectionArgs,
					null, null, Message.TIME_SENT + " DESC",
					String.valueOf(limit));
		}
		if (cursor.getCount() > 0) {
			cursor.moveToLast();
			do {
				Message message = Message.fromCursor(cursor);
				message.setConversation(conversation);
				list.add(message);
			} while (cursor.moveToPrevious());
		}
		cursor.close();
		return list;
	}

	public Conversation findConversation(final Account account, final Jid contactJid) {
		SQLiteDatabase db = this.getReadableDatabase();
		String[] selectionArgs = { account.getUuid(),
				contactJid.toBareJid().toString() + "/%",
				contactJid.toBareJid().toString()
				};
		Cursor cursor = db.query(Conversation.TABLENAME, null,
				Conversation.ACCOUNT + "=? AND (" + Conversation.CONTACTJID
						+ " like ? OR " + Conversation.CONTACTJID + "=?)", selectionArgs, null, null, null);
		if (cursor.getCount() == 0)
			return null;
		cursor.moveToFirst();
		Conversation conversation = Conversation.fromCursor(cursor);
		cursor.close();
		return conversation;
	}

	public void updateConversation(final Conversation conversation) {
		final SQLiteDatabase db = this.getWritableDatabase();
		final String[] args = { conversation.getUuid() };
		db.update(Conversation.TABLENAME, conversation.getContentValues(),
				Conversation.UUID + "=?", args);
	}

	public List<Account> getAccounts() {
		List<Account> list = new ArrayList<>();
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.query(Account.TABLENAME, null, null, null, null,
				null, null);
		while (cursor.moveToNext()) {
			list.add(Account.fromCursor(cursor));
		}
		cursor.close();
		return list;
	}

	public void updateAccount(Account account) {
		SQLiteDatabase db = this.getWritableDatabase();
		String[] args = { account.getUuid() };
		db.update(Account.TABLENAME, account.getContentValues(), Account.UUID
				+ "=?", args);
	}

	public void deleteAccount(Account account) {
		SQLiteDatabase db = this.getWritableDatabase();
		String[] args = { account.getUuid() };
		db.delete(Account.TABLENAME, Account.UUID + "=?", args);
	}

	public boolean hasEnabledAccounts() {
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery("select count(" + Account.UUID + ")  from "
				+ Account.TABLENAME + " where not options & (1 <<1)", null);
		try {
			cursor.moveToFirst();
			int count = cursor.getInt(0);
			cursor.close();
			return (count > 0);
		} catch (SQLiteCantOpenDatabaseException e) {
			return true; // better safe than sorry
		} catch (RuntimeException e) {
			return true; // better safe than sorry
		}
	}

	@Override
	public SQLiteDatabase getWritableDatabase() {
		SQLiteDatabase db = super.getWritableDatabase();
		db.execSQL("PRAGMA foreign_keys=ON;");
		return db;
	}

	public void updateMessage(Message message) {
		SQLiteDatabase db = this.getWritableDatabase();
		String[] args = { message.getUuid() };
		db.update(Message.TABLENAME, message.getContentValues(), Message.UUID
				+ "=?", args);
	}

	public void readRoster(Roster roster) {
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor;
		String args[] = { roster.getAccount().getUuid() };
		cursor = db.query(Contact.TABLENAME, null, Contact.ACCOUNT + "=?", args, null, null, null);
		while (cursor.moveToNext()) {
			roster.initContact(Contact.fromCursor(cursor));
		}
		cursor.close();
	}

	public void writeRoster(final Roster roster) {
		final Account account = roster.getAccount();
		final SQLiteDatabase db = this.getWritableDatabase();
		for (Contact contact : roster.getContacts()) {
			if (contact.getOption(Contact.Options.IN_ROSTER)) {
				db.insert(Contact.TABLENAME, null, contact.getContentValues());
			} else {
				String where = Contact.ACCOUNT + "=? AND " + Contact.JID + "=?";
				String[] whereArgs = { account.getUuid(), contact.getJid().toString() };
				db.delete(Contact.TABLENAME, where, whereArgs);
			}
		}
		account.setRosterVersion(roster.getVersion());
		updateAccount(account);
	}

	public void deleteMessage(Message message) {
		SQLiteDatabase db = this.getWritableDatabase();
		String[] args = { message.getUuid() };
		db.delete(Message.TABLENAME, Message.UUID + "=?", args);
	}

	public void deleteMessagesInConversation(Conversation conversation) {
		SQLiteDatabase db = this.getWritableDatabase();
		String[] args = { conversation.getUuid() };
		db.delete(Message.TABLENAME, Message.CONVERSATION + "=?", args);
	}

	public Conversation findConversationByUuid(String conversationUuid) {
		SQLiteDatabase db = this.getReadableDatabase();
		String[] selectionArgs = { conversationUuid };
		Cursor cursor = db.query(Conversation.TABLENAME, null,
				Conversation.UUID + "=?", selectionArgs, null, null, null);
		if (cursor.getCount() == 0) {
			return null;
		}
		cursor.moveToFirst();
		Conversation conversation = Conversation.fromCursor(cursor);
		cursor.close();
		return conversation;
	}

	public Message findMessageByUuid(String messageUuid) {
		SQLiteDatabase db = this.getReadableDatabase();
		String[] selectionArgs = { messageUuid };
		Cursor cursor = db.query(Message.TABLENAME, null, Message.UUID + "=?",
				selectionArgs, null, null, null);
		if (cursor.getCount() == 0) {
			return null;
		}
		cursor.moveToFirst();
		Message message = Message.fromCursor(cursor);
		cursor.close();
		return message;
	}

	public Account findAccountByUuid(String accountUuid) {
		SQLiteDatabase db = this.getReadableDatabase();
		String[] selectionArgs = { accountUuid };
		Cursor cursor = db.query(Account.TABLENAME, null, Account.UUID + "=?",
				selectionArgs, null, null, null);
		if (cursor.getCount() == 0) {
			return null;
		}
		cursor.moveToFirst();
		Account account = Account.fromCursor(cursor);
		cursor.close();
		return account;
	}

	public List<Message> getImageMessages(Conversation conversation) {
		ArrayList<Message> list = new ArrayList<>();
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor;
			String[] selectionArgs = { conversation.getUuid(), String.valueOf(Message.TYPE_IMAGE) };
			cursor = db.query(Message.TABLENAME, null, Message.CONVERSATION
					+ "=? AND "+Message.TYPE+"=?", selectionArgs, null, null,null);
		if (cursor.getCount() > 0) {
			cursor.moveToLast();
			do {
				Message message = Message.fromCursor(cursor);
				message.setConversation(conversation);
				list.add(message);
			} while (cursor.moveToPrevious());
		}
		cursor.close();
		return list;
	}

	private Cursor getCursorForSession(Account account, AxolotlAddress contact) {
		final SQLiteDatabase db = this.getReadableDatabase();
		String[] columns = null;
		String[] selectionArgs = {account.getUuid(),
				contact.getName(),
				Integer.toString(contact.getDeviceId())};
		Cursor cursor = db.query(AxolotlService.SQLiteAxolotlStore.SESSION_TABLENAME,
				columns,
				AxolotlService.SQLiteAxolotlStore.ACCOUNT + " = ? AND "
						+ AxolotlService.SQLiteAxolotlStore.NAME + " = ? AND "
						+ AxolotlService.SQLiteAxolotlStore.DEVICE_ID + " = ? ",
				selectionArgs,
				null, null, null);

		return cursor;
	}

	public SessionRecord loadSession(Account account, AxolotlAddress contact) {
		SessionRecord session = null;
		Cursor cursor = getCursorForSession(account, contact);
		if(cursor.getCount() != 0) {
			cursor.moveToFirst();
			try {
				session = new SessionRecord(Base64.decode(cursor.getString(cursor.getColumnIndex(AxolotlService.SQLiteAxolotlStore.KEY)),Base64.DEFAULT));
			} catch (IOException e) {
				cursor.close();
				throw new AssertionError(e);
			}
		}
		cursor.close();
		return session;
	}

	public List<Integer> getSubDeviceSessions(Account account, AxolotlAddress contact) {
		List<Integer> devices = new ArrayList<>();
		final SQLiteDatabase db = this.getReadableDatabase();
		String[] columns = {AxolotlService.SQLiteAxolotlStore.DEVICE_ID};
		String[] selectionArgs = {account.getUuid(),
				contact.getName()};
		Cursor cursor = db.query(AxolotlService.SQLiteAxolotlStore.SESSION_TABLENAME,
				columns,
				AxolotlService.SQLiteAxolotlStore.ACCOUNT + " = ? AND "
						+ AxolotlService.SQLiteAxolotlStore.NAME + " = ?",
				selectionArgs,
				null, null, null);

		while(cursor.moveToNext()) {
			devices.add(cursor.getInt(
					cursor.getColumnIndex(AxolotlService.SQLiteAxolotlStore.DEVICE_ID)));
		}

		cursor.close();
		return devices;
	}

	public boolean containsSession(Account account, AxolotlAddress contact) {
		Cursor cursor = getCursorForSession(account, contact);
		int count = cursor.getCount();
		cursor.close();
		return count != 0;
	}

	public void storeSession(Account account, AxolotlAddress contact, SessionRecord session) {
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(AxolotlService.SQLiteAxolotlStore.NAME, contact.getName());
		values.put(AxolotlService.SQLiteAxolotlStore.DEVICE_ID, contact.getDeviceId());
		values.put(AxolotlService.SQLiteAxolotlStore.KEY, Base64.encodeToString(session.serialize(),Base64.DEFAULT));
		values.put(AxolotlService.SQLiteAxolotlStore.ACCOUNT, account.getUuid());
		db.insert(AxolotlService.SQLiteAxolotlStore.SESSION_TABLENAME, null, values);
	}

	public void deleteSession(Account account, AxolotlAddress contact) {
		SQLiteDatabase db = this.getWritableDatabase();
		String[] args = {account.getUuid(),
				contact.getName(),
				Integer.toString(contact.getDeviceId())};
		db.delete(AxolotlService.SQLiteAxolotlStore.SESSION_TABLENAME,
				AxolotlService.SQLiteAxolotlStore.ACCOUNT + " = ? AND "
						+ AxolotlService.SQLiteAxolotlStore.NAME + " = ? AND "
						+ AxolotlService.SQLiteAxolotlStore.DEVICE_ID + " = ? ",
				args);
	}

	public void deleteAllSessions(Account account, AxolotlAddress contact) {
		SQLiteDatabase db = this.getWritableDatabase();
		String[] args = {account.getUuid(), contact.getName()};
		db.delete(AxolotlService.SQLiteAxolotlStore.SESSION_TABLENAME,
				AxolotlService.SQLiteAxolotlStore.ACCOUNT + "=? AND "
						+ AxolotlService.SQLiteAxolotlStore.NAME + " = ?",
				args);
	}

	public boolean isTrustedSession(Account account, AxolotlAddress contact) {
		boolean trusted = false;
		Cursor cursor = getCursorForSession(account, contact);
		if(cursor.getCount() != 0) {
			cursor.moveToFirst();
			trusted = cursor.getInt(cursor.getColumnIndex(
					AxolotlService.SQLiteAxolotlStore.TRUSTED)) > 0;
		}
		cursor.close();
		return trusted;
	}

	public void setTrustedSession(Account account, AxolotlAddress contact, boolean trusted) {
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(AxolotlService.SQLiteAxolotlStore.NAME, contact.getName());
		values.put(AxolotlService.SQLiteAxolotlStore.DEVICE_ID, contact.getDeviceId());
		values.put(AxolotlService.SQLiteAxolotlStore.ACCOUNT, account.getUuid());
		values.put(AxolotlService.SQLiteAxolotlStore.TRUSTED, trusted?1:0);
		db.insert(AxolotlService.SQLiteAxolotlStore.SESSION_TABLENAME, null, values);
	}

	private Cursor getCursorForPreKey(Account account, int preKeyId) {
		SQLiteDatabase db = this.getReadableDatabase();
		String[] columns = {AxolotlService.SQLiteAxolotlStore.KEY};
		String[] selectionArgs = {account.getUuid(), Integer.toString(preKeyId)};
		Cursor cursor = db.query(AxolotlService.SQLiteAxolotlStore.PREKEY_TABLENAME,
				columns,
				AxolotlService.SQLiteAxolotlStore.ACCOUNT + "=? AND "
						+ AxolotlService.SQLiteAxolotlStore.ID + "=?",
				selectionArgs,
				null, null, null);

		return cursor;
	}

	public PreKeyRecord loadPreKey(Account account, int preKeyId) {
		PreKeyRecord record = null;
		Cursor cursor = getCursorForPreKey(account, preKeyId);
		if(cursor.getCount() != 0) {
			cursor.moveToFirst();
			try {
				record = new PreKeyRecord(Base64.decode(cursor.getString(cursor.getColumnIndex(AxolotlService.SQLiteAxolotlStore.KEY)),Base64.DEFAULT));
			} catch (IOException e ) {
				throw new AssertionError(e);
			}
		}
		cursor.close();
		return record;
	}

	public boolean containsPreKey(Account account, int preKeyId) {
		Cursor cursor = getCursorForPreKey(account, preKeyId);
		int count = cursor.getCount();
		cursor.close();
		return count != 0;
	}

	public void storePreKey(Account account, PreKeyRecord record) {
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(AxolotlService.SQLiteAxolotlStore.ID, record.getId());
		values.put(AxolotlService.SQLiteAxolotlStore.KEY, Base64.encodeToString(record.serialize(),Base64.DEFAULT));
		values.put(AxolotlService.SQLiteAxolotlStore.ACCOUNT, account.getUuid());
		db.insert(AxolotlService.SQLiteAxolotlStore.PREKEY_TABLENAME, null, values);
	}

	public void deletePreKey(Account account, int preKeyId) {
		SQLiteDatabase db = this.getWritableDatabase();
		String[] args = {account.getUuid(), Integer.toString(preKeyId)};
		db.delete(AxolotlService.SQLiteAxolotlStore.PREKEY_TABLENAME,
				AxolotlService.SQLiteAxolotlStore.ACCOUNT + "=? AND "
						+ AxolotlService.SQLiteAxolotlStore.ID + "=?",
				args);
	}

	private Cursor getCursorForSignedPreKey(Account account, int signedPreKeyId) {
		SQLiteDatabase db = this.getReadableDatabase();
		String[] columns = {AxolotlService.SQLiteAxolotlStore.KEY};
		String[] selectionArgs = {account.getUuid(), Integer.toString(signedPreKeyId)};
		Cursor cursor = db.query(AxolotlService.SQLiteAxolotlStore.SIGNED_PREKEY_TABLENAME,
				columns,
				AxolotlService.SQLiteAxolotlStore.ACCOUNT + "=? AND " + AxolotlService.SQLiteAxolotlStore.ID + "=?",
				selectionArgs,
				null, null, null);

		return cursor;
	}

	public SignedPreKeyRecord loadSignedPreKey(Account account, int signedPreKeyId) {
		SignedPreKeyRecord record = null;
		Cursor cursor = getCursorForSignedPreKey(account, signedPreKeyId);
		if(cursor.getCount() != 0) {
			cursor.moveToFirst();
			try {
				record = new SignedPreKeyRecord(Base64.decode(cursor.getString(cursor.getColumnIndex(AxolotlService.SQLiteAxolotlStore.KEY)),Base64.DEFAULT));
			} catch (IOException e ) {
				throw new AssertionError(e);
			}
		}
		cursor.close();
		return record;
	}

	public List<SignedPreKeyRecord> loadSignedPreKeys(Account account) {
		List<SignedPreKeyRecord> prekeys = new ArrayList<>();
		SQLiteDatabase db = this.getReadableDatabase();
		String[] columns = {AxolotlService.SQLiteAxolotlStore.KEY};
		String[] selectionArgs = {account.getUuid()};
		Cursor cursor = db.query(AxolotlService.SQLiteAxolotlStore.SIGNED_PREKEY_TABLENAME,
				columns,
				AxolotlService.SQLiteAxolotlStore.ACCOUNT + "=?",
				selectionArgs,
				null, null, null);

		while(cursor.moveToNext()) {
			try {
				prekeys.add(new SignedPreKeyRecord(Base64.decode(cursor.getString(cursor.getColumnIndex(AxolotlService.SQLiteAxolotlStore.KEY)), Base64.DEFAULT)));
			} catch (IOException ignored) {
			}
		}
		cursor.close();
		return prekeys;
	}

	public boolean containsSignedPreKey(Account account, int signedPreKeyId) {
		Cursor cursor = getCursorForPreKey(account, signedPreKeyId);
		int count = cursor.getCount();
		cursor.close();
		return count != 0;
	}

	public void storeSignedPreKey(Account account, SignedPreKeyRecord record) {
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(AxolotlService.SQLiteAxolotlStore.ID, record.getId());
		values.put(AxolotlService.SQLiteAxolotlStore.KEY, Base64.encodeToString(record.serialize(),Base64.DEFAULT));
		values.put(AxolotlService.SQLiteAxolotlStore.ACCOUNT, account.getUuid());
		db.insert(AxolotlService.SQLiteAxolotlStore.SIGNED_PREKEY_TABLENAME, null, values);
	}

	public void deleteSignedPreKey(Account account, int signedPreKeyId) {
		SQLiteDatabase db = this.getWritableDatabase();
		String[] args = {account.getUuid(), Integer.toString(signedPreKeyId)};
		db.delete(AxolotlService.SQLiteAxolotlStore.SIGNED_PREKEY_TABLENAME,
				AxolotlService.SQLiteAxolotlStore.ACCOUNT + "=? AND "
						+ AxolotlService.SQLiteAxolotlStore.ID + "=?",
				args);
	}

	private Cursor getIdentityKeyCursor(Account account, String name, boolean own) {
		final SQLiteDatabase db = this.getReadableDatabase();
		String[] columns = {AxolotlService.SQLiteAxolotlStore.KEY};
		String[] selectionArgs = {account.getUuid(),
				name,
				own?"1":"0"};
		Cursor cursor = db.query(AxolotlService.SQLiteAxolotlStore.IDENTITIES_TABLENAME,
				columns,
				AxolotlService.SQLiteAxolotlStore.ACCOUNT + " = ? AND "
						+ AxolotlService.SQLiteAxolotlStore.NAME + " = ? AND "
						+ AxolotlService.SQLiteAxolotlStore.OWN + " = ? ",
				selectionArgs,
				null, null, null);

		return cursor;
	}

	public IdentityKeyPair loadOwnIdentityKeyPair(Account account, String name) {
		IdentityKeyPair identityKeyPair = null;
		Cursor cursor = getIdentityKeyCursor(account, name, true);
		if(cursor.getCount() != 0) {
			cursor.moveToFirst();
			try {
				identityKeyPair = new IdentityKeyPair(Base64.decode(cursor.getString(cursor.getColumnIndex(AxolotlService.SQLiteAxolotlStore.KEY)),Base64.DEFAULT));
			} catch (InvalidKeyException e) {
				Log.d(Config.LOGTAG, AxolotlService.getLogprefix(account)+"Encountered invalid IdentityKey in database for account" + account.getJid().toBareJid() + ", address: " + name);
			}
		}
		cursor.close();

		return identityKeyPair;
	}

	public Set<IdentityKey> loadIdentityKeys(Account account, String name) {
		Set<IdentityKey> identityKeys = new HashSet<>();
		Cursor cursor = getIdentityKeyCursor(account, name, false);

		while(cursor.moveToNext()) {
			try {
				identityKeys.add(new IdentityKey(Base64.decode(cursor.getString(cursor.getColumnIndex(AxolotlService.SQLiteAxolotlStore.KEY)),Base64.DEFAULT),0));
			} catch (InvalidKeyException e) {
				Log.d(Config.LOGTAG, AxolotlService.getLogprefix(account)+"Encountered invalid IdentityKey in database for account"+account.getJid().toBareJid()+", address: "+name);
			}
		}
		cursor.close();

		return identityKeys;
	}

	private void storeIdentityKey(Account account, String name, boolean own, String base64Serialized) {
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(AxolotlService.SQLiteAxolotlStore.ACCOUNT, account.getUuid());
		values.put(AxolotlService.SQLiteAxolotlStore.NAME, name);
		values.put(AxolotlService.SQLiteAxolotlStore.OWN, own?1:0);
		values.put(AxolotlService.SQLiteAxolotlStore.KEY, base64Serialized);
		db.insert(AxolotlService.SQLiteAxolotlStore.IDENTITIES_TABLENAME, null, values);
	}

	public void storeIdentityKey(Account account, String name, IdentityKey identityKey) {
		storeIdentityKey(account, name, false, Base64.encodeToString(identityKey.serialize(), Base64.DEFAULT));
	}

	public void storeOwnIdentityKeyPair(Account account, String name, IdentityKeyPair identityKeyPair) {
		storeIdentityKey(account, name, true, Base64.encodeToString(identityKeyPair.serialize(),Base64.DEFAULT));
	}

	public void recreateAxolotlDb() {
		Log.d(Config.LOGTAG, AxolotlService.LOGPREFIX+" : "+">>> (RE)CREATING AXOLOTL DATABASE <<<");
		SQLiteDatabase db = this.getWritableDatabase();
		db.execSQL("DROP TABLE IF EXISTS " + AxolotlService.SQLiteAxolotlStore.SESSION_TABLENAME);
		db.execSQL(CREATE_SESSIONS_STATEMENT);
		db.execSQL("DROP TABLE IF EXISTS " + AxolotlService.SQLiteAxolotlStore.PREKEY_TABLENAME);
		db.execSQL(CREATE_PREKEYS_STATEMENT);
		db.execSQL("DROP TABLE IF EXISTS " + AxolotlService.SQLiteAxolotlStore.SIGNED_PREKEY_TABLENAME);
		db.execSQL(CREATE_SIGNED_PREKEYS_STATEMENT);
		db.execSQL("DROP TABLE IF EXISTS " + AxolotlService.SQLiteAxolotlStore.IDENTITIES_TABLENAME);
		db.execSQL(CREATE_IDENTITIES_STATEMENT);
	}
	
	public void wipeAxolotlDb(Account account) {
		String accountName = account.getUuid();
		Log.d(Config.LOGTAG, AxolotlService.getLogprefix(account)+">>> WIPING AXOLOTL DATABASE FOR ACCOUNT " + accountName + " <<<");
		SQLiteDatabase db = this.getWritableDatabase();
		String[] deleteArgs= {
				accountName
		};
		db.delete(AxolotlService.SQLiteAxolotlStore.SESSION_TABLENAME,
				AxolotlService.SQLiteAxolotlStore.ACCOUNT + " = ?",
				deleteArgs);
		db.delete(AxolotlService.SQLiteAxolotlStore.PREKEY_TABLENAME,
				AxolotlService.SQLiteAxolotlStore.ACCOUNT + " = ?",
				deleteArgs);
		db.delete(AxolotlService.SQLiteAxolotlStore.SIGNED_PREKEY_TABLENAME,
				AxolotlService.SQLiteAxolotlStore.ACCOUNT + " = ?",
				deleteArgs);
		db.delete(AxolotlService.SQLiteAxolotlStore.IDENTITIES_TABLENAME,
				AxolotlService.SQLiteAxolotlStore.ACCOUNT + " = ?",
				deleteArgs);
	}
}
