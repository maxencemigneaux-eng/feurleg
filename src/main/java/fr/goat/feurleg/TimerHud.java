package fr.goat.feurleg;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.util.List;

/** Le petit panneau toujours affiché dans un coin de l'écran. */
public final class TimerHud {

    private static final int PAD = 4;
    private static final int LINE = 11;
    private static final int GAP = 10;

    private static final int COLOR_TITLE = 0xFFFF5555;
    private static final int COLOR_NAME = 0xFFDDDDDD;
    private static final int COLOR_TIME = 0xFFFFFF55;
    private static final int COLOR_SOON = 0xFFFF5555;
    private static final int COLOR_SPAWN = 0xFF55FF55;
    private static final int COLOR_STALE = 0xFF888888;

    private TimerHud() {}

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        FeurlegConfig cfg = FeurlegConfig.get();
        if (!cfg.enabled || client.player == null || client.options.hudHidden) return;
        if (client.getDebugHud().shouldShowDebugHud()) return;

        List<TimerStore.Timer> timers = TimerStore.get().list();
        TextRenderer tr = client.textRenderer;
        String title = "Légendaires";
        String hint = "ouvre /leg pour charger les timers";

        // Préparer les lignes et mesurer.
        int n = timers.size();
        String[] times = new String[n];
        int[] colors = new int[n];
        int nameW = 0, timeW = 0;
        boolean blink = (System.currentTimeMillis() / 500) % 2 == 0;
        for (int i = 0; i < n; i++) {
            TimerStore.Timer t = timers.get(i);
            long left = t.remainingMillis();
            if (left <= -cfg.staleAfterSeconds * 1000L) {
                times[i] = "/leg ?";
                colors[i] = COLOR_STALE;
            } else if (left <= 0) {
                times[i] = "Spawn !";
                colors[i] = blink ? COLOR_SPAWN : 0xFF22AA22;
            } else {
                times[i] = format(left);
                colors[i] = left <= cfg.warnSeconds * 1000L ? COLOR_SOON : COLOR_TIME;
            }
            nameW = Math.max(nameW, tr.getWidth(t.name));
            timeW = Math.max(timeW, tr.getWidth(times[i]));
        }

        int innerW = n == 0 ? tr.getWidth(hint) : Math.max(tr.getWidth(title), nameW + GAP + timeW);
        int w = innerW + PAD * 2;
        int h = PAD * 2 + LINE + (n == 0 ? LINE : n * LINE);

        float scale = Math.max(0.25f, cfg.scale);
        int screenW = ctx.getScaledWindowWidth();
        int screenH = ctx.getScaledWindowHeight();
        float sw = w * scale, sh = h * scale;
        String corner = cfg.corner == null ? "" : cfg.corner.toLowerCase();
        float x = corner.contains("droite") ? screenW - sw - cfg.offsetX : cfg.offsetX;
        float y = corner.contains("bas") ? screenH - sh - cfg.offsetY : cfg.offsetY;

        ctx.getMatrices().push();
        ctx.getMatrices().translate(x, y, 0);
        ctx.getMatrices().scale(scale, scale, 1f);

        int alpha = Math.max(0, Math.min(255, cfg.backgroundAlpha));
        ctx.fill(0, 0, w, h, (alpha << 24) | 0x101010);
        ctx.fill(0, 0, w, 1, 0xFFCC3333);

        ctx.drawTextWithShadow(tr, title, PAD, PAD, COLOR_TITLE);
        int ly = PAD + LINE;
        if (n == 0) {
            ctx.drawTextWithShadow(tr, hint, PAD, ly, COLOR_STALE);
        }
        for (int i = 0; i < n; i++) {
            ctx.drawTextWithShadow(tr, timers.get(i).name, PAD, ly, COLOR_NAME);
            ctx.drawTextWithShadow(tr, times[i], w - PAD - tr.getWidth(times[i]), ly, colors[i]);
            ly += LINE;
        }

        ctx.getMatrices().pop();
    }

    /** 41:03 ou 1:05:09 */
    public static String format(long millis) {
        long s = Math.max(0, (millis + 999) / 1000);
        long h = s / 3600, m = (s % 3600) / 60, sec = s % 60;
        if (h > 0) return String.format("%d:%02d:%02d", h, m, sec);
        return String.format("%d:%02d", m, sec);
    }
}
