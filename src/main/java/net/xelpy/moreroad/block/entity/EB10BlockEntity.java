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
import net.xelpy.moreroad.block.custom.CartoucheType;

public class EB10BlockEntity extends BlockEntity {

    private String line1 = "";
    private String line2 = "";
    private CartoucheType cartoucheType = CartoucheType.NONE;
    private String cartoucheText = "";

    public EB10BlockEntity(BlockPos pos, BlockState state) {
        super(MoreRoadBlockEntities.EB10.get(), pos, state);
    }

    public String getLine1() {
        return line1;
    }

    public String getLine2() {
        return line2;
    }

    public CartoucheType getCartoucheType() {
        return this.cartoucheType;
    }

    public void setCartoucheType(CartoucheType cartoucheType) {
        this.cartoucheType =
                cartoucheType == null
                        ? CartoucheType.NONE
                        : cartoucheType;

        setChanged();
    }

    public String getCartoucheText() {
        return this.cartoucheText;
    }

    public void setCartoucheText(String cartoucheText) {
        this.cartoucheText =
                cartoucheText == null
                        ? ""
                        : cartoucheText;

        setChanged();
    }

    public void setText(String line1, String line2) {
        this.line1 = line1 == null ? "" : line1;
        this.line2 = line2 == null ? "" : line2;

        setChanged();
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        // Compatibilité avec l'ancien système à une seule ligne.
        String oldCityName = input.getStringOr("city_name", "");

        this.line1 = input.getStringOr("line_1", oldCityName);
        this.line2 = input.getStringOr("line_2", "");
        this.cartoucheType = CartoucheType.fromSerializedName(
                input.getStringOr("cartouche_type", "none")
        );
        this.cartoucheText = input.getStringOr("cartouche_text", "");
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);

        output.putString("line_1", this.line1);
        output.putString("line_2", this.line2);
        output.putString(
                "cartouche_type",
                this.cartoucheType.getSerializedName()
        );
        output.putString("cartouche_text", this.cartoucheText);
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