package io.github.marcofallasu.camilookup.vanilla;

import io.github.marcofallasu.camilookup.api.info.InfoBuilder;
import io.github.marcofallasu.camilookup.api.target.BlockAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.EndPortalFrameBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import java.util.ArrayList;
import java.util.List;

/** Lecterns, signs, jukeboxes, note blocks, chiseled bookshelves, beacons, cauldrons and end portal frames. */
final class UtilityProvider {
    private static final String[] NOTE_NAMES = {"F#", "G", "G#", "A", "A#", "B", "C", "C#", "D", "D#", "E", "F"};
    private static final int BOOKSHELF_COLUMNS = 3;

    private UtilityProvider() {
    }

    /** Client: block states and the sign text, which the client already has. */
    static void appendClient(BlockAccessor accessor, InfoBuilder builder) {
        BlockState state = accessor.state();
        Block block = state.getBlock();
        if (block instanceof NoteBlock) {
            int note = state.getValue(NoteBlock.NOTE);
            builder.text(Format.labeled("camilookup.utility.note",
                    Component.literal(NOTE_NAMES[note % NOTE_NAMES.length] + " (" + note + ")")));
            builder.text(Format.labeled("camilookup.utility.instrument",
                    Component.translatable("camilookup.instrument." + state.getValue(NoteBlock.INSTRUMENT).getSerializedName())));
        } else if (block instanceof ChiseledBookShelfBlock) {
            int books = 0;
            for (BooleanProperty slot : ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES) {
                books += state.getValue(slot) ? 1 : 0;
            }
            builder.text(Format.labeled("camilookup.utility.books",
                    Format.fraction(books, ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES.size())));
        } else if (block instanceof LayeredCauldronBlock) {
            builder.text(Format.labeled("camilookup.utility.contents", cauldronLiquid(block)));
            builder.text(Format.labeled("camilookup.utility.level",
                    Format.fraction(state.getValue(LayeredCauldronBlock.LEVEL), LayeredCauldronBlock.MAX_FILL_LEVEL)));
        } else if (block == Blocks.LAVA_CAULDRON) {
            builder.text(Format.labeled("camilookup.utility.contents", Component.translatable("block.minecraft.lava")));
        } else if (block == Blocks.CAULDRON) {
            builder.text(Format.labeled("camilookup.utility.contents", Component.translatable("camilookup.container.empty")));
        } else if (block instanceof EndPortalFrameBlock) {
            builder.text(Format.labeled("camilookup.utility.eye", Format.yesNo(state.getValue(EndPortalFrameBlock.HAS_EYE))));
        }

        if (accessor.blockEntity() instanceof SignBlockEntity sign) {
            appendSignSide(builder, "camilookup.utility.sign_front", sign.getFrontText());
            appendSignSide(builder, "camilookup.utility.sign_back", sign.getBackText());
        }
    }

    /** Server: lectern page, jukebox disc, bookshelf contents and beacon effects. */
    static void appendServer(BlockAccessor accessor, InfoBuilder builder) {
        switch (accessor.blockEntity()) {
            case LecternBlockEntity lectern when lectern.hasBook() -> {
                ItemStack book = lectern.getBook();
                builder.iconText(book, book.getHoverName());
                int pages = pageCount(book);
                builder.text(Format.labeled("camilookup.utility.page",
                        Format.fraction(lectern.getPage() + 1, Math.max(1, pages))));
            }
            case JukeboxBlockEntity jukebox when !jukebox.getTheItem().isEmpty() -> {
                ItemStack disc = jukebox.getTheItem();
                Component song = JukeboxSong.fromStack(accessor.level().registryAccess(), disc)
                        .map(holder -> holder.value().description())
                        .orElse(disc.getHoverName());
                builder.iconText(disc, song);
                if (jukebox.getSongPlayer().isPlaying()) {
                    builder.text(Component.translatable("camilookup.utility.playing").withStyle(ChatFormatting.GREEN));
                }
            }
            case ChiseledBookShelfBlockEntity shelf -> builder.items(new ArrayList<>(shelf.getItems()), BOOKSHELF_COLUMNS);
            case BeaconBlockEntity beacon -> appendBeacon(accessor, beacon, builder);
            case null, default -> {
            }
        }
    }

    private static void appendBeacon(BlockAccessor accessor, BeaconBlockEntity beacon, InfoBuilder builder) {
        CompoundTag data = beacon.saveCustomOnly(accessor.level().registryAccess());
        builder.text(Format.labeled("camilookup.utility.beacon_level", Format.fraction(data.getIntOr("Levels", 0), 4)));
        Holder<MobEffect> primary = effect(data.getStringOr("primary_effect", ""));
        Holder<MobEffect> secondary = effect(data.getStringOr("secondary_effect", ""));
        if (primary != null) {
            MutableComponent name = primary.value().getDisplayName().copy();
            if (primary.equals(secondary)) {
                name.append(" ").append(Component.translatable("potion.potency.1"));
            }
            builder.text(Format.labeled("camilookup.utility.beacon_primary", name));
        }
        if (secondary != null && !secondary.equals(primary)) {
            builder.text(Format.labeled("camilookup.utility.beacon_secondary", secondary.value().getDisplayName()));
        }
    }

    private static Holder<MobEffect> effect(String id) {
        Identifier identifier = Identifier.tryParse(id);
        return identifier == null ? null : BuiltInRegistries.MOB_EFFECT.get(identifier).orElse(null);
    }

    private static int pageCount(ItemStack book) {
        WrittenBookContent written = book.get(DataComponents.WRITTEN_BOOK_CONTENT);
        if (written != null) {
            return written.pages().size();
        }
        WritableBookContent writable = book.get(DataComponents.WRITABLE_BOOK_CONTENT);
        return writable != null ? writable.pages().size() : 0;
    }

    private static Component cauldronLiquid(Block block) {
        if (block == Blocks.POWDER_SNOW_CAULDRON) {
            return Component.translatable("block.minecraft.powder_snow");
        }
        return Component.translatable("block.minecraft.water");
    }

    private static void appendSignSide(InfoBuilder builder, String labelKey, SignText text) {
        List<Component> lines = new ArrayList<>();
        for (int i = 0; i < SignText.LINES; i++) {
            Component line = text.getMessage(i, false);
            if (!line.getString().isBlank()) {
                lines.add(line);
            }
        }
        if (lines.isEmpty()) {
            return;
        }
        builder.text(Component.translatable(labelKey).withStyle(ChatFormatting.GRAY));
        for (Component line : lines) {
            builder.text(Component.literal("  ").append(line.copy().withStyle(ChatFormatting.WHITE)));
        }
    }
}
