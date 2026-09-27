package io.github.marcofallasu.camilookup.vanilla;

import io.github.marcofallasu.camilookup.api.info.InfoBuilder;
import io.github.marcofallasu.camilookup.api.target.EntityAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

final class EntityProviders {
    /** {@code ItemEntity} age value meaning the item never despawns. */
    private static final int INFINITE_ITEM_AGE = -32768;

    private EntityProviders() {
    }

    /** Client: name and icon. */
    static void appendBasic(EntityAccessor accessor, InfoBuilder builder) {
        Entity entity = accessor.entity();
        builder.title(entity.getDisplayName());
        ItemStack pick = entity.getPickResult();
        if (pick != null && !pick.isEmpty()) {
            builder.icon(pick);
        }
    }

    /** Client: health, armor, baby state and equipment — everything visible to any player. */
    static void appendLiving(EntityAccessor accessor, InfoBuilder builder) {
        if (!(accessor.entity() instanceof LivingEntity living)) {
            return;
        }
        if (!(living instanceof ArmorStand)) {
            builder.health(living.getHealth(), living.getMaxHealth());
            int armor = living.getArmorValue();
            if (armor > 0) {
                builder.text(Format.labeled("camilookup.entity.armor", String.valueOf(armor)));
            }
            if (living.isBaby()) {
                builder.text(Component.translatable("camilookup.entity.baby").withStyle(ChatFormatting.GRAY));
            }
        }
        List<ItemStack> equipment = new ArrayList<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = living.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                equipment.add(stack);
            }
        }
        if (!equipment.isEmpty()) {
            builder.itemRow(equipment);
        }
    }

    /** Client: the item and amount of a dropped item. */
    static void appendItemEntity(EntityAccessor accessor, InfoBuilder builder) {
        if (accessor.entity() instanceof ItemEntity item) {
            ItemStack stack = item.getItem();
            builder.icon(stack);
            MutableComponent title = stack.getHoverName().copy();
            if (stack.getCount() > 1) {
                title.append(Component.literal(" ×" + stack.getCount()).withStyle(ChatFormatting.GRAY));
            }
            builder.title(title);
        }
    }

    /** Client: the item held by an item frame and its rotation. */
    static void appendItemFrame(EntityAccessor accessor, InfoBuilder builder) {
        if (accessor.entity() instanceof ItemFrame frame && !frame.getItem().isEmpty()) {
            ItemStack stack = frame.getItem();
            builder.iconText(stack, stack.getHoverName());
            builder.detail(Format.labeled("camilookup.entity.rotation", frame.getRotation() + "/8"));
        }
    }

    /** Server: active status effects of mobs. */
    static void appendEffects(EntityAccessor accessor, InfoBuilder builder) {
        if (accessor.entity() instanceof LivingEntity living && !(living instanceof Player)) {
            appendEffectList(accessor, living, builder);
        }
    }

    /** Server: active status effects of other players, only when the server shares player details. */
    static void appendPlayerEffects(EntityAccessor accessor, InfoBuilder builder) {
        if (accessor.entity() instanceof Player player) {
            appendEffectList(accessor, player, builder);
        }
    }

    private static void appendEffectList(EntityAccessor accessor, LivingEntity living, InfoBuilder builder) {
        float tickRate = accessor.level().tickRateManager().tickrate();
        for (MobEffectInstance effect : living.getActiveEffects()) {
            if (!effect.isVisible()) {
                continue;
            }
            MutableComponent line = effect.getEffect().value().getDisplayName().copy();
            if (effect.getAmplifier() > 0) {
                line.append(" ").append(Component.translatable("potion.potency." + effect.getAmplifier()));
            }
            line.append(Component.literal(" (").append(MobEffectUtil.formatDuration(effect, 1.0F, tickRate)).append(")")
                    .withStyle(ChatFormatting.GRAY));
            builder.text(line.withStyle(effect.getEffect().value().getCategory().getTooltipFormatting()));
        }
    }

    /** Server: time until a baby grows up or an adult can breed again. */
    static void appendAge(EntityAccessor accessor, InfoBuilder builder) {
        if (!(accessor.entity() instanceof AgeableMob ageable)) {
            return;
        }
        int age = ageable.getAge();
        if (age < 0) {
            builder.text(Format.labeled("camilookup.entity.grows_in", Format.duration(-age)));
        } else if (age > 0 && ageable instanceof Animal) {
            builder.text(Format.labeled("camilookup.entity.breed_cooldown", Format.duration(age)));
        }
        if (ageable instanceof Animal animal && animal.isInLove()) {
            builder.text(Component.translatable("camilookup.entity.in_love").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /** Server: time left before a dropped item despawns. */
    static void appendItemDespawn(EntityAccessor accessor, InfoBuilder builder) {
        if (!(accessor.entity() instanceof ItemEntity item)) {
            return;
        }
        if (item.getAge() == INFINITE_ITEM_AGE) {
            builder.text(Component.translatable("camilookup.entity.never_despawns").withStyle(ChatFormatting.GRAY));
        } else {
            builder.text(Format.labeled("camilookup.entity.despawns_in", Format.duration(item.lifespan - item.getAge())));
        }
    }
}
