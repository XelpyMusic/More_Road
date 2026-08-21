package net.xelpy.moreroad.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.xelpy.moreroad.block.custom.D42bBranchData;
import net.xelpy.moreroad.block.custom.D42bLabelColor;
import net.xelpy.moreroad.block.custom.RoadTextFont;
import net.xelpy.moreroad.block.entity.D42bBlockEntity;
import net.xelpy.moreroad.network.UpdateD42bPayload;

public class D42bEditScreen extends Screen {

    private final BlockPos blockPos;
    private final D42bBranchData[] branches = new D42bBranchData[D42bBlockEntity.MAX_BRANCHES];
    private final String initialDistanceText;

    private int selectedBranch;
    private boolean branchEnabled;
    private RoadTextFont line1Font = RoadTextFont.L1;
    private RoadTextFont line2Font = RoadTextFont.L1;
    private D42bLabelColor line1Color = D42bLabelColor.NONE;
    private D42bLabelColor line2Color = D42bLabelColor.NONE;

    private EditBox angleField;
    private EditBox line1Field;
    private EditBox line2Field;
    private EditBox distanceField;

    private final SignEditorUi.Rect[] branchRects = new SignEditorUi.Rect[D42bBlockEntity.MAX_BRANCHES];
    private final SignEditorUi.Rect[] branchToggleRects = new SignEditorUi.Rect[D42bBlockEntity.MAX_BRANCHES];
    private SignEditorUi.Rect previewRect;
    private SignEditorUi.Rect geometryRect;
    private SignEditorUi.Rect contentRect;
    private SignEditorUi.Rect distanceRect;
    private SignEditorUi.Rect line1FontRect;
    private SignEditorUi.Rect line2FontRect;
    private SignEditorUi.Rect line1ColorRect;
    private SignEditorUi.Rect line2ColorRect;
    private SignEditorUi.Rect applyRect;
    private SignEditorUi.Rect cancelRect;

    private final SignEditorUi.Rect[] settingsPageRects = new SignEditorUi.Rect[3];
    private int settingsPage = 0;

    private int windowX;
    private int windowY;
    private int windowWidth;
    private int windowHeight;
    private float scale = 1.0F;

    public D42bEditScreen(BlockPos blockPos, D42bBranchData[] currentBranches, String distanceText) {
        super(Component.literal("D42b — Giratoire"));
        this.blockPos = blockPos.immutable();
        for (int i = 0; i < D42bBlockEntity.MAX_BRANCHES; i++) {
            this.branches[i] = currentBranches != null && i < currentBranches.length && currentBranches[i] != null
                    ? currentBranches[i]
                    : D42bBranchData.defaultForIndex(i);
        }
        this.initialDistanceText = distanceText == null ? "" : distanceText;
    }

    @Override
    protected void init() {
        super.init();

        int marginX = Math.max(8, Math.min(26, this.width / 32));
        int marginY = Math.max(8, Math.min(20, this.height / 32));
        this.windowX = marginX;
        this.windowY = marginY;
        this.windowWidth = this.width - marginX * 2;
        this.windowHeight = this.height - marginY * 2;
        this.scale = SignEditorUi.adaptiveEditorScale(this.windowWidth, this.windowHeight, 1180.0F, 750.0F);

        boolean tight = SignEditorUi.tightForScale(this.scale, this.windowHeight);
        boolean ultraTight = SignEditorUi.ultraTightForScale(this.scale, this.windowHeight);
        int pad = s(ultraTight ? 10 : tight ? 12 : 16);
        int header = SignEditorUi.adaptiveHeaderHeight(this.scale, true);
        int tabsH = SignEditorUi.adaptiveTabsHeight(this.scale);
        int footer = SignEditorUi.adaptiveFooterHeight(this.scale);
        int gap = s(tight ? 9 : 14);
        int sectionGap = s(ultraTight ? 5 : tight ? 7 : 12);
        int bodyY = this.windowY + header + tabsH;
        int bodyH = this.windowHeight - header - tabsH - footer;

        int tabsGap = s(6);
        int tabsW = this.windowWidth - pad * 2;
        int tabW = (tabsW - tabsGap * (D42bBlockEntity.MAX_BRANCHES - 1)) / D42bBlockEntity.MAX_BRANCHES;
        int tabY = this.windowY + header;
        for (int i = 0; i < this.branchRects.length; i++) {
            int tx = this.windowX + pad + i * (tabW + tabsGap);
            this.branchRects[i] = new SignEditorUi.Rect(tx, tabY, tabW, s(32));
            this.branchToggleRects[i] = new SignEditorUi.Rect(tx + tabW - s(34), tabY + s(7), s(28), s(18));
        }

        int leftW = Math.max(s(330), Math.round((this.windowWidth - pad * 2 - gap) * 0.48F));
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
            this.geometryRect = fullCard;
            this.contentRect = fullCard;
            this.distanceRect = fullCard;
        } else {
            int availableSections = Math.max(130, bodyH - s(ultraTight ? 2 : 8));
            int[] sectionHeights = SignEditorUi.fitSections(
                    availableSections,
                    sectionGap,
                    new float[]{0.22F, 0.55F, 0.23F},
                    new int[]{
                            s(ultraTight ? 52 : tight ? 66 : 92),
                            s(ultraTight ? 96 : tight ? 124 : 180),
                            s(ultraTight ? 50 : tight ? 62 : 74)
                    }
            );
            int y = bodyY;
            this.geometryRect = new SignEditorUi.Rect(rightX, y, rightW, sectionHeights[0]);
            y += sectionHeights[0] + sectionGap;
            this.contentRect = new SignEditorUi.Rect(rightX, y, rightW, sectionHeights[1]);
            y += sectionHeights[1] + sectionGap;
            this.distanceRect = new SignEditorUi.Rect(rightX, y, rightW, sectionHeights[2]);
        }

        int fieldH = pagedUi() ? 20 : s(24);
        int innerX = this.geometryRect.x() + s(10);
        int innerW = this.geometryRect.width() - s(20);
        this.angleField = new EditBox(this.font, innerX, this.geometryRect.y() + (pagedUi() ? 34 : s(compactUi() ? 28 : 48)), innerW, fieldH, Component.literal("Angle"));
        this.angleField.setMaxLength(4);
        this.addRenderableWidget(this.angleField);

        int cInnerX = this.contentRect.x() + s(10);
        int cInnerW = this.contentRect.width() - s(20);
        int smallGap = pagedUi() ? 8 : s(8);
        int fontW = Math.max(s(88), Math.round(cInnerW * 0.22F));
        int colorW = Math.max(s(82), Math.round(cInnerW * 0.20F));
        int textW = cInnerW - fontW - colorW - smallGap * 2;
        int row1Y = this.contentRect.y() + (pagedUi() ? 34 : s(compactUi() ? 30 : 48));
        int row2Y = row1Y + fieldH + (pagedUi() ? 8 : s(compactUi() ? 10 : 22));

        this.line1Field = new EditBox(this.font, cInnerX, row1Y, textW, fieldH, Component.literal("Destination ligne 1"));
        this.line1Field.setMaxLength(48);
        this.addRenderableWidget(this.line1Field);
        this.line1FontRect = new SignEditorUi.Rect(cInnerX + textW + smallGap, row1Y, fontW, fieldH);
        this.line1ColorRect = new SignEditorUi.Rect(this.line1FontRect.x() + fontW + smallGap, row1Y, colorW, fieldH);

        this.line2Field = new EditBox(this.font, cInnerX, row2Y, textW, fieldH, Component.literal("Destination ligne 2"));
        this.line2Field.setMaxLength(48);
        this.addRenderableWidget(this.line2Field);
        this.line2FontRect = new SignEditorUi.Rect(cInnerX + textW + smallGap, row2Y, fontW, fieldH);
        this.line2ColorRect = new SignEditorUi.Rect(this.line2FontRect.x() + fontW + smallGap, row2Y, colorW, fieldH);

        int dInnerX = this.distanceRect.x() + s(10);
        int dInnerW = this.distanceRect.width() - s(20);
        this.distanceField = new EditBox(this.font, dInnerX, this.distanceRect.y() + (pagedUi() ? 34 : s(compactUi() ? 28 : 44)), dInnerW, fieldH, Component.literal("Distance"));
        this.distanceField.setMaxLength(12);
        this.distanceField.setValue(this.initialDistanceText);
        this.addRenderableWidget(this.distanceField);

        int actionY = this.windowY + this.windowHeight - s(36);
        int actionW = s(145);
        this.cancelRect = new SignEditorUi.Rect(this.windowX + this.windowWidth - pad - actionW, actionY, actionW, s(28));
        this.applyRect = new SignEditorUi.Rect(this.cancelRect.x() - s(10) - actionW, actionY, actionW, s(28));

        loadSelectedBranchIntoWidgets();
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
                "D42b",
                "Éditeur de giratoire",
                compactUi() ? "" : "Configuration en direct"
        );

        for (int i = 0; i < this.branchRects.length; i++) {
            D42bBranchData branch = i == this.selectedBranch ? currentBranchFromWidgets() : this.branches[i];
            SignEditorUi.drawModernButton(
                    graphics,
                    this.font,
                    this.branchRects[i],
                    compactUi() ? "Sortie " + (i + 1) : "Sortie " + (i + 1) + "  •  " + branch.angleDegrees() + "°",
                    i == this.selectedBranch,
                    true,
                    mouseX,
                    mouseY
            );
            SignEditorUi.drawModernToggle(graphics, this.font, this.branchToggleRects[i], "", "", branch.enabled(), true, mouseX, mouseY);
        }

        SignEditorUi.drawD42FullPreview(
                graphics,
                this.font,
                new SignEditorUi.PreviewBox(this.previewRect.x(), this.previewRect.y(), this.previewRect.width(), this.previewRect.height(), true),
                previewBranches(),
                this.selectedBranch,
                this.distanceField.getValue()
        );

        if (pagedUi()) {
            SignEditorUi.drawPageTabs(graphics, this.font, this.settingsPageRects, new String[]{"Angle", "Texte", "Distance"}, this.settingsPage, mouseX, mouseY);
        }

        if (!pagedUi() || this.settingsPage == 0) {
            SignEditorUi.drawModernSection(graphics, this.font, this.geometryRect, "1. GÉOMÉTRIE", pagedUi() ? "" : "Angle de la sortie sélectionnée");
            if (!compactUi()) {
                SignEditorUi.drawFieldLabel(graphics, this.font, "Angle (-170° à 170°)", this.angleField.getX(), this.angleField.getY() - s(12));
            }
        }

        if (!pagedUi() || this.settingsPage == 1) {
            SignEditorUi.drawModernSection(graphics, this.font, this.contentRect, "2. CONTENU", pagedUi() ? "" : "Destinations, police et couleur de fond");
            if (!compactUi()) {
                SignEditorUi.drawFieldLabel(graphics, this.font, "Destination 1", this.line1Field.getX(), this.line1Field.getY() - s(12));
                SignEditorUi.drawFieldLabel(graphics, this.font, "Police", this.line1FontRect.x(), this.line1FontRect.y() - s(12));
                SignEditorUi.drawFieldLabel(graphics, this.font, "Fond", this.line1ColorRect.x(), this.line1ColorRect.y() - s(12));
            }
            SignEditorUi.drawModernButton(graphics, this.font, this.line1FontRect, SignEditorUi.fontLabel(this.line1Font), false, true, mouseX, mouseY);
            SignEditorUi.drawModernButton(graphics, this.font, this.line1ColorRect, this.line1Color.displayName(), this.line1Color != D42bLabelColor.NONE, true, mouseX, mouseY);

            if (!compactUi()) {
                SignEditorUi.drawFieldLabel(graphics, this.font, "Destination 2", this.line2Field.getX(), this.line2Field.getY() - s(12));
                SignEditorUi.drawFieldLabel(graphics, this.font, "Police", this.line2FontRect.x(), this.line2FontRect.y() - s(12));
                SignEditorUi.drawFieldLabel(graphics, this.font, "Fond", this.line2ColorRect.x(), this.line2ColorRect.y() - s(12));
            }
            SignEditorUi.drawModernButton(graphics, this.font, this.line2FontRect, SignEditorUi.fontLabel(this.line2Font), false, true, mouseX, mouseY);
            SignEditorUi.drawModernButton(graphics, this.font, this.line2ColorRect, this.line2Color.displayName(), this.line2Color != D42bLabelColor.NONE, true, mouseX, mouseY);
        }

        if (!pagedUi() || this.settingsPage == 2) {
            SignEditorUi.drawModernSection(graphics, this.font, this.distanceRect, "3. DISTANCE", pagedUi() ? "" : "Indication affichée en bas du panneau");
            if (!compactUi()) {
                SignEditorUi.drawFieldLabel(graphics, this.font, "Distance", this.distanceField.getX(), this.distanceField.getY() - s(12));
            }
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
            for (int i = 0; i < this.branchRects.length; i++) {
                if (this.branchToggleRects[i].contains(x, y)) {
                    toggleBranchEnabled(i);
                    return true;
                }
                if (this.branchRects[i].contains(x, y)) {
                    selectBranch(i);
                    return true;
                }
            }
            if (pagedUi()) {
                for (int i = 0; i < this.settingsPageRects.length; i++) {
                    if (this.settingsPageRects[i] != null && this.settingsPageRects[i].contains(x, y)) {
                        this.settingsPage = i;
                        updatePagedVisibility();
                        return true;
                    }
                }
            }
            if (!pagedUi() || this.settingsPage == 1) {
                if (this.line1FontRect.contains(x, y)) {
                    this.line1Font = this.line1Font.next();
                    return true;
                }
                if (this.line2FontRect.contains(x, y)) {
                    this.line2Font = this.line2Font.next();
                    return true;
                }
                if (this.line1ColorRect.contains(x, y)) {
                    this.line1Color = this.line1Color.next();
                    return true;
                }
                if (this.line2ColorRect.contains(x, y)) {
                    this.line2Color = this.line2Color.next();
                    return true;
                }
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

    private D42bBranchData[] previewBranches() {
        D42bBranchData[] result = this.branches.clone();
        result[this.selectedBranch] = currentBranchFromWidgets();
        return result;
    }

    private D42bBranchData currentBranchFromWidgets() {
        return new D42bBranchData(
                this.branchEnabled,
                parseAngle(this.angleField == null ? "0" : this.angleField.getValue()),
                this.line1Field == null ? "" : this.line1Field.getValue(),
                this.line2Field == null ? "" : this.line2Field.getValue(),
                this.line1Font,
                this.line2Font,
                this.line1Color,
                this.line2Color
        );
    }

    private void toggleBranchEnabled(int index) {
        if (index < 0 || index >= this.branches.length) {
            return;
        }
        if (index == this.selectedBranch) {
            this.branchEnabled = !this.branchEnabled;
            return;
        }
        D42bBranchData b = this.branches[index];
        this.branches[index] = new D42bBranchData(!b.enabled(), b.angleDegrees(), b.line1(), b.line2(), b.line1Font(), b.line2Font(), b.line1Color(), b.line2Color());
    }

    private void selectBranch(int index) {
        syncWidgetsIntoSelectedBranch();
        this.selectedBranch = Math.max(0, Math.min(D42bBlockEntity.MAX_BRANCHES - 1, index));
        loadSelectedBranchIntoWidgets();
    }

    private void loadSelectedBranchIntoWidgets() {
        D42bBranchData branch = this.branches[this.selectedBranch];
        this.branchEnabled = branch.enabled();
        this.line1Font = branch.line1Font();
        this.line2Font = branch.line2Font();
        this.line1Color = branch.line1Color();
        this.line2Color = branch.line2Color();
        this.angleField.setValue(Integer.toString(branch.angleDegrees()));
        this.line1Field.setValue(branch.line1());
        this.line2Field.setValue(branch.line2());
    }

    private void syncWidgetsIntoSelectedBranch() {
        if (this.angleField == null) {
            return;
        }
        this.branches[this.selectedBranch] = currentBranchFromWidgets();
    }

    private int parseAngle(String value) {
        try {
            int angle = Integer.parseInt(value.strip());
            return Math.max(D42bBranchData.MIN_ANGLE, Math.min(D42bBranchData.MAX_ANGLE, angle));
        } catch (NumberFormatException ignored) {
            return this.branches[this.selectedBranch].angleDegrees();
        }
    }

    private boolean compactUi() {
        return SignEditorUi.compactForScale(this.scale);
    }

    private boolean pagedUi() {
        return SignEditorUi.pagedCompactMode(this.scale, this.windowHeight);
    }

    private void updatePagedVisibility() {
        boolean geometryVisible = !pagedUi() || this.settingsPage == 0;
        boolean contentVisible = !pagedUi() || this.settingsPage == 1;
        boolean distanceVisible = !pagedUi() || this.settingsPage == 2;
        this.angleField.visible = geometryVisible;
        this.angleField.active = geometryVisible;
        this.line1Field.visible = contentVisible;
        this.line1Field.active = contentVisible;
        this.line2Field.visible = contentVisible;
        this.line2Field.active = contentVisible;
        this.distanceField.visible = distanceVisible;
        this.distanceField.active = distanceVisible;
    }

    private int s(int value) {
        return SignEditorUi.scaledUi(value, this.scale);
    }

    private void save() {
        syncWidgetsIntoSelectedBranch();
        ClientPacketDistributor.sendToServer(
                new UpdateD42bPayload(
                        this.blockPos,
                        this.distanceField.getValue(),
                        this.branches[0],
                        this.branches[1],
                        this.branches[2],
                        this.branches[3],
                        this.branches[4],
                        this.branches[5]
                )
        );
        this.onClose();
    }
}
