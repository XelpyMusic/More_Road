package net.xelpy.moreroad.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.xelpy.moreroad.MoreRoad;
import net.xelpy.moreroad.block.custom.CartoucheType;
import net.xelpy.moreroad.block.custom.D61AArrowPosition;
import net.xelpy.moreroad.block.custom.GenericArrowShape;
import net.xelpy.moreroad.block.custom.GenericDestinationRow;
import net.xelpy.moreroad.block.custom.GenericDirectionalSignData;
import net.xelpy.moreroad.block.custom.GenericRouteCartoucheData;
import net.xelpy.moreroad.block.custom.GenericSignAlignment;
import net.xelpy.moreroad.block.custom.GenericSignHeader;
import net.xelpy.moreroad.block.custom.GenericSignSymbol;
import net.xelpy.moreroad.block.custom.MotorwaySignColor;
import net.xelpy.moreroad.block.custom.RoadTextFont;

public record UpdateGenericDirectionalSignPayload(
        BlockPos pos,
        GenericDirectionalSignData data
) implements CustomPacketPayload {

    public static final Type<UpdateGenericDirectionalSignPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(MoreRoad.MODID, "update_generic_directional_sign"));

    public static final StreamCodec<ByteBuf, UpdateGenericDirectionalSignPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public UpdateGenericDirectionalSignPayload decode(ByteBuf buffer) {
                    BlockPos pos = BlockPos.STREAM_CODEC.decode(buffer);
                    MotorwaySignColor background = MotorwaySignColor.fromSerializedName(
                            ByteBufCodecs.STRING_UTF8.decode(buffer)
                    );
                    GenericSignHeader header = decodeHeader(buffer);
                    GenericDestinationRow[] rows = new GenericDestinationRow[GenericDirectionalSignData.MAX_ROWS];
                    for (int i = 0; i < rows.length; i++) {
                        rows[i] = decodeRow(buffer);
                    }
                    GenericRouteCartoucheData[] cartouches =
                            new GenericRouteCartoucheData[GenericDirectionalSignData.MAX_CARTOUCHES];
                    for (int i = 0; i < cartouches.length; i++) {
                        cartouches[i] = decodeCartouche(buffer);
                    }
                    return new UpdateGenericDirectionalSignPayload(
                            pos, new GenericDirectionalSignData(background, header, rows, cartouches)
                    );
                }

                @Override
                public void encode(ByteBuf buffer, UpdateGenericDirectionalSignPayload payload) {
                    BlockPos.STREAM_CODEC.encode(buffer, payload.pos());
                    ByteBufCodecs.STRING_UTF8.encode(buffer, payload.data().background().getSerializedName());
                    encodeHeader(buffer, payload.data().header());
                    for (GenericDestinationRow row : payload.data().rows()) {
                        encodeRow(buffer, row);
                    }
                    for (GenericRouteCartoucheData cartouche : payload.data().cartouches()) {
                        encodeCartouche(buffer, cartouche);
                    }
                }
            };

    public UpdateGenericDirectionalSignPayload {
        data = data == null ? GenericDirectionalSignData.blank() : data;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encodeHeader(ByteBuf buffer, GenericSignHeader header) {
        ByteBufCodecs.BOOL.encode(buffer, header.enabled());
        ByteBufCodecs.STRING_UTF8.encode(buffer, header.text());
        ByteBufCodecs.BOOL.encode(buffer, header.sameAsPanel());
        ByteBufCodecs.STRING_UTF8.encode(buffer, header.color().getSerializedName());
        ByteBufCodecs.STRING_UTF8.encode(buffer, header.alignment().getSerializedName());
        ByteBufCodecs.STRING_UTF8.encode(buffer, header.font().getSerializedName());
    }

    private static GenericSignHeader decodeHeader(ByteBuf buffer) {
        boolean enabled = ByteBufCodecs.BOOL.decode(buffer);
        String text = ByteBufCodecs.STRING_UTF8.decode(buffer);
        boolean sameAsPanel = ByteBufCodecs.BOOL.decode(buffer);
        MotorwaySignColor color = MotorwaySignColor.fromSerializedName(ByteBufCodecs.STRING_UTF8.decode(buffer));
        GenericSignAlignment alignment = GenericSignAlignment.fromSerializedName(ByteBufCodecs.STRING_UTF8.decode(buffer));
        RoadTextFont font = RoadTextFont.fromSerializedName(ByteBufCodecs.STRING_UTF8.decode(buffer));
        return new GenericSignHeader(enabled, text, sameAsPanel, color, alignment, font);
    }

    private static void encodeRow(ByteBuf buffer, GenericDestinationRow row) {
        ByteBufCodecs.BOOL.encode(buffer, row.enabled());
        ByteBufCodecs.STRING_UTF8.encode(buffer, row.text());
        ByteBufCodecs.STRING_UTF8.encode(buffer, row.alignment().getSerializedName());
        ByteBufCodecs.STRING_UTF8.encode(buffer, row.font().getSerializedName());
        ByteBufCodecs.BOOL.encode(buffer, row.arrowEnabled());
        ByteBufCodecs.STRING_UTF8.encode(buffer, row.arrowShape().getSerializedName());
        ByteBufCodecs.BOOL.encode(buffer, row.arrowMirrored());
        ByteBufCodecs.STRING_UTF8.encode(buffer, row.arrowPosition().getSerializedName());
        ByteBufCodecs.BOOL.encode(buffer, row.symbolEnabled());
        ByteBufCodecs.STRING_UTF8.encode(buffer, row.symbol().getSerializedName());
        ByteBufCodecs.STRING_UTF8.encode(buffer, row.symbolPosition().getSerializedName());
    }

    private static GenericDestinationRow decodeRow(ByteBuf buffer) {
        boolean enabled = ByteBufCodecs.BOOL.decode(buffer);
        String text = ByteBufCodecs.STRING_UTF8.decode(buffer);
        GenericSignAlignment alignment = GenericSignAlignment.fromSerializedName(ByteBufCodecs.STRING_UTF8.decode(buffer));
        RoadTextFont font = RoadTextFont.fromSerializedName(ByteBufCodecs.STRING_UTF8.decode(buffer));
        boolean arrowEnabled = ByteBufCodecs.BOOL.decode(buffer);
        GenericArrowShape arrowShape = GenericArrowShape.fromSerializedName(ByteBufCodecs.STRING_UTF8.decode(buffer));
        boolean arrowMirrored = ByteBufCodecs.BOOL.decode(buffer);
        D61AArrowPosition arrowPosition = D61AArrowPosition.fromSerializedName(ByteBufCodecs.STRING_UTF8.decode(buffer));
        boolean symbolEnabled = ByteBufCodecs.BOOL.decode(buffer);
        GenericSignSymbol symbol = GenericSignSymbol.fromSerializedName(ByteBufCodecs.STRING_UTF8.decode(buffer));
        D61AArrowPosition symbolPosition = D61AArrowPosition.fromSerializedName(ByteBufCodecs.STRING_UTF8.decode(buffer));
        return new GenericDestinationRow(
                enabled, text, alignment, font,
                arrowEnabled, arrowShape, arrowMirrored, arrowPosition,
                symbolEnabled, symbol, symbolPosition
        );
    }

    private static void encodeCartouche(ByteBuf buffer, GenericRouteCartoucheData cartouche) {
        ByteBufCodecs.STRING_UTF8.encode(buffer, cartouche.type().getSerializedName());
        ByteBufCodecs.STRING_UTF8.encode(buffer, cartouche.text());
    }

    private static GenericRouteCartoucheData decodeCartouche(ByteBuf buffer) {
        CartoucheType type = CartoucheType.fromSerializedName(ByteBufCodecs.STRING_UTF8.decode(buffer));
        String text = ByteBufCodecs.STRING_UTF8.decode(buffer);
        return new GenericRouteCartoucheData(type, text);
    }
}
