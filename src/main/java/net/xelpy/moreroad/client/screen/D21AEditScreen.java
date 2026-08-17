package net.xelpy.moreroad.client.screen;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.xelpy.moreroad.block.custom.D21APanelData;
import net.xelpy.moreroad.block.custom.D21AType;
import net.xelpy.moreroad.block.entity.D21ABlockEntity;
import net.xelpy.moreroad.network.UpdateD21APayload;

public class D21AEditScreen extends Screen {

    private final BlockPos blockPos;

    private final D21APanelData[] panels =
            new D21APanelData[D21ABlockEntity.MAX_PANELS];

    private int selectedPanelIndex = 0;

    private boolean panelEnabled = true;
    private D21AType selectedType = D21AType.WHITE;
    private boolean arrowRight = false;
    private boolean autorouteLogo = false;

    private EditBox destinationField;
    private EditBox distanceField;

    private final Button[] panelButtons =
            new Button[D21ABlockEntity.MAX_PANELS];

    private Button enabledButton;
    private Button whiteButton;
    private Button greenButton;
    private Button blueButton;
    private Button autorouteLogoButton;
    private Button directionButton;

    public D21AEditScreen(
            BlockPos blockPos,
            D21APanelData[] currentPanels
    ) {
        super(
                Component.literal(
                        "Ensemble directionnel D21A"
                )
        );

        this.blockPos = blockPos.immutable();

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

        /* ========================================================
         * ONGLETS PANNEAUX 1 A 4
         * ======================================================== */

        int tabWidth = 71;
        int tabGap = 5;
        int tabsTotalWidth =
                tabWidth * D21ABlockEntity.MAX_PANELS
                        + tabGap * (D21ABlockEntity.MAX_PANELS - 1);

        int tabsStartX = centerX - tabsTotalWidth / 2;
        int tabsY = centerY - 145;

        for (int i = 0; i < D21ABlockEntity.MAX_PANELS; i++) {
            final int panelIndex = i;

            this.panelButtons[i] =
                    Button.builder(
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

            this.addRenderableWidget(
                    this.panelButtons[i]
            );
        }

        /* ========================================================
         * ACTIF / INACTIF
         * ======================================================== */

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
                                centerX - 150,
                                centerY - 115,
                                300,
                                20
                        )
                        .build();

        this.addRenderableWidget(
                this.enabledButton
        );

        /* ========================================================
         * DESTINATION
         * ======================================================== */

        this.destinationField =
                new EditBox(
                        this.font,
                        centerX - 150,
                        centerY - 85,
                        300,
                        20,
                        Component.literal("Destination")
                );

        this.destinationField.setMaxLength(48);

        this.addRenderableWidget(
                this.destinationField
        );

        /* ========================================================
         * DISTANCE
         * ======================================================== */

        this.distanceField =
                new EditBox(
                        this.font,
                        centerX - 60,
                        centerY - 55,
                        120,
                        20,
                        Component.literal("Distance")
                );

        this.distanceField.setMaxLength(8);

        this.addRenderableWidget(
                this.distanceField
        );

        /* ========================================================
         * COULEURS
         * ======================================================== */

        this.whiteButton =
                Button.builder(
                                Component.literal("Blanc"),
                                button -> selectType(D21AType.WHITE)
                        )
                        .bounds(
                                centerX - 150,
                                centerY - 20,
                                95,
                                20
                        )
                        .build();

        this.addRenderableWidget(
                this.whiteButton
        );

        this.greenButton =
                Button.builder(
                                Component.literal("Vert"),
                                button -> selectType(D21AType.GREEN)
                        )
                        .bounds(
                                centerX - 47,
                                centerY - 20,
                                94,
                                20
                        )
                        .build();

        this.addRenderableWidget(
                this.greenButton
        );

        this.blueButton =
                Button.builder(
                                Component.literal("Bleu"),
                                button -> selectType(D21AType.BLUE)
                        )
                        .bounds(
                                centerX + 55,
                                centerY - 20,
                                95,
                                20
                        )
                        .build();

        this.addRenderableWidget(
                this.blueButton
        );

        /* ========================================================
         * LOGO AUTOROUTE
         * ========================================================
         *
         * Visible uniquement sur les panneaux verts et bleus.
         * Le passage en blanc désactive automatiquement le logo.
         */

        this.autorouteLogoButton =
                Button.builder(
                                Component.empty(),
                                button -> {
                                    this.autorouteLogo = !this.autorouteLogo;
                                    updateAutorouteLogoButton();
                                }
                        )
                        .bounds(
                                centerX - 150,
                                centerY + 10,
                                300,
                                20
                        )
                        .build();

        this.addRenderableWidget(
                this.autorouteLogoButton
        );

        /* ========================================================
         * DIRECTION
         * ======================================================== */

        this.directionButton =
                Button.builder(
                                Component.empty(),
                                button -> {
                                    this.arrowRight = !this.arrowRight;
                                    updateDirectionButton();
                                }
                        )
                        .bounds(
                                centerX - 150,
                                centerY + 40,
                                300,
                                20
                        )
                        .build();

        this.addRenderableWidget(
                this.directionButton
        );

        /* ========================================================
         * VALIDER / ANNULER
         * ======================================================== */

        this.addRenderableWidget(
                Button.builder(
                                Component.literal("Valider"),
                                button -> save()
                        )
                        .bounds(
                                centerX - 150,
                                centerY + 80,
                                145,
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
                                centerX + 5,
                                centerY + 80,
                                145,
                                20
                        )
                        .build()
        );

        loadSelectedPanelIntoWidgets();

        this.setInitialFocus(
                this.destinationField
        );
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

        this.setInitialFocus(
                this.destinationField
        );
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
                this.destinationField == null
                        || this.distanceField == null
        ) {
            return;
        }

        this.panels[this.selectedPanelIndex] =
                new D21APanelData(
                        this.panelEnabled,
                        this.destinationField.getValue(),
                        this.distanceField.getValue(),
                        this.selectedType,
                        this.arrowRight,
                        this.autorouteLogo
                );
    }

    private void loadSelectedPanelIntoWidgets() {
        D21APanelData panel =
                this.panels[this.selectedPanelIndex];

        this.panelEnabled = panel.enabled();
        this.selectedType = panel.type();
        this.arrowRight = panel.arrowRight();
        this.autorouteLogo = panel.autorouteLogo();

        if (this.selectedType == D21AType.WHITE) {
            this.autorouteLogo = false;
        }

        this.destinationField.setValue(
                panel.destination()
        );

        this.distanceField.setValue(
                panel.distance()
        );

        updateEnabledButton();
        updateTypeButtons();
        updateAutorouteLogoButton();
        updateDirectionButton();
        updatePanelButtons();
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

            String label =
                    "P" + (i + 1)
                            + (enabled ? " ON" : " OFF");

            if (i == this.selectedPanelIndex) {
                label = "[ " + label + " ]";
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
                                ? "Panneau actif : Oui"
                                : "Panneau actif : Non"
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

    private void save() {
        storeSelectedPanelFromWidgets();

        ClientPacketDistributor.sendToServer(
                new UpdateD21APayload(
                        this.blockPos,
                        this.panels[0],
                        this.panels[1],
                        this.panels[2],
                        this.panels[3]
                )
        );

        this.onClose();
    }
}
