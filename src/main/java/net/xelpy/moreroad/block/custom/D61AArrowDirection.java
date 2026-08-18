package net.xelpy.moreroad.block.custom;

import net.minecraft.util.StringRepresentable;

/**
 * Direction d'une flèche D61A.
 *
 * Les PNG fleche_blanche.png et fleche_noir.png pointent naturellement
 * vers le haut. Le renderer tourne donc simplement le même visuel autour
 * de l'axe Z pour obtenir les huit directions.
 */
public enum D61AArrowDirection implements StringRepresentable {
    UP("up", "↑", 0.0F),
    UP_RIGHT("up_right", "↗", 315.0F),
    RIGHT("right", "→", 270.0F),
    DOWN_RIGHT("down_right", "↘", 225.0F),
    DOWN("down", "↓", 180.0F),
    DOWN_LEFT("down_left", "↙", 135.0F),
    LEFT("left", "←", 90.0F),
    UP_LEFT("up_left", "↖", 45.0F);

    private final String name;
    private final String symbol;
    private final float modelRotationDegrees;

    D61AArrowDirection(
            String name,
            String symbol,
            float modelRotationDegrees
    ) {
        this.name = name;
        this.symbol = symbol;
        this.modelRotationDegrees = modelRotationDegrees;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public String symbol() {
        return this.symbol;
    }

    /**
     * Rotation appliquée au PNG dont l'orientation native est vers le haut.
     */
    public float modelRotationDegrees() {
        return this.modelRotationDegrees;
    }

    public static D61AArrowDirection fromSerializedName(String value) {
        if (value == null) {
            return UP;
        }

        return switch (value) {
            case "up_right" -> UP_RIGHT;
            case "right" -> RIGHT;
            case "down_right" -> DOWN_RIGHT;
            case "down" -> DOWN;
            case "down_left" -> DOWN_LEFT;
            case "left" -> LEFT;
            case "up_left" -> UP_LEFT;
            default -> UP;
        };
    }
}
