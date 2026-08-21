package net.xelpy.moreroad.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.xelpy.moreroad.MoreRoad;

/**
 * Validation finale de la prévisualisation du constructeur de route.
 *
 * Le client envoie les trois points ajustés ; le serveur recalcule lui-même
 * toute la géométrie avant de modifier le monde.
 */
public record BuildRoadPayload(
        BlockPos start,
        BlockPos control,
        BlockPos end
) implements CustomPacketPayload {

    public static final Type<BuildRoadPayload> TYPE =
            new Type<>(
                    Identifier.fromNamespaceAndPath(
                            MoreRoad.MODID,
                            "build_road"
                    )
            );

    public static final StreamCodec<ByteBuf, BuildRoadPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public BuildRoadPayload decode(ByteBuf buffer) {
                    return new BuildRoadPayload(
                            BlockPos.STREAM_CODEC.decode(buffer),
                            BlockPos.STREAM_CODEC.decode(buffer),
                            BlockPos.STREAM_CODEC.decode(buffer)
                    );
                }

                @Override
                public void encode(
                        ByteBuf buffer,
                        BuildRoadPayload payload
                ) {
                    BlockPos.STREAM_CODEC.encode(buffer, payload.start());
                    BlockPos.STREAM_CODEC.encode(buffer, payload.control());
                    BlockPos.STREAM_CODEC.encode(buffer, payload.end());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
