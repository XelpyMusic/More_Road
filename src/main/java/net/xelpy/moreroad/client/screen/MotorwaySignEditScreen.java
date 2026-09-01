package net.xelpy.moreroad.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.xelpy.moreroad.MoreRoad;
import net.xelpy.moreroad.block.custom.CartoucheType;
import net.xelpy.moreroad.block.custom.MotorwaySignCatalogInfo;
import net.xelpy.moreroad.block.custom.MotorwaySignColor;
import net.xelpy.moreroad.block.custom.MotorwaySignGraphic;
import net.xelpy.moreroad.block.custom.MotorwaySignLineData;
import net.xelpy.moreroad.block.custom.MotorwaySignPanelData;
import net.xelpy.moreroad.block.custom.MotorwaySignPreset;
import net.xelpy.moreroad.block.custom.MotorwaySignPreviewLayout;
import net.xelpy.moreroad.block.custom.MotorwaySignRole;
import net.xelpy.moreroad.block.custom.MotorwaySignServiceIcon;
import net.xelpy.moreroad.block.custom.MotorwaySignSlot;
import net.xelpy.moreroad.client.renderer.MotorwaySignArtworkCatalog;
import net.xelpy.moreroad.block.custom.MotorwaySignStyleProfile;
import net.xelpy.moreroad.block.custom.RoadTextFont;
import net.xelpy.moreroad.block.entity.MotorwaySignBlockEntity;
import net.xelpy.moreroad.network.UpdateMotorwaySignPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/** Éditeur commun aux 62 modèles autoroutiers issus de l'archive SVG. */
public class MotorwaySignEditScreen extends Screen {

    private static final int MAX_TEXT_LENGTH = 64;
    private static final FontDescription.Resource ROAD_FONT_L1 = new FontDescription.Resource(
            Identifier.fromNamespaceAndPath(MoreRoad.MODID, "caracteres_l1")
    );
    private static final FontDescription.Resource ROAD_FONT_L4 = new FontDescription.Resource(
            Identifier.fromNamespaceAndPath(MoreRoad.MODID, "caracteres_l4")
    );
    private static final FontDescription.Resource ROAD_FONT_L2 = new FontDescription.Resource(
            Identifier.fromNamespaceAndPath(MoreRoad.MODID, "caracteres_l2")
    );

    private static FontDescription.Resource roadFontResource(RoadTextFont font) {
        return switch (font) {
            case L2 -> ROAD_FONT_L2;
            case L4 -> ROAD_FONT_L4;
            case L1, NORMAL -> ROAD_FONT_L1;
        };
    }

    private final BlockPos blockPos;
    private MotorwaySignPreset preset;
    private final String[] initialTexts = new String[MotorwaySignBlockEntity.MAX_SLOTS];
    private final RoadTextFont[] lineFonts = new RoadTextFont[MotorwaySignBlockEntity.MAX_SLOTS];
    private final MotorwaySignColor[] lineColors = new MotorwaySignColor[MotorwaySignBlockEntity.MAX_SLOTS];
    private final EditBox[] fields = new EditBox[MotorwaySignBlockEntity.MAX_SLOTS];
    private final SignEditorUi.Rect[] fontRects = new SignEditorUi.Rect[MotorwaySignBlockEntity.MAX_SLOTS];
    private final SignEditorUi.Rect[] colorRects = new SignEditorUi.Rect[MotorwaySignBlockEntity.MAX_SLOTS];
    /** Onglet de réglage affiché : contenu du modèle ou l'une de ses pancartes. */
    private boolean customMode;
    private final MotorwaySignPanelData[] customPanels =
            new MotorwaySignPanelData[MotorwaySignBlockEntity.MAX_CUSTOM_PANELS];
    private int selectedCustomPanel;
    private EditBox customLine1Field;
    private EditBox customLine2Field;
    private EditBox customLine3Field;
    private EditBox customLine4Field;
    private EditBox customDistance1Field;
    private EditBox customDistance2Field;
    private EditBox customDistance3Field;
    private EditBox customDistance4Field;
    private EditBox customCartoucheField;
    private EditBox contentCartoucheField;
    private final SignEditorUi.Rect[] customTabRects =
            new SignEditorUi.Rect[MotorwaySignBlockEntity.MAX_CUSTOM_PANELS];
    private final SignEditorUi.Rect[] customEnabledRects =
            new SignEditorUi.Rect[MotorwaySignBlockEntity.MAX_CUSTOM_PANELS];
    /** Choix des panonceaux CE du D44 (voir MotorwaySignServiceIcon) : sans effet pour les autres modèles. */
    private final MotorwaySignServiceIcon[] services = new MotorwaySignServiceIcon[MotorwaySignServiceIcon.MAX_SLOTS];
    private final SignEditorUi.Rect[] serviceRects = new SignEditorUi.Rect[MotorwaySignServiceIcon.MAX_SLOTS];
    private final SignEditorUi.Rect[] customPageRects = new SignEditorUi.Rect[4];
    private int customSettingsPage;
    private SignEditorUi.Rect modeRect;
    private SignEditorUi.Rect customDoubleLineRect;
    private SignEditorUi.Rect customLine1FontRect;
    private SignEditorUi.Rect customLine2FontRect;
    private SignEditorUi.Rect customLine3FontRect;
    private SignEditorUi.Rect customLine4FontRect;
    private SignEditorUi.Rect customWhiteRect;
    private SignEditorUi.Rect customGreenRect;
    private SignEditorUi.Rect customBlueRect;
    private SignEditorUi.Rect customCartoucheRect;
    private SignEditorUi.Rect contentCartoucheRect;
    private SignEditorUi.Rect customGraphicRect;

    private SignEditorUi.Rect previewRect;
    private SignEditorUi.Rect contentRect;
    private SignEditorUi.Rect previousPresetRect;
    private SignEditorUi.Rect presetRect;
    private SignEditorUi.Rect nextPresetRect;
    private SignEditorUi.Rect resetRect;
    private SignEditorUi.Rect applyRect;
    private SignEditorUi.Rect cancelRect;

    private int windowX;
    private int windowY;
    private int windowWidth;
    private int windowHeight;
    private float scale = 1.0F;

    public MotorwaySignEditScreen(
            BlockPos blockPos,
            MotorwaySignPreset preset,
            MotorwaySignLineData[] values,
            boolean customMode,
            MotorwaySignPanelData[] customPanels,
            MotorwaySignServiceIcon[] services
    ) {
        super(Component.literal("Panneaux autoroutiers modulables"));
        this.blockPos = blockPos.immutable();
        this.preset = preset == null ? MotorwaySignPreset.FREEFORM : preset;
        /* L'ancien mode est volontairement ignoré : l'éditeur est désormais unifié. */
        this.customMode = false;
        for (int i = 0; i < MotorwaySignBlockEntity.MAX_SLOTS; i++) {
            MotorwaySignLineData fallback = i < this.preset.getSlotCount()
                    ? MotorwaySignLineData.blankForSlot(this.preset.getSlot(i))
                    : MotorwaySignLineData.empty();
            MotorwaySignLineData data = values != null && i < values.length && values[i] != null
                    ? values[i]
                    : fallback;
            this.initialTexts[i] = data.text();
            if (this.preset == MotorwaySignPreset.D32A && i < 2) {
                this.lineFonts[i] = RoadTextFont.L4;
                this.lineColors[i] = data.color() == MotorwaySignColor.BLUE
                        ? MotorwaySignColor.BLUE
                        : MotorwaySignColor.WHITE;
            } else {
                this.lineFonts[i] = data.font();
                this.lineColors[i] = data.color();
            }
        }
        for (int index = 0; index < this.customPanels.length; index++) {
            MotorwaySignPanelData panel = customPanels != null && index < customPanels.length
                    ? customPanels[index]
                    : null;
            MotorwaySignPanelData normalized = panel != null
                    ? withAllowedCustomBackground(panel)
                    : MotorwaySignPanelData.disabled();
            this.customPanels[index] = index == 0
                    ? normalized
                    : withoutCartouche(normalized);
        }
        MotorwaySignServiceIcon[] serviceDefaults = MotorwaySignServiceIcon.defaults();
        for (int index = 0; index < this.services.length; index++) {
            this.services[index] = services != null && index < services.length && services[index] != null
                    ? services[index]
                    : serviceDefaults[index];
        }
    }

    @Override
    protected void init() {
        super.init();
        int marginX = Math.max(10, Math.min(34, this.width / 24));
        int marginY = Math.max(8, Math.min(24, this.height / 24));
        this.windowX = marginX;
        this.windowY = marginY;
        this.windowWidth = this.width - marginX * 2;
        this.windowHeight = this.height - marginY * 2;
        this.scale = SignEditorUi.adaptiveEditorScale(this.windowWidth, this.windowHeight, 1220.0F, 720.0F);

        int pad = s(12);
        int header = SignEditorUi.adaptiveHeaderHeight(this.scale, false);
        int footer = SignEditorUi.adaptiveFooterHeight(this.scale);
        int gap = s(12);
        int bodyY = this.windowY + header;
        int bodyH = this.windowHeight - header - footer;
        int leftW = Math.max(s(340), Math.round((this.windowWidth - pad * 2 - gap) * 0.45F));
        int rightW = this.windowWidth - pad * 2 - gap - leftW;
        int leftX = this.windowX + pad;
        int rightX = leftX + leftW + gap;

        this.previewRect = new SignEditorUi.Rect(leftX, bodyY, leftW, bodyH - s(8));
        this.contentRect = new SignEditorUi.Rect(rightX, bodyY, rightW, bodyH - s(8));

        int innerX = this.contentRect.x() + s(10);
        int innerW = this.contentRect.width() - s(20);
        int selectorY = this.contentRect.y() + s(34);
        int selectorH = SignEditorUi.safeControlHeight(this.font, s(25));
        int resetW = Math.max(70, s(98));
        int selectorGap = Math.max(3, s(6));
        int presetW = innerW - resetW - selectorGap;

        this.previousPresetRect = new SignEditorUi.Rect(0, 0, 0, 0);
        this.presetRect = new SignEditorUi.Rect(innerX, selectorY, presetW, selectorH);
        this.nextPresetRect = new SignEditorUi.Rect(0, 0, 0, 0);
        this.resetRect = new SignEditorUi.Rect(this.presetRect.x() + presetW + selectorGap, selectorY, resetW, selectorH);

        int editorTabsY = selectorY + selectorH + s(6);
        int editorTabGap = Math.max(2, s(4));
        int editorTabW = (innerW - editorTabGap * 4) / 5;
        this.modeRect = new SignEditorUi.Rect(
                innerX, editorTabsY, editorTabW, selectorH
        );
        int enabledW = Math.max(18, s(22));
        for (int index = 0; index < this.customTabRects.length; index++) {
            int x = innerX + (index + 1) * (editorTabW + editorTabGap);
            int width = index == this.customTabRects.length - 1
                    ? innerX + innerW - x
                    : editorTabW;
            this.customTabRects[index] = new SignEditorUi.Rect(
                    x, editorTabsY, width, selectorH
            );
            this.customEnabledRects[index] = new SignEditorUi.Rect(
                    x + width - enabledW, editorTabsY, enabledW, selectorH
            );
        }
        if (!allowsExtraPanels(this.preset)) {
            if (showsServiceRow(this.preset)) {
                /*
                 * D44 : réutilise la rangée "Panneau principal / Registre N"
                 * pour les 6 panonceaux CE.
                 */
                int serviceGap = Math.max(2, s(4));
                int serviceW = (innerW - serviceGap * (this.serviceRects.length - 1)) / this.serviceRects.length;
                for (int index = 0; index < this.serviceRects.length; index++) {
                    int x = innerX + index * (serviceW + serviceGap);
                    int width = index == this.serviceRects.length - 1
                            ? innerX + innerW - x
                            : serviceW;
                    this.serviceRects[index] = new SignEditorUi.Rect(x, editorTabsY, width, selectorH);
                }
            } else {
                /*
                 * D32a : aucun registre supplémentaire. On conserve seulement
                 * l'onglet "Panneau principal", sur toute la largeur.
                 */
                this.modeRect = new SignEditorUi.Rect(innerX, editorTabsY, innerW, selectorH);
            }
        }
        int rowsTop = editorTabsY + selectorH + s(13);
        int availableRowsH = this.contentRect.y() + this.contentRect.height() - rowsTop - s(9);
        int rowGap = Math.max(3, s(7));
        /*
         * Signalé : la cartouche du panneau principal (rangée bouton +
         * rangée champ) chevauchait les derniers champs des modèles à
         * beaucoup de lignes (ex. D31d, 7 champs) — le budget de rangées
         * ci-dessous ne suivait pas MAX_SLOTS (initialement calé sur 6
         * champs + 2 rangées de cartouche = 8). Recalé sur la vraie
         * capacité maximale : MAX_SLOTS champs + 2 rangées de cartouche.
         */
        int maxRows = MotorwaySignBlockEntity.MAX_SLOTS + 2;
        int rowH = Math.max(18, (availableRowsH - rowGap * (maxRows - 1)) / maxRows);
        int fieldH = Math.min(rowH, Math.max(20, s(25)));
        int colorW = Math.max(62, s(90));
        int fontW = Math.max(56, s(78));
        int controlGap = Math.max(3, s(5));

        for (int i = 0; i < this.fields.length; i++) {
            int y = rowsTop + i * (rowH + rowGap);
            /*
             * Signalé : le numéro de sortie du D31d garde toujours son
             * apparence prévue (blanc) — inutile d'exposer un choix de
             * couleur pour ce champ précis, contrairement aux autres lignes
             * du même panneau. Vérifié par champ, pas seulement par
             * panneau : allowsPerFieldColor() reste le verrou global (D44).
             */
            boolean fieldColor = allowsColorForField(this.preset, i);
            int fieldW = fieldColor
                    ? innerW - colorW - fontW - controlGap * 2
                    : innerW - fontW - controlGap;
            this.fields[i] = new EditBox(
                    this.font,
                    innerX,
                    y,
                    fieldW,
                    fieldH,
                    Component.literal("Champ " + (i + 1))
            );
            this.fields[i].setMaxLength(MAX_TEXT_LENGTH);
            this.fields[i].setValue(this.initialTexts[i]);
            this.addRenderableWidget(this.fields[i]);
            this.fontRects[i] = new SignEditorUi.Rect(innerX + fieldW + controlGap, y, fontW, fieldH);
            this.colorRects[i] = fieldColor
                    ? new SignEditorUi.Rect(this.fontRects[i].x() + fontW + controlGap, y, colorW, fieldH)
                    : new SignEditorUi.Rect(0, 0, 0, 0);
        }

        /*
         * Signalé : la cartouche du "Panneau principal" doit apparaître
         * directement sous les champs propres au modèle plutôt qu'à une
         * rangée figée (4) — correcte seulement tant que le modèle a
         * exactement 4 champs (D63C), mais chevauchant les champs suivants
         * dès qu'un modèle en a plus (ex. D31d, 7 champs).
         */
        int contentCartoucheY = rowsTop + this.preset.getSlotCount() * (rowH + rowGap);
        this.contentCartoucheRect = new SignEditorUi.Rect(
                innerX, contentCartoucheY, innerW, fieldH
        );
        this.contentCartoucheField = new EditBox(
                this.font, innerX, contentCartoucheY + fieldH + rowGap,
                innerW, fieldH, Component.literal("Texte du cartouche du panneau")
        );
        this.contentCartoucheField.setMaxLength(24);
        this.contentCartoucheField.setHint(Component.literal("Texte du cartouche (ex. M 337)"));
        this.addRenderableWidget(this.contentCartoucheField);

        int customPageGap = Math.max(2, s(4));
        /*
         * Signalé : nombre d'onglets réellement visibles, en cohérence avec
         * customPageLabels dans drawCustomControls (même logique à 3
         * branches) — sinon un onglet supplémentaire, invisible mais quand
         * même cliquable/dessiné en blanc, resterait présent quand "Symbole"
         * ET "Cartouche" sont tous les deux absents (ex. D31d).
         */
        boolean allowsGraphicTab = allowsCustomGraphic(this.preset);
        boolean allowsCartoucheTabInit = allowsRegistryCartoucheTab(this.preset);
        int visibleCustomPages = allowsGraphicTab
                ? 4
                : (allowsCartoucheTabInit ? 3 : 2);
        int customPageW = (innerW - customPageGap * (visibleCustomPages - 1)) / visibleCustomPages;
        for (int index = 0; index < this.customPageRects.length; index++) {
            if (index >= visibleCustomPages) {
                /* Onglet masqué (ex. "Symbole" sur D31b) : case vide, jamais cliquable ni dessinée. */
                this.customPageRects[index] = new SignEditorUi.Rect(0, 0, 0, 0);
                continue;
            }
            int x = innerX + index * (customPageW + customPageGap);
            int width = index == visibleCustomPages - 1
                    ? innerX + innerW - x
                    : customPageW;
            this.customPageRects[index] = new SignEditorUi.Rect(x, rowsTop, width, selectorH);
        }
        int customControlsTop = rowsTop + selectorH + s(10);
        int customY = customControlsTop;
        int distanceW = Math.max(48, s(64));
        int customFieldW = Math.max(90, innerW - fontW - distanceW - controlGap * 2);
        this.customLine1Field = new EditBox(
                this.font, innerX, customY, customFieldW, fieldH, Component.literal("Ligne 1")
        );
        this.customLine1Field.setMaxLength(MAX_TEXT_LENGTH);
        this.customLine1Field.setHint(Component.literal("Destination 1"));
        this.addRenderableWidget(this.customLine1Field);
        this.customDistance1Field = new EditBox(
                this.font,
                innerX + customFieldW + controlGap,
                customY,
                distanceW,
                fieldH,
                Component.literal("Distance 1")
        );
        this.customDistance1Field.setMaxLength(8);
        this.customDistance1Field.setHint(Component.literal("Km"));
        this.addRenderableWidget(this.customDistance1Field);
        this.customLine1FontRect = new SignEditorUi.Rect(
                innerX + customFieldW + distanceW + controlGap * 2, customY, fontW, fieldH
        );

        customY += fieldH + rowGap;
        this.customLine2Field = new EditBox(
                this.font, innerX, customY, customFieldW, fieldH, Component.literal("Ligne 2")
        );
        this.customLine2Field.setMaxLength(MAX_TEXT_LENGTH);
        this.customLine2Field.setHint(Component.literal("Destination 2"));
        this.addRenderableWidget(this.customLine2Field);
        this.customDistance2Field = new EditBox(
                this.font,
                innerX + customFieldW + controlGap,
                customY,
                distanceW,
                fieldH,
                Component.literal("Distance 2")
        );
        this.customDistance2Field.setMaxLength(8);
        this.customDistance2Field.setHint(Component.literal("Km"));
        this.addRenderableWidget(this.customDistance2Field);
        this.customLine2FontRect = new SignEditorUi.Rect(
                innerX + customFieldW + distanceW + controlGap * 2, customY, fontW, fieldH
        );

        customY += fieldH + rowGap;
        this.customLine3Field = new EditBox(
                this.font, innerX, customY, customFieldW, fieldH, Component.literal("Ligne 3")
        );
        this.customLine3Field.setMaxLength(MAX_TEXT_LENGTH);
        this.customLine3Field.setHint(Component.literal("Destination 3"));
        this.addRenderableWidget(this.customLine3Field);
        this.customDistance3Field = new EditBox(
                this.font, innerX + customFieldW + controlGap, customY,
                distanceW, fieldH, Component.literal("Distance 3")
        );
        this.customDistance3Field.setMaxLength(8);
        this.customDistance3Field.setHint(Component.literal("Km"));
        this.addRenderableWidget(this.customDistance3Field);
        this.customLine3FontRect = new SignEditorUi.Rect(
                innerX + customFieldW + distanceW + controlGap * 2, customY, fontW, fieldH
        );

        customY += fieldH + rowGap;
        this.customLine4Field = new EditBox(
                this.font, innerX, customY, customFieldW, fieldH, Component.literal("Ligne 4")
        );
        this.customLine4Field.setMaxLength(MAX_TEXT_LENGTH);
        this.customLine4Field.setHint(Component.literal("Destination 4"));
        this.addRenderableWidget(this.customLine4Field);
        this.customDistance4Field = new EditBox(
                this.font, innerX + customFieldW + controlGap, customY,
                distanceW, fieldH, Component.literal("Distance 4")
        );
        this.customDistance4Field.setMaxLength(8);
        this.customDistance4Field.setHint(Component.literal("Km"));
        this.addRenderableWidget(this.customDistance4Field);
        this.customLine4FontRect = new SignEditorUi.Rect(
                innerX + customFieldW + distanceW + controlGap * 2, customY, fontW, fieldH
        );

        int colorGap = Math.max(3, s(6));
        int colorButtonW = (innerW - colorGap * 2) / 3;
        this.customWhiteRect = new SignEditorUi.Rect(
                innerX, customControlsTop, colorButtonW, fieldH
        );
        this.customGreenRect = new SignEditorUi.Rect(
                innerX + colorButtonW + colorGap, customControlsTop, colorButtonW, fieldH
        );
        this.customBlueRect = new SignEditorUi.Rect(
                this.customGreenRect.x() + colorButtonW + colorGap,
                customControlsTop,
                innerX + innerW - (this.customGreenRect.x() + colorButtonW + colorGap),
                fieldH
        );
        this.customGraphicRect = new SignEditorUi.Rect(
                innerX, customControlsTop + fieldH + rowGap, innerW, fieldH
        );

        this.customCartoucheRect = new SignEditorUi.Rect(
                innerX, customControlsTop, innerW, fieldH
        );
        this.customCartoucheField = new EditBox(
                this.font, innerX, customControlsTop + fieldH + rowGap,
                innerW, fieldH, Component.literal("Texte du cartouche")
        );
        this.customCartoucheField.setMaxLength(24);
        this.customCartoucheField.setHint(Component.literal("Texte du cartouche"));
        this.addRenderableWidget(this.customCartoucheField);

        /*
         * Sur la même page ("Format") que Blanc/Vert/Bleu juste au-dessus :
         * doit être sur sa propre rangée (comme customGraphicRect pour la
         * page "Symbole"), pas à la même position qu'eux sous peine de se
         * superposer aux 3 boutons de couleur.
         */
        this.customDoubleLineRect = new SignEditorUi.Rect(
                innerX, customControlsTop + fieldH + rowGap, innerW, fieldH
        );

        int actionH = SignEditorUi.safeControlHeight(this.font, s(28));
        int actionW = Math.max(82, Math.max(s(145), this.font.width("✓  Appliquer") + 18));
        int actionY = this.windowY + this.windowHeight - Math.max(s(36), actionH + 4);
        this.cancelRect = new SignEditorUi.Rect(this.windowX + this.windowWidth - pad - actionW, actionY, actionW, actionH);
        this.applyRect = new SignEditorUi.Rect(this.cancelRect.x() - s(10) - actionW, actionY, actionW, actionH);

        updateVisibleFields();
        loadCustomPanelIntoWidgets();
        loadContentCartoucheIntoWidgets();
        this.setInitialFocus(this.customMode ? this.customLine1Field : this.fields[0]);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, SignEditorUi.COLOR_OVERLAY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        SignEditorUi.drawModernWindow(
                graphics,
                this.font,
                this.windowX,
                this.windowY,
                this.windowWidth,
                this.windowHeight,
                presetBadge(this.preset),
                "Panneaux autoroutiers modulables",
                compactUi() ? "" : "1 seul bloc • modèles classés par fonction • réglages communs"
        );
        drawPreview(graphics);
        SignEditorUi.drawModernSection(
                graphics,
                this.font,
                this.contentRect,
                "CONFIGURATION DU PANNEAU",
                compactUi() ? "" : "Choisis un modèle de départ puis personnalise ses registres"
        );

        SignEditorUi.drawModernButton(graphics, this.font, this.presetRect,
                fitText("▦  " + this.preset.getDisplayName() + " — modèles / familles", this.presetRect.width() - 8),
                true, true, mouseX, mouseY);
        SignEditorUi.drawModernButton(graphics, this.font, this.resetRect, "Réinitialiser", false, true, mouseX, mouseY);
        if (allowsExtraPanels(this.preset)) {
            SignEditorUi.drawModernButton(
                    graphics, this.font, this.modeRect,
                    "Panneau principal", !this.customMode, true, mouseX, mouseY
            );
            MotorwaySignPanelData currentPanel = currentCustomPanelFromWidgets();
            for (int index = 0; index < this.customTabRects.length; index++) {
                MotorwaySignPanelData panel = this.customMode && index == this.selectedCustomPanel
                        ? currentPanel
                        : this.customPanels[index];
                SignEditorUi.drawModernButton(
                        graphics, this.font, this.customTabRects[index],
                        this.customTabRects[index].width() - this.customEnabledRects[index].width(),
                        "Registre " + (index + 1),
                        this.customMode && index == this.selectedCustomPanel,
                        true, mouseX, mouseY
                );
                SignEditorUi.drawModernButton(
                        graphics, this.font, this.customEnabledRects[index],
                        panel.enabled() ? "✓" : "×", panel.enabled(),
                        true, mouseX, mouseY
                );
            }
        } else if (showsServiceRow(this.preset)) {
            for (int index = 0; index < this.serviceRects.length; index++) {
                MotorwaySignServiceIcon icon = this.services[index];
                SignEditorUi.drawModernButton(
                        graphics, this.font, this.serviceRects[index],
                        fitText(icon.getDisplayName(), this.serviceRects[index].width() - 6),
                        icon.isVisible(), true, mouseX, mouseY
                );
            }
        } else {
            SignEditorUi.drawModernButton(
                    graphics, this.font, this.modeRect,
                    "Panneau principal", true, true, mouseX, mouseY
            );
        }

        if (this.customMode) {
            drawCustomControls(graphics, mouseX, mouseY);
        } else {
            for (int i = 0; i < this.preset.getSlotCount(); i++) {
                MotorwaySignSlot slot = this.preset.getSlot(i);
                if (!compactUi()) {
                    SignEditorUi.drawFieldLabel(
                            graphics, this.font, fitText(slotEditorLabel(slot), this.fields[i].getWidth()),
                            this.fields[i].getX(), this.fields[i].getY() - s(10)
                    );
                }
                RoadTextFont displayedFont = this.preset == MotorwaySignPreset.D32A
                        ? RoadTextFont.L4
                        : slot.role() == MotorwaySignRole.ROUTE
                        ? RoadTextFont.L1
                        : this.lineFonts[i];
                boolean fontEditable = slot.role() != MotorwaySignRole.ROUTE
                        && this.preset != MotorwaySignPreset.D32A;
                SignEditorUi.drawModernButton(
                        graphics, this.font, this.fontRects[i], SignEditorUi.fontLabel(displayedFont),
                        false, fontEditable, mouseX, mouseY
                );
                SignEditorUi.drawModernButton(
                        graphics, this.font, this.colorRects[i], this.lineColors[i].getDisplayName(),
                        false, true, mouseX, mouseY
                );
            }
            if (allowsMainCartoucheField(this.preset)) {
                MotorwaySignPanelData cartouche = contentCartouchePanelFromWidgets();
                String label = this.preset == MotorwaySignPreset.D63C
                        ? "Cartouche du panneau 1 : " + cartouche.cartoucheType().getDisplayName()
                        : "Cartouche : " + cartouche.cartoucheType().getDisplayName();
                SignEditorUi.drawModernButton(
                        graphics, this.font, this.contentCartoucheRect,
                        label,
                        cartouche.cartoucheType().isVisible(), true, mouseX, mouseY
                );
            }
        }

        SignEditorUi.drawModernButton(graphics, this.font, this.applyRect, "✓  Appliquer", true, true, mouseX, mouseY);
        SignEditorUi.drawModernButton(graphics, this.font, this.cancelRect, "×  Annuler", false, true, mouseX, mouseY);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawPreview(GuiGraphicsExtractor graphics) {
        if (this.customMode) {
            drawCustomPreview(graphics);
            return;
        }
        SignEditorUi.drawModernSection(
                graphics,
                this.font,
                this.previewRect,
                "APERÇU",
                compactUi() ? "" : this.preset.getDisplayName() + " • " + MotorwaySignCatalogInfo.usage(this.preset)
        );
        int pad = s(18);
        int x = this.previewRect.x() + pad;
        int y = this.previewRect.y() + pad + s(15);
        int w = this.previewRect.width() - pad * 2;
        int h = this.previewRect.height() - pad * 2 - s(8);
        graphics.fill(x, y, x + w, y + h, 0xFFF0F3F6);

        if (this.preset == MotorwaySignPreset.D44) {
            /*
             * L'algorithme générique ci-dessous suppose l'agencement des
             * autres modèles (pastille de sortie tout en haut, distance tout
             * en bas, registres au milieu) : il ne représente pas du tout le
             * D44 (sortie+distance sur un même registre en haut, nom en
             * dessous). Schéma dédié, cohérent avec D44_ARTWORK.
             */
            drawD44Preview(graphics, x, y, w, h);
            return;
        }

        if (this.preset == MotorwaySignPreset.D31D || this.preset == MotorwaySignPreset.D31E) {
            /*
             * Signalé : les registres vert et "destination locale" peuvent
             * désormais compter jusqu'à 4 villes chacun, avec un panneau qui
             * s'agrandit d'autant (voir MotorwaySignBlockEntityRenderer.
             * drawD31DStackText/StackPlate, réutilisés pour le D31e). Le
             * schéma générique ci-dessous (comme drawExactPreview) reprend
             * des tailles/positions figées mesurées sur le SVG à une seule
             * ville : il ne peut pas suivre ce changement de taille. Schéma
             * dédié, recalculé à chaque frappe.
             */
            drawD31DPreview(graphics, x, y, w, h);
            return;
        }

        MotorwaySignPreviewLayout exactLayout = MotorwaySignArtworkCatalog.previewLayoutFor(this.preset);
        if (exactLayout != null) {
            /*
             * Modèles à dessin exact (la plupart des ~62 préréglages) : la
             * disposition ROUTE-en-haut/DISTANCE-en-bas ci-dessous ne
             * correspond pas à leur registre réel (numéro de route encarté
             * dans un registre, distance regroupée avec un nom...). On
             * retrouve les vraies positions/tailles mesurées sur le SVG au
             * lieu de les redeviner.
             */
            drawExactPreview(graphics, exactLayout, x, y, w, h);
            return;
        }

        TreeMap<Integer, List<Integer>> groups = new TreeMap<>();
        List<Integer> routes = new ArrayList<>();
        int distance = -1;
        for (int i = 0; i < this.preset.getSlotCount(); i++) {
            MotorwaySignSlot slot = this.preset.getSlot(i);
            if (slot.role() == MotorwaySignRole.ROUTE) {
                routes.add(i);
            } else if (slot.role() == MotorwaySignRole.DISTANCE) {
                distance = i;
            } else {
                groups.computeIfAbsent(Math.max(0, slot.panelGroup()), ignored -> new ArrayList<>()).add(i);
            }
        }

        int signW = Math.min(w - s(32), Math.max(s(220), (int) (w * 0.76F)));
        int plateGap = Math.max(3, s(5));
        int lineStep = Math.max(13, s(17));
        int totalLines = groups.values().stream().mapToInt(List::size).sum();
        int mainHeight = Math.max(s(52), totalLines * lineStep + groups.size() * s(18));
        int extraHeight = (routes.isEmpty() ? 0 : s(34)) + (distance < 0 ? 0 : s(29));
        int signBottom = y + Math.min(h - s(45), Math.max(s(145), (h + mainHeight + extraHeight) / 2));
        int supportTop = signBottom - mainHeight - extraHeight;
        int poleColor = 0xFF2C2C2C;
        int centerX = x + w / 2;
        graphics.fill(centerX - s(5), supportTop, centerX + s(5), y + h - s(8), poleColor);
        graphics.fill(centerX - s(14), y + h - s(12), centerX + s(14), y + h - s(4), poleColor);

        int cursorY = signBottom;
        if (distance >= 0) {
            int ph = s(25);
            int pw = Math.min(signW / 2, Math.max(s(90), this.font.width(this.fields[distance].getValue()) + s(22)));
            cursorY -= ph;
            drawPreviewPlate(graphics, centerX - pw / 2, cursorY, pw, ph, this.lineColors[distance]);
            drawPreviewText(graphics, distance, centerX - pw / 2, cursorY, pw, ph);
            cursorY -= plateGap;
        }

        for (List<Integer> indices : groups.descendingMap().values()) {
            int ph = Math.max(s(39), indices.size() * lineStep + s(16));
            cursorY -= ph;
            MotorwaySignColor color = this.lineColors[indices.getFirst()];
            drawPreviewPlate(graphics, centerX - signW / 2, cursorY, signW, ph, color);
            int textY = cursorY + (ph - indices.size() * lineStep) / 2;
            for (int index : indices) {
                drawPreviewText(graphics, index, centerX - signW / 2, textY, signW, lineStep);
                textY += lineStep;
            }
            cursorY -= plateGap;
        }

        if (groups.isEmpty()) {
            int ph = s(48);
            cursorY -= ph;
            drawPreviewPlate(graphics, centerX - signW / 2, cursorY, signW, ph, MotorwaySignColor.WHITE);
            cursorY -= plateGap;
        }

        if (!routes.isEmpty()) {
            int ph = s(28);
            int pw = Math.max(s(68), (signW - plateGap * (routes.size() - 1)) / Math.max(2, routes.size()));
            int total = pw * routes.size() + plateGap * (routes.size() - 1);
            int routeX = centerX - total / 2;
            cursorY -= ph;
            for (int index : routes) {
                MotorwaySignSlot routeSlot = this.preset.getSlot(index);
                MotorwaySignColor routeColor = isRoadCartouchePreviewSlot(routeSlot)
                        ? MotorwaySignStyleProfile.visualRoadCartoucheColor(this.lineColors[index])
                        : this.lineColors[index];
                drawPreviewPlate(graphics, routeX, cursorY, pw, ph, routeColor);
                drawPreviewText(graphics, index, routeX, cursorY, pw, ph);
                routeX += pw + plateGap;
            }
        }
        if (this.preset == MotorwaySignPreset.D63C) {
            MotorwaySignPanelData cartouche = contentCartouchePanelFromWidgets();
            if (cartouche.cartoucheType().isVisible()) {
                int cartH = s(25);
                int cartW = Math.min(signW, Math.max(
                        s(70), this.font.width(cartouche.cartoucheText()) + s(24)
                ));
                int cartY = cursorY - plateGap - cartH;
                MotorwaySignColor color = cartouchePreviewColor(cartouche.cartoucheType());
                drawPreviewPlate(graphics, centerX - cartW / 2, cartY, cartW, cartH, color);
                drawCustomPreviewText(
                        graphics, cartouche.cartoucheText(), RoadTextFont.L1, color,
                        centerX - cartW / 2, cartY, cartW, cartH
                );
            }
        }
    }

    private void drawCustomControls(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        boolean allowsGraphic = allowsCustomGraphic(this.preset);
        if (!allowsGraphic && this.customSettingsPage >= 3) {
            /* Onglet "Symbole" masqué pour ce préréglage (ex. D31b) : jamais un état valide. */
            this.customSettingsPage = 0;
        }
        boolean allowsCartoucheTab = allowsRegistryCartoucheTab(this.preset);
        if (!allowsCartoucheTab && this.customSettingsPage == 2) {
            /* Cartouche de ce panonceau désormais choisie sur "Panneau principal" : jamais un état valide ici, quel que soit le registre. */
            this.customSettingsPage = 0;
        }
        MotorwaySignPanelData current = currentCustomPanelFromWidgets();
        /*
         * Signalé : sur ce panonceau, la cartouche se choisit désormais sur
         * "Panneau principal" pour les modèles qui l'exposent là-bas
         * (allowsMainCartoucheField) — l'onglet "Cartouche" n'a alors plus
         * lieu d'exister ici du tout. Attention cependant : "Cartouche" est
         * au milieu de la liste (index 2), et "Symbole" juste après (index
         * 3) — les retirer tous les deux ferait glisser "Symbole" à
         * l'index 2, alors que customSettingsPage == 2 route ailleurs dans
         * cette classe vers les widgets "Cartouche", pas "Symbole" (bug déjà
         * évité une fois, voir l'historique). On ne réduit donc la liste que
         * lorsque "Symbole" est ÉGALEMENT absent (ex. D31d) : dans tous les
         * autres cas, l'onglet "Cartouche" reste affiché (mais inerte, voir
         * plus haut) pour ne jamais décaler "Symbole".
         */
        String[] customPageLabels;
        if (allowsGraphic) {
            customPageLabels = new String[]{"Textes", "Format", "Cartouche", "Symbole"};
        } else if (allowsCartoucheTab) {
            customPageLabels = new String[]{"Textes", "Format", "Cartouche"};
        } else {
            customPageLabels = new String[]{"Textes", "Format"};
        }
        SignEditorUi.drawPageTabs(
                graphics, this.font, this.customPageRects,
                customPageLabels,
                this.customSettingsPage, mouseX, mouseY
        );
        switch (this.customSettingsPage) {
            case 0 -> {
                SignEditorUi.drawModernButton(
                        graphics, this.font, this.customLine1FontRect,
                        SignEditorUi.fontLabel(current.line1Font()), false, true, mouseX, mouseY
                );
                SignEditorUi.drawModernButton(
                        graphics, this.font, this.customLine2FontRect,
                        SignEditorUi.fontLabel(current.line2Font()), false,
                        current.lineCount() >= 2, mouseX, mouseY
                );
                SignEditorUi.drawModernButton(
                        graphics, this.font, this.customLine3FontRect,
                        SignEditorUi.fontLabel(current.line3Font()), false,
                        current.lineCount() >= 3, mouseX, mouseY
                );
                SignEditorUi.drawModernButton(
                        graphics, this.font, this.customLine4FontRect,
                        SignEditorUi.fontLabel(current.line4Font()), false,
                        current.lineCount() >= 4, mouseX, mouseY
                );
            }
            case 1 -> {
                SignEditorUi.drawModernButton(
                        graphics, this.font, this.customWhiteRect, "Blanc",
                        current.background() == MotorwaySignColor.WHITE, true, mouseX, mouseY
                );
                SignEditorUi.drawModernButton(
                        graphics, this.font, this.customGreenRect, "Vert",
                        current.background() == MotorwaySignColor.GREEN, true, mouseX, mouseY
                );
                SignEditorUi.drawModernButton(
                        graphics, this.font, this.customBlueRect, "Bleu",
                        current.background() == MotorwaySignColor.BLUE, true, mouseX, mouseY
                );
                SignEditorUi.drawModernButton(
                        graphics, this.font, this.customDoubleLineRect,
                        "Villes sur la pancarte : " + current.lineCount(),
                        current.lineCount() > 1, true, mouseX, mouseY
                );
            }
            case 2 -> {
                boolean cartoucheAllowed = this.selectedCustomPanel == 0
                        && allowsCustomCartouche(this.preset);
                SignEditorUi.drawModernButton(
                        graphics, this.font, this.customCartoucheRect,
                        cartoucheAllowed
                                ? "Cartouche : " + current.cartoucheType().getDisplayName()
                                : "Cartouche réservé au panneau 1",
                        cartoucheAllowed && current.cartoucheType().isVisible(),
                        cartoucheAllowed, mouseX, mouseY
                );
            }
            default -> SignEditorUi.drawModernButton(
                    graphics, this.font, this.customGraphicRect,
                    "Symbole : " + graphicLabel(current.graphic()),
                    current.graphic() != MotorwaySignGraphic.NONE, true, mouseX, mouseY
            );
        }
    }

    private void drawCustomPreview(GuiGraphicsExtractor graphics) {
        SignEditorUi.drawModernSection(
                graphics, this.font, this.previewRect, "APERÇU",
                compactUi() ? "" : "Ensemble complet • " + this.preset.getDisplayName()
        );
        int pad = s(18);
        int x = this.previewRect.x() + pad;
        int y = this.previewRect.y() + pad + s(15);
        int w = this.previewRect.width() - pad * 2;
        int h = this.previewRect.height() - pad * 2 - s(8);
        graphics.fill(x, y, x + w, y + h, 0xFFF0F3F6);

        MotorwaySignStyleProfile style = MotorwaySignStyleProfile.forPreset(this.preset);
        MotorwaySignPanelData[] panels = previewCustomPanels();
        List<MotorwaySignPanelData> enabled = new ArrayList<>();
        for (MotorwaySignPanelData panel : panels) {
            if (panel.enabled()
                    && !(panel.cartoucheType().isVisible() && !panel.hasPanelContent())) {
                enabled.add(withPreviewStyle(panel, style));
            }
        }
        int gap = Math.max(3, s(5));
        int panelW = Math.min(w - s(34), Math.max(s(210), Math.round(w * 0.78F)));
        int originalH = s(52);
        int totalH = originalH;
        for (MotorwaySignPanelData panel : enabled) {
            totalH += gap + previewPanelHeight(panel, style);
        }
        MotorwaySignPanelData firstPanel = panels.length > 0
                ? withPreviewStyle(panels[0], style)
                : null;
        boolean showTopCartouche = style.allowsCustomCartouche()
                && firstPanel != null && firstPanel.cartoucheType().isVisible();
        if (showTopCartouche) {
            totalH += s(29) + gap;
        }
        int cursorY = y + Math.max(s(18), (h - totalH) / 2);
        int centerX = x + w / 2;
        int poleTop = cursorY + totalH;
        graphics.fill(centerX - s(4), poleTop - s(4), centerX + s(4), y + h - s(7), 0xFF2C2C2C);

        if (showTopCartouche) {
            int cartH = s(29);
            int cartW = Math.min(panelW, Math.max(
                    s(70), this.font.width(firstPanel.cartoucheText()) + s(24)
            ));
            MotorwaySignColor color = cartouchePreviewColor(firstPanel.cartoucheType());
            drawPreviewPlate(graphics, centerX - cartW / 2, cursorY, cartW, cartH, color);
            drawCustomPreviewText(
                    graphics, firstPanel.cartoucheText(), RoadTextFont.L1, color,
                    centerX - cartW / 2, cursorY, cartW, cartH
            );
            cursorY += cartH + gap;
        }

        drawPreviewPlate(
                graphics, centerX - panelW / 2, cursorY,
                panelW, originalH, MotorwaySignColor.BLUE
        );
        drawCustomPreviewText(
                graphics, this.preset.getDisplayName(),
                RoadTextFont.L1, MotorwaySignColor.BLUE,
                centerX - panelW / 2, cursorY, panelW, originalH
        );
        cursorY += originalH;

        for (MotorwaySignPanelData panel : enabled) {
            cursorY += gap;
            int panelH = previewPanelHeight(panel, style);
            drawPreviewPlate(
                    graphics, centerX - panelW / 2, cursorY, panelW, panelH, panel.background()
            );
            int lineH = panelH / panel.lineCount();
            for (int lineIndex = 0; lineIndex < panel.lineCount(); lineIndex++) {
                drawCustomPreviewLine(
                        graphics,
                        panel.line(lineIndex),
                        style.allowsCustomDistances() ? panel.distance(lineIndex) : "",
                        panel.font(lineIndex), panel.background(),
                        centerX - panelW / 2, cursorY + lineIndex * lineH,
                        panelW, lineIndex == panel.lineCount() - 1
                                ? panelH - lineIndex * lineH
                                : lineH
                );
            }
            cursorY += panelH;
        }
    }

    private int previewPanelHeight(
            MotorwaySignPanelData panel,
            MotorwaySignStyleProfile style
    ) {
        float baseline = 0.48F + 0.40F * panel.lineCount();
        float profileHeight = style.addedPanelHeight(panel.lineCount(), panel.graphic());
        float ratio = baseline <= 0.001F ? 1.0F : profileHeight / baseline;
        return Math.max(s(39), Math.round(s(25 + panel.lineCount() * 19) * ratio));
    }

    private static MotorwaySignPanelData withPreviewStyle(
            MotorwaySignPanelData panel,
            MotorwaySignStyleProfile style
    ) {
        boolean keepDistances = style.allowsCustomDistances();
        return new MotorwaySignPanelData(
                panel.enabled(), panel.lineCount(),
                panel.line1(), panel.line2(), panel.line3(), panel.line4(),
                keepDistances ? panel.distance1() : "",
                keepDistances ? panel.distance2() : "",
                keepDistances ? panel.distance3() : "",
                keepDistances ? panel.distance4() : "",
                panel.line1Font(), panel.line2Font(), panel.line3Font(), panel.line4Font(),
                style.sanitizeCustomBackground(panel.background()),
                style.allowsCustomCartouche() ? panel.cartoucheType() : CartoucheType.NONE,
                style.allowsCustomCartouche() ? panel.cartoucheText() : "",
                style.forceBlueCustomPanels() ? MotorwaySignGraphic.NONE : panel.graphic()
        );
    }

    private static boolean isRoadCartouchePreviewSlot(MotorwaySignSlot slot) {
        return slot != null
                && slot.role() == MotorwaySignRole.ROUTE
                && !slot.label().toLowerCase(java.util.Locale.ROOT).contains("sortie");
    }

    private void drawCustomPreviewText(
            GuiGraphicsExtractor graphics,
            String value,
            RoadTextFont fontType,
            MotorwaySignColor color,
            int x,
            int y,
            int width,
            int height
    ) {
        Component component = Component.literal(fitText(value, width - s(14))).withStyle(
                Style.EMPTY.withFont(roadFontResource(fontType))
        );
        graphics.text(
                this.font, component,
                x + (width - this.font.width(component)) / 2,
                y + (height - this.font.lineHeight) / 2,
                color.getTextArgb(), false
        );
    }

    private void drawCustomPreviewLine(
            GuiGraphicsExtractor graphics,
            String value,
            String distance,
            RoadTextFont fontType,
            MotorwaySignColor color,
            int x,
            int y,
            int width,
            int height
    ) {
        Component distanceComponent = Component.literal(distance == null ? "" : distance).withStyle(
                Style.EMPTY.withFont(roadFontResource(fontType))
        );
        int distanceWidth = distance == null || distance.isBlank()
                ? 0
                : this.font.width(distanceComponent) + s(10);
        Component lineComponent = Component.literal(
                fitText(value, width - distanceWidth - s(18))
        ).withStyle(Style.EMPTY.withFont(roadFontResource(fontType)));
        int drawY = y + (height - this.font.lineHeight) / 2;
        int contentWidth = width - distanceWidth;
        graphics.text(
                this.font, lineComponent,
                x + s(9),
                drawY, color.getTextArgb(), false
        );
        if (distanceWidth > 0) {
            graphics.text(
                    this.font, distanceComponent,
                    x + width - this.font.width(distanceComponent) - s(7),
                    drawY, color.getTextArgb(), false
            );
        }
    }

    /**
     * Aperçu générique pour tout modèle à dessin exact (autre que D44, qui a
     * son propre schéma pour sa pastille/idéogramme/CE) : place chaque
     * registre et chaque texte à sa vraie position/taille relative mesurée
     * sur le SVG (voir MotorwaySignArtworkCatalog), à l'échelle de la zone
     * d'aperçu. Les graphismes (flèches, symboles) ne sont pas représentés :
     * seule la disposition des registres et du texte est reproduite.
     */
    private void drawExactPreview(
            GuiGraphicsExtractor graphics,
            MotorwaySignPreviewLayout layout,
            int x,
            int y,
            int w,
            int h
    ) {
        float scale = Math.min(w / layout.sourceWidth(), h / layout.sourceHeight());
        int signW = Math.round(layout.sourceWidth() * scale);
        int signH = Math.round(layout.sourceHeight() * scale);
        int left = x + (w - signW) / 2;
        int top = y + (h - signH) / 2;

        for (int index = 0; index < layout.bodyCount(); index++) {
            int bodyX = left + Math.round(layout.bodyX()[index] * scale);
            int bodyY = top + Math.round(layout.bodyY()[index] * scale);
            int bodyW = Math.round(layout.bodyWidth()[index] * scale);
            int bodyH = Math.round(layout.bodyHeight()[index] * scale);
            drawPreviewPlate(graphics, bodyX, bodyY, bodyW, bodyH, exactBodyPreviewColor(layout, index));
        }
        for (int index = 0; index < layout.textCount(); index++) {
            int slot = layout.textSlotIndex()[index];
            if (slot < 0 || slot >= this.preset.getSlotCount()) {
                continue;
            }
            int textW = Math.round(layout.textWidth()[index] * scale);
            int textH = Math.round(layout.textHeight()[index] * scale);
            int textX = left + Math.round(layout.textX()[index] * scale) - textW / 2;
            int textY = top + Math.round(layout.textY()[index] * scale) - textH / 2;
            drawPreviewText(graphics, slot, textX, textY, textW, textH);
        }
    }

    /**
     * Couleur du registre : priorité à une DESTINATION/INFO qui s'y trouve,
     * puis une DISTANCE, et seulement en dernier recours un simple cartouche
     * de numéro de route encarté dedans — un registre ne prend jamais le
     * jaune/rouge d'un cartouche de route s'il contient aussi une
     * destination, comme sur le vrai panneau. Mais si le registre ne
     * contient QUE le cartouche de route (rien d'autre dedans), c'est bien
     * sa couleur à lui qui doit s'afficher, pas un blanc par défaut — même
     * règle que le rendu 3D (voir exactBodyColor dans
     * MotorwaySignBlockEntityRenderer), que cette méthode reproduisait mal
     * en ignorant complètement les cartouches de route.
     */
    private MotorwaySignColor exactBodyPreviewColor(MotorwaySignPreviewLayout layout, int bodyIndex) {
        float top = layout.bodyY()[bodyIndex];
        float bottom = top + layout.bodyHeight()[bodyIndex];
        MotorwaySignColor best = null;
        int bestPriority = -1;
        for (int index = 0; index < layout.textCount(); index++) {
            float textY = layout.textY()[index];
            if (textY < top || textY > bottom) {
                continue;
            }
            int slot = layout.textSlotIndex()[index];
            if (slot < 0 || slot >= this.preset.getSlotCount()) {
                continue;
            }
            MotorwaySignRole role = this.preset.getSlot(slot).role();
            int priority = switch (role) {
                case DESTINATION, INFO -> 3;
                case DISTANCE -> 2;
                case ROUTE -> 1;
            };
            if (priority > bestPriority) {
                bestPriority = priority;
                best = this.lineColors[slot];
            }
        }
        return best != null ? best : MotorwaySignColor.WHITE;
    }

    /**
     * Schéma dédié au D44 : registre "sortie + distance" en haut, registre
     * "nom du village" en dessous, dans les mêmes proportions que
     * D44_ARTWORK (registres de 1826/4468 et 2455/4468 de la hauteur totale
     * des deux registres). Les panonceaux CE ne sont pas dessinés ici (pas
     * éditables), un rappel de trois cases suffit à situer leur emplacement.
     */
    private void drawD44Preview(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        int signW = Math.min(w - s(24), Math.max(s(200), (int) (w * 0.80F)));
        int totalHeight = Math.min(h - s(20), Math.max(s(120), (int) (h * 0.62F)));
        int register1Height = Math.max(s(30), Math.round(totalHeight * (1826.0F / 4281.0F)));
        int register2Height = totalHeight - register1Height;
        int centerX = x + w / 2;
        int poleColor = 0xFF2C2C2C;
        int top = y + s(14);

        drawPreviewPlate(graphics, centerX - signW / 2, top, signW, register1Height, this.lineColors[0]);
        /*
         * Rappel visuel de la pastille de sortie : un cadre (bordure + fond
         * clair), pas un carré plein — sinon le "12" dessiné juste après,
         * en noir, disparaît sur son propre fond noir (bug précédent).
         */
        int pillSize = register1Height - s(12);
        drawPreviewPlate(
                graphics,
                centerX - signW / 2 + s(8), top + s(6),
                pillSize, pillSize,
                this.lineColors[0]
        );
        drawPreviewText(graphics, 0, centerX - signW / 2 + s(8), top + s(6), pillSize, pillSize);
        drawPreviewText(graphics, 1, centerX, top, signW / 2 - s(8), register1Height);

        int register2Top = top + register1Height + Math.max(2, s(4));
        drawPreviewPlate(graphics, centerX - signW / 2, register2Top, signW, register2Height, this.lineColors[2]);
        drawPreviewText(graphics, 2, centerX - signW / 2, register2Top, signW, register2Height / 2);

        /* Panonceaux réellement choisis (voir le nouveau bandeau au-dessus des champs), pas un rappel fixe. */
        int visibleServices = 0;
        for (MotorwaySignServiceIcon icon : this.services) {
            if (icon.isVisible()) {
                visibleServices++;
            }
        }
        int rows = visibleServices > 3 ? 2 : 1;
        int servicesTop = register2Top + register2Height + Math.max(3, s(6));
        int serviceSize = Math.max(s(18), signW / 8);
        int serviceGap = Math.max(2, s(4));
        int servicesTotal = serviceSize * 3 + serviceGap * 2;
        int drawn = 0;
        for (MotorwaySignServiceIcon icon : this.services) {
            if (!icon.isVisible()) {
                continue;
            }
            int row = drawn / 3;
            int col = drawn % 3;
            int plateX = centerX - servicesTotal / 2 + col * (serviceSize + serviceGap);
            int plateY = servicesTop + row * (serviceSize + serviceGap);
            drawPreviewPlate(graphics, plateX, plateY, serviceSize, serviceSize, MotorwaySignColor.BLUE);
            graphics.blit(
                    Identifier.fromNamespaceAndPath(MoreRoad.MODID, "textures/block/" + icon.getTextureFile()),
                    plateX + s(3), plateY + s(3), plateX + serviceSize - s(3), plateY + serviceSize - s(3),
                    0.0F, 1.0F, 0.0F, 1.0F
            );
            drawn++;
        }

        int poleBottom = y + h - s(8);
        int poleTop = servicesTop + rows * serviceSize + Math.max(0, rows - 1) * serviceGap;
        graphics.fill(centerX - s(5), poleTop, centerX + s(5), poleBottom, poleColor);
        graphics.fill(centerX - s(14), poleBottom - s(8), centerX + s(14), poleBottom, poleColor);
    }

    /**
     * Schéma dédié au D31d et au D31e : registre "sortie"/"route" fixe en
     * haut, puis deux registres extensibles (vert : slots 1/2/3/4 ;
     * "destination locale" : slots 5/6/7 pour le D31d — 2 villes par
     * défaut, dessin d'origine — ou 5/6/7/8 pour le D31e — 1 ville par
     * défaut) dont la hauteur suit le nombre de villes réellement tapées
     * dans chacun — même logique de croissance/réduction que
     * MotorwaySignBlockEntityRenderer.drawD31DStackText/StackPlate, en 2D.
     */
    private void drawD31DPreview(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        int signW = Math.min(w - s(24), Math.max(s(200), (int) (w * 0.80F)));
        int centerX = x + w / 2;
        int poleColor = 0xFF2C2C2C;

        boolean isD31E = this.preset == MotorwaySignPreset.D31E;
        int[] localeSlots = isD31E ? new int[]{5, 6, 7, 8} : new int[]{5, 6, 7};
        int localeBaseline = isD31E ? 1 : 2;

        int lineStep = Math.max(14, s(18));
        int gap = Math.max(3, s(5));
        int exitHeight = Math.max(s(34), (int) (h * 0.24F));
        int greenCount = countPreviewStackLines(new int[]{1, 2, 3, 4}, 1);
        int localeCount = countPreviewStackLines(localeSlots, localeBaseline);
        int greenHeight = Math.max(s(30), greenCount * lineStep + s(14));
        int localeHeight = Math.max(s(30), localeCount * lineStep + s(14));
        int totalHeight = exitHeight + greenHeight + localeHeight + gap * 2;
        int available = h - s(16);
        if (totalHeight > available && totalHeight > 0) {
            float shrink = (float) available / totalHeight;
            exitHeight = Math.round(exitHeight * shrink);
            greenHeight = Math.round(greenHeight * shrink);
            localeHeight = Math.round(localeHeight * shrink);
            gap = Math.max(2, Math.round(gap * shrink));
            totalHeight = exitHeight + greenHeight + localeHeight + gap * 2;
        }
        int top = y + Math.max(0, (h - totalHeight) / 2);

        graphics.fill(centerX - s(5), top + totalHeight, centerX + s(5), y + h - s(4), poleColor);
        graphics.fill(centerX - s(14), y + h - s(12), centerX + s(14), y + h - s(4), poleColor);

        /*
         * Signalé : l'aperçu affichait toujours ce registre en blanc, même
         * quand sa couleur est réellement choisissable (D31e : cartouche de
         * numéro de route rouge/jaune/vert/bleu) — contrairement au numéro
         * de sortie du D31d, toujours blanc par réglementation.
         */
        drawPreviewPlate(graphics, centerX - signW / 2, top, signW, exitHeight,
                isD31E ? this.lineColors[0] : MotorwaySignColor.WHITE);
        drawPreviewText(graphics, 0, centerX - signW / 2, top, signW, exitHeight);

        int greenTop = top + exitHeight + gap;
        drawPreviewStackLines(graphics, new int[]{1, 2, 3, 4}, greenCount, centerX, signW, greenTop, greenHeight, lineStep);

        int localeTop = greenTop + greenHeight + gap;
        drawPreviewStackLines(graphics, localeSlots, localeCount, centerX, signW, localeTop, localeHeight, lineStep);
    }

    /**
     * Comme MotorwaySignBlockEntityRenderer.computeD31DStackInfo (compte les
     * villes remplies, en s'arrêtant à la première vide ; retombe sur le
     * nombre "naturel" si rien n'est encore rempli), mais lu directement
     * depuis les champs de saisie plutôt que depuis les données envoyées au
     * serveur — c'est ce que l'utilisateur tape à l'instant qui doit piloter
     * l'aperçu, avant même d'appliquer.
     */
    private int countPreviewStackLines(int[] slots, int baseline) {
        int count = 0;
        for (int slotIndex : slots) {
            if (this.fields[slotIndex].getValue().isBlank()) {
                break;
            }
            count++;
        }
        return count == 0 ? baseline : count;
    }

    private void drawPreviewStackLines(
            GuiGraphicsExtractor graphics, int[] slots, int count, int centerX, int signW, int top, int height, int lineStep
    ) {
        drawPreviewPlate(graphics, centerX - signW / 2, top, signW, height, this.lineColors[slots[0]]);
        int step = Math.max(1, Math.min(lineStep, (height - s(6)) / count));
        int textY = top + (height - count * step) / 2;
        for (int index = 0; index < count; index++) {
            drawPreviewText(graphics, slots[index], centerX - signW / 2, textY, signW, step);
            textY += step;
        }
    }

    private void drawPreviewPlate(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            MotorwaySignColor color
    ) {
        graphics.fill(x, y, x + width, y + height, previewBorderColor(color));
        int border = Math.max(2, s(3));
        graphics.fill(x + border, y + border, x + width - border, y + height - border, color.getArgb());
    }

    private void drawPreviewText(
            GuiGraphicsExtractor graphics,
            int index,
            int x,
            int y,
            int width,
            int height
    ) {
        String text = fitText(this.fields[index].getValue(), width - s(12));
        RoadTextFont previewFont = this.preset.getSlot(index).role() == MotorwaySignRole.ROUTE
                ? RoadTextFont.L1
                : this.lineFonts[index];
        Component component = Component.literal(text).withStyle(
                Style.EMPTY.withFont(roadFontResource(previewFont))
        );
        boolean leftAligned = (this.preset == MotorwaySignPreset.D63C && (index == 2 || index == 3))
                || (this.preset == MotorwaySignPreset.D44 && index == 2)
                || this.preset == MotorwaySignPreset.D32A;
        int drawX = leftAligned
                ? x + s(9)
                : x + (width - this.font.width(component)) / 2;
        int drawY = y + (height - this.font.lineHeight) / 2;
        if (this.preset == MotorwaySignPreset.D63C) {
            drawY += index == 3 ? s(2) : index == 2 ? s(1) : 0;
        }
        if ((this.preset == MotorwaySignPreset.D31B_EX1 || this.preset == MotorwaySignPreset.D31B_EX2)
                && this.preset.getSlot(index).role() == MotorwaySignRole.ROUTE) {
            /*
             * Même recalage optique que le rendu 3D (voir
             * MotorwaySignBlockEntityRenderer) : signalé trop haut dans son
             * cartouche (ex. "A 20"). En coordonnées d'écran du GUI (Y vers
             * le bas), corriger "trop haut" veut dire AUGMENTER Y, pas le
             * diminuer comme dans le renderer 3D (qui a son axe Y inversé).
             */
            drawY += Math.max(1, Math.round(this.font.lineHeight * 0.10F));
        }
        graphics.text(this.font, component, drawX, drawY, this.lineColors[index].getTextArgb(), false);
    }

    private static String slotEditorLabel(MotorwaySignSlot slot) {
        if (slot == null) {
            return "Champ";
        }
        return switch (slot.role()) {
            case ROUTE, DISTANCE -> "Base • " + slot.label();
            case DESTINATION, INFO -> slot.panelGroup() >= 0
                    ? "Panneau " + (slot.panelGroup() + 1) + " • " + slot.label()
                    : "Base • " + slot.label();
        };
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            if (allowsExtraPanels(this.preset)) {
                if (this.modeRect.contains(event.x(), event.y())) {
                    if (this.customMode) {
                        storeSelectedCustomPanel();
                    }
                    this.customMode = false;
                    loadContentCartoucheIntoWidgets();
                    updateVisibleFields();
                    this.setInitialFocus(this.fields[0]);
                    return true;
                }
                for (int index = 0; index < this.customTabRects.length; index++) {
                    if (this.customEnabledRects[index].contains(event.x(), event.y())) {
                        if (this.customMode && index == this.selectedCustomPanel) {
                            MotorwaySignPanelData current = currentCustomPanelFromWidgets();
                            setCurrentCustomPanel(copyPanel(
                                    current, !current.enabled(), null, null, null,
                                    null, null, null
                            ));
                        } else {
                            MotorwaySignPanelData panel = this.customPanels[index];
                            this.customPanels[index] = copyPanel(
                                    panel, !panel.enabled(), null, null, null,
                                    null, null, null
                            );
                        }
                        return true;
                    }
                    if (this.customTabRects[index].contains(event.x(), event.y())) {
                        if (this.customMode) {
                            storeSelectedCustomPanel();
                        } else {
                            storeContentCartouche();
                        }
                        this.selectedCustomPanel = index;
                        this.customMode = true;
                        loadCustomPanelIntoWidgets();
                        updateVisibleFields();
                        this.setInitialFocus(this.customLine1Field);
                        return true;
                    }
                }
            } else if (showsServiceRow(this.preset)) {
                for (int index = 0; index < this.serviceRects.length; index++) {
                    if (this.serviceRects[index].contains(event.x(), event.y())) {
                        this.services[index] = this.services[index].next();
                        return true;
                    }
                }
            } else if (this.modeRect.contains(event.x(), event.y())) {
                this.customMode = false;
                updateVisibleFields();
                this.setInitialFocus(this.fields[0]);
                return true;
            }
            if (this.customMode) {
                for (int index = 0; index < this.customPageRects.length; index++) {
                    if (this.customPageRects[index].contains(event.x(), event.y())) {
                        storeSelectedCustomPanel();
                        this.customSettingsPage = index;
                        updateVisibleFields();
                        return true;
                    }
                }
            }
            if (this.customMode && handleCustomClick(event.x(), event.y())) {
                return true;
            }
            if (!this.customMode && allowsMainCartoucheField(this.preset)
                    && this.contentCartoucheRect.contains(event.x(), event.y())) {
                storeContentCartouche();
                MotorwaySignPanelData panel = this.customPanels[0];
                this.customPanels[0] = withCartouche(
                        panel, panel.cartoucheType().next(), panel.cartoucheText()
                );
                loadContentCartoucheIntoWidgets();
                updateVisibleFields();
                return true;
            }
            if (this.presetRect.contains(event.x(), event.y())) {
                openPresetGallery();
                return true;
            }
            if (this.resetRect.contains(event.x(), event.y())) {
                applyPresetDefaults();
                return true;
            }
            for (int i = 0; !this.customMode && i < this.preset.getSlotCount(); i++) {
                if (this.fontRects[i].contains(event.x(), event.y())) {
                    /*
                     * Les numéros de route/sortie sont en L1 ; D32a est
                     * réglementairement en L4 et n'expose donc aucun cycle.
                     */
                    if (this.preset.getSlot(i).role() != MotorwaySignRole.ROUTE
                            && this.preset != MotorwaySignPreset.D32A) {
                        this.lineFonts[i] = RoadTextFont.nextForBackground(
                                this.lineFonts[i], !this.lineColors[i].isLight()
                        );
                    }
                    return true;
                }
                if (this.colorRects[i].contains(event.x(), event.y())) {
                    setGroupColor(i, nextSlotColor(i));
                    return true;
                }
            }
            if (this.applyRect.contains(event.x(), event.y())) {
                save();
                return true;
            }
            if (this.cancelRect.contains(event.x(), event.y())) {
                this.onClose();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void openPresetGallery() {
        if (this.customMode) {
            storeSelectedCustomPanel();
        } else {
            storeContentCartouche();
        }
        net.minecraft.client.Minecraft.getInstance().gui.setScreen(
                new MotorwayPresetGalleryScreen(
                        this,
                        this.blockPos,
                        this.preset,
                        this.customPanels
                )
        );
    }

    private void changePreset(MotorwaySignPreset newPreset) {
        if (newPreset == MotorwaySignPreset.D61B) {
            MotorwaySignLineData[] defaults = new MotorwaySignLineData[
                    MotorwaySignBlockEntity.MAX_SLOTS
            ];
            for (int index = 0; index < defaults.length; index++) {
                defaults[index] = index < newPreset.getSlotCount()
                        ? MotorwaySignLineData.blankForSlot(newPreset.getSlot(index))
                        : MotorwaySignLineData.empty();
            }
            MotorwaySignPanelData[] emptyPanels = new MotorwaySignPanelData[
                    MotorwaySignBlockEntity.MAX_CUSTOM_PANELS
            ];
            for (int index = 0; index < emptyPanels.length; index++) {
                emptyPanels[index] = MotorwaySignPanelData.disabled();
            }
            net.minecraft.client.Minecraft.getInstance().gui.setScreen(
                    new MotorwayD61BEditScreen(this.blockPos, defaults, emptyPanels)
            );
            return;
        }
        this.preset = newPreset;
        applyPresetDefaults();
        loadContentCartoucheIntoWidgets();
        updateVisibleFields();
    }

    private void applyPresetDefaults() {
        for (int i = 0; i < MotorwaySignBlockEntity.MAX_SLOTS; i++) {
            MotorwaySignLineData data = i < this.preset.getSlotCount()
                    ? MotorwaySignLineData.blankForSlot(this.preset.getSlot(i))
                    : MotorwaySignLineData.empty();
            this.fields[i].setValue(data.text());
            if (this.preset == MotorwaySignPreset.D32A && i < 2) {
                this.lineFonts[i] = RoadTextFont.L4;
                this.lineColors[i] = data.color() == MotorwaySignColor.BLUE
                        ? MotorwaySignColor.BLUE
                        : MotorwaySignColor.WHITE;
            } else {
                this.lineFonts[i] = data.font();
                this.lineColors[i] = data.color();
            }
        }
        MotorwaySignServiceIcon[] serviceDefaults = MotorwaySignServiceIcon.defaults();
        System.arraycopy(serviceDefaults, 0, this.services, 0, this.services.length);
    }

    private void updateVisibleFields() {
        for (int i = 0; i < this.fields.length; i++) {
            boolean visible = !this.customMode && i < this.preset.getSlotCount();
            this.fields[i].visible = visible;
            this.fields[i].active = visible;
        }
        if (this.customLine1Field != null) {
            MotorwaySignPanelData panel = currentCustomPanelFromWidgets();
            boolean textPage = this.customMode && this.customSettingsPage == 0;
            this.customLine1Field.visible = textPage;
            this.customLine1Field.active = textPage;
            boolean allowDistances = allowsCustomDistances(this.preset);
            this.customDistance1Field.visible = textPage && allowDistances;
            this.customDistance1Field.active = textPage && allowDistances;
            this.customLine2Field.visible = textPage && panel.doubleLine();
            this.customLine2Field.active = textPage && panel.doubleLine();
            this.customDistance2Field.visible = textPage && allowDistances && panel.doubleLine();
            this.customDistance2Field.active = textPage && allowDistances && panel.doubleLine();
            this.customLine3Field.visible = textPage && panel.lineCount() >= 3;
            this.customLine3Field.active = textPage && panel.lineCount() >= 3;
            this.customDistance3Field.visible = textPage && allowDistances && panel.lineCount() >= 3;
            this.customDistance3Field.active = textPage && allowDistances && panel.lineCount() >= 3;
            this.customLine4Field.visible = textPage && panel.lineCount() >= 4;
            this.customLine4Field.active = textPage && panel.lineCount() >= 4;
            this.customDistance4Field.visible = textPage && allowDistances && panel.lineCount() >= 4;
            this.customDistance4Field.active = textPage && allowDistances && panel.lineCount() >= 4;
            /*
             * Signalé : la cartouche du premier panonceau (seul à en porter
             * une réellement — voir MotorwaySignBlockEntityRenderer.
             * firstConfiguredPanel) doit se choisir sur "Panneau principal",
             * pas ici, dès que ce modèle expose ce choix là-bas
             * (allowsMainCartoucheField) — sinon les deux onglets
             * modifieraient la même donnée, source de confusion. S'applique
             * à tous les registres (1 à 4), pas seulement au premier : le
             * choix "Cartouche" n'a plus lieu d'être ici du tout pour ces
             * modèles.
             */
            boolean cartouchePage = this.customMode && this.customSettingsPage == 2
                    && allowsCustomCartouche(this.preset)
                    && !allowsMainCartoucheField(this.preset);
            this.customCartoucheField.visible = cartouchePage;
            this.customCartoucheField.active = cartouchePage && panel.cartoucheType().isVisible();
        }
        if (this.contentCartoucheField != null) {
            boolean visible = !this.customMode && allowsMainCartoucheField(this.preset);
            this.contentCartoucheField.visible = visible;
            this.contentCartoucheField.active = visible
                    && this.customPanels[0].cartoucheType().isVisible();
        }
    }

    private static boolean allowsCustomCartouche(MotorwaySignPreset preset) {
        return MotorwaySignStyleProfile.forPreset(preset).allowsCustomCartouche();
    }

    /**
     * Signalé : le choix de la cartouche d'un modèle "au dessin figé"
     * (ex. D31d) doit se faire directement sur l'onglet "Panneau
     * principal", pas via un "Registre N" qui n'a de toute façon aucun
     * effet propre (seul le premier panonceau, index 0, porte réellement
     * une cartouche — voir MotorwaySignBlockEntityRenderer.firstConfiguredPanel).
     * Ce champ (déjà utilisé par le D63C) est donc généralisé à tout
     * modèle qui autorise une cartouche personnalisée, sauf D61b et la
     * construction libre qui ont chacun leur propre mécanisme de cartouche
     * "au sommet" (voir buildD61BStackLayout / showTopCartouche).
     */
    private static boolean allowsMainCartoucheField(MotorwaySignPreset preset) {
        return allowsCustomCartouche(preset)
                && preset != MotorwaySignPreset.D61B
                && preset != MotorwaySignPreset.FREEFORM;
    }

    /**
     * Onglet « Cartouche » des registres supplémentaires. D31b ex.1/ex.2 et
     * D31e gèrent déjà leur numéro de route dans le panneau principal :
     * afficher un onglet grisé ici ne sert à rien et réduit la place utile.
     */
    private static boolean allowsRegistryCartoucheTab(MotorwaySignPreset preset) {
        if (preset == MotorwaySignPreset.D31B_EX1
                || preset == MotorwaySignPreset.D31B_EX2
                || preset == MotorwaySignPreset.D31E) {
            return false;
        }
        return !allowsMainCartoucheField(preset);
    }

    /** Le bandeau spécial de services appartient uniquement au D44. */
    private static boolean showsServiceRow(MotorwaySignPreset preset) {
        return preset == MotorwaySignPreset.D44;
    }

    /** Masque les onglets "Registre N" pour les panneaux au dessin figé (D44...) : voir MotorwaySignStyleProfile. */
    private static boolean allowsExtraPanels(MotorwaySignPreset preset) {
        return MotorwaySignStyleProfile.forPreset(preset).allowsExtraPanels();
    }

    /** Masque l'onglet "Symbole" par registre, inutile sur D31b (ex.1/ex.2) : voir MotorwaySignStyleProfile. */
    private static boolean allowsCustomGraphic(MotorwaySignPreset preset) {
        return MotorwaySignStyleProfile.forPreset(preset).allowsCustomGraphic();
    }

    /** Masque le choix de couleur par champ, inutile sur D44 (toujours blanc) : voir MotorwaySignStyleProfile. */
    private static boolean allowsPerFieldColor(MotorwaySignPreset preset) {
        return MotorwaySignStyleProfile.forPreset(preset).allowsPerFieldColor();
    }

    /**
     * Comme allowsPerFieldColor, mais au niveau du champ : le numéro de
     * sortie du D31d garde toujours son apparence prévue (blanc), sans que
     * ça retire le choix de couleur des autres lignes du même panneau. Les
     * deux villes vertes supplémentaires (index 2 et 3) partagent le même
     * registre que "Destination verte" (index 1) : leur couleur suit
     * toujours celle de cette première ligne, pas de choix indépendant qui
     * n'aurait pas de sens sur un seul registre physique.
     */
    private static boolean allowsColorForField(MotorwaySignPreset preset, int index) {
        if (!allowsPerFieldColor(preset)) {
            return false;
        }
        if (preset == MotorwaySignPreset.D31D) {
            return index != 0 && index != 2 && index != 3 && index != 4;
        }
        if (preset == MotorwaySignPreset.D31E) {
            /* Les villes vertes 2/3/4 partagent le registre de la première (index 1) : voir la remarque D31d ci-dessus. */
            return index != 2 && index != 3 && index != 4;
        }
        return true;
    }

    /**
     * Badge affiché dans l'en-tête : le nom du modèle actuellement chargé
     * (ex. "D31b" pour D31b — exemple 1), pas une étiquette générique figée
     * qui ne correspondait à aucun préréglage en particulier.
     */
    private static String presetBadge(MotorwaySignPreset preset) {
        if (preset == null) {
            return "D/DA";
        }
        String displayName = preset.getDisplayName();
        int dashIndex = displayName.indexOf(" — ");
        return dashIndex >= 0 ? displayName.substring(0, dashIndex) : displayName;
    }

    private static boolean allowsCustomDistances(MotorwaySignPreset preset) {
        return MotorwaySignStyleProfile.forPreset(preset).allowsCustomDistances();
    }

    private boolean handleCustomClick(double x, double y) {
        MotorwaySignPanelData current = currentCustomPanelFromWidgets();
        boolean darkBackground = !current.background().isLight();
        if (this.customSettingsPage == 0 && this.customLine1FontRect.contains(x, y)) {
            setCurrentCustomPanel(copyPanel(
                    current, null, null,
                    RoadTextFont.nextForBackground(current.line1Font(), darkBackground),
                    null, null, null, null
            ));
            return true;
        }
        if (this.customSettingsPage == 0 && current.doubleLine() && this.customLine2FontRect.contains(x, y)) {
            setCurrentCustomPanel(copyPanel(
                    current, null, null, null,
                    RoadTextFont.nextForBackground(current.line2Font(), darkBackground),
                    null, null, null
            ));
            return true;
        }
        if (this.customSettingsPage == 0 && current.lineCount() >= 3 && this.customLine3FontRect.contains(x, y)) {
            setCurrentCustomPanel(copyPanelWithFont(
                    current, 2, RoadTextFont.nextForBackground(current.line3Font(), darkBackground)
            ));
            return true;
        }
        if (this.customSettingsPage == 0 && current.lineCount() >= 4 && this.customLine4FontRect.contains(x, y)) {
            setCurrentCustomPanel(copyPanelWithFont(
                    current, 3, RoadTextFont.nextForBackground(current.line4Font(), darkBackground)
            ));
            return true;
        }
        if (this.customSettingsPage == 1 && this.customWhiteRect.contains(x, y)) {
            setCurrentCustomPanel(withCorrectedFonts(copyPanel(current, null, null, null, null,
                    MotorwaySignColor.WHITE, null, null)));
            return true;
        }
        if (this.customSettingsPage == 1 && this.customGreenRect.contains(x, y)) {
            setCurrentCustomPanel(withCorrectedFonts(copyPanel(current, null, null, null, null,
                    MotorwaySignColor.GREEN, null, null)));
            return true;
        }
        if (this.customSettingsPage == 1 && this.customBlueRect.contains(x, y)) {
            setCurrentCustomPanel(withCorrectedFonts(copyPanel(current, null, null, null, null,
                    MotorwaySignColor.BLUE, null, null)));
            return true;
        }
        if (this.customSettingsPage == 1 && this.customDoubleLineRect.contains(x, y)) {
            setCurrentCustomPanel(copyPanelWithLineCount(current, current.lineCount() % 4 + 1));
            updateVisibleFields();
            return true;
        }
        if (this.customSettingsPage == 2 && this.selectedCustomPanel == 0
                && allowsCustomCartouche(this.preset)
                && this.customCartoucheRect.contains(x, y)) {
            setCurrentCustomPanel(copyPanel(
                    current, null, null, null, null, null, current.cartoucheType().next(), null
            ));
            updateVisibleFields();
            return true;
        }
        if (this.customSettingsPage == 3 && this.customGraphicRect.contains(x, y)) {
            MotorwaySignGraphic[] graphics = MotorwaySignGraphic.values();
            MotorwaySignGraphic next = graphics[(current.graphic().ordinal() + 1) % graphics.length];
            setCurrentCustomPanel(copyPanel(current, null, null, null, null, null, null, next));
            return true;
        }
        return false;
    }

    private MotorwaySignPanelData copyPanel(
            MotorwaySignPanelData source,
            Boolean enabled,
            Boolean doubleLine,
            RoadTextFont line1Font,
            RoadTextFont line2Font,
            MotorwaySignColor background,
            CartoucheType cartoucheType,
            MotorwaySignGraphic graphic
    ) {
        return new MotorwaySignPanelData(
                enabled == null ? source.enabled() : enabled,
                doubleLine == null ? source.lineCount() : doubleLine ? 2 : 1,
                source.line1(), source.line2(), source.line3(), source.line4(),
                source.distance1(), source.distance2(), source.distance3(), source.distance4(),
                line1Font == null ? source.line1Font() : line1Font,
                line2Font == null ? source.line2Font() : line2Font,
                source.line3Font(), source.line4Font(),
                background == null ? source.background() : background,
                cartoucheType == null ? source.cartoucheType() : cartoucheType,
                source.cartoucheText(),
                graphic == null ? source.graphic() : graphic
        );
    }

    /*
     * Corrige les 4 polices d'un panneau custom d'après sa couleur de fond
     * actuelle (L1/L4 sur fond clair, L2 sur fond foncé) : appelé après un
     * changement de couleur, puisque copyPanel() ne touche que line1/2Font
     * et laisse line3/4Font inchangées.
     */
    private static MotorwaySignPanelData withCorrectedFonts(MotorwaySignPanelData panel) {
        MotorwaySignColor color = panel.background();
        RoadTextFont line1Font = forcedFontForColor(panel.line1Font(), color);
        RoadTextFont line2Font = forcedFontForColor(panel.line2Font(), color);
        RoadTextFont line3Font = forcedFontForColor(panel.line3Font(), color);
        RoadTextFont line4Font = forcedFontForColor(panel.line4Font(), color);
        if (line1Font == panel.line1Font() && line2Font == panel.line2Font()
                && line3Font == panel.line3Font() && line4Font == panel.line4Font()) {
            return panel;
        }
        return new MotorwaySignPanelData(
                panel.enabled(), panel.lineCount(),
                panel.line1(), panel.line2(), panel.line3(), panel.line4(),
                panel.distance1(), panel.distance2(), panel.distance3(), panel.distance4(),
                line1Font, line2Font, line3Font, line4Font,
                panel.background(), panel.cartoucheType(), panel.cartoucheText(), panel.graphic()
        );
    }

    private MotorwaySignPanelData copyPanelWithLineCount(MotorwaySignPanelData source, int lineCount) {
        return new MotorwaySignPanelData(
                source.enabled(), lineCount,
                source.line1(), source.line2(), source.line3(), source.line4(),
                source.distance1(), source.distance2(), source.distance3(), source.distance4(),
                source.line1Font(), source.line2Font(), source.line3Font(), source.line4Font(),
                source.background(), source.cartoucheType(), source.cartoucheText(), source.graphic()
        );
    }

    private MotorwaySignPanelData copyPanelWithFont(
            MotorwaySignPanelData source,
            int lineIndex,
            RoadTextFont font
    ) {
        RoadTextFont[] fonts = {
                source.line1Font(), source.line2Font(), source.line3Font(), source.line4Font()
        };
        fonts[Math.max(0, Math.min(3, lineIndex))] = font;
        return new MotorwaySignPanelData(
                source.enabled(), source.lineCount(),
                source.line1(), source.line2(), source.line3(), source.line4(),
                source.distance1(), source.distance2(), source.distance3(), source.distance4(),
                fonts[0], fonts[1], fonts[2], fonts[3],
                source.background(), source.cartoucheType(), source.cartoucheText(), source.graphic()
        );
    }

    private MotorwaySignPanelData currentCustomPanelFromWidgets() {
        MotorwaySignPanelData stored = this.customPanels[this.selectedCustomPanel];
        if (this.customLine1Field == null) {
            return stored;
        }
        boolean allowDistances = allowsCustomDistances(this.preset);
        return new MotorwaySignPanelData(
                stored.enabled(), stored.lineCount(),
                this.customLine1Field.getValue(), this.customLine2Field.getValue(),
                this.customLine3Field.getValue(), this.customLine4Field.getValue(),
                allowDistances ? this.customDistance1Field.getValue() : "",
                allowDistances ? this.customDistance2Field.getValue() : "",
                allowDistances ? this.customDistance3Field.getValue() : "",
                allowDistances ? this.customDistance4Field.getValue() : "",
                stored.line1Font(), stored.line2Font(), stored.line3Font(), stored.line4Font(),
                stored.background(),
                stored.cartoucheType(), this.customCartoucheField.getValue(), stored.graphic()
        );
    }

    private void setCurrentCustomPanel(MotorwaySignPanelData panel) {
        MotorwaySignPanelData normalized = withAllowedCustomBackground(panel);
        this.customPanels[this.selectedCustomPanel] = this.selectedCustomPanel == 0
                ? normalized
                : withoutCartouche(normalized);
        loadCustomPanelIntoWidgets();
    }

    private void storeSelectedCustomPanel() {
        if (this.customLine1Field != null) {
            this.customPanels[this.selectedCustomPanel] = currentCustomPanelFromWidgets();
        }
    }

    private MotorwaySignPanelData contentCartouchePanelFromWidgets() {
        MotorwaySignPanelData panel = this.customPanels[0];
        String text = this.contentCartoucheField == null
                ? panel.cartoucheText()
                : this.contentCartoucheField.getValue();
        return withCartouche(panel, panel.cartoucheType(), text);
    }

    private void storeContentCartouche() {
        if (this.contentCartoucheField != null && allowsMainCartoucheField(this.preset)) {
            this.customPanels[0] = contentCartouchePanelFromWidgets();
        }
    }

    private void loadContentCartoucheIntoWidgets() {
        if (this.contentCartoucheField != null) {
            this.contentCartoucheField.setValue(this.customPanels[0].cartoucheText());
        }
    }

    private static MotorwaySignPanelData withCartouche(
            MotorwaySignPanelData source,
            CartoucheType type,
            String text
    ) {
        return new MotorwaySignPanelData(
                source.enabled(), source.lineCount(),
                source.line1(), source.line2(), source.line3(), source.line4(),
                source.distance1(), source.distance2(), source.distance3(), source.distance4(),
                source.line1Font(), source.line2Font(), source.line3Font(), source.line4Font(),
                source.background(), type, text, source.graphic()
        );
    }

    private void loadCustomPanelIntoWidgets() {
        if (this.customLine1Field == null) {
            return;
        }
        MotorwaySignPanelData panel = this.customPanels[this.selectedCustomPanel];
        this.customLine1Field.setValue(panel.line1());
        this.customLine2Field.setValue(panel.line2());
        this.customLine3Field.setValue(panel.line3());
        this.customLine4Field.setValue(panel.line4());
        this.customDistance1Field.setValue(panel.distance1());
        this.customDistance2Field.setValue(panel.distance2());
        this.customDistance3Field.setValue(panel.distance3());
        this.customDistance4Field.setValue(panel.distance4());
        this.customCartoucheField.setValue(panel.cartoucheText());
        updateVisibleFields();
    }

    private MotorwaySignPanelData[] previewCustomPanels() {
        MotorwaySignPanelData[] result = this.customPanels.clone();
        result[this.selectedCustomPanel] = currentCustomPanelFromWidgets();
        return result;
    }

    private static MotorwaySignColor cartouchePreviewColor(CartoucheType type) {
        return switch (type) {
            case E41_45 -> MotorwaySignColor.GREEN;
            case E42 -> MotorwaySignColor.RED;
            case E43 -> MotorwaySignColor.YELLOW;
            case E44 -> MotorwaySignColor.WHITE;
            case E47 -> MotorwaySignColor.METROPOLITAN_BLUE;
            default -> MotorwaySignColor.BLUE;
        };
    }

    private MotorwaySignColor nextSlotColor(int index) {
        MotorwaySignSlot slot = this.preset.getSlot(index);
        if (slot.role() == MotorwaySignRole.ROUTE || slot.role() == MotorwaySignRole.DISTANCE) {
            return this.lineColors[index].next();
        }
        if (this.preset == MotorwaySignPreset.D32A) {
            return this.lineColors[index] == MotorwaySignColor.BLUE
                    ? MotorwaySignColor.WHITE
                    : MotorwaySignColor.BLUE;
        }
        MotorwaySignColor allowed = allowedMainPanelColor(this.lineColors[index]);
        return switch (allowed) {
            case WHITE -> MotorwaySignColor.BLUE;
            case BLUE -> MotorwaySignColor.GREEN;
            default -> MotorwaySignColor.WHITE;
        };
    }

    private static MotorwaySignColor allowedMainPanelColor(MotorwaySignColor color) {
        return color == MotorwaySignColor.GREEN || color == MotorwaySignColor.BLUE
                ? color
                : MotorwaySignColor.WHITE;
    }

    private static MotorwaySignColor allowedCustomBackground(MotorwaySignColor background) {
        return background == MotorwaySignColor.GREEN || background == MotorwaySignColor.WHITE
                ? background
                : MotorwaySignColor.BLUE;
    }

    private static int previewBorderColor(MotorwaySignColor color) {
        return switch (color) {
            case BLUE, GREEN -> MotorwaySignColor.WHITE.getArgb();
            case WHITE -> MotorwaySignColor.BLACK.getArgb();
            default -> 0xFFD7D7D2;
        };
    }

    private static MotorwaySignPanelData withAllowedCustomBackground(MotorwaySignPanelData panel) {
        MotorwaySignColor background = allowedCustomBackground(panel.background());
        if (background == panel.background()) {
            return panel;
        }
        return new MotorwaySignPanelData(
                panel.enabled(), panel.lineCount(),
                panel.line1(), panel.line2(), panel.line3(), panel.line4(),
                panel.distance1(), panel.distance2(), panel.distance3(), panel.distance4(),
                panel.line1Font(), panel.line2Font(), panel.line3Font(), panel.line4Font(),
                background, panel.cartoucheType(), panel.cartoucheText(), panel.graphic()
        );
    }

    private static MotorwaySignPanelData withoutCartouche(MotorwaySignPanelData panel) {
        if (!panel.cartoucheType().isVisible() && panel.cartoucheText().isBlank()) {
            return panel;
        }
        return new MotorwaySignPanelData(
                panel.enabled(), panel.lineCount(),
                panel.line1(), panel.line2(), panel.line3(), panel.line4(),
                panel.distance1(), panel.distance2(), panel.distance3(), panel.distance4(),
                panel.line1Font(), panel.line2Font(), panel.line3Font(), panel.line4Font(),
                panel.background(), CartoucheType.NONE, "", panel.graphic()
        );
    }

    private static String graphicLabel(MotorwaySignGraphic graphic) {
        return switch (graphic) {
            case NONE -> "Aucun";
            case DIAGONAL_LEFT -> "Flèche gauche";
            case DIAGONAL_RIGHT -> "Flèche droite";
            case DOWN -> "Flèche basse";
            case DOWN_DOUBLE -> "Deux flèches";
            case EXIT -> "Sortie";
            case EXIT_LIST -> "Liste sorties";
            case SCHEMATIC_LEFT -> "Schéma gauche";
            case SCHEMATIC_RIGHT -> "Schéma droit";
            case JUNCTION -> "Bifurcation";
            case MOTORWAY -> "Autoroute";
            case SERVICES -> "Services";
        };
    }

    private void setGroupColor(int index, MotorwaySignColor color) {
        MotorwaySignSlot clicked = this.preset.getSlot(index);
        MotorwaySignColor effectiveColor = this.preset == MotorwaySignPreset.D32A
                ? (color == MotorwaySignColor.BLUE ? MotorwaySignColor.BLUE : MotorwaySignColor.WHITE)
                : color;
        this.lineColors[index] = effectiveColor;
        this.lineFonts[index] = this.preset == MotorwaySignPreset.D32A
                ? RoadTextFont.L4
                : forcedFontForColor(this.lineFonts[index], effectiveColor);
        if (clicked.role() == MotorwaySignRole.ROUTE || clicked.role() == MotorwaySignRole.DISTANCE) {
            return;
        }
        for (int i = 0; i < this.preset.getSlotCount(); i++) {
            MotorwaySignSlot candidate = this.preset.getSlot(i);
            if (candidate.role() != MotorwaySignRole.ROUTE
                    && candidate.role() != MotorwaySignRole.DISTANCE
                    && candidate.panelGroup() == clicked.panelGroup()) {
                this.lineColors[i] = effectiveColor;
                this.lineFonts[i] = this.preset == MotorwaySignPreset.D32A
                        ? RoadTextFont.L4
                        : forcedFontForColor(this.lineFonts[i], effectiveColor);
            }
        }
    }

    /*
     * La police n'est pas indépendante de la couleur du fond : L1/L4
     * dessinent un texte sombre prévu pour un fond clair, L2 dessine un
     * vrai texte blanc prévu pour un fond foncé (bleu, vert, rouge, noir,
     * marron, bleu métropolitain...). On corrige donc la police à chaque
     * changement de couleur, comme sur D21A/D61A.
     */
    private static RoadTextFont forcedFontForColor(RoadTextFont font, MotorwaySignColor color) {
        return color.isLight()
                ? RoadTextFont.forceForLightBackground(font)
                : RoadTextFont.forceForDarkBackground(font);
    }

    private void save() {
        if (this.customMode) {
            storeSelectedCustomPanel();
        } else {
            storeContentCartouche();
        }
        MotorwaySignLineData[] data = new MotorwaySignLineData[MotorwaySignBlockEntity.MAX_SLOTS];
        for (int i = 0; i < data.length; i++) {
            RoadTextFont font = this.preset == MotorwaySignPreset.D32A && i < 2
                    ? RoadTextFont.L4
                    : this.lineFonts[i];
            MotorwaySignColor color = this.preset == MotorwaySignPreset.D32A && i < 2
                    ? (this.lineColors[i] == MotorwaySignColor.BLUE
                    ? MotorwaySignColor.BLUE
                    : MotorwaySignColor.WHITE)
                    : this.lineColors[i];
            data[i] = new MotorwaySignLineData(this.fields[i].getValue(), font, color);
        }
        ClientPacketDistributor.sendToServer(new UpdateMotorwaySignPayload(
                this.blockPos,
                this.preset.getSerializedName(),
                data,
                false,
                this.customPanels,
                this.services
        ));
        this.onClose();
    }

    private boolean compactUi() {
        return SignEditorUi.compactForScale(this.scale);
    }

    private int s(int value) {
        return SignEditorUi.scaledUi(value, this.scale);
    }

    private String fitText(String value, int maxWidth) {
        return SignEditorUi.fitText(this.font, value == null ? "" : value, Math.max(8, maxWidth));
    }
}
