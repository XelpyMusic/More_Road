package net.xelpy.moreroad.client.preview;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.xelpy.moreroad.road.RoadBuilderGeometry;

import java.util.Set;

/**
 * Caméra locale de prévisualisation vue du dessus.
 *
 * Le joueur n'est jamais téléporté. Une entité Marker uniquement cliente est
 * utilisée temporairement comme caméra, puis la caméra revient au joueur quand
 * l'éditeur est fermé.
 *
 * Important : le Marker n'est pas ajouté à la liste des entités du monde et ne
 * reçoit donc pas de tick normal. Il faut synchroniser manuellement ses
 * positions/rotations "précédentes" avec les valeurs actuelles. Sans cela,
 * Minecraft interpole à chaque frame entre les anciennes valeurs (0, 0, 0 /
 * rotation 0°) et la position aérienne, ce qui peut produire une vue vers
 * l'horizon ou le ciel au lieu d'une vraie vue verticale.
 */
public final class RoadBuilderTopDownCamera {

    private static Entity previousCamera;
    private static Marker previewCamera;
    private static double zoomMultiplier = 1.0D;

    private RoadBuilderTopDownCamera() {
    }

    public static void start(RoadBuilderGeometry.Geometry geometry) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;

        if (level == null) {
            return;
        }

        if (previewCamera == null) {
            previousCamera = minecraft.getCameraEntity();
            previewCamera = new Marker(EntityTypes.MARKER, level);
            previewCamera.setNoGravity(true);
            previewCamera.setInvisible(true);
        }

        zoomMultiplier = 1.0D;
        update(geometry);
        minecraft.setCameraEntity(previewCamera);
    }

    public static void update(RoadBuilderGeometry.Geometry geometry) {
        Minecraft minecraft = Minecraft.getInstance();

        if (previewCamera == null || minecraft.level == null) {
            return;
        }

        Bounds bounds = calculateBounds(geometry.surfacePositions());
        if (bounds == null) {
            return;
        }

        double centerX = (bounds.minX + bounds.maxX + 1.0D) * 0.5D;
        double centerZ = (bounds.minZ + bounds.maxZ + 1.0D) * 0.5D;

        double spanX = Math.max(1.0D, bounds.maxX - bounds.minX + 1.0D);
        double spanZ = Math.max(1.0D, bounds.maxZ - bounds.minZ + 1.0D);
        double span = Math.max(spanX, spanZ);

        // La hauteur est calculée à partir de l'emprise de la route afin de
        // garder du terrain visible autour. Les boutons Zoom +/- ne font que
        // modifier ce multiplicateur : ils ne déplacent jamais le joueur.
        double cameraHeight = Math.max(32.0D, span * 0.80D) * zoomMultiplier;
        double cameraY = bounds.maxY + cameraHeight;

        setStableCameraTransform(centerX, cameraY, centerZ);
        minecraft.setCameraEntity(previewCamera);
    }

    /**
     * Force une transformation parfaitement stable pour une entité caméra qui
     * n'est pas tickée par le ClientLevel.
     */
    private static void setStableCameraTransform(
            double x,
            double y,
            double z
    ) {
        if (previewCamera == null) {
            return;
        }

        // Position actuelle.
        previewCamera.setPos(x, y, z);

        // Positions précédentes utilisées par l'interpolation du rendu.
        previewCamera.xo = x;
        previewCamera.yo = y;
        previewCamera.zo = z;
        previewCamera.xOld = x;
        previewCamera.yOld = y;
        previewCamera.zOld = z;

        // Minecraft : +90° de pitch = regarder verticalement vers le bas.
        previewCamera.setYRot(0.0F);
        previewCamera.setXRot(90.0F);

        // Même principe pour la rotation : le Marker n'étant pas tické, on
        // synchronise explicitement les valeurs précédentes pour empêcher une
        // interpolation 0° -> 90° à chaque tick de rendu.
        previewCamera.yRotO = 0.0F;
        previewCamera.xRotO = 90.0F;
    }

    public static void zoomIn(RoadBuilderGeometry.Geometry geometry) {
        zoomMultiplier = Math.max(0.45D, zoomMultiplier * 0.80D);
        update(geometry);
    }

    public static void zoomOut(RoadBuilderGeometry.Geometry geometry) {
        zoomMultiplier = Math.min(3.0D, zoomMultiplier * 1.25D);
        update(geometry);
    }

    public static void resetZoom(RoadBuilderGeometry.Geometry geometry) {
        zoomMultiplier = 1.0D;
        update(geometry);
    }

    public static void stop() {
        Minecraft minecraft = Minecraft.getInstance();

        if (previewCamera != null) {
            Entity restore = previousCamera;

            if (restore == null || restore.isRemoved()) {
                restore = minecraft.player;
            }

            if (restore != null) {
                minecraft.setCameraEntity(restore);
            }
        }

        previewCamera = null;
        previousCamera = null;
        zoomMultiplier = 1.0D;
    }

    private static Bounds calculateBounds(Set<BlockPos> positions) {
        if (positions.isEmpty()) {
            return null;
        }

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (BlockPos pos : positions) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }

        return new Bounds(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private record Bounds(
            int minX,
            int minY,
            int minZ,
            int maxX,
            int maxY,
            int maxZ
    ) {
    }
}
