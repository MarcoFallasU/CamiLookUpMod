package io.github.marcofallasu.camilookup.api.provider;

/** Where a provider runs. */
public enum ProviderSide {
    /**
     * Runs on the client with the data the client already knows. Always available, even when the server does not have
     * Cami LookUp installed.
     */
    CLIENT,
    /** Runs on the server; its result is sent to the client. Only available when the server has Cami LookUp. */
    SERVER
}
