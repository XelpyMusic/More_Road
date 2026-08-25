package net.xelpy.moreroad.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.xelpy.moreroad.MoreRoad;
import net.xelpy.moreroad.block.custom.RoadTextFont;

public record UpdateDA31CPayload(
        BlockPos pos,
        String line1,
        String line2,
        String line3,
        String line4,
        RoadTextFont line1Font,
        RoadTextFont line2Font,
        RoadTextFont line3Font,
        RoadTextFont line4Font,
        String cartoucheTopType,
        String cartoucheTopText,
        String cartoucheLeftType,
        String cartoucheLeftText,
        String cartoucheRightType,
        String cartoucheRightText,
        String arrowLeftType,
        String arrowRightType
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
                            RoadTextFont.fromSerializedName(ByteBufCodecs.STRING_UTF8.decode(buffer)),
                            RoadTextFont.fromSerializedName(ByteBufCodecs.STRING_UTF8.decode(buffer)),
                            RoadTextFont.fromSerializedName(ByteBufCodecs.STRING_UTF8.decode(buffer)),
                            RoadTextFont.fromSerializedName(ByteBufCodecs.STRING_UTF8.decode(buffer)),
                            ByteBufCodecs.STRING_UTF8.decode(buffer),
                            ByteBufCodecs.STRING_UTF8.decode(buffer),
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
                    ByteBufCodecs.STRING_UTF8.encode(buffer, payload.line3());
                    ByteBufCodecs.STRING_UTF8.encode(buffer, payload.line4());
                    ByteBufCodecs.STRING_UTF8.encode(buffer, safeFont(payload.line1Font()).getSerializedName());
                    ByteBufCodecs.STRING_UTF8.encode(buffer, safeFont(payload.line2Font()).getSerializedName());
                    ByteBufCodecs.STRING_UTF8.encode(buffer, safeFont(payload.line3Font()).getSerializedName());
                    ByteBufCodecs.STRING_UTF8.encode(buffer, safeFont(payload.line4Font()).getSerializedName());
                    ByteBufCodecs.STRING_UTF8.encode(buffer, payload.cartoucheTopType());
                    ByteBufCodecs.STRING_UTF8.encode(buffer, payload.cartoucheTopText());
                    ByteBufCodecs.STRING_UTF8.encode(buffer, payload.cartoucheLeftType());
                    ByteBufCodecs.STRING_UTF8.encode(buffer, payload.cartoucheLeftText());
                    ByteBufCodecs.STRING_UTF8.encode(buffer, payload.cartoucheRightType());
                    ByteBufCodecs.STRING_UTF8.encode(buffer, payload.cartoucheRightText());
                    ByteBufCodecs.STRING_UTF8.encode(buffer, payload.arrowLeftType());
                    ByteBufCodecs.STRING_UTF8.encode(buffer, payload.arrowRightType());
                }
            };

    public UpdateDA31CPayload {
        line1 = safe(line1);
        line2 = safe(line2);
        line3 = safe(line3);
        line4 = safe(line4);
        line1Font = safeFont(line1Font);
        line2Font = safeFont(line2Font);
        line3Font = safeFont(line3Font);
        line4Font = safeFont(line4Font);
        cartoucheTopType = safeType(cartoucheTopType);
        cartoucheTopText = safe(cartoucheTopText);
        cartoucheLeftType = safeType(cartoucheLeftType);
        cartoucheLeftText = safe(cartoucheLeftText);
        cartoucheRightType = safeType(cartoucheRightType);
        cartoucheRightText = safe(cartoucheRightText);
        arrowLeftType = safeArrow(arrowLeftType);
        arrowRightType = safeArrow(arrowRightType);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static RoadTextFont safeFont(RoadTextFont font) {
        return font == null ? RoadTextFont.L1 : font;
    }

    private static String safeType(String value) {
        return value == null ? "none" : value;
    }

    private static String safeArrow(String value) {
        return value == null ? "down" : value;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
