import { mkdirSync, writeFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { deflateSync } from "node:zlib";

const SIZE = 16;
const TRANSPARENT = [0, 0, 0, 0];

const COMMON = {
    ".": TRANSPARENT,
    k: [20, 27, 32, 255],
    d: [35, 46, 54, 255],
    m: [66, 82, 91, 255],
    l: [118, 140, 148, 255],
    w: [218, 239, 238, 255],
    c: [21, 184, 197, 255],
    C: [104, 235, 232, 255],
    b: [28, 105, 171, 255],
    p: [91, 42, 145, 255],
    P: [174, 73, 202, 255],
    r: [151, 39, 51, 255],
    R: [230, 73, 72, 255],
    a: [154, 84, 22, 255],
    A: [232, 153, 39, 255],
    y: [255, 216, 91, 255],
    g: [29, 85, 57, 255],
    G: [60, 148, 78, 255],
    h: [139, 190, 91, 255]
};

function rgba(hex) {
    const value = Number.parseInt(hex.slice(1), 16);
    return [value >> 16, (value >> 8) & 0xFF, value & 0xFF, 255];
}

function rgbaAlpha(hex, alpha) {
    const color = rgba(hex);
    color[3] = alpha;
    return color;
}

function image(size = SIZE) {
    return new Uint8Array(size * size * 4);
}

function pixel(data, x, y, color, size = SIZE) {
    const offset = (y * size + x) * 4;
    data.set(color, offset);
}

function drawPattern(data, lines, palette, keepExisting = false) {
    const size = lines.length;
    for (let y = 0; y < size; y++) {
        if (lines[y].length !== size) throw new Error(`Row ${y} has ${lines[y].length} columns, expected ${size}: ${lines[y]}`);
        for (let x = 0; x < size; x++) {
            const symbol = lines[y][x];
            if (keepExisting && symbol === ".") continue;
            const color = palette[symbol];
            if (!color) throw new Error(`Unknown palette symbol '${symbol}' at ${x},${y}`);
            pixel(data, x, y, color, size);
        }
    }
}

function buildPattern(size, draw) {
    const grid = Array.from({ length: size }, () => Array(size).fill("."));
    const set = (x, y, symbol) => {
        if (x >= 0 && x < size && y >= 0 && y < size) grid[y][x] = symbol;
    };
    const span = (y, fromX, toX, symbol) => {
        for (let x = fromX; x <= toX; x++) set(x, y, symbol);
    };
    const rect = (fromX, fromY, toX, toY, symbol) => {
        for (let y = fromY; y <= toY; y++) span(y, fromX, toX, symbol);
    };
    draw({ grid, set, span, rect });
    return grid.map(row => row.join(""));
}

const STONE = [
    "4444322321223333",
    "3323233333311232",
    "3211222122233333",
    "3344343344333222",
    "2332223234444233",
    "3433333332212124",
    "2334442442223333",
    "2211212233222333",
    "4443433444333332",
    "3343334443344443",
    "1323221123233222",
    "3333344333334434",
    "3222343223311212",
    "4432233222332224",
    "3223333333334444",
    "3333332234433233"
];

const STONE_PALETTE = {
    1: rgba("#686868"),
    2: rgba("#747474"),
    3: rgba("#7f7f7f"),
    4: rgba("#8f8f8f")
};

function ore(overlay, mineralPalette) {
    const data = image();
    drawPattern(data, STONE, STONE_PALETTE);
    drawPattern(data, overlay, mineralPalette, true);
    return data;
}

const SILICON_OVERLAY = [
    "................",
    "..abb...........",
    ".abcC...........",
    ".abCCd..........",
    "..bCd......ab...",
    "...b......abcC..",
    ".........abCCd..",
    "..........bCd...",
    "..........bb....",
    "..ab............",
    ".abcC.....ab....",
    ".abCCd...abcC...",
    "..bCd...abCCd...",
    "...b.....bCd....",
    "..........b.....",
    "................"
];

const RARE_EARTH_OVERLAY = [
    "................",
    "..ab............",
    ".abcA...........",
    ".abAyv.....ab...",
    "..bA.....abcA...",
    "...b....abAyv...",
    "........bcA.....",
    ".........b......",
    "............ab..",
    "...ab......abcA.",
    "..abcA....abAyv.",
    "..bcAy...abcA...",
    "...b....abAy....",
    ".......abcA.....",
    "........bAy.....",
    ".........b......"
];

const RAW_SILICON_BLOCK = {
    palette: {
        1: rgba("#1c2a33"), 2: rgba("#283943"), 3: rgba("#344b56"),
        4: rgba("#42616b"), 5: rgba("#29889b"), 6: rgba("#59d4dd"),
        7: rgba("#d0ffff")
    },
    lines: [
        "2343323676542341",
        "3454234345113442",
        "2432254325125332",
        "2342113223256433",
        "3463122234234213",
        "4564323356433122",
        "6775433234232224",
        "3452235343343242",
        "2352456432232452",
        "3334534211343564",
        "3345643123454342",
        "3455654234565432",
        "2566755322351253",
        "2344612213332364",
        "3234523236323675",
        "1223433367431361"
    ]
};

const RAW_RARE_EARTH_BLOCK = {
    palette: {
        1: rgba("#63320a"), 2: rgba("#82420a"), 3: rgba("#a7540a"),
        4: rgba("#c6670b"), 5: rgba("#ea8e16"), 6: rgba("#ffe188")
    },
    lines: [
        "5521123532244433",
        "6532136533123435",
        "6543266543211256",
        "4332466554321114",
        "4321344542316632",
        "2211224422146533",
        "3256412421155443",
        "2566441211323342",
        "2454221124632321",
        "1124221246643211",
        "1212212235544321",
        "3442123423543212",
        "4454224432332542",
        "2563314321236644",
        "4243221112266554",
        "2112211221345553"
    ]
};

const SILICON_BLOCK = {
    palette: {
        1: rgba("#153f4a"), 2: rgba("#1c5965"), 3: rgba("#256f7a"),
        4: rgba("#2e8792"), 5: rgba("#369ca7"), 6: rgba("#43adb7"),
        7: rgba("#55c0c8"), 8: rgba("#70d2d6"), 9: rgba("#9be4e2"),
        A: rgba("#cef5ef")
    },
    lines: [
        "5555535333233322",
        "5AAAA99AA9998872",
        "5A99988778872272",
        "5A89887688742262",
        "5A98876887624462",
        "3A88768876467262",
        "5987688764674242",
        "5974787666742142",
        "3947876647441262",
        "3978762474422161",
        "2877624744121241",
        "2824247421212241",
        "2822444212122141",
        "2822222111221141",
        "1887666444464441",
        "2122112211111111"
    ]
};

const RARE_EARTH_BLOCK = {
    palette: {
        1: rgba("#6a3508"), 2: rgba("#7f4007"), 3: rgba("#954a06"),
        4: rgba("#ab5707"), 5: rgba("#bf6509"), 6: rgba("#d2730c"),
        7: rgba("#e48714"), 8: rgba("#f2a02a")
    },
    lines: [
        "8887877888887653",
        "8665566787775542",
        "8684667877758442",
        "8542678767554241",
        "8566777665533451",
        "8667776655323541",
        "8678766553235441",
        "8787665532355461",
        "8876654323554661",
        "8766544335556672",
        "8665443455566782",
        "8654444554667872",
        "8584345546678461",
        "8442454466774251",
        "8324544667776552",
        "7232212133322122"
    ]
};

const patterns = {
    raw_silicon_crystal: {
        palette: {
            ".": TRANSPARENT,
            1: rgba("#0e1922"), 2: rgba("#24323a"), 3: rgba("#293f4c"),
            4: rgba("#36454f"), 5: rgba("#46606d"), 6: rgba("#4a768a"),
            7: rgba("#3a9cb7"), 8: rgba("#3cd3ed"), 9: rgba("#bcfcfc")
        },
        lines: [
            "................",
            ".....112........",
            "...1233211......",
            "..124897321.....",
            ".12489964321....",
            ".125886455321...",
            ".123565443321...",
            "..245544678321..",
            ".12467433689321.",
            ".13986433265421.",
            "..478543223321..",
            "...233222112....",
            "....111111......",
            "................",
            "................",
            "................"
        ]
    },
    raw_rare_earth: {
        palette: {
            ".": TRANSPARENT,
            1: rgba("#3b1b05"), 2: rgba("#5a2d05"), 3: rgba("#663103"),
            4: rgba("#994e0a"), 5: rgba("#a75403"), 6: rgba("#db7a08"),
            7: rgba("#e27c08"), 8: rgba("#fcad2b"), 9: rgba("#ffd66b")
        },
        lines: [
            "................",
            "......12........",
            "....123321......",
            "...124768321....",
            "..12589974321...",
            ".1247898654321..",
            ".1356775543321..",
            ".12456547654321.",
            "..2354378995431.",
            "..1243367864321.",
            "...12234554321..",
            "....11123321....",
            ".....11111......",
            "................",
            "................",
            "................"
        ]
    },
    silicon_shard: {
        palette: {
            ".": TRANSPARENT,
            1: rgba("#111c24"), 2: rgba("#1d2d38"), 3: rgba("#29414e"),
            4: rgba("#3d5b68"), 5: rgba("#397789"), 6: rgba("#34aabe"),
            7: rgba("#62dbe5"), 8: rgba("#baf8f6"), 9: rgba("#e2ffff")
        },
        lines: [
            "................",
            "..........233...",
            "........234582..",
            "......22367762..",
            "....2346897762..",
            "....2488997762..",
            "...24788976632..",
            "...23577965532..",
            "..245776654321..",
            "..234665588432..",
            "..23334467752...",
            "..22334455231...",
            "..234455321.....",
            "..223321........",
            "...222..........",
            "................"
        ]
    },
    rare_earth_dust: {
        palette: {
            ".": TRANSPARENT,
            1: rgba("#994e0a"), 2: rgba("#fcad2b"), 3: rgba("#e27c08"),
            4: rgba("#db7a08"), 5: rgba("#a75403"), 6: rgba("#5a2d05"),
            7: rgba("#e08312"), 8: rgba("#663103")
        },
        lines: [
            "................",
            "................",
            "................",
            ".......12.......",
            "......33456.....",
            ".....4262546....",
            "....321474111...",
            "...22634241165..",
            "..4311441518166.",
            ".16114161518668.",
            "...47361151666..",
            "..5.81118166..1.",
            ".......61...6...",
            "........2.......",
            "................",
            "................"
        ]
    },
    plant_fiber: {
        palette: {
            ".": TRANSPARENT,
            1: rgba("#0f395a"), 2: rgba("#094453"), 3: rgba("#0c616f"),
            4: rgba("#0c7893"), 5: rgba("#0f8990"), 6: rgba("#4bccd4"),
            7: rgba("#a0dde4"), 8: rgba("#b7fcfd"), 9: rgba("#f4fdfb")
        },
        lines: [
            "................",
            "......7.669.9...",
            ".......6965959..",
            ".....6.98534965.",
            "......66987919..",
            "......699493955.",
            "......36493982..",
            "......979453995.",
            ".....587554413..",
            "....8574583533..",
            "...3954151..3...",
            "..333732........",
            ".337835.........",
            ".37212..........",
            ".31331..........",
            "................"
        ]
    },
    spectrum_monocle: {
        palette: {
            ".": TRANSPARENT,
            k: rgba("#2b2b30"), d: rgba("#515158"), m: rgba("#787a82"),
            l: rgba("#b5b8c0"), W: rgba("#e7e8eb"), p: rgba("#28165f"),
            P: rgba("#3c258c"), b: rgba("#493aa8"), B: rgba("#6551d1"),
            A: rgba("#9e75f5"), c: rgba("#51d3ce")
        },
        lines: [
            "................",
            ".....kllk.......",
            "...klWWldk......",
            "..klpPPpdkl.....",
            ".klpPABbpdkd....",
            ".kWPAcBbbpk.l...",
            ".klPABBBbpk..d..",
            ".klPBBBBbpk..l..",
            ".kdpbBBBPPk..d..",
            "..kdpbBPdk..ld..",
            "...kddddk..ld...",
            "...........l....",
            "..........ld....",
            ".........ld.....",
            "........kWk.....",
            "................"
        ]
    },
    synaptic_neural_berry: {
        palette: {
            ".": TRANSPARENT,
            1: rgba("#001341"), 2: rgba("#002d8e"), 3: rgba("#003daf"),
            4: rgba("#0058d8"), 5: rgba("#007def"), 6: rgba("#009bf5"),
            7: rgba("#00c5f8"), 8: rgba("#38fbf5"), A: rgba("#123a08"),
            B: rgba("#245908"), C: rgba("#337f08"), D: rgba("#54a803"),
            E: rgba("#9ef752")
        },
        lines: [
            "................",
            "...BB..CE.......",
            "...AEDCD........",
            ".....ACBDD......",
            "..1477ACAA41....",
            ".137853356731...",
            ".258776678651...",
            ".156766666651...",
            ".1456646654251..",
            ".1357874576241..",
            ".1236687465221..",
            "..12566513411...",
            "...125541122....",
            "....1332211.....",
            ".....2131.......",
            "................"
        ]
    },
    bio_pulse_belt: {
        palette: {
            ".": TRANSPARENT,
            1: rgba("#192436"), 2: rgba("#1c2a46"), 3: rgba("#48596a"),
            4: rgba("#081738"), 5: rgba("#5d6d79"), 6: rgba("#7a8691"),
            7: rgba("#aab4bb"), 8: rgba("#2a3650"), 9: rgba("#364460"),
            A: rgba("#697581"), B: rgba("#167b87"), C: rgba("#da2626")
        },
        lines: [
            "................",
            "................",
            "................",
            "....111111111...",
            "..112234356711..",
            ".12833498567271.",
            "11833349843A3311",
            "2B44411111185961",
            "24BBB41111112681",
            "24C9B49387A34A31",
            "24CCB48793A73831",
            ".42B47691897431.",
            "..449619189281..",
            "....111111111...",
            "................",
            "................"
        ]
    },
    life_support_pack: {
        palette: {
            ".": TRANSPARENT,
            k: rgba("#101b16"), d: rgba("#19271d"), g: rgba("#263527"),
            G: rgba("#485632"), L: rgba("#6f7846"), m: rgba("#3f4748"),
            M: rgba("#778082"), W: rgba("#c1c7c5"), c: rgba("#167d80"),
            C: rgba("#51d3ce"), X: rgba("#a1f3ea")
        },
        lines: [
            "................",
            ".....kkkkkk.....",
            "....kM....Mk....",
            "..kMWLLLLLLWMk..",
            ".kWMGGGGGGGGMWk.",
            ".kGWmLLLLLLmWGk.",
            ".kdmkggggggkmdk.",
            ".kGMGkkkkkkGMGk.",
            ".kGmGkgccgkGmGk.",
            ".kGmGkgCCgkGmGk.",
            ".kGmGkCXXCkGmGk.",
            ".kGMGkgCCgkGMGk.",
            ".kGGGkkkkkkGGGk.",
            ".kMWmGGGGGGmWMk.",
            "..kkddddddddkk..",
            "................"
        ]
    }
};

function materialBottle(liquid) {
    const palette = {
        ".": TRANSPARENT,
        ...liquid,
        o: rgba("#607786"),
        s: rgbaAlpha("#8397a3", 208),
        g: rgbaAlpha("#b7c5cc", 48),
        v: rgbaAlpha("#dce5e8", 104),
        W: rgbaAlpha("#f8fbfb", 192),
        q: rgba("#8d8a80"),
        Q: rgba("#d7d4ca"),
        R: rgba("#f3f1eb"),
        n: rgba("#073a8d"),
        B: rgba("#1165d2"),
        H: rgba("#438fe8")
    };
    const body = [
        [6, 5, 10], [7, 4, 11], [8, 3, 12], [9, 2, 13],
        [10, 2, 13], [11, 2, 13], [12, 3, 12], [13, 4, 11], [14, 6, 9]
    ];
    const lines = buildPattern(16, ({ set, span }) => {
        span(1, 6, 9, "q");
        span(2, 5, 10, "q");
        span(2, 6, 9, "Q");
        span(2, 7, 8, "R");
        span(3, 6, 9, "o");
        span(3, 7, 8, "g");
        set(7, 3, "W");

        span(4, 5, 10, "n");
        span(4, 6, 9, "B");
        set(7, 4, "H");
        span(5, 6, 9, "o");
        span(5, 7, 8, "g");
        set(9, 5, "B");
        set(10, 5, "B");

        for (const [y, left, right] of body) {
            span(y, left, right, "g");
            set(left, y, "o");
            set(right, y, "o");
            if (right - left >= 7) set(right - 1, y, "s");
        }

        set(6, 6, "W");
        span(7, 5, 6, "W");
        span(8, 4, 6, "W");
        span(9, 3, 5, "v");
        set(4, 9, "W");

        for (const [y, left, right] of body.filter(([y]) => y >= 10)) {
            span(y, left + 1, right - 1, "I");
            if (right - left >= 7) {
                set(left + 1, y, "i");
                set(right - 1, y, "i");
            }
        }
        span(10, 3, 12, "J");
        span(11, 3, 4, "J");
        set(4, 12, "J");
        set(5, 13, "i");
        span(14, 6, 9, "s");
        set(6, 14, "o");
        set(9, 14, "o");
    });
    return { palette, lines };
}

function serumBottle(colors) {
    const palette = { ".": TRANSPARENT, ...colors };
    const lines = [
        "................",
        "......ld.dl.....",
        ".....ldldlk.....",
        "....l.kdmddk....",
        "....l.kmlmd.....",
        "....d.dabck.....",
        ".....ddaccdk....",
        "......kcbek.k...",
        "......dceedml...",
        "......decfd.l...",
        "......kefhkdd...",
        "......kfhhk.k...",
        ".....kkhhikk....",
        "......kkkkk.....",
        ".......kdk......",
        "................"
    ];
    return { palette, lines };
}

Object.assign(patterns, {
    biochemical_solution: materialBottle({
        i: rgba("#0c6b34"), I: rgba("#2bbf56"), J: rgba("#7be86a")
    }),
    industrial_ethanol: materialBottle({
        i: rgba("#9c7418"), I: rgba("#e1bb43"), J: rgba("#f7e48a")
    }),
    purified_water_bottle: materialBottle({
        i: rgba("#3b98ae"), I: rgba("#9ce3eb"), J: rgba("#e0f8f8")
    }),
    synaptic_serum_s01: serumBottle({
        k: rgba("#3b1415"), d: rgba("#632625"), m: rgba("#78332e"), l: rgba("#9a4236"),
        a: rgba("#fef769"), b: rgba("#ffe141"), c: rgba("#fac140"), e: rgba("#f3893d"),
        f: rgba("#d24e4c"), h: rgba("#9e194e"), i: rgba("#a80244")
    }),
    synaptic_serum_s02: serumBottle({
        k: rgba("#1e2d35"), d: rgba("#31434e"), m: rgba("#4e6070"), l: rgba("#607381"),
        a: rgba("#52f5ff"), b: rgba("#20dbf0"), c: rgba("#39bce8"), e: rgba("#6e82ed"),
        f: rgba("#6f5ee6"), h: rgba("#6b35df"), i: rgba("#6e18d8")
    }),
    synaptic_serum_s03: serumBottle({
        k: rgba("#170825"), d: rgba("#3e1561"), m: rgba("#4e2662"), l: rgba("#6c3793"),
        a: rgba("#ff4df6"), b: rgba("#ff19f5"), c: rgba("#fe36b0"), e: rgba("#fe26a1"),
        f: rgba("#e81a76"), h: rgba("#b60061"), i: rgba("#750031")
    })
});

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

function png(data, size = SIZE) {
    const header = Buffer.alloc(13);
    header.writeUInt32BE(size, 0);
    header.writeUInt32BE(size, 4);
    header[8] = 8;
    header[9] = 6;
    const scanlines = Buffer.alloc((size * 4 + 1) * size);
    for (let y = 0; y < size; y++) {
        const row = y * (size * 4 + 1);
        scanlines[row] = 0;
        Buffer.from(data.buffer, data.byteOffset + y * size * 4, size * 4).copy(scanlines, row + 1);
    }
    return Buffer.concat([
        Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]),
        chunk("IHDR", header),
        chunk("IDAT", deflateSync(scanlines, { level: 9 })),
        chunk("IEND", Buffer.alloc(0))
    ]);
}

function save(relativePath, data, size = SIZE) {
    const path = resolve("src/main/resources/assets/cybercultivator/textures", relativePath);
    mkdirSync(dirname(path), { recursive: true });
    writeFileSync(path, png(data, size));
    console.log(path);
}

save("block/silicon_ore.png", ore(SILICON_OVERLAY, {
    a: rgba("#365c61"), b: rgba("#4b898f"), c: rgba("#68b9be"), C: rgba("#9adadd"), d: rgba("#d0eeee")
}));
save("block/rare_earth_ore.png", ore(RARE_EARTH_OVERLAY, {
    a: rgba("#684d35"), b: rgba("#926a40"), c: rgba("#be8749"), A: rgba("#dfb46d"), y: rgba("#f1d99b"), v: rgba("#88718b")
}));
for (const [name, definition] of Object.entries({
    raw_silicon_block: RAW_SILICON_BLOCK,
    raw_rare_earth_block: RAW_RARE_EARTH_BLOCK,
    silicon_block: SILICON_BLOCK,
    rare_earth_block: RARE_EARTH_BLOCK
})) {
    const data = image();
    drawPattern(data, definition.lines, definition.palette);
    save(`block/${name}.png`, data);
}

for (const [name, definition] of Object.entries(patterns)) {
    const size = definition.size ?? SIZE;
    const data = image(size);
    const [offsetX, offsetY] = definition.offset ?? [0, 0];
    const shifted = Array.from({ length: size }, () => Array(size).fill("."));
    for (let y = 0; y < size; y++) {
        for (let x = 0; x < size; x++) {
            const targetX = x + offsetX;
            const targetY = y + offsetY;
            if (targetX >= 0 && targetX < size && targetY >= 0 && targetY < size) {
                shifted[targetY][targetX] = definition.lines[y][x];
            }
        }
    }
    drawPattern(data, shifted.map(row => row.join("")), definition.palette);
    save(`item/${name}.png`, data, size);
}
