package net.xelpy.moreroad.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.xelpy.moreroad.MoreRoad;
import net.xelpy.moreroad.block.custom.PanonceauEntry;
import net.xelpy.moreroad.block.custom.PanonceauVariant;
import net.xelpy.moreroad.block.entity.PanonceauBlockEntity;
import net.xelpy.moreroad.network.UpdatePanonceauPayload;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Éditeur du support générique de panonceaux M1 à M12.
 *
 * Mise à jour : les variantes sont désormais présentées sous forme de
 * vignettes visuelles au lieu d'une simple liste de noms, afin de rendre
 * la recherche beaucoup plus simple quand une famille contient beaucoup de
 * panonceaux proches les uns des autres.
 */
public class PanonceauEditScreen extends Screen {

    private static final String[] FAMILIES = {"M1", "M2", "M3", "M4", "M5", "M6", "M7", "M8", "M9", "M10", "M11", "M12", "TXT"};
    private static final int MAX_VARIANT_CARDS = 12;

    // La grille n'est volontairement plus figée : son nombre de colonnes et
    // de lignes est recalculé à chaque ouverture selon la résolution logique
    // réellement disponible après application du GUI Scale de Minecraft.
    private int familyColumns = 6;
    private int variantColumns = 3;
    private int variantRows = 3;
    private int variantsPerPage = 9;

    private final BlockPos blockPos;
    private final PanonceauEntry[] entries = new PanonceauEntry[PanonceauBlockEntity.MAX_PANONCEAUX];
    private final Map<String, VariantPreview> previewCache = new HashMap<>();

    private int selectedSlot = 0;
    private String selectedFamily = "M1";
    private int variantPage = 0;

    private EditBox valueField;
    private EditBox secondLineField;

    private final SignEditorUi.Rect[] slotRects = new SignEditorUi.Rect[PanonceauBlockEntity.MAX_PANONCEAUX];
    private final SignEditorUi.Rect[] familyRects = new SignEditorUi.Rect[FAMILIES.length];
    private final SignEditorUi.Rect[] variantRects = new SignEditorUi.Rect[MAX_VARIANT_CARDS];

    private SignEditorUi.Rect stackRect;
    private SignEditorUi.Rect variantsRect;
    private SignEditorUi.Rect enabledRect;
    private SignEditorUi.Rect prevPageRect;
    private SignEditorUi.Rect nextPageRect;
    private SignEditorUi.Rect applyRect;
    private SignEditorUi.Rect cancelRect;

    private int windowX;
    private int windowY;
    private int windowWidth;
    private int windowHeight;
    private float scale = 1.0F;

    public PanonceauEditScreen(BlockPos blockPos, PanonceauEntry[] currentEntries) {
        super(Component.literal("Panonceaux M"));
        this.blockPos = blockPos.immutable();

        for (int i = 0; i < this.entries.length; i++) {
            this.entries[i] = currentEntries != null && i < currentEntries.length && currentEntries[i] != null
                    ? currentEntries[i]
                    : (i == 0 ? PanonceauEntry.defaultFirst() : PanonceauEntry.disabled());
        }

        this.selectedFamily = this.entries[0].variant().family();
    }

    @Override
    protected void init() {
        super.init();

        int marginX = Math.max(10, Math.min(32, this.width / 28));
        int marginY = Math.max(8, Math.min(22, this.height / 28));
        this.windowX = marginX;
        this.windowY = marginY;
        this.windowWidth = this.width - marginX * 2;
        this.windowHeight = this.height - marginY * 2;
        this.scale = SignEditorUi.adaptiveEditorScale(this.windowWidth, this.windowHeight, 1040.0F, 690.0F);

        int pad = s(SignEditorUi.ultraTightForScale(this.scale, this.windowHeight) ? 10 : 14);
        int header = SignEditorUi.adaptiveHeaderHeight(this.scale, false);
        int footer = SignEditorUi.adaptiveFooterHeight(this.scale);
        int gap = s(12);
        int bodyY = this.windowY + header;
        int bodyH = this.windowHeight - header - footer;

        int leftW = Math.max(s(300), Math.round((this.windowWidth - pad * 2 - gap) * 0.38F));
        int rightW = this.windowWidth - pad * 2 - gap - leftW;
        int leftX = this.windowX + pad;
        int rightX = leftX + leftW + gap;

        this.stackRect = new SignEditorUi.Rect(leftX, bodyY, leftW, bodyH - s(8));
        this.variantsRect = new SignEditorUi.Rect(rightX, bodyY, rightW, bodyH - s(8));

        int slotY = this.stackRect.y() + Math.max(s(46), 28);
        int slotGap = Math.max(3, s(6));
        int slotW = (this.stackRect.width() - Math.max(s(24), 12) - slotGap * 2) / 3;
        int slotX = this.stackRect.x() + Math.max(s(12), 6);

        for (int i = 0; i < this.slotRects.length; i++) {
            this.slotRects[i] = new SignEditorUi.Rect(
                    slotX + i * (slotW + slotGap),
                    slotY,
                    slotW,
                    Math.max(18, s(28))
            );
        }

        this.enabledRect = new SignEditorUi.Rect(
                this.stackRect.x() + Math.max(s(12), 6),
                slotY + Math.max(s(42), 30),
                this.stackRect.width() - Math.max(s(24), 12),
                Math.max(18, s(30))
        );

        int fieldY = slotY + Math.max(s(118), 82);
        this.valueField = new EditBox(
                this.font,
                this.stackRect.x() + Math.max(s(12), 6),
                fieldY,
                this.stackRect.width() - Math.max(s(24), 12),
                Math.max(18, s(26)),
                Component.literal("Valeur du panonceau")
        );
        this.valueField.setMaxLength(36);
        this.addRenderableWidget(this.valueField);

        int secondFieldY = fieldY + Math.max(31, s(36));
        this.secondLineField = new EditBox(
                this.font,
                this.stackRect.x() + Math.max(s(12), 6),
                secondFieldY,
                this.stackRect.width() - Math.max(s(24), 12),
                Math.max(18, s(26)),
                Component.literal("Ligne 2 facultative")
        );
        this.secondLineField.setMaxLength(36);
        this.addRenderableWidget(this.secondLineField);

        int familyY = this.variantsRect.y() + Math.max(s(46), 28);
        int familyGap = Math.max(3, s(6));
        int familyH = Math.max(18, s(28));
        int familyXPad = Math.max(s(12), 6);
        int familyX = this.variantsRect.x() + familyXPad;
        int familyAvailableW = this.variantsRect.width() - familyXPad * 2;

        this.familyColumns = chooseFamilyColumns(familyAvailableW);
        int familyW = Math.max(24, (familyAvailableW - familyGap * (this.familyColumns - 1)) / this.familyColumns);

        for (int i = 0; i < this.familyRects.length; i++) {
            int col = i % this.familyColumns;
            int row = i / this.familyColumns;
            this.familyRects[i] = new SignEditorUi.Rect(
                    familyX + col * (familyW + familyGap),
                    familyY + row * (familyH + familyGap),
                    familyW,
                    familyH
            );
        }

        int familyRows = (FAMILIES.length + this.familyColumns - 1) / this.familyColumns;
        int gridX = this.variantsRect.x() + familyXPad;
        int gridY = familyY + familyRows * familyH + Math.max(0, familyRows - 1) * familyGap + Math.max(s(18), 10);
        int gridW = this.variantsRect.width() - familyXPad * 2;
        int buttonGap = Math.max(4, s(7));

        // Les contrôles de pagination et la ligne de sélection ont une hauteur
        // minimale liée à la police : ils ne peuvent donc plus être écrasés par
        // un GUI Scale très élevé.
        int pageH = Math.max(18, s(26));
        int pageZoneH = pageH + 17;
        int gridBottom = this.variantsRect.y() + this.variantsRect.height() - pageZoneH - 10;
        int availableGridH = Math.max(24, gridBottom - gridY);

        this.variantColumns = chooseVariantColumns(gridW);
        this.variantRows = chooseVariantRows(availableGridH, buttonGap);
        this.variantsPerPage = Math.max(1, Math.min(MAX_VARIANT_CARDS, this.variantColumns * this.variantRows));

        int buttonW = Math.max(54, (gridW - buttonGap * (this.variantColumns - 1)) / this.variantColumns);
        int buttonH = Math.max(24, (availableGridH - buttonGap * (this.variantRows - 1)) / this.variantRows);

        for (int i = 0; i < this.variantRects.length; i++) {
            if (i >= this.variantsPerPage) {
                this.variantRects[i] = new SignEditorUi.Rect(0, 0, 0, 0);
                continue;
            }
            int col = i % this.variantColumns;
            int row = i / this.variantColumns;
            this.variantRects[i] = new SignEditorUi.Rect(
                    gridX + col * (buttonW + buttonGap),
                    gridY + row * (buttonH + buttonGap),
                    buttonW,
                    buttonH
            );
        }

        int pageY = gridY + this.variantRows * buttonH + Math.max(0, this.variantRows - 1) * buttonGap + 5;
        int pageW = Math.max(s(96), Math.max(this.font.width("← Page"), this.font.width("Page →")) + 16);
        this.prevPageRect = new SignEditorUi.Rect(gridX, pageY, pageW, pageH);
        this.nextPageRect = new SignEditorUi.Rect(gridX + gridW - pageW, pageY, pageW, pageH);

        // Si Minecraft rappelle init() après un changement de GUI Scale, la
        // variante sélectionnée reste visible même si le nombre de cartes par
        // page vient de changer.
        this.variantPage = pageContaining(this.entries[this.selectedSlot].variant(), this.selectedFamily);

        int actionH = Math.max(18, s(28));
        int actionW = Math.max(74, Math.max(s(145), this.font.width("✓  Appliquer") + 18));
        int actionY = this.windowY + this.windowHeight - Math.max(s(36), actionH + 4);
        this.cancelRect = new SignEditorUi.Rect(
                this.windowX + this.windowWidth - pad - actionW,
                actionY,
                actionW,
                actionH
        );
        this.applyRect = new SignEditorUi.Rect(
                this.cancelRect.x() - Math.max(4, s(10)) - actionW,
                actionY,
                actionW,
                actionH
        );

        loadSelectedEntryIntoWidgets();
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
                "M",
                "PANONCEAUX",
                compactUi() ? "" : "M1 à M12 + texte personnalisé • jusqu'à 3 plaques"
        );

        SignEditorUi.drawModernSection(
                graphics,
                this.font,
                this.stackRect,
                "SUPPORT",
                compactUi() ? "" : "Les plaques sont empilées de haut en bas"
        );

        for (int i = 0; i < this.slotRects.length; i++) {
            PanonceauEntry entry = this.entries[i];
            String label = fitText((i + 1) + " · " + entry.variant().getSerializedName().toUpperCase(), this.slotRects[i].width() - 8);
            SignEditorUi.drawModernButton(
                    graphics,
                    this.font,
                    this.slotRects[i],
                    label,
                    i == this.selectedSlot,
                    true,
                    mouseX,
                    mouseY
            );
        }

        PanonceauEntry selected = this.entries[this.selectedSlot];
        SignEditorUi.drawModernButton(
                graphics,
                this.font,
                this.enabledRect,
                fitText(
                        selected.enabled() ? "✓  Panonceau actif" : "○  Panonceau désactivé",
                        this.enabledRect.width() - 8
                ),
                selected.enabled(),
                true,
                mouseX,
                mouseY
        );

        int infoY = this.enabledRect.y() + Math.max(22, s(43));
        graphics.text(
                this.font,
                Component.literal(fitText(selected.variant().displayName(), this.stackRect.width() - 24)),
                this.stackRect.x() + s(12),
                infoY,
                SignEditorUi.COLOR_TEXT,
                false
        );

        if (selected.variant().isCustomText()) {
            SignEditorUi.drawFieldLabel(
                    graphics,
                    this.font,
                    "Ligne 1",
                    this.valueField.getX(),
                    this.valueField.getY() - Math.max(12, s(12))
            );
            SignEditorUi.drawFieldLabel(
                    graphics,
                    this.font,
                    "Ligne 2 (facultative)",
                    this.secondLineField.getX(),
                    this.secondLineField.getY() - Math.max(12, s(12))
            );
            if (!compactUi()) {
                graphics.text(
                        this.font,
                        Component.literal(fitText("Ligne 2 vide = texte centré sur une seule ligne.", this.stackRect.width() - 24)),
                        this.stackRect.x() + Math.max(s(12), 6),
                        this.secondLineField.getY() + Math.max(24, s(32)),
                        SignEditorUi.MODERN_MUTED,
                        false
                );
            }
        } else if (selected.variant().isEditable()) {
            SignEditorUi.drawFieldLabel(
                    graphics,
                    this.font,
                    "Valeur affichée",
                    this.valueField.getX(),
                    this.valueField.getY() - Math.max(12, s(12))
            );
            if (!compactUi()) {
                graphics.text(
                        this.font,
                        Component.literal(fitText("Exemples : 150 m, 4,5 km, 2 t", this.stackRect.width() - 24)),
                        this.stackRect.x() + Math.max(s(12), 6),
                        this.valueField.getY() + Math.max(24, s(36)),
                        SignEditorUi.MODERN_MUTED,
                        false
                );
            }
        } else if (!compactUi()) {
            graphics.text(
                    this.font,
                    Component.literal(fitText("Cette variante reprend directement le SVG fourni.", this.stackRect.width() - 24)),
                    this.stackRect.x() + Math.max(s(12), 6),
                    this.valueField.getY(),
                    SignEditorUi.MODERN_MUTED,
                    false
            );
        }

        int summaryLineGap = Math.max(11, s(16));
        int summaryFirstOffset = Math.max(13, s(18));
        int summaryRequiredHeight = summaryFirstOffset + summaryLineGap * 2 + this.font.lineHeight + 4;
        int currentFieldHeight = Math.max(18, s(26));
        int summaryBaseBottom = selected.variant().isCustomText()
                ? this.secondLineField.getY() + currentFieldHeight
                : this.valueField.getY() + currentFieldHeight;
        int summaryY = summaryBaseBottom + Math.max(14, s(24));
        int summaryBottomLimit = this.stackRect.y() + this.stackRect.height() - 6;

        // La composition n'est affichée que lorsqu'elle tient intégralement.
        // On ne la remonte jamais au-dessus des champs : aux GUI Scale élevés,
        // il vaut mieux masquer cette information secondaire que chevaucher le formulaire.
        if (summaryY + summaryRequiredHeight <= summaryBottomLimit) {
            graphics.text(
                    this.font,
                    Component.literal("Composition du support :"),
                    this.stackRect.x() + Math.max(s(12), 6),
                    summaryY,
                    SignEditorUi.COLOR_TEXT,
                    false
            );

            for (int i = 0; i < this.entries.length; i++) {
                PanonceauEntry entry = this.entries[i];
                String line = entry.enabled()
                        ? "• " + (i + 1) + "  " + entry.variant().getSerializedName().toUpperCase()
                        : "• " + (i + 1) + "  désactivé";
                line = fitText(line, this.stackRect.width() - Math.max(s(36), 18));
                graphics.text(
                        this.font,
                        Component.literal(line),
                        this.stackRect.x() + Math.max(s(18), 9),
                        summaryY + summaryFirstOffset + i * summaryLineGap,
                        entry.enabled() ? SignEditorUi.COLOR_TEXT : SignEditorUi.MODERN_MUTED,
                        false
                );
            }
        }

        SignEditorUi.drawModernSection(
                graphics,
                this.font,
                this.variantsRect,
                "TYPE",
                compactUi() ? "" : "Famille puis variante de signalisation"
        );

        for (int i = 0; i < this.familyRects.length; i++) {
            String family = FAMILIES[i];
            SignEditorUi.drawModernButton(
                    graphics,
                    this.font,
                    this.familyRects[i],
                    family,
                    family.equals(this.selectedFamily),
                    true,
                    mouseX,
                    mouseY
            );
        }

        List<PanonceauVariant> variants = PanonceauVariant.forFamily(this.selectedFamily);
        int start = this.variantPage * this.variantsPerPage;

        for (int i = 0; i < this.variantsPerPage; i++) {
            int index = start + i;
            if (index >= variants.size()) {
                drawEmptyVariantCard(graphics, this.variantRects[i]);
                continue;
            }

            PanonceauVariant variant = variants.get(index);
            drawVariantCard(
                    graphics,
                    this.variantRects[i],
                    variant,
                    variant == selected.variant(),
                    mouseX,
                    mouseY
            );
        }

        int pageCount = Math.max(1, (variants.size() + this.variantsPerPage - 1) / this.variantsPerPage);
        if (pageCount > 1) {
            SignEditorUi.drawModernButton(
                    graphics,
                    this.font,
                    this.prevPageRect,
                    "← Page",
                    false,
                    this.variantPage > 0,
                    mouseX,
                    mouseY
            );
            SignEditorUi.drawModernButton(
                    graphics,
                    this.font,
                    this.nextPageRect,
                    "Page →",
                    false,
                    this.variantPage < pageCount - 1,
                    mouseX,
                    mouseY
            );

            graphics.text(
                    this.font,
                    Component.literal((this.variantPage + 1) + " / " + pageCount),
                    this.variantsRect.x() + this.variantsRect.width() / 2 - s(12),
                    this.prevPageRect.y() + s(8),
                    SignEditorUi.MODERN_MUTED,
                    false
            );
        }

        String selectionText = fitText(
                "Sélection : " + selected.variant().displayName(),
                this.variantsRect.width() - Math.max(s(24), 12)
        );
        graphics.text(
                this.font,
                Component.literal(selectionText),
                this.variantsRect.x() + Math.max(s(12), 6),
                Math.min(
                        this.variantsRect.y() + this.variantsRect.height() - 13,
                        this.prevPageRect.y() + this.prevPageRect.height() + 4
                ),
                SignEditorUi.MODERN_MUTED,
                false
        );

        SignEditorUi.drawModernButton(graphics, this.font, this.applyRect, "✓  Appliquer", true, true, mouseX, mouseY);
        SignEditorUi.drawModernButton(graphics, this.font, this.cancelRect, "×  Annuler", false, true, mouseX, mouseY);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            double x = event.x();
            double y = event.y();

            for (int i = 0; i < this.slotRects.length; i++) {
                if (this.slotRects[i].contains(x, y)) {
                    commitCurrentEntry();
                    this.selectedSlot = i;
                    this.selectedFamily = this.entries[i].variant().family();
                    this.variantPage = pageContaining(this.entries[i].variant(), this.selectedFamily);
                    loadSelectedEntryIntoWidgets();
                    return true;
                }
            }

            if (this.enabledRect.contains(x, y)) {
                commitCurrentEntry();
                PanonceauEntry current = this.entries[this.selectedSlot];
                this.entries[this.selectedSlot] = new PanonceauEntry(
                        !current.enabled(),
                        current.variant(),
                        current.value()
                );
                loadSelectedEntryIntoWidgets();
                return true;
            }

            for (int i = 0; i < this.familyRects.length; i++) {
                if (this.familyRects[i].contains(x, y)) {
                    commitCurrentEntry();
                    this.selectedFamily = FAMILIES[i];
                    this.variantPage = 0;

                    PanonceauEntry current = this.entries[this.selectedSlot];
                    if (!current.variant().family().equals(this.selectedFamily)) {
                        PanonceauVariant first = PanonceauVariant.forFamily(this.selectedFamily).get(0);
                        this.entries[this.selectedSlot] = new PanonceauEntry(
                                current.enabled(),
                                first,
                                first.defaultValue()
                        );
                    }

                    loadSelectedEntryIntoWidgets();
                    return true;
                }
            }

            List<PanonceauVariant> variants = PanonceauVariant.forFamily(this.selectedFamily);
            int start = this.variantPage * this.variantsPerPage;
            for (int i = 0; i < this.variantsPerPage; i++) {
                int index = start + i;
                if (index >= variants.size()) {
                    continue;
                }
                if (this.variantRects[i].contains(x, y)) {
                    commitCurrentEntry();
                    PanonceauEntry current = this.entries[this.selectedSlot];
                    PanonceauVariant variant = variants.get(index);
                    String value = variant.isEditable()
                            ? variant.defaultValue()
                            : "";
                    this.entries[this.selectedSlot] = new PanonceauEntry(
                            current.enabled(),
                            variant,
                            value
                    );
                    loadSelectedEntryIntoWidgets();
                    return true;
                }
            }

            int pageCount = Math.max(1, (variants.size() + this.variantsPerPage - 1) / this.variantsPerPage);
            if (this.prevPageRect.contains(x, y) && this.variantPage > 0) {
                this.variantPage--;
                return true;
            }
            if (this.nextPageRect.contains(x, y) && this.variantPage < pageCount - 1) {
                this.variantPage++;
                return true;
            }

            if (this.applyRect.contains(x, y)) {
                save();
                return true;
            }

            if (this.cancelRect.contains(x, y)) {
                this.onClose();
                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    private void drawEmptyVariantCard(GuiGraphicsExtractor graphics, SignEditorUi.Rect rect) {
        if (rect.width() <= 0 || rect.height() <= 0) {
            return;
        }
        graphics.fill(rect.x(), rect.y(), rect.x() + rect.width(), rect.y() + rect.height(), 0x351D232A);
        graphics.outline(rect.x(), rect.y(), rect.width(), rect.height(), 0x604A5360);
    }

    private void drawVariantCard(
            GuiGraphicsExtractor graphics,
            SignEditorUi.Rect rect,
            PanonceauVariant variant,
            boolean selected,
            int mouseX,
            int mouseY
    ) {
        if (rect.width() <= 0 || rect.height() <= 0) {
            return;
        }

        boolean hovered = rect.contains(mouseX, mouseY);

        int fill = selected
                ? 0xFF3B526A
                : hovered ? 0xFF313A45 : 0xFF252B33;
        int border = selected
                ? 0xFFB7D0E8
                : hovered ? 0xFF93A5B8 : 0xFF657180;

        graphics.fill(rect.x(), rect.y(), rect.x() + rect.width(), rect.y() + rect.height(), fill);
        graphics.outline(rect.x(), rect.y(), rect.width(), rect.height(), border);

        // Le code reste toujours dans une bande dédiée de 13 px minimum :
        // la police ne peut donc plus sortir de la carte aux GUI Scale élevés.
        int tagH = Math.min(rect.height() - 6, Math.max(13, s(14)));
        int tagX = rect.x() + 4;
        int tagY = rect.y() + 3;
        int tagMaxW = Math.max(16, rect.width() - 8);
        String code = fitText(variant.getSerializedName().toUpperCase(), tagMaxW - 8);
        int tagW = Math.min(tagMaxW, Math.max(20, this.font.width(code) + 8));

        graphics.fill(tagX, tagY, tagX + tagW, tagY + tagH, selected ? 0xFF27445F : 0xCC1B2027);
        graphics.outline(tagX, tagY, tagW, tagH, selected ? 0xFF8CB6DA : 0x805F6B78);
        graphics.text(
                this.font,
                Component.literal(code),
                tagX + 4,
                tagY + Math.max(2, (tagH - 9) / 2),
                SignEditorUi.COLOR_TEXT,
                false
        );

        int previewTop = tagY + tagH + 4;
        int previewBottom = rect.y() + rect.height() - 5;
        int previewHeight = previewBottom - previewTop;

        // Si une carte devient exceptionnellement petite, on garde le code et
        // on ne dessine pas une image écrasée qui pourrait dépasser.
        if (previewHeight < 10) {
            return;
        }

        SignEditorUi.Rect previewRect = new SignEditorUi.Rect(
                rect.x() + 5,
                previewTop,
                Math.max(8, rect.width() - 10),
                previewHeight
        );

        graphics.fill(
                previewRect.x(),
                previewRect.y(),
                previewRect.x() + previewRect.width(),
                previewRect.y() + previewRect.height(),
                0xFFF1F1F1
        );
        graphics.outline(
                previewRect.x(),
                previewRect.y(),
                previewRect.width(),
                previewRect.height(),
                0xFFBBC1C7
        );

        drawVariantPreview(graphics, previewRect, variant);
        drawDynamicPreviewText(graphics, previewRect, variant);
    }

    private void drawDynamicPreviewText(
            GuiGraphicsExtractor graphics,
            SignEditorUi.Rect rect,
            PanonceauVariant variant
    ) {
        if (!variant.isEditable() || rect.height() < 18) {
            return;
        }

        String value = variant.defaultValue();
        int centerX = rect.x() + rect.width() / 2;
        int centerY = rect.y() + rect.height() / 2;

        switch (variant.renderMode()) {
            case FIXED -> {
            }
            case CENTER_VALUE, CENTER_VALUE_WITH_ARROWS -> drawPreviewText(
                    graphics,
                    value,
                    centerX,
                    centerY - 4,
                    Math.max(12, Math.round(rect.width() * 0.56F))
            );
            case M3B_RIGHT_VALUE -> drawPreviewText(
                    graphics,
                    value,
                    rect.x() + Math.round(rect.width() * 0.74F),
                    centerY - 4,
                    Math.max(10, Math.round(rect.width() * 0.38F))
            );
            case M3B_LEFT_VALUE -> drawPreviewText(
                    graphics,
                    value,
                    rect.x() + Math.round(rect.width() * 0.26F),
                    centerY - 4,
                    Math.max(10, Math.round(rect.width() * 0.38F))
            );
            case LOWER_VALUE -> drawPreviewText(
                    graphics,
                    value,
                    centerX,
                    rect.y() + Math.max(2, rect.height() - 12),
                    Math.max(12, Math.round(rect.width() * 0.62F))
            );
            case UPPER_VALUE -> drawPreviewText(
                    graphics,
                    value,
                    centerX,
                    rect.y() + 3,
                    Math.max(12, Math.round(rect.width() * 0.66F))
            );
            case M5A -> {
                drawPreviewText(
                        graphics,
                        "STOP",
                        centerX,
                        centerY - 11,
                        Math.max(12, Math.round(rect.width() * 0.76F))
                );
                drawPreviewText(
                        graphics,
                        value,
                        centerX,
                        centerY + 3,
                        Math.max(12, Math.round(rect.width() * 0.80F))
                );
            }
            case M5B -> {
                drawPreviewText(
                        graphics,
                        "STOP",
                        rect.x() + Math.round(rect.width() * 0.28F),
                        centerY - 4,
                        Math.max(12, Math.round(rect.width() * 0.40F))
                );
                drawPreviewText(
                        graphics,
                        value,
                        rect.x() + Math.round(rect.width() * 0.73F),
                        centerY - 4,
                        Math.max(12, Math.round(rect.width() * 0.42F))
                );
            }
            case CUSTOM_TEXT -> {
                String[] lines = splitCustomLines(value);
                if (lines[1].isBlank()) {
                    drawPreviewText(
                            graphics,
                            lines[0],
                            centerX,
                            centerY - 4,
                            Math.max(12, Math.round(rect.width() * 0.82F))
                    );
                } else {
                    drawPreviewText(
                            graphics,
                            lines[0],
                            centerX,
                            centerY - 10,
                            Math.max(12, Math.round(rect.width() * 0.82F))
                    );
                    drawPreviewText(
                            graphics,
                            lines[1],
                            centerX,
                            centerY + 2,
                            Math.max(12, Math.round(rect.width() * 0.82F))
                    );
                }
            }
        }
    }

    private void drawPreviewText(
            GuiGraphicsExtractor graphics,
            String value,
            int centerX,
            int y,
            int maxWidth
    ) {
        String fitted = fitText(value, maxWidth);
        if (fitted.isBlank()) {
            return;
        }
        graphics.centeredText(
                this.font,
                Component.literal(fitted),
                centerX,
                y,
                0xFF111111
        );
    }

    private void drawVariantPreview(GuiGraphicsExtractor graphics, SignEditorUi.Rect rect, PanonceauVariant variant) {
        VariantPreview preview = getPreview(variant, Math.max(12, rect.width() - 4), Math.max(10, rect.height() - 4));
        if (preview == null) {
            graphics.centeredText(
                    this.font,
                    Component.literal(variant.family()),
                    rect.x() + rect.width() / 2,
                    rect.y() + rect.height() / 2 - s(4),
                    0xFF202020
            );
            return;
        }

        int startX = rect.x() + Math.max(1, (rect.width() - preview.width()) / 2);
        int startY = rect.y() + Math.max(1, (rect.height() - preview.height()) / 2);

        for (int y = 0; y < preview.height(); y++) {
            int runColor = 0;
            int runStart = -1;

            for (int x = 0; x < preview.width(); x++) {
                int argb = preview.pixel(x, y);
                if (((argb >>> 24) & 0xFF) < 28) {
                    argb = 0;
                }

                if (argb != runColor) {
                    if (runStart >= 0 && runColor != 0) {
                        graphics.fill(startX + runStart, startY + y, startX + x, startY + y + 1, runColor);
                    }
                    runColor = argb;
                    runStart = x;
                }
            }

            if (runStart >= 0 && runColor != 0) {
                graphics.fill(startX + runStart, startY + y, startX + preview.width(), startY + y + 1, runColor);
            }
        }
    }

    private VariantPreview getPreview(PanonceauVariant variant, int maxWidth, int maxHeight) {
        String key = variant.getSerializedName() + "@" + maxWidth + "x" + maxHeight;
        VariantPreview cached = this.previewCache.get(key);
        if (cached != null) {
            return cached;
        }

        VariantPreview preview = buildPreview(variant, maxWidth, maxHeight);
        if (preview != null) {
            this.previewCache.put(key, preview);
        }
        return preview;
    }

    private VariantPreview buildPreview(PanonceauVariant variant, int maxWidth, int maxHeight) {
        Identifier textureId = Identifier.fromNamespaceAndPath(
                MoreRoad.MODID,
                "textures/block/panonceaux/" + variant.textureFile()
        );

        try {
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(textureId);
            if (resource.isEmpty()) {
                return null;
            }

            try (InputStream stream = resource.get().open()) {
                BufferedImage image = ImageIO.read(stream);
                if (image == null) {
                    return null;
                }

                int srcWidth = image.getWidth();
                int srcHeight = image.getHeight();
                if (srcWidth <= 0 || srcHeight <= 0) {
                    return null;
                }

                float scale = Math.min(maxWidth / (float) srcWidth, maxHeight / (float) srcHeight);
                scale = Math.max(scale, 0.01F);

                int outWidth = Math.max(1, Math.min(maxWidth, Math.round(srcWidth * scale)));
                int outHeight = Math.max(1, Math.min(maxHeight, Math.round(srcHeight * scale)));
                int[] pixels = new int[outWidth * outHeight];

                for (int y = 0; y < outHeight; y++) {
                    int srcY = Math.min(srcHeight - 1, Math.max(0, Math.round((y + 0.5F) / scale - 0.5F)));
                    for (int x = 0; x < outWidth; x++) {
                        int srcX = Math.min(srcWidth - 1, Math.max(0, Math.round((x + 0.5F) / scale - 0.5F)));
                        pixels[y * outWidth + x] = image.getRGB(srcX, srcY);
                    }
                }

                return new VariantPreview(outWidth, outHeight, pixels);
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private int chooseFamilyColumns(int availableWidth) {
        // Avec la famille TXT, 7 colonnes permettent de rester sur 2 rangées
        // quand la largeur le permet, sans compresser les libellés.
        if (availableWidth >= 420) {
            return 7;
        }
        if (availableWidth >= 250) {
            return 6;
        }
        if (availableWidth >= 150) {
            return 4;
        }
        return 3;
    }

    private int chooseVariantColumns(int availableWidth) {
        if (availableWidth >= 510) {
            return 3;
        }
        if (availableWidth >= 255) {
            return 2;
        }
        return 1;
    }

    private int chooseVariantRows(int availableHeight, int gap) {
        // Une carte de 52 px est suffisante pour une bande de code + une vraie
        // vignette. On choisit le plus grand nombre de lignes qui respecte ce
        // minimum au lieu de forcer 3 lignes qui s'écraseraient.
        int minCardHeight = 52;
        int rows = (availableHeight + gap) / (minCardHeight + gap);
        return Math.max(1, Math.min(3, rows));
    }

    private String fitText(String value, int maxWidth) {
        if (value == null || value.isEmpty() || maxWidth <= 0) {
            return "";
        }
        if (this.font.width(value) <= maxWidth) {
            return value;
        }

        String ellipsis = "…";
        int ellipsisWidth = this.font.width(ellipsis);
        if (ellipsisWidth >= maxWidth) {
            return "";
        }

        int end = value.length();
        while (end > 0 && this.font.width(value.substring(0, end)) + ellipsisWidth > maxWidth) {
            end--;
        }
        return end <= 0 ? ellipsis : value.substring(0, end) + ellipsis;
    }

    private void loadSelectedEntryIntoWidgets() {
        if (this.valueField == null || this.secondLineField == null) {
            return;
        }

        PanonceauEntry entry = this.entries[this.selectedSlot];
        boolean editable = entry.variant().isEditable();
        boolean customText = entry.variant().isCustomText();

        if (customText) {
            String[] lines = splitCustomLines(entry.value());
            this.valueField.setValue(lines[0]);
            this.secondLineField.setValue(lines[1]);
        } else {
            this.valueField.setValue(entry.value());
            this.secondLineField.setValue("");
        }

        this.valueField.visible = editable;
        this.valueField.active = editable;
        this.secondLineField.visible = customText;
        this.secondLineField.active = customText;

        if (editable) {
            this.setInitialFocus(this.valueField);
        }
    }

    private void commitCurrentEntry() {
        PanonceauEntry current = this.entries[this.selectedSlot];
        String value = current.value();

        if (current.variant().isCustomText() && this.valueField != null && this.secondLineField != null) {
            value = combineCustomLines(this.valueField.getValue(), this.secondLineField.getValue());
        } else if (current.variant().isEditable() && this.valueField != null) {
            value = this.valueField.getValue();
        }

        this.entries[this.selectedSlot] = new PanonceauEntry(
                current.enabled(),
                current.variant(),
                value
        );
    }

    private static String combineCustomLines(String line1, String line2) {
        String first = line1 == null ? "" : line1.strip();
        String second = line2 == null ? "" : line2.strip();
        return second.isBlank() ? first : first + "\n" + second;
    }

    private static String[] splitCustomLines(String value) {
        String normalized = value == null ? "" : value.replace("\r", "");
        String[] raw = normalized.split("\n", 2);
        return new String[]{
                raw.length > 0 ? raw[0].strip() : "",
                raw.length > 1 ? raw[1].strip() : ""
        };
    }

    private int pageContaining(PanonceauVariant variant, String family) {
        List<PanonceauVariant> variants = PanonceauVariant.forFamily(family);
        int index = variants.indexOf(variant);
        return index < 0 ? 0 : index / Math.max(1, this.variantsPerPage);
    }

    private boolean compactUi() {
        return SignEditorUi.compactForScale(this.scale);
    }

    private int s(int value) {
        return SignEditorUi.scaledUi(value, this.scale);
    }

    private void save() {
        commitCurrentEntry();
        ClientPacketDistributor.sendToServer(
                new UpdatePanonceauPayload(
                        this.blockPos,
                        this.entries[0],
                        this.entries[1],
                        this.entries[2]
                )
        );
        this.onClose();
    }

    private record VariantPreview(int width, int height, int[] pixels) {
        private int pixel(int x, int y) {
            return this.pixels[y * this.width + x];
        }
    }
}
