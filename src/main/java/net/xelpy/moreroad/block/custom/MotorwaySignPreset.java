package net.xelpy.moreroad.block.custom;

import java.util.Arrays;

/**
 * Catalogue des SVG fournis par l'utilisateur.
 *
 * Les références qui partageaient la même construction utilisent les mêmes
 * primitives de rendu. D62D, DA41D et DA41E étaient chacun composés de deux
 * panneaux physiques dans un seul SVG : ils sont donc exposés en deux
 * préréglages indépendants.
 */
public enum MotorwaySignPreset {

    FREEFORM("freeform", "Panneau libre", MotorwaySignGraphic.NONE, MotorwaySignSupport.POLE),

    D31B_EX1("d31b_ex1", "D31b — exemple 1", MotorwaySignGraphic.DIAGONAL_RIGHT, MotorwaySignSupport.POLE,
            route("Numéro de route", "D 922", MotorwaySignColor.YELLOW),
            line("Destination 1", "ST SAUVEUR", MotorwaySignColor.WHITE, 0),
            line("Destination 2", "PRÉCY", MotorwaySignColor.WHITE, 0)),
    D31B_EX2("d31b_ex2", "D31b — exemple 2", MotorwaySignGraphic.DIAGONAL_RIGHT, MotorwaySignSupport.POLE,
            route("Numéro de route", "A 13", MotorwaySignColor.RED),
            line("Destination 1", "ROUEN", MotorwaySignColor.BLUE, 0),
            line("Destination 2", "ST QUENTIN EN YNES", MotorwaySignColor.BLUE, 0),
            line("Destination 3", "VERSAILLES", MotorwaySignColor.BLUE, 0)),
    /*
     * Signalé : le registre vert (destination principale) doit pouvoir
     * accueillir jusqu'à 4 villes (comme sur les vrais panneaux, ex.
     * PARIS/-PTE DE LA VILLETTE/LA COURNEUVE/DRANCY), et le registre
     * "destination locale" jusqu'à 3 (il en compte 2 par défaut, dessin
     * d'origine). Le panneau s'agrandit ou se réduit d'autant. Chaque ville
     * optionnelle est placée juste après la première du même registre, pour
     * apparaître au bon endroit dans l'éditeur.
     */
    D31D("d31d", "D31d", MotorwaySignGraphic.EXIT, MotorwaySignSupport.POLE,
            route("Numéro de sortie", "19", MotorwaySignColor.WHITE),
            line("Destination verte", "CLERMONT-FD", MotorwaySignColor.GREEN, 0),
            line("Destination verte 2", "", MotorwaySignColor.GREEN, 0),
            line("Destination verte 3", "", MotorwaySignColor.GREEN, 0),
            line("Destination verte 4", "", MotorwaySignColor.GREEN, 0),
            line("Destination locale 1", "CHAMALIÈRES", MotorwaySignColor.WHITE, 1),
            line("Destination locale 2", "ROYAT", MotorwaySignColor.WHITE, 1),
            line("Destination locale 3", "", MotorwaySignColor.WHITE, 1)),
    /*
     * Signalé : comme le D31d, les deux registres (vert et "destination
     * locale") doivent tous les deux pouvoir accueillir de 1 à 4 villes —
     * y compris le premier, contrairement au D31d où il compte 1 ville de
     * base. Le panneau s'agrandit ou se réduit d'autant à chacun.
     */
    D31E("d31e", "D31e", MotorwaySignGraphic.DIAGONAL_RIGHT, MotorwaySignSupport.POLE,
            route("Numéro de route", "N 144", MotorwaySignColor.RED),
            line("Destination verte", "MONTLUÇON", MotorwaySignColor.GREEN, 0),
            line("Destination verte 2", "", MotorwaySignColor.GREEN, 0),
            line("Destination verte 3", "", MotorwaySignColor.GREEN, 0),
            line("Destination verte 4", "", MotorwaySignColor.GREEN, 0),
            line("Destination locale", "ST ÉLOI LES MINES", MotorwaySignColor.WHITE, 1),
            line("Destination locale 2", "", MotorwaySignColor.WHITE, 1),
            line("Destination locale 3", "", MotorwaySignColor.WHITE, 1),
            line("Destination locale 4", "", MotorwaySignColor.WHITE, 1)),
    /*
     * D32a : un seul modèle pour les deux variantes réglementaires de fond.
     * La typographie est toujours L4 (italique), comme sur les panneaux
     * d'aire réels ; la couleur du panneau est choisie dans l'éditeur
     * (blanc ou bleu uniquement).
     */
    D32A("d32a", "D32a", MotorwaySignGraphic.DIAGONAL_RIGHT, MotorwaySignSupport.POLE,
            italic("Ligne 1", "AIRE DE", MotorwaySignColor.WHITE, 0),
            italic("Ligne 2", "LIMOURS-JANVRY", MotorwaySignColor.WHITE, 0)),

    D41A("d41a", "D41a", MotorwaySignGraphic.EXIT, MotorwaySignSupport.POLE,
            route("Numéro de sortie", "4", MotorwaySignColor.WHITE),
            line("Destination 1", "ROCHEFORT", MotorwaySignColor.GREEN, 0),
            line("Destination 2", "LA ROCHELLE", MotorwaySignColor.GREEN, 0),
            line("Destination locale", "SURGÈRES", MotorwaySignColor.WHITE, 1),
            distance("Distance", "1000 m")),
    D41B("d41b", "D41b", MotorwaySignGraphic.DIAGONAL_RIGHT, MotorwaySignSupport.POLE,
            route("Numéro de route", "D 941", MotorwaySignColor.YELLOW),
            line("Destination verte", "GUÉRET", MotorwaySignColor.GREEN, 0),
            line("Destination locale", "AUBUSSON", MotorwaySignColor.WHITE, 1),
            distance("Distance", "300 m")),
    D41C("d41c", "D41c", MotorwaySignGraphic.DIAGONAL_RIGHT, MotorwaySignSupport.POLE,
            route("Numéro de route", "A 67", MotorwaySignColor.RED),
            line("Destination 1", "CLERMONT-FD", MotorwaySignColor.BLUE, 0),
            line("Destination 2", "ST ÉTIENNE", MotorwaySignColor.BLUE, 0),
            line("Destination 3", "NEVERS", MotorwaySignColor.BLUE, 1),
            distance("Distance", "1000 m")),

    /*
     * D44 : présignalisation de village étape (registre sortie + distance,
     * puis nom du village). Dessin exact (pastille SE2b, idéogramme et
     * mention "village étape") géré par ExactMappedArtwork dans le
     * renderer : voir D44_ARTWORK. La mention elle-même est réglementairement
     * fixe et n'est donc pas un champ éditable.
     */
    D44("d44", "D44", MotorwaySignGraphic.NONE, MotorwaySignSupport.POLE,
            /*
             * Rôle ROUTE + libellé contenant "sortie" : le renderer route ce
             * champ vers drawExitNumber(), qui dessine la pastille SE2b
             * partagée (EXIT_SYMBOL_TEXTURE, flèche + numéro) déjà utilisée
             * par D41A/D63C. D44_ARTWORK ne cuit donc plus sa propre pastille.
             */
            route("Numéro de sortie", "20", MotorwaySignColor.WHITE),
            distance("Distance", "500 m"),
            line("Nom du village étape", "ÉGUZON", MotorwaySignColor.WHITE, 0)),
    D45("d45", "D45", MotorwaySignGraphic.SERVICES, MotorwaySignSupport.POLE,
            line("Nom de l'aire", "AIRE DE LIMOURS", MotorwaySignColor.WHITE, 0),
            info("Services", "SERVICES", MotorwaySignColor.WHITE, 0),
            distance("Distance", "2000 m")),
    D45_DC("d45_dc", "D45 — caractères L4", MotorwaySignGraphic.SERVICES, MotorwaySignSupport.POLE,
            italic("Nom de l'aire", "AIRE DE LIMOURS", MotorwaySignColor.WHITE, 0),
            italic("Services", "SERVICES", MotorwaySignColor.WHITE, 0),
            distance("Distance", "2000 m")),
    D46A("d46a", "D46a", MotorwaySignGraphic.SERVICES, MotorwaySignSupport.POLE,
            line("Nom de l'aire", "AIRE DE LIMOURS", MotorwaySignColor.WHITE, 0),
            distance("Distance", "1000 m")),
    D46B("d46b", "D46b", MotorwaySignGraphic.SERVICES, MotorwaySignSupport.POLE,
            line("Nom de l'aire", "AIRE DE LIMOURS", MotorwaySignColor.BLUE, 0),
            distance("Distance", "1000 m")),
    D47A("d47a", "D47a", MotorwaySignGraphic.SERVICES, MotorwaySignSupport.POLE,
            line("Information", "PROCHAINE STATION", MotorwaySignColor.BLUE, 0),
            distance("Distance", "40 km")),
    D47B("d47b", "D47b", MotorwaySignGraphic.SERVICES, MotorwaySignSupport.POLE,
            line("Information", "PROCHAIN RESTAURANT", MotorwaySignColor.BLUE, 0),
            distance("Distance", "30 km")),
    D47C("d47c", "D47c", MotorwaySignGraphic.SERVICES, MotorwaySignSupport.POLE,
            line("Information", "PROCHAIN HÔTEL", MotorwaySignColor.BLUE, 0),
            distance("Distance", "25 km")),

    D51C("d51c", "D51c", MotorwaySignGraphic.SCHEMATIC_RIGHT, MotorwaySignSupport.POLE,
            line("Destination principale", "PARIS", MotorwaySignColor.WHITE, 0),
            line("Destination sortie", "ÉVRY", MotorwaySignColor.WHITE, 0),
            distance("Distance", "1500 m")),
    D51CR("d51cr", "D51cr", MotorwaySignGraphic.SCHEMATIC_RIGHT, MotorwaySignSupport.POLE,
            line("Destination principale", "PARIS", MotorwaySignColor.WHITE, 0),
            line("Destination sortie", "ÉVRY", MotorwaySignColor.WHITE, 0),
            info("Service", "AIRE", MotorwaySignColor.BLUE, 1),
            distance("Distance", "1500 m")),
    D51CR_DC("d51cr_dc", "D51cr — caractères L4", MotorwaySignGraphic.SCHEMATIC_RIGHT, MotorwaySignSupport.POLE,
            italic("Destination principale", "PARIS", MotorwaySignColor.WHITE, 0),
            italic("Destination sortie", "ÉVRY", MotorwaySignColor.WHITE, 0),
            italic("Service", "AIRE", MotorwaySignColor.BLUE, 1),
            distance("Distance", "1500 m")),
    D51D("d51d", "D51d", MotorwaySignGraphic.SCHEMATIC_LEFT, MotorwaySignSupport.POLE,
            line("Destination principale", "PARIS", MotorwaySignColor.WHITE, 0),
            line("Destination sortie", "ÉVRY", MotorwaySignColor.WHITE, 0),
            distance("Distance", "1500 m")),
    D51DR("d51dr", "D51dr", MotorwaySignGraphic.SCHEMATIC_LEFT, MotorwaySignSupport.POLE,
            line("Destination principale", "PARIS", MotorwaySignColor.WHITE, 0),
            line("Destination sortie", "ÉVRY", MotorwaySignColor.WHITE, 0),
            info("Service", "AIRE", MotorwaySignColor.BLUE, 1),
            distance("Distance", "1500 m")),
    D52A("d52a", "D52a", MotorwaySignGraphic.SCHEMATIC_RIGHT, MotorwaySignSupport.POLE,
            route("Route 1", "N 104", MotorwaySignColor.WHITE),
            line("Destination 1", "ÉVRY", MotorwaySignColor.BLUE, 0),
            line("Destination 2", "LYON", MotorwaySignColor.BLUE, 0),
            distance("Distance", "1500 m")),
    D52B("d52b", "D52b", MotorwaySignGraphic.SCHEMATIC_LEFT, MotorwaySignSupport.POLE,
            route("Route 1", "A 5b", MotorwaySignColor.RED),
            line("Destination 1", "PARIS", MotorwaySignColor.BLUE, 0),
            line("Destination 2", "LYON", MotorwaySignColor.BLUE, 0),
            distance("Distance", "1000 m")),
    D52C("d52c", "D52c", MotorwaySignGraphic.SCHEMATIC_RIGHT, MotorwaySignSupport.POLE,
            route("Route 1", "A 6", MotorwaySignColor.RED),
            route("Route 2", "N 104", MotorwaySignColor.WHITE),
            line("Destination 1", "PARIS", MotorwaySignColor.BLUE, 0),
            line("Destination 2", "LYON", MotorwaySignColor.BLUE, 0),
            distance("Distance", "1000 m")),

    D61B("d61b", "D61b", MotorwaySignGraphic.NONE, MotorwaySignSupport.POLE,
            route("Route", "A 10", MotorwaySignColor.RED),
            line("Destination 1", "BORDEAUX", MotorwaySignColor.BLUE, 0),
            distanceInPanel("Distance 1", "165", MotorwaySignColor.BLUE, 0),
            line("Destination 2", "SAINTES", MotorwaySignColor.BLUE, 1),
            distanceInPanel("Distance 2", "55", MotorwaySignColor.BLUE, 1)),
    D62A("d62a", "D62a", MotorwaySignGraphic.NONE, MotorwaySignSupport.POLE,
            route("Route", "N 7", MotorwaySignColor.RED),
            line("Destination 1", "ST ÉTIENNE", MotorwaySignColor.GREEN, 0),
            line("Destination 2", "VICHY", MotorwaySignColor.GREEN, 0),
            line("Destination locale", "ST POURÇAIN", MotorwaySignColor.WHITE, 1)),
    D62B("d62b", "D62b", MotorwaySignGraphic.NONE, MotorwaySignSupport.POLE,
            route("Route 1", "A 75", MotorwaySignColor.RED),
            route("Route 2", "E 11", MotorwaySignColor.GREEN),
            line("Destination 1", "MONTPELLIER", MotorwaySignColor.BLUE, 0),
            line("Destination 2", "MENDE", MotorwaySignColor.BLUE, 1)),
    D62C("d62c", "D62c", MotorwaySignGraphic.DOWN_DOUBLE, MotorwaySignSupport.POLE,
            route("Route 1", "A 75", MotorwaySignColor.RED),
            route("Route 2", "E 11", MotorwaySignColor.GREEN),
            line("Destination supérieure", "MONTPELLIER", MotorwaySignColor.BLUE, 0),
            line("Destination inférieure 1", "AURILLAC", MotorwaySignColor.BLUE, 1),
            line("Destination inférieure 2", "LE PUY", MotorwaySignColor.BLUE, 1)),
    D62D_TOP("d62d_top", "D62d — panneau supérieur", MotorwaySignGraphic.DOWN_DOUBLE, MotorwaySignSupport.POLE,
            route("Route 1", "A 75", MotorwaySignColor.RED),
            route("Route 2", "E 11", MotorwaySignColor.GREEN),
            line("Destination 1", "ST ÉTIENNE", MotorwaySignColor.BLUE, 0),
            line("Destination 2", "ROANNE", MotorwaySignColor.BLUE, 0),
            line("Destination 3", "MOULINS", MotorwaySignColor.BLUE, 1)),
    D62D_BOTTOM("d62d_bottom", "D62d — panneau inférieur", MotorwaySignGraphic.DOWN_DOUBLE, MotorwaySignSupport.POLE,
            route("Route", "N 89", MotorwaySignColor.RED),
            line("Destination", "TULLE", MotorwaySignColor.BLUE, 0)),
    D63C("d63c", "D63c", MotorwaySignGraphic.EXIT, MotorwaySignSupport.POLE,
            route("Numéro de sortie", "19", MotorwaySignColor.WHITE),
            info("Numéro", "25", MotorwaySignColor.WHITE, 0),
            line("Destination verte", "MONTLUÇON", MotorwaySignColor.GREEN, 1),
            line("Destination locale", "ST ÉLOI LES MINES", MotorwaySignColor.WHITE, 2)),
    D63D("d63d", "D63d", MotorwaySignGraphic.NONE, MotorwaySignSupport.POLE,
            route("Route", "A 75", MotorwaySignColor.RED),
            info("Sortie", "21", MotorwaySignColor.BLUE, 0),
            line("Destination 1", "MONTPELLIER", MotorwaySignColor.BLUE, 1),
            line("Destination 2", "AURILLAC", MotorwaySignColor.BLUE, 2),
            line("Destination 3", "LE PUY", MotorwaySignColor.BLUE, 2)),
    D64("d64", "D64", MotorwaySignGraphic.JUNCTION, MotorwaySignSupport.POLE,
            route("Route gauche", "A 4", MotorwaySignColor.RED),
            route("Route droite", "A 9", MotorwaySignColor.RED),
            distance("Distance", "14 km", MotorwaySignColor.BLUE)),

    D71("d71", "D71", MotorwaySignGraphic.NONE, MotorwaySignSupport.POLE,
            italic("Information", "prochaine sortie", MotorwaySignColor.WHITE, 0),
            line("Destination", "COURPIÈRE", MotorwaySignColor.WHITE, 0),
            italic("Distance", "900 mètres", MotorwaySignColor.WHITE, 0)),
    D72("d72", "D72", MotorwaySignGraphic.EXIT_LIST, MotorwaySignSupport.POLE,
            italic("Introduction", "accès", MotorwaySignColor.WHITE, 0),
            line("Destination", "CLERMONT-FERRAND", MotorwaySignColor.WHITE, 0),
            route("Sortie 1", "19", MotorwaySignColor.WHITE),
            route("Sortie 2", "9", MotorwaySignColor.WHITE),
            route("Sortie 3", "20", MotorwaySignColor.WHITE),
            route("Sortie 4", "21", MotorwaySignColor.WHITE)),
    D73("d73", "D73", MotorwaySignGraphic.EXIT, MotorwaySignSupport.POLE,
            route("Numéro de sortie", "19", MotorwaySignColor.WHITE),
            distance("Distance", "2000 m")),
    D74A("d74a", "D74a", MotorwaySignGraphic.JUNCTION, MotorwaySignSupport.POLE,
            route("Route gauche", "A 4", MotorwaySignColor.RED),
            route("Route droite", "A 9", MotorwaySignColor.RED),
            distance("Distance", "1000 m", MotorwaySignColor.BLUE)),
    D74B("d74b", "D74b", MotorwaySignGraphic.JUNCTION, MotorwaySignSupport.POLE,
            distance("Distance", "3000 m", MotorwaySignColor.BLUE)),

    DA31A("da31a", "DA31a", MotorwaySignGraphic.DOWN, MotorwaySignSupport.OVERHEAD,
            line("Destination verte", "COGNAC", MotorwaySignColor.GREEN, 0),
            line("Destination locale", "ST JEAN D'ANGÉLY", MotorwaySignColor.WHITE, 1),
            route("Numéro de sortie", "13", MotorwaySignColor.WHITE)),
    DA31B("da31b", "DA31b", MotorwaySignGraphic.DOWN_DOUBLE, MotorwaySignSupport.OVERHEAD,
            route("Route", "N 10", MotorwaySignColor.RED),
            line("Destination verte 1", "CHOLET", MotorwaySignColor.GREEN, 0),
            line("Destination verte 2", "ANGERS", MotorwaySignColor.GREEN, 0),
            line("Destination locale 1", "VENDRENNES", MotorwaySignColor.WHITE, 1),
            line("Destination locale 2", "LES HERBIERS", MotorwaySignColor.WHITE, 1)),
    DA31D("da31d", "DA31d", MotorwaySignGraphic.DOWN, MotorwaySignSupport.OVERHEAD,
            line("Destination verte", "CLERMONT-FD", MotorwaySignColor.GREEN, 0),
            line("Destination locale 1", "CHAMALIÈRES", MotorwaySignColor.WHITE, 1),
            line("Destination locale 2", "ROYAT", MotorwaySignColor.WHITE, 1),
            route("Numéro de sortie", "19", MotorwaySignColor.WHITE)),
    DA31E("da31e", "DA31e", MotorwaySignGraphic.DOWN, MotorwaySignSupport.OVERHEAD,
            route("Route", "N 144", MotorwaySignColor.RED),
            line("Destination verte", "MONTLUÇON", MotorwaySignColor.GREEN, 0),
            line("Destination locale", "ST ÉLOI LES MINES", MotorwaySignColor.WHITE, 1)),
    DA31F("da31f", "DA31f", MotorwaySignGraphic.DOWN, MotorwaySignSupport.OVERHEAD,
            route("Route", "A 75", MotorwaySignColor.RED),
            line("Destination 1", "TOULOUSE", MotorwaySignColor.BLUE, 0),
            line("Destination 2", "RODEZ", MotorwaySignColor.BLUE, 1),
            line("Destination 3", "MENDE", MotorwaySignColor.BLUE, 1)),
    DA32A("da32a", "DA32a", MotorwaySignGraphic.DOWN, MotorwaySignSupport.OVERHEAD,
            line("Ligne 1", "AIRE DE", MotorwaySignColor.WHITE, 0),
            line("Ligne 2", "ST SAUVEUR", MotorwaySignColor.WHITE, 0)),
    DA32A_DC("da32a_dc", "DA32a — caractères L4", MotorwaySignGraphic.DOWN, MotorwaySignSupport.OVERHEAD,
            italic("Ligne 1", "AIRE DE", MotorwaySignColor.WHITE, 0),
            italic("Ligne 2", "ST SAUVEUR", MotorwaySignColor.WHITE, 0)),
    DA32B("da32b", "DA32b", MotorwaySignGraphic.DOWN, MotorwaySignSupport.OVERHEAD,
            line("Ligne 1", "AIRE DE", MotorwaySignColor.BLUE, 0),
            line("Ligne 2", "ST SAUVEUR", MotorwaySignColor.BLUE, 0)),
    DA32B_DC("da32b_dc", "DA32b — caractères L4", MotorwaySignGraphic.DOWN, MotorwaySignSupport.OVERHEAD,
            italic("Ligne 1", "AIRE DE", MotorwaySignColor.BLUE, 0),
            italic("Ligne 2", "ST SAUVEUR", MotorwaySignColor.BLUE, 0)),

    DA41A("da41a", "DA41a", MotorwaySignGraphic.DOWN, MotorwaySignSupport.OVERHEAD,
            line("Destination verte", "CHARTRES", MotorwaySignColor.GREEN, 0),
            line("Destination locale", "ABLIS", MotorwaySignColor.WHITE, 1),
            route("Numéro de sortie", "12", MotorwaySignColor.WHITE),
            distance("Distance", "1500 m")),
    DA41B("da41b", "DA41b", MotorwaySignGraphic.DOWN_DOUBLE, MotorwaySignSupport.OVERHEAD,
            route("Route", "A 20", MotorwaySignColor.RED),
            line("Destination verte", "MONTAUBAN", MotorwaySignColor.GREEN, 0),
            line("Destination locale", "AGEN", MotorwaySignColor.WHITE, 1),
            distance("Distance", "1500 m")),
    DA41C("da41c", "DA41c", MotorwaySignGraphic.DOWN_DOUBLE, MotorwaySignSupport.OVERHEAD,
            route("Route", "A 6", MotorwaySignColor.RED),
            line("Destination 1", "PARIS", MotorwaySignColor.BLUE, 0),
            line("Destination 2", "LYON", MotorwaySignColor.BLUE, 1),
            line("Destination 3", "ÉVRY", MotorwaySignColor.BLUE, 2),
            distance("Distance", "1500 m")),
    DA41D_TOP("da41d_top", "DA41d — panneau gauche", MotorwaySignGraphic.DOWN, MotorwaySignSupport.OVERHEAD,
            route("Route", "A 10", MotorwaySignColor.RED),
            line("Destination 1", "BORDEAUX", MotorwaySignColor.BLUE, 0),
            line("Destination 2", "ORLÉANS", MotorwaySignColor.BLUE, 0),
            distance("Distance", "1500 m")),
    DA41D_BOTTOM("da41d_bottom", "DA41d — panneau droit", MotorwaySignGraphic.DOWN, MotorwaySignSupport.OVERHEAD,
            route("Route", "A 11", MotorwaySignColor.RED),
            line("Destination 1", "CHARTRES", MotorwaySignColor.GREEN, 0),
            line("Destination 2", "LE MANS", MotorwaySignColor.GREEN, 0),
            distance("Distance", "1500 m")),
    DA41E_TOP("da41e_top", "DA41e — panneau gauche", MotorwaySignGraphic.DOWN, MotorwaySignSupport.OVERHEAD,
            route("Route", "A 6", MotorwaySignColor.RED),
            line("Destination", "LYON", MotorwaySignColor.BLUE, 0),
            distance("Distance", "1500 m")),
    DA41E_BOTTOM("da41e_bottom", "DA41e — panneau droit", MotorwaySignGraphic.DOWN, MotorwaySignSupport.OVERHEAD,
            route("Route", "A 5", MotorwaySignColor.RED),
            line("Destination", "TROYES", MotorwaySignColor.BLUE, 0),
            distance("Distance", "1500 m")),
    DA41F("da41f", "DA41f", MotorwaySignGraphic.DIAGONAL_RIGHT, MotorwaySignSupport.OVERHEAD,
            line("Destination 1", "PARIS", MotorwaySignColor.BLUE, 0),
            line("Destination 2", "LYON", MotorwaySignColor.BLUE, 0),
            distance("Distance", "1500 m")),

    DA51B("da51b", "DA51b", MotorwaySignGraphic.SCHEMATIC_RIGHT, MotorwaySignSupport.OVERHEAD,
            line("Destination principale", "PARIS", MotorwaySignColor.WHITE, 0),
            line("Destination sortie", "ÉVRY", MotorwaySignColor.WHITE, 0),
            distance("Distance", "1500 m")),
    DA51BR("da51br", "DA51br", MotorwaySignGraphic.SCHEMATIC_RIGHT, MotorwaySignSupport.OVERHEAD,
            line("Destination principale", "PARIS", MotorwaySignColor.WHITE, 0),
            line("Destination sortie", "ÉVRY", MotorwaySignColor.WHITE, 0),
            info("Service", "AIRE", MotorwaySignColor.BLUE, 1),
            distance("Distance", "1500 m")),
    DA52A("da52a", "DA52a", MotorwaySignGraphic.SCHEMATIC_RIGHT, MotorwaySignSupport.OVERHEAD,
            route("Route", "N 104", MotorwaySignColor.WHITE),
            line("Destination 1", "ÉVRY", MotorwaySignColor.BLUE, 0),
            line("Destination 2", "LYON", MotorwaySignColor.BLUE, 0),
            distance("Distance", "1500 m")),
    DA52B("da52b", "DA52b", MotorwaySignGraphic.SCHEMATIC_LEFT, MotorwaySignSupport.OVERHEAD,
            route("Route", "A 5b", MotorwaySignColor.RED),
            line("Destination 1", "PARIS", MotorwaySignColor.BLUE, 0),
            line("Destination 2", "LYON", MotorwaySignColor.BLUE, 0),
            distance("Distance", "1500 m"));

    private final String serializedName;
    private final String displayName;
    private final MotorwaySignGraphic graphic;
    private final MotorwaySignSupport support;
    private final MotorwaySignSlot[] slots;

    MotorwaySignPreset(
            String serializedName,
            String displayName,
            MotorwaySignGraphic graphic,
            MotorwaySignSupport support,
            MotorwaySignSlot... slots
    ) {
        this.serializedName = serializedName;
        this.displayName = displayName;
        this.graphic = graphic;
        this.support = support;
        this.slots = Arrays.copyOf(slots, Math.min(slots.length, 6));
    }

    public String getSerializedName() {
        return this.serializedName;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public MotorwaySignGraphic getGraphic() {
        return this.graphic;
    }

    public MotorwaySignSupport getSupport() {
        return this.support;
    }

    public int getSlotCount() {
        return this.slots.length;
    }

    public MotorwaySignSlot getSlot(int index) {
        return index >= 0 && index < this.slots.length
                ? this.slots[index]
                : new MotorwaySignSlot("Texte", "", RoadTextFont.L1, MotorwaySignColor.BLUE, 0, MotorwaySignRole.DESTINATION);
    }

    public MotorwaySignPreset next() {
        MotorwaySignPreset[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }

    public MotorwaySignPreset previous() {
        MotorwaySignPreset[] values = values();
        return values[(this.ordinal() + values.length - 1) % values.length];
    }

    public static MotorwaySignPreset fromSerializedName(String value) {
        if (value != null) {
            /*
             * Migration transparente des deux anciens doublons désormais
             * fusionnés dans D32a : les mondes existants conservent leur
             * texte/couleur mais utilisent le modèle unique.
             */
            if ("d32a_dc".equals(value) || "d32b".equals(value)) {
                return D32A;
            }
            for (MotorwaySignPreset preset : values()) {
                if (preset.serializedName.equals(value)) {
                    return preset;
                }
            }
        }
        return FREEFORM;
    }

    private static MotorwaySignSlot line(String label, String text, MotorwaySignColor color, int group) {
        return new MotorwaySignSlot(label, text, RoadTextFont.L1, color, group, MotorwaySignRole.DESTINATION);
    }

    private static MotorwaySignSlot italic(String label, String text, MotorwaySignColor color, int group) {
        return new MotorwaySignSlot(label, text, RoadTextFont.L4, color, group, MotorwaySignRole.DESTINATION);
    }

    private static MotorwaySignSlot info(String label, String text, MotorwaySignColor color, int group) {
        return new MotorwaySignSlot(label, text, RoadTextFont.L1, color, group, MotorwaySignRole.INFO);
    }

    private static MotorwaySignSlot route(String label, String text, MotorwaySignColor color) {
        return new MotorwaySignSlot(label, text, RoadTextFont.L1, color, -1, MotorwaySignRole.ROUTE);
    }

    private static MotorwaySignSlot distance(String label, String text) {
        return new MotorwaySignSlot(label, text, RoadTextFont.L1, MotorwaySignColor.WHITE, -1, MotorwaySignRole.DISTANCE);
    }

    private static MotorwaySignSlot distance(String label, String text, MotorwaySignColor color) {
        return new MotorwaySignSlot(label, text, RoadTextFont.L1, color, -1, MotorwaySignRole.DISTANCE);
    }

    private static MotorwaySignSlot distanceInPanel(String label, String text, MotorwaySignColor color, int group) {
        return new MotorwaySignSlot(label, text, RoadTextFont.L1, color, group, MotorwaySignRole.INFO);
    }
}
