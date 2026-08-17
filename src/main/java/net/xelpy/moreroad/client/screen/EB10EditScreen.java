package net.xelpy.moreroad.client.screen;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.xelpy.moreroad.network.UpdateEB10TextPayload;

public class EB10EditScreen extends Screen {

    private final BlockPos blockPos;

    private final String currentLine1;
    private final String currentLine2;
    private final boolean currentEb20;

    private EditBox line1Field;
    private EditBox line2Field;
    private Checkbox eb20Checkbox;

    public EB10EditScreen(
            BlockPos blockPos,
            String currentLine1,
            String currentLine2,
            boolean currentEb20
    ) {
        super(Component.literal("Panneau d'agglomération"));

        this.blockPos = blockPos.immutable();
        this.currentLine1 = currentLine1;
        this.currentLine2 = currentLine2;
        this.currentEb20 = currentEb20;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        /*
         * ========================================================
         * LIGNE 1
         * ========================================================
         */

        this.line1Field = new EditBox(
                this.font,
                centerX - 120,
                centerY - 60,
                240,
                20,
                Component.literal("Ligne 1")
        );

        this.line1Field.setMaxLength(32);
        this.line1Field.setValue(this.currentLine1);

        this.addRenderableWidget(this.line1Field);


        /*
         * ========================================================
         * LIGNE 2
         * ========================================================
         */

        this.line2Field = new EditBox(
                this.font,
                centerX - 120,
                centerY - 30,
                240,
                20,
                Component.literal("Ligne 2")
        );

        this.line2Field.setMaxLength(32);
        this.line2Field.setValue(this.currentLine2);

        this.addRenderableWidget(this.line2Field);


        /*
         * ========================================================
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
                        centerY + 5
                )
                .selected(this.currentEb20)
                .maxWidth(240)
                .build();

        this.addRenderableWidget(this.eb20Checkbox);


        /*
         * ========================================================
         * BOUTON VALIDER
         * ========================================================
         */

        this.addRenderableWidget(
                Button.builder(
                                Component.literal("Valider"),
                                button -> save()
                        )
                        .bounds(
                                centerX - 120,
                                centerY + 40,
                                115,
                                20
                        )
                        .build()
        );


        /*
         * ========================================================
         * BOUTON ANNULER
         * ========================================================
         */

        this.addRenderableWidget(
                Button.builder(
                                Component.literal("Annuler"),
                                button -> this.onClose()
                        )
                        .bounds(
                                centerX + 5,
                                centerY + 40,
                                115,
                                20
                        )
                        .build()
        );


        this.setInitialFocus(this.line1Field);
    }


    /*
     * ============================================================
     * SAUVEGARDE
     * ============================================================
     */

    private void save() {

        ClientPacketDistributor.sendToServer(
                new UpdateEB10TextPayload(
                        this.blockPos,
                        this.line1Field.getValue(),
                        this.line2Field.getValue(),
                        this.eb20Checkbox.selected()
                )
        );

        this.onClose();
    }
}