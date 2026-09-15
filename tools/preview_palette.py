from PIL import Image, ImageDraw, ImageFont

SCHEME_BACKGROUND = "#1e1f22"
SCHEME_FOREGROUND = "#bcbec4"
SCHEME_SYNTAX = ["#7a7e85", "#cf8e6d", "#6aab73", "#2aacb8", "#56a8f5"]
IDE_PANEL_BACKGROUND = "#2b2d30"
IDE_BORDER = "#393b40"
IDE_LABEL_FOREGROUND = "#dfe1e5"

PANEL_WIDTH = 380
PANEL_HEIGHT = 260
SWATCH_HEIGHT = 26
LABEL_HEIGHT = 30
MARGIN = 18


def to_rgb(value):
    value = value.lstrip("#")
    return tuple(int(value[i:i + 2], 16) for i in (0, 2, 4))


def luminance(color):
    return (0.2126 * color[0] + 0.7152 * color[1] + 0.0722 * color[2]) / 255.0


def blend(first, second, ratio):
    ratio = max(0.0, min(1.0, ratio))
    return tuple(int(first[i] + (second[i] - first[i]) * ratio) for i in range(3))


def sample_at(ramp, position):
    position = max(0.0, min(1.0, position))
    scaled = position * (len(ramp) - 1)
    lower = int(scaled)
    upper = min(lower + 1, len(ramp) - 1)
    return blend(ramp[lower], ramp[upper], scaled - lower)


def resample(ramp, steps, floor=0.0, ceiling=1.0):
    if len(ramp) == 1:
        return [ramp[0]] * steps
    last_index = max(steps - 1, 1)
    return [sample_at(ramp, floor + (ceiling - floor) * index / last_index) for index in range(steps)]


def mono_ramp():
    return [to_rgb(SCHEME_BACKGROUND), to_rgb(SCHEME_FOREGROUND)]


def syntax_ramp():
    collected = [to_rgb(SCHEME_BACKGROUND)] + [to_rgb(c) for c in SCHEME_SYNTAX] + [to_rgb(SCHEME_FOREGROUND)]
    return sorted(set(collected), key=luminance)


def ide_chrome_ramp():
    return sorted({to_rgb(IDE_PANEL_BACKGROUND), to_rgb(IDE_BORDER), to_rgb(IDE_LABEL_FOREGROUND)}, key=luminance)


def build_palette(ramp, steps, floor, ceiling, reverse):
    palette = resample(ramp, steps, floor, ceiling)
    return list(reversed(palette)) if reverse else palette


def build_sample():
    image = Image.new("RGB", (PANEL_WIDTH, PANEL_HEIGHT), (255, 255, 255))
    draw = ImageDraw.Draw(image)

    draw.rectangle([0, 0, PANEL_WIDTH, 44], fill=(45, 127, 249))
    draw.rectangle([16, 16, 120, 30], fill=(255, 255, 255))

    draw.rectangle([16, 62, 300, 78], fill=(26, 26, 26))
    draw.rectangle([16, 88, 350, 98], fill=(107, 114, 128))
    draw.rectangle([16, 106, 330, 116], fill=(107, 114, 128))
    draw.rectangle([16, 124, 280, 134], fill=(107, 114, 128))

    for x in range(16, 180):
        for y in range(150, 230):
            ratio_x = (x - 16) / 164
            ratio_y = (y - 150) / 80
            image.putpixel((x, y), (int(230 * ratio_x + 20), int(90 + 120 * ratio_y), int(200 - 150 * ratio_x)))

    draw.rectangle([200, 150, 300, 178], fill=(34, 197, 94))
    draw.rectangle([200, 194, 360, 204], fill=(45, 127, 249))
    draw.rectangle([200, 212, 340, 222], fill=(45, 127, 249))

    return image


def quantize(image, table):
    steps = len(table)
    result = Image.new("RGB", image.size)
    source = image.load()
    target = result.load()

    for y in range(image.size[1]):
        for x in range(image.size[0]):
            red, green, blue = source[x, y]
            gray = 0.213 * red + 0.715 * green + 0.072 * blue
            index = min(int(gray / 255.0 * steps), steps - 1)
            target[x, y] = table[index]

    return result


def load_font(size):
    for path in ("C:/Windows/Fonts/consola.ttf", "C:/Windows/Fonts/segoeui.ttf"):
        try:
            return ImageFont.truetype(path, size)
        except OSError:
            continue
    return ImageFont.load_default()


def draw_swatches(canvas, table, origin):
    width = PANEL_WIDTH // len(table)
    draw = ImageDraw.Draw(canvas)

    for index, color in enumerate(table):
        left = origin[0] + index * width
        right = left + width if index < len(table) - 1 else origin[0] + PANEL_WIDTH
        draw.rectangle([left, origin[1], right, origin[1] + SWATCH_HEIGHT], fill=color)


def main():
    steps = 6
    sample = build_sample()

    configurations = [
        ("Original page", None),
        ("NEW DEFAULT  mono x6  0-70%  reversed", build_palette(mono_ramp(), steps, 0.0, 0.70, True)),
        ("mono x6  0-100%  reversed  (ceiling too high)", build_palette(mono_ramp(), steps, 0.0, 1.00, True)),
        ("IDE chrome x6  0-70%  reversed", build_palette(ide_chrome_ramp(), steps, 0.0, 0.70, True)),
        ("syntax x6  0-70%  reversed", build_palette(syntax_ramp(), steps, 0.0, 0.70, True)),
        ("mono x4  0-45%  reversed  (most subtle)", build_palette(mono_ramp(), 4, 0.0, 0.45, True)),
    ]

    variants = []
    for label, table in configurations:
        if table is None:
            variants.append((label, sample, None))
            continue
        variants.append((label, quantize(sample, table), table))

    columns = 3
    rows = 2
    cell_width = PANEL_WIDTH + MARGIN
    cell_height = LABEL_HEIGHT + PANEL_HEIGHT + SWATCH_HEIGHT + MARGIN
    canvas = Image.new("RGB", (columns * cell_width + MARGIN, rows * cell_height + MARGIN), to_rgb(IDE_PANEL_BACKGROUND))
    draw = ImageDraw.Draw(canvas)
    font = load_font(13)

    for index, (label, image, table) in enumerate(variants):
        column = index % columns
        row = index // columns
        left = MARGIN + column * cell_width
        top = MARGIN + row * cell_height

        draw.text((left, top + 6), label, fill=to_rgb(IDE_LABEL_FOREGROUND), font=font)
        canvas.paste(image, (left, top + LABEL_HEIGHT))

        if table is not None:
            draw_swatches(canvas, table, (left, top + LABEL_HEIGHT + PANEL_HEIGHT))

    canvas.save("tools/palette_preview.png")
    print("saved tools/palette_preview.png  (canvas = IDE tool window background #2b2d30)")

    for label, table in configurations:
        if table is None:
            continue
        formatted = " ".join("#%02x%02x%02x" % color for color in table)
        print(f"{label:46} {formatted}")


if __name__ == "__main__":
    main()
