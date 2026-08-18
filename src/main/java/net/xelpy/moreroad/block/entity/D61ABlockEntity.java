package net.xelpy.moreroad.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.xelpy.moreroad.block.custom.D21AType;
import net.xelpy.moreroad.block.custom.D61A2Block;
import net.xelpy.moreroad.block.custom.D61AArrowDirection;
import net.xelpy.moreroad.block.custom.D61AArrowPosition;
import net.xelpy.moreroad.block.custom.D61ABlock;
import net.xelpy.moreroad.block.custom.D61APanelData;

public class D61ABlockEntity extends net.minecraft.world.level.block.entity.BlockEntity {

    public static final int MAX_PANELS = 4;

    private final D61APanelData[] panels =
            new D61APanelData[MAX_PANELS];

    public D61ABlockEntity(BlockPos pos, BlockState state) {
        super(MoreRoadBlockEntities.D61A.get(), pos, state);

        D21AType legacyType =
                state.hasProperty(D61ABlock.TYPE)
                        ? sanitizeType(state.getValue(D61ABlock.TYPE))
                        : D21AType.WHITE;

        boolean defaultDoubleLine = state.getBlock() instanceof D61A2Block;

        this.panels[0] = new D61APanelData(
                true,
                "",
                "",
                "",
                "",
                legacyType,
                defaultDoubleLine,
                false,
                D61AArrowPosition.RIGHT,
                D61AArrowDirection.UP,
                false
        );

        for (int i = 1; i < MAX_PANELS; i++) {
            this.panels[i] = D61APanelData.disabled(defaultDoubleLine);
        }
    }

    public D61APanelData getPanel(int index) {
        if (index < 0 || index >= MAX_PANELS) {
            return D61APanelData.disabled();
        }

        return this.panels[index];
    }

    public D61APanelData[] getPanels() {
        return this.panels.clone();
    }

    public void setPanels(D61APanelData[] newPanels) {
        for (int i = 0; i < MAX_PANELS; i++) {
            D61APanelData panel =
                    newPanels != null && i < newPanels.length
                            ? newPanels[i]
                            : null;

            this.panels[i] = sanitizePanel(
                    panel == null ? D61APanelData.disabled() : panel
            );
        }

        setChanged();
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        String legacyDestination = input.getStringOr("destination", "");
        String legacyDistance = input.getStringOr("distance", "");

        D21AType legacyType =
                this.getBlockState().hasProperty(D61ABlock.TYPE)
                        ? sanitizeType(this.getBlockState().getValue(D61ABlock.TYPE))
                        : D21AType.WHITE;

        boolean blockDefaultDoubleLine =
                this.getBlockState().getBlock() instanceof D61A2Block;

        for (int i = 0; i < MAX_PANELS; i++) {
            String prefix = "panel_" + i + "_";

            boolean defaultEnabled = i == 0;
            String defaultLine1 = i == 0 ? legacyDestination : "";
            String defaultDistance1 = i == 0 ? legacyDistance : "";
            D21AType defaultType = i == 0 ? legacyType : D21AType.WHITE;

            boolean enabled = input.getBooleanOr(prefix + "enabled", defaultEnabled);
            String line1 = input.getStringOr(
                    prefix + "line1",
                    input.getStringOr(prefix + "destination", defaultLine1)
            );
            String line2 = input.getStringOr(prefix + "line2", "");
            String oldDistance = input.getStringOr(prefix + "distance", defaultDistance1);
            String distance1 = input.getStringOr(prefix + "distance1", oldDistance);
            String distance2 = input.getStringOr(prefix + "distance2", "");
            D21AType type = parseType(
                    input.getStringOr(prefix + "type", defaultType.getSerializedName())
            );
            boolean doubleLine = input.getBooleanOr(
                    prefix + "double_line",
                    blockDefaultDoubleLine
            );
            boolean arrowEnabled = input.getBooleanOr(prefix + "arrow_enabled", false);
            D61AArrowPosition arrowPosition =
                    D61AArrowPosition.fromSerializedName(
                            input.getStringOr(
                                    prefix + "arrow_position",
                                    D61AArrowPosition.RIGHT.getSerializedName()
                            )
                    );
            D61AArrowDirection arrowDirection =
                    D61AArrowDirection.fromSerializedName(
                            input.getStringOr(
                                    prefix + "arrow_direction",
                                    D61AArrowDirection.UP.getSerializedName()
                            )
                    );
            boolean autorouteLogo =
                    input.getBooleanOr(prefix + "autoroute_logo", false);

            this.panels[i] = sanitizePanel(
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
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);

        for (int i = 0; i < MAX_PANELS; i++) {
            D61APanelData panel = sanitizePanel(this.panels[i]);
            String prefix = "panel_" + i + "_";

            output.putBoolean(prefix + "enabled", panel.enabled());
            output.putString(prefix + "line1", panel.line1());
            output.putString(prefix + "line2", panel.line2());
            output.putString(prefix + "destination", panel.line1());
            output.putString(prefix + "distance1", panel.distance1());
            output.putString(prefix + "distance2", panel.distance2());
            output.putString(prefix + "distance", panel.distance());
            output.putString(prefix + "type", panel.type().getSerializedName());
            output.putBoolean(prefix + "double_line", panel.doubleLine());
            output.putBoolean(prefix + "arrow_enabled", panel.arrowEnabled());
            output.putString(prefix + "arrow_position", panel.arrowPosition().getSerializedName());
            output.putString(prefix + "arrow_direction", panel.arrowDirection().getSerializedName());
            output.putBoolean(prefix + "autoroute_logo", panel.autorouteLogo());
        }

        output.putString("destination", this.panels[0].line1());
        output.putString("distance", this.panels[0].distance());
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private static D61APanelData sanitizePanel(D61APanelData panel) {
        if (panel == null) {
            return D61APanelData.disabled();
        }

        D21AType type = sanitizeType(panel.type());

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

    private static D21AType sanitizeType(D21AType type) {
        if (type == D21AType.GREEN) {
            return D21AType.GREEN;
        }
        if (type == D21AType.BLUE) {
            return D21AType.BLUE;
        }
        return D21AType.WHITE;
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
