package net.xelpy.moreroad.client.screen;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.xelpy.moreroad.block.custom.CartoucheType;
import net.xelpy.moreroad.block.custom.D21APanelData;
import net.xelpy.moreroad.block.custom.D21AType;
import net.xelpy.moreroad.block.custom.RoadTextFont;
import net.xelpy.moreroad.block.entity.D21ABlockEntity;
import net.xelpy.moreroad.network.UpdateD21APayload;

public class D21A2EditScreen extends Screen {

    private final BlockPos blockPos;

    private final D21APanelData[] panels =
            new D21APanelData[D21ABlockEntity.MAX_PANELS];

    private int selectedPanelIndex = 0;

    private boolean panelEnabled = true;
    private boolean doubleLine = false;
    private D21AType selectedType = D21AType.WHITE;
    private boolean arrowRight = false;
    private boolean autorouteLogo = false;
    private RoadTextFont line1Font = RoadTextFont.L1;
    private RoadTextFont line2Font = RoadTextFont.L1;
    private CartoucheType cartoucheType = CartoucheType.NONE;
    private String cartoucheText = "";

    private EditBox line1Field;
    private EditBox line2Field;
    private EditBox distance1Field;
    private EditBox distance2Field;
    private EditBox cartoucheTextField;

    private final Button[] panelButtons =
            new Button[D21ABlockEntity.MAX_PANELS];

    private Button enabledButton;
    private Button formatButton;
    private Button whiteButton;
    private Button greenButton;
    private Button blueButton;
    private Button autorouteLogoButton;
    private Button directionButton;
    private Button line1FontButton;
    private Button line2FontButton;
    private Button cartoucheButton;

    public D21A2EditScreen(
            BlockPos blockPos,
            D21APanelData[] currentPanels,
            CartoucheType currentCartoucheType,
            String currentCartoucheText
    ) {
        super(
                Component.literal(
                        "Ensemble directionnel D21A"
                )
        );

        this.blockPos = blockPos.immutable();
        this.cartoucheType =
                currentCartoucheType == null
                        ? CartoucheType.NONE
                        : currentCartoucheType;
        this.cartoucheText =
                currentCartoucheText == null
                        ? ""
                        : currentCartoucheText;

        for (int i = 0; i < D21ABlockEntity.MAX_PANELS; i++) {
            D21APanelData panel =
                    currentPanels != null && i < currentPanels.length
                            ? currentPanels[i]
                            : null;

            if (panel == null) {
                panel =
                        i == 0
                                ? D21APanelData.firstPanelDefault()
                                : D21APanelData.disabled();
            }

            this.panels[i] = panel;
        }
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        /*
         * V63 - Inventory / editor UX pass
         *
         * Aucun réglage n'est retiré. L'interface est simplement regroupée
         * en quatre zones logiques :
         *  1. état du panneau ;
         *  2. textes ;
         *  3. apparence ;
         *  4. cartouche.
         *
         * Les largeurs sont calculées à partir de la largeur disponible pour
         * rester lisibles même avec une GUI scale élevée.
         */
        int formWidth = Math.min(380, this.width - 24);
        int left = centerX - formWidth / 2;
        int gap = 6;

        /* ========================================================
         * ONGLETS PANNEAUX 1 A 4
         * ======================================================== */

        int tabWidth =
                (
                        formWidth
                                - gap
                                * (D21ABlockEntity.MAX_PANELS - 1)
                )
                        / D21ABlockEntity.MAX_PANELS;

        int tabsY = centerY - 195;

        for (int i = 0; i < D21ABlockEntity.MAX_PANELS; i++) {
            final int panelIndex = i;

            this.panelButtons[i] =
                    Button.builder(
                                    Component.empty(),
                                    button -> selectPanel(panelIndex)
                            )
                            .bounds(
                                    left + i * (tabWidth + gap),
                                    tabsY,
                                    tabWidth,
                                    20
                            )
                            .build();

            this.addRenderableWidget(this.panelButtons[i]);
        }

        /* ========================================================
         * ÉTAT DU PANNEAU
         * ======================================================== */

        addSectionHeader(
                "ÉTAT DU PANNEAU",
                left,
                centerY - 167,
                formWidth
        );

        int halfWidth = (formWidth - gap) / 2;

        this.enabledButton =
                Button.builder(
                                Component.empty(),
                                button -> {
                                    this.panelEnabled = !this.panelEnabled;
                                    updateEnabledButton();
                                    updatePanelButtons();
                                }
                        )
                        .bounds(
                                left,
                                centerY - 146,
                                halfWidth,
                                20
                        )
                        .build();

        this.addRenderableWidget(this.enabledButton);

        this.formatButton =
                Button.builder(
                                Component.empty(),
                                button -> {
                                    this.doubleLine = !this.doubleLine;
                                    updateFormatButton();
                                    updateFieldVisibility();
                                    updatePanelButtons();
                                }
                        )
                        .bounds(
                                left + halfWidth + gap,
                                centerY - 146,
                                halfWidth,
                                20
                        )
                        .build();

        this.addRenderableWidget(this.formatButton);

        /* ========================================================
         * TEXTES
         * ======================================================== */

        addSectionHeader(
                "TEXTES  •  Destination / Police / Km",
                left,
                centerY - 116,
                formWidth
        );

        int lineLabelWidth = 36;
        int fontButtonWidth = 92;
        int distanceWidth = 70;
        int destinationWidth =
                formWidth
                        - lineLabelWidth
                        - fontButtonWidth
                        - distanceWidth
                        - gap * 3;

        addRowLabel(
                "L1",
                left,
                centerY - 95,
                lineLabelWidth
        );

        this.line1Field =
                new EditBox(
                        this.font,
                        left + lineLabelWidth + gap,
                        centerY - 95,
                        destinationWidth,
                        20,
                        Component.literal("Destination ligne 1")
                );

        this.line1Field.setMaxLength(48);
        this.addRenderableWidget(this.line1Field);

        this.line1FontButton =
                Button.builder(
                                Component.empty(),
                                button -> {
                                    this.line1Font = this.line1Font.next();
                                    updateFontButtons();
                                }
                        )
                        .bounds(
                                left + lineLabelWidth + gap + destinationWidth + gap,
                                centerY - 95,
                                fontButtonWidth,
                                20
                        )
                        .build();

        this.addRenderableWidget(this.line1FontButton);

        this.distance1Field =
                new EditBox(
                        this.font,
                        left
                                + lineLabelWidth
                                + gap
                                + destinationWidth
                                + gap
                                + fontButtonWidth
                                + gap,
                        centerY - 95,
                        distanceWidth,
                        20,
                        Component.literal("Distance ligne 1")
                );

        this.distance1Field.setMaxLength(8);
        this.addRenderableWidget(this.distance1Field);

        addRowLabel(
                "L2",
                left,
                centerY - 69,
                lineLabelWidth
        );

        this.line2Field =
                new EditBox(
                        this.font,
                        left + lineLabelWidth + gap,
                        centerY - 69,
                        destinationWidth,
                        20,
                        Component.literal("Destination ligne 2")
                );

        this.line2Field.setMaxLength(48);
        this.addRenderableWidget(this.line2Field);

        this.line2FontButton =
                Button.builder(
                                Component.empty(),
                                button -> {
                                    this.line2Font = this.line2Font.next();
                                    updateFontButtons();
                                }
                        )
                        .bounds(
                                left + lineLabelWidth + gap + destinationWidth + gap,
                                centerY - 69,
                                fontButtonWidth,
                                20
                        )
                        .build();

        this.addRenderableWidget(this.line2FontButton);

        this.distance2Field =
                new EditBox(
                        this.font,
                        left
                                + lineLabelWidth
                                + gap
                                + destinationWidth
                                + gap
                                + fontButtonWidth
                                + gap,
                        centerY - 69,
                        distanceWidth,
                        20,
                        Component.literal("Distance ligne 2")
                );

        this.distance2Field.setMaxLength(8);
        this.addRenderableWidget(this.distance2Field);

        /* ========================================================
         * APPARENCE
         * ======================================================== */

        addSectionHeader(
                "APPARENCE  •  Couleur / Logo / Direction",
                left,
                centerY - 39,
                formWidth
        );

        int colorWidth = (formWidth - gap * 2) / 3;

        this.whiteButton =
                Button.builder(
                                Component.literal("Blanc"),
                                button -> selectType(D21AType.WHITE)
                        )
                        .bounds(
                                left,
                                centerY - 18,
                                colorWidth,
                                20
                        )
                        .build();

        this.addRenderableWidget(this.whiteButton);

        this.greenButton =
                Button.builder(
                                Component.literal("Vert"),
                                button -> selectType(D21AType.GREEN)
                        )
                        .bounds(
                                left + colorWidth + gap,
                                centerY - 18,
                                colorWidth,
                                20
                        )
                        .build();

        this.addRenderableWidget(this.greenButton);

        this.blueButton =
                Button.builder(
                                Component.literal("Bleu"),
                                button -> selectType(D21AType.BLUE)
                        )
                        .bounds(
                                left + (colorWidth + gap) * 2,
                                centerY - 18,
                                colorWidth,
                                20
                        )
                        .build();

        this.addRenderableWidget(this.blueButton);

        this.autorouteLogoButton =
                Button.builder(
                                Component.empty(),
                                button -> {
                                    this.autorouteLogo = !this.autorouteLogo;
                                    updateAutorouteLogoButton();
                                }
                        )
                        .bounds(
                                left,
                                centerY + 8,
                                halfWidth,
                                20
                        )
                        .build();

        this.addRenderableWidget(this.autorouteLogoButton);

        this.directionButton =
                Button.builder(
                                Component.empty(),
                                button -> {
                                    this.arrowRight = !this.arrowRight;
                                    updateDirectionButton();
                                }
                        )
                        .bounds(
                                left + halfWidth + gap,
                                centerY + 8,
                                halfWidth,
                                20
                        )
                        .build();

        this.addRenderableWidget(this.directionButton);

        /* ========================================================
         * CARTOUCHE
         * ======================================================== */

        addSectionHeader(
                "CARTOUCHE  •  Type / Texte",
                left,
                centerY + 38,
                formWidth
        );

        this.cartoucheButton =
                Button.builder(
                                Component.empty(),
                                button -> {
                                    this.cartoucheType = this.cartoucheType.next();
                                    updateCartoucheButton();
                                }
                        )
                        .bounds(
                                left,
                                centerY + 59,
                                formWidth,
                                20
                        )
                        .build();

        this.addRenderableWidget(this.cartoucheButton);

        this.cartoucheTextField =
                new EditBox(
                        this.font,
                        left,
                        centerY + 85,
                        formWidth,
                        20,
                        Component.literal("Texte du cartouche")
                );

        this.cartoucheTextField.setMaxLength(24);
        this.cartoucheTextField.setValue(this.cartoucheText);
        this.addRenderableWidget(this.cartoucheTextField);

        /* ========================================================
         * ACTIONS
         * ======================================================== */

        this.addRenderableWidget(
                Button.builder(
                                Component.literal("Valider les modifications"),
                                button -> save()
                        )
                        .bounds(
                                left,
                                centerY + 115,
                                halfWidth,
                                20
                        )
                        .build()
        );

        this.addRenderableWidget(
                Button.builder(
                                Component.literal("Annuler"),
                                button -> this.onClose()
                        )
                        .bounds(
                                left + halfWidth + gap,
                                centerY + 115,
                                halfWidth,
                                20
                        )
                        .build()
        );

        loadSelectedPanelIntoWidgets();
        updateCartoucheButton();
        this.setInitialFocus(this.line1Field);
    }

    private void selectPanel(int newIndex) {
        if (
                newIndex < 0
                        || newIndex >= D21ABlockEntity.MAX_PANELS
                        || newIndex == this.selectedPanelIndex
        ) {
            return;
        }

        storeSelectedPanelFromWidgets();
        this.selectedPanelIndex = newIndex;
        loadSelectedPanelIntoWidgets();
        updateCartoucheButton();
        this.setInitialFocus(this.line1Field);
    }

    private void selectType(D21AType type) {
        this.selectedType =
                type == null
                        ? D21AType.WHITE
                        : type;

        if (this.selectedType == D21AType.WHITE) {
            this.autorouteLogo = false;
        }

        updateTypeButtons();
        updateAutorouteLogoButton();
    }

    private void storeSelectedPanelFromWidgets() {
        if (
                this.line1Field == null
                        || this.line2Field == null
                        || this.distance1Field == null
                        || this.distance2Field == null
        ) {
            return;
        }

        this.panels[this.selectedPanelIndex] =
                new D21APanelData(
                        this.panelEnabled,
                        this.line1Field.getValue(),
                        this.line2Field.getValue(),
                        this.distance1Field.getValue(),
                        this.distance2Field.getValue(),
                        this.selectedType,
                        this.arrowRight,
                        this.autorouteLogo,
                        this.doubleLine,
                        this.line1Font,
                        this.line2Font
                );
    }

    private void loadSelectedPanelIntoWidgets() {
        D21APanelData panel =
                this.panels[this.selectedPanelIndex];

        this.panelEnabled = panel.enabled();
        this.doubleLine = panel.doubleLine();
        this.selectedType = panel.type();
        this.arrowRight = panel.arrowRight();
        this.autorouteLogo = panel.autorouteLogo();
        this.line1Font = panel.line1Font();
        this.line2Font = panel.line2Font();

        if (this.selectedType == D21AType.WHITE) {
            this.autorouteLogo = false;
        }

        this.line1Field.setValue(panel.line1());
        this.line2Field.setValue(panel.line2());
        this.distance1Field.setValue(panel.distance1());
        this.distance2Field.setValue(panel.distance2());

        updateEnabledButton();
        updateFormatButton();
        updateFieldVisibility();
        updateTypeButtons();
        updateAutorouteLogoButton();
        updateDirectionButton();
        updateFontButtons();
        updatePanelButtons();
    }

    private void addSectionHeader(
            String label,
            int x,
            int y,
            int width
    ) {
        Button header =
                Button.builder(
                                Component.literal("— " + label + " —"),
                                button -> {
                                }
                        )
                        .bounds(
                                x,
                                y,
                                width,
                                16
                        )
                        .build();

        header.active = false;
        this.addRenderableWidget(header);
    }

    private void addRowLabel(
            String label,
            int x,
            int y,
            int width
    ) {
        Button rowLabel =
                Button.builder(
                                Component.literal(label),
                                button -> {
                                }
                        )
                        .bounds(
                                x,
                                y,
                                width,
                                20
                        )
                        .build();

        rowLabel.active = false;
        this.addRenderableWidget(rowLabel);
    }

    private void updateFieldVisibility() {
        this.line2Field.visible = this.doubleLine;
        this.line2Field.active = this.doubleLine;

        if (this.line2FontButton != null) {
            this.line2FontButton.visible = this.doubleLine;
            this.line2FontButton.active = this.doubleLine;
        }

        this.distance2Field.visible = this.doubleLine;
        this.distance2Field.active = this.doubleLine;
    }

    private void updateFontButtons() {
        if (this.line1FontButton != null) {
            this.line1FontButton.setMessage(
                    Component.literal(
                            this.line1Font == RoadTextFont.L4
                                    ? "Police : L4"
                                    : "Police : L1"
                    )
            );
        }

        if (this.line2FontButton != null) {
            this.line2FontButton.setMessage(
                    Component.literal(
                            this.line2Font == RoadTextFont.L4
                                    ? "Police : L4"
                                    : "Police : L1"
                    )
            );
        }
    }

    private void updatePanelButtons() {
        for (int i = 0; i < D21ABlockEntity.MAX_PANELS; i++) {
            if (this.panelButtons[i] == null) {
                continue;
            }

            boolean enabled =
                    i == this.selectedPanelIndex
                            ? this.panelEnabled
                            : this.panels[i].enabled();

            boolean isDouble =
                    i == this.selectedPanelIndex
                            ? this.doubleLine
                            : this.panels[i].doubleLine();

            String label =
                    "P" + (i + 1)
                            + " "
                            + (isDouble ? "2L" : "1L")
                            + (enabled ? "" : " OFF");

            if (i == this.selectedPanelIndex) {
                label = "[" + label + "]";
            }

            this.panelButtons[i].setMessage(
                    Component.literal(label)
            );
        }
    }

    private void updateEnabledButton() {
        this.enabledButton.setMessage(
                Component.literal(
                        this.panelEnabled
                                ? "Actif : Oui"
                                : "Actif : Non"
                )
        );
    }

    private void updateFormatButton() {
        this.formatButton.setMessage(
                Component.literal(
                        this.doubleLine
                                ? "[ Double • 2 lignes ]"
                                : "Simple • 1 ligne"
                )
        );
    }

    private void updateTypeButtons() {
        this.whiteButton.setMessage(
                Component.literal(
                        this.selectedType == D21AType.WHITE
                                ? "[ Blanc ]"
                                : "Blanc"
                )
        );

        this.greenButton.setMessage(
                Component.literal(
                        this.selectedType == D21AType.GREEN
                                ? "[ Vert ]"
                                : "Vert"
                )
        );

        this.blueButton.setMessage(
                Component.literal(
                        this.selectedType == D21AType.BLUE
                                ? "[ Bleu ]"
                                : "Bleu"
                )
        );
    }

    private void updateAutorouteLogoButton() {
        boolean allowed =
                this.selectedType == D21AType.GREEN
                        || this.selectedType == D21AType.BLUE;

        if (!allowed) {
            this.autorouteLogo = false;
        }

        this.autorouteLogoButton.visible = allowed;
        this.autorouteLogoButton.active = allowed;

        this.autorouteLogoButton.setMessage(
                Component.literal(
                        this.autorouteLogo
                                ? "[ Logo autoroute : Oui ]"
                                : "Logo autoroute : Non"
                )
        );
    }

    private Component getDirectionText() {
        if (this.arrowRight) {
            return Component.literal(
                    "Direction : Droite >"
            );
        }

        return Component.literal(
                "< Direction : Gauche"
        );
    }

    private void updateDirectionButton() {
        this.directionButton.setMessage(
                getDirectionText()
        );
    }

    private void updateCartoucheButton() {
        if (this.cartoucheButton == null) {
            return;
        }

        this.cartoucheButton.setMessage(
                Component.literal(
                        "Cartouche : "
                                + this.cartoucheType.getDisplayName()
                                + (this.cartoucheType.isVisible()
                                ? " | texte ci-dessous"
                                : "")
                )
        );

        if (this.cartoucheTextField != null) {
            this.cartoucheTextField.active = this.cartoucheType.isVisible();
        }
    }

    private void save() {
        storeSelectedPanelFromWidgets();

        ClientPacketDistributor.sendToServer(
                new UpdateD21APayload(
                        this.blockPos,
                        this.panels[0],
                        this.panels[1],
                        this.panels[2],
                        this.panels[3],
                        this.cartoucheType,
                        this.cartoucheTextField.getValue()
                )
        );

        this.onClose();
    }
}
