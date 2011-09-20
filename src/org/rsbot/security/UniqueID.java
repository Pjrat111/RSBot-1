package org.rsbot.security;

import org.rsbot.Configuration;
import org.rsbot.util.StringUtil;
import org.rsbot.util.io.IOHelper;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class UniqueID {
	private static final int LENGTH = 64;
	private static File store = new File(Configuration.Paths.getSettingsDirectory(), "random.dat");

	public static String getID() {
		final MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-1");
		} catch (final NoSuchAlgorithmException ignored) {
			return store.getAbsolutePath();
		}
		if (store.exists()) {
			md.update(IOHelper.read(store));
		} else {
			final byte[] d = new byte[LENGTH];
			new SecureRandom().nextBytes(d);
			IOHelper.write(new ByteArrayInputStream(d), store);
			md.update(d);
		}
		md.update(StringUtil.getBytesUtf8(Configuration.NAME));
		md.update(StringUtil.getBytesUtf8(Configuration.Paths.URLs.CLIENTPATCH));
		return StringUtil.byteArrayToHexString(md.digest());
	}
}
