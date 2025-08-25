package com.aireviewer.i18n;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Properties;

/**
 * Simple i18n messages loader that reads message.properties in UTF-8 encoding.
 */
public final class Messages {
    private static final String BUNDLE_PATH = "/message.properties";
    private static final Properties PROPS = new Properties();

    static {
        try (InputStream is = Messages.class.getResourceAsStream(BUNDLE_PATH)) {
            if (is != null) {
                try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    PROPS.load(reader);
                }
            } else {
                // No bundle found; keep properties empty
            }
        } catch (Exception e) {
            // Swallow exception to avoid breaking application startup; properties will be empty
        }
    }

    private Messages() {}

    public static String get(String key, Object... args) {
        String pattern = PROPS.getProperty(key, key);
        if (args == null || args.length == 0) {
            return pattern;
        }
        return MessageFormat.format(pattern, args);
    }
}
