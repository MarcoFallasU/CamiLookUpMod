package io.github.marcofallasu.camilookup.api;

import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.api.event.RecordEvent;

/**
 * Fired once during common setup, on both physical sides, so addons can register providers, pin actions and
 * restriction rules. Add a listener from your mod constructor:
 *
 * <pre>{@code
 * RegisterLookUpEvent.BUS.addListener(event -> event.registrar().registerBlockProvider(...));
 * }</pre>
 */
public record RegisterLookUpEvent(LookUpRegistrar registrar) implements RecordEvent {
    public static final EventBus<RegisterLookUpEvent> BUS = EventBus.create(RegisterLookUpEvent.class);
}
