package net.xelpy.moreroad.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.xelpy.moreroad.block.custom.D42bBranchData;
import net.xelpy.moreroad.block.custom.D42bLabelColor;
import net.xelpy.moreroad.block.custom.RoadTextFont;

/**
 * Données persistantes du D42b dynamique.
 */
public class D42bBlockEntity extends BlockEntity {

    public static final int MAX_BRANCHES = 6;

    private final D42bBranchData[] branches =
            new D42bBranchData[MAX_BRANCHES];

    private String distanceText = "100 m";

    public D42bBlockEntity(BlockPos pos, BlockState state) {
        super(MoreRoadBlockEntities.D42B.get(), pos, state);

        for (int i = 0; i < MAX_BRANCHES; i++) {
            this.branches[i] = D42bBranchData.defaultForIndex(i);
        }
    }

    public D42bBranchData getBranch(int index) {
        if (index < 0 || index >= MAX_BRANCHES) {
            return D42bBranchData.disabled(0);
        }
        return this.branches[index];
    }

    public D42bBranchData[] getBranches() {
        return this.branches.clone();
    }

    public void setBranches(D42bBranchData[] values) {
        for (int i = 0; i < MAX_BRANCHES; i++) {
            this.branches[i] =
                    values != null && i < values.length && values[i] != null
                            ? values[i]
                            : D42bBranchData.defaultForIndex(i);
        }
        setChanged();
    }

    public String getDistanceText() {
        return this.distanceText;
    }

    public void setDistanceText(String distanceText) {
        this.distanceText = distanceText == null ? "" : distanceText;
        setChanged();
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        this.distanceText = input.getStringOr("distance_text", "100 m");

        for (int i = 0; i < MAX_BRANCHES; i++) {
            D42bBranchData defaults = D42bBranchData.defaultForIndex(i);
            String prefix = "branch_" + i + "_";

            this.branches[i] = new D42bBranchData(
                    input.getBooleanOr(prefix + "enabled", defaults.enabled()),
                    parseAngle(
                            input.getStringOr(
                                    prefix + "angle",
                                    Integer.toString(defaults.angleDegrees())
                            ),
                            defaults.angleDegrees()
                    ),
                    input.getStringOr(prefix + "line1", defaults.line1()),
                    input.getStringOr(prefix + "line2", defaults.line2()),
                    RoadTextFont.fromSerializedName(
                            input.getStringOr(
                                    prefix + "line1_font",
                                    defaults.line1Font().getSerializedName()
                            )
                    ),
                    RoadTextFont.fromSerializedName(
                            input.getStringOr(
                                    prefix + "line2_font",
                                    defaults.line2Font().getSerializedName()
                            )
                    ),
                    D42bLabelColor.fromSerializedName(
                            input.getStringOr(
                                    prefix + "line1_color",
                                    defaults.line1Color().getSerializedName()
                            )
                    ),
                    D42bLabelColor.fromSerializedName(
                            input.getStringOr(
                                    prefix + "line2_color",
                                    defaults.line2Color().getSerializedName()
                            )
                    )
            );
        }
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);

        output.putString("distance_text", this.distanceText);

        for (int i = 0; i < MAX_BRANCHES; i++) {
            D42bBranchData branch = this.branches[i];
            String prefix = "branch_" + i + "_";

            output.putBoolean(prefix + "enabled", branch.enabled());
            output.putString(prefix + "angle", Integer.toString(branch.angleDegrees()));
            output.putString(prefix + "line1", branch.line1());
            output.putString(prefix + "line2", branch.line2());
            output.putString(prefix + "line1_font", branch.line1Font().getSerializedName());
            output.putString(prefix + "line2_font", branch.line2Font().getSerializedName());
            output.putString(prefix + "line1_color", branch.line1Color().getSerializedName());
            output.putString(prefix + "line2_color", branch.line2Color().getSerializedName());
        }
    }

    private static int parseAngle(String value, int fallback) {
        try {
            return Integer.parseInt(value == null ? "" : value.strip());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
