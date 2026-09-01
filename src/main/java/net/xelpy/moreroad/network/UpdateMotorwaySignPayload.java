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
import net.xelpy.moreroad.block.custom.MotorwaySignServiceIcon;
import net.xelpy.moreroad.block.custom.RoadTextFont;
import net.xelpy.moreroad.block.entity.MotorwaySignBlockEntity;

/**
 * Signalé : ce paquet portait un champ nommé par ligne (line0, line1...),
 * en dur pour un nombre de champs fixe. Chaque ajout d'un nouveau champ au
 * D31d/D31e a nécessité de garder à la main le decode() (nombre d'appels à
 * decodeLine), le constructeur compact, l'accesseur line(int) ET tous les
 * appelants de ce constructeur en phase avec MotorwaySignBlockEntity.
 * MAX_SLOTS — un oubli côté decode() a déjà provoqué un plantage réseau
 * (désynchronisation encode/decode). Tableau de taille MAX_SLOTS à la
 * place : plus aucun de ces endroits à maintenir séparément la prochaine
 * fois qu'un modèle gagne un champ.
 */
public record UpdateMotorwaySignPayload(
        BlockPos pos,
        String presetName,
        MotorwaySignLineData[] lines,
        boolean customMode,
        MotorwaySignPanelData[] panels,
        MotorwaySignServiceIcon[] services
) implements CustomPacketPayload {

    public static final Type<UpdateMotorwaySignPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(MoreRoad.MODID, "update_motorway_sign"));

    public static final StreamCodec<ByteBuf, UpdateMotorwaySignPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public UpdateMotorwaySignPayload decode(ByteBuf buffer) {
            BlockPos pos = BlockPos.STREAM_CODEC.decode(buffer);
            String presetName = ByteBufCodecs.STRING_UTF8.decode(buffer);
            MotorwaySignLineData[] lines = new MotorwaySignLineData[MotorwaySignBlockEntity.MAX_SLOTS];
            for (int i = 0; i < lines.length; i++) {
                lines[i] = decodeLine(buffer);
            }
            boolean customMode = ByteBufCodecs.BOOL.decode(buffer);
            MotorwaySignPanelData[] panels = new MotorwaySignPanelData[MotorwaySignBlockEntity.MAX_CUSTOM_PANELS];
            for (int i = 0; i < panels.length; i++) {
                panels[i] = decodePanel(buffer);
            }
            MotorwaySignServiceIcon[] services = new MotorwaySignServiceIcon[MotorwaySignServiceIcon.MAX_SLOTS];
            for (int i = 0; i < services.length; i++) {
                services[i] = decodeService(buffer);
            }
            return new UpdateMotorwaySignPayload(pos, presetName, lines, customMode, panels, services);
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
            for (int i = 0; i < MotorwaySignServiceIcon.MAX_SLOTS; i++) {
                encodeService(buffer, payload.service(i));
            }
        }
    };

    public UpdateMotorwaySignPayload {
        presetName = MotorwaySignPreset.fromSerializedName(presetName).getSerializedName();
        lines = sanitizedLines(lines);
        panels = sanitizedPanels(panels);
        services = sanitizedServices(services);
    }

    private static MotorwaySignLineData[] sanitizedLines(MotorwaySignLineData[] source) {
        MotorwaySignLineData[] result = new MotorwaySignLineData[MotorwaySignBlockEntity.MAX_SLOTS];
        for (int i = 0; i < result.length; i++) {
            MotorwaySignLineData value = source != null && i < source.length ? source[i] : null;
            result[i] = safe(value);
        }
        return result;
    }

    private static MotorwaySignPanelData[] sanitizedPanels(MotorwaySignPanelData[] source) {
        MotorwaySignPanelData[] result = new MotorwaySignPanelData[MotorwaySignBlockEntity.MAX_CUSTOM_PANELS];
        for (int i = 0; i < result.length; i++) {
            MotorwaySignPanelData value = source != null && i < source.length ? source[i] : null;
            result[i] = safePanel(value);
        }
        return result;
    }

    private static MotorwaySignServiceIcon[] sanitizedServices(MotorwaySignServiceIcon[] source) {
        MotorwaySignServiceIcon[] result = new MotorwaySignServiceIcon[MotorwaySignServiceIcon.MAX_SLOTS];
        for (int i = 0; i < result.length; i++) {
            MotorwaySignServiceIcon value = source != null && i < source.length ? source[i] : null;
            result[i] = safeService(value);
        }
        return result;
    }

    public MotorwaySignPanelData panel(int index) {
        return index >= 0 && index < this.panels.length ? this.panels[index] : MotorwaySignPanelData.disabled();
    }

    public MotorwaySignServiceIcon service(int index) {
        return index >= 0 && index < this.services.length ? this.services[index] : MotorwaySignServiceIcon.NONE;
    }

    public MotorwaySignLineData line(int index) {
        return index >= 0 && index < this.lines.length ? this.lines[index] : MotorwaySignLineData.empty();
    }

    private static MotorwaySignLineData safe(MotorwaySignLineData line) {
        return line == null ? MotorwaySignLineData.empty() : line;
    }

    private static MotorwaySignPanelData safePanel(MotorwaySignPanelData panel) {
        return panel == null ? MotorwaySignPanelData.disabled() : panel;
    }

    private static MotorwaySignServiceIcon safeService(MotorwaySignServiceIcon icon) {
        return icon == null ? MotorwaySignServiceIcon.NONE : icon;
    }

    private static void encodeService(ByteBuf buffer, MotorwaySignServiceIcon icon) {
        ByteBufCodecs.STRING_UTF8.encode(buffer, safeService(icon).getSerializedName());
    }

    private static MotorwaySignServiceIcon decodeService(ByteBuf buffer) {
        return MotorwaySignServiceIcon.fromSerializedName(ByteBufCodecs.STRING_UTF8.decode(buffer));
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
