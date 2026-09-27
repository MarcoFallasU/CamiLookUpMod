package io.github.marcofallasu.camilookup.api;

import io.github.marcofallasu.camilookup.api.target.LookUpAccessor;

/**
 * Action run on the client when the player right-clicks a pinned box, for example to open another mod's screen.
 * The first registered action (by priority) whose {@link #appliesTo} returns {@code true} is used.
 */
public interface IPinAction {
    boolean appliesTo(LookUpAccessor target);

    void onRightClick(LookUpAccessor target);
}
