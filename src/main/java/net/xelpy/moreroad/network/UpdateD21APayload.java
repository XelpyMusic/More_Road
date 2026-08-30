package net.xelpy.moreroad.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.xelpy.moreroad.MoreRoad;
import net.xelpy.moreroad.block.custom.CartoucheType;
import net.xelpy.moreroad.block.custom.D21APanelData;
import net.xelpy.moreroad.block.custom.D21AType;
import net.xelpy.moreroad.block.custom.RoadTextFont;

public record UpdateD21APayload(
        BlockPos pos,
        D21APanelData panel0,
        D21APanelData panel1,
        D21APanelData panel2,
        D21APanelData panel3,
        CartoucheType cartoucheType,
        String cartoucheText
) implements CustomPacketPayload {

    public static final Type<UpdateD21APayload> TYPE =
            new Type<>(
                    Identifier.fromNamespaceAndPath(
                            MoreRoad.MODID,
                            "update_d21a"
                    )
            );

    public static final StreamCodec<ByteBuf, UpdateD21APayload>
            STREAM_CODEC =
            new StreamCodec<>() {

                @Override
                public UpdateD21APayload decode(ByteBuf buffer) {
                    return new UpdateD21APayload(
                            BlockPos.STREAM_CODEC.decode(buffer),
                            decodePanel(buffer),
                            decodePanel(buffer),
                            decodePanel(buffer),
                            decodePanel(buffer),
                            CartoucheType.fromSerializedName(
                                    ByteBufCodecs.STRING_UTF8.decode(buffer)
                            ),
                            ByteBufCodecs.STRING_UTF8.decode(buffer)
                    );
                }

                @Override
                public void encode(
                        ByteBuf buffer,
                        UpdateD21APayload payload
                ) {
                    BlockPos.STREAM_CODEC.encode(
                            buffer,
                            payload.pos()
                    );

                    encodePanel(buffer, payload.panel0());
                    encodePanel(buffer, payload.panel1());
                    encodePanel(buffer, payload.panel2());
                    encodePanel(buffer, payload.panel3());
                    ByteBufCodecs.STRING_UTF8.encode(
                            buffer,
                            payload.cartoucheType().getSerializedName()
                    );
                    ByteBufCodecs.STRING_UTF8.encode(
                            buffer,
                            payload.cartoucheText()
                    );
                }
            };

    public UpdateD21APayload {
        panel0 = normalize(panel0, true);
        panel1 = normalize(panel1, false);
        panel2 = normalize(panel2, false);
        panel3 = normalize(panel3, false);
        cartoucheType = cartoucheType == null
                ? CartoucheType.NONE
                : cartoucheType;
        cartoucheText = cartoucheText == null
                ? ""
                : cartoucheText;
    }

    public UpdateD21APayload(
            BlockPos pos,
            D21APanelData panel0,
            D21APanelData panel1,
            D21APanelData panel2,
            D21APanelData panel3
    ) {
        this(
                pos,
                panel0,
                panel1,
                panel2,
                panel3,
                CartoucheType.NONE,
                ""
        );
    }

    public UpdateD21APayload(
            BlockPos pos,
            D21APanelData panel0,
            D21APanelData panel1,
            D21APanelData panel2,
            D21APanelData panel3,
            CartoucheType cartoucheType
    ) {
        this(
                pos,
                panel0,
                panel1,
                panel2,
                panel3,
                cartoucheType,
                ""
        );
    }

    public D21APanelData panel(int index) {
        return switch (index) {
            case 0 -> this.panel0;
            case 1 -> this.panel1;
            case 2 -> this.panel2;
            case 3 -> this.panel3;
            default -> D21APanelData.disabled();
        };
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static D21APanelData normalize(
            D21APanelData panel,
            boolean first
    ) {
        if (panel != null) {
            return panel;
        }

        return first
                ? D21APanelData.firstPanelDefault()
                : D21APanelData.disabled();
    }

    private static void encodePanel(
            ByteBuf buffer,
            D21APanelData panel
    ) {
        ByteBufCodecs.BOOL.encode(buffer, panel.enabled());
        ByteBufCodecs.STRING_UTF8.encode(buffer, panel.line1());
        ByteBufCodecs.STRING_UTF8.encode(buffer, panel.line2());
        ByteBufCodecs.STRING_UTF8.encode(buffer, panel.distance1());
        ByteBufCodecs.STRING_UTF8.encode(buffer, panel.distance2());
        ByteBufCodecs.STRING_UTF8.encode(
                buffer,
                panel.type().getSerializedName()
        );
        ByteBufCodecs.BOOL.encode(buffer, panel.arrowRight());
        ByteBufCodecs.BOOL.encode(buffer, panel.autorouteLogo());
        ByteBufCodecs.BOOL.encode(buffer, panel.doubleLine());
        ByteBufCodecs.STRING_UTF8.encode(
                buffer,
                panel.line1Font().getSerializedName()
        );
        ByteBufCodecs.STRING_UTF8.encode(
                buffer,
                panel.line2Font().getSerializedName()
        );
        ByteBufCodecs.BOOL.encode(buffer, panel.line1Spacing());
        ByteBufCodecs.BOOL.encode(buffer, panel.line2Spacing());
    }

    private static D21APanelData decodePanel(ByteBuf buffer) {
        boolean enabled = ByteBufCodecs.BOOL.decode(buffer);
        String line1 = ByteBufCodecs.STRING_UTF8.decode(buffer);
        String line2 = ByteBufCodecs.STRING_UTF8.decode(buffer);
        String distance1 = ByteBufCodecs.STRING_UTF8.decode(buffer);
        String distance2 = ByteBufCodecs.STRING_UTF8.decode(buffer);

        D21AType type =
                parseType(
                        ByteBufCodecs.STRING_UTF8.decode(buffer)
                );

        boolean arrowRight = ByteBufCodecs.BOOL.decode(buffer);
        boolean autorouteLogo = ByteBufCodecs.BOOL.decode(buffer);
        boolean doubleLine = ByteBufCodecs.BOOL.decode(buffer);

        RoadTextFont line1Font =
                RoadTextFont.fromSerializedName(
                        ByteBufCodecs.STRING_UTF8.decode(buffer)
                );

        RoadTextFont line2Font =
                RoadTextFont.fromSerializedName(
                        ByteBufCodecs.STRING_UTF8.decode(buffer)
                );

        boolean line1Spacing = ByteBufCodecs.BOOL.decode(buffer);
        boolean line2Spacing = ByteBufCodecs.BOOL.decode(buffer);

        return new D21APanelData(
                enabled,
                line1,
                line2,
                distance1,
                distance2,
                type,
                arrowRight,
                autorouteLogo,
                doubleLine,
                line1Font,
                line2Font,
                line1Spacing,
                line2Spacing
        );
    }

    private static D21AType parseType(String value) {
        if ("green".equals(value)) {
            return D21AType.GREEN;
        }
        if ("blue".equals(value)) {
            return D21AType.BLUE;
        }
        return D21AType.WHITE;
    }
}
