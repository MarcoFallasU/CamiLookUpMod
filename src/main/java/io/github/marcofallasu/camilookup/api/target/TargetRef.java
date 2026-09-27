package io.github.marcofallasu.camilookup.api.target;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Lightweight, network-safe reference to an inspected target: either a block position or an entity id.
 */
public sealed interface TargetRef permits TargetRef.BlockRef, TargetRef.EntityRef {
    StreamCodec<ByteBuf, TargetRef> STREAM_CODEC = StreamCodec.of(
            (buf, ref) -> {
                switch (ref) {
                    case BlockRef block -> {
                        buf.writeByte(0);
                        BlockPos.STREAM_CODEC.encode(buf, block.pos());
                    }
                    case EntityRef entity -> {
                        buf.writeByte(1);
                        ByteBufCodecs.VAR_INT.encode(buf, entity.entityId());
                    }
                }
            },
            buf -> switch (buf.readByte()) {
                case 0 -> new BlockRef(BlockPos.STREAM_CODEC.decode(buf));
                case 1 -> new EntityRef(ByteBufCodecs.VAR_INT.decode(buf));
                default -> throw new IllegalArgumentException("Unknown target type");
            });

    record BlockRef(BlockPos pos) implements TargetRef {
        public BlockRef {
            pos = pos.immutable();
        }
    }

    record EntityRef(int entityId) implements TargetRef {
    }
}
