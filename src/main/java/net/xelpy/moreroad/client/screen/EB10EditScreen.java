package net.xelpy.moreroad.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.xelpy.moreroad.block.custom.CartoucheType;
import net.xelpy.moreroad.block.custom.RoadTextFont;
import net.xelpy.moreroad.network.UpdateEB10TextPayload;

public class EB10EditScreen extends Screen {

    private final BlockPos blockPos;
    private final String currentLine1;
    private final String currentLine2;
    private final boolean currentEb20;
    private final String currentCartoucheText;

    private RoadTextFont line1Font;
    private RoadTextFont line2Font;
    private CartoucheType cartoucheType;
    private boolean eb20;

    private EditBox line1Field;
    private EditBox line2Field;
    private EditBox cartoucheTextField;

    private SignEditorUi.Rect previewRect;
    private SignEditorUi.Rect contentRect;
    private SignEditorUi.Rect cartoucheRect;
    private SignEditorUi.Rect structureRect;
    private SignEditorUi.Rect line1FontRect;
    private SignEditorUi.Rect line2FontRect;
    private SignEditorUi.Rect cartoucheTypeRect;
    private SignEditorUi.Rect eb20Rect;
    private SignEditorUi.Rect applyRect;
    private SignEditorUi.Rect cancelRect;

    private final SignEditorUi.Rect[] settingsPageRects = new SignEditorUi.Rect[3];
    private int settingsPage = 0;

    private int windowX;
    private int windowY;
    private int windowWidth;
    private int windowHeight;
    private float scale = 1.0F;

    public EB10EditScreen(
            BlockPos blockPos,
            String currentLine1,
            String currentLine2,
            RoadTextFont currentLine1Font,
            RoadTextFont currentLine2Font,
            boolean currentEb20,
            CartoucheType currentCartoucheType,
            String currentCartoucheText
    ) {
        super(Component.literal("Panneau d'agglomération"));
        this.blockPos = blockPos.immutable();
        this.currentLine1 = currentLine1 == null ? "" : currentLine1;
        this.currentLine2 = currentLine2 == null ? "" : currentLine2;
        this.line1Font = currentLine1Font == null ? RoadTextFont.L1 : currentLine1Font;
        this.line2Font = currentLine2Font == null ? RoadTextFont.L1 : currentLine2Font;
        this.currentEb20 = currentEb20;
        this.eb20 = currentEb20;
        this.currentCartoucheText = currentCartoucheText == null ? "" : currentCartoucheText;
        this.cartoucheType = currentCartoucheType == null ? CartoucheType.NONE : currentCartoucheType;
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
        this.scale = SignEditorUi.adaptiveEditorScale(this.windowWidth, this.windowHeight, 1080.0F, 700.0F);

        boolean tight = SignEditorUi.tightForScale(this.scale, this.windowHeight);
        boolean ultraTight = SignEditorUi.ultraTightForScale(this.scale, this.windowHeight);
        int pad = s(ultraTight ? 10 : tight ? 12 : 16);
        int header = SignEditorUi.adaptiveHeaderHeight(this.scale, false);
        int footer = SignEditorUi.adaptiveFooterHeight(this.scale);
        int gap = s(tight ? 9 : 14);
        int sectionGap = s(ultraTight ? 5 : tight ? 7 : 12);
        int bodyY = this.windowY + header;
        int bodyH = this.windowHeight - header - footer;

        int leftW = Math.max(s(300), Math.round((this.windowWidth - pad * 2 - gap) * 0.45F));
        int rightW = this.windowWidth - pad * 2 - gap - leftW;
        int leftX = this.windowX + pad;
        int rightX = leftX + leftW + gap;

        this.previewRect = new SignEditorUi.Rect(leftX, bodyY, leftW, bodyH - s(8));

        if (pagedUi()) {
            int pageHeight = 22;
            SignEditorUi.Rect pageBar = new SignEditorUi.Rect(rightX, bodyY, rightW, pageHeight);
            SignEditorUi.Rect[] pages = SignEditorUi.pageTabRects(pageBar, this.settingsPageRects.length, pageHeight, 4);
            System.arraycopy(pages, 0, this.settingsPageRects, 0, this.settingsPageRects.length);
            int cardY = bodyY + pageHeight + 6;
            SignEditorUi.Rect fullCard = new SignEditorUi.Rect(rightX, cardY, rightW, Math.max(96, bodyH - pageHeight - 10));
            this.contentRect = fullCard;
            this.cartoucheRect = fullCard;
            this.structureRect = fullCard;
        } else {
            int availableSections = Math.max(120, bodyH - s(ultraTight ? 2 : 8));
            int[] sectionHeights = SignEditorUi.fitSections(
                    availableSections,
                    sectionGap,
                    new float[]{0.46F, 0.30F, 0.24F},
                    new int[]{
                            s(ultraTight ? 74 : tight ? 98 : 150),
                            s(ultraTight ? 56 : tight ? 72 : 110),
                            s(ultraTight ? 48 : tight ? 60 : 80)
                    }
            );

            int y = bodyY;
            this.contentRect = new SignEditorUi.Rect(rightX, y, rightW, sectionHeights[0]);
            y += sectionHeights[0] + sectionGap;
            this.cartoucheRect = new SignEditorUi.Rect(rightX, y, rightW, sectionHeights[1]);
            y += sectionHeights[1] + sectionGap;
            this.structureRect = new SignEditorUi.Rect(rightX, y, rightW, sectionHeights[2]);
        }

        int innerX = this.contentRect.x() + s(10);
        int innerW = this.contentRect.width() - s(20);
        int rowGap = pagedUi() ? 8 : s(8);
        int fieldH = pagedUi() ? 20 : s(24);
        int fontW = Math.max(s(100), Math.round(innerW * 0.28F));
        int textW = innerW - fontW - rowGap;
        int row1Y = this.contentRect.y() + (pagedUi() ? 34 : s(compactUi() ? 30 : 48));
        int row2Y = row1Y + fieldH + (pagedUi() ? 8 : s(compactUi() ? 8 : 18));

        this.line1Field = new EditBox(this.font, innerX, row1Y, textW, fieldH, Component.literal("Nom ligne 1"));
        this.line1Field.setMaxLength(48);
        this.line1Field.setValue(this.currentLine1);
        this.addRenderableWidget(this.line1Field);
        this.line1FontRect = new SignEditorUi.Rect(innerX + textW + rowGap, row1Y, fontW, fieldH);

        this.line2Field = new EditBox(this.font, innerX, row2Y, textW, fieldH, Component.literal("Nom ligne 2"));
        this.line2Field.setMaxLength(48);
        this.line2Field.setValue(this.currentLine2);
        this.addRenderableWidget(this.line2Field);
        this.line2FontRect = new SignEditorUi.Rect(innerX + textW + rowGap, row2Y, fontW, fieldH);

        int cInnerX = this.cartoucheRect.x() + s(10);
        int cInnerW = this.cartoucheRect.width() - s(20);
        int cY = this.cartoucheRect.y() + (pagedUi() ? 34 : s(compactUi() ? 30 : 48));
        int typeW = Math.max(s(130), Math.round(cInnerW * 0.42F));
        this.cartoucheTypeRect = new SignEditorUi.Rect(cInnerX, cY, typeW, fieldH);
        this.cartoucheTextField = new EditBox(
                this.font,
                cInnerX + typeW + rowGap,
                cY,
                cInnerW - typeW - rowGap,
                fieldH,
                Component.literal("Texte du cartouche")
        );
        this.cartoucheTextField.setMaxLength(24);
        this.cartoucheTextField.setValue(this.currentCartoucheText);
        this.addRenderableWidget(this.cartoucheTextField);

        this.eb20Rect = new SignEditorUi.Rect(
                this.structureRect.x() + s(10),
                this.structureRect.y() + (pagedUi() ? 34 : s(compactUi() ? 24 : 40)),
                this.structureRect.width() - s(20),
                pagedUi() ? 24 : s(34)
        );

        int actionY = this.windowY + this.windowHeight - s(36);
        int actionW = s(145);
        this.cancelRect = new SignEditorUi.Rect(this.windowX + this.windowWidth - pad - actionW, actionY, actionW, s(28));
        this.applyRect = new SignEditorUi.Rect(this.cancelRect.x() - s(10) - actionW, actionY, actionW, s(28));

        updateCartoucheFieldState();
        updatePagedVisibility();
        this.setInitialFocus(this.line1Field);
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
                this.eb20 ? "EB20" : "EB10",
                "Panneau d'agglomération",
                compactUi() ? "" : "Configuration en direct"
        );

        SignEditorUi.drawEBPreview(
                graphics,
                this.font,
                new SignEditorUi.PreviewBox(this.previewRect.x(), this.previewRect.y(), this.previewRect.width(), this.previewRect.height(), true),
                this.eb20,
                this.line1Field.getValue(),
                this.line2Field.getValue(),
                this.line1Font,
                this.line2Font,
                this.cartoucheType,
                this.cartoucheTextField.getValue()
        );

        if (pagedUi()) {
            SignEditorUi.drawPageTabs(graphics, this.font, this.settingsPageRects, new String[]{"Texte", "Cart.", "Type"}, this.settingsPage, mouseX, mouseY);
        }

        if (!pagedUi() || this.settingsPage == 0) {
            SignEditorUi.drawModernSection(graphics, this.font, this.contentRect, "1. CONTENU", pagedUi() ? "" : "Nom de l'agglomération et style d'écriture");
            if (!compactUi()) {
                SignEditorUi.drawFieldLabel(graphics, this.font, "Ligne 1", this.line1Field.getX(), this.line1Field.getY() - s(12));
                SignEditorUi.drawFieldLabel(graphics, this.font, "Police", this.line1FontRect.x(), this.line1FontRect.y() - s(12));
            }
            SignEditorUi.drawModernButton(graphics, this.font, this.line1FontRect, SignEditorUi.fontLabel(this.line1Font), false, true, mouseX, mouseY);
            if (!compactUi()) {
                SignEditorUi.drawFieldLabel(graphics, this.font, "Ligne 2", this.line2Field.getX(), this.line2Field.getY() - s(12));
                SignEditorUi.drawFieldLabel(graphics, this.font, "Police", this.line2FontRect.x(), this.line2FontRect.y() - s(12));
            }
            SignEditorUi.drawModernButton(graphics, this.font, this.line2FontRect, SignEditorUi.fontLabel(this.line2Font), false, true, mouseX, mouseY);
        }

        if (!pagedUi() || this.settingsPage == 1) {
            SignEditorUi.drawModernSection(graphics, this.font, this.cartoucheRect, "2. CARTOUCHE", pagedUi() ? "" : "Couleur et texte affiché au-dessus du panneau");
            if (!compactUi()) {
                SignEditorUi.drawFieldLabel(graphics, this.font, "Couleur", this.cartoucheTypeRect.x(), this.cartoucheTypeRect.y() - s(12));
            }
            SignEditorUi.drawModernButton(graphics, this.font, this.cartoucheTypeRect, SignEditorUi.cartoucheLabel(this.cartoucheType), this.cartoucheType.isVisible(), true, mouseX, mouseY);
            if (!compactUi()) {
                SignEditorUi.drawFieldLabel(graphics, this.font, "Texte du cartouche", this.cartoucheTextField.getX(), this.cartoucheTextField.getY() - s(12));
            }
        }

        if (!pagedUi() || this.settingsPage == 2) {
            SignEditorUi.drawModernSection(graphics, this.font, this.structureRect, "3. STRUCTURE", pagedUi() ? "" : "Type de panneau d'agglomération");
            SignEditorUi.drawModernToggle(graphics, this.font, this.eb20Rect, "Sortie d'agglomération (EB20)", compactUi() ? "" : (this.eb20 ? "EB20 activé" : "EB10 entrée d'agglomération"), this.eb20, true, mouseX, mouseY);
        }

        SignEditorUi.drawModernButton(graphics, this.font, this.applyRect, "✓  Appliquer", true, true, mouseX, mouseY);
        SignEditorUi.drawModernButton(graphics, this.font, this.cancelRect, "×  Annuler", false, true, mouseX, mouseY);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            double x = event.x();
            double y = event.y();
            if (pagedUi()) {
                for (int i = 0; i < this.settingsPageRects.length; i++) {
                    if (this.settingsPageRects[i] != null && this.settingsPageRects[i].contains(x, y)) {
                        this.settingsPage = i;
                        updatePagedVisibility();
                        return true;
                    }
                }
            }
            if (!pagedUi() || this.settingsPage == 0) {
                if (this.line1FontRect.contains(x, y)) {
                    this.line1Font = this.line1Font.next();
                    return true;
                }
                if (this.line2FontRect.contains(x, y)) {
                    this.line2Font = this.line2Font.next();
                    return true;
                }
            }
            if ((!pagedUi() || this.settingsPage == 1) && this.cartoucheTypeRect.contains(x, y)) {
                this.cartoucheType = this.cartoucheType.next();
                updateCartoucheFieldState();
                return true;
            }
            if ((!pagedUi() || this.settingsPage == 2) && this.eb20Rect.contains(x, y)) {
                this.eb20 = !this.eb20;
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

    private void updateCartoucheFieldState() {
        if (this.cartoucheTextField != null) {
            boolean visible = !pagedUi() || this.settingsPage == 1;
            this.cartoucheTextField.visible = visible;
            this.cartoucheTextField.active = visible && this.cartoucheType.isVisible();
        }
    }

    private void updatePagedVisibility() {
        boolean contentVisible = !pagedUi() || this.settingsPage == 0;
        this.line1Field.visible = contentVisible;
        this.line1Field.active = contentVisible;
        this.line2Field.visible = contentVisible;
        this.line2Field.active = contentVisible;
        updateCartoucheFieldState();
    }

    private boolean compactUi() {
        return SignEditorUi.compactForScale(this.scale);
    }

    private boolean pagedUi() {
        return SignEditorUi.pagedCompactMode(this.scale, this.windowHeight);
    }

    private int s(int value) {
        return SignEditorUi.scaledUi(value, this.scale);
    }

    private void save() {
        ClientPacketDistributor.sendToServer(
                new UpdateEB10TextPayload(
                        this.blockPos,
                        this.line1Field.getValue(),
                        this.line2Field.getValue(),
                        this.line1Font,
                        this.line2Font,
                        this.eb20,
                        this.cartoucheType,
                        this.cartoucheTextField.getValue()
                )
        );
        this.onClose();
    }
}
