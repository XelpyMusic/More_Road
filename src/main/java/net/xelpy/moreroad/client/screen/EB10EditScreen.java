package net.xelpy.moreroad.client.screen;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.xelpy.moreroad.block.custom.CartoucheType;
import net.xelpy.moreroad.network.UpdateEB10TextPayload;

public class EB10EditScreen extends Screen {

    private final BlockPos blockPos;

    private final String currentLine1;
    private final String currentLine2;
    private final boolean currentEb20;
    private final String currentCartoucheText;

    private CartoucheType cartoucheType;

    private EditBox line1Field;
    private EditBox line2Field;
    private EditBox cartoucheTextField;
    private Checkbox eb20Checkbox;
    private Button cartoucheButton;

    public EB10EditScreen(
            BlockPos blockPos,
            String currentLine1,
            String currentLine2,
            boolean currentEb20,
            CartoucheType currentCartoucheType,
            String currentCartoucheText
    ) {
        super(Component.literal("Panneau d'agglomération"));

        this.blockPos = blockPos.immutable();
        this.currentLine1 = currentLine1;
        this.currentLine2 = currentLine2;
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

        /* ========================================================
         * LIGNE 1
         * ======================================================== */

        this.line1Field = new EditBox(
                this.font,
                centerX - 120,
                centerY - 75,
                240,
                20,
                Component.literal("Ligne 1")
        );

        this.line1Field.setMaxLength(32);
        this.line1Field.setValue(this.currentLine1);

        this.addRenderableWidget(this.line1Field);

        /* ========================================================
         * LIGNE 2
         * ======================================================== */

        this.line2Field = new EditBox(
                this.font,
                centerX - 120,
                centerY - 45,
                240,
                20,
                Component.literal("Ligne 2")
        );

        this.line2Field.setMaxLength(32);
        this.line2Field.setValue(this.currentLine2);

        this.addRenderableWidget(this.line2Field);

        /* ========================================================
         * CASE EB20
         * ========================================================
         *
         * Décoché = EB10
         * Coché   = EB20
         */

        this.eb20Checkbox = Checkbox.builder(
                        Component.literal("Sortie d'agglomération (EB20)"),
                        this.font
                )
                .pos(
                        centerX - 120,
                        centerY - 10
                )
                .selected(this.currentEb20)
                .maxWidth(240)
                .build();

        this.addRenderableWidget(this.eb20Checkbox);

        /* ========================================================
         * CARTOUCHE E41 A E45
         * ======================================================== */

        this.cartoucheButton = Button.builder(
                        Component.empty(),
                        button -> {
                            this.cartoucheType = this.cartoucheType.next();
                            updateCartoucheButton();
                        }
                )
                .bounds(
                        centerX - 120,
                        centerY + 25,
                        240,
                        20
                )
                .build();

        this.addRenderableWidget(this.cartoucheButton);

        this.cartoucheTextField = new EditBox(
                this.font,
                centerX - 120,
                centerY + 55,
                240,
                20,
                Component.literal("Texte du cartouche (ex. D 240)")
        );
        this.cartoucheTextField.setMaxLength(24);
        this.cartoucheTextField.setValue(this.currentCartoucheText);
        this.addRenderableWidget(this.cartoucheTextField);

        updateCartoucheButton();

        /* ========================================================
         * BOUTON VALIDER
         * ======================================================== */

        this.addRenderableWidget(
                Button.builder(
                                Component.literal("Valider"),
                                button -> save()
                        )
                        .bounds(
                                centerX - 120,
                                centerY + 90,
                                115,
                                20
                        )
                        .build()
        );

        /* ========================================================
         * BOUTON ANNULER
         * ======================================================== */

        this.addRenderableWidget(
                Button.builder(
                                Component.literal("Annuler"),
                                button -> this.onClose()
                        )
                        .bounds(
                                centerX + 5,
                                centerY + 90,
                                115,
                                20
                        )
                        .build()
        );

        this.setInitialFocus(this.line1Field);
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
                        this.eb20Checkbox.selected(),
                        this.cartoucheType,
                        this.cartoucheTextField.getValue()
                )
        );

        this.onClose();
    }
}
