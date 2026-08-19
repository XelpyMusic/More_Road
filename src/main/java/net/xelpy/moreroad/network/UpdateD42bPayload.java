package net.xelpy.moreroad.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.xelpy.moreroad.MoreRoad;
import net.xelpy.moreroad.block.custom.D42bBranchData;
import net.xelpy.moreroad.block.custom.D42bLabelColor;
import net.xelpy.moreroad.block.custom.RoadTextFont;
import net.xelpy.moreroad.block.entity.D42bBlockEntity;

public record UpdateD42bPayload(
        BlockPos pos,
        String distanceText,
        D42bBranchData branch0,
        D42bBranchData branch1,
        D42bBranchData branch2,
        D42bBranchData branch3,
        D42bBranchData branch4,
        D42bBranchData branch5
) implements CustomPacketPayload {

    public static final Type<UpdateD42bPayload> TYPE =
            new Type<>(
                    Identifier.fromNamespaceAndPath(
                            MoreRoad.MODID,
                            "update_d42b"
                    )
            );

    public static final StreamCodec<ByteBuf, UpdateD42bPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public UpdateD42bPayload decode(ByteBuf buffer) {
                    return new UpdateD42bPayload(
                            BlockPos.STREAM_CODEC.decode(buffer),
                            ByteBufCodecs.STRING_UTF8.decode(buffer),
                            decodeBranch(buffer),
                            decodeBranch(buffer),
                            decodeBranch(buffer),
                            decodeBranch(buffer),
                            decodeBranch(buffer),
                            decodeBranch(buffer)
                    );
                }

                @Override
                public void encode(ByteBuf buffer, UpdateD42bPayload payload) {
                    BlockPos.STREAM_CODEC.encode(buffer, payload.pos());
                    ByteBufCodecs.STRING_UTF8.encode(buffer, payload.distanceText());

                    for (int i = 0; i < D42bBlockEntity.MAX_BRANCHES; i++) {
                        encodeBranch(buffer, payload.branch(i));
                    }
                }
            };

    public UpdateD42bPayload {
        distanceText = distanceText == null ? "" : distanceText;
        branch0 = normalize(branch0, 0);
        branch1 = normalize(branch1, 1);
        branch2 = normalize(branch2, 2);
        branch3 = normalize(branch3, 3);
        branch4 = normalize(branch4, 4);
        branch5 = normalize(branch5, 5);
    }

    public D42bBranchData branch(int index) {
        return switch (index) {
            case 0 -> branch0;
            case 1 -> branch1;
            case 2 -> branch2;
            case 3 -> branch3;
            case 4 -> branch4;
            case 5 -> branch5;
            default -> D42bBranchData.disabled(0);
        };
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static D42bBranchData normalize(D42bBranchData branch, int index) {
        return branch == null
                ? D42bBranchData.defaultForIndex(index)
                : branch;
    }

    private static void encodeBranch(ByteBuf buffer, D42bBranchData branch) {
        ByteBufCodecs.BOOL.encode(buffer, branch.enabled());
        ByteBufCodecs.VAR_INT.encode(buffer, branch.angleDegrees());
        ByteBufCodecs.STRING_UTF8.encode(buffer, branch.line1());
        ByteBufCodecs.STRING_UTF8.encode(buffer, branch.line2());
        ByteBufCodecs.STRING_UTF8.encode(buffer, branch.line1Font().getSerializedName());
        ByteBufCodecs.STRING_UTF8.encode(buffer, branch.line2Font().getSerializedName());
        ByteBufCodecs.STRING_UTF8.encode(buffer, branch.line1Color().getSerializedName());
        ByteBufCodecs.STRING_UTF8.encode(buffer, branch.line2Color().getSerializedName());
    }

    private static D42bBranchData decodeBranch(ByteBuf buffer) {
        return new D42bBranchData(
                ByteBufCodecs.BOOL.decode(buffer),
                ByteBufCodecs.VAR_INT.decode(buffer),
                ByteBufCodecs.STRING_UTF8.decode(buffer),
                ByteBufCodecs.STRING_UTF8.decode(buffer),
                RoadTextFont.fromSerializedName(ByteBufCodecs.STRING_UTF8.decode(buffer)),
                RoadTextFont.fromSerializedName(ByteBufCodecs.STRING_UTF8.decode(buffer)),
                D42bLabelColor.fromSerializedName(ByteBufCodecs.STRING_UTF8.decode(buffer)),
                D42bLabelColor.fromSerializedName(ByteBufCodecs.STRING_UTF8.decode(buffer))
        );
    }
}
