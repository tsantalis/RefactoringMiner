package eu.siacs.conversations.crypto.axolotl;

import android.util.Base64;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;
import java.util.HashSet;
import java.util.Set;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import eu.siacs.conversations.entities.Contact;
import eu.siacs.conversations.xml.Element;
import eu.siacs.conversations.xmpp.jid.Jid;

public class XmppAxolotlMessage {
	private byte[] innerKey;
	private byte[] ciphertext;
	private byte[] iv;
	private final Set<XmppAxolotlMessageHeader> headers;
	private final Jid from;
	private final int sourceDeviceId;

	public static class XmppAxolotlMessageHeader {
		private final int recipientDeviceId;
		private final byte[] content;

		public XmppAxolotlMessageHeader(int deviceId, byte[] content) {
			this.recipientDeviceId = deviceId;
			this.content = content;
		}

		public XmppAxolotlMessageHeader(Element header) {
			if("header".equals(header.getName())) {
				this.recipientDeviceId = Integer.parseInt(header.getAttribute("rid"));
				this.content = Base64.decode(header.getContent(),Base64.DEFAULT);
			} else {
				throw new IllegalArgumentException("Argument not a <header> Element!");
			}
		}

		public int getRecipientDeviceId() {
			return recipientDeviceId;
		}

		public byte[] getContents() {
			return content;
		}

		public Element toXml() {
			Element headerElement = new Element("header");
			// TODO: generate XML
			headerElement.setAttribute("rid", getRecipientDeviceId());
			headerElement.setContent(Base64.encodeToString(getContents(), Base64.DEFAULT));
			return headerElement;
		}
	}

	public static class XmppAxolotlPlaintextMessage {
		private final AxolotlService.XmppAxolotlSession session;
		private final String plaintext;

		public XmppAxolotlPlaintextMessage(AxolotlService.XmppAxolotlSession session, String plaintext) {
			this.session = session;
			this.plaintext = plaintext;
		}

		public String getPlaintext() {
			return plaintext;
		}

		public AxolotlService.XmppAxolotlSession getSession() {
			return session;
		}

	}

	public XmppAxolotlMessage(Jid from, Element axolotlMessage) {
		this.from = from;
		this.sourceDeviceId = Integer.parseInt(axolotlMessage.getAttribute("id"));
		this.headers = new HashSet<>();
		for(Element child:axolotlMessage.getChildren()) {
			switch(child.getName()) {
				case "header":
					headers.add(new XmppAxolotlMessageHeader(child));
					break;
				case "message":
					iv = Base64.decode(child.getAttribute("iv"),Base64.DEFAULT);
					ciphertext = Base64.decode(child.getContent(),Base64.DEFAULT);
					break;
				default:
					break;
			}
		}
	}

	public XmppAxolotlMessage(Jid from, int sourceDeviceId, String plaintext) {
		this.from = from;
		this.sourceDeviceId = sourceDeviceId;
		this.headers = new HashSet<>();
		this.encrypt(plaintext);
	}

	private void encrypt(String plaintext) {
		try {
			KeyGenerator generator = KeyGenerator.getInstance("AES");
			generator.init(128);
			SecretKey secretKey = generator.generateKey();
			Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, secretKey);
			this.innerKey = secretKey.getEncoded();
			this.iv = cipher.getIV();
			this.ciphertext = cipher.doFinal(plaintext.getBytes());
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
				| IllegalBlockSizeException | BadPaddingException e) {

		}
	}

	public Jid getFrom() {
		return this.from;
	}

	public int getSenderDeviceId() {
		return sourceDeviceId;
	}

	public byte[] getCiphertext() {
		return ciphertext;
	}

	public Set<XmppAxolotlMessageHeader> getHeaders() {
		return headers;
	}

	public void addHeader(XmppAxolotlMessageHeader header) {
		headers.add(header);
	}

	public byte[] getInnerKey(){
		return innerKey;
	}

	public byte[] getIV() {
		return this.iv;
	}

	public Element toXml() {
		// TODO: generate outer XML, add in header XML
		Element message= new Element("axolotl_message", AxolotlService.PEP_PREFIX);
		message.setAttribute("id", sourceDeviceId);
		for(XmppAxolotlMessageHeader header: headers) {
			message.addChild(header.toXml());
		}
		Element payload = message.addChild("message");
		payload.setAttribute("iv",Base64.encodeToString(iv, Base64.DEFAULT));
		payload.setContent(Base64.encodeToString(ciphertext,Base64.DEFAULT));
		return message;
	}


	public XmppAxolotlPlaintextMessage decrypt(AxolotlService.XmppAxolotlSession session, byte[] key) {
		XmppAxolotlPlaintextMessage plaintextMessage = null;
		try {

			Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
			SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
			IvParameterSpec ivSpec = new IvParameterSpec(iv);

			cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

			String plaintext = new String(cipher.doFinal(ciphertext));
			plaintextMessage = new XmppAxolotlPlaintextMessage(session, plaintext);

		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
				| InvalidAlgorithmParameterException | IllegalBlockSizeException
				| BadPaddingException e) {
			throw new AssertionError(e);
		}
		return plaintextMessage;
	}
}
