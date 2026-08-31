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
import net.xelpy.moreroad.block.custom.CartoucheType;
import net.xelpy.moreroad.block.custom.D21A2Block;
import net.xelpy.moreroad.block.custom.D21ABlock;
import net.xelpy.moreroad.block.custom.D21APanelData;
import net.xelpy.moreroad.block.custom.D21AType;
import net.xelpy.moreroad.block.custom.RoadTextFont;

public class D21ABlockEntity extends BlockEntity {

    public static final int MAX_PANELS = 4;

    private final D21APanelData[] panels =
            new D21APanelData[MAX_PANELS];

    private CartoucheType cartoucheType = CartoucheType.NONE;
    private String cartoucheText = "";

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

        boolean defaultDoubleLine =
                state.getBlock() instanceof D21A2Block;

        this.panels[0] =
                new D21APanelData(
                        true,
                        "",
                        "",
                        "",
                        "",
                        legacyType,
                        legacyArrowRight,
                        false,
                        defaultDoubleLine
                );

        for (int i = 1; i < MAX_PANELS; i++) {
            this.panels[i] =
                    D21APanelData.disabled(defaultDoubleLine);
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

    public CartoucheType getCartoucheType() {
        return this.cartoucheType;
    }

    public void setCartoucheType(CartoucheType cartoucheType) {
        this.cartoucheType =
                cartoucheType == null
                        ? CartoucheType.NONE
                        : cartoucheType;

        setChanged();
    }

    public String getCartoucheText() {
        return this.cartoucheText;
    }

    public void setCartoucheText(String cartoucheText) {
        this.cartoucheText =
                cartoucheText == null
                        ? ""
                        : cartoucheText;

        setChanged();
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

        this.cartoucheType = CartoucheType.fromSerializedName(
                input.getStringOr("cartouche_type", "none")
        );
        this.cartoucheText = input.getStringOr("cartouche_text", "");

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

        boolean blockDefaultDoubleLine =
                this.getBlockState().getBlock() instanceof D21A2Block;

        for (int i = 0; i < MAX_PANELS; i++) {
            String prefix = "panel_" + i + "_";

            boolean defaultEnabled = i == 0;

            String defaultLine1 =
                    i == 0
                            ? legacyDestination
                            : "";

            String defaultDistance1 =
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

            String line1 =
                    input.getStringOr(
                            prefix + "line1",
                            input.getStringOr(
                                    prefix + "destination",
                                    defaultLine1
                            )
                    );

            String line2 =
                    input.getStringOr(
                            prefix + "line2",
                            ""
                    );

            String oldDistance =
                    input.getStringOr(
                            prefix + "distance",
                            defaultDistance1
                    );

            String distance1 =
                    input.getStringOr(
                            prefix + "distance1",
                            oldDistance
                    );

            String distance2 =
                    input.getStringOr(
                            prefix + "distance2",
                            ""
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

            /*
             * Compatibilité : les anciens blocs D21A restent simples et
             * les anciens blocs D21A2 restent doubles tant qu'aucune valeur
             * par panneau n'a encore été enregistrée.
             */
            boolean doubleLine =
                    input.getBooleanOr(
                            prefix + "double_line",
                            blockDefaultDoubleLine
                    );

            RoadTextFont line1Font =
                    RoadTextFont.fromSerializedName(
                            input.getStringOr(
                                    prefix + "line1_font",
                                    RoadTextFont.L1.getSerializedName()
                            )
                    );

            RoadTextFont line2Font =
                    RoadTextFont.fromSerializedName(
                            input.getStringOr(
                                    prefix + "line2_font",
                                    RoadTextFont.L1.getSerializedName()
                            )
                    );

            boolean line1Spacing =
                    input.getBooleanOr(
                            prefix + "line1_spacing",
                            false
                    );

            boolean line2Spacing =
                    input.getBooleanOr(
                            prefix + "line2_spacing",
                            false
                    );

            this.panels[i] =
                    new D21APanelData(
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
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);

        output.putString(
                "cartouche_type",
                this.cartoucheType.getSerializedName()
        );
        output.putString("cartouche_text", this.cartoucheText);

        for (int i = 0; i < MAX_PANELS; i++) {
            D21APanelData panel = this.panels[i];
            String prefix = "panel_" + i + "_";

            output.putBoolean(
                    prefix + "enabled",
                    panel.enabled()
            );

            output.putString(
                    prefix + "line1",
                    panel.line1()
            );

            output.putString(
                    prefix + "line2",
                    panel.line2()
            );

            output.putString(
                    prefix + "destination",
                    panel.line1()
            );

            output.putString(
                    prefix + "distance1",
                    panel.distance1()
            );

            output.putString(
                    prefix + "distance2",
                    panel.distance2()
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

            output.putBoolean(
                    prefix + "double_line",
                    panel.doubleLine()
            );

            output.putString(
                    prefix + "line1_font",
                    panel.line1Font().getSerializedName()
            );

            output.putString(
                    prefix + "line2_font",
                    panel.line2Font().getSerializedName()
            );

            output.putBoolean(
                    prefix + "line1_spacing",
                    panel.line1Spacing()
            );
            output.putBoolean(
                    prefix + "line2_spacing",
                    panel.line2Spacing()
            );
        }

        output.putString(
                "destination",
                this.panels[0].line1()
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
