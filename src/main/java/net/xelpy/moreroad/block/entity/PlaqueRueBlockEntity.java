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
import net.xelpy.moreroad.block.custom.RoadTextFont;

/** Données persistantes des deux lignes personnalisables de la plaque de rue. */
public class PlaqueRueBlockEntity extends BlockEntity {

    private String line1 = "";
    private String line2 = "";
    private RoadTextFont line1Font = RoadTextFont.NORMAL;
    private RoadTextFont line2Font = RoadTextFont.NORMAL;

    public PlaqueRueBlockEntity(BlockPos pos, BlockState state) {
        super(MoreRoadBlockEntities.PLAQUE_RUE.get(), pos, state);
    }

    public String getLine1() {
        return this.line1;
    }

    public String getLine2() {
        return this.line2;
    }

    public RoadTextFont getLine1Font() {
        return this.line1Font;
    }

    public RoadTextFont getLine2Font() {
        return this.line2Font;
    }

    public void setContent(
            String line1,
            String line2,
            RoadTextFont line1Font,
            RoadTextFont line2Font
    ) {
        this.line1 = line1 == null ? "" : line1;
        this.line2 = line2 == null ? "" : line2;
        this.line1Font = line1Font == null ? RoadTextFont.NORMAL : line1Font;
        this.line2Font = line2Font == null ? RoadTextFont.NORMAL : line2Font;
        setChanged();
    }

    /** Compatibilité avec les plaques créées avec la première version à une ligne. */
    public String getText() {
        return this.line1;
    }

    /** Compatibilité avec les anciens appels internes. */
    public void setText(String text) {
        setContent(text, "", RoadTextFont.NORMAL, RoadTextFont.NORMAL);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        String legacyText = input.getStringOr("text", "");
        this.line1 = input.getStringOr("line_1", legacyText);
        this.line2 = input.getStringOr("line_2", "");
        this.line1Font = RoadTextFont.fromSerializedName(
                input.getStringOr("line_1_font", RoadTextFont.NORMAL.getSerializedName())
        );
        this.line2Font = RoadTextFont.fromSerializedName(
                input.getStringOr("line_2_font", RoadTextFont.NORMAL.getSerializedName())
        );
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("line_1", this.line1);
        output.putString("line_2", this.line2);
        output.putString("line_1_font", this.line1Font.getSerializedName());
        output.putString("line_2_font", this.line2Font.getSerializedName());
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
