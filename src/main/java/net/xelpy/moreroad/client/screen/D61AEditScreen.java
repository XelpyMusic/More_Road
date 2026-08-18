package net.xelpy.moreroad.client.screen;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.xelpy.moreroad.block.custom.D21APanelData;
import net.xelpy.moreroad.block.custom.D21AType;
import net.xelpy.moreroad.block.entity.D61ABlockEntity;
import net.xelpy.moreroad.network.UpdateD61APayload;

public class D61AEditScreen extends Screen {

    private final BlockPos blockPos;

    private final D21APanelData[] panels =
            new D21APanelData[D61ABlockEntity.MAX_PANELS];

    private int selectedPanelIndex = 0;

    private boolean panelEnabled = true;
    private boolean doubleLine = false;
    private D21AType selectedType = D21AType.WHITE;

    private EditBox line1Field;
    private EditBox line2Field;
    private EditBox distance1Field;
    private EditBox distance2Field;

    private final Button[] panelButtons =
            new Button[D61ABlockEntity.MAX_PANELS];

    private Button enabledButton;
    private Button formatButton;
    private Button whiteButton;
    private Button greenButton;

    public D61AEditScreen(
            BlockPos blockPos,
            D21APanelData[] currentPanels
    ) {
        super(Component.literal("Ensemble directionnel D61A"));

        this.blockPos = blockPos.immutable();

        for (int i = 0; i < D61ABlockEntity.MAX_PANELS; i++) {
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

            D21AType type = panel.type() == D21AType.GREEN
                    ? D21AType.GREEN
                    : D21AType.WHITE;

            this.panels[i] =
                    new D21APanelData(
                            panel.enabled(),
                            panel.line1(),
                            panel.line2(),
                            panel.distance1(),
                            panel.distance2(),
                            type,
                            false,
                            false,
                            panel.doubleLine()
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
        int tabsY = centerY - 175;

        for (int i = 0; i < D61ABlockEntity.MAX_PANELS; i++) {
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

            this.addRenderableWidget(this.panelButtons[i]);
        }

        this.enabledButton =
                Button.builder(
                                Component.empty(),
                                button -> {
                                    this.panelEnabled = !this.panelEnabled;
                                    updateEnabledButton();
                                    updatePanelButtons();
                                }
                        )
                        .bounds(centerX - 150, centerY - 145, 300, 20)
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
                        .bounds(centerX - 150, centerY - 115, 300, 20)
                        .build();
        this.addRenderableWidget(this.formatButton);

        this.line1Field =
                new EditBox(
                        this.font,
                        centerX - 150,
                        centerY - 85,
                        230,
                        20,
                        Component.literal("Destination ligne 1")
                );
        this.line1Field.setMaxLength(48);
        this.addRenderableWidget(this.line1Field);

        this.distance1Field =
                new EditBox(
                        this.font,
                        centerX + 90,
                        centerY - 85,
                        60,
                        20,
                        Component.literal("Distance ligne 1")
                );
        this.distance1Field.setMaxLength(8);
        this.addRenderableWidget(this.distance1Field);

        this.line2Field =
                new EditBox(
                        this.font,
                        centerX - 150,
                        centerY - 55,
                        230,
                        20,
                        Component.literal("Destination ligne 2")
                );
        this.line2Field.setMaxLength(48);
        this.addRenderableWidget(this.line2Field);

        this.distance2Field =
                new EditBox(
                        this.font,
                        centerX + 90,
                        centerY - 55,
                        60,
                        20,
                        Component.literal("Distance ligne 2")
                );
        this.distance2Field.setMaxLength(8);
        this.addRenderableWidget(this.distance2Field);

        this.whiteButton =
                Button.builder(
                                Component.literal("Blanc"),
                                button -> selectType(D21AType.WHITE)
                        )
                        .bounds(centerX - 150, centerY - 20, 145, 20)
                        .build();
        this.addRenderableWidget(this.whiteButton);

        this.greenButton =
                Button.builder(
                                Component.literal("Vert"),
                                button -> selectType(D21AType.GREEN)
                        )
                        .bounds(centerX + 5, centerY - 20, 145, 20)
                        .build();
        this.addRenderableWidget(this.greenButton);

        this.addRenderableWidget(
                Button.builder(
                                Component.literal("Valider"),
                                button -> save()
                        )
                        .bounds(centerX - 150, centerY + 20, 145, 20)
                        .build()
        );

        this.addRenderableWidget(
                Button.builder(
                                Component.literal("Annuler"),
                                button -> this.onClose()
                        )
                        .bounds(centerX + 5, centerY + 20, 145, 20)
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
        this.selectedType =
                type == D21AType.GREEN
                        ? D21AType.GREEN
                        : D21AType.WHITE;

        updateTypeButtons();
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
                        false,
                        false,
                        this.doubleLine
                );
    }

    private void loadSelectedPanelIntoWidgets() {
        D21APanelData panel = this.panels[this.selectedPanelIndex];

        this.panelEnabled = panel.enabled();
        this.doubleLine = panel.doubleLine();
        this.selectedType = panel.type() == D21AType.GREEN
                ? D21AType.GREEN
                : D21AType.WHITE;

        this.line1Field.setValue(panel.line1());
        this.line2Field.setValue(panel.line2());
        this.distance1Field.setValue(panel.distance1());
        this.distance2Field.setValue(panel.distance2());

        updateEnabledButton();
        updateFormatButton();
        updateFieldVisibility();
        updateTypeButtons();
        updatePanelButtons();
    }

    private void updateFieldVisibility() {
        this.line2Field.visible = this.doubleLine;
        this.line2Field.active = this.doubleLine;
        this.distance2Field.visible = this.doubleLine;
        this.distance2Field.active = this.doubleLine;
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

            String label =
                    "P" + (i + 1)
                            + " "
                            + (isDouble ? "2L" : "1L")
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
}
