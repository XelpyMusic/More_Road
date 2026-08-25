/*
 * Génère les calques PNG exacts des panneaux autoroutiers à partir des SVG
 * de référence. Les textes vectorisés sont volontairement exclus : ils sont
 * remplacés en jeu par les champs L1/L4 de la BlockEntity.
 *
 * Usage :
 *   node scripts/generate-motorway-sign-artwork.cjs <dossier-svg>
 */
const fs = require("fs");
const path = require("path");
const sharp = require("sharp");
const { xml2js, js2xml } = require("xml-js");
const { generateSe2bSymbol } = require("./generate-se2b-symbol.cjs");

const sourceDirectory = process.argv[2];
if (!sourceDirectory) {
    throw new Error("Le dossier contenant les SVG est requis.");
}

const outputDirectory = path.resolve(
    "src/main/resources/assets/moreroad/textures/block/motorway_sign"
);

/*
 * Les identifiants viennent directement des objets vectoriels LibreOffice.
 * Une couche de masque est convertie en blanc afin que le renderer puisse la
 * teinter avec la couleur choisie sans altérer sa silhouette exacte.
 */
const signs = {
    d31b_ex1: {
        source: "D31B_ex1.svg",
        layers: {
            frame: { ids: ["id3", "id4", "id7", "id8"], mask: false },
            route: { ids: ["id6"], mask: true },
            graphics: { ids: ["id5"], mask: false }
        }
    },
    d31b_ex2: {
        source: "D31B_ex2.svg",
        layers: {
            frame: { ids: ["id3", "id4", "id6", "id7"], mask: false },
            route: { ids: ["id9"], mask: true },
            panel_top: { ids: ["id8"], mask: true },
            panel_bottom: { ids: ["id5"], mask: true },
            graphics: { ids: ["id10"], mask: false }
        }
    },
    d31d: {
        source: "D31D.svg",
        layers: {
            frame: { ids: ["id3", "id19", "id29"], mask: false },
            panel_top: { ids: ["id4"], mask: true },
            panel_middle: { ids: ["id20"], mask: true },
            panel_bottom: { ids: ["id30"], mask: true },
            graphics: { ids: ["id21", "id22", "id23"], mask: false }
        }
    },
    d31e: {
        source: "D31E.svg",
        layers: {
            frame: { ids: ["id3", "id4", "id7"], mask: false },
            route: { ids: ["id6"], mask: true },
            panel_top: { ids: ["id5"], mask: true },
            panel_middle: { ids: ["id8"], mask: true },
            panel_bottom: { ids: ["id9"], mask: true },
            graphics: { ids: ["id16"], mask: false }
        }
    },
    d32a: {
        source: "D32A.svg",
        layers: {
            frame: { ids: ["id3"], mask: false },
            panel: { ids: ["id4"], mask: true },
            graphics: { ids: ["id25"], mask: false }
        }
    },
    d32a_dc: {
        source: "D32A_dc.svg",
        layers: {
            frame: { ids: ["id3"], mask: false },
            panel: { ids: ["id4"], mask: true },
            graphics: { ids: ["id25"], mask: false }
        }
    },
    d32b: {
        source: "D32B.svg",
        layers: {
            frame: { ids: ["id3"], mask: false },
            panel: { ids: ["id4"], mask: true },
            graphics: { ids: ["id26"], mask: false }
        }
    },
    d41a: {
        source: "D41A.svg",
        layers: {
            frame: { ids: ["id3", "id15", "id43"], mask: false },
            panel_top: { ids: ["id4"], mask: true },
            panel_middle: { ids: ["id16"], mask: true },
            panel_bottom: { ids: ["id44"], mask: true },
            graphics: { ids: ["id13", "id14"], mask: false }
        }
    },
    d41b: {
        source: "D41B.svg",
        layers: {
            frame: { ids: ["id3", "id6", "id16"], mask: false },
            route: { ids: ["id5"], mask: true },
            panel_top: { ids: ["id4"], mask: true },
            panel_middle: { ids: ["id7"], mask: true },
            panel_bottom: { ids: ["id17"], mask: true }
        }
    },
    d41c: {
        source: "D41C.svg",
        layers: {
            frame: { ids: ["id3", "id6", "id32"], mask: false },
            route: { ids: ["id5"], mask: true },
            panel_top: { ids: ["id4"], mask: true },
            panel_middle: { ids: ["id7"], mask: true },
            panel_bottom: { ids: ["id33"], mask: true }
        }
    },
    d61b: {
        source: "D61B.svg",
        layers: {
            frame: { ids: ["id4", "id6"], mask: false },
            route: { ids: ["id3"], mask: true },
            panel_top: { ids: ["id7"], mask: true },
            panel_bottom: { ids: ["id5"], mask: true }
        }
    },
    d62a: {
        source: "D62A.svg",
        layers: {
            frame: { ids: ["id4", "id6"], mask: false },
            route: { ids: ["id3"], mask: true },
            panel_top: { ids: ["id5"], mask: true },
            panel_bottom: { ids: ["id7"], mask: true }
        }
    },
    d62b: {
        source: "D62B.svg",
        layers: {
            frame: { ids: ["id5", "id7"], mask: false },
            route_left: { ids: ["id3"], mask: true },
            route_right: { ids: ["id4"], mask: true },
            panel_top: { ids: ["id6"], mask: true },
            panel_bottom: { ids: ["id8"], mask: true }
        }
    },
    d62c: {
        source: "D62C.svg",
        layers: {
            frame: { ids: ["id5", "id7"], mask: false },
            route_left: { ids: ["id3"], mask: true },
            route_right: { ids: ["id4"], mask: true },
            panel_top: { ids: ["id6"], mask: true },
            panel_bottom: { ids: ["id8"], mask: true },
            graphics: { ids: ["id9", "id10"], mask: false }
        }
    },
    d62d_top: {
        source: "D62D.svg",
        viewBox: [0, 0, 16204, 12987],
        layers: {
            frame: { ids: ["id5", "id7"], mask: false },
            route_left: { ids: ["id3"], mask: true },
            route_right: { ids: ["id4"], mask: true },
            panel_top: { ids: ["id6"], mask: true },
            panel_bottom: { ids: ["id8"], mask: true },
            graphics: { ids: ["id9", "id10"], mask: false }
        }
    },
    d62d_bottom: {
        source: "D62D.svg",
        viewBox: [0, 18742, 16204, 8215],
        layers: {
            frame: { ids: ["id17"], mask: false },
            route: { ids: ["id15"], mask: true },
            panel: { ids: ["id18"], mask: true },
            graphics: { ids: ["id19", "id20"], mask: false }
        }
    },
    d63c: {
        source: "D63C.svg",
        layers: {
            frame: { ids: ["id3", "id5", "id11"], mask: false },
            panel_top: { ids: ["id4", "id6"], mask: true },
            panel_middle: { ids: ["id12"], mask: true },
            graphics: { ids: ["id7", "id8"], mask: false }
        }
    },
    d63d: {
        source: "D63D.svg",
        layers: {
            frame: { ids: ["id3", "id6", "id8"], mask: false },
            route: { ids: ["id5"], mask: true },
            panel_top: { ids: ["id4"], mask: true },
            panel_middle: { ids: ["id7"], mask: true },
            panel_bottom: { ids: ["id9"], mask: true }
        }
    },
    d64: {
        source: "D64.svg",
        layers: {
            frame: { ids: ["id4", "id12"], mask: false },
            route_left: { ids: ["id10"], mask: true },
            route_right: { ids: ["id8"], mask: true },
            panel_top: { ids: ["id5"], mask: true },
            panel_bottom: { ids: ["id13"], mask: true },
            graphics: { ids: ["id6", "id7"], mask: false }
        }
    },
    d74a: {
        source: "D74a.svg",
        layers: {
            frame: { ids: ["id4", "id12"], mask: false },
            route_left: { ids: ["id10"], mask: true },
            route_right: { ids: ["id8"], mask: true },
            panel_top: { ids: ["id5"], mask: true },
            panel_bottom: { ids: ["id13"], mask: true },
            graphics: { ids: ["id6", "id7"], mask: false }
        }
    },
    d74b: {
        source: "D74b.svg",
        layers: {
            frame: { ids: ["id4", "id7"], mask: false },
            panel_top: { ids: ["id8"], mask: true },
            panel_bottom: { ids: ["id5"], mask: true },
            graphics: { ids: ["id9", "id10"], mask: false }
        }
    },
    d64: {
        source: "D64.svg",
        layers: {
            frame: { ids: ["id4", "id12"], mask: false },
            route_left: { ids: ["id10"], mask: true },
            route_right: { ids: ["id8"], mask: true },
            panel_top: { ids: ["id5"], mask: true },
            panel_bottom: { ids: ["id13"], mask: true },
            graphics: { ids: ["id6", "id7"], mask: false }
        }
    },
    d74a: {
        source: "D74a.svg",
        layers: {
            frame: { ids: ["id4", "id12"], mask: false },
            route_left: { ids: ["id10"], mask: true },
            route_right: { ids: ["id8"], mask: true },
            panel_top: { ids: ["id5"], mask: true },
            panel_bottom: { ids: ["id13"], mask: true },
            graphics: { ids: ["id6", "id7"], mask: false }
        }
    },
    d74b: {
        source: "D74b.svg",
        layers: {
            frame: { ids: ["id4", "id7"], mask: false },
            panel_top: { ids: ["id8"], mask: true },
            panel_bottom: { ids: ["id5"], mask: true },
            graphics: { ids: ["id9", "id10"], mask: false }
        }
    },
    d71: {
        source: "D71.svg",
        layers: {
            frame: { ids: ["id3", "id4", "id5"], mask: false },
            panel: { ids: ["id6"], mask: true }
        }
    },
    d72: {
        source: "D72.svg",
        layers: {
            frame: { ids: ["id3", "id4", "id5"], mask: false },
            panel: { ids: ["id6"], mask: true },
            graphics: { ids: ["id34", "id35", "id42", "id43", "id49", "id50", "id56", "id57"], mask: false }
        }
    },
    d73: {
        source: "D73.svg",
        layers: {
            frame: { ids: ["id3", "id8"], mask: false },
            panel_top: { ids: ["id4"], mask: true },
            panel_bottom: { ids: ["id9"], mask: true },
            graphics: { ids: ["id5", "id6"], mask: false }
        }
    },
    da31a: {
        source: "DA31A.svg",
        layers: {
            frame: { ids: ["id3", "id5"], mask: false },
            panel_top: { ids: ["id4"], mask: true },
            panel_bottom: { ids: ["id6"], mask: true },
            graphics: { ids: ["id7", "id26", "id27"], mask: false }
        }
    },
    da31b: {
        source: "DA31B.svg",
        layers: {
            frame: { ids: ["id8", "id25"], mask: false },
            route: { ids: ["id3"], mask: true },
            panel_top: { ids: ["id9"], mask: true },
            panel_bottom: { ids: ["id26"], mask: true },
            graphics: { ids: ["id27", "id28"], mask: false }
        }
    },
    da31d: {
        source: "DA31D.svg",
        layers: {
            frame: { ids: ["id3", "id4"], mask: false },
            panel_top: { ids: ["id5"], mask: true },
            panel_bottom: { ids: ["id20"], mask: true },
            graphics: { ids: ["id21", "id45", "id46"], mask: false }
        }
    },
    da31e: {
        source: "DA31E.svg",
        layers: {
            frame: { ids: ["id3", "id4"], mask: false },
            route: { ids: ["id5"], mask: true },
            panel_top: { ids: ["id12"], mask: true },
            panel_bottom: { ids: ["id24"], mask: true },
            graphics: { ids: ["id25"], mask: false }
        }
    },
    da31f: {
        source: "DA31F.svg",
        layers: {
            frame: { ids: ["id3", "id4"], mask: false },
            route: { ids: ["id5"], mask: true },
            panel_top: { ids: ["id10"], mask: true },
            panel_bottom: { ids: ["id21"], mask: true },
            graphics: { ids: ["id22"], mask: false }
        }
    },
    da32a: {
        source: "DA32A.svg",
        layers: {
            frame: { ids: ["id3"], mask: false },
            panel: { ids: ["id4"], mask: true },
            graphics: { ids: ["id5"], mask: false }
        }
    },
    da32a_dc: {
        source: "DA32A_dc.svg",
        layers: {
            frame: { ids: ["id3"], mask: false },
            panel: { ids: ["id4"], mask: true },
            graphics: { ids: ["id5"], mask: false }
        }
    },
    da32b: {
        source: "DA32B.svg",
        layers: {
            frame: { ids: ["id3"], mask: false },
            panel: { ids: ["id4"], mask: true },
            graphics: { ids: ["id5"], mask: false }
        }
    },
    da32b_dc: {
        source: "DA32B_dc.svg",
        layers: {
            frame: { ids: ["id3"], mask: false },
            panel: { ids: ["id4"], mask: true },
            graphics: { ids: ["id5"], mask: false }
        }
    }
};

function isBoundingBox(element) {
    return element?.type === "element"
        && element.name === "rect"
        && element.attributes?.class === "BoundingBox";
}

function isDrawableShapeGroup(element) {
    return element?.type === "element"
        && element.name === "g"
        && /^id\d+$/.test(element.attributes?.id || "")
        && (element.elements || []).some(isBoundingBox);
}

function filteredElement(element, keptIds) {
    if (isDrawableShapeGroup(element) && !keptIds.has(element.attributes.id)) {
        return null;
    }
    if (!element?.elements) {
        return element;
    }
    return {
        ...element,
        elements: element.elements
            .map(child => filteredElement(child, keptIds))
            .filter(Boolean)
    };
}

async function renderLayer(svgText, ids, makeMask, outputPath) {
    const document = xml2js(svgText, { compact: false, alwaysChildren: true });
    const filtered = filteredElement(document, new Set(ids));
    const xml = js2xml(filtered, { compact: false, spaces: 0 });
    const rendered = await sharp(Buffer.from(xml))
        .resize({ width: 1024 })
        .ensureAlpha()
        .png()
        .toBuffer();

    if (!makeMask) {
        await sharp(rendered).png().toFile(outputPath);
        return;
    }

    const { data, info } = await sharp(rendered)
        .ensureAlpha()
        .raw()
        .toBuffer({ resolveWithObject: true });
    for (let offset = 0; offset < data.length; offset += info.channels) {
        if (data[offset + 3] !== 0) {
            data[offset] = 255;
            data[offset + 1] = 255;
            data[offset + 2] = 255;
        }
    }
    await sharp(data, { raw: info }).png().toFile(outputPath);
}

async function renderCroppedLayer(svgText, ids, outputPath) {
    const document = xml2js(svgText, { compact: false, alwaysChildren: true });
    const filtered = filteredElement(document, new Set(ids));
    const xml = js2xml(filtered, { compact: false, spaces: 0 });
    const rendered = await sharp(Buffer.from(xml))
        .resize({ width: 2048 })
        .ensureAlpha()
        .png()
        .toBuffer();
    await sharp(rendered)
        .trim({ background: { r: 0, g: 0, b: 0, alpha: 0 } })
        .resize({ width: 256, height: 256, fit: "contain" })
        .png()
        .toFile(outputPath);
}

function applyViewBox(svgText, viewBox) {
    if (!viewBox) {
        return svgText;
    }
    const document = xml2js(svgText, { compact: false, alwaysChildren: true });
    const root = (document.elements || []).find(element => element.type === "element" && element.name === "svg");
    if (!root) {
        throw new Error("Racine SVG introuvable.");
    }
    root.attributes.viewBox = viewBox.join(" ");
    root.attributes.width = `${viewBox[2] / 100}mm`;
    root.attributes.height = `${viewBox[3] / 100}mm`;
    return js2xml(document, { compact: false, spaces: 0 });
}

async function main() {
    fs.mkdirSync(outputDirectory, { recursive: true });
    for (const [signName, sign] of Object.entries(signs)) {
        const svgPath = path.resolve(sourceDirectory, sign.source);
        const svgText = applyViewBox(fs.readFileSync(svgPath, "utf8"), sign.viewBox);
        for (const [layerName, layer] of Object.entries(sign.layers)) {
            const outputPath = path.join(outputDirectory, `${signName}_${layerName}.png`);
            await renderLayer(svgText, layer.ids, layer.mask, outputPath);
        }
    }

    /* Symbole officiel SE2b dédié, indépendant des objets internes d'un panneau. */
    await generateSe2bSymbol(path.join(outputDirectory, "exit_symbol.png"));
}

main().catch(error => {
    console.error(error);
    process.exitCode = 1;
});
