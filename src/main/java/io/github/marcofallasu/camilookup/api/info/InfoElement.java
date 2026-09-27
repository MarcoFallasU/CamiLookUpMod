package io.github.marcofallasu.camilookup.api.info;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * One piece of content inside a Cami LookUp box. Elements are plain data so they can be produced on the server and
 * sent to the client.
 */
public sealed interface InfoElement permits InfoElement.Text, InfoElement.IconText, InfoElement.ItemGrid,
        InfoElement.ItemRow, InfoElement.Health, InfoElement.Process {

    StreamCodec<RegistryFriendlyByteBuf, List<ItemStack>> STACKS_CODEC =
            ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list(4096));

    StreamCodec<RegistryFriendlyByteBuf, InfoElement> STREAM_CODEC = StreamCodec.of(
            (buf, element) -> {
                buf.writeByte(element.type());
                buf.writeByte(element.visibility().ordinal());
                switch (element) {
                    case Text text -> ComponentSerialization.STREAM_CODEC.encode(buf, text.text());
                    case IconText iconText -> {
                        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, iconText.icon());
                        ComponentSerialization.STREAM_CODEC.encode(buf, iconText.text());
                    }
                    case ItemGrid grid -> {
                        STACKS_CODEC.encode(buf, grid.slots());
                        buf.writeVarInt(grid.columns());
                    }
                    case ItemRow row -> STACKS_CODEC.encode(buf, row.items());
                    case Health health -> {
                        buf.writeFloat(health.health());
                        buf.writeFloat(health.maxHealth());
                    }
                    case Process process -> {
                        STACKS_CODEC.encode(buf, process.inputs());
                        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, process.fuel());
                        STACKS_CODEC.encode(buf, process.outputs());
                        buf.writeFloat(process.progress());
                        buf.writeFloat(process.fuelLevel());
                    }
                }
            },
            buf -> {
                int type = buf.readByte();
                Visibility visibility = Visibility.values()[buf.readByte()];
                return switch (type) {
                    case 0 -> new Text(ComponentSerialization.STREAM_CODEC.decode(buf), visibility);
                    case 1 -> new IconText(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf),
                            ComponentSerialization.STREAM_CODEC.decode(buf), visibility);
                    case 2 -> new ItemGrid(STACKS_CODEC.decode(buf), buf.readVarInt(), visibility);
                    case 3 -> new ItemRow(STACKS_CODEC.decode(buf), visibility);
                    case 4 -> new Health(buf.readFloat(), buf.readFloat(), visibility);
                    case 5 -> new Process(STACKS_CODEC.decode(buf), ItemStack.OPTIONAL_STREAM_CODEC.decode(buf),
                            STACKS_CODEC.decode(buf), buf.readFloat(), buf.readFloat(), visibility);
                    default -> throw new IllegalArgumentException("Unknown info element type " + type);
                };
            });

    Visibility visibility();

    int type();

    /** A single line of text. */
    record Text(Component text, Visibility visibility) implements InfoElement {
        @Override
        public int type() {
            return 0;
        }
    }

    /** A line of text preceded by an item icon. */
    record IconText(ItemStack icon, Component text, Visibility visibility) implements InfoElement {
        public IconText {
            icon = icon.copy();
        }

        @Override
        public int type() {
            return 1;
        }
    }

    /**
     * An inventory shown as a grid of item icons. {@code slots} contains every slot, including empty ones: the summary
     * shows only the non-empty stacks, the detail shows the full grid.
     */
    record ItemGrid(List<ItemStack> slots, int columns, Visibility visibility) implements InfoElement {
        public ItemGrid {
            slots = slots.stream().map(ItemStack::copy).toList();
            columns = Math.max(1, columns);
        }

        @Override
        public int type() {
            return 2;
        }
    }

    /** A compact row of item icons, such as worn equipment. Empty stacks are skipped when rendering. */
    record ItemRow(List<ItemStack> items, Visibility visibility) implements InfoElement {
        public ItemRow {
            items = items.stream().map(ItemStack::copy).toList();
        }

        @Override
        public int type() {
            return 3;
        }
    }

    /** Health shown as hearts, or as a number when there are too many hearts. */
    record Health(float health, float maxHealth, Visibility visibility) implements InfoElement {
        @Override
        public int type() {
            return 4;
        }
    }

    /**
     * A machine that turns inputs into outputs, drawn like a furnace: inputs, a progress arrow and outputs, with an
     * optional fuel slot and flame.
     *
     * @param progress  progress of the current operation, from 0 to 1
     * @param fuelLevel remaining fuel from 0 to 1, or a negative value when the machine has no fuel slot
     */
    record Process(List<ItemStack> inputs, ItemStack fuel, List<ItemStack> outputs, float progress, float fuelLevel,
                   Visibility visibility) implements InfoElement {
        public Process {
            inputs = inputs.stream().map(ItemStack::copy).toList();
            fuel = fuel.copy();
            outputs = outputs.stream().map(ItemStack::copy).toList();
            progress = Math.clamp(progress, 0.0F, 1.0F);
            fuelLevel = Math.min(fuelLevel, 1.0F);
        }

        public boolean hasFuel() {
            return fuelLevel >= 0;
        }

        @Override
        public int type() {
            return 5;
        }
    }
}
