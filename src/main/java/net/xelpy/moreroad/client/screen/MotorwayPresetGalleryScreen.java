package net.xelpy.moreroad.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.xelpy.moreroad.MoreRoad;
import net.xelpy.moreroad.block.custom.MotorwaySignLineData;
import net.xelpy.moreroad.block.custom.MotorwaySignPanelData;
import net.xelpy.moreroad.block.custom.MotorwaySignPreset;
import net.xelpy.moreroad.block.entity.MotorwaySignBlockEntity;
import net.xelpy.moreroad.client.MotorwaySignClientHooks;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Galerie visuelle commune à tous les panneaux D / DA.
 *
 * V4 : la galerie est répartie en trois familles pour garder de vraies grandes
 * miniatures lisibles. Les 62 modèles ne sont plus tassés sur une seule ligne
 * géante : cela évite les cartes hors écran, les titres tronqués et le rendu
 * fortement pixelisé. Les aperçus 640x360 sont redimensionnés en bicubique
 * avant d'être dessinés dans le GUI.
 */
public final class MotorwayPresetGalleryScreen extends Screen {

    private enum GalleryCategory {
        D31_D52("D31 → D52") {
            @Override
            boolean accepts(MotorwaySignPreset preset) {
                String name = preset.name();
                return name.startsWith("D")
                        && !name.startsWith("DA")
                        && categoryNumber(name) >= 31
                        && categoryNumber(name) <= 52;
            }
        },
        D61_D74("D61 → D74") {
            @Override
            boolean accepts(MotorwaySignPreset preset) {
                String name = preset.name();
                return name.startsWith("D")
                        && !name.startsWith("DA")
                        && categoryNumber(name) >= 61
                        && categoryNumber(name) <= 74;
            }
        },
        DA("DA31 → DA52") {
            @Override
            boolean accepts(MotorwaySignPreset preset) {
                return preset.name().startsWith("DA");
            }
        };

        private final String label;

        GalleryCategory(String label) {
            this.label = label;
        }

        abstract boolean accepts(MotorwaySignPreset preset);

        static GalleryCategory forPreset(MotorwaySignPreset preset) {
            for (GalleryCategory category : values()) {
                if (category.accepts(preset)) {
                    return category;
                }
            }
            return D31_D52;
        }

        private static int categoryNumber(String name) {
            int index = name.startsWith("DA") ? 2 : 1;
            int value = 0;
            boolean found = false;
            while (index < name.length()) {
                char c = name.charAt(index);
                if (c < '0' || c > '9') {
                    break;
                }
                found = true;
                value = value * 10 + (c - '0');
                index++;
            }
            return found ? value : -1;
        }
    }

    private final Screen returnScreen;
    private final BlockPos blockPos;
    private final MotorwaySignPreset currentPreset;
    private final MotorwaySignPanelData[] customPanels;
    private final SignEditorUi.Rect[] cards =
            new SignEditorUi.Rect[MotorwaySignPreset.values().length];
    private final Map<String, GalleryPreview> previewCache = new HashMap<>();
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
                ? MotorwaySignPreset.D31B_EX1
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
            if (this.category.accepts(presets[i])) {
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
                "Choisir le modèle en image",
                visibleCount + " modèles dans cette famille • miniatures agrandies"
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
        int previewH = Math.max(20, rect.height() - 27);
        drawArtworkPreview(
                graphics,
                preset,
                previewX,
                previewY,
                previewW,
                previewH
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
        GalleryPreview preview = getPreview(preset, width, height);
        if (preview == null) {
            graphics.centeredText(
                    this.font,
                    Component.literal(preset.getDisplayName()),
                    x + width / 2,
                    y + Math.max(0, height / 2 - this.font.lineHeight / 2),
                    0xFFD5DDE6
            );
            return;
        }

        int startX = x + Math.max(0, (width - preview.width()) / 2);
        int startY = y + Math.max(0, (height - preview.height()) / 2);

        /*
         * Dessin par segments horizontaux. L'image a déjà été réduite en
         * bicubique : les contours et les textes restent donc bien plus doux
         * que dans l'ancienne réduction nearest-neighbour.
         */
        for (int py = 0; py < preview.height(); py++) {
            int runColor = 0;
            int runStart = -1;
            for (int px = 0; px < preview.width(); px++) {
                int argb = preview.pixel(px, py);
                if (((argb >>> 24) & 0xFF) < 18) {
                    argb = 0;
                }
                if (argb != runColor) {
                    if (runStart >= 0 && runColor != 0) {
                        graphics.fill(
                                startX + runStart,
                                startY + py,
                                startX + px,
                                startY + py + 1,
                                runColor
                        );
                    }
                    runColor = argb;
                    runStart = px;
                }
            }
            if (runStart >= 0 && runColor != 0) {
                graphics.fill(
                        startX + runStart,
                        startY + py,
                        startX + preview.width(),
                        startY + py + 1,
                        runColor
                );
            }
        }
    }

    private GalleryPreview getPreview(
            MotorwaySignPreset preset,
            int maxWidth,
            int maxHeight
    ) {
        String key = preset.getSerializedName()
                + "@" + maxWidth + "x" + maxHeight;
        GalleryPreview cached = this.previewCache.get(key);
        if (cached != null) {
            return cached;
        }

        GalleryPreview preview = buildPreview(preset, maxWidth, maxHeight);
        if (preview != null) {
            this.previewCache.put(key, preview);
        }
        return preview;
    }

    private static GalleryPreview buildPreview(
            MotorwaySignPreset preset,
            int maxWidth,
            int maxHeight
    ) {
        Identifier textureId = Identifier.fromNamespaceAndPath(
                MoreRoad.MODID,
                "textures/gui/motorway_gallery/"
                        + preset.getSerializedName()
                        + ".png"
        );

        try {
            Optional<Resource> resource = Minecraft.getInstance()
                    .getResourceManager()
                    .getResource(textureId);
            if (resource.isEmpty()) {
                return null;
            }

            try (InputStream stream = resource.get().open()) {
                BufferedImage image = ImageIO.read(stream);
                if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                    return null;
                }

                float scale = Math.min(
                        maxWidth / (float) image.getWidth(),
                        maxHeight / (float) image.getHeight()
                );
                scale = Math.max(0.01F, Math.min(1.0F, scale));

                int outWidth = Math.max(
                        1,
                        Math.min(maxWidth, Math.round(image.getWidth() * scale))
                );
                int outHeight = Math.max(
                        1,
                        Math.min(maxHeight, Math.round(image.getHeight() * scale))
                );

                BufferedImage resized = new BufferedImage(
                        outWidth,
                        outHeight,
                        BufferedImage.TYPE_INT_ARGB
                );
                Graphics2D g = resized.createGraphics();
                try {
                    g.setRenderingHint(
                            RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_BICUBIC
                    );
                    g.setRenderingHint(
                            RenderingHints.KEY_RENDERING,
                            RenderingHints.VALUE_RENDER_QUALITY
                    );
                    g.setRenderingHint(
                            RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON
                    );
                    g.drawImage(image, 0, 0, outWidth, outHeight, null);
                } finally {
                    g.dispose();
                }

                int[] pixels = new int[outWidth * outHeight];
                resized.getRGB(
                        0,
                        0,
                        outWidth,
                        outHeight,
                        pixels,
                        0,
                        outWidth
                );
                return new GalleryPreview(outWidth, outHeight, pixels);
            }
        } catch (Exception ignored) {
            return null;
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
                            emptyPanels()
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
                    ? MotorwaySignLineData.fromSlot(preset.getSlot(index))
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

    private record GalleryPreview(int width, int height, int[] pixels) {
        private int pixel(int x, int y) {
            return this.pixels[y * this.width + x];
        }
    }
}
