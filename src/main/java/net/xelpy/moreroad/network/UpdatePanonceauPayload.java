package net.xelpy.moreroad.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.xelpy.moreroad.MoreRoad;
import net.xelpy.moreroad.block.custom.PanonceauEntry;
import net.xelpy.moreroad.block.custom.PanonceauVariant;
import net.xelpy.moreroad.block.entity.PanonceauBlockEntity;

public record UpdatePanonceauPayload(
        BlockPos pos,
        PanonceauEntry entry0,
        PanonceauEntry entry1,
        PanonceauEntry entry2
) implements CustomPacketPayload {

    public static final Type<UpdatePanonceauPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(MoreRoad.MODID, "update_panonceau"));

    public static final StreamCodec<ByteBuf, UpdatePanonceauPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public UpdatePanonceauPayload decode(ByteBuf buffer) {
                    return new UpdatePanonceauPayload(
                            BlockPos.STREAM_CODEC.decode(buffer),
                            decodeEntry(buffer),
                            decodeEntry(buffer),
                            decodeEntry(buffer)
                    );
                }

                @Override
                public void encode(ByteBuf buffer, UpdatePanonceauPayload payload) {
                    BlockPos.STREAM_CODEC.encode(buffer, payload.pos());
                    for (int i = 0; i < PanonceauBlockEntity.MAX_PANONCEAUX; i++) {
                        encodeEntry(buffer, payload.entry(i));
                    }
                }
            };

    public UpdatePanonceauPayload {
        entry0 = normalize(entry0);
        entry1 = normalize(entry1);
        entry2 = normalize(entry2);
    }

    public PanonceauEntry entry(int index) {
        return switch (index) {
            case 0 -> entry0;
            case 1 -> entry1;
            case 2 -> entry2;
            default -> PanonceauEntry.disabled();
        };
    }

    private static PanonceauEntry normalize(PanonceauEntry entry) {
        return entry == null ? PanonceauEntry.disabled() : entry;
    }

    private static void encodeEntry(ByteBuf buffer, PanonceauEntry entry) {
        ByteBufCodecs.BOOL.encode(buffer, entry.enabled());
        ByteBufCodecs.STRING_UTF8.encode(buffer, entry.variant().getSerializedName());
        ByteBufCodecs.STRING_UTF8.encode(buffer, entry.value());
    }

    private static PanonceauEntry decodeEntry(ByteBuf buffer) {
        return new PanonceauEntry(
                ByteBufCodecs.BOOL.decode(buffer),
                PanonceauVariant.fromSerializedName(ByteBufCodecs.STRING_UTF8.decode(buffer)),
                ByteBufCodecs.STRING_UTF8.decode(buffer)
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
