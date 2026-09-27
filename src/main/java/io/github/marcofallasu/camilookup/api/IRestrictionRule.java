package io.github.marcofallasu.camilookup.api;

import io.github.marcofallasu.camilookup.api.target.LookUpAccessor;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-side rule that can hide information from players. Every registered rule must allow a request for it to be
 * answered.
 */
public interface IRestrictionRule {
    /** Whether the player may receive any server information about the target. */
    default boolean canInspect(ServerPlayer player, LookUpAccessor target) {
        return true;
    }

    /** Whether the player may receive the information of one category about the target. */
    default boolean canShowCategory(ServerPlayer player, LookUpAccessor target, Identifier category) {
        return true;
    }
}
