package net.xelpy.moreroad.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.xelpy.moreroad.MoreRoad;
import net.xelpy.moreroad.block.custom.MotorwaySignColor;
import net.xelpy.moreroad.block.custom.CartoucheType;
import net.xelpy.moreroad.block.custom.MotorwaySignGraphic;
import net.xelpy.moreroad.block.custom.MotorwaySignLineData;
import net.xelpy.moreroad.block.custom.MotorwaySignPanelData;
import net.xelpy.moreroad.block.custom.MotorwaySignPreset;
import net.xelpy.moreroad.block.custom.RoadTextFont;
import net.xelpy.moreroad.block.entity.MotorwaySignBlockEntity;

public record UpdateMotorwaySignPayload(
        BlockPos pos,
        String presetName,
        MotorwaySignLineData line0,
        MotorwaySignLineData line1,
        MotorwaySignLineData line2,
        MotorwaySignLineData line3,
        MotorwaySignLineData line4,
        MotorwaySignLineData line5,
        boolean customMode,
        MotorwaySignPanelData panel0,
        MotorwaySignPanelData panel1,
        MotorwaySignPanelData panel2,
        MotorwaySignPanelData panel3
) implements CustomPacketPayload {

    public static final Type<UpdateMotorwaySignPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(MoreRoad.MODID, "update_motorway_sign"));

    public static final StreamCodec<ByteBuf, UpdateMotorwaySignPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public UpdateMotorwaySignPayload decode(ByteBuf buffer) {
            return new UpdateMotorwaySignPayload(
                    BlockPos.STREAM_CODEC.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    decodeLine(buffer), decodeLine(buffer), decodeLine(buffer),
                    decodeLine(buffer), decodeLine(buffer), decodeLine(buffer),
                    ByteBufCodecs.BOOL.decode(buffer),
                    decodePanel(buffer), decodePanel(buffer), decodePanel(buffer), decodePanel(buffer)
            );
        }

        @Override
        public void encode(ByteBuf buffer, UpdateMotorwaySignPayload payload) {
            BlockPos.STREAM_CODEC.encode(buffer, payload.pos());
            ByteBufCodecs.STRING_UTF8.encode(buffer, payload.presetName());
            for (int i = 0; i < MotorwaySignBlockEntity.MAX_SLOTS; i++) {
                encodeLine(buffer, payload.line(i));
            }
            ByteBufCodecs.BOOL.encode(buffer, payload.customMode());
            for (int i = 0; i < MotorwaySignBlockEntity.MAX_CUSTOM_PANELS; i++) {
                encodePanel(buffer, payload.panel(i));
            }
        }
    };

    public UpdateMotorwaySignPayload {
        presetName = MotorwaySignPreset.fromSerializedName(presetName).getSerializedName();
        line0 = safe(line0);
        line1 = safe(line1);
        line2 = safe(line2);
        line3 = safe(line3);
        line4 = safe(line4);
        line5 = safe(line5);
        panel0 = safePanel(panel0);
        panel1 = safePanel(panel1);
        panel2 = safePanel(panel2);
        panel3 = safePanel(panel3);
    }

    public MotorwaySignPanelData panel(int index) {
        return switch (index) {
            case 0 -> panel0;
            case 1 -> panel1;
            case 2 -> panel2;
            case 3 -> panel3;
            default -> MotorwaySignPanelData.disabled();
        };
    }

    public MotorwaySignLineData line(int index) {
        return switch (index) {
            case 0 -> line0;
            case 1 -> line1;
            case 2 -> line2;
            case 3 -> line3;
            case 4 -> line4;
            case 5 -> line5;
            default -> MotorwaySignLineData.empty();
        };
    }

    private static MotorwaySignLineData safe(MotorwaySignLineData line) {
        return line == null ? MotorwaySignLineData.empty() : line;
    }

    private static MotorwaySignPanelData safePanel(MotorwaySignPanelData panel) {
        return panel == null ? MotorwaySignPanelData.disabled() : panel;
    }

    private static void encodeLine(ByteBuf buffer, MotorwaySignLineData line) {
        ByteBufCodecs.STRING_UTF8.encode(buffer, line.text());
        ByteBufCodecs.STRING_UTF8.encode(buffer, line.font().getSerializedName());
        ByteBufCodecs.STRING_UTF8.encode(buffer, line.color().getSerializedName());
    }

    private static MotorwaySignLineData decodeLine(ByteBuf buffer) {
        return new MotorwaySignLineData(
                ByteBufCodecs.STRING_UTF8.decode(buffer),
                RoadTextFont.fromSerializedName(ByteBufCodecs.STRING_UTF8.decode(buffer)),
                MotorwaySignColor.fromSerializedName(ByteBufCodecs.STRING_UTF8.decode(buffer))
        );
    }

    private static void encodePanel(ByteBuf buffer, MotorwaySignPanelData panel) {
        MotorwaySignPanelData safe = safePanel(panel);
        ByteBufCodecs.BOOL.encode(buffer, safe.enabled());
        ByteBufCodecs.VAR_INT.encode(buffer, safe.lineCount());
        for (int index = 0; index < 4; index++) {
            ByteBufCodecs.STRING_UTF8.encode(buffer, safe.line(index));
        }
        for (int index = 0; index < 4; index++) {
            ByteBufCodecs.STRING_UTF8.encode(buffer, safe.distance(index));
        }
        for (int index = 0; index < 4; index++) {
            ByteBufCodecs.STRING_UTF8.encode(buffer, safe.font(index).getSerializedName());
        }
        ByteBufCodecs.STRING_UTF8.encode(buffer, safe.background().getSerializedName());
        ByteBufCodecs.STRING_UTF8.encode(buffer, safe.cartoucheType().getSerializedName());
        ByteBufCodecs.STRING_UTF8.encode(buffer, safe.cartoucheText());
        ByteBufCodecs.STRING_UTF8.encode(buffer, safe.graphic().name());
    }

    private static MotorwaySignPanelData decodePanel(ByteBuf buffer) {
        return new MotorwaySignPanelData(
                ByteBufCodecs.BOOL.decode(buffer),
                ByteBufCodecs.VAR_INT.decode(buffer),
                ByteBufCodecs.STRING_UTF8.decode(buffer),
                ByteBufCodecs.STRING_UTF8.decode(buffer),
                ByteBufCodecs.STRING_UTF8.decode(buffer),
                ByteBufCodecs.STRING_UTF8.decode(buffer),
                ByteBufCodecs.STRING_UTF8.decode(buffer),
                ByteBufCodecs.STRING_UTF8.decode(buffer),
                ByteBufCodecs.STRING_UTF8.decode(buffer),
                ByteBufCodecs.STRING_UTF8.decode(buffer),
                RoadTextFont.fromSerializedName(ByteBufCodecs.STRING_UTF8.decode(buffer)),
                RoadTextFont.fromSerializedName(ByteBufCodecs.STRING_UTF8.decode(buffer)),
                RoadTextFont.fromSerializedName(ByteBufCodecs.STRING_UTF8.decode(buffer)),
                RoadTextFont.fromSerializedName(ByteBufCodecs.STRING_UTF8.decode(buffer)),
                MotorwaySignColor.fromSerializedName(ByteBufCodecs.STRING_UTF8.decode(buffer)),
                CartoucheType.fromSerializedName(ByteBufCodecs.STRING_UTF8.decode(buffer)),
                ByteBufCodecs.STRING_UTF8.decode(buffer),
                MotorwaySignPanelData.parseGraphic(ByteBufCodecs.STRING_UTF8.decode(buffer))
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
