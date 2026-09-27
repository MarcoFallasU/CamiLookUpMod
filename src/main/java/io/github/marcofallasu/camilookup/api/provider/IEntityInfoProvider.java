package io.github.marcofallasu.camilookup.api.provider;

import io.github.marcofallasu.camilookup.api.info.InfoBuilder;
import io.github.marcofallasu.camilookup.api.target.EntityAccessor;

/** Adds information about an inspected entity. Must not modify the world. */
@FunctionalInterface
public interface IEntityInfoProvider {
    void appendInfo(EntityAccessor accessor, InfoBuilder builder);
}
