package net.xelpy.moreroad.client.screen;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.xelpy.moreroad.block.custom.CartoucheType;
import net.xelpy.moreroad.block.custom.D21AType;
import net.xelpy.moreroad.block.custom.D61AArrowDirection;
import net.xelpy.moreroad.block.custom.D61AArrowPosition;
import net.xelpy.moreroad.block.custom.D61APanelData;
import net.xelpy.moreroad.block.custom.RoadTextFont;
import net.xelpy.moreroad.block.entity.D61ABlockEntity;
import net.xelpy.moreroad.network.UpdateD61APayload;

public class D61AEditScreen extends Screen {

    private final BlockPos blockPos;

    private final D61APanelData[] panels =
            new D61APanelData[D61ABlockEntity.MAX_PANELS];

    private int selectedPanelIndex = 0;

    private boolean panelEnabled = true;
    private boolean doubleLine = false;
    private D21AType selectedType = D21AType.WHITE;
    private boolean autorouteLogo = false;
    private RoadTextFont line1Font = RoadTextFont.L1;
    private RoadTextFont line2Font = RoadTextFont.L1;

    private boolean arrowEnabled = false;
    private D61AArrowPosition arrowPosition = D61AArrowPosition.RIGHT;
    private D61AArrowDirection arrowDirection = D61AArrowDirection.UP;
    private CartoucheType cartoucheType = CartoucheType.NONE;
    private String cartoucheText = "";

    private EditBox line1Field;
    private EditBox line2Field;
    private EditBox distance1Field;
    private EditBox distance2Field;
    private EditBox cartoucheTextField;

    private final Button[] panelButtons =
            new Button[D61ABlockEntity.MAX_PANELS];

    private final Button[] directionButtons =
            new Button[D61AArrowDirection.values().length];

    private Button enabledButton;
    private Button formatButton;
    private Button whiteButton;
    private Button greenButton;
    private Button blueButton;
    private Button autorouteLogoButton;
    private Button arrowEnabledButton;
    private Button arrowPositionButton;
    private Button line1FontButton;
    private Button line2FontButton;
    private Button cartoucheButton;

    public D61AEditScreen(
            BlockPos blockPos,
            D61APanelData[] currentPanels,
            CartoucheType currentCartoucheType,
            String currentCartoucheText
    ) {
        super(Component.literal("Ensemble directionnel D61A"));

        this.blockPos = blockPos.immutable();
        this.cartoucheType =
                currentCartoucheType == null
                        ? CartoucheType.NONE
                        : currentCartoucheType;
        this.cartoucheText =
                currentCartoucheText == null
                        ? ""
                        : currentCartoucheText;

        for (int i = 0; i < D61ABlockEntity.MAX_PANELS; i++) {
            D61APanelData panel =
                    currentPanels != null && i < currentPanels.length
                            ? currentPanels[i]
                            : null;

            if (panel == null) {
                panel =
                        i == 0
                                ? D61APanelData.firstPanelDefault()
                                : D61APanelData.disabled();
            }

            D21AType type = sanitizeType(panel.type());

            this.panels[i] = new D61APanelData(
                    panel.enabled(),
                    panel.line1(),
                    panel.line2(),
                    panel.distance1(),
                    panel.distance2(),
                    type,
                    panel.doubleLine(),
                    panel.arrowEnabled(),
                    panel.arrowPosition(),
                    panel.arrowDirection(),
                    type != D21AType.WHITE && panel.autorouteLogo(),
                    panel.line1Font(),
                    panel.line2Font()
            );
        }
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int formWidth = Math.min(380, this.width - 24);
        int left = centerX - formWidth / 2;
        int gap = 6;
        int halfWidth = (formWidth - gap) / 2;

        /* ========================================================
         * ONGLETS PANNEAUX 1 A 4
         * ======================================================== */

        int tabWidth =
                (
                        formWidth
                                - gap
                                * (D61ABlockEntity.MAX_PANELS - 1)
                )
                        / D61ABlockEntity.MAX_PANELS;

        int tabsY = centerY - 218;

        for (int i = 0; i < D61ABlockEntity.MAX_PANELS; i++) {
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
                centerY - 190,
                formWidth
        );

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
                                centerY - 169,
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
                                centerY - 169,
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
                centerY - 139,
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
                centerY - 118,
                lineLabelWidth
        );

        this.line1Field =
                new EditBox(
                        this.font,
                        left + lineLabelWidth + gap,
                        centerY - 118,
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
                                centerY - 118,
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
                        centerY - 118,
                        distanceWidth,
                        20,
                        Component.literal("Distance ligne 1")
                );
        this.distance1Field.setMaxLength(8);
        this.addRenderableWidget(this.distance1Field);

        addRowLabel(
                "L2",
                left,
                centerY - 92,
                lineLabelWidth
        );

        this.line2Field =
                new EditBox(
                        this.font,
                        left + lineLabelWidth + gap,
                        centerY - 92,
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
                                centerY - 92,
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
                        centerY - 92,
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
                "APPARENCE  •  Couleur / Logo",
                left,
                centerY - 62,
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
                                centerY - 41,
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
                                centerY - 41,
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
                                centerY - 41,
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
                                    updateAutorouteButton();
                                }
                        )
                        .bounds(
                                left,
                                centerY - 15,
                                formWidth,
                                20
                        )
                        .build();
        this.addRenderableWidget(this.autorouteLogoButton);

        /* ========================================================
         * FLÈCHE
         * ======================================================== */

        addSectionHeader(
                "FLÈCHE  •  Activation / Position / Direction",
                left,
                centerY + 15,
                formWidth
        );

        this.arrowEnabledButton =
                Button.builder(
                                Component.empty(),
                                button -> {
                                    this.arrowEnabled = !this.arrowEnabled;
                                    updateFieldVisibility();
                                    updateArrowControls();
                                    updatePanelButtons();
                                }
                        )
                        .bounds(
                                left,
                                centerY + 36,
                                halfWidth,
                                20
                        )
                        .build();
        this.addRenderableWidget(this.arrowEnabledButton);

        this.arrowPositionButton =
                Button.builder(
                                Component.empty(),
                                button -> {
                                    this.arrowPosition =
                                            this.arrowPosition.opposite();
                                    updateArrowControls();
                                }
                        )
                        .bounds(
                                left + halfWidth + gap,
                                centerY + 36,
                                halfWidth,
                                20
                        )
                        .build();
        this.addRenderableWidget(this.arrowPositionButton);

        D61AArrowDirection[] directions =
                D61AArrowDirection.values();

        int directionGap = 4;
        int directionWidth =
                (
                        formWidth
                                - directionGap
                                * (directions.length - 1)
                )
                        / directions.length;

        int directionY = centerY + 62;

        for (int i = 0; i < directions.length; i++) {
            final D61AArrowDirection direction = directions[i];

            this.directionButtons[i] =
                    Button.builder(
                                    Component.literal(direction.symbol()),
                                    button -> {
                                        this.arrowDirection = direction;
                                        updateArrowControls();
                                    }
                            )
                            .bounds(
                                    left
                                            + i
                                            * (
                                            directionWidth
                                                    + directionGap
                                    ),
                                    directionY,
                                    directionWidth,
                                    20
                            )
                            .build();

            this.addRenderableWidget(this.directionButtons[i]);
        }

        /* ========================================================
         * CARTOUCHE
         * ======================================================== */

        addSectionHeader(
                "CARTOUCHE  •  Type / Texte",
                left,
                centerY + 92,
                formWidth
        );

        this.cartoucheButton =
                Button.builder(
                                Component.empty(),
                                button -> {
                                    this.cartoucheType =
                                            this.cartoucheType.next();
                                    updateCartoucheButton();
                                }
                        )
                        .bounds(
                                left,
                                centerY + 113,
                                formWidth,
                                20
                        )
                        .build();
        this.addRenderableWidget(this.cartoucheButton);

        this.cartoucheTextField =
                new EditBox(
                        this.font,
                        left,
                        centerY + 139,
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
                                centerY + 169,
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
                                centerY + 169,
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
                        || newIndex >= D61ABlockEntity.MAX_PANELS
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
        this.selectedType = sanitizeType(type);

        if (this.selectedType == D21AType.WHITE) {
            this.autorouteLogo = false;
        }

        updateTypeButtons();
        updateAutorouteButton();
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

        this.panels[this.selectedPanelIndex] = new D61APanelData(
                this.panelEnabled,
                this.line1Field.getValue(),
                this.line2Field.getValue(),
                this.distance1Field.getValue(),
                this.distance2Field.getValue(),
                this.selectedType,
                this.doubleLine,
                this.arrowEnabled,
                this.arrowPosition,
                this.arrowDirection,
                this.selectedType != D21AType.WHITE && this.autorouteLogo,
                this.line1Font,
                this.line2Font
        );
    }

    private void loadSelectedPanelIntoWidgets() {
        D61APanelData panel = this.panels[this.selectedPanelIndex];

        this.panelEnabled = panel.enabled();
        this.doubleLine = panel.doubleLine();
        this.selectedType = sanitizeType(panel.type());
        this.autorouteLogo =
                this.selectedType != D21AType.WHITE
                        && panel.autorouteLogo();

        this.arrowEnabled = panel.arrowEnabled();
        this.arrowPosition = panel.arrowPosition();
        this.arrowDirection = panel.arrowDirection();
        this.line1Font = panel.line1Font();
        this.line2Font = panel.line2Font();

        this.line1Field.setValue(panel.line1());
        this.line2Field.setValue(panel.line2());
        this.distance1Field.setValue(panel.distance1());
        this.distance2Field.setValue(panel.distance2());

        updateEnabledButton();
        updateFormatButton();
        updateFieldVisibility();
        updateTypeButtons();
        updateAutorouteButton();
        updateArrowControls();
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

        // Flèche OU kilométrage : la valeur reste mémorisée dans le champ.
        this.distance1Field.visible = !this.arrowEnabled;
        this.distance1Field.active = !this.arrowEnabled;
        this.distance2Field.visible = this.doubleLine && !this.arrowEnabled;
        this.distance2Field.active = this.doubleLine && !this.arrowEnabled;
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
        for (int i = 0; i < D61ABlockEntity.MAX_PANELS; i++) {
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

            boolean hasArrow =
                    i == this.selectedPanelIndex
                            ? this.arrowEnabled
                            : this.panels[i].arrowEnabled();

            D61AArrowDirection panelArrowDirection =
                    i == this.selectedPanelIndex
                            ? this.arrowDirection
                            : this.panels[i].arrowDirection();

            String label =
                    "P" + (i + 1)
                            + " "
                            + (isDouble ? "2L" : "1L")
                            + (hasArrow ? " " + panelArrowDirection.symbol() : "")
                            + (enabled ? "" : " OFF");

            if (i == this.selectedPanelIndex) {
                label = "[" + label + "]";
            }

            this.panelButtons[i].setMessage(Component.literal(label));
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

    private void updateAutorouteButton() {
        boolean allowed = this.selectedType != D21AType.WHITE;

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

    private void updateArrowControls() {
        this.arrowEnabledButton.setMessage(
                Component.literal(
                        this.arrowEnabled
                                ? "Flèche : Oui"
                                : "Flèche : Non"
                )
        );

        this.arrowPositionButton.setMessage(
                Component.literal(
                        "Position : "
                                + (this.arrowPosition == D61AArrowPosition.LEFT
                                ? "Gauche"
                                : "Droite")
                )
        );

        this.arrowPositionButton.active = this.arrowEnabled;

        D61AArrowDirection[] directions = D61AArrowDirection.values();

        for (int i = 0; i < directions.length; i++) {
            Button button = this.directionButtons[i];
            if (button == null) {
                continue;
            }

            D61AArrowDirection direction = directions[i];
            button.active = this.arrowEnabled;
            button.setMessage(
                    Component.literal(
                            direction == this.arrowDirection
                                    ? "[" + direction.symbol() + "]"
                                    : direction.symbol()
                    )
            );
        }
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
                new UpdateD61APayload(
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

    private static D21AType sanitizeType(D21AType type) {
        if (type == D21AType.GREEN) {
            return D21AType.GREEN;
        }
        if (type == D21AType.BLUE) {
            return D21AType.BLUE;
        }
        return D21AType.WHITE;
    }
}
