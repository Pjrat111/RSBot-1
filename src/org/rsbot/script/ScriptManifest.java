package org.rsbot.script;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ScriptManifest {
	String name();

	double version() default 1.0;

	String description() default "";

	String[] authors();

	String[] keywords() default {};

	String website() default "";

	int requiresVersion() default 2000;

	/**
	 * Whether or not to obfuscate scripts on the server.
	 * This will be force-set to true if a license token is specified,
	 * in other words this should only be false if the script is free and open source.
	 */
	boolean obfuscated() default true;
}
