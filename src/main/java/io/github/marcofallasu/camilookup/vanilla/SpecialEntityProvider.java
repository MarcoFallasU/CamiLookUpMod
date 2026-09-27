package io.github.marcofallasu.camilookup.vanilla;

import io.github.marcofallasu.camilookup.api.info.InfoBuilder;
import io.github.marcofallasu.camilookup.api.info.InfoElement;
import io.github.marcofallasu.camilookup.api.info.Visibility;
import io.github.marcofallasu.camilookup.api.target.EntityAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/** Pets and mounts, villagers and armor stands. */
final class SpecialEntityProvider {
    /** Converts the movement speed attribute to blocks per second for a ridden horse. */
    private static final double SPEED_TO_BLOCKS_PER_SECOND = 42.157;
    private static final int MAX_LLAMA_STRENGTH = 5;

    private SpecialEntityProvider() {
    }

    /** Client: what the client already knows about pets, mounts, villagers and armor stands. */
    static void appendClient(EntityAccessor accessor, InfoBuilder builder) {
        Entity entity = accessor.entity();
        if (entity instanceof TamableAnimal pet && pet.isTame()) {
            builder.text(Component.translatable("camilookup.entity.tamed").withStyle(ChatFormatting.GREEN));
            if (pet.isInSittingPose()) {
                builder.text(Component.translatable("camilookup.entity.sitting").withStyle(ChatFormatting.GRAY));
            }
            DyeColor collar = pet instanceof Wolf wolf ? wolf.getCollarColor() : pet instanceof Cat cat ? cat.getCollarColor() : null;
            if (collar != null) {
                builder.text(Format.labeled("camilookup.entity.collar",
                        Component.translatable("color.minecraft." + collar.getSerializedName())));
            }
        }
        if (entity instanceof AbstractHorse horse) {
            if (horse.isTamed()) {
                builder.text(Component.translatable("camilookup.entity.tamed").withStyle(ChatFormatting.GREEN));
            }
            double speed = horse.getAttributeValue(Attributes.MOVEMENT_SPEED) * SPEED_TO_BLOCKS_PER_SECOND;
            builder.text(Format.labeled("camilookup.entity.speed", Component.translatable("camilookup.entity.blocks_per_second",
                    String.format(Locale.ROOT, "%.2f", speed))));
            builder.text(Format.labeled("camilookup.entity.jump", Component.translatable("camilookup.entity.blocks",
                    String.format(Locale.ROOT, "%.2f", jumpHeight(horse.getAttributeValue(Attributes.JUMP_STRENGTH))))));
            if (horse instanceof Llama llama) {
                builder.text(Format.labeled("camilookup.entity.strength", Format.fraction(llama.getStrength(), MAX_LLAMA_STRENGTH)));
            }
        }
        if (entity instanceof Villager villager) {
            VillagerData data = villager.getVillagerData();
            builder.text(Format.labeled("camilookup.entity.profession", data.profession().value().name().copy()
                    .append(Component.literal(" (").append(Component.translatable("merchant.level." + data.level())).append(")"))));
        }
        if (entity instanceof ArmorStand stand) {
            if (stand.isSmall()) {
                builder.detail(Component.translatable("camilookup.entity.small").withStyle(ChatFormatting.GRAY));
            }
            if (stand.showArms()) {
                builder.detail(Component.translatable("camilookup.entity.arms").withStyle(ChatFormatting.GRAY));
            }
            if (!stand.showBasePlate()) {
                builder.detail(Component.translatable("camilookup.entity.no_base_plate").withStyle(ChatFormatting.GRAY));
            }
        }
    }

    /** Server: owners, villager trades, bed and workplace. */
    static void appendServer(EntityAccessor accessor, InfoBuilder builder) {
        Entity entity = accessor.entity();
        EntityReference<?> owner = entity instanceof TamableAnimal pet ? pet.getOwnerReference()
                : entity instanceof AbstractHorse horse ? horse.getOwnerReference() : null;
        if (owner != null && accessor.level().getServer() != null) {
            builder.text(Format.labeled("camilookup.entity.owner", ownerName(accessor.level().getServer(), owner.getUUID())));
        }

        if (entity instanceof AbstractVillager merchant) {
            List<MerchantOffer> offers = merchant.getOffers();
            if (!offers.isEmpty()) {
                builder.text(Format.labeled("camilookup.entity.trades", String.valueOf(offers.size())));
            }
            for (MerchantOffer offer : offers) {
                ItemStack costB = offer.getCostB();
                List<ItemStack> inputs = costB.isEmpty() ? List.of(offer.getCostA()) : List.of(offer.getCostA(), costB);
                builder.add(new InfoElement.Process(inputs, ItemStack.EMPTY, List.of(offer.getResult()),
                        offer.isOutOfStock() ? 0.0F : 1.0F, -1.0F, Visibility.DETAIL));
            }
        }
        if (entity instanceof Villager villager) {
            villager.getBrain().getMemory(MemoryModuleType.HOME)
                    .ifPresent(pos -> builder.text(Format.labeled("camilookup.entity.bed", position(pos))));
            villager.getBrain().getMemory(MemoryModuleType.JOB_SITE)
                    .ifPresent(pos -> builder.text(Format.labeled("camilookup.entity.job_site", position(pos))));
        }
    }

    private static Component ownerName(MinecraftServer server, UUID id) {
        ServerPlayer online = server.getPlayerList().getPlayer(id);
        if (online != null) {
            return online.getName();
        }
        Optional<String> cached = server.services().nameToIdCache().get(id).map(profile -> profile.name());
        return Component.literal(cached.orElse(id.toString().substring(0, 8)));
    }

    private static String position(GlobalPos pos) {
        BlockPos block = pos.pos();
        return block.getX() + ", " + block.getY() + ", " + block.getZ();
    }

    /** Jump height in blocks for a horse's jump strength (approximation used by the community). */
    private static double jumpHeight(double strength) {
        return -0.1817584952 * Math.pow(strength, 3) + 3.689713992 * strength * strength
                + 2.128599134 * strength - 0.343930367;
    }
}
