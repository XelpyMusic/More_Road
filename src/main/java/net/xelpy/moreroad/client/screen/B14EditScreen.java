package net.xelpy.moreroad.client.screen;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.xelpy.moreroad.block.custom.B14Speed;
import net.xelpy.moreroad.network.UpdateB14Payload;

public class B14EditScreen extends Screen {

    private static final int BUTTON_WIDTH = 56;
    private static final int BUTTON_HEIGHT = 20;
    private static final int GAP = 6;
    private static final int COLUMNS = 5;

    private final BlockPos blockPos;
    private final Button[] speedButtons =
            new Button[B14Speed.values().length];

    private B14Speed selectedSpeed;

    public B14EditScreen(
            BlockPos blockPos,
            B14Speed currentSpeed
    ) {
        super(Component.literal("B14 — Limitation de vitesse"));

        this.blockPos = blockPos.immutable();
        this.selectedSpeed = currentSpeed == null
                ? B14Speed.KMH_5
                : currentSpeed;
    }

    @Override
    protected void init() {
        super.init();

        B14Speed[] speeds = B14Speed.values();

        int totalWidth =
                COLUMNS * BUTTON_WIDTH
                        + (COLUMNS - 1) * GAP;

        int startX = this.width / 2 - totalWidth / 2;
        int startY = this.height / 2 - 58;

        for (int i = 0; i < speeds.length; i++) {
            final int index = i;
            B14Speed speed = speeds[i];

            int column = i % COLUMNS;
            int row = i / COLUMNS;

            this.speedButtons[i] =
                    Button.builder(
                                    Component.empty(),
                                    button -> selectSpeed(index)
                            )
                            .bounds(
                                    startX + column * (BUTTON_WIDTH + GAP),
                                    startY + row * (BUTTON_HEIGHT + GAP),
                                    BUTTON_WIDTH,
                                    BUTTON_HEIGHT
                            )
                            .build();

            this.addRenderableWidget(this.speedButtons[i]);
        }

        int actionY =
                startY
                        + 3 * (BUTTON_HEIGHT + GAP)
                        + 10;

        this.addRenderableWidget(
                Button.builder(
                                Component.literal("Valider"),
                                button -> save()
                        )
                        .bounds(
                                this.width / 2 - 121,
                                actionY,
                                116,
                                BUTTON_HEIGHT
                        )
                        .build()
        );

        this.addRenderableWidget(
                Button.builder(
                                Component.literal("Annuler"),
                                button -> this.onClose()
                        )
                        .bounds(
                                this.width / 2 + 5,
                                actionY,
                                116,
                                BUTTON_HEIGHT
                        )
                        .build()
        );

        updateSpeedButtons();
    }

    private void selectSpeed(int index) {
        B14Speed[] speeds = B14Speed.values();

        if (index < 0 || index >= speeds.length) {
            return;
        }

        this.selectedSpeed = speeds[index];
        updateSpeedButtons();
    }

    private void updateSpeedButtons() {
        B14Speed[] speeds = B14Speed.values();

        for (int i = 0; i < this.speedButtons.length; i++) {
            Button button = this.speedButtons[i];

            if (button == null) {
                continue;
            }

            B14Speed speed = speeds[i];

            button.setMessage(
                    Component.literal(
                            speed == this.selectedSpeed
                                    ? "[ " + speed.value() + " ]"
                                    : Integer.toString(speed.value())
                    )
            );
        }
    }

    private void save() {
        ClientPacketDistributor.sendToServer(
                new UpdateB14Payload(
                        this.blockPos,
                        this.selectedSpeed
                )
        );

        this.onClose();
    }
}
