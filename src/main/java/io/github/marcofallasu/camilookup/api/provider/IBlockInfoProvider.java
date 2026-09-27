package io.github.marcofallasu.camilookup.api.provider;

import io.github.marcofallasu.camilookup.api.info.InfoBuilder;
import io.github.marcofallasu.camilookup.api.target.BlockAccessor;

/** Adds information about an inspected block. Must not modify the world. */
@FunctionalInterface
public interface IBlockInfoProvider {
    void appendInfo(BlockAccessor accessor, InfoBuilder builder);
}
