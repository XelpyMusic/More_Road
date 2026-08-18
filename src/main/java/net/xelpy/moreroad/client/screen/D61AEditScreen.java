package net.xelpy.moreroad.client.screen;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.xelpy.moreroad.block.custom.D21AType;
import net.xelpy.moreroad.block.custom.D61AArrowDirection;
import net.xelpy.moreroad.block.custom.D61AArrowPosition;
import net.xelpy.moreroad.block.custom.D61APanelData;
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

    private boolean arrowEnabled = false;
    private D61AArrowPosition arrowPosition = D61AArrowPosition.RIGHT;
    private D61AArrowDirection arrowDirection = D61AArrowDirection.UP;

    private EditBox line1Field;
    private EditBox line2Field;
    private EditBox distance1Field;
    private EditBox distance2Field;

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

    public D61AEditScreen(
            BlockPos blockPos,
            D61APanelData[] currentPanels
    ) {
        super(Component.literal("Ensemble directionnel D61A"));

        this.blockPos = blockPos.immutable();

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
                    type != D21AType.WHITE && panel.autorouteLogo()
            );
        }
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int tabWidth = 71;
        int tabGap = 5;
        int tabsTotalWidth =
                tabWidth * D61ABlockEntity.MAX_PANELS
                        + tabGap * (D61ABlockEntity.MAX_PANELS - 1);

        int tabsStartX = centerX - tabsTotalWidth / 2;
        int tabsY = centerY - 190;

        for (int i = 0; i < D61ABlockEntity.MAX_PANELS; i++) {
            final int panelIndex = i;

            this.panelButtons[i] = Button.builder(
                            Component.empty(),
                            button -> selectPanel(panelIndex)
                    )
                    .bounds(
                            tabsStartX + i * (tabWidth + tabGap),
                            tabsY,
                            tabWidth,
                            20
                    )
                    .build();

            this.addRenderableWidget(this.panelButtons[i]);
        }

        this.enabledButton = Button.builder(
                        Component.empty(),
                        button -> {
                            this.panelEnabled = !this.panelEnabled;
                            updateEnabledButton();
                            updatePanelButtons();
                        }
                )
                .bounds(centerX - 150, centerY - 160, 300, 20)
                .build();
        this.addRenderableWidget(this.enabledButton);

        this.formatButton = Button.builder(
                        Component.empty(),
                        button -> {
                            this.doubleLine = !this.doubleLine;
                            updateFormatButton();
                            updateFieldVisibility();
                            updatePanelButtons();
                        }
                )
                .bounds(centerX - 150, centerY - 130, 300, 20)
                .build();
        this.addRenderableWidget(this.formatButton);

        this.line1Field = new EditBox(
                this.font,
                centerX - 150,
                centerY - 100,
                230,
                20,
                Component.literal("Destination ligne 1")
        );
        this.line1Field.setMaxLength(48);
        this.addRenderableWidget(this.line1Field);

        this.distance1Field = new EditBox(
                this.font,
                centerX + 90,
                centerY - 100,
                60,
                20,
                Component.literal("Distance ligne 1")
        );
        this.distance1Field.setMaxLength(8);
        this.addRenderableWidget(this.distance1Field);

        this.line2Field = new EditBox(
                this.font,
                centerX - 150,
                centerY - 70,
                230,
                20,
                Component.literal("Destination ligne 2")
        );
        this.line2Field.setMaxLength(48);
        this.addRenderableWidget(this.line2Field);

        this.distance2Field = new EditBox(
                this.font,
                centerX + 90,
                centerY - 70,
                60,
                20,
                Component.literal("Distance ligne 2")
        );
        this.distance2Field.setMaxLength(8);
        this.addRenderableWidget(this.distance2Field);

        this.whiteButton = Button.builder(
                        Component.literal("Blanc"),
                        button -> selectType(D21AType.WHITE)
                )
                .bounds(centerX - 150, centerY - 35, 96, 20)
                .build();
        this.addRenderableWidget(this.whiteButton);

        this.greenButton = Button.builder(
                        Component.literal("Vert"),
                        button -> selectType(D21AType.GREEN)
                )
                .bounds(centerX - 48, centerY - 35, 96, 20)
                .build();
        this.addRenderableWidget(this.greenButton);

        this.blueButton = Button.builder(
                        Component.literal("Bleu"),
                        button -> selectType(D21AType.BLUE)
                )
                .bounds(centerX + 54, centerY - 35, 96, 20)
                .build();
        this.addRenderableWidget(this.blueButton);

        this.autorouteLogoButton = Button.builder(
                        Component.empty(),
                        button -> {
                            this.autorouteLogo = !this.autorouteLogo;
                            updateAutorouteButton();
                        }
                )
                .bounds(centerX - 150, centerY - 5, 300, 20)
                .build();
        this.addRenderableWidget(this.autorouteLogoButton);

        this.arrowEnabledButton = Button.builder(
                        Component.empty(),
                        button -> {
                            this.arrowEnabled = !this.arrowEnabled;
                            updateFieldVisibility();
                            updateArrowControls();
                            updatePanelButtons();
                        }
                )
                .bounds(centerX - 150, centerY + 25, 145, 20)
                .build();
        this.addRenderableWidget(this.arrowEnabledButton);

        this.arrowPositionButton = Button.builder(
                        Component.empty(),
                        button -> {
                            this.arrowPosition = this.arrowPosition.opposite();
                            updateArrowControls();
                        }
                )
                .bounds(centerX + 5, centerY + 25, 145, 20)
                .build();
        this.addRenderableWidget(this.arrowPositionButton);

        D61AArrowDirection[] directions = D61AArrowDirection.values();
        int directionWidth = 34;
        int directionGap = 4;
        int directionStartX = centerX - 150;
        int directionY = centerY + 55;

        for (int i = 0; i < directions.length; i++) {
            final D61AArrowDirection direction = directions[i];

            this.directionButtons[i] = Button.builder(
                            Component.literal(direction.symbol()),
                            button -> {
                                this.arrowDirection = direction;
                                updateArrowControls();
                            }
                    )
                    .bounds(
                            directionStartX + i * (directionWidth + directionGap),
                            directionY,
                            directionWidth,
                            20
                    )
                    .build();

            this.addRenderableWidget(this.directionButtons[i]);
        }

        this.addRenderableWidget(
                Button.builder(
                                Component.literal("Valider"),
                                button -> save()
                        )
                        .bounds(centerX - 150, centerY + 90, 145, 20)
                        .build()
        );

        this.addRenderableWidget(
                Button.builder(
                                Component.literal("Annuler"),
                                button -> this.onClose()
                        )
                        .bounds(centerX + 5, centerY + 90, 145, 20)
                        .build()
        );

        loadSelectedPanelIntoWidgets();
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
                this.selectedType != D21AType.WHITE && this.autorouteLogo
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
        updatePanelButtons();
    }

    private void updateFieldVisibility() {
        this.line2Field.visible = this.doubleLine;
        this.line2Field.active = this.doubleLine;

        // Flèche OU kilométrage : la valeur reste mémorisée dans le champ.
        this.distance1Field.visible = !this.arrowEnabled;
        this.distance1Field.active = !this.arrowEnabled;
        this.distance2Field.visible = this.doubleLine && !this.arrowEnabled;
        this.distance2Field.active = this.doubleLine && !this.arrowEnabled;
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
                                ? "Panneau actif : Oui"
                                : "Panneau actif : Non"
                )
        );
    }

    private void updateFormatButton() {
        this.formatButton.setMessage(
                Component.literal(
                        this.doubleLine
                                ? "[ Format : Double - 2 lignes ]"
                                : "Format : Simple - 1 ligne"
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

    private void save() {
        storeSelectedPanelFromWidgets();

        ClientPacketDistributor.sendToServer(
                new UpdateD61APayload(
                        this.blockPos,
                        this.panels[0],
                        this.panels[1],
                        this.panels[2],
                        this.panels[3]
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
