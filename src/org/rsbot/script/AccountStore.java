package org.rsbot.script;

import org.rsbot.gui.AccountManager;
import org.rsbot.security.RestrictedSecurityManager;
import org.rsbot.util.StringUtil;
import org.rsbot.util.io.IniParser;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * @author Paris
 * @author Timer
 */
public class AccountStore {
	public static class Account {
		private final static String PASSWORD = "password";
		private final String username;
		private final Map<String, String> attributes = new TreeMap<String, String>();

		public Account(final String username) {
			this.username = username;
		}

		public String getUsername() {
			return username;
		}

		public String getPassword() {
			return getAttribute(PASSWORD);
		}

		public String getAttribute(final String key) {
			return attributes.get(key);
		}

		public void setAttribute(final String key, final String value) {
			attributes.put(key, value);
		}

		public void setPassword(final String password) {
			setAttribute(PASSWORD, password);
		}

		@Override
		public String toString() {
			return username;
		}
	}

	public static final Account FACEBOOK_ACCOUNT = new Account("Facebook");
	private static final String KEY_ALGORITHM = "DESede";
	private static final String CIPHER_TRANSFORMATION = "DESede/CBC/PKCS5Padding";
	private static final int FORMAT_VERSION = 3;

	private final File file;
	private byte[] digest;
	private final String[] protectedAttributes = {Account.PASSWORD, "pin"};

	private final Map<String, Account> accounts = new TreeMap<String, Account>();

	public AccountStore(final File file) {
		RestrictedSecurityManager.assertNonScript();
		final StackTraceElement[] s = Thread.currentThread().getStackTrace();
		if (s.length < 3 ||
				!s[0].getClassName().equals(Thread.class.getName()) ||
				!s[1].getClassName().equals(AccountStore.class.getName()) ||
				!s[2].getClassName().equals(AccountManager.class.getName())) {
			throw new SecurityException();
		}
		this.file = file;
	}

	public Account get(final String username) {
		return accounts.get(username);
	}

	public void remove(final String username) {
		accounts.remove(username);
	}

	public void add(final Account account) {
		accounts.put(account.username, account);
	}

	public Collection<Account> list() {
		return accounts.values();
	}

	public void load() throws IOException {
		final BufferedReader in = new BufferedReader(new FileReader(file));
		final String header = in.readLine();
		if (header == null || header.length() == 0 || Integer.parseInt(header) != FORMAT_VERSION) {
			throw new IOException("Invalid format");
		}
		final Map<String, Map<String, String>> data = IniParser.deserialise(in);
		for (final Entry<String, Map<String, String>> entry : data.entrySet()) {
			final Account account = new Account(entry.getKey());
			for (final Entry<String, String> a : entry.getValue().entrySet()) {
				account.setAttribute(a.getKey(), Arrays.asList(protectedAttributes).contains(a.getKey()) ? decrypt(a.getValue()) : a.getValue());
			}
			accounts.put(normaliseUsername(account.getUsername()), account);
		}
		in.close();
	}

	public void save() throws IOException {
		final BufferedWriter out = new BufferedWriter(new FileWriter(file));
		out.append(Integer.toString(FORMAT_VERSION));
		out.newLine();
		final Map<String, Map<String, String>> data = new TreeMap<String, Map<String, String>>();
		for (final Entry<String, Account> account : accounts.entrySet()) {
			final Map<String, String> attributes = new TreeMap<String, String>();
			for (final Entry<String, String> a : account.getValue().attributes.entrySet()) {
				attributes.put(a.getKey(), Arrays.asList(protectedAttributes).contains(a.getKey()) ? encrypt(a.getValue()) : a.getValue());
			}
			data.put(normaliseUsername(account.getKey()), attributes);
		}
		IniParser.serialise(data, out);
		out.close();
	}

	public void setPassword(final String password) {
		if (password == null) {
			digest = null;
			return;
		}
		final MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-1");
		} catch (final NoSuchAlgorithmException ignored) {
			throw new RuntimeException("Unable to digest password!");
		}
		md.update(StringUtil.getBytesUtf8(password));
		digest = md.digest();
		digest = Arrays.copyOf(digest, 24);
		for (int i = 0, off = 20; i < 4; ++i) {
			digest[off++] = digest[i];
		}
	}

	private String encrypt(final String data) {
		final byte[] raw = StringUtil.getBytesUtf8(data);
		if (digest == null) {
			return StringUtil.byteArrayToHexString(raw);
		}
		final SecretKey key = new SecretKeySpec(digest, KEY_ALGORITHM);
		final IvParameterSpec iv = new IvParameterSpec(new byte[8]);

		byte[] enc;
		try {
			final Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
			cipher.init(Cipher.ENCRYPT_MODE, key, iv);
			enc = cipher.doFinal(raw);
		} catch (final Exception e) {
			throw new RuntimeException("Unable to encrypt data!");
		}
		return StringUtil.byteArrayToHexString(enc);
	}

	private String decrypt(final String data) throws IOException {
		final byte[] raw = StringUtil.hexStringToByteArray(data);
		if (digest == null) {
			return StringUtil.newStringUtf8(raw);
		}
		final SecretKey key = new SecretKeySpec(digest, KEY_ALGORITHM);
		final IvParameterSpec iv = new IvParameterSpec(new byte[8]);

		byte[] dec;
		try {
			final Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
			cipher.init(Cipher.DECRYPT_MODE, key, iv);
			dec = cipher.doFinal(raw);
		} catch (final Exception e) {
			throw new IOException("Unable to decrypt data!");
		}
		return StringUtil.newStringUtf8(dec);
	}

	/**
	 * Capitalises the first character and replaces spaces with underscores.
	 *
	 * @param name The name of the account
	 * @return Normalised username
	 */
	private static String normaliseUsername(String name) {
		name = name.toLowerCase().trim().replaceAll("\\s", "_");
		name = name.substring(0, 1).toUpperCase() + name.substring(1);
		return name;
	}
}
