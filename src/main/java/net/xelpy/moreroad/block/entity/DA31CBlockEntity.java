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

public class DA31CBlockEntity extends BlockEntity {

    private String line1 = "";
    private String line2 = "";

    private CartoucheType cartoucheLeftType = CartoucheType.NONE;
    private String cartoucheLeftText = "";

    private CartoucheType cartoucheRightType = CartoucheType.NONE;
    private String cartoucheRightText = "";

    public DA31CBlockEntity(BlockPos pos, BlockState state) {
        super(MoreRoadBlockEntities.DA31C.get(), pos, state);
    }

    public String getLine1() {
        return this.line1;
    }

    public String getLine2() {
        return this.line2;
    }

    public CartoucheType getCartoucheLeftType() {
        return this.cartoucheLeftType;
    }

    public String getCartoucheLeftText() {
        return this.cartoucheLeftText;
    }

    public CartoucheType getCartoucheRightType() {
        return this.cartoucheRightType;
    }

    public String getCartoucheRightText() {
        return this.cartoucheRightText;
    }

    public void setData(
            String line1,
            String line2,
            CartoucheType cartoucheLeftType,
            String cartoucheLeftText,
            CartoucheType cartoucheRightType,
            String cartoucheRightText
    ) {
        this.line1 = line1 == null ? "" : line1;
        this.line2 = line2 == null ? "" : line2;
        this.cartoucheLeftType = cartoucheLeftType == null ? CartoucheType.NONE : cartoucheLeftType;
        this.cartoucheLeftText = cartoucheLeftText == null ? "" : cartoucheLeftText;
        this.cartoucheRightType = cartoucheRightType == null ? CartoucheType.NONE : cartoucheRightType;
        this.cartoucheRightText = cartoucheRightText == null ? "" : cartoucheRightText;
        setChanged();
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        this.line1 = input.getStringOr("line1", "");
        this.line2 = input.getStringOr("line2", "");

        /*
         * Compatibilité avec les premières versions du DA31C :
         * - gauche = ancien cartouche autoroute rouge ;
         * - droite = ancien cartouche européen vert.
         */
        String leftFallback = input.getBooleanOr("autoroute_enabled", false)
                ? CartoucheType.E42.getSerializedName()
                : CartoucheType.NONE.getSerializedName();
        String rightFallback = input.getBooleanOr("european_enabled", false)
                ? CartoucheType.E41_45.getSerializedName()
                : CartoucheType.NONE.getSerializedName();

        this.cartoucheLeftType = CartoucheType.fromSerializedName(
                input.getStringOr("cartouche_left_type", leftFallback)
        );
        this.cartoucheLeftText = input.getStringOr(
                "cartouche_left_text",
                input.getStringOr("autoroute_text", "")
        );

        this.cartoucheRightType = CartoucheType.fromSerializedName(
                input.getStringOr("cartouche_right_type", rightFallback)
        );
        this.cartoucheRightText = input.getStringOr(
                "cartouche_right_text",
                input.getStringOr("european_text", "")
        );
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);

        output.putString("line1", this.line1);
        output.putString("line2", this.line2);
        output.putString("cartouche_left_type", this.cartoucheLeftType.getSerializedName());
        output.putString("cartouche_left_text", this.cartoucheLeftText);
        output.putString("cartouche_right_type", this.cartoucheRightType.getSerializedName());
        output.putString("cartouche_right_text", this.cartoucheRightText);
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
