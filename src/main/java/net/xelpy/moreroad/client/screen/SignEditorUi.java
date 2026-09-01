package net.xelpy.moreroad.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.xelpy.moreroad.MoreRoad;
import net.xelpy.moreroad.block.custom.CartoucheType;
import net.xelpy.moreroad.block.custom.D21APanelData;
import net.xelpy.moreroad.block.custom.D21AType;
import net.xelpy.moreroad.block.custom.D42bLabelColor;
import net.xelpy.moreroad.block.custom.D61AArrowDirection;
import net.xelpy.moreroad.block.custom.D61AArrowPosition;
import net.xelpy.moreroad.block.custom.D61APanelData;
import net.xelpy.moreroad.block.custom.D42bBranchData;
import net.xelpy.moreroad.block.custom.RoadTextFont;

/**
 * Outils visuels communs aux éditeurs de panneaux.
 *
 * V103 :
 * - mise en page responsive pour les GUI Scale élevées ;
 * - fond moderne commun ;
 * - aperçu 2D en direct, recalculé à chaque frame depuis les champs ouverts.
 *
 * L'aperçu est volontairement schématique : il sert à vérifier rapidement
 * texte, couleur, direction et occupation de l'espace sans fermer l'éditeur.
 */
public final class SignEditorUi {

    public static final int COLOR_OVERLAY = 0xA812151A;
    public static final int COLOR_CARD = 0xE01B2027;
    public static final int COLOR_CARD_SOFT = 0xD4262C34;
    public static final int COLOR_BORDER = 0xFF6E7885;
    public static final int COLOR_ACCENT = 0xFF9AA9BC;
    public static final int COLOR_TEXT = 0xFFF3F5F7;
    public static final int COLOR_SUBTEXT = 0xFFB9C1CC;
    public static final int COLOR_BLACK = 0xFF111111;
    public static final int COLOR_WHITE = 0xFFF2F2F2;
    public static final int COLOR_GREEN = 0xFF16823A;
    public static final int COLOR_BLUE = 0xFF2A7FFF;
    public static final int COLOR_RED = 0xFFC72828;
    public static final int COLOR_YELLOW = 0xFFE5C42D;

    private static final FontDescription.Resource ROAD_FONT_L1 =
            new FontDescription.Resource(
                    Identifier.fromNamespaceAndPath(MoreRoad.MODID, "caracteres_l1")
            );

    private static final FontDescription.Resource ROAD_FONT_L4 =
            new FontDescription.Resource(
                    Identifier.fromNamespaceAndPath(MoreRoad.MODID, "caracteres_l4")
            );

    private static final FontDescription.Resource ROAD_FONT_L2 =
            new FontDescription.Resource(
                    Identifier.fromNamespaceAndPath(MoreRoad.MODID, "caracteres_l2")
            );

    private SignEditorUi() {
    }

    public record PreviewBox(int x, int y, int width, int height, boolean side) {
    }

    public static float responsiveScale(
            int screenHeight,
            int designSpan,
            boolean compactPreview
    ) {
        int reserved = compactPreview ? 42 : 24;
        float scale = (screenHeight - reserved) / (float) designSpan;
        return clamp(scale, 0.46F, 1.0F);
    }

    public static int y(int centerY, int designOffset, float scale) {
        return centerY + Math.round(designOffset * scale);
    }

    public static int size(int base, float scale, int minimum) {
        return Math.max(minimum, Math.round(base * scale));
    }

    public static int gap(float scale) {
        return Math.max(3, Math.round(6.0F * scale));
    }

    public static PreviewBox previewBox(
            int screenWidth,
            int formLeft,
            int formWidth,
            int screenHeight
    ) {
        int sideWidth = 196;
        int sideGap = 12;

        if (formLeft >= sideWidth + sideGap + 8) {
            return new PreviewBox(
                    formLeft - sideWidth - sideGap,
                    Math.max(24, screenHeight / 2 - 78),
                    sideWidth,
                    156,
                    true
            );
        }

        int width = Math.min(150, Math.max(96, screenWidth / 3));
        return new PreviewBox(
                screenWidth - width - 8,
                4,
                width,
                32,
                false
        );
    }

    public static int formCenterY(int screenHeight, PreviewBox preview) {
        if (preview == null || preview.side()) {
            return screenHeight / 2;
        }

        // Le mini-aperçu compact occupe la bande supérieure. On descend
        // légèrement le formulaire pour que ses onglets restent toujours
        // visibles, même avec une GUI Scale très élevée.
        return screenHeight / 2 + 24;
    }


    public static int formCenterY(
            int screenHeight,
            PreviewBox preview,
            int minDesignOffset,
            int maxDesignOffset,
            float scale,
            int controlHeight
    ) {
        int topLimit = preview != null && !preview.side() ? 40 : 24;
        int bottomLimit = screenHeight - 8;
        int topRelative = Math.round(minDesignOffset * scale);
        int bottomRelative = Math.round(maxDesignOffset * scale) + controlHeight;

        int centerFromTop = topLimit - topRelative;
        int centerFromBottom = bottomLimit - bottomRelative;
        return (centerFromTop + centerFromBottom) / 2;
    }

    public static void drawBackground(
            GuiGraphicsExtractor graphics,
            Font font,
            int screenWidth,
            int screenHeight,
            Component title
    ) {
        graphics.fill(0, 0, screenWidth, screenHeight, COLOR_OVERLAY);
        graphics.centeredText(
                font,
                title,
                screenWidth / 2,
                7,
                COLOR_TEXT
        );
        if (screenHeight >= 180) {
            graphics.centeredText(
                    font,
                    Component.literal("Aperçu en direct • interface adaptative"),
                    screenWidth / 2,
                    18,
                    COLOR_SUBTEXT
            );
        }
    }


    public static void drawFormCard(
            GuiGraphicsExtractor graphics,
            int screenWidth,
            int screenHeight,
            int formWidth,
            PreviewBox preview
    ) {
        int x = (screenWidth - formWidth) / 2 - 7;
        int y = preview != null && !preview.side() ? 40 : 24;
        int width = formWidth + 14;
        int height = Math.max(20, screenHeight - y - 8);
        graphics.fill(x, y, x + width, y + height, COLOR_CARD_SOFT);
        graphics.outline(x, y, width, height, COLOR_BORDER);
    }

    public static void drawPreviewCard(
            GuiGraphicsExtractor graphics,
            Font font,
            PreviewBox box,
            String subtitle
    ) {
        if (box == null) {
            return;
        }

        graphics.fill(
                box.x(),
                box.y(),
                box.x() + box.width(),
                box.y() + box.height(),
                COLOR_CARD
        );
        graphics.outline(
                box.x(),
                box.y(),
                box.width(),
                box.height(),
                COLOR_BORDER
        );

        if (box.side()) {
            graphics.centeredText(
                    font,
                    Component.literal("APERÇU EN DIRECT"),
                    box.x() + box.width() / 2,
                    box.y() + 8,
                    COLOR_TEXT
            );
            if (subtitle != null && !subtitle.isBlank()) {
                graphics.centeredText(
                        font,
                        Component.literal(subtitle),
                        box.x() + box.width() / 2,
                        box.y() + 19,
                        COLOR_SUBTEXT
                );
            }
        }
    }


    /**
     * V106 : aperçu fidèle de l'ensemble D21.
     *
     * Contrairement à l'ancien aperçu qui ne dessinait que le panneau
     * actuellement sélectionné sous forme de simple rectangle, cette version
     * affiche tous les étages actifs (P1 à P4), leur vraie direction, leur
     * format simple/double, leur couleur et leur logo autoroute éventuel.
     */
    public static void drawD21StackPreview(
            GuiGraphicsExtractor graphics,
            Font font,
            PreviewBox box,
            D21APanelData[] panels,
            int selectedPanelIndex,
            CartoucheType cartoucheType,
            String cartoucheText
    ) {
        if (box == null) {
            return;
        }

        drawPreviewCard(graphics, font, box, "D21 • ensemble configuré");

        int topPad = box.side() ? 34 : 5;
        int areaX = box.x() + 12;
        int areaY = box.y() + topPad;
        int areaWidth = Math.max(40, box.width() - 24);
        int areaHeight = Math.max(30, box.height() - topPad - 12);

        int activeCount = 0;
        int rawHeight = 0;
        if (panels != null) {
            for (D21APanelData panel : panels) {
                if (panel == null || !panel.enabled()) {
                    continue;
                }
                activeCount++;
                rawHeight += panel.doubleLine() ? 54 : 40;
            }
        }

        if (activeCount == 0) {
            graphics.centeredText(
                    font,
                    Component.literal("Aucun panneau actif"),
                    areaX + areaWidth / 2,
                    areaY + areaHeight / 2 - 4,
                    MODERN_MUTED
            );
            return;
        }

        rawHeight += Math.max(0, activeCount - 1) * 5;

        int cartoucheHeight = 0;
        if (cartoucheType != null && cartoucheType.isVisible()) {
            cartoucheHeight = Math.min(24, Math.max(14, areaHeight / 8));
            rawHeight += cartoucheHeight + 7;
        }

        float stackScale = Math.min(1.0F, areaHeight / (float) Math.max(1, rawHeight));
        int gap = Math.max(2, Math.round(5 * stackScale));
        int panelWidth = Math.max(70, areaWidth - 20);

        int actualHeight = 0;
        if (cartoucheHeight > 0) {
            actualHeight += Math.max(10, Math.round(cartoucheHeight * stackScale)) + gap;
        }
        if (panels != null) {
            for (D21APanelData panel : panels) {
                if (panel == null || !panel.enabled()) {
                    continue;
                }
                actualHeight += Math.max(22, Math.round((panel.doubleLine() ? 54 : 40) * stackScale));
            }
        }
        actualHeight += Math.max(0, activeCount - 1) * gap;

        int y = areaY + Math.max(0, (areaHeight - actualHeight) / 2);

        if (cartoucheHeight > 0) {
            int renderedCartoucheHeight = Math.max(10, Math.round(cartoucheHeight * stackScale));
            drawCartouchePreview(
                    graphics,
                    font,
                    areaX,
                    y,
                    areaWidth,
                    renderedCartoucheHeight,
                    cartoucheType,
                    cartoucheText
            );
            y += renderedCartoucheHeight + gap;
        }

        if (panels == null) {
            return;
        }

        for (int i = 0; i < panels.length; i++) {
            D21APanelData panel = panels[i];
            if (panel == null || !panel.enabled()) {
                continue;
            }

            int panelHeight = Math.max(22, Math.round((panel.doubleLine() ? 54 : 40) * stackScale));
            int x = areaX + (areaWidth - panelWidth) / 2;

            drawD21PanelPreview(
                    graphics,
                    font,
                    x,
                    y,
                    panelWidth,
                    panelHeight,
                    panel,
                    i == selectedPanelIndex
            );

            y += panelHeight + gap;
        }
    }

    private static void drawCartouchePreview(
            GuiGraphicsExtractor graphics,
            Font font,
            int areaX,
            int y,
            int areaWidth,
            int height,
            CartoucheType type,
            String text
    ) {
        String displayText = clean(text).isBlank() ? cartoucheLabel(type) : clean(text);
        int width = Math.min(areaWidth - 20, Math.max(48, textPixelWidth(displayText) + 20));
        int x = areaX + (areaWidth - width) / 2;
        int bg = cartoucheColor(type);
        int fg = cartoucheTextColor(type);

        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, COLOR_BLACK);
        graphics.fill(x, y, x + width, y + height, bg);
        drawCenteredNoShadow(
                graphics,
                font,
                Component.literal(shorten(displayText, 22)),
                x + width / 2,
                y + Math.max(2, (height - 9) / 2),
                fg
        );
    }

    private static void drawD21PanelPreview(
            GuiGraphicsExtractor graphics,
            Font font,
            int x,
            int y,
            int width,
            int height,
            D21APanelData panel,
            boolean selected
    ) {
        D21AType type = panel.type() == null ? D21AType.WHITE : panel.type();
        boolean pointsRight = panel.arrowRight();
        boolean colored = type != D21AType.WHITE;

        int outer = COLOR_BLACK;
        int border = colored ? COLOR_WHITE : COLOR_BLACK;
        int fill = panelColor(type);
        int textColor = panelTextColor(type);

        // Ombre / contour extérieur.
        drawPointedPanelShape(graphics, x, y, width, height, pointsRight, outer);

        // Liseré : blanc sur panneaux verts/bleus, noir sur panneau blanc.
        int borderInset = 2;
        drawPointedPanelShape(
                graphics,
                x + borderInset,
                y + borderInset,
                Math.max(10, width - borderInset * 2),
                Math.max(10, height - borderInset * 2),
                pointsRight,
                border
        );

        int fillInset = colored ? 4 : 3;
        drawPointedPanelShape(
                graphics,
                x + fillInset,
                y + fillInset,
                Math.max(10, width - fillInset * 2),
                Math.max(10, height - fillInset * 2),
                pointsRight,
                fill
        );

        int pointWidth = Math.max(16, Math.min(width / 4, Math.round(height * 0.70F)));
        int bodyLeft = pointsRight ? x + 7 : x + pointWidth + 3;
        int bodyRight = pointsRight ? x + width - pointWidth - 3 : x + width - 7;
        int bodyWidth = Math.max(30, bodyRight - bodyLeft);

        boolean showLogo = panel.autorouteLogo() && type != D21AType.WHITE;
        int logoSize = showLogo ? Math.max(18, Math.min(height - 10, 30)) : 0;
        int logoGap = showLogo ? 5 : 0;

        int distanceWidth = Math.max(30, Math.min(46, bodyWidth / 4));
        int distanceCenter;
        int textLeft;
        int textRight;

        if (pointsRight) {
            distanceCenter = bodyRight - distanceWidth / 2;
            textLeft = bodyLeft + (showLogo ? logoSize + logoGap : 3);
            textRight = bodyRight - distanceWidth - 3;
        } else {
            distanceCenter = bodyLeft + distanceWidth / 2;
            textLeft = bodyLeft + distanceWidth + 3;
            textRight = bodyRight - (showLogo ? logoSize + logoGap : 3);
        }

        if (showLogo) {
            int logoX = pointsRight ? bodyLeft : bodyRight - logoSize;
            int logoY = y + (height - logoSize) / 2;
            drawMotorwayLogo(graphics, logoX, logoY, logoSize, type);
        }

        textLeft = Math.min(textLeft, textRight - 12);
        int textCenter = (textLeft + textRight) / 2;

        String line1 = clean(panel.line1());
        String line2 = clean(panel.line2());
        String distance1 = clean(panel.distance1());
        String distance2 = clean(panel.distance2());
        if (!panel.doubleLine() && distance1.isBlank()) {
            distance1 = distance2;
        }

        /*
         * V109 : sur un panneau blanc, l'ombre native de la police Minecraft
         * créait un second contour gris/noir très visible dans l'aperçu.
         * Les textes noirs sont donc rendus sans ombre. Sur les panneaux
         * verts/bleus, l'ombre légère reste conservée pour la lisibilité du
         * blanc, comme dans les aperçus précédents.
         */
        boolean textShadow = false;

        if (panel.doubleLine()) {
            int y1 = y + height / 3 - 4;
            int y2 = y + (height * 2) / 3 - 4;
            boolean line1Tracked = RoadTextFont.usesRegulatoryLetterSpacing(panel.line1Font())
                    && panel.line1Spacing();
            boolean line2Tracked = RoadTextFont.usesRegulatoryLetterSpacing(panel.line2Font())
                    && panel.line2Spacing();
            drawCenteredTrackedPreviewText(
                    graphics,
                    font,
                    shorten(line1, Math.max(8, (textRight - textLeft) / 6)),
                    textCenter,
                    y1,
                    textColor,
                    panel.line1Font(),
                    line1Tracked
            );
            drawCenteredTrackedPreviewText(
                    graphics,
                    font,
                    shorten(line2, Math.max(8, (textRight - textLeft) / 6)),
                    textCenter,
                    y2,
                    textColor,
                    panel.line2Font(),
                    line2Tracked
            );
            drawCenteredPreviewText(graphics, font, shorten(distance1, 6), distanceCenter, y1, textColor, textShadow);
            drawCenteredPreviewText(graphics, font, shorten(distance2, 6), distanceCenter, y2, textColor, textShadow);
        } else {
            boolean line1Tracked = RoadTextFont.usesRegulatoryLetterSpacing(panel.line1Font())
                    && panel.line1Spacing();
            int textY = y + height / 2 - 4;
            drawCenteredTrackedPreviewText(
                    graphics,
                    font,
                    shorten(line1, Math.max(8, (textRight - textLeft) / 6)),
                    textCenter,
                    textY,
                    textColor,
                    panel.line1Font(),
                    line1Tracked
            );
            drawCenteredPreviewText(graphics, font, shorten(distance1, 6), distanceCenter, textY, textColor, textShadow);
        }

        if (selected) {
            // Petit repère discret : le panneau édité est souligné en bleu,
            // sans masquer son vrai contour réglementaire.
            graphics.fill(x + 8, y + height - 2, x + width - 8, y + height, MODERN_BLUE);
        }
    }

    private static void drawCenteredPreviewText(
            GuiGraphicsExtractor graphics,
            Font font,
            String value,
            int centerX,
            int y,
            int color,
            boolean shadow
    ) {
        Component component = Component.literal(value == null ? "" : value);

        if (shadow) {
            graphics.centeredText(font, component, centerX, y, color);
            return;
        }

        int textX = centerX - font.width(component) / 2;
        graphics.text(font, component, textX, y, color, false);
    }

    private static void drawCenteredPreviewText(
            GuiGraphicsExtractor graphics,
            Font font,
            String value,
            int centerX,
            int y,
            int color,
            boolean shadow,
            RoadTextFont roadFont
    ) {
        Component component = roadComponent(value == null ? "" : value, roadFont);
        if (shadow) {
            graphics.centeredText(font, component, centerX, y, color);
            return;
        }
        int textX = centerX - font.width(component) / 2;
        graphics.text(font, component, textX, y, color, false);
    }

    private static void drawLeftPreviewText(
            GuiGraphicsExtractor graphics,
            Font font,
            String value,
            int x,
            int y,
            int color,
            RoadTextFont roadFont
    ) {
        Component component = roadComponent(value == null ? "" : value, roadFont);
        graphics.text(font, component, x, y, color, false);
    }

    private static void drawCenteredNoShadow(
            GuiGraphicsExtractor graphics,
            Font font,
            Component component,
            int centerX,
            int y,
            int color
    ) {
        int textX = centerX - font.width(component) / 2;
        graphics.text(font, component, textX, y, color, false);
    }

    /**
     * Espacement des lettres de l'aperçu : mêmes lettres dessinées une à une
     * avec un petit vide, pas une espace insérée dans le texte (une espace
     * pleine est trop large, et l'espace fine Unicode ressort en glyphe
     * manquant faute d'exister dans la police routière) — voir le même
     * choix, ainsi que l'abandon de l'espacement "optique" par lettre au
     * profit d'un écart fixe, dans
     * D21ABlockEntityRenderer.submitAnchoredTrackedText.
     */
    private static final float PREVIEW_LETTER_TRACKING_PIXELS = 1F;

    private static void drawCenteredTrackedPreviewText(
            GuiGraphicsExtractor graphics,
            Font font,
            String value,
            int centerX,
            int y,
            int color,
            RoadTextFont roadFont,
            boolean tracked
    ) {
        String safeValue = value == null ? "" : value;
        if (!tracked || safeValue.codePointCount(0, safeValue.length()) <= 1) {
            drawCenteredPreviewText(graphics, font, safeValue, centerX, y, color, false, roadFont);
            return;
        }
        int[] codePoints = safeValue.codePoints().toArray();
        Component[] chars = new Component[codePoints.length];
        /*
         * Font.width(...) arrondit (Mth.ceil) : appelé lettre par lettre,
         * chaque appel ajoute son propre arrondi indépendant, jusqu'à ~1px
         * de bruit différent par lettre. stringWidth (float, sans arrondi
         * intermédiaire, même splitter) évite ce bruit — voir la même
         * remarque dans D21ABlockEntityRenderer.submitAnchoredTrackedText.
         */
        float[] widths = new float[codePoints.length];
        for (int index = 0; index < codePoints.length; index++) {
            chars[index] = roadComponent(new String(Character.toChars(codePoints[index])), roadFont);
            widths[index] = font.getSplitter().stringWidth(chars[index]);
        }

        float[] advances = new float[codePoints.length - 1];
        float totalWidth = widths[codePoints.length - 1];
        for (int index = 0; index < codePoints.length - 1; index++) {
            float advance = widths[index] + PREVIEW_LETTER_TRACKING_PIXELS;
            advances[index] = advance;
            totalWidth += advance;
        }

        float cursor = centerX - totalWidth / 2F;
        for (int index = 0; index < codePoints.length; index++) {
            graphics.text(font, chars[index], Math.round(cursor), y, color, false);
            if (index < advances.length) {
                cursor += advances[index];
            }
        }
    }

    private static Component roadComponent(String value, RoadTextFont roadFont) {
        FontDescription.Resource resource = switch (roadFont) {
            case L1,NORMAL -> ROAD_FONT_L1;
            case L2 -> ROAD_FONT_L2;
            case L4 ->  ROAD_FONT_L4;
        };
        return Component.literal(value == null ? "" : value)
                .withStyle(Style.EMPTY.withFont(resource));
    }

    private static void drawPointedPanelShape(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            boolean pointsRight,
            int color
    ) {
        int pointWidth = Math.max(10, Math.min(width / 4, Math.round(height * 0.70F)));
        float center = (height - 1) / 2.0F;
        float divisor = Math.max(1.0F, center);

        for (int row = 0; row < height; row++) {
            float normalized = Math.abs(row - center) / divisor;
            int taper = Math.round(pointWidth * normalized);

            int start;
            int end;
            if (pointsRight) {
                start = x;
                end = x + width - taper;
            } else {
                start = x + taper;
                end = x + width;
            }

            if (end > start) {
                graphics.fill(start, y + row, end, y + row + 1, color);
            }
        }
    }

    private static void drawMotorwayLogo(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int size,
            D21AType panelType
    ) {
        int logoBg = panelType == D21AType.GREEN ? COLOR_BLUE : panelColor(panelType);
        int inset = Math.max(2, size / 10);

        graphics.fill(x, y, x + size, y + size, COLOR_WHITE);
        graphics.fill(x + inset, y + inset, x + size - inset, y + size - inset, logoBg);

        int cx = x + size / 2;
        int top = y + inset + 3;
        int bottom = y + size - inset - 3;
        int roadHalf = Math.max(2, size / 10);
        int bridgeY = y + size / 2;

        graphics.fill(cx - roadHalf - 2, top, cx - 1, bridgeY - 2, COLOR_WHITE);
        graphics.fill(cx + 1, top, cx + roadHalf + 2, bridgeY - 2, COLOR_WHITE);
        graphics.fill(x + inset + 2, bridgeY - 1, x + size - inset - 2, bridgeY + 2, COLOR_WHITE);
        graphics.fill(cx - roadHalf - 4, bridgeY + 2, cx - 1, bottom, COLOR_WHITE);
        graphics.fill(cx + 1, bridgeY + 2, cx + roadHalf + 4, bottom, COLOR_WHITE);
    }

    public static void drawD21Preview(
            GuiGraphicsExtractor graphics,
            Font font,
            PreviewBox box,
            D21AType type,
            boolean arrowRight,
            boolean doubleLine,
            String line1,
            String line2,
            String distance1,
            String distance2,
            CartoucheType cartoucheType,
            String cartoucheText
    ) {
        drawDirectionalPreview(
                graphics,
                font,
                box,
                doubleLine ? "D21 • 2 lignes" : "D21 • 1 ligne",
                type,
                arrowRight,
                doubleLine,
                line1,
                line2,
                distance1,
                distance2,
                cartoucheType,
                cartoucheText,
                true,
                arrowRight ? ">" : "<"
        );
    }

    private static void drawDirectionalPreview(
            GuiGraphicsExtractor graphics,
            Font font,
            PreviewBox box,
            String previewSubtitle,
            D21AType type,
            boolean arrowRight,
            boolean doubleLine,
            String line1,
            String line2,
            String distance1,
            String distance2,
            CartoucheType cartoucheType,
            String cartoucheText,
            boolean drawArrow,
            String arrowSymbol
    ) {
        if (box == null) {
            return;
        }

        drawPreviewCard(graphics, font, box, previewSubtitle);

        int topPad = box.side() ? 34 : 5;
        int px = box.x() + 10;
        int py = box.y() + topPad;
        int pw = box.width() - 20;
        int ph = Math.max(22, box.height() - topPad - 10);

        int cartoucheHeight = 0;
        if (cartoucheType != null && cartoucheType.isVisible()) {
            cartoucheHeight = Math.min(22, Math.max(10, ph / 4));
            int cw = Math.min(pw - 18, Math.max(48, textPixelWidth(cartoucheText) + 20));
            int cx = px + (pw - cw) / 2;
            graphics.fill(cx, py, cx + cw, py + cartoucheHeight, cartoucheColor(cartoucheType));
            graphics.outline(cx, py, cw, cartoucheHeight, COLOR_BLACK);
            drawCenteredNoShadow(
                    graphics,
                    font,
                    Component.literal(clean(cartoucheText).isBlank() ? cartoucheLabel(cartoucheType) : clean(cartoucheText)),
                    cx + cw / 2,
                    py + Math.max(1, (cartoucheHeight - 9) / 2),
                    cartoucheTextColor(cartoucheType)
            );
            py += cartoucheHeight + 4;
            ph -= cartoucheHeight + 4;
        }

        int bg = panelColor(type);
        int fg = panelTextColor(type);
        graphics.fill(px, py, px + pw, py + ph, bg);
        graphics.outline(px, py, pw, ph, COLOR_BLACK);

        int arrowZone = drawArrow ? Math.max(18, pw / 5) : 0;
        int distanceZone = Math.max(26, pw / 5);
        int textLeft;
        int textRight;
        int distanceCenter;

        if (arrowRight) {
            textLeft = px + 8;
            textRight = px + pw - arrowZone - distanceZone - 4;
            distanceCenter = px + pw - arrowZone - distanceZone / 2;
            if (drawArrow) {
                graphics.centeredText(font, Component.literal(arrowSymbol), px + pw - arrowZone / 2, py + ph / 2 - 4, fg);
            }
        } else {
            textLeft = px + arrowZone + distanceZone + 4;
            textRight = px + pw - 8;
            distanceCenter = px + arrowZone + distanceZone / 2;
            if (drawArrow) {
                graphics.centeredText(font, Component.literal(arrowSymbol), px + arrowZone / 2, py + ph / 2 - 4, fg);
            }
        }

        if (doubleLine) {
            int y1 = py + ph / 3 - 4;
            int y2 = py + (ph * 2) / 3 - 4;
            graphics.centeredText(font, Component.literal(shorten(clean(line1), 22)), (textLeft + textRight) / 2, y1, fg);
            graphics.centeredText(font, Component.literal(shorten(clean(line2), 22)), (textLeft + textRight) / 2, y2, fg);
            graphics.centeredText(font, Component.literal(shorten(clean(distance1), 6)), distanceCenter, y1, fg);
            graphics.centeredText(font, Component.literal(shorten(clean(distance2), 6)), distanceCenter, y2, fg);
        } else {
            int ty = py + ph / 2 - 4;
            graphics.centeredText(font, Component.literal(shorten(clean(line1), 24)), (textLeft + textRight) / 2, ty, fg);
            graphics.centeredText(font, Component.literal(shorten(clean(distance1), 6)), distanceCenter, ty, fg);
        }
    }


    public static void drawD61StackPreview(
            GuiGraphicsExtractor graphics,
            Font font,
            PreviewBox box,
            D61APanelData[] panels,
            int selectedPanelIndex,
            CartoucheType cartoucheType,
            String cartoucheText
    ) {
        if (box == null) {
            return;
        }

        drawPreviewCard(graphics, font, box, "D61 • ensemble configuré");
        int topPad = box.side() ? 34 : 5;
        int areaX = box.x() + 12;
        int areaY = box.y() + topPad;
        int areaWidth = Math.max(50, box.width() - 24);
        int areaHeight = Math.max(40, box.height() - topPad - 12);

        int activeCount = 0;
        int rawHeight = 0;
        if (panels != null) {
            for (D61APanelData panel : panels) {
                if (panel != null && panel.enabled()) {
                    activeCount++;
                    rawHeight += panel.doubleLine() ? 52 : 38;
                }
            }
        }
        if (activeCount == 0) {
            graphics.centeredText(font, Component.literal("Aucun panneau actif"), areaX + areaWidth / 2, areaY + areaHeight / 2 - 4, MODERN_MUTED);
            return;
        }

        rawHeight += Math.max(0, activeCount - 1) * 5;
        int cartoucheHeight = cartoucheType != null && cartoucheType.isVisible() ? 20 : 0;
        if (cartoucheHeight > 0) rawHeight += cartoucheHeight + 6;
        float scale = Math.min(1.0F, areaHeight / (float) Math.max(1, rawHeight));
        int gap = Math.max(2, Math.round(5 * scale));
        int panelWidth = Math.max(90, areaWidth - 20);

        int actualHeight = cartoucheHeight > 0 ? Math.max(10, Math.round(cartoucheHeight * scale)) + gap : 0;
        if (panels != null) {
            for (D61APanelData panel : panels) {
                if (panel != null && panel.enabled()) {
                    actualHeight += Math.max(22, Math.round((panel.doubleLine() ? 52 : 38) * scale));
                }
            }
        }
        actualHeight += Math.max(0, activeCount - 1) * gap;
        int y = areaY + Math.max(0, (areaHeight - actualHeight) / 2);

        if (cartoucheHeight > 0) {
            int h = Math.max(10, Math.round(cartoucheHeight * scale));
            drawCartouchePreview(graphics, font, areaX, y, areaWidth, h, cartoucheType, cartoucheText);
            y += h + gap;
        }

        if (panels == null) return;
        for (int i = 0; i < panels.length; i++) {
            D61APanelData panel = panels[i];
            if (panel == null || !panel.enabled()) continue;
            int h = Math.max(22, Math.round((panel.doubleLine() ? 52 : 38) * scale));
            int x = areaX + (areaWidth - panelWidth) / 2;
            drawD61PanelPreview(graphics, font, x, y, panelWidth, h, panel, i == selectedPanelIndex);
            y += h + gap;
        }
    }

    private static void drawD61PanelPreview(
            GuiGraphicsExtractor graphics,
            Font font,
            int x,
            int y,
            int width,
            int height,
            D61APanelData panel,
            boolean selected
    ) {
        D21AType type = panel.type() == null ? D21AType.WHITE : panel.type();
        boolean colored = type != D21AType.WHITE;
        int fill = panelColor(type);
        int fg = panelTextColor(type);

        graphics.fill(x, y, x + width, y + height, colored ? COLOR_WHITE : COLOR_BLACK);
        int inset = colored ? 2 : 2;
        graphics.fill(x + inset, y + inset, x + width - inset, y + height - inset, fill);

        int innerLeft = x + 8;
        int innerRight = x + width - 8;
        int logoSize = panel.autorouteLogo() && colored ? Math.max(16, Math.min(28, height - 8)) : 0;
        boolean arrow = panel.arrowEnabled();
        int arrowZone = arrow ? Math.max(22, width / 7) : 0;
        int arrowCenter = 0;

        if (arrow) {
            if (panel.arrowPosition() == D61AArrowPosition.LEFT) {
                arrowCenter = innerLeft + arrowZone / 2;
                innerLeft += arrowZone;
            } else {
                arrowCenter = innerRight - arrowZone / 2;
                innerRight -= arrowZone;
            }
        }

        if (logoSize > 0) {
            int logoX = panel.arrowPosition() == D61AArrowPosition.LEFT ? innerRight - logoSize : innerLeft;
            if (panel.arrowPosition() == D61AArrowPosition.LEFT) innerRight -= logoSize + 5; else innerLeft += logoSize + 5;
            drawMotorwayLogo(graphics, logoX, y + (height - logoSize) / 2, logoSize, type);
        }

        if (arrow) {
            drawCenteredNoShadow(graphics, font, Component.literal(panel.arrowDirection().symbol()), arrowCenter, y + height / 2 - 4, fg);
        }

        int distanceWidth = arrow ? 0 : Math.max(30, Math.min(48, (innerRight - innerLeft) / 4));
        int distanceCenter = innerLeft + distanceWidth / 2;
        int textLeft = innerLeft + (distanceWidth > 0 ? distanceWidth + 3 : 0);
        int textRight = innerRight;
        int textCenter = (textLeft + textRight) / 2;

        if (panel.doubleLine()) {
            int y1 = y + height / 3 - 4;
            int y2 = y + height * 2 / 3 - 4;
            drawLeftPreviewText(graphics, font, shorten(clean(panel.line1()), Math.max(8, (textRight - textLeft) / 6)), textLeft, y1, fg, panel.line1Font());
            drawLeftPreviewText(graphics, font, shorten(clean(panel.line2()), Math.max(8, (textRight - textLeft) / 6)), textLeft, y2, fg, panel.line2Font());
            if (!arrow) {
                drawCenteredPreviewText(graphics, font, shorten(clean(panel.distance1()), 6), distanceCenter, y1, fg, false);
                drawCenteredPreviewText(graphics, font, shorten(clean(panel.distance2()), 6), distanceCenter, y2, fg, false);
            }
        } else {
            int ty = y + height / 2 - 4;
            drawLeftPreviewText(graphics, font, shorten(clean(panel.line1()), Math.max(8, (textRight - textLeft) / 6)), textLeft, ty, fg, panel.line1Font());
            if (!arrow) drawCenteredPreviewText(graphics, font, shorten(clean(panel.distance()), 6), distanceCenter, ty, fg, false);
        }

        if (selected) graphics.fill(x + 5, y + height - 2, x + width - 5, y + height, MODERN_BLUE);
    }

    public static void drawD61Preview(
            GuiGraphicsExtractor graphics,
            Font font,
            PreviewBox box,
            D21AType type,
            boolean doubleLine,
            String line1,
            String line2,
            String distance1,
            String distance2,
            boolean arrowEnabled,
            D61AArrowPosition arrowPosition,
            D61AArrowDirection arrowDirection,
            CartoucheType cartoucheType,
            String cartoucheText
    ) {
        boolean rightSide = !arrowEnabled || arrowPosition == D61AArrowPosition.RIGHT;
        String symbol = arrowDirection == null ? "↑" : arrowDirection.symbol();

        drawDirectionalPreview(
                graphics,
                font,
                box,
                doubleLine ? "D61 • 2 lignes" : "D61 • 1 ligne",
                type,
                rightSide,
                doubleLine,
                line1,
                line2,
                arrowEnabled ? "" : distance1,
                arrowEnabled ? "" : distance2,
                cartoucheType,
                cartoucheText,
                arrowEnabled,
                symbol
        );
    }

    public static void drawE31Preview(
            GuiGraphicsExtractor graphics,
            Font font,
            PreviewBox box,
            boolean waterName,
            String value
    ) {
        if (box == null) {
            return;
        }

        drawPreviewCard(graphics, font, box, waterName ? "E31b • cours d'eau" : "E31a • lieu-dit");
        int topPad = box.side() ? 42 : 8;
        int areaX = box.x() + 12;
        int areaY = box.y() + topPad;
        int areaW = box.width() - 24;
        int areaH = Math.max(28, box.height() - topPad - 16);

        // E31a/E31b sont des plaques horizontales noires et non de grands rectangles verticaux.
        int signH = Math.max(32, Math.min(70, areaH / 3));
        int signW = Math.max(120, Math.min(areaW - 12, Math.round(signH * (waterName ? 5.0F : 5.7F))));
        int sx = areaX + (areaW - signW) / 2;
        int sy = areaY + (areaH - signH) / 2;
        graphics.fill(sx, sy, sx + signW, sy + signH, COLOR_BLACK);
        graphics.outline(sx, sy, signW, signH, 0xFF3B3B3B);

        int textLeft = sx + 12;
        int textRight = sx + signW - 12;
        if (waterName) {
            int waveX = sx + Math.max(28, signW / 8);
            int waveY = sy + signH / 2;
            // Trois lignes ondulées stylisées, proches du pictogramme réel.
            for (int row = -1; row <= 1; row++) {
                int yy = waveY + row * 8;
                for (int i = 0; i < 5; i++) {
                    int x1 = waveX - 18 + i * 7;
                    int offset = (i % 2 == 0) ? -2 : 2;
                    graphics.fill(x1, yy + offset, x1 + 7, yy + offset + 2, COLOR_WHITE);
                }
            }
            textLeft = sx + Math.max(58, signW / 4);
        }

        Component text = roadComponent(shorten(clean(value), waterName ? 22 : 28), RoadTextFont.L4);
        int center = (textLeft + textRight) / 2;
        drawCenteredNoShadow(graphics, font, text, center, sy + signH / 2 - 4, COLOR_WHITE);
    }

    public static void drawEBPreview(
            GuiGraphicsExtractor graphics,
            Font font,
            PreviewBox box,
            boolean eb20,
            String line1,
            String line2,
            RoadTextFont line1Font,
            RoadTextFont line2Font,
            CartoucheType cartoucheType,
            String cartoucheText
    ) {
        if (box == null) {
            return;
        }

        drawPreviewCard(graphics, font, box, eb20 ? "EB20 • sortie d'agglomération" : "EB10 • entrée d'agglomération");
        int topPad = box.side() ? 36 : 6;
        int areaX = box.x() + 12;
        int areaY = box.y() + topPad;
        int areaW = box.width() - 24;
        int areaH = Math.max(30, box.height() - topPad - 12);

        int cartoucheH = 0;
        if (cartoucheType != null && cartoucheType.isVisible()) {
            cartoucheH = Math.min(22, Math.max(12, areaH / 5));
            drawCartouchePreview(graphics, font, areaX, areaY, areaW, cartoucheH, cartoucheType, cartoucheText);
        }

        int signH = Math.max(34, Math.min(88, (areaH - cartoucheH - 8) / 2));
        int signW = Math.max(150, Math.min(areaW - 10, Math.round(signH * 3.0F)));
        int sx = areaX + (areaW - signW) / 2;
        int sy = areaY + cartoucheH + (cartoucheH > 0 ? 7 : 0) + Math.max(0, (areaH - cartoucheH - signH - 7) / 2);

        // EB10 : bordure rouge. EB20 : bordure noire.
        int border = eb20 ? COLOR_BLACK : COLOR_RED;
        graphics.fill(sx, sy, sx + signW, sy + signH, COLOR_WHITE);
        int thick = Math.max(3, signH / 10);
        graphics.fill(sx, sy, sx + signW, sy + thick, border);
        graphics.fill(sx, sy + signH - thick, sx + signW, sy + signH, border);
        graphics.fill(sx, sy, sx + thick, sy + signH, border);
        graphics.fill(sx + signW - thick, sy, sx + signW, sy + signH, border);

        int centerX = sx + signW / 2;
        if (clean(line2).isBlank()) {
            drawCenteredNoShadow(graphics, font, roadComponent(shorten(clean(line1), 28), line1Font), centerX, sy + signH / 2 - 4, COLOR_BLACK);
        } else {
            drawCenteredNoShadow(graphics, font, roadComponent(shorten(clean(line1), 28), line1Font), centerX, sy + signH / 3 - 4, COLOR_BLACK);
            drawCenteredNoShadow(graphics, font, roadComponent(shorten(clean(line2), 28), line2Font), centerX, sy + signH * 2 / 3 - 4, COLOR_BLACK);
        }
    }

    public static void drawD42FullPreview(
            GuiGraphicsExtractor graphics,
            Font font,
            PreviewBox box,
            D42bBranchData[] branches,
            int selectedBranchIndex,
            String distance
    ) {
        if (box == null) return;

        drawPreviewCard(graphics, font, box, "D42b • giratoire complet");
        int topPad = box.side() ? 34 : 5;
        int px = box.x() + 10;
        int py = box.y() + topPad;
        int pw = box.width() - 20;
        int ph = Math.max(40, box.height() - topPad - 10);

        graphics.fill(px, py, px + pw, py + ph, 0xFFD3D3D3);
        graphics.outline(px, py, pw, ph, COLOR_BLACK);

        int cx = px + pw / 2;
        int cy = py + ph / 2;
        int outerRadius = Math.max(14, Math.min(pw, ph) / 9);
        int innerRadius = Math.max(8, Math.round(outerRadius * 0.58F));
        drawCircleRing(graphics, cx, cy, outerRadius, innerRadius, COLOR_BLACK);

        if (branches != null) {
            for (int i = 0; i < branches.length; i++) {
                D42bBranchData branch = branches[i];
                if (branch == null || !branch.enabled()) continue;

                double rad = Math.toRadians(branch.angleDegrees());
                double dx = Math.sin(rad);
                double dy = -Math.cos(rad);
                int maxRadius = Math.max(outerRadius + 18, Math.min(pw, ph) / 3);
                int startRadius = outerRadius - 1;
                int endX = cx + (int) Math.round(dx * maxRadius);
                int endY = cy + (int) Math.round(dy * maxRadius);

                drawRadialLine(graphics, cx, cy, startRadius, maxRadius, dx, dy, COLOR_BLACK);
                drawArrowHead(graphics, endX, endY, dx, dy, COLOR_BLACK);

                int labelX = cx + (int) Math.round(dx * (maxRadius + 30));
                int labelY = cy + (int) Math.round(dy * (maxRadius + 18));
                labelX = Math.max(px + 48, Math.min(px + pw - 48, labelX));
                labelY = Math.max(py + 14, Math.min(py + ph - 30, labelY));
                drawD42BranchLabels(graphics, font, labelX, labelY, branch, i == selectedBranchIndex);
            }
        }

        if (!clean(distance).isBlank()) {
            Component dist = roadComponent(shorten(clean(distance), 12), RoadTextFont.L4);
            graphics.text(font, dist, px + 10, py + ph - 15, COLOR_BLACK, false);
        }
    }

    private static void drawD42BranchLabels(
            GuiGraphicsExtractor graphics,
            Font font,
            int centerX,
            int centerY,
            D42bBranchData branch,
            boolean selected
    ) {
        String line1 = shorten(clean(branch.line1()), 18);
        String line2 = shorten(clean(branch.line2()), 18);
        int y = centerY - (line2.isBlank() ? 5 : 11);
        if (!line1.isBlank()) {
            drawD42LabelWithFont(graphics, font, centerX, y, line1, branch.line1Color(), branch.line1Font());
            y += 13;
        }
        if (!line2.isBlank()) {
            drawD42LabelWithFont(graphics, font, centerX, y, line2, branch.line2Color(), branch.line2Font());
        }
        if (selected) {
            graphics.fill(centerX - 12, y + 11, centerX + 12, y + 13, MODERN_BLUE);
        }
    }

    private static void drawD42LabelWithFont(
            GuiGraphicsExtractor graphics,
            Font font,
            int centerX,
            int y,
            String value,
            D42bLabelColor color,
            RoadTextFont roadFont
    ) {
        Component component = roadComponent(value, roadFont);
        int textWidth = font.width(component);
        int bg = labelColor(color);
        if (bg != 0) {
            graphics.fill(centerX - textWidth / 2 - 4, y - 2, centerX + textWidth / 2 + 4, y + 10, bg);
        }
        int fg = bg == 0 ? COLOR_BLACK : COLOR_WHITE;
        drawCenteredNoShadow(graphics, font, component, centerX, y, fg);
    }

    private static void drawCircleRing(GuiGraphicsExtractor graphics, int cx, int cy, int outer, int inner, int color) {
        int outer2 = outer * outer;
        int inner2 = inner * inner;
        for (int yy = -outer; yy <= outer; yy++) {
            int runStart = Integer.MIN_VALUE;
            for (int xx = -outer; xx <= outer; xx++) {
                int d2 = xx * xx + yy * yy;
                boolean fill = d2 <= outer2 && d2 >= inner2;
                if (fill && runStart == Integer.MIN_VALUE) runStart = xx;
                if ((!fill || xx == outer) && runStart != Integer.MIN_VALUE) {
                    int end = fill && xx == outer ? xx + 1 : xx;
                    graphics.fill(cx + runStart, cy + yy, cx + end, cy + yy + 1, color);
                    runStart = Integer.MIN_VALUE;
                }
            }
        }
    }

    private static void drawRadialLine(
            GuiGraphicsExtractor graphics, int cx, int cy, int startRadius, int endRadius, double dx, double dy, int color
    ) {
        for (int r = startRadius; r <= endRadius; r += 2) {
            int x = cx + (int) Math.round(dx * r);
            int y = cy + (int) Math.round(dy * r);
            graphics.fill(x - 2, y - 2, x + 3, y + 3, color);
        }
    }

    private static void drawArrowHead(GuiGraphicsExtractor graphics, int x, int y, double dx, double dy, int color) {
        double px = -dy;
        double py = dx;
        for (int back = 0; back <= 10; back += 2) {
            int half = Math.max(1, (10 - back) / 2);
            int bx = x - (int) Math.round(dx * back);
            int by = y - (int) Math.round(dy * back);
            int x1 = bx + (int) Math.round(px * half);
            int y1 = by + (int) Math.round(py * half);
            int x2 = bx - (int) Math.round(px * half);
            int y2 = by - (int) Math.round(py * half);
            graphics.fill(Math.min(x1, x2) - 1, Math.min(y1, y2) - 1, Math.max(x1, x2) + 2, Math.max(y1, y2) + 2, color);
        }
    }

    public static void drawD42Preview(
            GuiGraphicsExtractor graphics,
            Font font,
            PreviewBox box,
            String line1,
            String line2,
            D42bLabelColor line1Color,
            D42bLabelColor line2Color,
            String distance
    ) {
        if (box == null) {
            return;
        }

        drawPreviewCard(graphics, font, box, "D42b • branche sélectionnée");
        int topPad = box.side() ? 34 : 5;
        int px = box.x() + 10;
        int py = box.y() + topPad;
        int pw = box.width() - 20;
        int ph = Math.max(22, box.height() - topPad - 10);

        graphics.fill(px, py, px + pw, py + ph, 0xFFD3D3D3);
        graphics.outline(px, py, pw, ph, COLOR_BLACK);
        graphics.centeredText(font, Component.literal("↻"), px + pw / 2, py + ph / 2 - 5, COLOR_BLACK);

        int textX = px + (pw * 3) / 4;
        int y1 = py + ph / 3 - 4;
        int y2 = py + ph / 2 + 5;
        drawOptionalLabel(graphics, font, textX, y1, line1, line1Color);
        drawOptionalLabel(graphics, font, textX, y2, line2, line2Color);

        if (!clean(distance).isBlank()) {
            graphics.centeredText(font, Component.literal(shorten(clean(distance), 10)), px + Math.max(24, pw / 6), py + ph - 14, COLOR_BLACK);
        }
    }

    private static void drawOptionalLabel(
            GuiGraphicsExtractor graphics,
            Font font,
            int x,
            int y,
            String value,
            D42bLabelColor color
    ) {
        String clean = shorten(clean(value), 18);
        if (clean.isBlank()) {
            return;
        }

        int width = Math.max(34, textPixelWidth(clean) + 12);
        int bg = labelColor(color);
        if (bg != 0) {
            graphics.fill(x - width / 2, y - 2, x + width / 2, y + 11, bg);
        }
        int fg = bg == 0 ? COLOR_BLACK : COLOR_WHITE;
        graphics.centeredText(font, Component.literal(clean), x, y, fg);
    }

    public static void drawB14Preview(
            GuiGraphicsExtractor graphics,
            Font font,
            PreviewBox box,
            String speed
    ) {
        if (box == null) return;
        drawPreviewCard(graphics, font, box, "B14 • limitation");
        int topPad = box.side() ? 38 : 6;
        int diameter = Math.max(36, Math.min(box.width() - 36, box.height() - topPad - 18));
        int cx = box.x() + box.width() / 2;
        int cy = box.y() + topPad + diameter / 2;
        drawFilledCircle(graphics, cx, cy, diameter / 2, COLOR_RED);
        drawFilledCircle(graphics, cx, cy, Math.max(8, diameter / 2 - Math.max(4, diameter / 12)), COLOR_WHITE);
        drawCenteredNoShadow(graphics, font, roadComponent(clean(speed), RoadTextFont.L1), cx, cy - 4, COLOR_BLACK);
    }

    private static void drawFilledCircle(GuiGraphicsExtractor graphics, int cx, int cy, int radius, int color) {
        int r2 = radius * radius;
        for (int yy = -radius; yy <= radius; yy++) {
            int half = (int) Math.floor(Math.sqrt(Math.max(0, r2 - yy * yy)));
            graphics.fill(cx - half, cy + yy, cx + half + 1, cy + yy + 1, color);
        }
    }

    public static int panelColor(D21AType type) {
        if (type == D21AType.GREEN) {
            return COLOR_GREEN;
        }
        if (type == D21AType.BLUE) {
            return COLOR_BLUE;
        }
        return COLOR_WHITE;
    }

    public static int panelTextColor(D21AType type) {
        return type == D21AType.WHITE ? COLOR_BLACK : COLOR_WHITE;
    }

    private static int cartoucheColor(CartoucheType type) {
        if (type == null) {
            return COLOR_WHITE;
        }
        return switch (type) {
            case E41_45 -> COLOR_GREEN;
            case E42 -> COLOR_RED;
            case E43 -> COLOR_YELLOW;
            case E44 -> COLOR_WHITE;
            case E47 -> COLOR_BLUE;
            default -> COLOR_WHITE;
        };
    }

    private static int cartoucheTextColor(CartoucheType type) {
        return type == CartoucheType.E43 || type == CartoucheType.E44
                ? COLOR_BLACK
                : COLOR_WHITE;
    }

    private static int labelColor(D42bLabelColor color) {
        if (color == D42bLabelColor.GREEN) {
            return COLOR_GREEN;
        }
        if (color == D42bLabelColor.BLUE) {
            return COLOR_BLUE;
        }
        return 0;
    }

    private static int textPixelWidth(String value) {
        return clean(value).length() * 6;
    }

    public static String clean(String value) {
        return value == null ? "" : value.strip();
    }

    public static String shorten(String value, int max) {
        String clean = clean(value);
        if (clean.length() <= max) {
            return clean;
        }
        if (max <= 1) {
            return clean.substring(0, Math.max(0, max));
        }
        return clean.substring(0, max - 1) + "…";
    }


    public record EditorLayout(
            int formX,
            int formY,
            int formWidth,
            int formHeight,
            PreviewBox previewBox,
            boolean compact
    ) {
    }

    public static EditorLayout editorLayout(
            int screenWidth,
            int screenHeight,
            int preferredFormWidth,
            int preferredFormHeight,
            int preferredPreviewWidth,
            int preferredPreviewHeight
    ) {
        int margin = 14;
        int gap = 14;
        int previewWidth = Math.min(preferredPreviewWidth, Math.max(180, screenWidth / 4));
        int previewHeight = Math.min(preferredPreviewHeight, Math.max(140, screenHeight / 3));

        boolean side = screenWidth >= preferredFormWidth + previewWidth + margin * 2 + gap + 24;

        int formWidth = Math.min(preferredFormWidth, screenWidth - margin * 2 - (side ? previewWidth + gap : 0));
        int formHeight = Math.min(preferredFormHeight, screenHeight - margin * 2 - (side ? 0 : previewHeight + gap));

        int formX;
        int formY;
        PreviewBox preview;

        if (side) {
            int groupWidth = previewWidth + gap + formWidth;
            int startX = (screenWidth - groupWidth) / 2;
            formX = startX + previewWidth + gap;
            formY = Math.max(margin, (screenHeight - formHeight) / 2);
            int previewY = formY + Math.max(22, Math.min(46, formHeight / 8));
            int sidePreviewHeight = Math.min(formHeight - Math.max(34, formHeight / 8), previewHeight);
            preview = new PreviewBox(startX, previewY, previewWidth, sidePreviewHeight, true);
        } else {
            formX = (screenWidth - formWidth) / 2;
            int previewX = (screenWidth - previewWidth) / 2;
            int previewY = margin;
            preview = new PreviewBox(previewX, previewY, previewWidth, previewHeight, false);
            formY = previewY + previewHeight + gap;
        }

        return new EditorLayout(formX, formY, formWidth, formHeight, preview, !side);
    }

    public static void drawModernFormCard(
            GuiGraphicsExtractor graphics,
            Font font,
            EditorLayout layout,
            Component title,
            Component subtitle
    ) {
        int x = layout.formX();
        int y = layout.formY();
        int w = layout.formWidth();
        int h = layout.formHeight();

        graphics.fill(x, y, x + w, y + h, COLOR_CARD_SOFT);
        graphics.outline(x, y, w, h, COLOR_BORDER);

        graphics.centeredText(font, title, x + w / 2, y + 8, COLOR_TEXT);
        if (subtitle != null) {
            graphics.centeredText(font, subtitle, x + w / 2, y + 20, COLOR_SUBTEXT);
        }
    }

    public static void drawSectionCard(
            GuiGraphicsExtractor graphics,
            Font font,
            int x,
            int y,
            int width,
            int height,
            String title,
            String subtitle
    ) {
        graphics.fill(x, y, x + width, y + height, COLOR_CARD);
        graphics.outline(x, y, width, height, COLOR_BORDER);
        graphics.centeredText(font, Component.literal(title), x + width / 2, y + 6, COLOR_TEXT);
        if (subtitle != null && !subtitle.isBlank()) {
            graphics.centeredText(font, Component.literal(subtitle), x + width / 2, y + 17, COLOR_SUBTEXT);
        }
    }


    public static final int MODERN_WINDOW = 0xF218222E;
    public static final int MODERN_PANEL = 0xEE202A36;
    public static final int MODERN_PANEL_HOVER = 0xFF2B3948;
    public static final int MODERN_PANEL_SELECTED = 0xFF145CC8;
    public static final int MODERN_BLUE = 0xFF1776E5;
    public static final int MODERN_BLUE_DARK = 0xFF0D4FA8;
    public static final int MODERN_GREEN = 0xFF15834A;
    public static final int MODERN_BORDER = 0xFF46576B;
    public static final int MODERN_BORDER_SOFT = 0xFF334252;
    public static final int MODERN_MUTED = 0xFF93A0AF;

    public record Rect(int x, int y, int width, int height) {
        public boolean contains(double mouseX, double mouseY) {
            return mouseX >= this.x
                    && mouseX < this.x + this.width
                    && mouseY >= this.y
                    && mouseY < this.y + this.height;
        }
    }

    /**
     * Tronque un texte selon sa largeur réelle dans la police Minecraft.
     * Utilisé par toutes les interfaces configurables afin qu'aucun libellé
     * ne puisse sortir de son cadre quand le GUI Scale réduit la résolution
     * logique de l'écran.
     */
    public static String fitText(Font font, String value, int maxWidth) {
        if (font == null || value == null || value.isEmpty() || maxWidth <= 0) {
            return "";
        }
        if (font.width(value) <= maxWidth) {
            return value;
        }

        String ellipsis = "…";
        int ellipsisWidth = font.width(ellipsis);
        if (ellipsisWidth >= maxWidth) {
            return "";
        }

        int end = value.length();
        while (end > 0 && font.width(value.substring(0, end)) + ellipsisWidth > maxWidth) {
            end--;
        }
        return end <= 0 ? ellipsis : value.substring(0, end) + ellipsis;
    }

    public static int safeControlHeight(Font font, int requestedHeight) {
        int fontHeight = font == null ? 9 : font.lineHeight;
        return Math.max(fontHeight + 7, requestedHeight);
    }

    public static void drawModernWindow(
            GuiGraphicsExtractor graphics,
            Font font,
            int x,
            int y,
            int width,
            int height,
            String title,
            String subtitle
    ) {
        graphics.fill(x, y, x + width, y + height, MODERN_WINDOW);
        graphics.outline(x, y, width, height, MODERN_BORDER);

        boolean compactHeader = height < 560;
        boolean ultraCompactHeader = height < 440;
        int iconSize = ultraCompactHeader ? 20 : compactHeader ? 24 : 28;
        int iconX = x + 12;
        int iconY = y + (ultraCompactHeader ? 6 : compactHeader ? 7 : 10);
        graphics.fill(iconX, iconY, iconX + iconSize, iconY + iconSize, MODERN_BLUE_DARK);
        graphics.outline(iconX, iconY, iconSize, iconSize, 0xFF2C8CFF);
        graphics.centeredText(font, Component.literal("D21"), iconX + iconSize / 2, iconY + Math.max(4, (iconSize - 9) / 2), COLOR_WHITE);
        if (!ultraCompactHeader) {
            graphics.fill(iconX + iconSize / 2 - 1, iconY + iconSize - 7, iconX + iconSize / 2 + 1, iconY + iconSize - 1, COLOR_WHITE);
        }

        int titleX = iconX + iconSize + 12;
        int titleY = y + (ultraCompactHeader ? 8 : compactHeader ? 9 : 12);
        int textMaxWidth = Math.max(0, x + width - titleX - 10);
        graphics.text(font, Component.literal(fitText(font, title, textMaxWidth)), titleX, titleY, COLOR_TEXT, false);
        if (!ultraCompactHeader && subtitle != null && !subtitle.isBlank()) {
            graphics.text(font, Component.literal(fitText(font, subtitle, textMaxWidth)), titleX, titleY + 13, MODERN_MUTED, false);
        }
    }

    public static void drawModernWindow(
            GuiGraphicsExtractor graphics,
            Font font,
            int x,
            int y,
            int width,
            int height,
            String iconLabel,
            String title,
            String subtitle
    ) {
        graphics.fill(x, y, x + width, y + height, MODERN_WINDOW);
        graphics.outline(x, y, width, height, MODERN_BORDER);

        boolean compactHeader = height < 560;
        boolean ultraCompactHeader = height < 440;
        int iconSize = ultraCompactHeader ? 20 : compactHeader ? 24 : 28;
        int iconX = x + 12;
        int iconY = y + (ultraCompactHeader ? 6 : compactHeader ? 7 : 10);
        graphics.fill(iconX, iconY, iconX + iconSize, iconY + iconSize, MODERN_BLUE_DARK);
        graphics.outline(iconX, iconY, iconSize, iconSize, 0xFF2C8CFF);
        /*
         * shorten() coupe par NOMBRE de caractères, pas par largeur réelle :
         * un badge de 4 caractères larges (ex. "D/DA") pouvait donc
         * dépasser cette petite case, alors qu'un badge à 3 lettres fines
         * y tenait. fitText() mesure en pixels et garantit que le badge
         * reste dans la case quelle que soit sa longueur.
         */
        graphics.centeredText(
                font,
                Component.literal(fitText(font, iconLabel == null ? "" : iconLabel, iconSize - 4)),
                iconX + iconSize / 2,
                iconY + Math.max(4, (iconSize - 9) / 2),
                COLOR_WHITE
        );

        int titleX = iconX + iconSize + 12;
        int titleY = y + (ultraCompactHeader ? 8 : compactHeader ? 9 : 12);
        int textMaxWidth = Math.max(0, x + width - titleX - 10);
        graphics.text(font, Component.literal(fitText(font, title, textMaxWidth)), titleX, titleY, COLOR_TEXT, false);
        if (!ultraCompactHeader && subtitle != null && !subtitle.isBlank()) {
            graphics.text(font, Component.literal(fitText(font, subtitle, textMaxWidth)), titleX, titleY + 13, MODERN_MUTED, false);
        }
    }

    public static String fontLabel(RoadTextFont font) {
        if (font == null) {
            return "Standard";
        }
        return switch (font) {
            case NORMAL -> "Normal";
            case L1, L2 -> "Standard";
            case L4 -> "Italique";
        };
    }

    public static String cartoucheLabel(CartoucheType type) {
        if (type == null) {
            return "Aucun";
        }
        return switch (type) {
            case E41_45 -> "Vert";
            case E42 -> "Rouge";
            case E43 -> "Jaune";
            case E44 -> "Blanc";
            case E47 -> "Bleu";
            default -> "Aucun";
        };
    }

    public static void drawModernSection(
            GuiGraphicsExtractor graphics,
            Font font,
            Rect rect,
            String title,
            String subtitle
    ) {
        graphics.fill(rect.x(), rect.y(), rect.x() + rect.width(), rect.y() + rect.height(), MODERN_PANEL);
        graphics.outline(rect.x(), rect.y(), rect.width(), rect.height(), MODERN_BORDER_SOFT);
        int maxTextWidth = Math.max(0, rect.width() - 20);
        if (rect.height() >= font.lineHeight + 6) {
            graphics.text(font, Component.literal(fitText(font, title, maxTextWidth)), rect.x() + 10, rect.y() + 8, COLOR_TEXT, false);
        }

        /*
         * V111 : avec une GUI Scale élevée, la résolution logique de Minecraft
         * devient beaucoup plus petite. Les anciennes descriptions restaient
         * pourtant dessinées à taille de police fixe et finissaient par se
         * superposer aux champs. Une carte compacte garde donc uniquement son
         * titre ; le sous-titre revient automatiquement dès que la hauteur le
         * permet.
         */
        if (rect.height() >= 104 && subtitle != null && !subtitle.isBlank()) {
            graphics.text(font, Component.literal(fitText(font, subtitle, maxTextWidth)), rect.x() + 10, rect.y() + 21, MODERN_MUTED, false);
        }
    }

    /**
     * Zone de clic commune pour les interrupteurs placés dans les onglets.
     * Les dimensions sont exprimées en pixels GUI réels (pas en design pixels
     * multipliés par le facteur adaptatif), ce qui garantit que la zone cliquable
     * reste superposée à l'interrupteur visible même avec un GUI Scale élevé.
     */
    public static Rect tabToggleRect(Rect tabRect, Font font) {
        if (tabRect == null) {
            return new Rect(0, 0, 1, 1);
        }

        int fontHeight = font == null ? 9 : font.lineHeight;
        int maxWidth = Math.max(1, tabRect.width() - 8);
        int desiredWidth = Math.max(28, Math.min(36, tabRect.width() / 4));
        int width = Math.min(desiredWidth, maxWidth);

        int maxHeight = Math.max(1, tabRect.height() - 4);
        int desiredHeight = Math.max(18, fontHeight + 8);
        int height = Math.min(desiredHeight, maxHeight);

        int x = tabRect.x() + tabRect.width() - width - 4;
        int y = tabRect.y() + Math.max(2, (tabRect.height() - height) / 2);
        return new Rect(x, y, width, height);
    }

    public static void drawModernButton(
            GuiGraphicsExtractor graphics,
            Font font,
            Rect rect,
            String label,
            boolean selected,
            boolean enabled,
            double mouseX,
            double mouseY
    ) {
        drawModernButton(graphics, font, rect, rect.width(), label, selected, enabled, mouseX, mouseY);
    }

    /**
     * Variante avec une largeur de texte distincte de la largeur du fond :
     * utilisée quand un autre élément (ex. une case à cocher) est dessiné
     * par-dessus une partie du bouton, pour que le texte se centre et se
     * tronque sur la zone réellement libre au lieu de déborder dessous.
     */
    public static void drawModernButton(
            GuiGraphicsExtractor graphics,
            Font font,
            Rect rect,
            int textAreaWidth,
            String label,
            boolean selected,
            boolean enabled,
            double mouseX,
            double mouseY
    ) {
        boolean hovered = enabled && rect.contains(mouseX, mouseY);
        int background = selected
                ? MODERN_PANEL_SELECTED
                : hovered
                ? MODERN_PANEL_HOVER
                : MODERN_PANEL;
        int border = selected ? 0xFF3D9BFF : MODERN_BORDER_SOFT;
        int textColor = enabled ? COLOR_TEXT : 0xFF697583;

        graphics.fill(rect.x(), rect.y(), rect.x() + rect.width(), rect.y() + rect.height(), background);
        graphics.outline(rect.x(), rect.y(), rect.width(), rect.height(), border);
        if (rect.height() >= font.lineHeight + 2 && textAreaWidth >= 8) {
            String fitted = fitText(font, label, Math.max(0, textAreaWidth - 8));
            graphics.centeredText(font, Component.literal(fitted), rect.x() + textAreaWidth / 2, rect.y() + Math.max(2, (rect.height() - font.lineHeight) / 2), textColor);
        }
    }

    public static void drawModernToggle(
            GuiGraphicsExtractor graphics,
            Font font,
            Rect rect,
            String label,
            String helper,
            boolean value,
            boolean enabled,
            double mouseX,
            double mouseY
    ) {
        boolean hovered = enabled && rect.contains(mouseX, mouseY);
        if (hovered) {
            graphics.fill(rect.x(), rect.y(), rect.x() + rect.width(), rect.y() + rect.height(), 0x552D3E50);
        }
        int availableToggleWidth = Math.max(1, rect.width() - 4);
        int toggleWidth = Math.min(30, availableToggleWidth);
        int labelMaxWidth = Math.max(0, rect.width() - toggleWidth - 10);
        if (rect.height() >= font.lineHeight + 2) {
            graphics.text(font, Component.literal(fitText(font, label, labelMaxWidth)), rect.x() + 2, rect.y() + 3, enabled ? COLOR_TEXT : 0xFF697583, false);
        }
        if (rect.height() >= 28 && helper != null && !helper.isBlank()) {
            graphics.text(font, Component.literal(fitText(font, helper, labelMaxWidth)), rect.x() + 2, rect.y() + 14, MODERN_MUTED, false);
        }

        int toggleHeight = Math.min(14, Math.max(1, rect.height() - 4));
        int tx = rect.x() + rect.width() - toggleWidth - 2;
        int ty = rect.y() + Math.max(2, (rect.height() - toggleHeight) / 2);
        int bg = value ? MODERN_BLUE : 0xFF59636E;
        graphics.fill(tx, ty, tx + toggleWidth, ty + toggleHeight, bg);
        graphics.outline(tx, ty, toggleWidth, toggleHeight, value ? 0xFF64AFFF : 0xFF74808C);
        int knobSize = Math.max(2, toggleHeight - 4);
        int knobX = value ? tx + toggleWidth - knobSize - 2 : tx + 2;
        graphics.fill(knobX, ty + 2, knobX + knobSize, ty + 2 + knobSize, COLOR_WHITE);
    }

    public static void drawFieldLabel(
            GuiGraphicsExtractor graphics,
            Font font,
            String label,
            int x,
            int y
    ) {
        graphics.text(font, Component.literal(label), x, y, MODERN_MUTED, false);
    }



    /**
     * V114 : métriques adaptatives communes à toutes les interfaces de panneaux.
     *
     * Le GUI Scale de Minecraft réduit la résolution logique de l'écran. Plutôt
     * que de corriger chaque écran séparément, tous les éditeurs utilisent les
     * mêmes règles de réduction, les mêmes hauteurs de bandeau et la même
     * redistribution verticale des sections.
     */
    public static float adaptiveEditorScale(
            int windowWidth,
            int windowHeight,
            float designWidth,
            float designHeight
    ) {
        float raw = Math.min(windowWidth / designWidth, windowHeight / designHeight);

        // Aux GUI Scale extrêmes, on réduit un peu plus vite que le simple ratio
        // afin de préserver des marges et le pied de page.
        if (windowHeight < 430) {
            raw *= 0.80F;
        } else if (windowHeight < 520) {
            raw *= 0.90F;
        }

        return clamp(raw, 0.26F, 1.0F);
    }

    public static boolean tightForScale(float scale, int windowHeight) {
        return scale < 0.62F || windowHeight < 560;
    }

    public static boolean ultraTightForScale(float scale, int windowHeight) {
        return scale < 0.46F || windowHeight < 440;
    }

    /**
     * Dimension mise à l'échelle avec un plancher qui diminue lui aussi en mode
     * compact. Cela évite qu'une succession de contrôles à 14 px minimum finisse
     * par dépasser la hauteur disponible lorsque le GUI Scale est très élevé.
     */
    public static int scaledUi(int value, float scale) {
        int scaled = Math.max(1, Math.round(value * scale));
        if (value >= 20 && value <= 34) {
            int minimum = scale < 0.46F ? 10 : scale < 0.62F ? 12 : 14;
            return Math.max(minimum, scaled);
        }
        return scaled;
    }

    public static int adaptiveHeaderHeight(float scale, boolean hasTabs) {
        if (scale < 0.46F) {
            return hasTabs ? 32 : 30;
        }
        if (scale < 0.62F) {
            return hasTabs ? 36 : 34;
        }
        return Math.max(hasTabs ? 42 : 38, Math.round(54.0F * scale));
    }

    public static int adaptiveTabsHeight(float scale) {
        if (scale < 0.46F) {
            return 30;
        }
        if (scale < 0.62F) {
            return 34;
        }
        return Math.max(36, Math.round(40.0F * scale));
    }

    public static int adaptiveFooterHeight(float scale) {
        if (scale < 0.46F) {
            return 34;
        }
        if (scale < 0.62F) {
            return 38;
        }
        return Math.max(40, Math.round(46.0F * scale));
    }

    /**
     * Répartit exactement la hauteur disponible entre plusieurs cartes.
     * Les minima sont "souples" : s'ils ne tiennent pas, ils sont réduits
     * proportionnellement afin que la dernière section ne sorte jamais du cadre.
     */
    public static int[] fitSections(
            int availableHeight,
            int gap,
            float[] weights,
            int[] preferredMinimums
    ) {
        int count = Math.min(weights.length, preferredMinimums.length);
        int[] result = new int[count];
        if (count == 0) {
            return result;
        }

        int usable = Math.max(count, availableHeight - gap * Math.max(0, count - 1));
        float weightSum = 0.0F;
        for (int i = 0; i < count; i++) {
            weightSum += Math.max(0.01F, weights[i]);
        }

        int assigned = 0;
        for (int i = 0; i < count; i++) {
            int proportional = Math.max(1, Math.round(usable * (Math.max(0.01F, weights[i]) / weightSum)));
            result[i] = Math.max(preferredMinimums[i], proportional);
            assigned += result[i];
        }

        if (assigned > usable) {
            // Réduction uniforme des excès au-dessus d'un plancher absolu.
            int floor = Math.max(28, Math.min(46, usable / count - 2));
            int overflow = assigned - usable;
            while (overflow > 0) {
                boolean changed = false;
                for (int i = 0; i < count && overflow > 0; i++) {
                    if (result[i] > floor) {
                        result[i]--;
                        overflow--;
                        changed = true;
                    }
                }
                if (!changed) {
                    break;
                }
            }
        } else if (assigned < usable) {
            result[count - 1] += usable - assigned;
        }

        // Dernière garantie : somme exacte, sans hauteur négative.
        int sum = 0;
        for (int value : result) {
            sum += value;
        }
        int delta = usable - sum;
        result[count - 1] = Math.max(1, result[count - 1] + delta);
        return result;
    }



    /**
     * V115 : quand la résolution logique devient trop petite, réduire encore les
     * cartes finit nécessairement par faire se chevaucher le texte Minecraft,
     * dont la hauteur ne suit pas notre facteur d'échelle. On bascule donc dans
     * un mode "pages" : l'aperçu reste visible et une seule famille de réglages
     * occupe la colonne de droite à la fois.
     */
    public static boolean pagedCompactMode(float scale, int windowHeight) {
        return scale < 0.62F || windowHeight < 580;
    }

    public static Rect[] pageTabRects(Rect area, int count, int height, int gap) {
        int safeCount = Math.max(1, count);
        int usable = area.width() - gap * (safeCount - 1);
        int width = Math.max(24, usable / safeCount);
        Rect[] result = new Rect[safeCount];
        for (int i = 0; i < safeCount; i++) {
            int x = area.x() + i * (width + gap);
            int w = i == safeCount - 1 ? area.x() + area.width() - x : width;
            result[i] = new Rect(x, area.y(), Math.max(1, w), height);
        }
        return result;
    }

    public static void drawPageTabs(
            GuiGraphicsExtractor graphics,
            Font font,
            Rect[] rects,
            String[] labels,
            int selectedIndex,
            double mouseX,
            double mouseY
    ) {
        if (rects == null || labels == null) {
            return;
        }
        int count = Math.min(rects.length, labels.length);
        for (int i = 0; i < count; i++) {
            drawModernButton(
                    graphics,
                    font,
                    rects[i],
                    labels[i],
                    i == selectedIndex,
                    true,
                    mouseX,
                    mouseY
            );
        }
    }

    public static boolean compactForScale(float scale) {
        return scale < 0.76F;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
