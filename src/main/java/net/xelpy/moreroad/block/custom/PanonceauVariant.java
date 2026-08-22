package net.xelpy.moreroad.block.custom;

import java.util.Arrays;
import java.util.List;

/**
 * Variantes de panonceaux M1 à M12 prises en charge par le bloc générique.
 *
 * Le système est volontairement piloté par les données de cette enum :
 * ajouter une nouvelle variante fixe revient surtout à ajouter sa texture
 * et une entrée ici, sans créer un nouveau bloc Java.
 */
public enum PanonceauVariant {

    M1("m1", "M1", "M1 — Distance", 3.44F, "m1_base.png", RenderMode.CENTER_VALUE, "150 m"),
    M1A("m1a", "M1", "M1a — Repère / distance", 3.42F, "m1a.png", RenderMode.FIXED, ""),

    M2("m2", "M2", "M2 — Étendue", 3.55F, "m2_base.png", RenderMode.CENTER_VALUE_WITH_ARROWS, "500 m"),

    M3A1("m3a1", "M3", "M3a1 — Voie à droite", 1.00F, "m3a1.png", RenderMode.FIXED, ""),
    M3A2("m3a2", "M3", "M3a2 — Voie à gauche", 1.00F, "m3a2.png", RenderMode.FIXED, ""),
    M3B1("m3b1", "M3", "M3b1 — Direction droite", 2.51F, "m3b1.png", RenderMode.FIXED, ""),
    M3B2("m3b2", "M3", "M3b2 — Direction gauche", 2.47F, "m3b2.png", RenderMode.FIXED, ""),
    M3B3("m3b3", "M3", "M3b3 — Droite + distance", 3.42F, "m3b3_base.png", RenderMode.M3B_RIGHT_VALUE, "50 m"),
    M3B4("m3b4", "M3", "M3b4 — Gauche + distance", 3.44F, "m3b4_base.png", RenderMode.M3B_LEFT_VALUE, "50 m"),
    M3D("m3d", "M3", "M3d — Voie concernée", 1.42F, "m3d.png", RenderMode.FIXED, ""),

    M4A("m4a", "M4", "M4a — Véhicules légers", 3.40F, "m4a.png", RenderMode.FIXED, ""),
    M4B("m4b", "M4", "M4b — Transport en commun", 3.46F, "m4b.png", RenderMode.FIXED, ""),
    M4C("m4c", "M4", "M4c — Deux-roues motorisés", 3.45F, "m4c.png", RenderMode.FIXED, ""),
    M4D1("m4d1", "M4", "M4d1 — Cycles", 3.43F, "m4d1.png", RenderMode.FIXED, ""),
    M4D2("m4d2", "M4", "M4d2 — Cyclomoteurs", 2.47F, "m4d2.png", RenderMode.FIXED, ""),
    M4E("m4e", "M4", "M4e — Remorques", 3.44F, "m4e.png", RenderMode.FIXED, ""),
    M4F("m4f", "M4", "M4f — Masse", 2.47F, "m4f_base.png", RenderMode.CENTER_VALUE, "5,5 t"),
    M4G("m4g", "M4", "M4g — Marchandises", 1.97F, "m4g.png", RenderMode.FIXED, ""),
    M4H("m4h", "M4", "M4h — Marchandises + masse", 0.91F, "m4h_base.png", RenderMode.LOWER_VALUE, "4,5 t"),
    M4I("m4i", "M4", "M4i — Véhicules agricoles", 1.42F, "m4i.png", RenderMode.FIXED, ""),
    M4J("m4j", "M4", "M4j — Chaînes à neige", 1.43F, "m4j.png", RenderMode.FIXED, ""),
    M4K("m4k", "M4", "M4k — Matières explosives", 1.43F, "m4k.png", RenderMode.FIXED, ""),
    M4L("m4l", "M4", "M4l — Matières polluantes", 1.42F, "m4l.png", RenderMode.FIXED, ""),
    M4M("m4m", "M4", "M4m — Matières dangereuses", 1.42F, "m4m.png", RenderMode.FIXED, ""),
    M4N("m4n", "M4", "M4n — Handicap", 1.71F, "m4n.png", RenderMode.FIXED, ""),
    M4P("m4p", "M4", "M4p — Piétons", 1.43F, "m4p.png", RenderMode.FIXED, ""),
    M4Q("m4q", "M4", "M4q — Véhicules longs", 1.43F, "m4q_base.png", RenderMode.LOWER_VALUE, "10 m"),
    M4R("m4r", "M4", "M4r — Charge par essieu", 1.42F, "m4r_base.png", RenderMode.UPPER_VALUE, "2 t"),
    M4S("m4s", "M4", "M4s — Traction animale", 2.48F, "m4s.png", RenderMode.FIXED, ""),
    M4T("m4t", "M4", "M4t — Charrettes à bras", 2.49F, "m4t.png", RenderMode.FIXED, ""),
    M4U("m4u", "M4", "M4u — Largeur", 2.48F, "m4u_base.png", RenderMode.CENTER_VALUE, "2,3 m"),
    M4V("m4v", "M4", "M4v — Hauteur", 1.42F, "m4v_base.png", RenderMode.CENTER_VALUE, "4,5 m"),
    M4W("m4w", "M4", "M4w — Remorque", 2.47F, "m4w.png", RenderMode.FIXED, ""),
    M4X("m4x", "M4", "M4x — Caravane / remorque", 3.44F, "m4x.png", RenderMode.FIXED, ""),
    M4Y("m4y", "M4", "M4y — Cavaliers", 2.00F, "m4y.png", RenderMode.FIXED, ""),
    M4Z("m4z", "M4", "M4z — Catégorie tunnel", 1.99F, "m4z_base.png", RenderMode.CENTER_VALUE, "D"),

    M5A("m5a", "M5", "M5a — STOP + distance", 1.00F, "m5a_base.png", RenderMode.M5A, "150 m"),
    M5B("m5b", "M5", "M5b — STOP + distance horizontal", 3.27F, "m5b_base.png", RenderMode.M5B, "150 m"),

    M6A("m6a", "M6", "M6a", 3.2473F, "m6a.png", RenderMode.FIXED, ""),
    M6B_EX("m6b_ex", "M6", "M6b — exemple", 2.4673F, "m6b_ex.png", RenderMode.FIXED, ""),
    M6C_EX1("m6c_ex1", "M6", "M6c — exemple 1", 1.4139F, "m6c_ex1.png", RenderMode.FIXED, ""),
    M6C_EX2("m6c_ex2", "M6", "M6c — exemple 2", 3.4154F, "m6c_ex2.png", RenderMode.FIXED, ""),
    M6D_EX1("m6d_ex1", "M6", "M6d — exemple 1", 2.4704F, "m6d_ex1.png", RenderMode.FIXED, ""),
    M6D_EX2("m6d_ex2", "M6", "M6d — exemple 2", 2.4704F, "m6d_ex2.png", RenderMode.FIXED, ""),
    M6E_EX("m6e_ex", "M6", "M6e — exemple", 3.4534F, "m6e_ex.png", RenderMode.FIXED, ""),
    M6F_EX1("m6f_ex1", "M6", "M6f — exemple 1", 2.4491F, "m6f_ex1.png", RenderMode.FIXED, ""),
    M6F_EX2("m6f_ex2", "M6", "M6f — exemple 2", 2.4679F, "m6f_ex2.png", RenderMode.FIXED, ""),
    M6F_EX3("m6f_ex3", "M6", "M6f — exemple 3", 2.4641F, "m6f_ex3.png", RenderMode.FIXED, ""),
    M6H("m6h", "M6", "M6h", 1.9291F, "m6h.png", RenderMode.FIXED, ""),
    M6I_EX1("m6i_ex1", "M6", "M6i — exemple 1", 1.4251F, "m6i_ex1.png", RenderMode.FIXED, ""),
    M6I_EX2("m6i_ex2", "M6", "M6i — exemple 2", 1.4251F, "m6i_ex2.png", RenderMode.FIXED, ""),
    M6J("m6j", "M6", "M6j", 3.2689F, "m6j.png", RenderMode.FIXED, ""),
    M7_EX1("m7_ex1", "M7", "M7 — exemple 1", 0.9976F, "m7_ex1.png", RenderMode.FIXED, ""),
    M7_EX2("m7_ex2", "M7", "M7 — exemple 2", 0.9992F, "m7_ex2.png", RenderMode.FIXED, ""),
    M7_EX3("m7_ex3", "M7", "M7 — exemple 3", 1.0006F, "m7_ex3.png", RenderMode.FIXED, ""),
    M8A("m8a", "M8", "M8a", 0.4025F, "m8a.png", RenderMode.FIXED, ""),
    M8A_BIS("m8a_bis", "M8", "M8a — variante bis", 0.4015F, "m8a_bis.png", RenderMode.FIXED, ""),
    M8AD("m8ad", "M8", "M8ad", 0.6381F, "m8ad.png", RenderMode.FIXED, ""),
    M8B("m8b", "M8", "M8b", 0.4047F, "m8b.png", RenderMode.FIXED, ""),
    M8C("m8c", "M8", "M8c", 0.4043F, "m8c.png", RenderMode.FIXED, ""),
    M8C_BIS("m8c_bis", "M8", "M8c — variante bis", 0.2862F, "m8c_bis.png", RenderMode.FIXED, ""),
    M8D("m8d", "M8", "M8d", 2.4979F, "m8d.png", RenderMode.FIXED, ""),
    M8D_BIS_HYPHEN("m8d-bis", "M8", "M8d — variante bis", 2.2579F, "m8d-bis.png", RenderMode.FIXED, ""),
    M8D_BIS("m8d_bis", "M8", "M8d — variante bis", 2.4557F, "m8d_bis.png", RenderMode.FIXED, ""),
    M8E("m8e", "M8", "M8e", 2.4979F, "m8e.png", RenderMode.FIXED, ""),
    M8E_BIS("m8e_bis", "M8", "M8e — variante bis", 2.4979F, "m8e_bis.png", RenderMode.FIXED, ""),
    M8F("m8f", "M8", "M8f", 3.4941F, "m8f.png", RenderMode.FIXED, ""),
    M8F_BIS("m8f_bis", "M8", "M8f — variante bis", 3.4941F, "m8f_bis.png", RenderMode.FIXED, ""),
    M9A("m9a", "M9", "M9A", 1.7996F, "m9a.png", RenderMode.FIXED, ""),
    M9B("m9b", "M9", "M9B", 3.5964F, "m9b.png", RenderMode.FIXED, ""),
    M9C("m9c", "M9", "M9C", 2.4936F, "m9c.png", RenderMode.FIXED, ""),
    M9D("m9d", "M9", "M9D", 3.4936F, "m9d.png", RenderMode.FIXED, ""),
    M9E("m9e", "M9", "M9e", 1.8389F, "m9e.png", RenderMode.FIXED, ""),
    M9F("m9f", "M9", "M9f", 1.8448F, "m9f.png", RenderMode.FIXED, ""),
    M9G("m9g", "M9", "M9g", 3.5424F, "m9g.png", RenderMode.FIXED, ""),
    M9J1("m9j1", "M9", "M9j1", 1.7986F, "m9j1.png", RenderMode.FIXED, ""),
    M9J2("m9j2", "M9", "M9j2", 1.7965F, "m9j2.png", RenderMode.FIXED, ""),
    M9V1("m9v1", "M9", "M9v1", 1.9946F, "m9v1.png", RenderMode.FIXED, ""),
    M9V2("m9v2", "M9", "M9v2", 1.9946F, "m9v2.png", RenderMode.FIXED, ""),
    M9Z_EX1("m9z_ex1", "M9", "M9z — exemple 1", 1.7996F, "m9z_ex1.png", RenderMode.FIXED, ""),
    M9Z_EX2("m9z_ex2", "M9", "M9z — exemple 2", 1.7133F, "m9z_ex2.png", RenderMode.FIXED, ""),
    M9Z_EX3("m9z_ex3", "M9", "M9z — exemple 3", 1.7996F, "m9z_ex3.png", RenderMode.FIXED, ""),
    M9Z_EX4("m9z_ex4", "M9", "M9z — exemple 4", 3.5964F, "m9z_ex4.png", RenderMode.FIXED, ""),
    M9Z_EX5("m9z_ex5", "M9", "M9z — exemple 5", 1.7996F, "m9z_ex5.png", RenderMode.FIXED, ""),
    M9Z_EX6("m9z_ex6", "M9", "M9z — exemple 6", 3.5964F, "m9z_ex6.png", RenderMode.FIXED, ""),
    M9Z_EX7("m9z_ex7", "M9", "M9z — exemple 7", 1.7996F, "m9z_ex7.png", RenderMode.FIXED, ""),
    M9Z_EX8("m9z_ex8", "M9", "M9z — exemple 8", 1.7996F, "m9z_ex8.png", RenderMode.FIXED, ""),
    M9Z_EX9("m9z_ex9", "M9", "M9z — exemple 9", 3.5964F, "m9z_ex9.png", RenderMode.FIXED, ""),
    M9Z_EX10("m9z_ex10", "M9", "M9z — exemple 10", 1.7996F, "m9z_ex10.png", RenderMode.FIXED, ""),
    M9Z_EX11("m9z_ex11", "M9", "M9z — exemple 11", 3.4941F, "m9z_ex11.png", RenderMode.FIXED, ""),
    M9Z_EX12("m9z_ex12", "M9", "M9z — exemple 12", 3.4941F, "m9z_ex12.png", RenderMode.FIXED, ""),
    M9Z_EX13("m9z_ex13", "M9", "M9z — exemple 13", 3.4941F, "m9z_ex13.png", RenderMode.FIXED, ""),
    M9Z_EX14("m9z_ex14", "M9", "M9z — exemple 14", 3.4941F, "m9z_ex14.png", RenderMode.FIXED, ""),
    M9Z_EX15("m9z_ex15", "M9", "M9z — exemple 15", 3.4941F, "m9z_ex15.png", RenderMode.FIXED, ""),
    M9Z_EX16("m9z_ex16", "M9", "M9z — exemple 16", 3.4941F, "m9z_ex16.png", RenderMode.FIXED, ""),
    M9ZEX1CDR("m9zex1cdr", "M9", "M9z — exemple CDR", 3.4498F, "m9zex1cdr.png", RenderMode.FIXED, ""),
    M9ZG2("m9zg2", "M9", "M9zG2", 1.7853F, "m9zg2.png", RenderMode.FIXED, ""),
    M10A("m10a", "M10", "M10A", 3.4815F, "m10a.png", RenderMode.FIXED, ""),
    M10A_EX1("m10a_ex1", "M10", "M10a — exemple 1", 3.4385F, "m10a_ex1.png", RenderMode.FIXED, ""),
    M10A_EX2("m10a_ex2", "M10", "M10a — exemple 2", 3.4458F, "m10a_ex2.png", RenderMode.FIXED, ""),
    M10B("m10b", "M10", "M10b", 1.9832F, "m10b.png", RenderMode.FIXED, ""),
    M10BL("m10bl", "M10", "M10BL", 1.9974F, "m10bl.png", RenderMode.FIXED, ""),
    M10C1("m10c1", "M10", "M10c1", 3.2764F, "m10c1.png", RenderMode.FIXED, ""),
    M10C2("m10c2", "M10", "M10c2", 3.3020F, "m10c2.png", RenderMode.FIXED, ""),
    M10C3("m10c3", "M10", "M10c3", 3.3054F, "m10c3.png", RenderMode.FIXED, ""),
    M10Z("m10z", "M10", "M10z", 3.4589F, "m10z.png", RenderMode.FIXED, ""),
    M11A("m11a", "M11", "M11a", 1.9922F, "m11a.png", RenderMode.FIXED, ""),
    M11B("m11b", "M11", "M11b", 0.9991F, "m11b.png", RenderMode.FIXED, ""),
    M11B_EX1("m11b_ex1", "M11", "M11b — exemple 1", 1.6639F, "m11b_ex1.png", RenderMode.FIXED, ""),
    M11B_EX1_DC("m11b_ex1_dc", "M11", "M11b — exemple 1 DC", 3.5659F, "m11b_ex1_dc.png", RenderMode.FIXED, ""),
    M11B_EX2("m11b_ex2", "M11", "M11b — exemple 2", 1.6636F, "m11b_ex2.png", RenderMode.FIXED, ""),
    M11B_EX2_DC("m11b_ex2_dc", "M11", "M11b — exemple 2 DC", 3.5686F, "m11b_ex2_dc.png", RenderMode.FIXED, ""),
    M11C2("m11c2", "M11", "M11c2", 2.4679F, "m11c2.png", RenderMode.FIXED, ""),
    M12A("m12a", "M12", "M12a", 1.1429F, "m12a.png", RenderMode.FIXED, ""),
    M12A_C("m12a_c", "M12", "M12a C", 1.0000F, "m12a_c.png", RenderMode.FIXED, ""),
    M12B("m12b", "M12", "M12b", 1.1429F, "m12b.png", RenderMode.FIXED, ""),
    M12B_C("m12b_c", "M12", "M12b C", 1.0000F, "m12b_c.png", RenderMode.FIXED, ""),

    CUSTOM_TEXT("texte", "TXT", "Texte personnalisé", 3.40F, "custom_text_base.png", RenderMode.CUSTOM_TEXT, "TEXTE");

    public enum RenderMode {
        FIXED,
        CENTER_VALUE,
        CENTER_VALUE_WITH_ARROWS,
        M3B_RIGHT_VALUE,
        M3B_LEFT_VALUE,
        LOWER_VALUE,
        UPPER_VALUE,
        M5A,
        M5B,
        CUSTOM_TEXT
    }

    private final String serializedName;
    private final String family;
    private final String displayName;
    private final float aspectRatio;
    private final String textureFile;
    private final RenderMode renderMode;
    private final String defaultValue;

    PanonceauVariant(
            String serializedName,
            String family,
            String displayName,
            float aspectRatio,
            String textureFile,
            RenderMode renderMode,
            String defaultValue
    ) {
        this.serializedName = serializedName;
        this.family = family;
        this.displayName = displayName;
        this.aspectRatio = aspectRatio;
        this.textureFile = textureFile;
        this.renderMode = renderMode;
        this.defaultValue = defaultValue;
    }

    public String getSerializedName() {
        return this.serializedName;
    }

    public String family() {
        return this.family;
    }

    public String displayName() {
        return this.displayName;
    }

    public float aspectRatio() {
        return this.aspectRatio;
    }

    public String textureFile() {
        return this.textureFile;
    }

    public RenderMode renderMode() {
        return this.renderMode;
    }

    public String defaultValue() {
        return this.defaultValue;
    }

    public boolean isEditable() {
        return this.renderMode != RenderMode.FIXED;
    }

    public boolean isCustomText() {
        return this.renderMode == RenderMode.CUSTOM_TEXT;
    }

    /**
     * Les M12a/M12b sans fond carré utilisent une plaque triangulaire réelle.
     * Les variantes M12a C / M12b C gardent leur support carré visible dans le SVG.
     */
    public boolean isTriangular() {
        return this == M12A || this == M12B;
    }

    public static PanonceauVariant fromSerializedName(String value) {
        if (value != null) {
            for (PanonceauVariant variant : values()) {
                if (variant.serializedName.equalsIgnoreCase(value)) {
                    return variant;
                }
            }
        }
        return M1;
    }

    public static List<PanonceauVariant> forFamily(String family) {
        String normalized = family == null ? "M1" : family;
        return Arrays.stream(values())
                .filter(variant -> variant.family.equalsIgnoreCase(normalized))
                .toList();
    }
}
