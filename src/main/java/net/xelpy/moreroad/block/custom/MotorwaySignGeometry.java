package net.xelpy.moreroad.block.custom;

/**
 * Dimensions physiques partagées par le renderer et les hitboxs.
 * Une unité correspond à un mètre/bloc Minecraft.
 */
public record MotorwaySignGeometry(
        float width,
        float height,
        boolean mountedOnCrossbar,
        float panelBottom,
        float supportTop
) {

    /** Même ordre de grandeur que le DA31C : environ trois blocs pour les grands panneaux. */
    public static final float WORLD_SCALE = 0.48F;
    public static final float POLE_PANEL_BOTTOM = 2.05F * WORLD_SCALE;
    public static final float D61B_PANEL_BOTTOM = 1.35F * WORLD_SCALE;
    public static final float D61B_SINGLE_PANEL_BOTTOM = 1.05F * WORLD_SCALE;
    /** Décalage avant : demi-poteau + demi-plaque, avec un léger recouvrement de jonction. */
    public static final float D61B_PANEL_FORWARD = 0.365F;
    public static final float D61B_SUPPORT_OVERLAP = 0.44F * WORLD_SCALE;
    public static final float MOUNTED_PANEL_TOP = 26.0F / 16.0F;

    public static MotorwaySignGeometry forPreset(MotorwaySignPreset preset) {
        return forPreset(preset, null);
    }

    public static MotorwaySignGeometry forPreset(MotorwaySignPreset preset, MotorwaySignLineData[] values) {
        return forPreset(
                preset,
                values,
                preset.getSupport() == MotorwaySignSupport.OVERHEAD
        );
    }

    public static MotorwaySignGeometry forPreset(
            MotorwaySignPreset preset,
            MotorwaySignLineData[] values,
            boolean mountedOnCrossbar
    ) {
        float width;
        float height;
        switch (preset) {
            case D31B_EX1 -> { width = 4.20F; height = scaled(width, 2349.0F, 7832.0F); }
            case D31B_EX2 -> { width = 6.00F; height = scaled(width, 4535.0F, 15397.0F); }
            /*
             * Signalé : agrandir cette hauteur avec les registres extensibles
             * (comme pour la boîte de rendu/collision, tenté puis retiré)
             * casse le calcul de la cartouche "premier registre" plus loin
             * (MotorwaySignBlockEntityRenderer.submit), qui suppose que ce
             * haut de panneau reste fixe et que seul le bas s'étire vers le
             * poteau — l'agrandir ici faisait flotter la cartouche bien trop
             * haut. On garde donc la hauteur naturelle (1 ville / 2 villes),
             * cohérente avec le sommet réellement fixe du dessin.
             */
            case D31D -> { width = 5.50F; height = scaled(width, 3930.0F, 12505.0F); }
            case D31E -> { width = 5.80F; height = scaled(width, 4005.0F, 13403.0F); }
            case D32A -> { width = 5.20F; height = scaled(width, 3922.0F, 11621.0F); }
            case D41A -> { width = 5.50F; height = scaled(width, 3894.0F, 12467.0F); }
            case D41B -> { width = 5.20F; height = scaled(width, 2399.0F, 11536.0F); }
            case D41C -> { width = 5.50F; height = scaled(width, 2897.0F, 13098.0F); }
            case D44 -> { width = 4.60F; height = scaled(width, 9304.0F, 7267.0F); }
            case D46A -> { width = 5.20F; height = scaled(width, 760.0F, 1000.0F); }
            case D47A -> { width = 5.20F; height = scaled(width, 8032.0F, 17924.0F); }
            case D61B -> { width = 6.20F; height = scaled(width, 7879.0F, 15788.0F); }
            case D62A -> { width = 5.60F; height = scaled(width, 10619.0F, 11537.0F); }
            case D62B -> { width = 5.80F; height = scaled(width, 8129.0F, 13203.0F); }
            case D62C -> { width = 5.60F; height = 4.60F; }
            case D62D_TOP -> { width = 5.80F; height = scaled(width, 12987.0F, 16204.0F); }
            case D62D_BOTTOM -> { width = 5.80F; height = scaled(width, 8215.0F, 16204.0F); }
            case D63C -> { width = 6.00F; height = scaled(width, 9935.0F, 13403.0F); }
            case D63D -> { width = 5.80F; height = scaled(width, 11469.0F, 12370.0F); }
            case D64 -> { width = 5.342F; height = 2.798F; }
            case D71 -> { width = 5.60F; height = scaled(width, 6434.0F, 13213.0F); }
            case D72 -> { width = 4.40F; height = scaled(width, 14926.0F, 12012.0F); }
            case D73 -> { width = 3.80F; height = scaled(width, 6596.0F, 6037.0F); }
            case D74A -> { width = 5.339F; height = 2.793F; }
            case D74B -> { width = 3.098F; height = 2.793F; }
            case DA31A -> { width = 5.90F; height = scaled(width, 8662.0F, 14843.0F); }
            case DA31B -> { width = 5.80F; height = scaled(width, 13776.0F, 14494.0F); }
            case DA31D -> { width = 5.80F; height = scaled(width, 10470.0F, 14336.0F); }
            case DA31E -> { width = 5.60F; height = scaled(width, 10792.0F, 13331.0F); }
            case DA31F -> { width = 4.80F; height = scaled(width, 14281.0F, 9971.0F); }
            case DA32A, DA32A_DC -> { width = 4.60F; height = scaled(width, 7208.0F, 9909.0F); }
            case DA32B, DA32B_DC -> { width = 4.60F; height = scaled(width, 8942.0F, 9909.0F); }
            default -> {
                return generic(preset, values, mountedOnCrossbar);
            }
        }
        return scaledGeometry(width, height, mountedOnCrossbar);
    }

    public static MotorwaySignGeometry forCustomPanels(
            MotorwaySignPanelData[] panels,
            boolean mountedOnCrossbar,
            MotorwaySignStyleProfile style
    ) {
        float width = 0.0F;
        float height = 0.0F;
        int enabledCount = 0;
        if (panels != null) {
            for (int panelIndex = 0; panelIndex < panels.length; panelIndex++) {
                MotorwaySignPanelData panel = panels[panelIndex];
                if (panel == null || !panel.enabled()) {
                    continue;
                }
                if (panel.cartoucheType().isVisible() && !panel.hasPanelContent()) {
                    continue;
                }
                enabledCount++;
                int longest = 0;
                int longestDistance = 0;
                for (int lineIndex = 0; lineIndex < panel.lineCount(); lineIndex++) {
                    String line = panel.line(lineIndex);
                    String distance = style.allowsCustomDistances() ? panel.distance(lineIndex) : "";
                    longest = Math.max(longest, line.codePointCount(0, line.length()));
                    longestDistance = Math.max(
                            longestDistance,
                            distance.codePointCount(0, distance.length())
                    );
                }
                /* Marge volontaire pour que la zone de sélection couvre aussi les textes larges. */
                MotorwaySignColor visualBackground = style.sanitizeCustomBackground(panel.background());
                float averageWidth = visualBackground.isLight() ? 0.18F : 0.20F;
                float textWidth = (longest + longestDistance) * averageWidth;
                width = Math.max(width, textWidth + 0.80F + graphicReserve(panel.graphic()));
                if (style.allowsCustomCartouche()
                        && panelIndex == 0 && panel.cartoucheType().isVisible()) {
                    int cartoucheLength = panel.cartoucheText().codePointCount(0, panel.cartoucheText().length());
                    width = Math.max(width, Math.min(2.40F, 0.72F + cartoucheLength * 0.15F));
                }
                height += style.addedPanelHeight(panel.lineCount(), panel.graphic());
                height += 0.075F;
            }
        }
        if (enabledCount > 0) {
            height -= 0.075F;
        }
        return scaledGeometry(
                enabledCount == 0 ? 0.0F : clamp(width, 2.30F, 6.80F),
                height,
                mountedOnCrossbar
        );
    }

    /** Le SVG d'origine est toujours conservé ; les plaques libres sont ajoutées en dessous. */
    public static MotorwaySignGeometry forComposite(
            MotorwaySignPreset preset,
            MotorwaySignLineData[] values,
            MotorwaySignPanelData[] panels,
            boolean mountedOnCrossbar
    ) {
        MotorwaySignStyleProfile style = MotorwaySignStyleProfile.forPreset(preset);
        if (preset == MotorwaySignPreset.D61B || preset == MotorwaySignPreset.FREEFORM) {
            float height = 0.0F;
            float dynamicWidth = preset == MotorwaySignPreset.D61B ? 6.20F : 2.30F;
            int enabledCount = 0;
            if (panels != null) {
                MotorwaySignGeometry customBounds = forCustomPanels(panels, mountedOnCrossbar, style);
                if (preset == MotorwaySignPreset.FREEFORM && customBounds.width() > 0.0F) {
                    dynamicWidth = Math.max(2.30F, customBounds.width() / WORLD_SCALE);
                }
                for (MotorwaySignPanelData panel : panels) {
                    if (panel == null || !panel.enabled()) {
                        continue;
                    }
                    if (enabledCount > 0) {
                        height += 0.075F;
                    }
                    height += style.addedPanelHeight(panel.lineCount(), panel.graphic());
                    enabledCount++;
                }
                if (style.allowsCustomCartouche()
                        && panels.length > 0 && panels[0] != null
                        && panels[0].cartoucheType().isVisible()) {
                    height += (float) (CartoucheLayout.CARTOUCHE_RENDER_HEIGHT / WORLD_SCALE)
                            + 0.075F;
                }
            }
            if (enabledCount == 0) {
                height = style.addedPanelHeight(1, MotorwaySignGraphic.NONE);
                enabledCount = 1;
            }
            float worldHeight = Math.max(0.88F, height) * WORLD_SCALE;
            boolean actualMounted = preset == MotorwaySignPreset.FREEFORM && mountedOnCrossbar;
            float panelBottom = actualMounted
                    ? MOUNTED_PANEL_TOP - worldHeight
                    : enabledCount <= 1 ? D61B_SINGLE_PANEL_BOTTOM : D61B_PANEL_BOTTOM;
            return new MotorwaySignGeometry(
                    dynamicWidth * WORLD_SCALE,
                    worldHeight,
                    actualMounted,
                    panelBottom,
                    actualMounted ? MOUNTED_PANEL_TOP : d61SupportTop(panels, style)
            );
        }
        MotorwaySignGeometry original = forPreset(preset, values, mountedOnCrossbar);
        /*
         * Les panneaux au dessin figé (D44, D32a...) ne doivent jamais
         * agrandir leur géométrie à cause d'anciens registres restés en NBT.
         */
        MotorwaySignPanelData[] effectivePanels = style.allowsExtraPanels() ? panels : null;
        MotorwaySignGeometry additions = forCustomPanels(effectivePanels, mountedOnCrossbar, style);
        if (additions.height() <= 0.0F) {
            return original;
        }
        return new MotorwaySignGeometry(
                original.width(),
                original.height() + additions.height() + 0.075F * WORLD_SCALE,
                mountedOnCrossbar,
                original.panelBottom(),
                mountedOnCrossbar
                        ? original.supportTop()
                        : original.panelBottom() + additions.height()
                        + 0.075F * WORLD_SCALE + original.height() / 2.0F
        );
    }

    /**
     * Le poteau D61b traverse toute la pile puis s'arrête au milieu de la
     * pancarte supérieure. Sa hauteur suit donc réellement chaque ajout de
     * panneau, au lieu de rester figée sur la hauteur du premier modèle.
     */
    public static float d61SupportTop(
            MotorwaySignPanelData[] panels,
            MotorwaySignStyleProfile style
    ) {
        float stackedHeight = 0.0F;
        float firstPanelHeight = 0.0F;
        int enabledCount = 0;
        if (panels != null) {
            for (MotorwaySignPanelData panel : panels) {
                if (panel == null || !panel.enabled()) {
                    continue;
                }
                if (enabledCount > 0) {
                    stackedHeight += 0.075F;
                }
                float panelHeight = style.addedPanelHeight(panel.lineCount(), panel.graphic());
                if (enabledCount == 0) {
                    firstPanelHeight = panelHeight;
                }
                stackedHeight += panelHeight;
                enabledCount++;
            }
        }
        if (enabledCount == 0) {
            stackedHeight = style.addedPanelHeight(1, MotorwaySignGraphic.NONE);
            firstPanelHeight = stackedHeight;
        }
        float panelBottom = enabledCount <= 1
                ? D61B_SINGLE_PANEL_BOTTOM
                : D61B_PANEL_BOTTOM;
        return panelBottom
                + (stackedHeight - firstPanelHeight / 2.0F) * WORLD_SCALE;
    }

    /**
     * Largeur physique (en mètres) dérivée du SVG pour les modèles du
     * Groupe B (pas encore de texture exacte type ExactMappedArtwork).
     *
     * SOURCE UNIQUE : cette méthode est appelée à la fois ici (hitbox/
     * positionnement) et depuis buildLayout() dans le renderer (dessin
     * réel du panneau). Les deux doivent TOUJOURS lire la même valeur,
     * sous peine de désynchronisation entre la boîte englobante et le
     * panneau réellement dessiné (texte invisible, mauvais recadrage).
     * Ne modifier qu'ici, jamais dupliquer ces chiffres ailleurs.
     *
     * La hauteur n'est volontairement PAS fixée : elle continue de
     * dépendre du nombre de lignes/texte (comme avant), ce qui reste
     * cohérent entre les deux appelants sans risque de divergence.
     *
     * Retourne 0 si le modèle n'a pas de largeur fixe connue (dimensionnement
     * générique classique, inchangé).
     */
    public static float fixedWidthMeters(MotorwaySignPreset preset) {
        return switch (preset) {
            /* D51/D52/DA41/DA51/DA52 : SVG autonomes, largeur mesurée sur leur viewBox. */
            case D51C -> 4.14F;
            case D51CR -> 6.30F;
            case D51CR_DC -> 6.60F;
            case D51D -> 5.02F;
            case D51DR -> 6.47F;
            case D52A, D52B -> 4.66F;
            case D52C -> 4.16F;
            case DA41A -> 6.59F;
            case DA41B -> 6.30F;
            case DA41C -> 3.40F;
            case DA41D_TOP, DA41D_BOTTOM -> 6.23F;
            case DA41E_TOP, DA41E_BOTTOM -> 5.03F;
            case DA41F -> 6.26F;
            case DA51B -> 4.46F;
            case DA51BR -> 6.13F;
            case DA52A -> 5.54F;
            case DA52B -> 4.98F;
            /*
             * D45/D46/D47 : SVG "ensemble" (panneau + pictogrammes CE).
             * Largeur reprise par analogie (même graphique SERVICES, nombre
             * de lignes comparable) en attendant une mesure dédiée.
             * D44 a désormais sa propre géométrie mesurée (voir le switch
             * explicite ci-dessus) : il ne passe plus par ce chemin générique.
             */
            case D45, D45_DC -> 4.60F;
            case D46A -> 5.20F;
            case D47A -> 5.20F;
            case D47B, D47C -> 4.00F;
            default -> 0.0F;
        };
    }

    /** Reproduit les dimensions du renderer paramétrique sans dépendre du moteur de polices client. */
    private static MotorwaySignGeometry generic(
            MotorwaySignPreset preset,
            MotorwaySignLineData[] values,
            boolean mountedOnCrossbar
    ) {
        int[] groupCounts = new int[MotorwaySignBlockEntitySlots.MAX_GROUPS];
        MotorwaySignColor[] groupColors = new MotorwaySignColor[MotorwaySignBlockEntitySlots.MAX_GROUPS];
        float sharedWidth = 2.30F;
        float routeWidth = 0.0F;
        int routeCount = 0;
        float distanceWidth = 0.0F;

        for (int index = 0; index < preset.getSlotCount(); index++) {
            MotorwaySignSlot slot = preset.getSlot(index);
            MotorwaySignLineData line = values != null && index < values.length && values[index] != null
                    ? values[index]
                    : MotorwaySignLineData.blankForSlot(slot);
            float textWidth = estimatedTextWidth(line);
            if (slot.role() == MotorwaySignRole.ROUTE) {
                routeWidth += clamp(textWidth + 0.38F, 1.02F, 2.20F);
                routeCount++;
            } else if (slot.role() == MotorwaySignRole.DISTANCE) {
                distanceWidth = clamp(textWidth + 0.42F, 1.35F, 2.65F);
            } else {
                int group = Math.min(Math.max(0, slot.panelGroup()), groupCounts.length - 1);
                if (groupCounts[group] == 0) {
                    groupColors[group] = line.color();
                }
                groupCounts[group]++;
                sharedWidth = Math.max(sharedWidth, textWidth + 0.72F + graphicReserve(preset.getGraphic()));
            }
        }

        float fixedWidth = fixedWidthMeters(preset);
        if (fixedWidth > 0.0F) {
            sharedWidth = fixedWidth;
        }
        sharedWidth = clamp(sharedWidth, 2.30F, 6.80F);
        if (routeCount > 1) {
            routeWidth += 0.075F * (routeCount - 1);
        }
        float width = Math.max(sharedWidth, Math.max(routeWidth, distanceWidth));

        float height = distanceWidth > 0.0F ? 0.56F + 0.075F : 0.0F;
        boolean hasPanel = false;
        for (int group = 0; group < groupCounts.length; group++) {
            if (groupCounts[group] == 0) {
                continue;
            }
            float lineStep = groupColors[group] != null && groupColors[group].isLight() ? 0.39F : 0.45F;
            height += 0.46F + lineStep * groupCounts[group];
            if (!hasPanel && usesBottomArrow(preset.getGraphic())) {
                height += 0.50F;
            }
            height += 0.075F;
            hasPanel = true;
        }
        if (!hasPanel) {
            height += 0.975F;
        }
        if (routeCount > 0) {
            height += 0.55F;
        }
        return scaledGeometry(
                width,
                Math.max(0.90F, height),
                mountedOnCrossbar
        );
    }

    private static MotorwaySignGeometry scaledGeometry(float width, float height, boolean mountedOnCrossbar) {
        return new MotorwaySignGeometry(
                width * WORLD_SCALE,
                height * WORLD_SCALE,
                mountedOnCrossbar,
                POLE_PANEL_BOTTOM,
                POLE_PANEL_BOTTOM + height * WORLD_SCALE / 2.0F
        );
    }

    private static float estimatedTextWidth(MotorwaySignLineData line) {
        String text = line.text() == null ? "" : line.text();
        int characters = text.codePointCount(0, text.length());
        float averageCharacterWidth = line.color().isLight() ? 0.145F : 0.165F;
        if (line.font() == RoadTextFont.L4) {
            averageCharacterWidth *= 0.96F;
        }
        return characters * averageCharacterWidth;
    }

    private static float graphicReserve(MotorwaySignGraphic graphic) {
        return switch (graphic) {
            case DIAGONAL_LEFT, DIAGONAL_RIGHT, EXIT -> 0.82F;
            case SCHEMATIC_LEFT, SCHEMATIC_RIGHT -> 1.02F;
            case SERVICES, MOTORWAY -> 0.55F;
            case EXIT_LIST -> 0.36F;
            default -> 0.0F;
        };
    }

    private static boolean usesBottomArrow(MotorwaySignGraphic graphic) {
        return graphic == MotorwaySignGraphic.DOWN || graphic == MotorwaySignGraphic.DOWN_DOUBLE;
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static float scaled(float physicalWidth, float sourceHeight, float sourceWidth) {
        return physicalWidth * sourceHeight / sourceWidth;
    }

    /** Evite une dépendance du package bloc vers la BlockEntity uniquement pour une petite constante. */
    private static final class MotorwaySignBlockEntitySlots {
        private static final int MAX_GROUPS = 8;

        private MotorwaySignBlockEntitySlots() {
        }
    }
}
