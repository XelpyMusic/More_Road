/* Génère le masque du symbole officiel d'échangeur SE2b, sans le numéro. */
const path = require("path");
const sharp = require("sharp");

/*
 * Tracé SE2b public (forme réglementaire) remis dans un viewBox serré.
 * Le masque blanc est teinté en noir ou en blanc par le renderer.
 */
const svg = Buffer.from(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="-273 204 42 34">
  <path fill="#fff" d="m -250.91761,204.92404 c -0.81028,0 -1.12017,0.81046 -0.6103,1.27072 l 5.21984,4.67 c -4.36011,2.23931 -9.12951,8.08943 -8.88008,10.13995 l -2.38952,-16.06 h -3.27008 l -0.10077,32.34945 h 8.05067 c -1.15066,-6.87916 2.58917,-19.07998 8.8992,-23.01979 l 1.95026,6.15001 c 0.21085,0.67938 1.04042,0.89039 1.45108,0.12971 l 8.37985,-13.92938 c 0.35967,-0.70969 -0.15038,-1.65106 -1.05988,-1.70067 z m -16.24965,0.0403 -5.02037,32.33963 h 8.30957 l 0.0605,-32.33963 z"/>
</svg>`);

function defaultOutput() {
    return path.resolve(
        "src/main/resources/assets/moreroad/textures/block/motorway_sign/exit_symbol.png"
    );
}

async function generateSe2bSymbol(output = defaultOutput()) {
    await sharp(svg)
        .resize(504, 408, { fit: "contain", background: { r: 0, g: 0, b: 0, alpha: 0 } })
        .png()
        .toFile(path.resolve(output));
}

module.exports = { generateSe2bSymbol };

if (require.main === module) {
    generateSe2bSymbol(process.argv[2] || defaultOutput()).catch(error => {
        console.error(error);
        process.exitCode = 1;
    });
}
