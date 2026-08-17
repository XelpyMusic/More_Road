package net.xelpy.moreroad.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.xelpy.moreroad.MoreRoad;

public record UpdateEB10TextPayload(
        BlockPos pos,
        String line1,
        String line2,
        boolean eb20
) implements CustomPacketPayload {

    public static final Type<UpdateEB10TextPayload> TYPE =
            new Type<>(
                    Identifier.fromNamespaceAndPath(
                            MoreRoad.MODID,
                            "update_eb10_text"
                    )
            );


    public static final StreamCodec<ByteBuf, UpdateEB10TextPayload> STREAM_CODEC =
            StreamCodec.composite(

                    BlockPos.STREAM_CODEC,
                    UpdateEB10TextPayload::pos,

                    ByteBufCodecs.STRING_UTF8,
                    UpdateEB10TextPayload::line1,

                    ByteBufCodecs.STRING_UTF8,
                    UpdateEB10TextPayload::line2,

                    ByteBufCodecs.BOOL,
                    UpdateEB10TextPayload::eb20,

                    UpdateEB10TextPayload::new
            );


    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}