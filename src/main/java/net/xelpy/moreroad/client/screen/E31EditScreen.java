package net.xelpy.moreroad.client.screen;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.xelpy.moreroad.network.UpdateE31TextPayload;

public class E31EditScreen extends Screen {

    private static final int MAX_TEXT_LENGTH = 48;

    private final BlockPos blockPos;
    private final String currentText;
    private final boolean waterName;

    private EditBox textField;

    public E31EditScreen(
            BlockPos blockPos,
            String currentText,
            boolean waterName
    ) {
        super(Component.literal(
                waterName
                        ? "E31b — Nom de ruisseau"
                        : "E31a — Lieu-dit"
        ));

        this.blockPos = blockPos.immutable();
        this.currentText = currentText == null ? "" : currentText;
        this.waterName = waterName;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int formWidth = Math.min(360, this.width - 24);
        int left = centerX - formWidth / 2;
        int gap = 6;

        Button header = Button.builder(
                        Component.literal(
                                this.waterName
                                        ? "— E31b  •  NOM DU RUISSEAU —"
                                        : "— E31a  •  LIEU-DIT —"
                        ),
                        button -> {
                        }
                )
                .bounds(left, centerY - 60, formWidth, 18)
                .build();
        header.active = false;
        this.addRenderableWidget(header);

        this.textField = new EditBox(
                this.font,
                left,
                centerY - 33,
                formWidth,
                20,
                Component.literal("Texte du panneau")
        );
        this.textField.setMaxLength(MAX_TEXT_LENGTH);
        this.textField.setValue(this.currentText);
        this.addRenderableWidget(this.textField);

        Button fontInfo = Button.builder(
                        Component.literal("Police : caractères L4"),
                        button -> {
                        }
                )
                .bounds(left, centerY - 7, formWidth, 18)
                .build();
        fontInfo.active = false;
        this.addRenderableWidget(fontInfo);

        int halfWidth = (formWidth - gap) / 2;

        this.addRenderableWidget(
                Button.builder(
                                Component.literal("Valider"),
                                button -> save()
                        )
                        .bounds(left, centerY + 24, halfWidth, 20)
                        .build()
        );

        this.addRenderableWidget(
                Button.builder(
                                Component.literal("Annuler"),
                                button -> this.onClose()
                        )
                        .bounds(
                                left + halfWidth + gap,
                                centerY + 24,
                                halfWidth,
                                20
                        )
                        .build()
        );

        this.setInitialFocus(this.textField);
    }

    private void save() {
        ClientPacketDistributor.sendToServer(
                new UpdateE31TextPayload(
                        this.blockPos,
                        this.textField.getValue()
                )
        );

        this.onClose();
    }
}
