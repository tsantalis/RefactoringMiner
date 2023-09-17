package eu.siacs.conversations.parser;

import android.util.Log;
import android.util.Pair;

import net.java.otr4j.session.Session;
import net.java.otr4j.session.SessionStatus;

import java.util.List;
import java.util.Set;

import eu.siacs.conversations.Config;
import eu.siacs.conversations.crypto.axolotl.AxolotlService;
import eu.siacs.conversations.crypto.axolotl.XmppAxolotlMessage;
import eu.siacs.conversations.entities.Account;
import eu.siacs.conversations.entities.Contact;
import eu.siacs.conversations.entities.Conversation;
import eu.siacs.conversations.entities.Message;
import eu.siacs.conversations.entities.MucOptions;
import eu.siacs.conversations.http.HttpConnectionManager;
import eu.siacs.conversations.services.MessageArchiveService;
import eu.siacs.conversations.services.XmppConnectionService;
import eu.siacs.conversations.utils.CryptoHelper;
import eu.siacs.conversations.xml.Element;
import eu.siacs.conversations.xmpp.OnMessagePacketReceived;
import eu.siacs.conversations.xmpp.chatstate.ChatState;
import eu.siacs.conversations.xmpp.jid.Jid;
import eu.siacs.conversations.xmpp.pep.Avatar;
import eu.siacs.conversations.xmpp.stanzas.MessagePacket;

public class MessageParser extends AbstractParser implements
		OnMessagePacketReceived {
	public MessageParser(XmppConnectionService service) {
		super(service);
	}

	private boolean extractChatState(Conversation conversation, final MessagePacket packet) {
		ChatState state = ChatState.parse(packet);
		if (state != null && conversation != null) {
			final Account account = conversation.getAccount();
			Jid from = packet.getFrom();
			if (from.toBareJid().equals(account.getJid().toBareJid())) {
				conversation.setOutgoingChatState(state);
				return false;
			} else {
				return conversation.setIncomingChatState(state);
			}
		}
		return false;
	}

	private Message parseOtrChat(String body, Jid from, String id, Conversation conversation) {
		String presence;
		if (from.isBareJid()) {
			presence = "";
		} else {
			presence = from.getResourcepart();
		}
		if (body.matches("^\\?OTRv\\d{1,2}\\?.*")) {
			conversation.endOtrIfNeeded();
		}
		if (!conversation.hasValidOtrSession()) {
			conversation.startOtrSession(presence,false);
		} else {
			String foreignPresence = conversation.getOtrSession().getSessionID().getUserID();
			if (!foreignPresence.equals(presence)) {
				conversation.endOtrIfNeeded();
				conversation.startOtrSession(presence, false);
			}
		}
		try {
			conversation.setLastReceivedOtrMessageId(id);
			Session otrSession = conversation.getOtrSession();
			SessionStatus before = otrSession.getSessionStatus();
			body = otrSession.transformReceiving(body);
			SessionStatus after = otrSession.getSessionStatus();
			if ((before != after) && (after == SessionStatus.ENCRYPTED)) {
				conversation.setNextEncryption(Message.ENCRYPTION_OTR);
				mXmppConnectionService.onOtrSessionEstablished(conversation);
			} else if ((before != after) && (after == SessionStatus.FINISHED)) {
				conversation.setNextEncryption(Message.ENCRYPTION_NONE);
				conversation.resetOtrSession();
				mXmppConnectionService.updateConversationUi();
			}
			if ((body == null) || (body.isEmpty())) {
				return null;
			}
			if (body.startsWith(CryptoHelper.FILETRANSFER)) {
				String key = body.substring(CryptoHelper.FILETRANSFER.length());
				conversation.setSymmetricKey(CryptoHelper.hexToBytes(key));
				return null;
			}
			Message finishedMessage = new Message(conversation, body, Message.ENCRYPTION_OTR, Message.STATUS_RECEIVED);
			conversation.setLastReceivedOtrMessageId(null);
			return finishedMessage;
		} catch (Exception e) {
			conversation.resetOtrSession();
			return null;
		}
	}

	private Message parseAxolotlChat(Element axolotlMessage, Jid from, String id, Conversation conversation, int status) {
		Message finishedMessage = null;
		AxolotlService service = conversation.getAccount().getAxolotlService();
		XmppAxolotlMessage xmppAxolotlMessage = new XmppAxolotlMessage(from.toBareJid(), axolotlMessage);
		XmppAxolotlMessage.XmppAxolotlPlaintextMessage plaintextMessage = service.processReceiving(xmppAxolotlMessage);
		if(plaintextMessage != null) {
			finishedMessage = new Message(conversation, plaintextMessage.getPlaintext(), Message.ENCRYPTION_AXOLOTL, status);
			finishedMessage.setAxolotlSession(plaintextMessage.getSession());
		}

		return finishedMessage;
	}

	private class Invite {
		Jid jid;
		String password;
		Invite(Jid jid, String password) {
			this.jid = jid;
			this.password = password;
		}

		public boolean execute(Account account) {
			if (jid != null) {
				Conversation conversation = mXmppConnectionService.findOrCreateConversation(account, jid, true);
				if (!conversation.getMucOptions().online()) {
					conversation.getMucOptions().setPassword(password);
					mXmppConnectionService.databaseBackend.updateConversation(conversation);
					mXmppConnectionService.joinMuc(conversation);
					mXmppConnectionService.updateConversationUi();
				}
				return true;
			}
			return false;
		}
	}

	private Invite extractInvite(Element message) {
		Element x = message.findChild("x", "http://jabber.org/protocol/muc#user");
		if (x != null) {
			Element invite = x.findChild("invite");
			if (invite != null) {
				Element pw = x.findChild("password");
				return new Invite(message.getAttributeAsJid("from"), pw != null ? pw.getContent(): null);
			}
		} else {
			x = message.findChild("x","jabber:x:conference");
			if (x != null) {
				return new Invite(x.getAttributeAsJid("jid"),x.getAttribute("password"));
			}
		}
		return null;
	}

	private void parseEvent(final Element event, final Jid from, final Account account) {
		Element items = event.findChild("items");
		String node = items == null ? null : items.getAttribute("node");
		if ("urn:xmpp:avatar:metadata".equals(node)) {
			Avatar avatar = Avatar.parseMetadata(items);
			if (avatar != null) {
				avatar.owner = from;
				if (mXmppConnectionService.getFileBackend().isAvatarCached(avatar)) {
					if (account.getJid().toBareJid().equals(from)) {
						if (account.setAvatar(avatar.getFilename())) {
							mXmppConnectionService.databaseBackend.updateAccount(account);
						}
						mXmppConnectionService.getAvatarService().clear(account);
						mXmppConnectionService.updateConversationUi();
						mXmppConnectionService.updateAccountUi();
					} else {
						Contact contact = account.getRoster().getContact(from);
						contact.setAvatar(avatar);
						mXmppConnectionService.getAvatarService().clear(contact);
						mXmppConnectionService.updateConversationUi();
						mXmppConnectionService.updateRosterUi();
					}
				} else {
					mXmppConnectionService.fetchAvatar(account, avatar);
				}
			}
		} else if ("http://jabber.org/protocol/nick".equals(node)) {
			Element i = items.findChild("item");
			Element nick = i == null ? null : i.findChild("nick", "http://jabber.org/protocol/nick");
			if (nick != null) {
				Contact contact = account.getRoster().getContact(from);
				contact.setPresenceName(nick.getContent());
				mXmppConnectionService.getAvatarService().clear(account);
				mXmppConnectionService.updateConversationUi();
				mXmppConnectionService.updateAccountUi();
			}
		} else if (AxolotlService.PEP_DEVICE_LIST.equals(node)) {
			Log.d(Config.LOGTAG, AxolotlService.getLogprefix(account)+"Received PEP device list update from "+ from + ", processing...");
			Element item = items.findChild("item");
			Set<Integer> deviceIds = mXmppConnectionService.getIqParser().deviceIds(item);
			AxolotlService axolotlService = account.getAxolotlService();
			axolotlService.registerDevices(from, deviceIds);
			mXmppConnectionService.updateAccountUi();
		}
	}

	private boolean handleErrorMessage(Account account, MessagePacket packet) {
		if (packet.getType() == MessagePacket.TYPE_ERROR) {
			Jid from = packet.getFrom();
			if (from != null) {
				mXmppConnectionService.markMessage(account, from.toBareJid(), packet.getId(), Message.STATUS_SEND_FAILED);
			}
			return true;
		}
		return false;
	}

	@Override
	public void onMessagePacketReceived(Account account, MessagePacket original) {
		if (handleErrorMessage(account, original)) {
			return;
		}
		final MessagePacket packet;
		Long timestamp = null;
		final boolean isForwarded;
		String serverMsgId = null;
		final Element fin = original.findChild("fin", "urn:xmpp:mam:0");
		if (fin != null) {
			mXmppConnectionService.getMessageArchiveService().processFin(fin,original.getFrom());
			return;
		}
		final Element result = original.findChild("result","urn:xmpp:mam:0");
		final MessageArchiveService.Query query = result == null ? null : mXmppConnectionService.getMessageArchiveService().findQuery(result.getAttribute("queryid"));
		if (query != null && query.validFrom(original.getFrom())) {
			Pair<MessagePacket, Long> f = original.getForwardedMessagePacket("result", "urn:xmpp:mam:0");
			if (f == null) {
				return;
			}
			timestamp = f.second;
			packet = f.first;
			isForwarded = true;
			serverMsgId = result.getAttribute("id");
			query.incrementTotalCount();
		} else if (query != null) {
			Log.d(Config.LOGTAG,account.getJid().toBareJid()+": received mam result from invalid sender");
			return;
		} else if (original.fromServer(account)) {
			Pair<MessagePacket, Long> f;
			f = original.getForwardedMessagePacket("received", "urn:xmpp:carbons:2");
			f = f == null ? original.getForwardedMessagePacket("sent", "urn:xmpp:carbons:2") : f;
			packet = f != null ? f.first : original;
			if (handleErrorMessage(account, packet)) {
				return;
			}
			timestamp = f != null ? f.second : null;
			isForwarded = f != null;
		} else {
			packet = original;
			isForwarded = false;
		}

		if (timestamp == null) {
			timestamp = AbstractParser.getTimestamp(packet, System.currentTimeMillis());
		}
		final String body = packet.getBody();
		final Element mucUserElement = packet.findChild("x","http://jabber.org/protocol/muc#user");
		final String pgpEncrypted = packet.findChildContent("x", "jabber:x:encrypted");
		final Element axolotlEncrypted = packet.findChild("axolotl_message", AxolotlService.PEP_PREFIX);
		int status;
		final Jid counterpart;
		final Jid to = packet.getTo();
		final Jid from = packet.getFrom();
		final String remoteMsgId = packet.getId();

		if (from == null || to == null) {
			Log.d(Config.LOGTAG,"no to or from in: "+packet.toString());
			return;
		}
		
		boolean isTypeGroupChat = packet.getType() == MessagePacket.TYPE_GROUPCHAT;
		boolean isProperlyAddressed = !to.isBareJid() || account.countPresences() == 1;
		boolean isMucStatusMessage = from.isBareJid() && mucUserElement != null && mucUserElement.hasChild("status");
		if (packet.fromAccount(account)) {
			status = Message.STATUS_SEND;
			counterpart = to;
		} else {
			status = Message.STATUS_RECEIVED;
			counterpart = from;
		}

		Invite invite = extractInvite(packet);
		if (invite != null && invite.execute(account)) {
			return;
		}

		if (extractChatState(mXmppConnectionService.find(account, from), packet)) {
			mXmppConnectionService.updateConversationUi();
		}

		if ((body != null || pgpEncrypted != null || axolotlEncrypted != null) && !isMucStatusMessage) {
			Conversation conversation = mXmppConnectionService.findOrCreateConversation(account, counterpart.toBareJid(), isTypeGroupChat);
			if (isTypeGroupChat) {
				if (counterpart.getResourcepart().equals(conversation.getMucOptions().getActualNick())) {
					status = Message.STATUS_SEND_RECEIVED;
					if (mXmppConnectionService.markMessage(conversation, remoteMsgId, status)) {
						return;
					} else {
						Message message = conversation.findSentMessageWithBody(body);
						if (message != null) {
							message.setRemoteMsgId(remoteMsgId);
							mXmppConnectionService.markMessage(message, status);
							return;
						}
					}
				} else {
					status = Message.STATUS_RECEIVED;
				}
			}
			Message message;
			if (body != null && body.startsWith("?OTR")) {
				if (!isForwarded && !isTypeGroupChat && isProperlyAddressed) {
					message = parseOtrChat(body, from, remoteMsgId, conversation);
					if (message == null) {
						return;
					}
				} else {
					message = new Message(conversation, body, Message.ENCRYPTION_NONE, status);
				}
			} else if (pgpEncrypted != null) {
				message = new Message(conversation, pgpEncrypted, Message.ENCRYPTION_PGP, status);
			} else if (axolotlEncrypted != null) {
				message = parseAxolotlChat(axolotlEncrypted, from, remoteMsgId, conversation, status);
				if (message == null) {
					return;
				}
			} else {
				message = new Message(conversation, body, Message.ENCRYPTION_NONE, status);
			}
			message.setCounterpart(counterpart);
			message.setRemoteMsgId(remoteMsgId);
			message.setServerMsgId(serverMsgId);
			message.setTime(timestamp);
			message.markable = packet.hasChild("markable", "urn:xmpp:chat-markers:0");
			if (conversation.getMode() == Conversation.MODE_MULTI) {
				message.setTrueCounterpart(conversation.getMucOptions().getTrueCounterpart(counterpart.getResourcepart()));
				if (!isTypeGroupChat) {
					message.setType(Message.TYPE_PRIVATE);
				}
			}
			updateLastseen(packet,account,true);
			boolean checkForDuplicates = serverMsgId != null
					|| (isTypeGroupChat && packet.hasChild("delay","urn:xmpp:delay"))
					|| message.getType() == Message.TYPE_PRIVATE;
			if (checkForDuplicates && conversation.hasDuplicateMessage(message)) {
				Log.d(Config.LOGTAG,"skipping duplicate message from "+message.getCounterpart().toString()+" "+message.getBody());
				return;
			}
			if (query != null) {
				query.incrementMessageCount();
			}
			conversation.add(message);
			if (serverMsgId == null) {
				if (status == Message.STATUS_SEND || status == Message.STATUS_SEND_RECEIVED) {
					mXmppConnectionService.markRead(conversation);
					account.activateGracePeriod();
				} else {
					message.markUnread();
				}
				mXmppConnectionService.updateConversationUi();
			}

			if (mXmppConnectionService.confirmMessages() && remoteMsgId != null && !isForwarded) {
				if (packet.hasChild("markable", "urn:xmpp:chat-markers:0")) {
					MessagePacket receipt = mXmppConnectionService
							.getMessageGenerator().received(account, packet, "urn:xmpp:chat-markers:0");
					mXmppConnectionService.sendMessagePacket(account, receipt);
				}
				if (packet.hasChild("request", "urn:xmpp:receipts")) {
					MessagePacket receipt = mXmppConnectionService
							.getMessageGenerator().received(account, packet, "urn:xmpp:receipts");
					mXmppConnectionService.sendMessagePacket(account, receipt);
				}
			}
			if (account.getXmppConnection() != null && account.getXmppConnection().getFeatures().advancedStreamFeaturesLoaded()) {
				if (conversation.setLastMessageTransmitted(System.currentTimeMillis())) {
					mXmppConnectionService.updateConversation(conversation);
				}
			}

			if (message.getStatus() == Message.STATUS_RECEIVED
					&& conversation.getOtrSession() != null
					&& !conversation.getOtrSession().getSessionID().getUserID()
					.equals(message.getCounterpart().getResourcepart())) {
				conversation.endOtrIfNeeded();
			}

			if (message.getEncryption() == Message.ENCRYPTION_NONE || mXmppConnectionService.saveEncryptedMessages()) {
				mXmppConnectionService.databaseBackend.createMessage(message);
			}
			final HttpConnectionManager manager = this.mXmppConnectionService.getHttpConnectionManager();
			if (message.trusted() && message.treatAsDownloadable() != Message.Decision.NEVER && manager.getAutoAcceptFileSize() > 0) {
				manager.createNewConnection(message);
			} else if (!message.isRead()) {
				mXmppConnectionService.getNotificationService().push(message);
			}
		} else { //no body
			if (isTypeGroupChat) {
				Conversation conversation = mXmppConnectionService.find(account, from.toBareJid());
				if (packet.hasChild("subject")) {
					if (conversation != null && conversation.getMode() == Conversation.MODE_MULTI) {
						conversation.setHasMessagesLeftOnServer(conversation.countMessages() > 0);
						conversation.getMucOptions().setSubject(packet.findChildContent("subject"));
						mXmppConnectionService.updateConversationUi();
						return;
					}
				}

				if (conversation != null && isMucStatusMessage) {
					for (Element child : mucUserElement.getChildren()) {
						if (child.getName().equals("status")
								&& MucOptions.STATUS_CODE_ROOM_CONFIG_CHANGED.equals(child.getAttribute("code"))) {
							mXmppConnectionService.fetchConferenceConfiguration(conversation);
						}
					}
				}
			}
		}

		Element received = packet.findChild("received", "urn:xmpp:chat-markers:0");
		if (received == null) {
			received = packet.findChild("received", "urn:xmpp:receipts");
		}
		if (received != null && !packet.fromAccount(account)) {
			mXmppConnectionService.markMessage(account, from.toBareJid(), received.getAttribute("id"), Message.STATUS_SEND_RECEIVED);
		}
		Element displayed = packet.findChild("displayed", "urn:xmpp:chat-markers:0");
		if (displayed != null) {
			if (packet.fromAccount(account)) {
				Conversation conversation = mXmppConnectionService.find(account,counterpart.toBareJid());
				if (conversation != null) {
					mXmppConnectionService.markRead(conversation);
				}
			} else {
				updateLastseen(packet, account, true);
				final Message displayedMessage = mXmppConnectionService.markMessage(account, from.toBareJid(), displayed.getAttribute("id"), Message.STATUS_SEND_DISPLAYED);
				Message message = displayedMessage == null ? null : displayedMessage.prev();
				while (message != null
						&& message.getStatus() == Message.STATUS_SEND_RECEIVED
						&& message.getTimeSent() < displayedMessage.getTimeSent()) {
					mXmppConnectionService.markMessage(message, Message.STATUS_SEND_DISPLAYED);
					message = message.prev();
				}
			}
		}

		Element event = packet.findChild("event", "http://jabber.org/protocol/pubsub#event");
		if (event != null) {
			parseEvent(event, from, account);
		}

		String nick = packet.findChildContent("nick", "http://jabber.org/protocol/nick");
		if (nick != null) {
			Contact contact = account.getRoster().getContact(from);
			contact.setPresenceName(nick);
		}
	}
}