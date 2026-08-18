package net.xelpy.moreroad.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.xelpy.moreroad.MoreRoad;
import net.xelpy.moreroad.block.custom.D21APanelData;
import net.xelpy.moreroad.block.custom.D21AType;

public record UpdateD61APayload(
        BlockPos pos,
        D21APanelData panel0,
        D21APanelData panel1,
        D21APanelData panel2,
        D21APanelData panel3
) implements CustomPacketPayload {

    public static final Type<UpdateD61APayload> TYPE =
            new Type<>(
                    Identifier.fromNamespaceAndPath(
                            MoreRoad.MODID,
                            "update_d61a"
                    )
            );

    public static final StreamCodec<ByteBuf, UpdateD61APayload>
            STREAM_CODEC = new StreamCodec<>() {

        @Override
        public UpdateD61APayload decode(ByteBuf buffer) {
            return new UpdateD61APayload(
                    BlockPos.STREAM_CODEC.decode(buffer),
                    decodePanel(buffer),
                    decodePanel(buffer),
                    decodePanel(buffer),
                    decodePanel(buffer)
            );
        }

        @Override
        public void encode(
                ByteBuf buffer,
                UpdateD61APayload payload
        ) {
            BlockPos.STREAM_CODEC.encode(buffer, payload.pos());
            encodePanel(buffer, payload.panel0());
            encodePanel(buffer, payload.panel1());
            encodePanel(buffer, payload.panel2());
            encodePanel(buffer, payload.panel3());
        }
    };

    public UpdateD61APayload {
        panel0 = normalize(panel0, true);
        panel1 = normalize(panel1, false);
        panel2 = normalize(panel2, false);
        panel3 = normalize(panel3, false);
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
            return sanitizePanel(panel);
        }

        return first
                ? D21APanelData.firstPanelDefault()
                : D21APanelData.disabled();
    }

    private static void encodePanel(
            ByteBuf buffer,
            D21APanelData panel
    ) {
        D21APanelData sanitized = sanitizePanel(panel);

        ByteBufCodecs.BOOL.encode(buffer, sanitized.enabled());
        ByteBufCodecs.STRING_UTF8.encode(buffer, sanitized.line1());
        ByteBufCodecs.STRING_UTF8.encode(buffer, sanitized.line2());
        ByteBufCodecs.STRING_UTF8.encode(buffer, sanitized.distance1());
        ByteBufCodecs.STRING_UTF8.encode(buffer, sanitized.distance2());
        ByteBufCodecs.STRING_UTF8.encode(buffer, sanitized.type().getSerializedName());
        ByteBufCodecs.BOOL.encode(buffer, false);
        ByteBufCodecs.BOOL.encode(buffer, false);
        ByteBufCodecs.BOOL.encode(buffer, sanitized.doubleLine());
    }

    private static D21APanelData decodePanel(ByteBuf buffer) {
        boolean enabled = ByteBufCodecs.BOOL.decode(buffer);
        String line1 = ByteBufCodecs.STRING_UTF8.decode(buffer);
        String line2 = ByteBufCodecs.STRING_UTF8.decode(buffer);
        String distance1 = ByteBufCodecs.STRING_UTF8.decode(buffer);
        String distance2 = ByteBufCodecs.STRING_UTF8.decode(buffer);
        D21AType type = parseType(ByteBufCodecs.STRING_UTF8.decode(buffer));
        ByteBufCodecs.BOOL.decode(buffer);
        ByteBufCodecs.BOOL.decode(buffer);
        boolean doubleLine = ByteBufCodecs.BOOL.decode(buffer);

        return sanitizePanel(
                new D21APanelData(
                        enabled,
                        line1,
                        line2,
                        distance1,
                        distance2,
                        type,
                        false,
                        false,
                        doubleLine
                )
        );
    }

    private static D21APanelData sanitizePanel(D21APanelData panel) {
        return new D21APanelData(
                panel.enabled(),
                panel.line1(),
                panel.line2(),
                panel.distance1(),
                panel.distance2(),
                panel.type() == D21AType.GREEN ? D21AType.GREEN : D21AType.WHITE,
                false,
                false,
                panel.doubleLine()
        );
    }

    private static D21AType parseType(String value) {
        return "green".equals(value)
                ? D21AType.GREEN
                : D21AType.WHITE;
    }
}
