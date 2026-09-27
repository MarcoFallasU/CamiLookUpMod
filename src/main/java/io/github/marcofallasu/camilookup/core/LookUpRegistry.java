package io.github.marcofallasu.camilookup.core;

import io.github.marcofallasu.camilookup.api.IPinAction;
import io.github.marcofallasu.camilookup.api.IRestrictionRule;
import io.github.marcofallasu.camilookup.api.LookUpRegistrar;
import io.github.marcofallasu.camilookup.api.RegisterLookUpEvent;
import io.github.marcofallasu.camilookup.api.provider.IBlockInfoProvider;
import io.github.marcofallasu.camilookup.api.provider.IEntityInfoProvider;
import io.github.marcofallasu.camilookup.api.provider.ProviderSide;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class LookUpRegistry implements LookUpRegistrar {
    public record Entry<P>(Identifier id, Identifier category, int priority, P provider) {
    }

    public record PinActionEntry(Identifier id, int priority, IPinAction action) {
    }

    private static final LookUpRegistry INSTANCE = new LookUpRegistry();

    private final List<Entry<IBlockInfoProvider>> clientBlock = new ArrayList<>();
    private final List<Entry<IBlockInfoProvider>> serverBlock = new ArrayList<>();
    private final List<Entry<IEntityInfoProvider>> clientEntity = new ArrayList<>();
    private final List<Entry<IEntityInfoProvider>> serverEntity = new ArrayList<>();
    private final List<PinActionEntry> pinActions = new ArrayList<>();
    private final List<IRestrictionRule> restrictionRules = new ArrayList<>();
    private boolean frozen;

    private LookUpRegistry() {
    }

    public static LookUpRegistry get() {
        return INSTANCE;
    }

    /** Fires {@link RegisterLookUpEvent} and freezes the registry. */
    public static void load() {
        RegisterLookUpEvent.BUS.post(new RegisterLookUpEvent(INSTANCE));
        Comparator<Entry<?>> order = Comparator.comparingInt(Entry::priority);
        INSTANCE.clientBlock.sort(order);
        INSTANCE.serverBlock.sort(order);
        INSTANCE.clientEntity.sort(order);
        INSTANCE.serverEntity.sort(order);
        INSTANCE.pinActions.sort(Comparator.comparingInt(PinActionEntry::priority));
        INSTANCE.frozen = true;
    }

    @Override
    public Dist dist() {
        return FMLEnvironment.dist;
    }

    @Override
    public void registerBlockProvider(ProviderSide side, Identifier id, Identifier category, int priority, IBlockInfoProvider provider) {
        checkOpen();
        if (side == ProviderSide.SERVER) {
            serverBlock.add(new Entry<>(id, category, priority, provider));
        } else if (dist().isClient()) {
            clientBlock.add(new Entry<>(id, category, priority, provider));
        }
    }

    @Override
    public void registerEntityProvider(ProviderSide side, Identifier id, Identifier category, int priority, IEntityInfoProvider provider) {
        checkOpen();
        if (side == ProviderSide.SERVER) {
            serverEntity.add(new Entry<>(id, category, priority, provider));
        } else if (dist().isClient()) {
            clientEntity.add(new Entry<>(id, category, priority, provider));
        }
    }

    @Override
    public void registerPinAction(Identifier id, int priority, IPinAction action) {
        checkOpen();
        if (dist().isClient()) {
            pinActions.add(new PinActionEntry(id, priority, action));
        }
    }

    @Override
    public void registerRestrictionRule(Identifier id, IRestrictionRule rule) {
        checkOpen();
        restrictionRules.add(rule);
    }

    private void checkOpen() {
        if (frozen) {
            throw new IllegalStateException("Cami LookUp registrations must happen during RegisterLookUpEvent");
        }
    }

    public List<Entry<IBlockInfoProvider>> blockProviders(ProviderSide side) {
        return side == ProviderSide.SERVER ? serverBlock : clientBlock;
    }

    public List<Entry<IEntityInfoProvider>> entityProviders(ProviderSide side) {
        return side == ProviderSide.SERVER ? serverEntity : clientEntity;
    }

    public List<PinActionEntry> pinActions() {
        return pinActions;
    }

    public List<IRestrictionRule> restrictionRules() {
        return restrictionRules;
    }
}
