package net.xelpy.moreroad.client.screen;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.xelpy.moreroad.network.UpdateEB10TextPayload;

public class EB10EditScreen extends Screen {

    private final BlockPos blockPos;
    private final String currentName;

    private EditBox cityNameField;

    public EB10EditScreen(BlockPos blockPos, String currentName) {
        super(Component.literal("Panneau d'entrée de commune"));

        this.blockPos = blockPos.immutable();
        this.currentName = currentName;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.cityNameField = new EditBox(
                this.font,
                centerX - 100,
                centerY - 20,
                200,
                20,
                Component.literal("Nom de la commune")
        );

        this.cityNameField.setMaxLength(32);
        this.cityNameField.setValue(this.currentName);

        this.addRenderableWidget(this.cityNameField);

        this.addRenderableWidget(
                Button.builder(
                                Component.literal("Valider"),
                                button -> save()
                        )
                        .bounds(
                                centerX - 100,
                                centerY + 15,
                                95,
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
                                centerY + 15,
                                95,
                                20
                        )
                        .build()
        );
    }

    private void save() {

        ClientPacketDistributor.sendToServer(
                new UpdateEB10TextPayload(
                        this.blockPos,
                        this.cityNameField.getValue()
                )
        );

        this.onClose();
    }
}