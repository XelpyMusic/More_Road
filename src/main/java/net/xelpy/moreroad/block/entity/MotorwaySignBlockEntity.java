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
import net.xelpy.moreroad.block.custom.MotorwaySignServiceIcon;
import net.xelpy.moreroad.block.custom.RoadTextFont;

/** Données persistantes du panneau autoroutier paramétrique. */
public class MotorwaySignBlockEntity extends BlockEntity {

    public static final int MAX_SLOTS = 9;
    public static final int MAX_CUSTOM_PANELS = 4;

    private MotorwaySignPreset preset = MotorwaySignPreset.FREEFORM;
    private final MotorwaySignLineData[] lines = new MotorwaySignLineData[MAX_SLOTS];
    private boolean customMode;
    private boolean additivePanels = true;
    private final MotorwaySignPanelData[] customPanels = new MotorwaySignPanelData[MAX_CUSTOM_PANELS];
    /** Panonceaux CE choisis sous un D44 ; sans effet pour les autres modèles. */
    private MotorwaySignServiceIcon[] services = MotorwaySignServiceIcon.defaults();

    public MotorwaySignBlockEntity(BlockPos pos, BlockState state) {
        super(MoreRoadBlockEntities.MOTORWAY_SIGN.get(), pos, state);
        applyPresetDefaults(this.preset);
        this.customMode = true;
        for (int index = 0; index < MAX_CUSTOM_PANELS; index++) {
            this.customPanels[index] = index == 0
                    ? starterPanel()
                    : MotorwaySignPanelData.disabled();
        }
    }


    private static MotorwaySignPanelData starterPanel() {
        return new MotorwaySignPanelData(
                true, 1,
                "", "", "", "",
                "", "", "", "",
                RoadTextFont.L1, RoadTextFont.L1, RoadTextFont.L1, RoadTextFont.L1,
                MotorwaySignColor.BLUE, CartoucheType.NONE, "", MotorwaySignGraphic.NONE
        );
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

    public MotorwaySignServiceIcon getService(int index) {
        return index >= 0 && index < MotorwaySignServiceIcon.MAX_SLOTS
                ? this.services[index]
                : MotorwaySignServiceIcon.NONE;
    }

    public MotorwaySignServiceIcon[] getServices() {
        return this.services.clone();
    }

    public void setConfiguration(MotorwaySignPreset preset, MotorwaySignLineData[] values) {
        setConfiguration(preset, values, false, null, null);
    }

    public void setConfiguration(
            MotorwaySignPreset preset,
            MotorwaySignLineData[] values,
            boolean customMode,
            MotorwaySignPanelData[] panels
    ) {
        setConfiguration(preset, values, customMode, panels, null);
    }

    public void setConfiguration(
            MotorwaySignPreset preset,
            MotorwaySignLineData[] values,
            boolean customMode,
            MotorwaySignPanelData[] panels,
            MotorwaySignServiceIcon[] services
    ) {
        this.preset = preset == null ? MotorwaySignPreset.FREEFORM : preset;
        for (int i = 0; i < MAX_SLOTS; i++) {
            if (values != null && i < values.length && values[i] != null) {
                this.lines[i] = normalizeLineForPreset(this.preset, i, values[i]);
            } else if (i < this.preset.getSlotCount()) {
                this.lines[i] = MotorwaySignLineData.blankForSlot(this.preset.getSlot(i));
            } else {
                this.lines[i] = MotorwaySignLineData.empty();
            }
        }
        this.customMode = this.preset != MotorwaySignPreset.D32A
                && this.preset != MotorwaySignPreset.D46A
                && this.preset != MotorwaySignPreset.D47A
                && customMode;
        this.additivePanels = true;
        for (int index = 0; index < MAX_CUSTOM_PANELS; index++) {
            if (this.preset == MotorwaySignPreset.D32A || this.preset == MotorwaySignPreset.D46A || this.preset == MotorwaySignPreset.D47A) {
                /* D32a/D46a/D47a n'ont ni registre ajouté ni cartouche : on purge les anciennes données. */
                this.customPanels[index] = MotorwaySignPanelData.disabled();
                continue;
            }
            MotorwaySignPanelData panel = panels != null && index < panels.length
                    ? panels[index]
                    : null;
            if (panel != null) {
                this.customPanels[index] = panel;
            } else if (this.customPanels[index] == null) {
                this.customPanels[index] = MotorwaySignPanelData.disabled();
            }
        }
        /* Empêche aussi un ancien registre "800 m" déjà ouvert dans le GUI
         * d'être renvoyé au serveur quand on clique sur Appliquer. */
        repairD41BGeneratedDistancePanels();
        repairD41CGeneratedDistancePanels();
        for (int index = 0; index < MotorwaySignServiceIcon.MAX_SLOTS; index++) {
            MotorwaySignServiceIcon icon = services != null && index < services.length
                    ? services[index]
                    : null;
            if (icon != null) {
                this.services[index] = icon;
            } else if (this.services[index] == null) {
                this.services[index] = MotorwaySignServiceIcon.NONE;
            }
        }
        setChanged();
    }


    /**
     * D32a n'expose qu'une seule construction : caractères L4, fond blanc
     * ou bleu. On normalise aussi les anciennes sauvegardes D32a/D32b afin
     * qu'elles ne puissent pas réintroduire L1/L2, vert, rouge, etc.
     */
    private static MotorwaySignLineData normalizeLineForPreset(
            MotorwaySignPreset preset,
            int index,
            MotorwaySignLineData line
    ) {
        if (preset == MotorwaySignPreset.D32A && index >= 0 && index < 2) {
            MotorwaySignColor color = line.color() == MotorwaySignColor.BLUE
                    ? MotorwaySignColor.BLUE
                    : MotorwaySignColor.WHITE;
            return new MotorwaySignLineData(line.text(), RoadTextFont.L4, color);
        }
        if (preset == MotorwaySignPreset.D46A && index >= 0 && index < 3) {
            MotorwaySignColor color = line.color() == MotorwaySignColor.BLUE
                    ? MotorwaySignColor.BLUE
                    : MotorwaySignColor.WHITE;
            return new MotorwaySignLineData(line.text(), RoadTextFont.L4, color);
        }
        if (preset == MotorwaySignPreset.D47A && index >= 0 && index < 2) {
            MotorwaySignColor color = line.color() == MotorwaySignColor.BLUE
                    ? MotorwaySignColor.BLUE
                    : MotorwaySignColor.WHITE;
            return new MotorwaySignLineData(line.text(), RoadTextFont.L4, color);
        }
        if (preset == MotorwaySignPreset.D41C && index == 1) {
            return new MotorwaySignLineData(line.text(), line.font(), MotorwaySignColor.BLUE);
        }
        return line;
    }

    private void applyPresetDefaults(MotorwaySignPreset targetPreset) {
        for (int i = 0; i < MAX_SLOTS; i++) {
            this.lines[i] = i < targetPreset.getSlotCount()
                    ? MotorwaySignLineData.blankForSlot(targetPreset.getSlot(i))
                    : MotorwaySignLineData.empty();
        }
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        String serializedPreset = input.getStringOr(
                "preset", MotorwaySignPreset.FREEFORM.getSerializedName()
        );
        boolean legacyD32Blue = "d32b".equals(serializedPreset);
        this.preset = MotorwaySignPreset.fromSerializedName(serializedPreset);
        for (int i = 0; i < MAX_SLOTS; i++) {
            MotorwaySignLineData fallback = i < this.preset.getSlotCount()
                    ? MotorwaySignLineData.blankForSlot(this.preset.getSlot(i))
                    : MotorwaySignLineData.empty();
            MotorwaySignColor fallbackColor = legacyD32Blue && i < 2
                    ? MotorwaySignColor.BLUE
                    : fallback.color();
            String prefix = "slot_" + i + "_";
            this.lines[i] = normalizeLineForPreset(
                    this.preset,
                    i,
                    new MotorwaySignLineData(
                            input.getStringOr(prefix + "text", fallback.text()),
                            RoadTextFont.fromSerializedName(input.getStringOr(prefix + "font", fallback.font().getSerializedName())),
                            MotorwaySignColor.fromSerializedName(input.getStringOr(prefix + "color", fallbackColor.getSerializedName()))
                    )
            );
        }

        this.customMode = this.preset != MotorwaySignPreset.D32A
                && this.preset != MotorwaySignPreset.D46A
                && this.preset != MotorwaySignPreset.D47A
                && input.getBooleanOr("custom_mode", false);
        this.additivePanels = input.getBooleanOr("additive_panels", false);
        for (int index = 0; index < MAX_CUSTOM_PANELS; index++) {
            String prefix = "custom_panel_" + index + "_";
            MotorwaySignPanelData fallback = MotorwaySignPanelData.disabled();
            MotorwaySignPanelData loadedPanel = new MotorwaySignPanelData(
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
            this.customPanels[index] = (this.preset == MotorwaySignPreset.D32A
                    || this.preset == MotorwaySignPreset.D46A
                    || this.preset == MotorwaySignPreset.D47A)
                    ? MotorwaySignPanelData.disabled()
                    : loadedPanel;
        }

        migrateLegacyHeaderOnlyConfigurations();
        repairD41BGeneratedDistancePanels();
        repairD41CGeneratedDistancePanels();

        MotorwaySignServiceIcon[] serviceDefaults = MotorwaySignServiceIcon.defaults();
        for (int index = 0; index < MotorwaySignServiceIcon.MAX_SLOTS; index++) {
            this.services[index] = MotorwaySignServiceIcon.fromSerializedName(
                    input.getStringOr("service_" + index, serviceDefaults[index].getSerializedName())
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
                && matchesTexts("4", "1000 m")) {
            applyPresetDefaults(this.preset);
        }
        if (this.preset == MotorwaySignPreset.D41B
                && matchesTexts("N 10", "CHARTRES", "ABLIS", "1500 m")) {
            applyPresetDefaults(this.preset);
        }
        if (this.preset == MotorwaySignPreset.D41C
                && (matchesTexts("A 67", "CLERMONT-FD", "ST ÉTIENNE", "NEVERS", "1000 m")
                || matchesTexts("A 6", "LYON", "ÉVRY", "1500 m"))) {
            applyPresetDefaults(this.preset);
        }
        /*
         * D44 avait auparavant deux lignes de texte libres suivies d'une
         * distance ("PROCHAINE AIRE" / "LIMOURS-JANVRY" / "20 km"). Le
         * préréglage a été refait pour suivre le vrai registre sortie +
         * distance puis nom du village étape : un panneau resté sur cet
         * ancien texte par défaut reprend donc les nouvelles valeurs.
         */
        if (this.preset == MotorwaySignPreset.D44
                && matchesTexts("PROCHAINE AIRE", "LIMOURS-JANVRY", "20 km")) {
            applyPresetDefaults(this.preset);
        }
    }

    /**
     * Migration vers les variantes « en-tête fixe + registres libres ».
     * Les anciennes destinations intégrées au SVG sont converties une seule
     * fois en pancartes personnalisables. Les registres supplémentaires déjà
     * créés par le joueur sont ensuite conservés tant qu'il reste de la place.
     */
    private void migrateLegacyHeaderOnlyConfigurations() {
        MotorwaySignPanelData[] existing = this.customPanels.clone();
        int nextPanel = 0;
        boolean migrated = false;

        switch (this.preset) {
            case D31B_EX1 -> {
                if (hasAnyLegacyText(1, 2)) {
                    clearCustomPanels();
                    nextPanel = appendLegacyPanel(nextPanel, new int[]{1, 2}, MotorwaySignColor.WHITE,
                            CartoucheType.NONE, "");
                    migrated = true;
                }
            }
            case D31B_EX2 -> {
                if (hasAnyLegacyText(1, 2, 3)) {
                    clearCustomPanels();
                    nextPanel = appendLegacyPanel(nextPanel, new int[]{1, 2, 3}, MotorwaySignColor.BLUE,
                            CartoucheType.NONE, "");
                    migrated = true;
                }
            }
            case D31D -> {
                if (hasAnyLegacyText(1, 2, 3, 4, 5, 6, 7)) {
                    CartoucheType type = existing.length > 0 && existing[0] != null
                            ? existing[0].cartoucheType() : CartoucheType.NONE;
                    String cartoucheText = existing.length > 0 && existing[0] != null
                            ? existing[0].cartoucheText() : "";
                    clearCustomPanels();
                    nextPanel = appendLegacyPanel(nextPanel, new int[]{1, 2, 3, 4}, MotorwaySignColor.GREEN,
                            type, cartoucheText);
                    nextPanel = appendLegacyPanel(nextPanel, new int[]{5, 6, 7}, MotorwaySignColor.WHITE,
                            CartoucheType.NONE, "");
                    migrated = true;
                }
            }
            case D31E -> {
                if (hasAnyLegacyText(1, 2, 3, 4, 5, 6, 7, 8)) {
                    clearCustomPanels();
                    nextPanel = appendLegacyPanel(nextPanel, new int[]{1, 2, 3, 4}, MotorwaySignColor.GREEN,
                            CartoucheType.NONE, "");
                    nextPanel = appendLegacyPanel(nextPanel, new int[]{5, 6, 7, 8}, MotorwaySignColor.WHITE,
                            CartoucheType.NONE, "");
                    migrated = true;
                }
            }
            case D41A -> {
                /* Ancien format : destinations 1..3 puis distance en slot 4. */
                if (hasAnyLegacyText(2, 3, 4)) {
                    MotorwaySignLineData legacyDistance = this.lines[4];
                    clearCustomPanels();
                    nextPanel = appendLegacyPanel(nextPanel, new int[]{1, 2},
                            this.lines[1].color() == MotorwaySignColor.BLUE
                                    ? MotorwaySignColor.BLUE : MotorwaySignColor.GREEN,
                            CartoucheType.NONE, "");
                    nextPanel = appendLegacyPanel(nextPanel, new int[]{3}, MotorwaySignColor.WHITE,
                            CartoucheType.NONE, "");
                    this.lines[1] = legacyDistance.text().isBlank()
                            ? MotorwaySignLineData.blankForSlot(this.preset.getSlot(1))
                            : normalizeLineForPreset(this.preset, 1, legacyDistance);
                    migrated = true;
                }
            }
            case D41B -> {
                /*
                 * Ancien format : slot 1 = destination verte, slot 2 =
                 * destination locale, slot 3 = distance. Le NOUVEAU format
                 * utilise déjà le slot 1 pour la distance de l'en-tête.
                 * Il ne faut donc surtout pas tester le slot 1 pour décider
                 * qu'une migration est nécessaire : "800 m" serait alors
                 * reconverti en registre à chaque chargement, ce qui créait
                 * les pancartes 800 m en cascade signalées en jeu.
                 */
                if (hasAnyLegacyText(2, 3)) {
                    MotorwaySignLineData legacyDistance = this.lines[3];
                    clearCustomPanels();
                    nextPanel = appendLegacyPanel(nextPanel, new int[]{1}, MotorwaySignColor.GREEN,
                            CartoucheType.NONE, "");
                    nextPanel = appendLegacyPanel(nextPanel, new int[]{2}, MotorwaySignColor.WHITE,
                            CartoucheType.NONE, "");
                    this.lines[1] = legacyDistance.text().isBlank()
                            ? MotorwaySignLineData.blankForSlot(this.preset.getSlot(1))
                            : normalizeLineForPreset(this.preset, 1, legacyDistance);
                    migrated = true;
                }
            }
            case D41C -> {
                /*
                 * Ancien format : slots 1..3 = destinations bleues, slot 4 = distance.
                 * Nouveau format : seul l'en-tête reste dans le panneau principal
                 * (slot 0 = cartouche route, slot 1 = distance), toutes les villes
                 * passent dans les registres personnalisables.
                 */
                if (hasAnyLegacyText(2, 3, 4)
                        || (hasAnyLegacyText(1) && !looksLikeDistanceText(this.lines[1].text()))) {
                    MotorwaySignLineData legacyDistance = this.lines[4];
                    clearCustomPanels();
                    nextPanel = appendLegacyPanel(nextPanel, new int[]{1, 2}, MotorwaySignColor.BLUE,
                            CartoucheType.NONE, "");
                    nextPanel = appendLegacyPanel(nextPanel, new int[]{3}, MotorwaySignColor.BLUE,
                            CartoucheType.NONE, "");
                    this.lines[1] = legacyDistance.text().isBlank()
                            ? MotorwaySignLineData.blankForSlot(this.preset.getSlot(1))
                            : normalizeLineForPreset(this.preset, 1, legacyDistance);
                    migrated = true;
                }
            }
            default -> {
                return;
            }
        }

        if (!migrated) {
            return;
        }

        this.additivePanels = true;
        nextPanel = appendExistingPanelContents(existing, nextPanel);
        int firstClearedSlot = (this.preset == MotorwaySignPreset.D41A || this.preset == MotorwaySignPreset.D41B
                || this.preset == MotorwaySignPreset.D41C) ? 2 : 1;
        for (int index = firstClearedSlot; index < MAX_SLOTS; index++) {
            this.lines[index] = MotorwaySignLineData.empty();
        }
    }

    /**
     * Répare les sauvegardes produites par la première version du D41b
     * « en-tête + registres libres ». À cause de l'ancienne condition de
     * migration, la distance de l'en-tête (ex. 800 m) pouvait être recopiée
     * dans un nouveau registre à chaque chargement, jusqu'à remplir les 4
     * registres. On supprime uniquement les pancartes dont le SEUL contenu
     * est exactement la distance de l'en-tête ; toutes les vraies villes et
     * leur ordre sont conservés.
     */
    private void repairD41BGeneratedDistancePanels() {
        if (this.preset != MotorwaySignPreset.D41B) {
            return;
        }
        String headerDistance = this.lines[1] == null ? "" : this.lines[1].text();
        if (headerDistance == null || headerDistance.isBlank()) {
            return;
        }

        MotorwaySignPanelData[] repaired = new MotorwaySignPanelData[MAX_CUSTOM_PANELS];
        int target = 0;
        boolean changed = false;
        for (MotorwaySignPanelData panel : this.customPanels) {
            MotorwaySignPanelData safePanel = panel == null
                    ? MotorwaySignPanelData.disabled()
                    : panel;
            if (isDistanceOnlyPanel(safePanel, headerDistance)) {
                changed = true;
                continue;
            }
            if (target < repaired.length) {
                repaired[target++] = safePanel;
            }
        }
        while (target < repaired.length) {
            repaired[target++] = MotorwaySignPanelData.disabled();
        }
        if (changed) {
            System.arraycopy(repaired, 0, this.customPanels, 0, MAX_CUSTOM_PANELS);
            this.additivePanels = true;
        }
    }

    private static boolean isDistanceOnlyPanel(
            MotorwaySignPanelData panel,
            String headerDistance
    ) {
        if (panel == null || !panel.hasPanelContent()
                || panel.cartoucheType().isVisible()
                || panel.graphic() != MotorwaySignGraphic.NONE) {
            return false;
        }
        if (!panel.distance1().isBlank() || !panel.distance2().isBlank()
                || !panel.distance3().isBlank() || !panel.distance4().isBlank()) {
            return false;
        }

        boolean foundText = false;
        for (int index = 0; index < 4; index++) {
            String value = panel.line(index);
            if (value == null || value.isBlank()) {
                continue;
            }
            foundText = true;
            if (!sameSignText(value, headerDistance)) {
                return false;
            }
        }
        return foundText;
    }

    private void repairD41CGeneratedDistancePanels() {
        if (this.preset != MotorwaySignPreset.D41C) {
            return;
        }
        String headerDistance = this.lines[1] == null ? "" : this.lines[1].text();
        if (headerDistance == null || headerDistance.isBlank()) {
            return;
        }

        MotorwaySignPanelData[] repaired = new MotorwaySignPanelData[MAX_CUSTOM_PANELS];
        int target = 0;
        boolean changed = false;
        for (MotorwaySignPanelData panel : this.customPanels) {
            MotorwaySignPanelData safePanel = panel == null
                    ? MotorwaySignPanelData.disabled()
                    : panel;
            if (isDistanceOnlyPanel(safePanel, headerDistance)) {
                changed = true;
                continue;
            }
            if (target < repaired.length) {
                repaired[target++] = safePanel;
            }
        }
        while (target < repaired.length) {
            repaired[target++] = MotorwaySignPanelData.disabled();
        }
        if (changed) {
            System.arraycopy(repaired, 0, this.customPanels, 0, MAX_CUSTOM_PANELS);
            this.additivePanels = true;
        }
    }

    private static boolean looksLikeDistanceText(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.strip().toLowerCase().replaceAll("\\s+", " ");
        return normalized.matches("[0-9]+([ ,.][0-9]+)? ?(m|km)");
    }

    private static boolean sameSignText(String first, String second) {
        if (first == null || second == null) {
            return false;
        }
        String a = first.strip().replaceAll("\\s+", " ");
        String b = second.strip().replaceAll("\\s+", " ");
        return a.equalsIgnoreCase(b);
    }

    private boolean hasAnyLegacyText(int... slots) {
        for (int slot : slots) {
            if (slot >= 0 && slot < MAX_SLOTS
                    && this.lines[slot] != null
                    && !this.lines[slot].text().isBlank()) {
                return true;
            }
        }
        return false;
    }

    private void clearCustomPanels() {
        for (int index = 0; index < MAX_CUSTOM_PANELS; index++) {
            this.customPanels[index] = MotorwaySignPanelData.disabled();
        }
    }

    private int appendLegacyPanel(
            int target,
            int[] slots,
            MotorwaySignColor background,
            CartoucheType cartoucheType,
            String cartoucheText
    ) {
        if (target >= MAX_CUSTOM_PANELS) {
            return target;
        }
        String[] texts = {"", "", "", ""};
        RoadTextFont[] fonts = {RoadTextFont.L1, RoadTextFont.L1, RoadTextFont.L1, RoadTextFont.L1};
        int count = 0;
        for (int slot : slots) {
            if (slot < 0 || slot >= MAX_SLOTS || count >= 4) {
                continue;
            }
            MotorwaySignLineData line = this.lines[slot];
            if (line == null || line.text().isBlank()) {
                continue;
            }
            texts[count] = line.text();
            fonts[count] = line.font();
            count++;
        }
        CartoucheType safeCartouche = cartoucheType == null ? CartoucheType.NONE : cartoucheType;
        if (count == 0 && !safeCartouche.isVisible()) {
            return target;
        }
        this.customPanels[target] = new MotorwaySignPanelData(
                count > 0, Math.max(1, count),
                texts[0], texts[1], texts[2], texts[3],
                "", "", "", "",
                fonts[0], fonts[1], fonts[2], fonts[3],
                background, safeCartouche, cartoucheText, MotorwaySignGraphic.NONE
        );
        return target + 1;
    }

    private int appendExistingPanelContents(MotorwaySignPanelData[] existing, int target) {
        if (existing == null) {
            return target;
        }
        for (MotorwaySignPanelData panel : existing) {
            if (target >= MAX_CUSTOM_PANELS) {
                break;
            }
            if (panel == null || !panel.hasPanelContent()) {
                continue;
            }
            this.customPanels[target++] = new MotorwaySignPanelData(
                    true, panel.lineCount(),
                    panel.line1(), panel.line2(), panel.line3(), panel.line4(),
                    panel.distance1(), panel.distance2(), panel.distance3(), panel.distance4(),
                    panel.line1Font(), panel.line2Font(), panel.line3Font(), panel.line4Font(),
                    panel.background(), CartoucheType.NONE, "", panel.graphic()
            );
        }
        return target;
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
        for (int index = 0; index < MotorwaySignServiceIcon.MAX_SLOTS; index++) {
            MotorwaySignServiceIcon icon = this.services[index] == null
                    ? MotorwaySignServiceIcon.NONE
                    : this.services[index];
            output.putString("service_" + index, icon.getSerializedName());
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
