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
import net.xelpy.moreroad.block.custom.D61AArrowPosition;
import net.xelpy.moreroad.block.custom.GenericArrowShape;
import net.xelpy.moreroad.block.custom.GenericDestinationRow;
import net.xelpy.moreroad.block.custom.GenericDirectionalSignData;
import net.xelpy.moreroad.block.custom.GenericRouteCartoucheData;
import net.xelpy.moreroad.block.custom.GenericSignAlignment;
import net.xelpy.moreroad.block.custom.GenericSignHeader;
import net.xelpy.moreroad.block.custom.GenericSignSymbol;
import net.xelpy.moreroad.block.custom.MotorwaySignColor;
import net.xelpy.moreroad.block.custom.RoadTextFont;

/**
 * Données persistantes du panneau directionnel modulable générique.
 *
 * Posé vierge (voir {@link GenericDirectionalSignData#blank()}) : aucune
 * ville, aucun en-tête, aucun cartouche, aucune flèche par défaut, comme le
 * D61A/D21A — le joueur construit son panneau dans l'éditeur.
 */
public class GenericDirectionalSignBlockEntity extends BlockEntity {

    private GenericDirectionalSignData data = GenericDirectionalSignData.blank();

    public GenericDirectionalSignBlockEntity(BlockPos pos, BlockState state) {
        super(MoreRoadBlockEntities.GENERIC_DIRECTIONAL_SIGN.get(), pos, state);
    }

    public GenericDirectionalSignData getData() {
        return this.data;
    }

    public void setData(GenericDirectionalSignData data) {
        this.data = data == null ? GenericDirectionalSignData.blank() : data;
        setChanged();
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        MotorwaySignColor background = MotorwaySignColor.fromSerializedName(
                input.getStringOr("background", MotorwaySignColor.BLUE.getSerializedName())
        );

        GenericSignHeader headerFallback = GenericSignHeader.blank();
        GenericSignHeader header = new GenericSignHeader(
                input.getBooleanOr("header_enabled", headerFallback.enabled()),
                input.getStringOr("header_text", headerFallback.text()),
                input.getBooleanOr("header_same_as_panel", headerFallback.sameAsPanel()),
                MotorwaySignColor.fromSerializedName(
                        input.getStringOr("header_color", headerFallback.color().getSerializedName())
                ),
                GenericSignAlignment.fromSerializedName(
                        input.getStringOr("header_alignment", headerFallback.alignment().getSerializedName())
                ),
                RoadTextFont.fromSerializedName(
                        input.getStringOr("header_font", headerFallback.font().getSerializedName())
                )
        );

        GenericDestinationRow[] rows = new GenericDestinationRow[GenericDirectionalSignData.MAX_ROWS];
        for (int i = 0; i < rows.length; i++) {
            String prefix = "row_" + i + "_";
            GenericDestinationRow fallback = GenericDestinationRow.blank();
            rows[i] = new GenericDestinationRow(
                    input.getBooleanOr(prefix + "enabled", fallback.enabled()),
                    input.getStringOr(prefix + "text", fallback.text()),
                    GenericSignAlignment.fromSerializedName(
                            input.getStringOr(prefix + "alignment", fallback.alignment().getSerializedName())
                    ),
                    RoadTextFont.fromSerializedName(
                            input.getStringOr(prefix + "font", fallback.font().getSerializedName())
                    ),
                    input.getBooleanOr(prefix + "arrow_enabled", fallback.arrowEnabled()),
                    GenericArrowShape.fromSerializedName(
                            input.getStringOr(prefix + "arrow_shape", fallback.arrowShape().getSerializedName())
                    ),
                    input.getBooleanOr(prefix + "arrow_mirrored", fallback.arrowMirrored()),
                    D61AArrowPosition.fromSerializedName(
                            input.getStringOr(prefix + "arrow_position", fallback.arrowPosition().getSerializedName())
                    ),
                    input.getBooleanOr(prefix + "symbol_enabled", fallback.symbolEnabled()),
                    GenericSignSymbol.fromSerializedName(
                            input.getStringOr(prefix + "symbol", fallback.symbol().getSerializedName())
                    ),
                    D61AArrowPosition.fromSerializedName(
                            input.getStringOr(prefix + "symbol_position", fallback.symbolPosition().getSerializedName())
                    )
            );
        }

        GenericRouteCartoucheData[] cartouches = new GenericRouteCartoucheData[GenericDirectionalSignData.MAX_CARTOUCHES];
        for (int i = 0; i < cartouches.length; i++) {
            String prefix = "cartouche_" + i + "_";
            cartouches[i] = new GenericRouteCartoucheData(
                    CartoucheType.fromSerializedName(input.getStringOr(prefix + "type", CartoucheType.NONE.getSerializedName())),
                    input.getStringOr(prefix + "text", "")
            );
        }

        this.data = new GenericDirectionalSignData(background, header, rows, cartouches);
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("background", this.data.background().getSerializedName());

        GenericSignHeader header = this.data.header();
        output.putBoolean("header_enabled", header.enabled());
        output.putString("header_text", header.text());
        output.putBoolean("header_same_as_panel", header.sameAsPanel());
        output.putString("header_color", header.color().getSerializedName());
        output.putString("header_alignment", header.alignment().getSerializedName());
        output.putString("header_font", header.font().getSerializedName());

        for (int i = 0; i < this.data.rows().length; i++) {
            GenericDestinationRow row = this.data.rows()[i];
            String prefix = "row_" + i + "_";
            output.putBoolean(prefix + "enabled", row.enabled());
            output.putString(prefix + "text", row.text());
            output.putString(prefix + "alignment", row.alignment().getSerializedName());
            output.putString(prefix + "font", row.font().getSerializedName());
            output.putBoolean(prefix + "arrow_enabled", row.arrowEnabled());
            output.putString(prefix + "arrow_shape", row.arrowShape().getSerializedName());
            output.putBoolean(prefix + "arrow_mirrored", row.arrowMirrored());
            output.putString(prefix + "arrow_position", row.arrowPosition().getSerializedName());
            output.putBoolean(prefix + "symbol_enabled", row.symbolEnabled());
            output.putString(prefix + "symbol", row.symbol().getSerializedName());
            output.putString(prefix + "symbol_position", row.symbolPosition().getSerializedName());
        }
        for (int i = 0; i < this.data.cartouches().length; i++) {
            GenericRouteCartoucheData cartouche = this.data.cartouches()[i];
            String prefix = "cartouche_" + i + "_";
            output.putString(prefix + "type", cartouche.type().getSerializedName());
            output.putString(prefix + "text", cartouche.text());
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
