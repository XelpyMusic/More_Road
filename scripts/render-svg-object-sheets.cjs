/*
 * Produit une planche de contrôle des objets LibreOffice d'un SVG.
 * Cet outil permet de repérer visuellement les fonds, symboles et textes
 * avant de constituer les calques utilisés en jeu.
 *
 * Usage : node scripts/render-svg-object-sheets.cjs <dossier-svg> <svg...>
 */
const fs = require("fs");
const path = require("path");
const sharp = require("sharp");
const { xml2js, js2xml } = require("xml-js");

const sourceDirectory = process.argv[2];
const sourceNames = process.argv.slice(3);
if (!sourceDirectory || sourceNames.length === 0) {
    throw new Error("Le dossier source et au moins un SVG sont requis.");
}

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

function collectGroups(element, result = []) {
    if (isDrawableShapeGroup(element)) {
        result.push(element.attributes.id);
    }
    for (const child of element?.elements || []) {
        collectGroups(child, result);
    }
    return result;
}

function filteredElement(element, keptId) {
    if (isDrawableShapeGroup(element) && element.attributes.id !== keptId) {
        return null;
    }
    if (!element?.elements) {
        return element;
    }
    return {
        ...element,
        elements: element.elements
            .map(child => filteredElement(child, keptId))
            .filter(Boolean)
    };
}

function escapeXml(value) {
    return value.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
}

async function renderSheet(sourceName) {
    const sourcePath = path.resolve(sourceDirectory, sourceName);
    const source = fs.readFileSync(sourcePath, "utf8");
    const document = xml2js(source, { compact: false, alwaysChildren: true });
    const groups = collectGroups(document);
    const columns = 4;
    const cellWidth = 300;
    const cellHeight = 230;
    const rows = Math.ceil(groups.length / columns);
    const composites = [];

    for (let index = 0; index < groups.length; index++) {
        const id = groups[index];
        const filtered = filteredElement(document, id);
        const xml = js2xml(filtered, { compact: false, spaces: 0 });
        const image = await sharp(Buffer.from(xml))
            .resize({ width: cellWidth - 20, height: cellHeight - 42, fit: "contain" })
            .png()
            .toBuffer();
        const left = (index % columns) * cellWidth + 10;
        const top = Math.floor(index / columns) * cellHeight + 34;
        composites.push({ input: image, left, top });
        composites.push({
            input: Buffer.from(`<svg width="${cellWidth}" height="30"><text x="10" y="22" font-family="sans-serif" font-size="20" font-weight="bold" fill="#111">${escapeXml(id)}</text></svg>`),
            left: (index % columns) * cellWidth,
            top: Math.floor(index / columns) * cellHeight
        });
    }

    const outputDirectory = process.env.SVG_INSPECTION_OUTPUT
        ? path.resolve(process.env.SVG_INSPECTION_OUTPUT)
        : path.resolve("build/svg-inspection");
    fs.mkdirSync(outputDirectory, { recursive: true });
    const outputPath = path.join(outputDirectory, `${path.parse(sourceName).name}.png`);
    await sharp({
        create: {
            width: columns * cellWidth,
            height: Math.max(1, rows * cellHeight),
            channels: 4,
            background: { r: 238, g: 241, b: 245, alpha: 1 }
        }
    }).composite(composites).png().toFile(outputPath);
    await sharp(Buffer.from(source))
        .resize({ width: 1200 })
        .flatten({ background: "#eef1f5" })
        .png()
        .toFile(path.join(outputDirectory, `${path.parse(sourceName).name}_full.png`));
    console.log(`${sourceName}: ${groups.length} objets -> ${outputPath}`);
}

Promise.all(sourceNames.map(renderSheet)).catch(error => {
    console.error(error);
    process.exitCode = 1;
});
