package com.webclient.lib.auth;

/**
 * Callback interface for providing bearer tokens to the HTTP client.
 * <p>
 * Implementations should handle token caching, expiry checking, and refresh logic.
 * The {@link #getToken()} method is called before each HTTP request (including retries),
 * so implementations can return a cached token or issue a new one as needed.
 */
@FunctionalInterface
public interface BearerTokenProvider {

    /**
     * Returns a valid bearer token. Implementations should check if the current token
     * is expired and issue a new authentication token as needed.
     *
     * @return the bearer token string, or {@code null} to skip token injection
     */
    String getToken();
}
