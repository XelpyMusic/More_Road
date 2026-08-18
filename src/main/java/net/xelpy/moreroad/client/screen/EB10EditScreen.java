package net.xelpy.moreroad.client.screen;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
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

    private EditBox line1Field;
    private EditBox line2Field;
    private EditBox cartoucheTextField;
    private Checkbox eb20Checkbox;
    private Button line1FontButton;
    private Button line2FontButton;
    private Button cartoucheButton;

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
        this.currentLine1 = currentLine1;
        this.currentLine2 = currentLine2;
        this.line1Font = currentLine1Font == null
                ? RoadTextFont.L1
                : currentLine1Font;
        this.line2Font = currentLine2Font == null
                ? RoadTextFont.L1
                : currentLine2Font;
        this.currentEb20 = currentEb20;
        this.currentCartoucheText =
                currentCartoucheText == null
                        ? ""
                        : currentCartoucheText;
        this.cartoucheType =
                currentCartoucheType == null
                        ? CartoucheType.NONE
                        : currentCartoucheType;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int formWidth = Math.min(340, this.width - 24);
        int left = centerX - formWidth / 2;
        int gap = 6;

        /* ========================================================
         * TEXTES
         * ======================================================== */

        addSectionHeader(
                "TEXTES  •  Contenu / Police",
                left,
                centerY - 110,
                formWidth
        );

        int lineLabelWidth = 36;
        int fontButtonWidth = 100;
        int textWidth =
                formWidth
                        - lineLabelWidth
                        - fontButtonWidth
                        - gap * 2;

        addRowLabel(
                "L1",
                left,
                centerY - 89,
                lineLabelWidth
        );

        this.line1Field =
                new EditBox(
                        this.font,
                        left + lineLabelWidth + gap,
                        centerY - 89,
                        textWidth,
                        20,
                        Component.literal("Ligne 1")
                );

        this.line1Field.setMaxLength(32);
        this.line1Field.setValue(this.currentLine1);
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
                                centerY - 89,
                                fontButtonWidth,
                                20
                        )
                        .build();
        this.addRenderableWidget(this.line1FontButton);

        addRowLabel(
                "L2",
                left,
                centerY - 63,
                lineLabelWidth
        );

        this.line2Field =
                new EditBox(
                        this.font,
                        left + lineLabelWidth + gap,
                        centerY - 63,
                        textWidth,
                        20,
                        Component.literal("Ligne 2")
                );

        this.line2Field.setMaxLength(32);
        this.line2Field.setValue(this.currentLine2);
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
                                centerY - 63,
                                fontButtonWidth,
                                20
                        )
                        .build();
        this.addRenderableWidget(this.line2FontButton);

        /* ========================================================
         * TYPE DE PANNEAU
         * ======================================================== */

        addSectionHeader(
                "TYPE DE PANNEAU",
                left,
                centerY - 33,
                formWidth
        );

        this.eb20Checkbox =
                Checkbox.builder(
                                Component.literal(
                                        "Sortie d'agglomération (EB20)"
                                ),
                                this.font
                        )
                        .pos(
                                left,
                                centerY - 9
                        )
                        .selected(this.currentEb20)
                        .maxWidth(formWidth)
                        .build();

        this.addRenderableWidget(this.eb20Checkbox);

        /* ========================================================
         * CARTOUCHE
         * ======================================================== */

        addSectionHeader(
                "CARTOUCHE  •  Type / Texte",
                left,
                centerY + 20,
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
                                centerY + 41,
                                formWidth,
                                20
                        )
                        .build();

        this.addRenderableWidget(this.cartoucheButton);

        this.cartoucheTextField =
                new EditBox(
                        this.font,
                        left,
                        centerY + 67,
                        formWidth,
                        20,
                        Component.literal("Texte du cartouche")
                );
        this.cartoucheTextField.setMaxLength(24);
        this.cartoucheTextField.setValue(
                this.currentCartoucheText
        );
        this.addRenderableWidget(this.cartoucheTextField);

        updateFontButtons();
        updateCartoucheButton();

        /* ========================================================
         * ACTIONS
         * ======================================================== */

        int halfWidth = (formWidth - gap) / 2;

        this.addRenderableWidget(
                Button.builder(
                                Component.literal("Valider les modifications"),
                                button -> save()
                        )
                        .bounds(
                                left,
                                centerY + 97,
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
                                centerY + 97,
                                halfWidth,
                                20
                        )
                        .build()
        );

        this.setInitialFocus(this.line1Field);
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

    /* ============================================================
     * SAUVEGARDE
     * ============================================================ */

    private void save() {
        ClientPacketDistributor.sendToServer(
                new UpdateEB10TextPayload(
                        this.blockPos,
                        this.line1Field.getValue(),
                        this.line2Field.getValue(),
                        this.line1Font,
                        this.line2Font,
                        this.eb20Checkbox.selected(),
                        this.cartoucheType,
                        this.cartoucheTextField.getValue()
                )
        );

        this.onClose();
    }
}
