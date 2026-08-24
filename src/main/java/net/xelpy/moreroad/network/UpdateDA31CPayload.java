package net.xelpy.moreroad.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.xelpy.moreroad.MoreRoad;

public record UpdateDA31CPayload(
        BlockPos pos,
        String line1,
        String line2,
        String cartoucheLeftType,
        String cartoucheLeftText,
        String cartoucheRightType,
        String cartoucheRightText
) implements CustomPacketPayload {

    public static final Type<UpdateDA31CPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(MoreRoad.MODID, "update_da31c"));

    public static final StreamCodec<ByteBuf, UpdateDA31CPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public UpdateDA31CPayload decode(ByteBuf buffer) {
                    return new UpdateDA31CPayload(
                            BlockPos.STREAM_CODEC.decode(buffer),
                            ByteBufCodecs.STRING_UTF8.decode(buffer),
                            ByteBufCodecs.STRING_UTF8.decode(buffer),
                            ByteBufCodecs.STRING_UTF8.decode(buffer),
                            ByteBufCodecs.STRING_UTF8.decode(buffer),
                            ByteBufCodecs.STRING_UTF8.decode(buffer),
                            ByteBufCodecs.STRING_UTF8.decode(buffer)
                    );
                }

                @Override
                public void encode(ByteBuf buffer, UpdateDA31CPayload payload) {
                    BlockPos.STREAM_CODEC.encode(buffer, payload.pos());
                    ByteBufCodecs.STRING_UTF8.encode(buffer, payload.line1());
                    ByteBufCodecs.STRING_UTF8.encode(buffer, payload.line2());
                    ByteBufCodecs.STRING_UTF8.encode(buffer, payload.cartoucheLeftType());
                    ByteBufCodecs.STRING_UTF8.encode(buffer, payload.cartoucheLeftText());
                    ByteBufCodecs.STRING_UTF8.encode(buffer, payload.cartoucheRightType());
                    ByteBufCodecs.STRING_UTF8.encode(buffer, payload.cartoucheRightText());
                }
            };

    public UpdateDA31CPayload {
        line1 = line1 == null ? "" : line1;
        line2 = line2 == null ? "" : line2;
        cartoucheLeftType = cartoucheLeftType == null ? "none" : cartoucheLeftType;
        cartoucheLeftText = cartoucheLeftText == null ? "" : cartoucheLeftText;
        cartoucheRightType = cartoucheRightType == null ? "none" : cartoucheRightType;
        cartoucheRightText = cartoucheRightText == null ? "" : cartoucheRightText;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
