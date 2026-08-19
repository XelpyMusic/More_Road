package net.xelpy.moreroad.client.screen;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.xelpy.moreroad.block.custom.D42bBranchData;
import net.xelpy.moreroad.block.custom.D42bLabelColor;
import net.xelpy.moreroad.block.custom.RoadTextFont;
import net.xelpy.moreroad.block.entity.D42bBlockEntity;
import net.xelpy.moreroad.network.UpdateD42bPayload;

/**
 * Éditeur du panneau D42b dynamique.
 *
 * Chaque sortie possède son angle, deux mentions indépendantes, une police
 * L1/L4 et un fond Aucun/Vert/Bleu par mention.
 */
public class D42bEditScreen extends Screen {

    private final BlockPos blockPos;
    private final D42bBranchData[] branches =
            new D42bBranchData[D42bBlockEntity.MAX_BRANCHES];

    private int selectedBranch = 0;

    private boolean branchEnabled;
    private RoadTextFont line1Font = RoadTextFont.L1;
    private RoadTextFont line2Font = RoadTextFont.L1;
    private D42bLabelColor line1Color = D42bLabelColor.NONE;
    private D42bLabelColor line2Color = D42bLabelColor.NONE;

    private final Button[] branchButtons =
            new Button[D42bBlockEntity.MAX_BRANCHES];

    private Button enabledButton;
    private Button line1FontButton;
    private Button line2FontButton;
    private Button line1ColorButton;
    private Button line2ColorButton;

    private EditBox angleField;
    private EditBox line1Field;
    private EditBox line2Field;
    private EditBox distanceField;

    public D42bEditScreen(
            BlockPos blockPos,
            D42bBranchData[] currentBranches,
            String distanceText
    ) {
        super(Component.literal("D42b - Giratoire"));

        this.blockPos = blockPos.immutable();

        for (int i = 0; i < D42bBlockEntity.MAX_BRANCHES; i++) {
            this.branches[i] =
                    currentBranches != null
                            && i < currentBranches.length
                            && currentBranches[i] != null
                            ? currentBranches[i]
                            : D42bBranchData.defaultForIndex(i);
        }

        this.initialDistanceText =
                distanceText == null
                        ? ""
                        : distanceText;
    }

    private final String initialDistanceText;

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int formWidth = Math.min(430, this.width - 24);
        int left = centerX - formWidth / 2;
        int gap = 6;

        addSectionHeader(
                "SORTIES DU GIRATOIRE",
                left,
                centerY - 205,
                formWidth
        );

        int tabWidth =
                (
                        formWidth
                                - gap
                                * (D42bBlockEntity.MAX_BRANCHES - 1)
                )
                        / D42bBlockEntity.MAX_BRANCHES;

        for (int i = 0; i < D42bBlockEntity.MAX_BRANCHES; i++) {
            final int branchIndex = i;

            this.branchButtons[i] =
                    Button.builder(
                                    Component.empty(),
                                    button -> selectBranch(branchIndex)
                            )
                            .bounds(
                                    left + i * (tabWidth + gap),
                                    centerY - 184,
                                    tabWidth,
                                    20
                            )
                            .build();

            this.addRenderableWidget(this.branchButtons[i]);
        }

        addSectionHeader(
                "BRANCHE  •  Active / Angle",
                left,
                centerY - 154,
                formWidth
        );

        int halfWidth = (formWidth - gap) / 2;

        this.enabledButton =
                Button.builder(
                                Component.empty(),
                                button -> {
                                    this.branchEnabled = !this.branchEnabled;
                                    updateEnabledButton();
                                    updateBranchButtons();
                                }
                        )
                        .bounds(left, centerY - 133, halfWidth, 20)
                        .build();
        this.addRenderableWidget(this.enabledButton);

        this.angleField =
                new EditBox(
                        this.font,
                        left + halfWidth + gap,
                        centerY - 133,
                        halfWidth,
                        20,
                        Component.literal("Angle en degrés")
                );
        this.angleField.setMaxLength(4);
        this.addRenderableWidget(this.angleField);

        int presetWidth = (formWidth - gap * 4) / 5;
        String[] presetNames = {"← -90°", "↖ -45°", "↑ 0°", "↗ 45°", "→ 90°"};
        int[] presetAngles = {-90, -45, 0, 45, 90};

        for (int i = 0; i < presetNames.length; i++) {
            final int angle = presetAngles[i];
            this.addRenderableWidget(
                    Button.builder(
                                    Component.literal(presetNames[i]),
                                    button -> this.angleField.setValue(
                                            Integer.toString(angle)
                                    )
                            )
                            .bounds(
                                    left + i * (presetWidth + gap),
                                    centerY - 107,
                                    presetWidth,
                                    20
                            )
                            .build()
            );
        }

        addSectionHeader(
                "MENTIONS  •  Texte / Police / Fond",
                left,
                centerY - 77,
                formWidth
        );

        int lineLabelWidth = 36;
        int fontWidth = 96;
        int colorWidth = 92;
        int textWidth =
                formWidth
                        - lineLabelWidth
                        - fontWidth
                        - colorWidth
                        - gap * 3;

        addRowLabel("L1", left, centerY - 56, lineLabelWidth);

        this.line1Field =
                new EditBox(
                        this.font,
                        left + lineLabelWidth + gap,
                        centerY - 56,
                        textWidth,
                        20,
                        Component.literal("Mention ligne 1")
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
                                left + lineLabelWidth + gap + textWidth + gap,
                                centerY - 56,
                                fontWidth,
                                20
                        )
                        .build();
        this.addRenderableWidget(this.line1FontButton);

        this.line1ColorButton =
                Button.builder(
                                Component.empty(),
                                button -> {
                                    this.line1Color = this.line1Color.next();
                                    updateColorButtons();
                                }
                        )
                        .bounds(
                                left
                                        + lineLabelWidth
                                        + gap
                                        + textWidth
                                        + gap
                                        + fontWidth
                                        + gap,
                                centerY - 56,
                                colorWidth,
                                20
                        )
                        .build();
        this.addRenderableWidget(this.line1ColorButton);

        addRowLabel("L2", left, centerY - 30, lineLabelWidth);

        this.line2Field =
                new EditBox(
                        this.font,
                        left + lineLabelWidth + gap,
                        centerY - 30,
                        textWidth,
                        20,
                        Component.literal("Mention ligne 2")
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
                                left + lineLabelWidth + gap + textWidth + gap,
                                centerY - 30,
                                fontWidth,
                                20
                        )
                        .build();
        this.addRenderableWidget(this.line2FontButton);

        this.line2ColorButton =
                Button.builder(
                                Component.empty(),
                                button -> {
                                    this.line2Color = this.line2Color.next();
                                    updateColorButtons();
                                }
                        )
                        .bounds(
                                left
                                        + lineLabelWidth
                                        + gap
                                        + textWidth
                                        + gap
                                        + fontWidth
                                        + gap,
                                centerY - 30,
                                colorWidth,
                                20
                        )
                        .build();
        this.addRenderableWidget(this.line2ColorButton);

        addSectionHeader(
                "DISTANCE  •  Case inférieure gauche",
                left,
                centerY,
                formWidth
        );

        this.distanceField =
                new EditBox(
                        this.font,
                        left,
                        centerY + 21,
                        formWidth,
                        20,
                        Component.literal("Distance - ex. 100 m")
                );
        this.distanceField.setMaxLength(16);
        this.distanceField.setValue(this.initialDistanceText);
        this.addRenderableWidget(this.distanceField);

        addSectionHeader(
                "ANGLE LIBRE : -170° à +170°  •  0° = tout droit",
                left,
                centerY + 51,
                formWidth
        );

        this.addRenderableWidget(
                Button.builder(
                                Component.literal("Valider le D42b"),
                                button -> save()
                        )
                        .bounds(left, centerY + 78, halfWidth, 20)
                        .build()
        );

        this.addRenderableWidget(
                Button.builder(
                                Component.literal("Annuler"),
                                button -> this.onClose()
                        )
                        .bounds(
                                left + halfWidth + gap,
                                centerY + 78,
                                halfWidth,
                                20
                        )
                        .build()
        );

        loadSelectedBranchIntoWidgets();
        this.setInitialFocus(this.line1Field);
    }

    private void selectBranch(int index) {
        syncWidgetsIntoSelectedBranch();
        this.selectedBranch = Math.max(
                0,
                Math.min(D42bBlockEntity.MAX_BRANCHES - 1, index)
        );
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

        updateEnabledButton();
        updateFontButtons();
        updateColorButtons();
        updateBranchButtons();
    }

    private void syncWidgetsIntoSelectedBranch() {
        if (this.angleField == null) {
            return;
        }

        int angle = parseAngle(this.angleField.getValue());

        this.branches[this.selectedBranch] =
                new D42bBranchData(
                        this.branchEnabled,
                        angle,
                        this.line1Field.getValue(),
                        this.line2Field.getValue(),
                        this.line1Font,
                        this.line2Font,
                        this.line1Color,
                        this.line2Color
                );
    }

    private int parseAngle(String value) {
        try {
            int angle = Integer.parseInt(value.strip());
            return Math.max(
                    D42bBranchData.MIN_ANGLE,
                    Math.min(D42bBranchData.MAX_ANGLE, angle)
            );
        } catch (NumberFormatException ignored) {
            return this.branches[this.selectedBranch].angleDegrees();
        }
    }

    private void updateEnabledButton() {
        this.enabledButton.setMessage(
                Component.literal(
                        this.branchEnabled
                                ? "Branche active : Oui"
                                : "Branche active : Non"
                )
        );
    }

    private void updateFontButtons() {
        this.line1FontButton.setMessage(
                Component.literal(
                        this.line1Font == RoadTextFont.L4
                                ? "Police : L4"
                                : "Police : L1"
                )
        );

        this.line2FontButton.setMessage(
                Component.literal(
                        this.line2Font == RoadTextFont.L4
                                ? "Police : L4"
                                : "Police : L1"
                )
        );
    }

    private void updateColorButtons() {
        this.line1ColorButton.setMessage(
                Component.literal("Fond : " + this.line1Color.displayName())
        );
        this.line2ColorButton.setMessage(
                Component.literal("Fond : " + this.line2Color.displayName())
        );
    }

    private void updateBranchButtons() {
        for (int i = 0; i < this.branchButtons.length; i++) {
            D42bBranchData branch =
                    i == this.selectedBranch
                            ? new D42bBranchData(
                                    this.branchEnabled,
                                    parseAngle(this.angleField.getValue()),
                                    this.line1Field.getValue(),
                                    this.line2Field.getValue(),
                                    this.line1Font,
                                    this.line2Font,
                                    this.line1Color,
                                    this.line2Color
                            )
                            : this.branches[i];

            String marker = i == this.selectedBranch ? "[" : "";
            String endMarker = i == this.selectedBranch ? "]" : "";
            String off = branch.enabled() ? "" : " OFF";

            this.branchButtons[i].setMessage(
                    Component.literal(
                            marker
                                    + "S"
                                    + (i + 1)
                                    + " "
                                    + branch.angleDegrees()
                                    + "°"
                                    + off
                                    + endMarker
                    )
            );
        }
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

    private void addSectionHeader(String label, int x, int y, int width) {
        Button header =
                Button.builder(
                                Component.literal("— " + label + " —"),
                                button -> {
                                }
                        )
                        .bounds(x, y, width, 16)
                        .build();

        header.active = false;
        this.addRenderableWidget(header);
    }

    private void addRowLabel(String label, int x, int y, int width) {
        Button rowLabel =
                Button.builder(
                                Component.literal(label),
                                button -> {
                                }
                        )
                        .bounds(x, y, width, 20)
                        .build();

        rowLabel.active = false;
        this.addRenderableWidget(rowLabel);
    }
}
