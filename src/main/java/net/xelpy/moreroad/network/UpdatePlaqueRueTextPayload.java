package net.xelpy.moreroad.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.xelpy.moreroad.MoreRoad;
import net.xelpy.moreroad.block.custom.RoadTextFont;

public record UpdatePlaqueRueTextPayload(
        BlockPos pos,
        String line1,
        String line2,
        RoadTextFont line1Font,
        RoadTextFont line2Font
) implements CustomPacketPayload {

    public static final Type<UpdatePlaqueRueTextPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(
                    MoreRoad.MODID,
                    "update_plaque_rue_text"
            ));

    public static final StreamCodec<ByteBuf, UpdatePlaqueRueTextPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public UpdatePlaqueRueTextPayload decode(ByteBuf buffer) {
                    return new UpdatePlaqueRueTextPayload(
                            BlockPos.STREAM_CODEC.decode(buffer),
                            ByteBufCodecs.STRING_UTF8.decode(buffer),
                            ByteBufCodecs.STRING_UTF8.decode(buffer),
                            RoadTextFont.fromSerializedName(ByteBufCodecs.STRING_UTF8.decode(buffer)),
                            RoadTextFont.fromSerializedName(ByteBufCodecs.STRING_UTF8.decode(buffer))
                    );
                }

                @Override
                public void encode(ByteBuf buffer, UpdatePlaqueRueTextPayload payload) {
                    BlockPos.STREAM_CODEC.encode(buffer, payload.pos());
                    ByteBufCodecs.STRING_UTF8.encode(buffer, normalizeText(payload.line1()));
                    ByteBufCodecs.STRING_UTF8.encode(buffer, normalizeText(payload.line2()));
                    ByteBufCodecs.STRING_UTF8.encode(
                            buffer,
                            normalizeFont(payload.line1Font()).getSerializedName()
                    );
                    ByteBufCodecs.STRING_UTF8.encode(
                            buffer,
                            normalizeFont(payload.line2Font()).getSerializedName()
                    );
                }
            };

    public UpdatePlaqueRueTextPayload {
        line1 = normalizeText(line1);
        line2 = normalizeText(line2);
        line1Font = normalizeFont(line1Font);
        line2Font = normalizeFont(line2Font);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static RoadTextFont normalizeFont(RoadTextFont font) {
        return font == null ? RoadTextFont.NORMAL : font;
    }

    private static String normalizeText(String text) {
        return text == null ? "" : text;
    }
}
