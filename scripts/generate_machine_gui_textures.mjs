import { mkdirSync, writeFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { deflateSync } from "node:zlib";

const WIDTH = 256;
const HEIGHT = 256;
const PANEL_WIDTH = 194;
const PANEL_HEIGHT = 210;
const PALETTE = {
    transparent: [0, 0, 0, 0],
    outline: [55, 55, 55, 255],
    shadow: [85, 85, 85, 255],
    slot: [139, 139, 139, 255],
    panel: [198, 198, 198, 255],
    highlight: [238, 238, 238, 255],
    dark: [43, 48, 49, 255],
    green: [117, 189, 75, 255],
    cyan: [73, 184, 209, 255],
    amber: [219, 180, 65, 255],
    magenta: [184, 104, 178, 255],
    processCyan: [93, 185, 199, 255]
};

function image() {
    return new Uint8Array(WIDTH * HEIGHT * 4);
}

function pixel(data, x, y, color) {
    if (x < 0 || y < 0 || x >= WIDTH || y >= HEIGHT) return;
    const offset = (y * WIDTH + x) * 4;
    data.set(color, offset);
}

function rect(data, x, y, width, height, color) {
    for (let yy = y; yy < y + height; yy++) {
        for (let xx = x; xx < x + width; xx++) pixel(data, xx, yy, color);
    }
}

function bevel(data, x, y, width, height, fill = PALETTE.panel) {
    rect(data, x, y, width, height, PALETTE.outline);
    rect(data, x + 1, y + 1, width - 2, height - 2, fill);
    rect(data, x + 1, y + 1, width - 2, 1, PALETTE.highlight);
    rect(data, x + 1, y + 1, 1, height - 2, PALETTE.highlight);
    rect(data, x + 1, y + height - 2, width - 2, 1, PALETTE.shadow);
    rect(data, x + width - 2, y + 1, 1, height - 2, PALETTE.shadow);
}

function slot(data, itemX, itemY) {
    const x = itemX - 1;
    const y = itemY - 1;
    rect(data, x, y, 18, 18, PALETTE.shadow);
    rect(data, x + 1, y + 1, 16, 16, PALETTE.slot);
    rect(data, x + 1, y + 1, 16, 1, PALETTE.outline);
    rect(data, x + 1, y + 1, 1, 16, PALETTE.outline);
    rect(data, x + 2, y + 16, 15, 1, PALETTE.highlight);
    rect(data, x + 16, y + 2, 1, 15, PALETTE.highlight);
}

function arrow(data, x, y, length, color = PALETTE.shadow) {
    rect(data, x, y + 2, length - 4, 3, color);
    rect(data, x + length - 6, y, 2, 7, color);
    rect(data, x + length - 4, y + 1, 2, 5, color);
    rect(data, x + length - 2, y + 2, 2, 3, color);
}

function meter(data, x, y, height) {
    rect(data, x, y, 8, height, PALETTE.outline);
    rect(data, x + 1, y + 1, 6, height - 2, PALETTE.dark);
}

function downArrow(data, x, y, color = PALETTE.shadow) {
    rect(data, x + 2, y, 3, 4, color);
    rect(data, x, y + 3, 7, 2, color);
    rect(data, x + 1, y + 5, 5, 1, color);
    rect(data, x + 2, y + 6, 3, 1, color);
}

function common(data) {
    bevel(data, 0, 0, PANEL_WIDTH, PANEL_HEIGHT);
    rect(data, 8, 19, 178, 95, PALETTE.panel);
    bevel(data, 8, 19, 178, 95, PALETTE.panel);
    for (let row = 0; row < 3; row++) {
        for (let column = 0; column < 9; column++) slot(data, 16 + column * 18, 128 + row * 18);
    }
    for (let column = 0; column < 9; column++) slot(data, 16 + column * 18, 186);
}

function incubator(data) {
    common(data);
    slot(data, 30, 50);
    slot(data, 64, 50);
    slot(data, 94, 50);
    slot(data, 124, 50);
    slot(data, 154, 50);
    meter(data, 55, 49, 18);
    meter(data, 85, 49, 18);
    meter(data, 115, 49, 18);
    arrow(data, 142, 55, 10);
    bevel(data, 21, 101, 157, 7, PALETTE.shadow);
}

function splicer(data) {
    common(data);
    slot(data, 38, 48);
    slot(data, 74, 48);
    slot(data, 146, 48);
    rect(data, 46, 31, 39, 3, PALETTE.shadow);
    rect(data, 46, 33, 3, 14, PALETTE.shadow);
    rect(data, 82, 33, 3, 14, PALETTE.shadow);
    rect(data, 63, 28, 6, 6, PALETTE.outline);
    rect(data, 65, 30, 2, 2, PALETTE.dark);
    rect(data, 101, 53, 29, 5, PALETTE.shadow);
    arrow(data, 134, 52, 10, PALETTE.shadow);
}

function bottler(data) {
    common(data);
    slot(data, 30, 50);
    slot(data, 54, 50);
    slot(data, 78, 50);
    slot(data, 158, 50);

    // Three inputs rise into one neutral bus, then descend into the filling stage.
    rect(data, 37, 36, 66, 3, PALETTE.shadow);
    rect(data, 37, 38, 3, 11, PALETTE.shadow);
    rect(data, 61, 38, 3, 11, PALETTE.shadow);
    rect(data, 85, 38, 3, 11, PALETTE.shadow);
    rect(data, 100, 38, 3, 18, PALETTE.shadow);

    // Progress module and outbound arrow share the inventory-slot center line.
    rect(data, 104, 52, 36, 13, PALETTE.outline);
    rect(data, 106, 54, 32, 9, PALETTE.dark);
    arrow(data, 142, 55, 13, PALETTE.processCyan);
}

function condenser(data) {
    common(data);
    slot(data, 158, 50);
    bevel(data, 27, 28, 70, 34, PALETTE.slot);
    for (let i = 0; i < 6; i++) rect(data, 31 + i * 10, 32, 4, 26, PALETTE.shadow);
    rect(data, 116, 27, 8, 34, PALETTE.outline);
    arrow(data, 126, 55, 28, PALETTE.processCyan);
    rect(data, 101, 34, 3, 20, PALETTE.shadow);
    rect(data, 103, 51, 10, 3, PALETTE.shadow);
}

const crcTable = new Uint32Array(256);
for (let n = 0; n < 256; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) c = (c & 1) ? 0xEDB88320 ^ (c >>> 1) : c >>> 1;
    crcTable[n] = c >>> 0;
}

function crc32(buffer) {
    let crc = 0xFFFFFFFF;
    for (const byte of buffer) crc = crcTable[(crc ^ byte) & 0xFF] ^ (crc >>> 8);
    return (crc ^ 0xFFFFFFFF) >>> 0;
}

function chunk(type, payload) {
    const name = Buffer.from(type, "ascii");
    const length = Buffer.alloc(4);
    length.writeUInt32BE(payload.length);
    const checksum = Buffer.alloc(4);
    checksum.writeUInt32BE(crc32(Buffer.concat([name, payload])));
    return Buffer.concat([length, name, payload, checksum]);
}

function png(data) {
    const header = Buffer.alloc(13);
    header.writeUInt32BE(WIDTH, 0);
    header.writeUInt32BE(HEIGHT, 4);
    header[8] = 8;
    header[9] = 6;
    const scanlines = Buffer.alloc((WIDTH * 4 + 1) * HEIGHT);
    for (let y = 0; y < HEIGHT; y++) {
        const row = y * (WIDTH * 4 + 1);
        scanlines[row] = 0;
        Buffer.from(data.buffer, data.byteOffset + y * WIDTH * 4, WIDTH * 4).copy(scanlines, row + 1);
    }
    return Buffer.concat([
        Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]),
        chunk("IHDR", header),
        chunk("IDAT", deflateSync(scanlines, { level: 9 })),
        chunk("IEND", Buffer.alloc(0))
    ]);
}

const outputDir = resolve("src/main/resources/assets/cybercultivator/textures/gui");
mkdirSync(outputDir, { recursive: true });
for (const [name, draw] of Object.entries({
    bio_incubator: incubator,
    gene_splicer: splicer,
    serum_bottler: bottler,
    atmospheric_condenser: condenser
})) {
    const data = image();
    draw(data);
    const path = resolve(outputDir, `${name}.png`);
    writeFileSync(path, png(data));
    console.log(path);
}
