package net.xelpy.moreroad.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.xelpy.moreroad.MoreRoad;
import net.xelpy.moreroad.block.custom.CartoucheType;

public record UpdateEB10TextPayload(
        BlockPos pos,
        String line1,
        String line2,
        boolean eb20,
        CartoucheType cartoucheType,
        String cartoucheText
) implements CustomPacketPayload {

    public static final Type<UpdateEB10TextPayload> TYPE =
            new Type<>(
                    Identifier.fromNamespaceAndPath(
                            MoreRoad.MODID,
                            "update_eb10_text"
                    )
            );

    public static final StreamCodec<ByteBuf, UpdateEB10TextPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public UpdateEB10TextPayload decode(ByteBuf buffer) {
                    return new UpdateEB10TextPayload(
                            BlockPos.STREAM_CODEC.decode(buffer),
                            ByteBufCodecs.STRING_UTF8.decode(buffer),
                            ByteBufCodecs.STRING_UTF8.decode(buffer),
                            ByteBufCodecs.BOOL.decode(buffer),
                            CartoucheType.fromSerializedName(
                                    ByteBufCodecs.STRING_UTF8.decode(buffer)
                            ),
                            ByteBufCodecs.STRING_UTF8.decode(buffer)
                    );
                }

                @Override
                public void encode(
                        ByteBuf buffer,
                        UpdateEB10TextPayload payload
                ) {
                    BlockPos.STREAM_CODEC.encode(buffer, payload.pos());
                    ByteBufCodecs.STRING_UTF8.encode(buffer, payload.line1());
                    ByteBufCodecs.STRING_UTF8.encode(buffer, payload.line2());
                    ByteBufCodecs.BOOL.encode(buffer, payload.eb20());
                    ByteBufCodecs.STRING_UTF8.encode(
                            buffer,
                            normalizeCartoucheType(payload.cartoucheType())
                                    .getSerializedName()
                    );
                    ByteBufCodecs.STRING_UTF8.encode(
                            buffer,
                            normalizeText(payload.cartoucheText())
                    );
                }
            };

    public UpdateEB10TextPayload {
        cartoucheType = normalizeCartoucheType(cartoucheType);
        cartoucheText = normalizeText(cartoucheText);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static CartoucheType normalizeCartoucheType(
            CartoucheType cartoucheType
    ) {
        return cartoucheType == null
                ? CartoucheType.NONE
                : cartoucheType;
    }

    private static String normalizeText(String text) {
        return text == null ? "" : text;
    }
}
