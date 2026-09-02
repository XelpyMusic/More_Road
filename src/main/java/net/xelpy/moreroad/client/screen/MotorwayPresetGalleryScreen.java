package net.xelpy.moreroad.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.xelpy.moreroad.MoreRoad;
import net.xelpy.moreroad.block.custom.MotorwaySignCatalogInfo;
import net.xelpy.moreroad.block.custom.MotorwaySignLineData;
import net.xelpy.moreroad.block.custom.MotorwaySignPanelData;
import net.xelpy.moreroad.block.custom.MotorwaySignPreset;
import net.xelpy.moreroad.block.custom.MotorwaySignServiceIcon;
import net.xelpy.moreroad.block.entity.MotorwaySignBlockEntity;
import net.xelpy.moreroad.client.MotorwaySignClientHooks;

import java.util.ArrayList;
import java.util.List;

/**
 * Galerie visuelle commune à tous les panneaux D / DA.
 *
 * La galerie classe les modèles par rôle fonctionnel (libre, avancée,
 * présignalisation, confirmation, affectation de voies…) afin de retrouver
 * rapidement la bonne famille sans parcourir une liste brute de références.
 * Les miniatures (640x360, lissées via leur sidecar .png.mcmeta) sont
 * dessinées par un blit GPU direct, sans décodage côté CPU.
 */
public final class MotorwayPresetGalleryScreen extends Screen {

    private static final float GALLERY_SOURCE_WIDTH = 640.0F;
    private static final float GALLERY_SOURCE_HEIGHT = 360.0F;

    private enum GalleryCategory {
        CUSTOM(MotorwaySignCatalogInfo.Family.CUSTOM),
        ADVANCED(MotorwaySignCatalogInfo.Family.ADVANCED),
        PRESIGNAL(MotorwaySignCatalogInfo.Family.PRESIGNAL),
        CONFIRMATION(MotorwaySignCatalogInfo.Family.CONFIRMATION),
        COMPLEMENT(MotorwaySignCatalogInfo.Family.COMPLEMENT),
        LANE_ADVANCED(MotorwaySignCatalogInfo.Family.LANE_ADVANCED),
        LANE_PRESIGNAL(MotorwaySignCatalogInfo.Family.LANE_PRESIGNAL);

        private final MotorwaySignCatalogInfo.Family family;
        private final String label;

        GalleryCategory(MotorwaySignCatalogInfo.Family family) {
            this.family = family;
            this.label = family.label();
        }

        boolean accepts(MotorwaySignPreset preset) {
            return MotorwaySignCatalogInfo.family(preset) == this.family;
        }

        String description() {
            return this.family.description();
        }

        static GalleryCategory forPreset(MotorwaySignPreset preset) {
            MotorwaySignCatalogInfo.Family family = MotorwaySignCatalogInfo.family(preset);
            for (GalleryCategory category : values()) {
                if (category.family == family) {
                    return category;
                }
            }
            return CONFIRMATION;
        }
    }

    private final Screen returnScreen;
    private final BlockPos blockPos;
    private final MotorwaySignPreset currentPreset;
    private final MotorwaySignPanelData[] customPanels;
    private final SignEditorUi.Rect[] cards =
            new SignEditorUi.Rect[MotorwaySignPreset.values().length];
    private final SignEditorUi.Rect[] categoryRects =
            new SignEditorUi.Rect[GalleryCategory.values().length];

    private GalleryCategory category;
    private SignEditorUi.Rect cancelRect;
    private int windowX;
    private int windowY;
    private int windowWidth;
    private int windowHeight;

    public MotorwayPresetGalleryScreen(
            Screen returnScreen,
            BlockPos blockPos,
            MotorwaySignPreset currentPreset,
            MotorwaySignPanelData[] customPanels
    ) {
        super(Component.literal("Choisir un modèle autoroutier"));
        this.returnScreen = returnScreen;
        this.blockPos = blockPos.immutable();
        this.currentPreset = currentPreset == null
                ? MotorwaySignPreset.FREEFORM
                : currentPreset;
        this.customPanels = copyPanels(customPanels);
        this.category = GalleryCategory.forPreset(this.currentPreset);
    }

    @Override
    protected void init() {
        super.init();

        int marginX = Math.max(10, Math.min(30, this.width / 36));
        int marginY = Math.max(8, Math.min(20, this.height / 38));
        this.windowX = marginX;
        this.windowY = marginY;
        this.windowWidth = this.width - marginX * 2;
        this.windowHeight = this.height - marginY * 2;

        layoutGallery();
    }

    private void layoutGallery() {
        for (int i = 0; i < this.cards.length; i++) {
            this.cards[i] = null;
        }

        int headerH = 58;
        int footerH = 48;
        int categoryH = 28;
        int pad = 14;
        int gap = 8;

        int categoryX = this.windowX + pad;
        int categoryY = this.windowY + headerH;
        int categoryW = this.windowWidth - pad * 2;
        int tabGap = 7;
        int tabW = (categoryW - tabGap * (this.categoryRects.length - 1))
                / this.categoryRects.length;

        for (int i = 0; i < this.categoryRects.length; i++) {
            int x = categoryX + i * (tabW + tabGap);
            int width = i == this.categoryRects.length - 1
                    ? categoryX + categoryW - x
                    : tabW;
            this.categoryRects[i] = new SignEditorUi.Rect(
                    x, categoryY, width, categoryH
            );
        }

        int bodyX = this.windowX + pad;
        int bodyY = categoryY + categoryH + 10;
        int bodyW = this.windowWidth - pad * 2;
        int bodyBottom = this.windowY + this.windowHeight - footerH;
        int bodyH = Math.max(100, bodyBottom - bodyY - 4);

        List<Integer> visibleIndices = new ArrayList<>();
        MotorwaySignPreset[] presets = MotorwaySignPreset.values();
        for (int i = 0; i < presets.length; i++) {
            if (isVisibleInGallery(presets[i]) && this.category.accepts(presets[i])) {
                visibleIndices.add(i);
            }
        }

        /*
         * De grandes cartes, mais toujours entièrement contenues dans l'écran.
         * 5 colonnes sur les écrans larges, 4 en moyen, 3 en compact.
         */
        int cols;
        if (bodyW >= 1450) {
            cols = 5;
        } else if (bodyW >= 1050) {
            cols = 4;
        } else {
            cols = 3;
        }

        /*
         * Si l'écran est peu haut, on augmente automatiquement le nombre de
         * colonnes plutôt que de laisser la dernière rangée sortir du cadre.
         */
        int minimumCardH = 54;
        int maxRowsThatFit = Math.max(
                1,
                (bodyH + gap) / (minimumCardH + gap)
        );
        int colsNeededForHeight = Math.max(
                1,
                (visibleIndices.size() + maxRowsThatFit - 1) / maxRowsThatFit
        );
        cols = Math.max(cols, colsNeededForHeight);
        cols = Math.max(1, Math.min(cols, visibleIndices.size()));

        int rows = Math.max(1, (visibleIndices.size() + cols - 1) / cols);
        int cardW = Math.max(72, (bodyW - gap * (cols - 1)) / cols);
        int cardH = Math.max(1, (bodyH - gap * (rows - 1)) / rows);

        for (int visible = 0; visible < visibleIndices.size(); visible++) {
            int index = visibleIndices.get(visible);
            int row = visible / cols;
            int col = visible % cols;
            int x = bodyX + col * (cardW + gap);
            int y = bodyY + row * (cardH + gap);
            int width = col == cols - 1
                    ? bodyX + bodyW - x
                    : cardW;
            int height = row == rows - 1
                    ? Math.max(54, bodyY + bodyH - y)
                    : cardH;

            this.cards[index] = new SignEditorUi.Rect(
                    x,
                    y,
                    Math.max(80, width),
                    Math.max(54, height)
            );
        }

        int cancelW = Math.max(120, this.font.width("Retour") + 38);
        this.cancelRect = new SignEditorUi.Rect(
                this.windowX + this.windowWidth - pad - cancelW,
                this.windowY + this.windowHeight - 37,
                cancelW,
                27
        );
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        graphics.fill(0, 0, this.width, this.height, SignEditorUi.COLOR_OVERLAY);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        int visibleCount = 0;
        for (MotorwaySignPreset preset : MotorwaySignPreset.values()) {
            if (this.category.accepts(preset)) {
                visibleCount++;
            }
        }

        SignEditorUi.drawModernWindow(
                graphics,
                this.font,
                this.windowX,
                this.windowY,
                this.windowWidth,
                this.windowHeight,
                "D/DA",
                "Choisir le rôle puis le modèle",
                this.category.description() + " • " + visibleCount + " modèles"
        );

        GalleryCategory[] categories = GalleryCategory.values();
        for (int i = 0; i < categories.length; i++) {
            SignEditorUi.drawModernButton(
                    graphics,
                    this.font,
                    this.categoryRects[i],
                    categories[i].label,
                    categories[i] == this.category,
                    true,
                    mouseX,
                    mouseY
            );
        }

        MotorwaySignPreset[] presets = MotorwaySignPreset.values();
        for (int index = 0; index < presets.length; index++) {
            SignEditorUi.Rect rect = this.cards[index];
            if (rect == null) {
                continue;
            }
            drawPresetCard(
                    graphics,
                    rect,
                    presets[index],
                    mouseX,
                    mouseY
            );
        }

        SignEditorUi.drawModernButton(
                graphics,
                this.font,
                this.cancelRect,
                "←  Retour",
                false,
                true,
                mouseX,
                mouseY
        );
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawPresetCard(
            GuiGraphicsExtractor graphics,
            SignEditorUi.Rect rect,
            MotorwaySignPreset preset,
            int mouseX,
            int mouseY
    ) {
        boolean selected = preset == this.currentPreset;
        boolean hovered = rect.contains(mouseX, mouseY);
        int border = selected
                ? 0xFF2E8BFF
                : hovered ? 0xFF71849A : 0xFF344354;
        int fill = selected
                ? 0xFF183650
                : hovered ? 0xFF273442 : 0xFF202A35;

        graphics.fill(
                rect.x(), rect.y(),
                rect.x() + rect.width(), rect.y() + rect.height(),
                border
        );
        graphics.fill(
                rect.x() + 1, rect.y() + 1,
                rect.x() + rect.width() - 1, rect.y() + rect.height() - 1,
                fill
        );

        String title = SignEditorUi.fitText(
                this.font,
                preset.getDisplayName(),
                rect.width() - 12
        );
        graphics.text(
                this.font,
                Component.literal(title),
                rect.x() + (rect.width() - this.font.width(title)) / 2,
                rect.y() + 5,
                0xFFF4F6F8,
                false
        );

        int previewX = rect.x() + 8;
        int previewY = rect.y() + 20;
        int previewW = Math.max(24, rect.width() - 16);
        int previewH = Math.max(16, rect.height() - 39);
        drawArtworkPreview(
                graphics,
                preset,
                previewX,
                previewY,
                previewW,
                previewH
        );
        String usage = MotorwaySignCatalogInfo.usesSpecialArtwork(preset)
                ? "Rendu spécial"
                : MotorwaySignCatalogInfo.family(preset).label();
        String footer = SignEditorUi.fitText(this.font, usage, rect.width() - 12);
        graphics.text(
                this.font,
                Component.literal(footer),
                rect.x() + (rect.width() - this.font.width(footer)) / 2,
                rect.y() + rect.height() - this.font.lineHeight - 4,
                0xFFAEBBC8,
                false
        );
    }

    private void drawArtworkPreview(
            GuiGraphicsExtractor graphics,
            MotorwaySignPreset preset,
            int x,
            int y,
            int width,
            int height
    ) {
        if (preset == MotorwaySignPreset.FREEFORM) {
            int panelW = Math.max(20, Math.min(width - 12, Math.round(width * 0.76F)));
            int panelH = Math.max(12, Math.min(height - 8, Math.round(height * 0.46F)));
            int panelX = x + (width - panelW) / 2;
            int panelY = y + Math.max(2, (height - panelH) / 2);
            graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xFFD7D7D2);
            int border = Math.max(2, Math.min(4, panelH / 7));
            graphics.fill(
                    panelX + border, panelY + border,
                    panelX + panelW - border, panelY + panelH - border,
                    0xFF0000FF
            );
            return;
        }
        Identifier textureId = galleryTextureId(preset);
        if (!textureExists(textureId)) {
            graphics.centeredText(
                    this.font,
                    Component.literal(preset.getDisplayName()),
                    x + width / 2,
                    y + Math.max(0, height / 2 - this.font.lineHeight / 2),
                    0xFFD5DDE6
            );
            return;
        }

        /*
         * Blit GPU direct plutôt qu'un décodage ImageIO + redimensionnement
         * Java2D + report pixel par pixel via des centaines de fill() : ça
         * évitait de dépendre d'un atlas de texture, mais c'était lent (des
         * dizaines de miniatures à chaque ouverture/défilement de la galerie)
         * et flou/pixelisé une fois réagrandi par l'échelle d'interface. Le
         * lissage vient maintenant du sidecar "<fichier>.png.mcmeta"
         * (blur: true) de chaque miniature, comme pour les polices du mod.
         */
        float aspect = GALLERY_SOURCE_WIDTH / GALLERY_SOURCE_HEIGHT;
        int fitWidth = width;
        int fitHeight = Math.round(width / aspect);
        if (fitHeight > height) {
            fitHeight = height;
            fitWidth = Math.round(height * aspect);
        }
        int startX = x + (width - fitWidth) / 2;
        int startY = y + (height - fitHeight) / 2;
        graphics.blit(textureId, startX, startY, startX + fitWidth, startY + fitHeight, 0.0F, 1.0F, 0.0F, 1.0F);
    }

    private static boolean isVisibleInGallery(MotorwaySignPreset preset) {
        return preset != MotorwaySignPreset.D45
                && preset != MotorwaySignPreset.D45_DC;
    }

    private static Identifier galleryTextureId(MotorwaySignPreset preset) {
        return Identifier.fromNamespaceAndPath(
                MoreRoad.MODID,
                "textures/gui/motorway_gallery/" + preset.getSerializedName() + ".png"
        );
    }

    /** Vérifie juste l'existence de la ressource : pas de décodage, contrairement à l'ancien chemin ImageIO. */
    private static boolean textureExists(Identifier textureId) {
        try {
            return Minecraft.getInstance().getResourceManager().getResource(textureId).isPresent();
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            if (this.cancelRect.contains(event.x(), event.y())) {
                onClose();
                return true;
            }

            GalleryCategory[] categories = GalleryCategory.values();
            for (int i = 0; i < categories.length; i++) {
                if (!this.categoryRects[i].contains(event.x(), event.y())) {
                    continue;
                }
                if (this.category != categories[i]) {
                    this.category = categories[i];
                    layoutGallery();
                }
                return true;
            }

            MotorwaySignPreset[] presets = MotorwaySignPreset.values();
            for (int index = 0; index < this.cards.length; index++) {
                SignEditorUi.Rect rect = this.cards[index];
                if (rect == null || !rect.contains(event.x(), event.y())) {
                    continue;
                }

                MotorwaySignPreset selected = presets[index];
                if (selected == this.currentPreset) {
                    onClose();
                } else {
                    MotorwaySignClientHooks.openEditor(
                            this.blockPos,
                            selected,
                            defaultsFor(selected),
                            false,
                            emptyPanels(),
                            MotorwaySignServiceIcon.defaults()
                    );
                }
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(this.returnScreen);
    }

    private static MotorwaySignLineData[] defaultsFor(MotorwaySignPreset preset) {
        MotorwaySignLineData[] defaults =
                new MotorwaySignLineData[MotorwaySignBlockEntity.MAX_SLOTS];
        for (int index = 0; index < defaults.length; index++) {
            defaults[index] = index < preset.getSlotCount()
                    ? MotorwaySignLineData.blankForSlot(preset.getSlot(index))
                    : MotorwaySignLineData.empty();
        }
        return defaults;
    }

    private static MotorwaySignPanelData[] emptyPanels() {
        MotorwaySignPanelData[] result =
                new MotorwaySignPanelData[MotorwaySignBlockEntity.MAX_CUSTOM_PANELS];
        for (int i = 0; i < result.length; i++) {
            result[i] = MotorwaySignPanelData.disabled();
        }
        return result;
    }

    private static MotorwaySignPanelData[] copyPanels(
            MotorwaySignPanelData[] panels
    ) {
        MotorwaySignPanelData[] result = emptyPanels();
        if (panels == null) {
            return result;
        }
        int count = Math.min(result.length, panels.length);
        for (int i = 0; i < count; i++) {
            MotorwaySignPanelData panel = panels[i];
            result[i] = panel == null
                    ? MotorwaySignPanelData.disabled()
                    : panel;
        }
        return result;
    }
}
