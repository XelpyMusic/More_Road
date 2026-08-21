package net.xelpy.moreroad.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.xelpy.moreroad.client.preview.RoadBuilderTopDownCamera;
import net.xelpy.moreroad.client.preview.RoadBuilderWorldPreview;
import net.xelpy.moreroad.network.BuildRoadPayload;
import net.xelpy.moreroad.road.RoadBuilderGeometry;

/**
 * Éditeur de prévisualisation du constructeur de route.
 *
 * Le monde réel reste affiché derrière l'interface. À l'ouverture, la caméra
 * quitte temporairement le joueur et se place automatiquement à la verticale
 * au-dessus de toute la route. Le joueur n'est jamais téléporté : seule la
 * caméra locale change.
 *
 * Les blocs de prévisualisation sont eux aussi uniquement locaux :
 * - gris = chaussée ;
 * - blanc = ligne finale ; les petits doublons sont conservés volontairement pour lisser la courbe.
 */
public class RoadBuilderPreviewScreen extends Screen {

    private static final int COLOR_PANEL = 0xC0181B20;
    private static final int COLOR_PANEL_BORDER = 0xDD737B87;
    private static final int COLOR_TEXT = 0xFFF5F5F5;
    private static final int COLOR_SUBTEXT = 0xFFC4CBD5;
    private static final int COLOR_WARNING = 0xFFFF7777;

    private final BlockPos originalStart;
    private final BlockPos originalControl;
    private final BlockPos originalEnd;

    private BlockPos start;
    private BlockPos control;
    private BlockPos end;

    private RoadBuilderGeometry.Geometry geometry;

    /** 0 = départ, 1 = contrôle, 2 = fin. */
    private int selectedPoint = 1;

    /** Pas de déplacement X/Z/Y : 1, 5 ou 10 blocs. */
    private int moveStep = 1;

    private Button startButton;
    private Button controlButton;
    private Button endButton;
    private Button stepButton;

    private boolean validated;

    public RoadBuilderPreviewScreen(
            BlockPos start,
            BlockPos control,
            BlockPos end
    ) {
        super(Component.literal("Constructeur de Route - Prévisualisation"));

        this.originalStart = start.immutable();
        this.originalControl = control.immutable();
        this.originalEnd = end.immutable();

        this.start = this.originalStart;
        this.control = this.originalControl;
        this.end = this.originalEnd;

        updateGeometry(false);
    }

    @Override
    protected void init() {
        super.init();

        int gap = 5;
        int panelWidth = Math.min(780, Math.max(560, this.width - 24));
        int panelX = (this.width - panelWidth) / 2;
        int panelBottom = this.height - 10;

        int pointButtonWidth = (panelWidth - gap * 4) / 3;
        int pointRowY = panelBottom - 101;

        this.startButton = this.addRenderableWidget(
                Button.builder(
                                Component.empty(),
                                button -> selectPoint(0)
                        )
                        .bounds(
                                panelX + gap,
                                pointRowY,
                                pointButtonWidth,
                                20
                        )
                        .build()
        );

        this.controlButton = this.addRenderableWidget(
                Button.builder(
                                Component.empty(),
                                button -> selectPoint(1)
                        )
                        .bounds(
                                panelX + gap * 2 + pointButtonWidth,
                                pointRowY,
                                pointButtonWidth,
                                20
                        )
                        .build()
        );

        this.endButton = this.addRenderableWidget(
                Button.builder(
                                Component.empty(),
                                button -> selectPoint(2)
                        )
                        .bounds(
                                panelX + gap * 3 + pointButtonWidth * 2,
                                pointRowY,
                                pointButtonWidth,
                                20
                        )
                        .build()
        );

        int moveY = panelBottom - 76;
        int moveButtonWidth = 58;
        int stepWidth = 64;
        int moveRowWidth = moveButtonWidth * 6 + stepWidth + gap * 6;
        int moveX = (this.width - moveRowWidth) / 2;

        addMoveButton("X -", moveX, moveY, -1, 0, 0, moveButtonWidth);
        addMoveButton("X +", moveX + (moveButtonWidth + gap), moveY, 1, 0, 0, moveButtonWidth);
        addMoveButton("Z -", moveX + (moveButtonWidth + gap) * 2, moveY, 0, 0, -1, moveButtonWidth);
        addMoveButton("Z +", moveX + (moveButtonWidth + gap) * 3, moveY, 0, 0, 1, moveButtonWidth);
        addMoveButton("Y -", moveX + (moveButtonWidth + gap) * 4, moveY, 0, -1, 0, moveButtonWidth);
        addMoveButton("Y +", moveX + (moveButtonWidth + gap) * 5, moveY, 0, 1, 0, moveButtonWidth);

        this.stepButton = this.addRenderableWidget(
                Button.builder(
                                Component.empty(),
                                button -> cycleMoveStep()
                        )
                        .bounds(
                                moveX + (moveButtonWidth + gap) * 6,
                                moveY,
                                stepWidth,
                                20
                        )
                        .build()
        );

        int cameraY = panelBottom - 51;
        int cameraButtonWidth = 108;
        int cameraRowWidth = cameraButtonWidth * 3 + gap * 2;
        int cameraX = (this.width - cameraRowWidth) / 2;

        this.addRenderableWidget(
                Button.builder(
                                Component.literal("Zoom +"),
                                button -> RoadBuilderTopDownCamera.zoomIn(this.geometry)
                        )
                        .bounds(cameraX, cameraY, cameraButtonWidth, 20)
                        .build()
        );

        this.addRenderableWidget(
                Button.builder(
                                Component.literal("Recentrer"),
                                button -> RoadBuilderTopDownCamera.resetZoom(this.geometry)
                        )
                        .bounds(
                                cameraX + cameraButtonWidth + gap,
                                cameraY,
                                cameraButtonWidth,
                                20
                        )
                        .build()
        );

        this.addRenderableWidget(
                Button.builder(
                                Component.literal("Zoom -"),
                                button -> RoadBuilderTopDownCamera.zoomOut(this.geometry)
                        )
                        .bounds(
                                cameraX + (cameraButtonWidth + gap) * 2,
                                cameraY,
                                cameraButtonWidth,
                                20
                        )
                        .build()
        );

        int actionY = panelBottom - 26;
        int resetWidth = 100;
        int validateWidth = 166;
        int cancelWidth = 90;
        int actionRowWidth = resetWidth + validateWidth + cancelWidth + gap * 2;
        int actionX = (this.width - actionRowWidth) / 2;

        this.addRenderableWidget(
                Button.builder(
                                Component.literal("Réinitialiser"),
                                button -> resetPoints()
                        )
                        .bounds(actionX, actionY, resetWidth, 20)
                        .build()
        );

        this.addRenderableWidget(
                Button.builder(
                                Component.literal("Valider et construire"),
                                button -> validateAndBuild()
                        )
                        .bounds(
                                actionX + resetWidth + gap,
                                actionY,
                                validateWidth,
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
                                actionX + resetWidth + gap + validateWidth + gap,
                                actionY,
                                cancelWidth,
                                20
                        )
                        .build()
        );

        updatePointButtons();
        updateStepButton();

        // La route apparaît directement dans le vrai terrain, puis la caméra
        // se place automatiquement au-dessus de l'ensemble de la construction.
        RoadBuilderWorldPreview.show(this.geometry);
        RoadBuilderTopDownCamera.start(this.geometry);
    }

    /**
     * Le serveur reste autoritaire sur les chunks et peut restaurer les vrais
     * blocs juste après l'ouverture de l'aperçu. On réapplique donc la route
     * fantôme à chaque tick et on verrouille en même temps la caméra aérienne.
     */
    @Override
    public void tick() {
        super.tick();
        RoadBuilderWorldPreview.refresh();
        RoadBuilderTopDownCamera.update(this.geometry);
    }

    private void addMoveButton(
            String label,
            int x,
            int y,
            int dx,
            int dy,
            int dz,
            int width
    ) {
        this.addRenderableWidget(
                Button.builder(
                                Component.literal(label),
                                button -> moveSelectedPoint(dx, dy, dz)
                        )
                        .bounds(x, y, width, 20)
                        .build()
        );
    }

    /**
     * Aucun fond opaque ou flou : le monde vu du ciel reste réellement visible
     * derrière les contrôles.
     */
    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        // Intentionnellement vide.
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        int panelWidth = Math.min(780, Math.max(560, this.width - 24));
        int panelX = (this.width - panelWidth) / 2;
        int panelY = this.height - 121;
        int panelHeight = 111;

        graphics.fill(
                panelX,
                panelY,
                panelX + panelWidth,
                panelY + panelHeight,
                COLOR_PANEL
        );

        graphics.outline(
                panelX,
                panelY,
                panelWidth,
                panelHeight,
                COLOR_PANEL_BORDER
        );

        graphics.centeredText(
                this.font,
                this.title,
                this.width / 2,
                panelY - 39,
                COLOR_TEXT
        );

        graphics.centeredText(
                this.font,
                Component.literal(
                        "Vue aérienne réelle du terrain - rien n'est construit avant validation."
                ),
                this.width / 2,
                panelY - 27,
                COLOR_SUBTEXT
        );

        graphics.centeredText(
                this.font,
                Component.literal(
                        "Blanc = ligne finale   |   Doubles conservés   |   Pente : 6 blocs centraux par niveau (X/Z auto)"
                ),
                this.width / 2,
                panelY - 15,
                COLOR_SUBTEXT
        );

        BlockPos selected = getSelectedPoint();

        graphics.centeredText(
                this.font,
                Component.literal(
                        pointName(this.selectedPoint)
                                + "   X=" + selected.getX()
                                + "  Y=" + selected.getY()
                                + "  Z=" + selected.getZ()
                                + "   |   T R R R R R T R R R R R T"
                ),
                this.width / 2,
                panelY + 5,
                COLOR_TEXT
        );

        super.extractRenderState(
                graphics,
                mouseX,
                mouseY,
                partialTick
        );
    }

    private void selectPoint(int pointIndex) {
        this.selectedPoint = Math.max(0, Math.min(2, pointIndex));
        updatePointButtons();
    }

    private void moveSelectedPoint(
            int directionX,
            int directionY,
            int directionZ
    ) {
        BlockPos current = getSelectedPoint();

        setPoint(
                this.selectedPoint,
                new BlockPos(
                        current.getX() + directionX * this.moveStep,
                        current.getY() + directionY * this.moveStep,
                        current.getZ() + directionZ * this.moveStep
                )
        );
    }

    private void cycleMoveStep() {
        this.moveStep = switch (this.moveStep) {
            case 1 -> 5;
            case 5 -> 10;
            default -> 1;
        };

        updateStepButton();
    }

    private void resetPoints() {
        this.start = this.originalStart;
        this.control = this.originalControl;
        this.end = this.originalEnd;
        this.selectedPoint = 1;
        this.moveStep = 1;

        updateGeometry(true);
        RoadBuilderTopDownCamera.resetZoom(this.geometry);
        updatePointButtons();
        updateStepButton();
    }

    private void validateAndBuild() {
        this.validated = true;

        RoadBuilderWorldPreview.clear();
        RoadBuilderTopDownCamera.stop();

        ClientPacketDistributor.sendToServer(
                new BuildRoadPayload(
                        this.start,
                        this.control,
                        this.end
                )
        );

        super.onClose();
    }

    @Override
    public void onClose() {
        RoadBuilderWorldPreview.clear();
        RoadBuilderTopDownCamera.stop();
        super.onClose();
    }

    @Override
    public void removed() {
        // Sécurité pour un changement d'écran, une déconnexion ou Échap.
        if (!this.validated) {
            RoadBuilderWorldPreview.clear();
        }

        RoadBuilderTopDownCamera.stop();
        super.removed();
    }

    private void setPoint(int pointIndex, BlockPos value) {
        switch (pointIndex) {
            case 0 -> this.start = value.immutable();
            case 1 -> this.control = value.immutable();
            case 2 -> this.end = value.immutable();
            default -> {
                return;
            }
        }

        updateGeometry(true);
        updatePointButtons();
    }

    private BlockPos getPoint(int pointIndex) {
        return switch (pointIndex) {
            case 0 -> this.start;
            case 2 -> this.end;
            default -> this.control;
        };
    }

    private BlockPos getSelectedPoint() {
        return getPoint(this.selectedPoint);
    }

    private void updateGeometry(boolean refreshWorldPreview) {
        this.geometry = RoadBuilderGeometry.calculate(
                this.start,
                this.control,
                this.end
        );

        if (refreshWorldPreview) {
            RoadBuilderWorldPreview.show(this.geometry);
            RoadBuilderTopDownCamera.update(this.geometry);
        }
    }

    private void updatePointButtons() {
        if (this.startButton == null
                || this.controlButton == null
                || this.endButton == null) {
            return;
        }

        this.startButton.setMessage(
                pointButtonText(0, "A - Départ", this.start)
        );
        this.controlButton.setMessage(
                pointButtonText(1, "B - Courbe", this.control)
        );
        this.endButton.setMessage(
                pointButtonText(2, "C - Fin", this.end)
        );
    }

    private void updateStepButton() {
        if (this.stepButton != null) {
            this.stepButton.setMessage(
                    Component.literal("Pas " + this.moveStep)
            );
        }
    }

    private Component pointButtonText(
            int index,
            String name,
            BlockPos pos
    ) {
        String prefix = index == this.selectedPoint ? "[ " : "";
        String suffix = index == this.selectedPoint ? " ]" : "";

        return Component.literal(
                prefix
                        + name
                        + "  Y=" + pos.getY()
                        + suffix
        );
    }

    private static String pointName(int pointIndex) {
        return switch (pointIndex) {
            case 0 -> "A - Départ";
            case 2 -> "C - Fin";
            default -> "B - Courbe";
        };
    }
}
