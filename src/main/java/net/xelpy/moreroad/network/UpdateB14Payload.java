package net.xelpy.moreroad.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.xelpy.moreroad.MoreRoad;
import net.xelpy.moreroad.block.custom.B14Speed;

public record UpdateB14Payload(
        BlockPos pos,
        B14Speed speed
) implements CustomPacketPayload {

    public static final Type<UpdateB14Payload> TYPE =
            new Type<>(
                    Identifier.fromNamespaceAndPath(
                            MoreRoad.MODID,
                            "update_b14"
                    )
            );

    public static final StreamCodec<ByteBuf, UpdateB14Payload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public UpdateB14Payload decode(ByteBuf buffer) {
                    BlockPos pos = BlockPos.STREAM_CODEC.decode(buffer);
                    String speedValue = ByteBufCodecs.STRING_UTF8.decode(buffer);

                    return new UpdateB14Payload(
                            pos,
                            B14Speed.fromSerializedName(speedValue)
                    );
                }

                @Override
                public void encode(
                        ByteBuf buffer,
                        UpdateB14Payload payload
                ) {
                    BlockPos.STREAM_CODEC.encode(buffer, payload.pos());
                    ByteBufCodecs.STRING_UTF8.encode(
                            buffer,
                            payload.speed().getSerializedName()
                    );
                }
            };

    public UpdateB14Payload {
        if (speed == null) {
            speed = B14Speed.KMH_5;
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
