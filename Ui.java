package sharkus.extras;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Gemeinsame Zeichen-Helfer für das Design (Karten, Schalter, Verläufe, Akzentfarbe). */
final class Ui {
    static final int[] ACCENTS = {0x3FA9F5, 0x3DDC84, 0xA56BFF, 0xFF5FA2, 0xFFA23D, 0xFF5555};

    private Ui() { }

    static int accent() {
        return 0xFF000000 | ACCENTS[Math.floorMod(Config.I.accent, ACCENTS.length)];
    }

    static int mix(int a, int b, double t) {
        t = Math.max(0, Math.min(1, t));
        int aa = (int) (((a >>> 24) & 255) * (1 - t) + ((b >>> 24) & 255) * t);
        int ar = (int) (((a >>> 16) & 255) * (1 - t) + ((b >>> 16) & 255) * t);
        int ag = (int) (((a >>> 8) & 255) * (1 - t) + ((b >>> 8) & 255) * t);
        int ab = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return (aa << 24) | (ar << 16) | (ag << 8) | ab;
    }

    static void gradientV(GuiGraphicsExtractor g, int x, int y, int w, int h, int top, int bot) {
        for (int i = 0; i < h; i++) {
            g.fill(x, y + i, x + w, y + i + 1, mix(top, bot, i / (double) Math.max(1, h - 1)));
        }
    }

    /** Abgerundetes Rechteck (Radius r). */
    static void round(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int col) {
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        g.fill(x + r, y, x + w - r, y + h, col);
        for (int i = 0; i < r; i++) {
            double d = r - i - 0.5;
            int inset = (int) Math.round(r - Math.sqrt(r * r - d * d));
            g.fill(x + i, y + inset, x + i + 1, y + h - inset, col);
            g.fill(x + w - 1 - i, y + inset, x + w - i, y + h - inset, col);
        }
    }

    /** Karte mit Schatten, Rand in Akzentfarbe und dunklem Hintergrund. */
    static void card(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        round(g, x - 3, y - 3, w + 6, h + 6, 9, 0x44000000);
        round(g, x - 1, y - 1, w + 2, h + 2, 7, mix(0xFF0B0E16, accent(), 0.55));
        round(g, x, y, w, h, 6, 0xF2121621);
    }

    /** Schalter im Pillen-Stil (an = Akzentfarbe, aus = grau). */
    static void pill(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean on) {
        round(g, x, y, w, h, h / 2, on ? accent() : 0xFF4A5062);
        int k = h - 4;
        int kx = on ? x + w - k - 2 : x + 2;
        round(g, kx, y + 2, k, k, k / 2, 0xFFFFFFFF);
    }

    /** Dunkler Hintergrund + Karte für die Unterbildschirme. */
    static void backdrop(GuiGraphicsExtractor g, int width, int height) {
        g.fill(0, 0, width, height, 0xA0080A10);
        int cw = Math.min(400, width - 4);
        card(g, (width - cw) / 2, 3, cw, height - 6);
        g.fill((width - cw) / 2 + 3, 5, (width + cw) / 2 - 3, 7, accent());
    }
}
