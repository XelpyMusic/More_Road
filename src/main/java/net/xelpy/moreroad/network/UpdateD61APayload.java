package net.xelpy.moreroad.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.xelpy.moreroad.MoreRoad;
import net.xelpy.moreroad.block.custom.CartoucheType;
import net.xelpy.moreroad.block.custom.D21AType;
import net.xelpy.moreroad.block.custom.D61AArrowDirection;
import net.xelpy.moreroad.block.custom.D61AArrowPosition;
import net.xelpy.moreroad.block.custom.D61APanelData;

public record UpdateD61APayload(
        BlockPos pos,
        D61APanelData panel0,
        D61APanelData panel1,
        D61APanelData panel2,
        D61APanelData panel3,
        CartoucheType cartoucheType,
        String cartoucheText
) implements CustomPacketPayload {

    public static final Type<UpdateD61APayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(MoreRoad.MODID, "update_d61a"));

    public static final StreamCodec<ByteBuf, UpdateD61APayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public UpdateD61APayload decode(ByteBuf buffer) {
                    return new UpdateD61APayload(
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
                public void encode(ByteBuf buffer, UpdateD61APayload payload) {
                    BlockPos.STREAM_CODEC.encode(buffer, payload.pos());
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

    public UpdateD61APayload {
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

    public UpdateD61APayload(
            BlockPos pos,
            D61APanelData panel0,
            D61APanelData panel1,
            D61APanelData panel2,
            D61APanelData panel3
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

    public UpdateD61APayload(
            BlockPos pos,
            D61APanelData panel0,
            D61APanelData panel1,
            D61APanelData panel2,
            D61APanelData panel3,
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

    public D61APanelData panel(int index) {
        return switch (index) {
            case 0 -> this.panel0;
            case 1 -> this.panel1;
            case 2 -> this.panel2;
            case 3 -> this.panel3;
            default -> D61APanelData.disabled();
        };
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static D61APanelData normalize(D61APanelData panel, boolean first) {
        if (panel != null) {
            return sanitizePanel(panel);
        }
        return first ? D61APanelData.firstPanelDefault() : D61APanelData.disabled();
    }

    private static void encodePanel(ByteBuf buffer, D61APanelData panel) {
        D61APanelData sanitized = sanitizePanel(panel);

        ByteBufCodecs.BOOL.encode(buffer, sanitized.enabled());
        ByteBufCodecs.STRING_UTF8.encode(buffer, sanitized.line1());
        ByteBufCodecs.STRING_UTF8.encode(buffer, sanitized.line2());
        ByteBufCodecs.STRING_UTF8.encode(buffer, sanitized.distance1());
        ByteBufCodecs.STRING_UTF8.encode(buffer, sanitized.distance2());
        ByteBufCodecs.STRING_UTF8.encode(buffer, sanitized.type().getSerializedName());
        ByteBufCodecs.BOOL.encode(buffer, sanitized.doubleLine());
        ByteBufCodecs.BOOL.encode(buffer, sanitized.arrowEnabled());
        ByteBufCodecs.STRING_UTF8.encode(buffer, sanitized.arrowPosition().getSerializedName());
        ByteBufCodecs.STRING_UTF8.encode(buffer, sanitized.arrowDirection().getSerializedName());
        ByteBufCodecs.BOOL.encode(buffer, sanitized.autorouteLogo());
    }

    private static D61APanelData decodePanel(ByteBuf buffer) {
        boolean enabled = ByteBufCodecs.BOOL.decode(buffer);
        String line1 = ByteBufCodecs.STRING_UTF8.decode(buffer);
        String line2 = ByteBufCodecs.STRING_UTF8.decode(buffer);
        String distance1 = ByteBufCodecs.STRING_UTF8.decode(buffer);
        String distance2 = ByteBufCodecs.STRING_UTF8.decode(buffer);
        D21AType type = parseType(ByteBufCodecs.STRING_UTF8.decode(buffer));
        boolean doubleLine = ByteBufCodecs.BOOL.decode(buffer);
        boolean arrowEnabled = ByteBufCodecs.BOOL.decode(buffer);
        D61AArrowPosition arrowPosition =
                D61AArrowPosition.fromSerializedName(ByteBufCodecs.STRING_UTF8.decode(buffer));
        D61AArrowDirection arrowDirection =
                D61AArrowDirection.fromSerializedName(ByteBufCodecs.STRING_UTF8.decode(buffer));
        boolean autorouteLogo = ByteBufCodecs.BOOL.decode(buffer);

        return sanitizePanel(
                new D61APanelData(
                        enabled,
                        line1,
                        line2,
                        distance1,
                        distance2,
                        type,
                        doubleLine,
                        arrowEnabled,
                        arrowPosition,
                        arrowDirection,
                        autorouteLogo
                )
        );
    }

    private static D61APanelData sanitizePanel(D61APanelData panel) {
        D21AType type = switch (panel.type()) {
            case GREEN -> D21AType.GREEN;
            case BLUE -> D21AType.BLUE;
            default -> D21AType.WHITE;
        };

        return new D61APanelData(
                panel.enabled(),
                panel.line1(),
                panel.line2(),
                panel.distance1(),
                panel.distance2(),
                type,
                panel.doubleLine(),
                panel.arrowEnabled(),
                panel.arrowPosition(),
                panel.arrowDirection(),
                type != D21AType.WHITE && panel.autorouteLogo()
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
