package net.xelpy.moreroad.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.xelpy.moreroad.MoreRoad;

public record UpdateE31TextPayload(
        BlockPos pos,
        String text
) implements CustomPacketPayload {

    public static final Type<UpdateE31TextPayload> TYPE =
            new Type<>(
                    Identifier.fromNamespaceAndPath(
                            MoreRoad.MODID,
                            "update_e31_text"
                    )
            );

    public static final StreamCodec<ByteBuf, UpdateE31TextPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public UpdateE31TextPayload decode(ByteBuf buffer) {
                    return new UpdateE31TextPayload(
                            BlockPos.STREAM_CODEC.decode(buffer),
                            ByteBufCodecs.STRING_UTF8.decode(buffer)
                    );
                }

                @Override
                public void encode(
                        ByteBuf buffer,
                        UpdateE31TextPayload payload
                ) {
                    BlockPos.STREAM_CODEC.encode(buffer, payload.pos());
                    ByteBufCodecs.STRING_UTF8.encode(
                            buffer,
                            normalizeText(payload.text())
                    );
                }
            };

    public UpdateE31TextPayload {
        text = normalizeText(text);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static String normalizeText(String text) {
        return text == null ? "" : text;
    }
}
