package io.github.marcofallasu.camilookup.api.info;

/** Controls in which box mode an element is shown. */
public enum Visibility {
    /** Shown in the summary and in the full detail. */
    ALWAYS,
    /** Shown only in the summary (hover without SHIFT). */
    SUMMARY,
    /** Shown only in the full detail (SHIFT or a pinned box). */
    DETAIL;

    public boolean isShown(boolean detailed) {
        return switch (this) {
            case ALWAYS -> true;
            case SUMMARY -> !detailed;
            case DETAIL -> detailed;
        };
    }
}
