package net.xelpy.moreroad.network;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.xelpy.moreroad.block.custom.B14Speed;
import net.xelpy.moreroad.block.custom.CartoucheType;
import net.xelpy.moreroad.block.custom.B14_5Block;
import net.xelpy.moreroad.block.custom.D21ABlock;
import net.xelpy.moreroad.block.custom.D21APanelData;
import net.xelpy.moreroad.block.custom.D21AType;
import net.xelpy.moreroad.block.custom.DA31CBlock;
import net.xelpy.moreroad.block.custom.D42bBranchData;
import net.xelpy.moreroad.block.custom.D61APanelData;
import net.xelpy.moreroad.block.custom.EB10Block;
import net.xelpy.moreroad.block.custom.PanonceauEntry;
import net.xelpy.moreroad.block.custom.PanonceauVariant;
import net.xelpy.moreroad.block.entity.D21ABlockEntity;
import net.xelpy.moreroad.block.entity.DA31CBlockEntity;
import net.xelpy.moreroad.block.entity.D42bBlockEntity;
import net.xelpy.moreroad.block.entity.D61ABlockEntity;
import net.xelpy.moreroad.block.entity.EB10BlockEntity;
import net.xelpy.moreroad.block.entity.E31BlockEntity;
import net.xelpy.moreroad.block.entity.PanonceauBlockEntity;
import net.xelpy.moreroad.item.MoreRoadItems;
import net.xelpy.moreroad.item.RoadBuilderItem;

public final class MoreRoadNetworking {

    /*
     * ============================================================
     * LIMITES
     * ============================================================
     */

    private static final int MAX_EB10_LINE_LENGTH = 32;

    private static final int MAX_E31_TEXT_LENGTH = 48;

    private static final int MAX_DA31C_LINE_LENGTH = 48;

    private static final int MAX_DA31C_CARTOUCHE_LENGTH = 24;

    private static final int MAX_D21A_LINE_LENGTH = 48;

    private static final int MAX_D21A_DISTANCE_LENGTH = 8;

    private static final int MAX_D42B_LINE_LENGTH = 48;

    private static final int MAX_D42B_DISTANCE_LENGTH = 16;

    private static final int MAX_CARTOUCHE_TEXT_LENGTH = 24;

    private static final int MAX_PANONCEAU_VALUE_LENGTH = 73;

    private static final int MAX_EDIT_DISTANCE = 8;


    /*
     * ============================================================
     * CONSTRUCTEUR PRIVÉ
     * ============================================================
     */

    private MoreRoadNetworking() {
    }


    /*
     * ============================================================
     * ENREGISTREMENT DES PAYLOADS
     * ============================================================
     */

    public static void register(
            RegisterPayloadHandlersEvent event
    ) {

        PayloadRegistrar registrar =
                event.registrar("1");


        /*
         * --------------------------------------------------------
         * EB10 / EB20
         * --------------------------------------------------------
         */

        registrar.playToServer(
                UpdateEB10TextPayload.TYPE,
                UpdateEB10TextPayload.STREAM_CODEC,
                MoreRoadNetworking::handleUpdateEB10Text
        );


        /*
         * --------------------------------------------------------
         * E31a / E31b
         * --------------------------------------------------------
         */

        registrar.playToServer(
                UpdateE31TextPayload.TYPE,
                UpdateE31TextPayload.STREAM_CODEC,
                MoreRoadNetworking::handleUpdateE31Text
        );

        registrar.playToServer(
                UpdateDA31CPayload.TYPE,
                UpdateDA31CPayload.STREAM_CODEC,
                MoreRoadNetworking::handleUpdateDA31C
        );


        /*
         * --------------------------------------------------------
         * D21A
         * --------------------------------------------------------
         */

        registrar.playToServer(
                UpdateD21APayload.TYPE,
                UpdateD21APayload.STREAM_CODEC,
                MoreRoadNetworking::handleUpdateD21A
        );

        registrar.playToServer(
                UpdateD42bPayload.TYPE,
                UpdateD42bPayload.STREAM_CODEC,
                MoreRoadNetworking::handleUpdateD42b
        );

        registrar.playToServer(
                UpdateD61APayload.TYPE,
                UpdateD61APayload.STREAM_CODEC,
                MoreRoadNetworking::handleUpdateD61A
        );

        /*
         * --------------------------------------------------------
         * B14 unifié
         * --------------------------------------------------------
         */
        registrar.playToServer(
                UpdateB14Payload.TYPE,
                UpdateB14Payload.STREAM_CODEC,
                MoreRoadNetworking::handleUpdateB14
        );

        /*
         * --------------------------------------------------------
         * Panonceaux M1 à M5
         * --------------------------------------------------------
         */
        registrar.playToServer(
                UpdatePanonceauPayload.TYPE,
                UpdatePanonceauPayload.STREAM_CODEC,
                MoreRoadNetworking::handleUpdatePanonceau
        );

        /*
         * --------------------------------------------------------
         * Constructeur de route
         * --------------------------------------------------------
         */
        registrar.playToServer(
                BuildRoadPayload.TYPE,
                BuildRoadPayload.STREAM_CODEC,
                MoreRoadNetworking::handleBuildRoad
        );
    }


    /*
     * ============================================================
     * EB10 / EB20
     * ============================================================
     */

    private static void handleUpdateEB10Text(
            UpdateEB10TextPayload payload,
            IPayloadContext context
    ) {

        var player = context.player();

        if (player == null) {
            return;
        }


        Level level = player.level();
        BlockPos pos = payload.pos();


        /*
         * Le chunk doit être chargé.
         */
        if (!level.hasChunkAt(pos)) {
            return;
        }


        /*
         * Le joueur doit être suffisamment proche.
         */
        if (
                player
                        .blockPosition()
                        .distManhattan(pos)
                        > MAX_EDIT_DISTANCE
        ) {

            return;
        }


        /*
         * Vérification de la BlockEntity.
         */
        if (
                !(level.getBlockEntity(pos)
                        instanceof EB10BlockEntity blockEntity)
        ) {

            return;
        }


        /*
         * --------------------------------------------------------
         * TEXTE
         * --------------------------------------------------------
         */

        String line1 =
                cleanText(
                        payload.line1(),
                        MAX_EB10_LINE_LENGTH
                );


        String line2 =
                cleanText(
                        payload.line2(),
                        MAX_EB10_LINE_LENGTH
                );


        blockEntity.setText(
                line1,
                line2,
                payload.line1Font(),
                payload.line2Font()
        );

        blockEntity.setCartoucheType(
                payload.cartoucheType()
        );
        blockEntity.setCartoucheText(
                cleanText(
                        payload.cartoucheText(),
                        MAX_CARTOUCHE_TEXT_LENGTH
                )
        );


        /*
         * --------------------------------------------------------
         * BLOCKSTATE EB10 / EB20
         * --------------------------------------------------------
         */

        BlockState currentState =
                level.getBlockState(pos);


        if (
                !currentState.hasProperty(
                        EB10Block.EB20
                )
        ) {

            return;
        }


        boolean currentEb20 =
                currentState.getValue(
                        EB10Block.EB20
                );


        boolean requestedEb20 =
                payload.eb20();


        /*
         * On ne modifie l'état que si nécessaire.
         */
        if (
                currentEb20
                        != requestedEb20
        ) {

            BlockState newState =
                    currentState.setValue(
                            EB10Block.EB20,
                            requestedEb20
                    );


            level.setBlock(
                    pos,
                    newState,
                    Block.UPDATE_ALL
            );
        }


        /*
         * --------------------------------------------------------
         * SYNCHRONISATION
         * --------------------------------------------------------
         */

        BlockState finalState =
                level.getBlockState(pos);


        level.sendBlockUpdated(
                pos,
                finalState,
                finalState,
                Block.UPDATE_ALL
        );
    }


    /*
     * ============================================================
     * E31a / E31b
     * ============================================================
     */

    private static void handleUpdateE31Text(
            UpdateE31TextPayload payload,
            IPayloadContext context
    ) {
        var player = context.player();

        if (player == null) {
            return;
        }

        Level level = player.level();
        BlockPos pos = payload.pos();

        if (!level.hasChunkAt(pos)) {
            return;
        }

        if (player.blockPosition().distManhattan(pos) > MAX_EDIT_DISTANCE) {
            return;
        }

        if (!(level.getBlockEntity(pos) instanceof E31BlockEntity blockEntity)) {
            return;
        }

        blockEntity.setText(
                cleanText(payload.text(), MAX_E31_TEXT_LENGTH)
        );

        BlockState state = level.getBlockState(pos);
        level.sendBlockUpdated(
                pos,
                state,
                state,
                Block.UPDATE_ALL
        );
    }


    /*
     * ============================================================
     * D21A
     * ============================================================
     */

    private static void handleUpdateDA31C(
            UpdateDA31CPayload payload,
            IPayloadContext context
    ) {

        var player = context.player();

        if (player == null) {
            return;
        }

        Level level = player.level();
        BlockPos pos = payload.pos();

        if (!level.hasChunkAt(pos)) {
            return;
        }

        if (player.blockPosition().distManhattan(pos) > MAX_EDIT_DISTANCE) {
            return;
        }

        if (!(level.getBlockEntity(pos) instanceof DA31CBlockEntity blockEntity)) {
            return;
        }

        String line1 = cleanText(payload.line1(), MAX_DA31C_LINE_LENGTH);
        String line2 = cleanText(payload.line2(), MAX_DA31C_LINE_LENGTH);
        String leftText = cleanText(payload.cartoucheLeftText(), MAX_DA31C_CARTOUCHE_LENGTH);
        String rightText = cleanText(payload.cartoucheRightText(), MAX_DA31C_CARTOUCHE_LENGTH);

        CartoucheType leftType = CartoucheType.fromSerializedName(payload.cartoucheLeftType());
        CartoucheType rightType = CartoucheType.fromSerializedName(payload.cartoucheRightType());

        blockEntity.setData(
                line1,
                line2,
                leftType,
                leftText,
                rightType,
                rightText
        );

        BlockState currentState = level.getBlockState(pos);
        if (currentState.getBlock() instanceof DA31CBlock) {
            BlockState newState = currentState
                    .setValue(DA31CBlock.CARTOUCHE_LEFT, leftType)
                    .setValue(DA31CBlock.CARTOUCHE_RIGHT, rightType);

            if (!newState.equals(currentState)) {
                level.setBlock(pos, newState, Block.UPDATE_ALL);
            }
        }

        BlockState finalState = level.getBlockState(pos);
        level.sendBlockUpdated(pos, finalState, finalState, Block.UPDATE_ALL);
    }


    private static void handleUpdateD21A(
            UpdateD21APayload payload,
            IPayloadContext context
    ) {

        var player = context.player();

        if (player == null) {
            return;
        }

        Level level = player.level();
        BlockPos pos = payload.pos();

        if (!level.hasChunkAt(pos)) {
            return;
        }

        if (
                player
                        .blockPosition()
                        .distManhattan(pos)
                        > MAX_EDIT_DISTANCE
        ) {
            return;
        }

        if (
                !(level.getBlockEntity(pos)
                        instanceof D21ABlockEntity blockEntity)
        ) {
            return;
        }

        D21APanelData[] panels =
                new D21APanelData[D21ABlockEntity.MAX_PANELS];

        for (int i = 0; i < D21ABlockEntity.MAX_PANELS; i++) {
            D21APanelData requested = payload.panel(i);

            String line1 =
                    cleanText(
                            requested.line1(),
                            MAX_D21A_LINE_LENGTH
                    );

            String line2 =
                    cleanText(
                            requested.line2(),
                            MAX_D21A_LINE_LENGTH
                    );

            String distance1 =
                    cleanText(
                            requested.distance1(),
                            MAX_D21A_DISTANCE_LENGTH
                    );

            String distance2 =
                    cleanText(
                            requested.distance2(),
                            MAX_D21A_DISTANCE_LENGTH
                    );

            panels[i] =
                    new D21APanelData(
                            requested.enabled(),
                            line1,
                            line2,
                            distance1,
                            distance2,
                            requested.type(),
                            requested.arrowRight(),
                            requested.autorouteLogo(),
                            requested.doubleLine(),
                            requested.line1Font(),
                            requested.line2Font()
                    );
        }

        blockEntity.setPanels(panels);
        blockEntity.setCartoucheType(payload.cartoucheType());
        blockEntity.setCartoucheText(
                cleanText(
                        payload.cartoucheText(),
                        MAX_CARTOUCHE_TEXT_LENGTH
                )
        );

        /*
         * Les propriétés TYPE / ARROW_RIGHT restent synchronisées avec le
         * panneau 1 uniquement pour préserver la compatibilité avec les
         * anciens mondes et les anciennes données. Le renderer multi-panneaux
         * n'utilise plus ces propriétés pour choisir les plaques.
         */
        BlockState currentState =
                level.getBlockState(pos);

        D21APanelData firstPanel = panels[0];

        if (
                currentState.hasProperty(D21ABlock.TYPE)
                        && currentState.hasProperty(D21ABlock.ARROW_RIGHT)
        ) {
            BlockState newState =
                    currentState
                            .setValue(
                                    D21ABlock.TYPE,
                                    firstPanel.type()
                            )
                            .setValue(
                                    D21ABlock.ARROW_RIGHT,
                                    firstPanel.arrowRight()
                            );

            if (!newState.equals(currentState)) {
                level.setBlock(
                        pos,
                        newState,
                        Block.UPDATE_ALL
                );
            }
        }

        BlockState finalState =
                level.getBlockState(pos);

        level.sendBlockUpdated(
                pos,
                finalState,
                finalState,
                Block.UPDATE_ALL
        );
    }



    /*
     * ============================================================
     * D42b
     * ============================================================
     */

    private static void handleUpdateD42b(
            UpdateD42bPayload payload,
            IPayloadContext context
    ) {
        var player = context.player();

        if (player == null) {
            return;
        }

        Level level = player.level();
        BlockPos pos = payload.pos();

        if (!level.hasChunkAt(pos)) {
            return;
        }

        if (player.blockPosition().distManhattan(pos) > MAX_EDIT_DISTANCE) {
            return;
        }

        if (!(level.getBlockEntity(pos) instanceof D42bBlockEntity blockEntity)) {
            return;
        }

        D42bBranchData[] branches =
                new D42bBranchData[D42bBlockEntity.MAX_BRANCHES];

        for (int i = 0; i < D42bBlockEntity.MAX_BRANCHES; i++) {
            D42bBranchData requested = payload.branch(i);

            branches[i] = new D42bBranchData(
                    requested.enabled(),
                    requested.angleDegrees(),
                    cleanText(requested.line1(), MAX_D42B_LINE_LENGTH),
                    cleanText(requested.line2(), MAX_D42B_LINE_LENGTH),
                    requested.line1Font(),
                    requested.line2Font(),
                    requested.line1Color(),
                    requested.line2Color()
            );
        }

        blockEntity.setBranches(branches);
        blockEntity.setDistanceText(
                cleanText(
                        payload.distanceText(),
                        MAX_D42B_DISTANCE_LENGTH
                )
        );

        BlockState state = level.getBlockState(pos);
        level.sendBlockUpdated(
                pos,
                state,
                state,
                Block.UPDATE_ALL
        );
    }


    /*
     * ============================================================
     * D61A
     * ============================================================
     */

    private static void handleUpdateD61A(
            UpdateD61APayload payload,
            IPayloadContext context
    ) {

        var player = context.player();

        if (player == null) {
            return;
        }

        Level level = player.level();
        BlockPos pos = payload.pos();

        if (!level.hasChunkAt(pos)) {
            return;
        }

        if (
                player
                        .blockPosition()
                        .distManhattan(pos)
                        > MAX_EDIT_DISTANCE
        ) {
            return;
        }

        if (!(level.getBlockEntity(pos) instanceof D61ABlockEntity blockEntity)) {
            return;
        }

        D61APanelData[] panels = new D61APanelData[D61ABlockEntity.MAX_PANELS];

        for (int i = 0; i < D61ABlockEntity.MAX_PANELS; i++) {
            D61APanelData requested = payload.panel(i);

            String line1 = cleanText(requested.line1(), MAX_D21A_LINE_LENGTH);
            String line2 = cleanText(requested.line2(), MAX_D21A_LINE_LENGTH);
            String distance1 = cleanText(requested.distance1(), MAX_D21A_DISTANCE_LENGTH);
            String distance2 = cleanText(requested.distance2(), MAX_D21A_DISTANCE_LENGTH);

            D21AType type = switch (requested.type()) {
                case GREEN -> D21AType.GREEN;
                case BLUE -> D21AType.BLUE;
                default -> D21AType.WHITE;
            };

            boolean autorouteLogo =
                    type != D21AType.WHITE
                            && requested.autorouteLogo();

            panels[i] = new D61APanelData(
                    requested.enabled(),
                    line1,
                    line2,
                    distance1,
                    distance2,
                    type,
                    requested.doubleLine(),
                    requested.arrowEnabled(),
                    requested.arrowPosition(),
                    requested.arrowDirection(),
                    autorouteLogo,
                    requested.line1Font(),
                    requested.line2Font()
            );
        }

        blockEntity.setPanels(panels);
        blockEntity.setCartoucheType(payload.cartoucheType());
        blockEntity.setCartoucheText(
                cleanText(
                        payload.cartoucheText(),
                        MAX_CARTOUCHE_TEXT_LENGTH
                )
        );

        BlockState currentState = level.getBlockState(pos);
        D61APanelData firstPanel = panels[0];

        if (currentState.hasProperty(net.xelpy.moreroad.block.custom.D61ABlock.TYPE)) {
            BlockState newState = currentState.setValue(
                    net.xelpy.moreroad.block.custom.D61ABlock.TYPE,
                    switch (firstPanel.type()) {
                        case GREEN -> D21AType.GREEN;
                        case BLUE -> D21AType.BLUE;
                        default -> D21AType.WHITE;
                    }
            );

            if (!newState.equals(currentState)) {
                level.setBlock(pos, newState, Block.UPDATE_ALL);
            }
        }

        BlockState finalState = level.getBlockState(pos);
        level.sendBlockUpdated(pos, finalState, finalState, Block.UPDATE_ALL);
    }

    /*
     * ============================================================
     * B14 UNIFIÉ
     * ============================================================
     */

    private static void handleUpdateB14(
            UpdateB14Payload payload,
            IPayloadContext context
    ) {
        var player = context.player();

        if (player == null) {
            return;
        }

        Level level = player.level();
        BlockPos pos = payload.pos();

        if (!level.hasChunkAt(pos)) {
            return;
        }

        if (
                player
                        .blockPosition()
                        .distManhattan(pos)
                        > MAX_EDIT_DISTANCE
        ) {
            return;
        }

        BlockState currentState = level.getBlockState(pos);

        if (!(currentState.getBlock() instanceof B14_5Block)) {
            return;
        }

        B14Speed speed = payload.speed();

        BlockState newState = currentState.setValue(
                B14_5Block.SPEED,
                speed
        );

        if (!newState.equals(currentState)) {
            level.setBlock(
                    pos,
                    newState,
                    Block.UPDATE_ALL
            );
        }
    }


    /*
     * ============================================================
     * PANONCEAUX M1 À M5
     * ============================================================
     */

    private static void handleUpdatePanonceau(
            UpdatePanonceauPayload payload,
            IPayloadContext context
    ) {
        var player = context.player();

        if (player == null) {
            return;
        }

        Level level = player.level();
        BlockPos pos = payload.pos();

        if (!level.hasChunkAt(pos)) {
            return;
        }

        if (player.blockPosition().distManhattan(pos) > MAX_EDIT_DISTANCE) {
            return;
        }

        if (!(level.getBlockEntity(pos) instanceof PanonceauBlockEntity blockEntity)) {
            return;
        }

        PanonceauEntry[] entries = new PanonceauEntry[PanonceauBlockEntity.MAX_PANONCEAUX];

        for (int i = 0; i < entries.length; i++) {
            PanonceauEntry requested = payload.entry(i);
            PanonceauVariant variant = requested.variant() == null
                    ? PanonceauVariant.M1
                    : requested.variant();

            entries[i] = new PanonceauEntry(
                    requested.enabled(),
                    variant,
                    cleanPanonceauText(requested.value(), MAX_PANONCEAU_VALUE_LENGTH)
            );
        }

        blockEntity.setEntries(entries);
        BlockState state = level.getBlockState(pos);
        level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
    }


    /*
     * ============================================================
     * CONSTRUCTEUR DE ROUTE
     * ============================================================
     */

    private static void handleBuildRoad(
            BuildRoadPayload payload,
            IPayloadContext context
    ) {
        var player = context.player();

        if (player == null) {
            return;
        }

        boolean hasBuilderInMainHand =
                player.getMainHandItem().getItem()
                        == MoreRoadItems.ROAD_BUILDER.get();

        boolean hasBuilderInOffHand =
                player.getOffhandItem().getItem()
                        == MoreRoadItems.ROAD_BUILDER.get();

        if (!hasBuilderInMainHand && !hasBuilderInOffHand) {
            return;
        }

        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return;
        }

        RoadBuilderItem.buildFromEditor(
                serverPlayer,
                payload.start(),
                payload.control(),
                payload.end()
        );
    }


    /*
     * ============================================================
     * CONVERSION DU TYPE D21A
     * ============================================================
     */

    private static D21AType parseD21AType(
            String value
    ) {

        if (value == null) {
            return D21AType.WHITE;
        }


        return switch (value) {

            case "green" ->
                    D21AType.GREEN;

            case "blue" ->
                    D21AType.BLUE;

            default ->
                    D21AType.WHITE;
        };
    }


    /*
     * ============================================================
     * NETTOYAGE DU TEXTE
     * ============================================================
     */

    /**
     * Nettoyage réservé aux panonceaux : contrairement aux autres champs du
     * mod, un saut de ligne est une donnée utile (TXT, M1a et M8f bis).
     * On conserve au maximum deux lignes, on retire les retours chariot et on
     * borne ensuite la longueur totale sans transformer le saut de ligne en espace.
     */
    private static String cleanPanonceauText(String text, int maxLength) {
        if (text == null) {
            return "";
        }

        String normalized = text.replace("\r", "");
        String[] raw = normalized.split("\n", 3);
        String first = raw.length > 0 ? raw[0].strip() : "";
        String second = raw.length > 1 ? raw[1].strip() : "";
        String cleaned = second.isBlank() ? first : first + "\n" + second;

        if (cleaned.length() > maxLength) {
            cleaned = cleaned.substring(0, maxLength);
        }
        return cleaned;
    }

    private static String cleanText(
            String text,
            int maxLength
    ) {

        if (text == null) {
            return "";
        }


        String cleaned =
                text
                        .replace(
                                '\n',
                                ' '
                        )
                        .replace(
                                '\r',
                                ' '
                        )
                        .strip();


        if (
                cleaned.length()
                        > maxLength
        ) {

            cleaned =
                    cleaned.substring(
                            0,
                            maxLength
                    );
        }


        return cleaned;
    }
}