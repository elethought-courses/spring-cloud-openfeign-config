package tech.elethoughts.courses.cloud.feign.infrastructure;

import org.springframework.core.env.Environment;

public class FeignClientProperties {

    private static final String PREFIX = "spring.cloud.openfeign.client.config.";

    private final Environment env;
    private final String clientName;

    public FeignClientProperties(Environment env, String clientName) {
        this.env = env;
        this.clientName = clientName;
    }

    public String getClientName() {
        return clientName;
    }

    public boolean isProxyEnabled() {
        return getBoolean("proxy.enabled", false);
    }

    public String getProxyHost() {
        return getString("proxy.host");
    }

    public int getProxyPort() {
        return getInt("proxy.port", 8080);
    }

    public boolean isTlsEnabled() {
        return getBoolean("tls.enabled", false);
    }

    public String getTrustStore() {
        return getString("tls.trust-store");
    }

    public char[] getTrustStorePassword() {
        return getString("tls.trust-store-password", "").toCharArray();
    }

    public String getKeyStore() {
        return getString("tls.key-store");
    }

    public char[] getKeyStorePassword() {
        return getString("tls.key-store-password", "").toCharArray();
    }

    public boolean isVerifyHostname() {
        return getBoolean("tls.verify-hostname", true);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return Boolean.parseBoolean(env.getProperty(PREFIX + clientName + "." + key, String.valueOf(defaultValue)));
    }

    public String getString(String key) {
        return env.getProperty(PREFIX + clientName + "." + key);
    }

    public String getString(String key, String defaultValue) {
        return env.getProperty(PREFIX + clientName + "." + key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        return Integer.parseInt(env.getProperty(PREFIX + clientName + "." + key, String.valueOf(defaultValue)));
    }
}
