package net.xelpy.moreroad.block;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.xelpy.moreroad.MoreRoad;
import net.xelpy.moreroad.block.custom.*;
import net.xelpy.moreroad.item.MoreRoadItems;

import java.util.function.Function;

public class MoreRoadBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(MoreRoad.MODID);

    public static final DeferredBlock<Block> A1A = registerBlock("a1a",
            (properties) -> new A1aBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> A1B = registerBlock("a1b",
            (properties) -> new A1bBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> A1C = registerBlock("a1c",
            (properties) -> new A1cBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> A1D = registerBlock("a1d",
            (properties) -> new A1dBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> A2A = registerBlock("a2a",
            (properties) -> new A2aBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> A2B = registerBlock("a2b",
            (properties) -> new A2bBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> A3 = registerBlock("a3",
            (properties) -> new A3Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> A3A = registerBlock("a3a",
            (properties) -> new A3aBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> A3B = registerBlock("a3b",
            (properties) -> new A3bBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> A4 = registerBlock("a4",
            (properties) -> new A4Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> A6 = registerBlock("a6",
            (properties) -> new A6Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> A7 = registerBlock("a7",
            (properties) -> new A7Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> A8 = registerBlock("a8",
            (properties) -> new A8Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> A9 = registerBlock("a9",
            (properties) -> new A9Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> A13A = registerBlock("a13a",
            (properties) -> new A13aBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> A13B = registerBlock("a13b",
            (properties) -> new A13bBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> A14 = registerBlock("a14",
            (properties) -> new A14Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> A15A1 = registerBlock("a15a1",
            (properties) -> new A15a1Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> A15A2 = registerBlock("a15a2",
            (properties) -> new A15a2Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> A15B = registerBlock("a15b",
            (properties) -> new A15bBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> A15C = registerBlock("a15c",
            (properties) -> new A15cBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> A16 = registerBlock("a16",
            (properties) -> new A16Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> A17 = registerBlock("a17",
            (properties) -> new A17Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> A18 = registerBlock("a18",
            (properties) -> new A18Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> A19 = registerBlock("a19",
            (properties) -> new A19Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> A20 = registerBlock("a20",
            (properties) -> new A20Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> A21A = registerBlock("a21a",
            (properties) -> new A21aBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> A23 = registerBlock("a23",
            (properties) -> new A23Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> A24 = registerBlock("a24",
            (properties) -> new A24Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> AB1 = registerBlock("ab1",
            (properties) -> new AB1Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> AB2 = registerBlock("ab2",
            (properties) -> new AB2Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> AB25 = registerBlock("ab25",
            (properties) -> new AB25Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> AB3 = registerBlock("ab3",
            (properties) -> new AB3Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> AB3A = registerBlock("ab3a",
            (properties) -> new AB3aBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> AB3B = registerBlock("ab3b",
            (properties) -> new AB3bBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> AB5 = registerBlock("ab5",
            (properties) -> new AB5Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> AB4 = registerBlock("ab4",
            (properties) -> new AB4Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> AB6 = registerBlock("ab6",
            (properties) -> new AB6Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> AB7 = registerBlock("ab7",
            (properties) -> new AB7Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B0 = registerBlock("b0",
            (properties) -> new B0Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B1 = registerBlock("b1",
            (properties) -> new B1Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B2A = registerBlock("b2a",
            (properties) -> new B2aBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B2B = registerBlock("b2b",
            (properties) -> new B2bBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B2C = registerBlock("b2c",
            (properties) -> new B2cBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B3 = registerBlock("b3",
            (properties) -> new B3Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B3A = registerBlock("b3a",
            (properties) -> new B3aBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B4 = registerBlock("b4",
            (properties) -> new B4Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B5A = registerBlock("b5a",
            (properties) -> new B5aBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B5B = registerBlock("b5b",
            (properties) -> new B5bBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B3C = registerBlock("b3c",
            (properties) -> new B5cBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B5C = registerBlock("b5c",
            (properties) -> new B5cBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B6A1 = registerBlock("b6a1",
            (properties) -> new B6a1Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B6A2 = registerBlock("b6a2",
            (properties) -> new B6a2Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B6A3 = registerBlock("b6a3",
            (properties) -> new B6a3Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B6D = registerBlock("b6d",
            (properties) -> new B6dBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B7A = registerBlock("b7a",
            (properties) -> new B7aBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B7B = registerBlock("b7b",
            (properties) -> new B7bBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B8 = registerBlock("b8",
            (properties) -> new B8Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B9A = registerBlock("b9a",
            (properties) -> new B9aBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B9B = registerBlock("b9b",
            (properties) -> new B9bBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B9C = registerBlock("b9c",
            (properties) -> new B9cBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B9D = registerBlock("b9d",
            (properties) -> new B9dBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B9E = registerBlock("b9e",
            (properties) -> new B9eBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B9F = registerBlock("b9f",
            (properties) -> new B9fBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B9G = registerBlock("b9g",
            (properties) -> new B9gBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B9H = registerBlock("b9h",
            (properties) -> new B9hBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B9I = registerBlock("b9i",
            (properties) -> new B9iBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B10A = registerBlock("b10a",
            (properties) -> new B10aBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B11 = registerBlock("b11",
            (properties) -> new B11Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B12 = registerBlock("b12",
            (properties) -> new B12Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B13 = registerBlock("b13",
            (properties) -> new B13Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B13A = registerBlock("b13a",
            (properties) -> new B13aBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B14_15 = registerBlock("b14_15",
            (properties) -> new B14_15Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B14_30 = registerBlock("b14_30",
            (properties) -> new B14_30Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B14_50 = registerBlock("b14_50",
            (properties) -> new B14_50Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B14_70 = registerBlock("b14_70",
            (properties) -> new B14_70Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B14_90 = registerBlock("b14_90",
            (properties) -> new B14_90Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B14_110 = registerBlock("b14_110",
            (properties) -> new B14_110Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B14_130 = registerBlock("b14_130",
            (properties) -> new B14_130Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B15 = registerBlock("b15",
            (properties) -> new B15Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B16 = registerBlock("b16",
            (properties) -> new B16Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B17 = registerBlock("b17",
            (properties) -> new B17Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B18A = registerBlock("b18a",
            (properties) -> new B18aBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B18B = registerBlock("b18b",
            (properties) -> new B18bBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B18C = registerBlock("b18c",
            (properties) -> new B18cBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B19 = registerBlock("b19",
            (properties) -> new B19Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B21_1 = registerBlock("b21-1",
            (properties) -> new B21_1Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B21_2 = registerBlock("b21-2",
            (properties) -> new B21_2Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B21A1 = registerBlock("b21a1",
            (properties) -> new B21a1Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B21A2 = registerBlock("b21a2",
            (properties) -> new B21a2Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B21B = registerBlock("b21b",
            (properties) -> new B21bBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B21C1 = registerBlock("b21c1",
            (properties) -> new B21c1Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B21C2 = registerBlock("b21c2",
            (properties) -> new B21c2Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B21D1 = registerBlock("b21d1",
            (properties) -> new B21d1Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B21D2 = registerBlock("b21d2",
            (properties) -> new B21d2Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B21E = registerBlock("b21e",
            (properties) -> new B21eBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B22A = registerBlock("b22a",
            (properties) -> new B22aBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B22B = registerBlock("b22b",
            (properties) -> new B22bBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B22C = registerBlock("b22c",
            (properties) -> new B22cBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B25 = registerBlock("b25",
            (properties) -> new B25Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B26 = registerBlock("b26",
            (properties) -> new B26Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B27A = registerBlock("b27a",
            (properties) -> new B27aBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B27B = registerBlock("b27b",
            (properties) -> new B27bBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B29 = registerBlock("b29",
            (properties) -> new B29Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B31 = registerBlock("b31",
            (properties) -> new B31Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B33_15 = registerBlock("b33_15",
            (properties) -> new B33_15Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B33_30 = registerBlock("b33_30",
            (properties) -> new B33_30Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B33_50 = registerBlock("b33_50",
            (properties) -> new B33_50Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B33_70 = registerBlock("b33_70",
            (properties) -> new B33_70Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B33_90 = registerBlock("b33_90",
            (properties) -> new B33_90Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B33_110 = registerBlock("b33_110",
            (properties) -> new B33_110Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B33_130 = registerBlock("b33_130",
            (properties) -> new B33_130Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B34 = registerBlock("b34",
            (properties) -> new B34Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B34A = registerBlock("b34a",
            (properties) -> new B34aBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B35 = registerBlock("b35",
            (properties) -> new B35Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B39 = registerBlock("b39",
            (properties) -> new B39Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B40 = registerBlock("b40",
            (properties) -> new B40Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B41 = registerBlock("b41",
            (properties) -> new B41Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B42 = registerBlock("b42",
            (properties) -> new B42Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B43 = registerBlock("b43",
            (properties) -> new B43Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B44 = registerBlock("b44",
            (properties) -> new B44Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B45A = registerBlock("b45a",
            (properties) -> new B45aBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> B49 = registerBlock("b49",
            (properties) -> new B49Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> C1A = registerBlock("c1a",
            (properties) -> new C1aBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> C1B = registerBlock("c1b",
            (properties) -> new C1bBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> C1C = registerBlock("c1c",
            (properties) -> new C1cBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> C3 = registerBlock("c3",
            (properties) -> new C3Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> C4A_50 = registerBlock("c4a_50",
            (properties) -> new C4a_50Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> C4B_50 = registerBlock("c4b_50",
            (properties) -> new C4b_50Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> C5 = registerBlock("c5",
            (properties) -> new C5Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> C6 = registerBlock("c6",
            (properties) -> new C6Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> C8 = registerBlock("c8",
            (properties) -> new C8Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> C12 = registerBlock("c12",
            (properties) -> new C12Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> C13A = registerBlock("c13a",
            (properties) -> new C13aBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> C13B = registerBlock("c13b",
            (properties) -> new C13bBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> C14_1 = registerBlock("c14_1",
            (properties) -> new C14_1Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> C14_2 = registerBlock("c14_2",
            (properties) -> new C14_2Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> C18 = registerBlock("c18",
            (properties) -> new C18Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> C20A = registerBlock("c20a",
            (properties) -> new C20aBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> C20C = registerBlock("c20c",
            (properties) -> new C20cBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> C23 = registerBlock("c23",
            (properties) -> new C23Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> C24A_1 = registerBlock("c24a_1",
            (properties) -> new C24a_1Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> C24A_4 = registerBlock("c24a_4",
            (properties) -> new C24a_4Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> C24B_1 = registerBlock("c24b_1",
            (properties) -> new C24b_1Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> C24B_2 = registerBlock("c24b_2",
            (properties) -> new C24b_2Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> C24C_1 = registerBlock("c24c_1",
            (properties) -> new C24c_1Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> C24C_2 = registerBlock("c24c_2",
            (properties) -> new C24c_2Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> C25A = registerBlock("c25a",
            (properties) -> new C25aBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> C25B = registerBlock("c25b",
            (properties) -> new C25bBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> C26A = registerBlock("c26a",
            (properties) -> new C26aBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> C26B = registerBlock("c26b",
            (properties) -> new C26bBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> C27 = registerBlock("c27",
            (properties) -> new C27Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> C28_1 = registerBlock("c28_1",
            (properties) -> new C28_1Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> C28_3 = registerBlock("c28_3",
            (properties) -> new C28_3Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> C29A = registerBlock("c29a",
            (properties) -> new C29aBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> C29B = registerBlock("c29b",
            (properties) -> new C29bBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> C30 = registerBlock("c30",
            (properties) -> new C30Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> C50 = registerBlock("c50",
            (properties) -> new C50Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> C62 = registerBlock("c62",
            (properties) -> new C62Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> C64A = registerBlock("c64a",
            (properties) -> new C64aBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> C64B = registerBlock("c64b",
            (properties) -> new C64bBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> C64C = registerBlock("c64c",
            (properties) -> new C64cBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> C64D_1 = registerBlock("c64d_1",
            (properties) -> new C64d_1Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> C64D_2 = registerBlock("c64d_2",
            (properties) -> new C64d_2Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> C107 = registerBlock("c107",
            (properties) -> new C107Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> C108 = registerBlock("c108",
            (properties) -> new C108Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> C111 = registerBlock("c111",
            (properties) -> new C111Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> C112 = registerBlock("c112",
            (properties) -> new C112Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> C113 = registerBlock("c113",
            (properties) -> new C113Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> C114 = registerBlock("c114",
            (properties) -> new C114Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> C115 = registerBlock("c115",
            (properties) -> new C115Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> C116 = registerBlock("c116",
            (properties) -> new C116Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> C207 = registerBlock("c207",
            (properties) -> new C207Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> C208 = registerBlock("c208",
            (properties) -> new C208Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> CE1 = registerBlock("ce1",
            (properties) -> new CE1Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> CE2A = registerBlock("ce2a",
            (properties) -> new CE2aBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> CE2B = registerBlock("ce2b",
            (properties) -> new CE2bBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> CE3A = registerBlock("ce3a",
            (properties) -> new CE3aBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> CE4A = registerBlock("ce4a",
            (properties) -> new CE4aBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> CE4B = registerBlock("ce4b",
            (properties) -> new CE4bBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> CE4C = registerBlock("ce4c",
            (properties) -> new CE4cBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> CE5A = registerBlock("ce5a",
            (properties) -> new CE5aBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> CE5B = registerBlock("ce5b",
            (properties) -> new CE5bBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> CE6A = registerBlock("ce6a",
            (properties) -> new CE6aBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> CE6B = registerBlock("ce6b",
            (properties) -> new CE6bBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> CE7 = registerBlock("ce7",
            (properties) -> new CE7Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> CE8 = registerBlock("ce8",
            (properties) -> new CE8Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> CE9 = registerBlock("ce9",
            (properties) -> new CE9Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> CE10 = registerBlock("ce10",
            (properties) -> new CE10Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> CE12 = registerBlock("ce12",
            (properties) -> new CE12Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> CE14 = registerBlock("ce14",
            (properties) -> new CE14Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> CE15A = registerBlock("ce15a",
            (properties) -> new CE15aBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> CE15C = registerBlock("ce15c",
            (properties) -> new CE15cBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> CE16 = registerBlock("ce16",
            (properties) -> new CE16Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> CE17 = registerBlock("ce17",
            (properties) -> new CE17Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> CE18 = registerBlock("ce18",
            (properties) -> new CE18Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> CE19 = registerBlock("ce19",
            (properties) -> new CE19Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> CE20A = registerBlock("ce20a",
            (properties) -> new CE20aBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> CE20B = registerBlock("ce20b",
            (properties) -> new CE20bBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> CE21 = registerBlock("ce21",
            (properties) -> new CE21Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> CE22 = registerBlock("ce22",
            (properties) -> new CE22Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> CE23 = registerBlock("ce23",
            (properties) -> new CE23Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> CE24 = registerBlock("ce24",
            (properties) -> new CE24Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> CE25 = registerBlock("ce25",
            (properties) -> new CE25Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> CE26 = registerBlock("ce26",
            (properties) -> new CE26Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> CE27 = registerBlock("ce27",
            (properties) -> new CE27Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> CE28 = registerBlock("ce28",
            (properties) -> new CE28Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> CE29 = registerBlock("ce29",
            (properties) -> new CE29Block(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> CE30a = registerBlock("ce30a",
            (properties) -> new CE30aBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> CE30b = registerBlock("ce30b",
            (properties) -> new CE30bBlock(properties
                    .noOcclusion().sound(SoundType.IRON)));

    public static final DeferredBlock<Block> CE50 = registerBlock("ce50",
            (properties) -> new CE50Block(properties
                    .noOcclusion().sound(SoundType.IRON)));



    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Function<BlockBehaviour.Properties, T> function) {
        DeferredBlock<T> toReturn = BLOCKS.registerBlock(name, function);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        MoreRoadItems.ITEMS.registerItem(name, (properties) -> new BlockItem(block.get(), properties.useBlockDescriptionPrefix()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}