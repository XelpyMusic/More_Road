/* Liste les dimensions et couleurs des objets dessinables d'un SVG LibreOffice. */
const fs = require("fs");
const path = require("path");
const { xml2js } = require("xml-js");

const sourceDirectory = process.argv[2];
for (const sourceName of process.argv.slice(3)) {
    const document = xml2js(fs.readFileSync(path.resolve(sourceDirectory, sourceName), "utf8"), {
        compact: false,
        alwaysChildren: true
    });
    const result = [];
    visit(document, result);
    console.log(`\n${sourceName}`);
    for (const item of result) {
        console.log([
            item.id,
            `${item.box.x},${item.box.y},${item.box.width},${item.box.height}`,
            [...item.fills].join(","),
            [...item.strokes].join(","),
            item.pathLength
        ].join("\t"));
    }
}

function visit(element, result) {
    if (element?.type === "element"
        && element.name === "g"
        && /^id\d+$/.test(element.attributes?.id || "")) {
        const box = (element.elements || []).find(child => child?.name === "rect" && child.attributes?.class === "BoundingBox");
        if (box) {
            const fills = new Set();
            const strokes = new Set();
            let pathLength = 0;
            collectStyle(element, fills, strokes, value => pathLength += value);
            result.push({ id: element.attributes.id, box: box.attributes, fills, strokes, pathLength });
        }
    }
    for (const child of element?.elements || []) {
        visit(child, result);
    }
}

function collectStyle(element, fills, strokes, addPathLength) {
    if (element?.attributes?.class !== "BoundingBox") {
        const fill = element?.attributes?.fill;
        const stroke = element?.attributes?.stroke;
        if (fill && fill !== "none") fills.add(fill);
        if (stroke && stroke !== "none") strokes.add(stroke);
        if (element?.name === "path") addPathLength((element.attributes?.d || "").length);
    }
    for (const child of element?.elements || []) {
        collectStyle(child, fills, strokes, addPathLength);
    }
}
