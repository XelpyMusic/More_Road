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
import net.xelpy.moreroad.block.custom.DA31CArrowType;
import net.xelpy.moreroad.block.custom.RoadTextFont;

public class DA31CBlockEntity extends BlockEntity {

    private String line1 = "";
    private String line2 = "";
    private String line3 = "";
    private String line4 = "";
    private RoadTextFont line1Font = RoadTextFont.L1;
    private RoadTextFont line2Font = RoadTextFont.L1;
    private RoadTextFont line3Font = RoadTextFont.L1;
    private RoadTextFont line4Font = RoadTextFont.L1;

    private CartoucheType cartoucheTopType = CartoucheType.NONE;
    private String cartoucheTopText = "";

    private CartoucheType cartoucheLeftType = CartoucheType.NONE;
    private String cartoucheLeftText = "";

    private CartoucheType cartoucheRightType = CartoucheType.NONE;
    private String cartoucheRightText = "";

    private DA31CArrowType arrowLeftType = DA31CArrowType.DOWN;
    private DA31CArrowType arrowRightType = DA31CArrowType.DOWN;

    public DA31CBlockEntity(BlockPos pos, BlockState state) {
        super(MoreRoadBlockEntities.DA31C.get(), pos, state);
    }

    public String getLine1() { return this.line1; }
    public String getLine2() { return this.line2; }
    public String getLine3() { return this.line3; }
    public String getLine4() { return this.line4; }
    public RoadTextFont getLine1Font() { return this.line1Font; }
    public RoadTextFont getLine2Font() { return this.line2Font; }
    public RoadTextFont getLine3Font() { return this.line3Font; }
    public RoadTextFont getLine4Font() { return this.line4Font; }

    public CartoucheType getCartoucheTopType() { return this.cartoucheTopType; }
    public String getCartoucheTopText() { return this.cartoucheTopText; }

    public CartoucheType getCartoucheLeftType() { return this.cartoucheLeftType; }
    public String getCartoucheLeftText() { return this.cartoucheLeftText; }

    public CartoucheType getCartoucheRightType() { return this.cartoucheRightType; }
    public String getCartoucheRightText() { return this.cartoucheRightText; }

    public DA31CArrowType getArrowLeftType() { return this.arrowLeftType; }
    public DA31CArrowType getArrowRightType() { return this.arrowRightType; }

    public void setData(
            String line1,
            String line2,
            String line3,
            String line4,
            RoadTextFont line1Font,
            RoadTextFont line2Font,
            RoadTextFont line3Font,
            RoadTextFont line4Font,
            CartoucheType cartoucheTopType,
            String cartoucheTopText,
            CartoucheType cartoucheLeftType,
            String cartoucheLeftText,
            CartoucheType cartoucheRightType,
            String cartoucheRightText,
            DA31CArrowType arrowLeftType,
            DA31CArrowType arrowRightType
    ) {
        this.line1 = line1 == null ? "" : line1;
        this.line2 = line2 == null ? "" : line2;
        this.line3 = line3 == null ? "" : line3;
        this.line4 = line4 == null ? "" : line4;
        this.line1Font = line1Font == null ? RoadTextFont.L1 : line1Font;
        this.line2Font = line2Font == null ? RoadTextFont.L1 : line2Font;
        this.line3Font = line3Font == null ? RoadTextFont.L1 : line3Font;
        this.line4Font = line4Font == null ? RoadTextFont.L1 : line4Font;
        this.cartoucheTopType = cartoucheTopType == null ? CartoucheType.NONE : cartoucheTopType;
        this.cartoucheTopText = cartoucheTopText == null ? "" : cartoucheTopText;
        this.cartoucheLeftType = cartoucheLeftType == null ? CartoucheType.NONE : cartoucheLeftType;
        this.cartoucheLeftText = cartoucheLeftText == null ? "" : cartoucheLeftText;
        this.cartoucheRightType = cartoucheRightType == null ? CartoucheType.NONE : cartoucheRightType;
        this.cartoucheRightText = cartoucheRightText == null ? "" : cartoucheRightText;
        this.arrowLeftType = arrowLeftType == null ? DA31CArrowType.DOWN : arrowLeftType;
        this.arrowRightType = arrowRightType == null ? DA31CArrowType.DOWN : arrowRightType;
        setChanged();
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        this.line1 = input.getStringOr("line1", "");
        this.line2 = input.getStringOr("line2", "");
        this.line3 = input.getStringOr("line3", "");
        this.line4 = input.getStringOr("line4", "");
        this.line1Font = RoadTextFont.fromSerializedName(input.getStringOr("line1_font", "l1"));
        this.line2Font = RoadTextFont.fromSerializedName(input.getStringOr("line2_font", "l1"));
        this.line3Font = RoadTextFont.fromSerializedName(input.getStringOr("line3_font", "l1"));
        this.line4Font = RoadTextFont.fromSerializedName(input.getStringOr("line4_font", "l1"));

        this.cartoucheTopType = CartoucheType.fromSerializedName(
                input.getStringOr("cartouche_top_type", "none")
        );
        this.cartoucheTopText = input.getStringOr("cartouche_top_text", "");

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

        this.arrowLeftType = DA31CArrowType.fromSerializedName(
                input.getStringOr("arrow_left_type", "down")
        );
        this.arrowRightType = DA31CArrowType.fromSerializedName(
                input.getStringOr("arrow_right_type", "down")
        );
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);

        output.putString("line1", this.line1);
        output.putString("line2", this.line2);
        output.putString("line3", this.line3);
        output.putString("line4", this.line4);
        output.putString("line1_font", this.line1Font.getSerializedName());
        output.putString("line2_font", this.line2Font.getSerializedName());
        output.putString("line3_font", this.line3Font.getSerializedName());
        output.putString("line4_font", this.line4Font.getSerializedName());

        output.putString("cartouche_top_type", this.cartoucheTopType.getSerializedName());
        output.putString("cartouche_top_text", this.cartoucheTopText);
        output.putString("cartouche_left_type", this.cartoucheLeftType.getSerializedName());
        output.putString("cartouche_left_text", this.cartoucheLeftText);
        output.putString("cartouche_right_type", this.cartoucheRightType.getSerializedName());
        output.putString("cartouche_right_text", this.cartoucheRightText);
        output.putString("arrow_left_type", this.arrowLeftType.getSerializedName());
        output.putString("arrow_right_type", this.arrowRightType.getSerializedName());
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
