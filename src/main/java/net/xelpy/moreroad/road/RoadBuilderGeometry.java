package net.xelpy.moreroad.road;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Calcul commun client / serveur de la géométrie du constructeur de route.
 *
 * Profil fixe :
 * T R R R R R T R R R R R T
 * soit 13 blocs de large avec les lignes aux offsets -6, 0 et +6.
 *
 * Les petits doublons des lignes blanches sont volontairement conservés afin
 * de lisser visuellement les courbes. Ils peuvent être nettoyés manuellement
 * après construction.
 *
 * IMPORTANT POUR LA PENTE :
 * la hauteur n'est pas calculée à partir des échantillons de la Bézier.
 * On construit d'abord une vraie ligne centrale discrète indépendante des axes X/Z.
 * Une diagonale compte comme UNE progression de bloc, et non comme deux pas
 * séparés X puis Z. On conserve donc six cellules centrales de progression au
 * même Y ; la septième peut passer au Y suivant si la route doit encore monter
 * ou descendre. La règle 1 bloc vertical / 6 blocs s'adapte ainsi automatiquement
 * à une route orientée Nord/Sud, Est/Ouest, diagonale ou courbe.
 */
public final class RoadBuilderGeometry {

    public static final int HALF_ROAD_WIDTH = 6;
    public static final int ROAD_WIDTH = HALF_ROAD_WIDTH * 2 + 1;
    public static final int HEIGHT_STEP_DISTANCE = 6;

    private static final double SURFACE_SAMPLE_STEP = 0.18D;

    private RoadBuilderGeometry() {
    }

    public static Geometry calculate(
            BlockPos start,
            BlockPos control,
            BlockPos end
    ) {
        DPoint p0 = DPoint.fromBlock(start);
        DPoint p1 = DPoint.fromBlock(control);
        DPoint p2 = DPoint.fromBlock(end);

        double estimatedLength =
                horizontalDistance(p0, p1)
                        + horizontalDistance(p1, p2);

        int samples = Math.max(
                48,
                (int) Math.ceil(estimatedLength / SURFACE_SAMPLE_STEP)
        );

        // Premier passage : création d'une ligne centrale ordonnée 8-connectée.
        // Une diagonale X+Z est UNE seule progression. C'est le point essentiel
        // pour que « 6 blocs puis +1 Y » soit identique quelle que soit
        // l'orientation de la route dans le monde.
        CenterPath centerPath = buildCenterPath(p0, p1, p2, samples);

        if (centerPath.cells.isEmpty()) {
            return new Geometry(Set.of(), Set.of(), Set.of());
        }

        int controlCenterX = (int) Math.round(p1.x);
        int controlCenterZ = (int) Math.round(p1.z);
        int controlIndex = findNearestCenterIndex(
                centerPath.cells,
                controlCenterX,
                controlCenterZ
        );

        int[] centerY = new int[centerPath.cells.size()];

        // Le compteur de 6 blocs est CONTINU sur toute la route. Il n'est pas
        // remis à zéro au point B : B ne sert qu'à changer la hauteur cible.
        // Ainsi, déplacer/faire pivoter la route ne modifie jamais le rythme
        // des marches.
        assignAdaptiveSixBlockSlope(
                centerY,
                controlIndex,
                start.getY(),
                control.getY(),
                end.getY()
        );

        Map<Long, Integer> centerYByXZ = new HashMap<>();
        for (int i = 0; i < centerPath.cells.size(); i++) {
            CenterCell cell = centerPath.cells.get(i);
            centerYByXZ.put(xzKey(cell.x, cell.z), centerY[i]);
        }

        Set<BlockPos> surfacePositions = new HashSet<>();
        Set<BlockPos> whiteLinePositions = new HashSet<>();

        BlockPos[] previousSurfaceByLateral = new BlockPos[ROAD_WIDTH];

        // Deuxième passage : on conserve la Bézier fine pour la largeur de la
        // chaussée, mais son Y est maintenant lu sur la ligne centrale discrète
        // ci-dessus. Il ne dépend donc plus du nombre d'échantillons.
        for (int i = 0; i <= samples; i++) {
            double t = i / (double) samples;

            DPoint rawCenter = quadraticBezier(p0, p1, p2, t);
            DPoint tangent = quadraticBezierDerivative(p0, p1, p2, t);

            double tangentLength = Math.hypot(tangent.x, tangent.z);
            if (tangentLength < 1.0E-6D) {
                continue;
            }

            int centerBlockX = (int) Math.round(rawCenter.x);
            int centerBlockZ = (int) Math.round(rawCenter.z);

            Integer currentRoadY = centerYByXZ.get(
                    xzKey(centerBlockX, centerBlockZ)
            );

            if (currentRoadY == null) {
                int nearestIndex = findNearestCenterIndex(
                        centerPath.cells,
                        centerBlockX,
                        centerBlockZ
                );
                currentRoadY = centerY[nearestIndex];
            }

            DPoint center = new DPoint(
                    rawCenter.x,
                    currentRoadY,
                    rawCenter.z
            );

            double perpendicularX = -tangent.z / tangentLength;
            double perpendicularZ = tangent.x / tangentLength;

            // Chaussée complète : les 13 bandes restent reliées pour ne laisser
            // aucun trou dans le gray concrete.
            for (
                    int lateral = -HALF_ROAD_WIDTH;
                    lateral <= HALF_ROAD_WIDTH;
                    lateral++
            ) {
                int index = lateral + HALF_ROAD_WIDTH;

                BlockPos current = offsetBlockPos(
                        center,
                        perpendicularX,
                        perpendicularZ,
                        lateral
                );

                BlockPos previous = previousSurfaceByLateral[index];

                if (previous == null) {
                    surfacePositions.add(current);
                } else if (!previous.equals(current)) {
                    addSurfaceConnectedPath(
                            previous,
                            current,
                            surfacePositions
                    );
                }

                previousSurfaceByLateral[index] = current;
            }

            // Trois lignes blanches : on conserve le tracé fin de la Bézier,
            // doublons compris. Le chemin 8-connecté ci-dessus sert uniquement
            // de règle de mesure pour la pente ; il ne force donc pas le rendu
            // visuel des lignes à devenir haché dans les virages.
            whiteLinePositions.add(
                    offsetBlockPos(
                            center,
                            perpendicularX,
                            perpendicularZ,
                            -HALF_ROAD_WIDTH
                    )
            );

            whiteLinePositions.add(
                    offsetBlockPos(
                            center,
                            perpendicularX,
                            perpendicularZ,
                            0
                    )
            );

            whiteLinePositions.add(
                    offsetBlockPos(
                            center,
                            perpendicularX,
                            perpendicularZ,
                            HALF_ROAD_WIDTH
                    )
            );
        }

        return new Geometry(
                Set.copyOf(surfacePositions),
                Set.copyOf(whiteLinePositions),
                Set.of()
        );
    }

    /**
     * Construit la ligne de MESURE de la pente en 8-connexité.
     *
     * Contrairement à l'ancienne version, une progression diagonale ne devient
     * pas artificiellement « X puis Z ». Elle compte comme un seul bloc de
     * progression. Cela rend le compteur de 6 blocs indépendant de l'axe du
     * monde. Les lignes blanches visibles restent, elles, calculées finement
     * plus bas afin de conserver les doublons qui lissent les courbes.
     */
    private static CenterPath buildCenterPath(
            DPoint p0,
            DPoint p1,
            DPoint p2,
            int samples
    ) {
        List<CenterCell> cells = new ArrayList<>();

        DPoint firstPoint = quadraticBezier(p0, p1, p2, 0.0D);
        int previousX = (int) Math.round(firstPoint.x);
        int previousZ = (int) Math.round(firstPoint.z);

        cells.add(new CenterCell(previousX, previousZ));

        for (int i = 1; i <= samples; i++) {
            double t = i / (double) samples;
            DPoint point = quadraticBezier(p0, p1, p2, t);

            int targetX = (int) Math.round(point.x);
            int targetZ = (int) Math.round(point.z);

            if (targetX == previousX && targetZ == previousZ) {
                continue;
            }

            appendEightConnectedXZ(
                    previousX,
                    previousZ,
                    targetX,
                    targetZ,
                    cells
            );

            CenterCell last = cells.get(cells.size() - 1);
            previousX = last.x;
            previousZ = last.z;
        }

        return new CenterPath(cells);
    }

    private static void appendEightConnectedXZ(
            int fromX,
            int fromZ,
            int toX,
            int toZ,
            List<CenterCell> destination
    ) {
        int x = fromX;
        int z = fromZ;

        int deltaX = Math.abs(toX - fromX);
        int deltaZ = Math.abs(toZ - fromZ);
        int stepX = Integer.compare(toX, fromX);
        int stepZ = Integer.compare(toZ, fromZ);

        int error = deltaX - deltaZ;

        while (x != toX || z != toZ) {
            int doubledError = error * 2;

            // Bresenham 8-connecté : les deux conditions peuvent être vraies
            // au même tour. Dans ce cas X et Z changent ensemble et cela ne
            // crée qu'UNE nouvelle cellule centrale.
            if (doubledError > -deltaZ) {
                error -= deltaZ;
                x += stepX;
            }

            if (doubledError < deltaX) {
                error += deltaX;
                z += stepZ;
            }

            CenterCell next = new CenterCell(x, z);
            CenterCell last = destination.get(destination.size() - 1);

            if (last.x != next.x || last.z != next.z) {
                destination.add(next);
            }
        }
    }

    /**
     * Affecte les Y avec un compteur continu de 6 cellules centrales :
     * - au moins 6 cellules de progression entre deux changements de Y ;
     * - avant B, on se rapproche de la hauteur de B ;
     * - après B, on se rapproche de la hauteur de C ;
     * - le compteur n'est jamais remis à zéro simplement parce qu'on passe B.
     *
     * Comme centerPath est 8-connecté, une diagonale X+Z compte comme UNE
     * cellule, exactement comme une progression d'un bloc dans la direction
     * de la route.
     */
    private static void assignAdaptiveSixBlockSlope(
            int[] yValues,
            int controlIndex,
            int startY,
            int controlY,
            int endY
    ) {
        if (yValues.length == 0) {
            return;
        }

        int currentY = startY;
        int blocksSinceLastHeightChange = 1;
        yValues[0] = currentY;

        for (int i = 1; i < yValues.length; i++) {
            int targetY = i <= controlIndex ? controlY : endY;

            if (
                    currentY != targetY
                            && blocksSinceLastHeightChange >= HEIGHT_STEP_DISTANCE
            ) {
                currentY += Integer.compare(targetY, currentY);
                blocksSinceLastHeightChange = 1;
            } else {
                blocksSinceLastHeightChange++;
            }

            yValues[i] = currentY;
        }
    }

    private static int findNearestCenterIndex(
            List<CenterCell> cells,
            int x,
            int z
    ) {
        int bestIndex = 0;
        long bestDistanceSquared = Long.MAX_VALUE;

        for (int i = 0; i < cells.size(); i++) {
            CenterCell cell = cells.get(i);
            long dx = (long) cell.x - x;
            long dz = (long) cell.z - z;
            long distanceSquared = dx * dx + dz * dz;

            if (distanceSquared < bestDistanceSquared) {
                bestDistanceSquared = distanceSquared;
                bestIndex = i;

                if (distanceSquared == 0L) {
                    break;
                }
            }
        }

        return bestIndex;
    }

    private static long xzKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    private static BlockPos offsetBlockPos(
            DPoint center,
            double perpendicularX,
            double perpendicularZ,
            int lateral
    ) {
        return toBlockPos(
                center.x + perpendicularX * lateral,
                center.y,
                center.z + perpendicularZ * lateral
        );
    }

    /**
     * Connexion 4-directionnelle utilisée uniquement pour la chaussée afin de
     * garantir une surface de gray concrete sans trous.
     */
    private static void addSurfaceConnectedPath(
            BlockPos from,
            BlockPos to,
            Set<BlockPos> destination
    ) {
        int x = from.getX();
        int z = from.getZ();

        int targetX = to.getX();
        int targetZ = to.getZ();

        int deltaX = Math.abs(targetX - x);
        int deltaZ = Math.abs(targetZ - z);
        int stepX = Integer.compare(targetX, x);
        int stepZ = Integer.compare(targetZ, z);

        int progressedX = 0;
        int progressedZ = 0;
        int totalSteps = deltaX + deltaZ;
        int completedSteps = 0;

        destination.add(from);

        if (totalSteps == 0) {
            destination.add(to);
            return;
        }

        while (progressedX < deltaX || progressedZ < deltaZ) {
            boolean moveX;

            if (progressedX >= deltaX) {
                moveX = false;
            } else if (progressedZ >= deltaZ) {
                moveX = true;
            } else {
                double nextXCrossing = (progressedX + 0.5D) / deltaX;
                double nextZCrossing = (progressedZ + 0.5D) / deltaZ;
                moveX = nextXCrossing <= nextZCrossing;
            }

            if (moveX) {
                x += stepX;
                progressedX++;
            } else {
                z += stepZ;
                progressedZ++;
            }

            completedSteps++;

            destination.add(
                    new BlockPos(
                            x,
                            interpolateY(
                                    from.getY(),
                                    to.getY(),
                                    completedSteps,
                                    totalSteps
                            ),
                            z
                    )
            );
        }
    }

    private static int interpolateY(
            int fromY,
            int toY,
            int completedSteps,
            int totalSteps
    ) {
        if (totalSteps <= 0) {
            return toY;
        }

        double progress = Math.min(
                1.0D,
                completedSteps / (double) totalSteps
        );

        return (int) Math.round(
                fromY + (toY - fromY) * progress
        );
    }

    private static DPoint quadraticBezier(
            DPoint p0,
            DPoint p1,
            DPoint p2,
            double t
    ) {
        double oneMinusT = 1.0D - t;

        double x =
                oneMinusT * oneMinusT * p0.x
                        + 2.0D * oneMinusT * t * p1.x
                        + t * t * p2.x;

        double y =
                oneMinusT * oneMinusT * p0.y
                        + 2.0D * oneMinusT * t * p1.y
                        + t * t * p2.y;

        double z =
                oneMinusT * oneMinusT * p0.z
                        + 2.0D * oneMinusT * t * p1.z
                        + t * t * p2.z;

        return new DPoint(x, y, z);
    }

    private static DPoint quadraticBezierDerivative(
            DPoint p0,
            DPoint p1,
            DPoint p2,
            double t
    ) {
        double x =
                2.0D * (1.0D - t) * (p1.x - p0.x)
                        + 2.0D * t * (p2.x - p1.x);

        double y =
                2.0D * (1.0D - t) * (p1.y - p0.y)
                        + 2.0D * t * (p2.y - p1.y);

        double z =
                2.0D * (1.0D - t) * (p1.z - p0.z)
                        + 2.0D * t * (p2.z - p1.z);

        return new DPoint(x, y, z);
    }

    private static BlockPos toBlockPos(
            double x,
            double y,
            double z
    ) {
        return new BlockPos(
                (int) Math.round(x),
                (int) Math.round(y),
                (int) Math.round(z)
        );
    }

    private static double horizontalDistance(
            DPoint first,
            DPoint second
    ) {
        return Math.hypot(
                second.x - first.x,
                second.z - first.z
        );
    }

    private record CenterCell(int x, int z) {
    }

    private record CenterPath(List<CenterCell> cells) {
    }

    private record DPoint(double x, double y, double z) {
        private static DPoint fromBlock(BlockPos pos) {
            return new DPoint(
                    pos.getX() + 0.5D,
                    pos.getY(),
                    pos.getZ() + 0.5D
            );
        }
    }

    public record Geometry(
            Set<BlockPos> surfacePositions,
            Set<BlockPos> whiteLinePositions,
            Set<BlockPos> excessLinePositions
    ) {
    }
}
