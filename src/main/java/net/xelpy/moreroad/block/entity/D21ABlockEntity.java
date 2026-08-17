package net.xelpy.moreroad.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.xelpy.moreroad.block.custom.D21ABlock;
import net.xelpy.moreroad.block.custom.D21APanelData;
import net.xelpy.moreroad.block.custom.D21AType;

public class D21ABlockEntity extends BlockEntity {

    public static final int MAX_PANELS = 4;

    private final D21APanelData[] panels =
            new D21APanelData[MAX_PANELS];

    public D21ABlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                MoreRoadBlockEntities.D21A.get(),
                pos,
                state
        );

        D21AType legacyType =
                state.hasProperty(D21ABlock.TYPE)
                        ? state.getValue(D21ABlock.TYPE)
                        : D21AType.WHITE;

        boolean legacyArrowRight =
                state.hasProperty(D21ABlock.ARROW_RIGHT)
                        && state.getValue(D21ABlock.ARROW_RIGHT);

        this.panels[0] =
                new D21APanelData(
                        true,
                        "",
                        "",
                        legacyType,
                        legacyArrowRight,
                        false
                );

        for (int i = 1; i < MAX_PANELS; i++) {
            this.panels[i] = D21APanelData.disabled();
        }
    }

    public D21APanelData getPanel(int index) {
        if (index < 0 || index >= MAX_PANELS) {
            return D21APanelData.disabled();
        }

        return this.panels[index];
    }

    public D21APanelData[] getPanels() {
        return this.panels.clone();
    }

    public void setPanel(
            int index,
            D21APanelData panel
    ) {
        if (index < 0 || index >= MAX_PANELS) {
            return;
        }

        this.panels[index] =
                panel == null
                        ? D21APanelData.disabled()
                        : panel;

        setChanged();
    }

    public void setPanels(D21APanelData[] newPanels) {
        for (int i = 0; i < MAX_PANELS; i++) {
            D21APanelData panel =
                    newPanels != null && i < newPanels.length
                            ? newPanels[i]
                            : null;

            this.panels[i] =
                    panel == null
                            ? D21APanelData.disabled()
                            : panel;
        }

        setChanged();
    }

    public int getEnabledPanelCount() {
        int count = 0;

        for (D21APanelData panel : this.panels) {
            if (panel.enabled()) {
                count++;
            }
        }

        return count;
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        /*
         * Compatibilité avec l'ancien D21A à panneau unique.
         */
        String legacyDestination =
                input.getStringOr(
                        "destination",
                        ""
                );

        String legacyDistance =
                input.getStringOr(
                        "distance",
                        ""
                );

        D21AType legacyType =
                this.getBlockState().hasProperty(D21ABlock.TYPE)
                        ? this.getBlockState().getValue(D21ABlock.TYPE)
                        : D21AType.WHITE;

        boolean legacyArrowRight =
                this.getBlockState().hasProperty(D21ABlock.ARROW_RIGHT)
                        && this.getBlockState().getValue(D21ABlock.ARROW_RIGHT);

        for (int i = 0; i < MAX_PANELS; i++) {
            String prefix = "panel_" + i + "_";

            boolean defaultEnabled = i == 0;

            String defaultDestination =
                    i == 0
                            ? legacyDestination
                            : "";

            String defaultDistance =
                    i == 0
                            ? legacyDistance
                            : "";

            D21AType defaultType =
                    i == 0
                            ? legacyType
                            : D21AType.WHITE;

            boolean defaultArrowRight =
                    i == 0
                            && legacyArrowRight;

            boolean enabled =
                    input.getBooleanOr(
                            prefix + "enabled",
                            defaultEnabled
                    );

            String destination =
                    input.getStringOr(
                            prefix + "destination",
                            defaultDestination
                    );

            String distance =
                    input.getStringOr(
                            prefix + "distance",
                            defaultDistance
                    );

            D21AType type =
                    parseType(
                            input.getStringOr(
                                    prefix + "type",
                                    defaultType.getSerializedName()
                            )
                    );

            boolean arrowRight =
                    input.getBooleanOr(
                            prefix + "arrow_right",
                            defaultArrowRight
                    );

            boolean autorouteLogo =
                    input.getBooleanOr(
                            prefix + "autoroute_logo",
                            false
                    );

            this.panels[i] =
                    new D21APanelData(
                            enabled,
                            destination,
                            distance,
                            type,
                            arrowRight,
                            autorouteLogo
                    );
        }
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);

        for (int i = 0; i < MAX_PANELS; i++) {
            D21APanelData panel = this.panels[i];
            String prefix = "panel_" + i + "_";

            output.putBoolean(
                    prefix + "enabled",
                    panel.enabled()
            );

            output.putString(
                    prefix + "destination",
                    panel.destination()
            );

            output.putString(
                    prefix + "distance",
                    panel.distance()
            );

            output.putString(
                    prefix + "type",
                    panel.type().getSerializedName()
            );

            output.putBoolean(
                    prefix + "arrow_right",
                    panel.arrowRight()
            );

            output.putBoolean(
                    prefix + "autoroute_logo",
                    panel.autorouteLogo()
            );
        }

        /*
         * On garde aussi les deux anciennes clés pour une compatibilité
         * maximale avec les mondes / outils utilisant encore l'ancien format.
         */
        output.putString(
                "destination",
                this.panels[0].destination()
        );

        output.putString(
                "distance",
                this.panels[0].distance()
        );
    }

    @Override
    public CompoundTag getUpdateTag(
            HolderLookup.Provider registries
    ) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private static D21AType parseType(String value) {
        if (value == null) {
            return D21AType.WHITE;
        }

        return switch (value) {
            case "green" -> D21AType.GREEN;
            case "blue" -> D21AType.BLUE;
            default -> D21AType.WHITE;
        };
    }
}
