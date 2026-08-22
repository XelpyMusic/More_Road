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
import net.xelpy.moreroad.block.custom.PanonceauEntry;
import net.xelpy.moreroad.block.custom.PanonceauVariant;

/**
 * Données persistantes du support de panonceaux.
 */
public class PanonceauBlockEntity extends BlockEntity {

    public static final int MAX_PANONCEAUX = 3;

    private final PanonceauEntry[] entries = new PanonceauEntry[MAX_PANONCEAUX];

    public PanonceauBlockEntity(BlockPos pos, BlockState state) {
        super(MoreRoadBlockEntities.PANONCEAU.get(), pos, state);
        this.entries[0] = PanonceauEntry.defaultFirst();
        for (int i = 1; i < MAX_PANONCEAUX; i++) {
            this.entries[i] = PanonceauEntry.disabled();
        }
    }

    public PanonceauEntry getEntry(int index) {
        if (index < 0 || index >= MAX_PANONCEAUX) {
            return PanonceauEntry.disabled();
        }
        return this.entries[index];
    }

    public PanonceauEntry[] getEntries() {
        return this.entries.clone();
    }

    public void setEntries(PanonceauEntry[] values) {
        for (int i = 0; i < MAX_PANONCEAUX; i++) {
            this.entries[i] = values != null && i < values.length && values[i] != null
                    ? values[i]
                    : PanonceauEntry.disabled();
        }
        setChanged();
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        for (int i = 0; i < MAX_PANONCEAUX; i++) {
            PanonceauEntry fallback = i == 0
                    ? PanonceauEntry.defaultFirst()
                    : PanonceauEntry.disabled();
            String prefix = "panonceau_" + i + "_";
            PanonceauVariant variant = PanonceauVariant.fromSerializedName(
                    input.getStringOr(prefix + "variant", fallback.variant().getSerializedName())
            );

            this.entries[i] = new PanonceauEntry(
                    input.getBooleanOr(prefix + "enabled", fallback.enabled()),
                    variant,
                    input.getStringOr(prefix + "value", fallback.value())
            );
        }
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);

        for (int i = 0; i < MAX_PANONCEAUX; i++) {
            PanonceauEntry entry = this.entries[i];
            String prefix = "panonceau_" + i + "_";
            output.putBoolean(prefix + "enabled", entry.enabled());
            output.putString(prefix + "variant", entry.variant().getSerializedName());
            output.putString(prefix + "value", entry.value());
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
