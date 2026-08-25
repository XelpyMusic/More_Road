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
import net.xelpy.moreroad.block.custom.MotorwaySignColor;
import net.xelpy.moreroad.block.custom.CartoucheType;
import net.xelpy.moreroad.block.custom.MotorwaySignGraphic;
import net.xelpy.moreroad.block.custom.MotorwaySignLineData;
import net.xelpy.moreroad.block.custom.MotorwaySignPanelData;
import net.xelpy.moreroad.block.custom.MotorwaySignPreset;
import net.xelpy.moreroad.block.custom.RoadTextFont;

/** Données persistantes du panneau autoroutier paramétrique. */
public class MotorwaySignBlockEntity extends BlockEntity {

    public static final int MAX_SLOTS = 6;
    public static final int MAX_CUSTOM_PANELS = 4;

    private MotorwaySignPreset preset = MotorwaySignPreset.D31B_EX1;
    private final MotorwaySignLineData[] lines = new MotorwaySignLineData[MAX_SLOTS];
    private boolean customMode;
    private boolean additivePanels = true;
    private final MotorwaySignPanelData[] customPanels = new MotorwaySignPanelData[MAX_CUSTOM_PANELS];

    public MotorwaySignBlockEntity(BlockPos pos, BlockState state) {
        super(MoreRoadBlockEntities.MOTORWAY_SIGN.get(), pos, state);
        applyPresetDefaults(this.preset);
        for (int index = 0; index < MAX_CUSTOM_PANELS; index++) {
            this.customPanels[index] = MotorwaySignPanelData.disabled();
        }
    }

    public MotorwaySignPreset getPreset() {
        return this.preset;
    }

    public MotorwaySignLineData getLine(int index) {
        return index >= 0 && index < MAX_SLOTS ? this.lines[index] : MotorwaySignLineData.empty();
    }

    public MotorwaySignLineData[] getLines() {
        return this.lines.clone();
    }

    public boolean isCustomMode() {
        return this.additivePanels && this.customMode;
    }

    public MotorwaySignPanelData getCustomPanel(int index) {
        return this.additivePanels && index >= 0 && index < MAX_CUSTOM_PANELS
                ? this.customPanels[index]
                : MotorwaySignPanelData.disabled();
    }

    public MotorwaySignPanelData[] getCustomPanels() {
        if (this.additivePanels) {
            return this.customPanels.clone();
        }
        MotorwaySignPanelData[] hiddenLegacyPanels = new MotorwaySignPanelData[MAX_CUSTOM_PANELS];
        for (int index = 0; index < hiddenLegacyPanels.length; index++) {
            hiddenLegacyPanels[index] = MotorwaySignPanelData.disabled();
        }
        return hiddenLegacyPanels;
    }

    public void setConfiguration(MotorwaySignPreset preset, MotorwaySignLineData[] values) {
        setConfiguration(preset, values, false, null);
    }

    public void setConfiguration(
            MotorwaySignPreset preset,
            MotorwaySignLineData[] values,
            boolean customMode,
            MotorwaySignPanelData[] panels
    ) {
        this.preset = preset == null ? MotorwaySignPreset.D31B_EX1 : preset;
        for (int i = 0; i < MAX_SLOTS; i++) {
            if (values != null && i < values.length && values[i] != null) {
                this.lines[i] = values[i];
            } else if (i < this.preset.getSlotCount()) {
                this.lines[i] = MotorwaySignLineData.fromSlot(this.preset.getSlot(i));
            } else {
                this.lines[i] = MotorwaySignLineData.empty();
            }
        }
        this.customMode = customMode;
        this.additivePanels = true;
        for (int index = 0; index < MAX_CUSTOM_PANELS; index++) {
            MotorwaySignPanelData panel = panels != null && index < panels.length
                    ? panels[index]
                    : null;
            if (panel != null) {
                this.customPanels[index] = panel;
            } else if (this.customPanels[index] == null) {
                this.customPanels[index] = MotorwaySignPanelData.disabled();
            }
        }
        setChanged();
    }

    private void applyPresetDefaults(MotorwaySignPreset targetPreset) {
        for (int i = 0; i < MAX_SLOTS; i++) {
            this.lines[i] = i < targetPreset.getSlotCount()
                    ? MotorwaySignLineData.fromSlot(targetPreset.getSlot(i))
                    : MotorwaySignLineData.empty();
        }
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.preset = MotorwaySignPreset.fromSerializedName(
                input.getStringOr("preset", MotorwaySignPreset.D31B_EX1.getSerializedName())
        );
        for (int i = 0; i < MAX_SLOTS; i++) {
            MotorwaySignLineData fallback = i < this.preset.getSlotCount()
                    ? MotorwaySignLineData.fromSlot(this.preset.getSlot(i))
                    : MotorwaySignLineData.empty();
            String prefix = "slot_" + i + "_";
            this.lines[i] = new MotorwaySignLineData(
                    input.getStringOr(prefix + "text", fallback.text()),
                    RoadTextFont.fromSerializedName(input.getStringOr(prefix + "font", fallback.font().getSerializedName())),
                    MotorwaySignColor.fromSerializedName(input.getStringOr(prefix + "color", fallback.color().getSerializedName()))
            );
        }

        this.customMode = input.getBooleanOr("custom_mode", false);
        this.additivePanels = input.getBooleanOr("additive_panels", false);
        for (int index = 0; index < MAX_CUSTOM_PANELS; index++) {
            String prefix = "custom_panel_" + index + "_";
            MotorwaySignPanelData fallback = MotorwaySignPanelData.disabled();
            this.customPanels[index] = new MotorwaySignPanelData(
                    input.getBooleanOr(prefix + "enabled", fallback.enabled()),
                    input.getIntOr(
                            prefix + "line_count",
                            input.getBooleanOr(prefix + "double_line", fallback.doubleLine()) ? 2 : 1
                    ),
                    input.getStringOr(prefix + "line1", fallback.line1()),
                    input.getStringOr(prefix + "line2", fallback.line2()),
                    input.getStringOr(prefix + "line3", fallback.line3()),
                    input.getStringOr(prefix + "line4", fallback.line4()),
                    input.getStringOr(prefix + "distance1", fallback.distance1()),
                    input.getStringOr(prefix + "distance2", fallback.distance2()),
                    input.getStringOr(prefix + "distance3", fallback.distance3()),
                    input.getStringOr(prefix + "distance4", fallback.distance4()),
                    RoadTextFont.fromSerializedName(input.getStringOr(
                            prefix + "line1_font", fallback.line1Font().getSerializedName()
                    )),
                    RoadTextFont.fromSerializedName(input.getStringOr(
                            prefix + "line2_font", fallback.line2Font().getSerializedName()
                    )),
                    RoadTextFont.fromSerializedName(input.getStringOr(
                            prefix + "line3_font", fallback.line3Font().getSerializedName()
                    )),
                    RoadTextFont.fromSerializedName(input.getStringOr(
                            prefix + "line4_font", fallback.line4Font().getSerializedName()
                    )),
                    MotorwaySignColor.fromSerializedName(input.getStringOr(
                            prefix + "background", fallback.background().getSerializedName()
                    )),
                    CartoucheType.fromSerializedName(input.getStringOr(
                            prefix + "cartouche_type", fallback.cartoucheType().getSerializedName()
                    )),
                    input.getStringOr(prefix + "cartouche_text", fallback.cartoucheText()),
                    MotorwaySignPanelData.parseGraphic(input.getStringOr(
                            prefix + "graphic", MotorwaySignGraphic.NONE.name()
                    ))
            );
        }

        /*
         * Migration du premier gabarit D62C générique vers les valeurs du
         * véritable SVG. Une configuration réellement modifiée par le joueur
         * n'est jamais écrasée.
         */
        if (this.preset == MotorwaySignPreset.D62C
                && "A 6".equals(this.lines[0].text())
                && "N 104".equals(this.lines[1].text())
                && "PARIS".equals(this.lines[2].text())
                && "LYON".equals(this.lines[3].text())) {
            applyPresetDefaults(this.preset);
        }

        if (this.preset == MotorwaySignPreset.D64
                && "A 6".equals(this.lines[0].text())
                && "A 10".equals(this.lines[1].text())
                && "BORDEAUX".equals(this.lines[2].text())) {
            applyPresetDefaults(this.preset);
        }
        if (this.preset == MotorwaySignPreset.D74A
                && "A 6".equals(this.lines[0].text())
                && "A 10".equals(this.lines[1].text())
                && "BORDEAUX".equals(this.lines[2].text())) {
            applyPresetDefaults(this.preset);
        }
        if (this.preset == MotorwaySignPreset.D74B
                && "BORDEAUX".equals(this.lines[0].text())) {
            applyPresetDefaults(this.preset);
        }

        if (this.preset == MotorwaySignPreset.D61B
                && matchesTexts("PARIS", "35 km", "LYON", "460 km")) {
            applyPresetDefaults(this.preset);
        }
        if (this.preset == MotorwaySignPreset.D62A
                && matchesTexts("CHARTRES", "ORLÉANS", "ABLIS")) {
            applyPresetDefaults(this.preset);
        }
        if (this.preset == MotorwaySignPreset.D62B
                && matchesTexts("A 6", "N 104", "PARIS", "LYON")) {
            applyPresetDefaults(this.preset);
        }
        if (this.preset == MotorwaySignPreset.D62D_TOP
                && matchesTexts("A 75", "E 11", "SAINT-ÉTIENNE", "ROANNE", "MOULINS")) {
            applyPresetDefaults(this.preset);
        }
        if (this.preset == MotorwaySignPreset.D62D_BOTTOM
                && matchesTexts("A 71", "CLERMONT-FERRAND", "RIOM")) {
            applyPresetDefaults(this.preset);
        }
        if (this.preset == MotorwaySignPreset.D63C
                && matchesTexts("SORTIE 12", "CHARTRES", "ABLIS", "1500 m")) {
            applyPresetDefaults(this.preset);
        }
        if (this.preset == MotorwaySignPreset.D63D
                && matchesTexts("A 6", "PARIS", "LYON", "1500 m")) {
            applyPresetDefaults(this.preset);
        }
        if (this.preset == MotorwaySignPreset.D71
                && matchesTexts("PROCHAINE SORTIE", "ABLIS", "TOUS SERVICES")) {
            applyPresetDefaults(this.preset);
        }
        if (this.preset == MotorwaySignPreset.D72
                && matchesTexts("ACCÈS À", "SORTIE 10", "SORTIE 12", "SORTIE 14")) {
            applyPresetDefaults(this.preset);
        }
        if (this.preset == MotorwaySignPreset.D73
                && matchesTexts("SORTIE 12", "1000 m")) {
            applyPresetDefaults(this.preset);
        }
        if (this.preset == MotorwaySignPreset.DA31A
                && matchesTexts("CHARTRES", "RAMBOUILLET", "ABLIS", "SORTIE 12")) {
            applyPresetDefaults(this.preset);
        }
        if (this.preset == MotorwaySignPreset.DA31B
                && matchesTexts("A 20", "MONTAUBAN", "CAHORS", "AUCH", "AGEN")) {
            applyPresetDefaults(this.preset);
        }
        if (this.preset == MotorwaySignPreset.DA31D
                && matchesTexts("SORTIE 12", "CHARTRES", "ABLIS")) {
            applyPresetDefaults(this.preset);
        }
        if (this.preset == MotorwaySignPreset.DA31E
                && matchesTexts("A 11", "CHARTRES", "ABLIS")) {
            applyPresetDefaults(this.preset);
        }
        if (this.preset == MotorwaySignPreset.DA31F
                && matchesTexts("A 6", "PARIS", "LYON")) {
            applyPresetDefaults(this.preset);
        }
        if ((this.preset == MotorwaySignPreset.DA32A
                || this.preset == MotorwaySignPreset.DA32A_DC)
                && matchesTexts("AIRE DE", "LIMOURS-JANVRY")) {
            applyPresetDefaults(this.preset);
        }
        if ((this.preset == MotorwaySignPreset.DA32B
                || this.preset == MotorwaySignPreset.DA32B_DC)
                && matchesTexts("AIRE DE", "LIMOURS-JANVRY")) {
            applyPresetDefaults(this.preset);
        }
        if (this.preset == MotorwaySignPreset.D31B_EX1
                && matchesTexts("N 104", "ÉVRY", "CORBEIL")) {
            applyPresetDefaults(this.preset);
        }
        if (this.preset == MotorwaySignPreset.D31B_EX2
                && matchesTexts("A 6", "PARIS", "LYON", "ÉVRY")) {
            applyPresetDefaults(this.preset);
        }
        if (this.preset == MotorwaySignPreset.D31D
                && matchesTexts("SORTIE 12", "CHARTRES", "RAMBOUILLET", "ABLIS")) {
            applyPresetDefaults(this.preset);
        }
        if (this.preset == MotorwaySignPreset.D31E
                && matchesTexts("A 11", "CHARTRES", "ABLIS")) {
            applyPresetDefaults(this.preset);
        }
        if (this.preset == MotorwaySignPreset.D41A
                && matchesTexts("SORTIE 12", "CHARTRES", "ORLÉANS", "ABLIS", "1500 m")) {
            applyPresetDefaults(this.preset);
        }
        if (this.preset == MotorwaySignPreset.D41B
                && matchesTexts("N 10", "CHARTRES", "ABLIS", "1500 m")) {
            applyPresetDefaults(this.preset);
        }
        if (this.preset == MotorwaySignPreset.D41C
                && matchesTexts("A 6", "LYON", "ÉVRY", "1500 m")) {
            applyPresetDefaults(this.preset);
        }
    }

    private boolean matchesTexts(String... expected) {
        if (expected == null || expected.length > MAX_SLOTS) {
            return false;
        }
        for (int index = 0; index < expected.length; index++) {
            if (!expected[index].equals(this.lines[index].text())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("preset", this.preset.getSerializedName());
        for (int i = 0; i < MAX_SLOTS; i++) {
            MotorwaySignLineData line = this.lines[i];
            String prefix = "slot_" + i + "_";
            output.putString(prefix + "text", line.text());
            output.putString(prefix + "font", line.font().getSerializedName());
            output.putString(prefix + "color", line.color().getSerializedName());
        }
        output.putBoolean("custom_mode", this.customMode);
        output.putBoolean("additive_panels", this.additivePanels);
        for (int index = 0; index < MAX_CUSTOM_PANELS; index++) {
            MotorwaySignPanelData panel = this.customPanels[index] == null
                    ? MotorwaySignPanelData.disabled()
                    : this.customPanels[index];
            String prefix = "custom_panel_" + index + "_";
            output.putBoolean(prefix + "enabled", panel.enabled());
            output.putInt(prefix + "line_count", panel.lineCount());
            output.putBoolean(prefix + "double_line", panel.doubleLine());
            output.putString(prefix + "line1", panel.line1());
            output.putString(prefix + "line2", panel.line2());
            output.putString(prefix + "line3", panel.line3());
            output.putString(prefix + "line4", panel.line4());
            output.putString(prefix + "distance1", panel.distance1());
            output.putString(prefix + "distance2", panel.distance2());
            output.putString(prefix + "distance3", panel.distance3());
            output.putString(prefix + "distance4", panel.distance4());
            output.putString(prefix + "line1_font", panel.line1Font().getSerializedName());
            output.putString(prefix + "line2_font", panel.line2Font().getSerializedName());
            output.putString(prefix + "line3_font", panel.line3Font().getSerializedName());
            output.putString(prefix + "line4_font", panel.line4Font().getSerializedName());
            output.putString(prefix + "background", panel.background().getSerializedName());
            output.putString(prefix + "cartouche_type", panel.cartoucheType().getSerializedName());
            output.putString(prefix + "cartouche_text", panel.cartoucheText());
            output.putString(prefix + "graphic", panel.graphic().name());
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
