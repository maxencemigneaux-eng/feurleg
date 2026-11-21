package fr.goat.feurleg;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Récupère tout le texte affiché par un menu ouvert, puis y cherche des timers
 * (ex. "41:03") avec, juste au-dessus, le nom de la zone (ex. "Johto").
 * Ça marche sans connaître le fonctionnement interne du menu /leg du serveur.
 */
public final class TextCapture {

    /** Un texte dessiné, avec sa position à l'écran (en pixels GUI). */
    public record Entry(String text, float x, float y, float scale) {}

    private static final Pattern TIMER = Pattern.compile("^(\\d{1,3}):(\\d{2})(?::(\\d{2}))?$");
    private static final Pattern FORMAT_CODES = Pattern.compile("§.");

    private static boolean capturing = false;
    private static final List<Entry> frame = new ArrayList<>();

    private static Screen lastScreen = null;
    private static boolean autoDumpedThisScreen = false;
    private static boolean dumpRequested = false;

    private TextCapture() {}

    public static boolean isCapturing() {
        return capturing;
    }

    public static void requestDump() {
        dumpRequested = true;
    }

    public static void begin(Screen screen) {
        frame.clear();
        capturing = true;
        if (screen != lastScreen) {
            lastScreen = screen;
            autoDumpedThisScreen = false;
        }
    }

    public static void record(String raw, float x, float y, Matrix4f matrix) {
        String text = FORMAT_CODES.matcher(raw).replaceAll("").trim();
        if (text.isEmpty()) return;
        Vector3f pos = matrix.transformPosition(x, y, 0f, new Vector3f());
        float scale = Math.abs(matrix.m00());
        frame.add(new Entry(text, pos.x, pos.y, scale));
    }

    public static void end(Screen screen) {
        capturing = false;
        if (frame.isEmpty()) return;

        List<Entry> entries = new ArrayList<>(frame);
        frame.clear();

        Map<String, Long> found = findTimers(entries);
        if (found.size() >= 2) {
            TimerStore.get().sync(found);
            if (!autoDumpedThisScreen) {
                autoDumpedThisScreen = true;
                writeDump(entries, screen, found, false);
            }
        }
        if (dumpRequested) {
            dumpRequested = false;
            writeDump(entries, screen, found, true);
        }
    }

    /** Associe chaque timer au texte placé juste au-dessus (le nom de la zone). */
    static Map<String, Long> findTimers(List<Entry> entries) {
        FeurlegConfig cfg = FeurlegConfig.get();
        List<Entry> timers = new ArrayList<>();
        List<Entry> labels = new ArrayList<>();
        for (Entry e : entries) {
            if (TIMER.matcher(e.text()).matches()) timers.add(e);
            else if (looksLikeLabel(e.text())) labels.add(e);
        }

        // Ordre d'affichage : de gauche à droite, puis de haut en bas.
        timers.sort(Comparator.comparingDouble((Entry e) -> Math.round(e.y() / 10f)).thenComparingDouble(Entry::x));

        Map<String, Long> result = new LinkedHashMap<>();
        for (Entry t : timers) {
            Entry best = null;
            for (Entry l : labels) {
                float dy = t.y() - l.y();
                float dx = Math.abs(t.x() - l.x());
                if (dy > 0 && dy <= cfg.labelMaxDistanceY && dx <= cfg.labelMaxDistanceX) {
                    if (best == null || l.y() > best.y()) best = l;
                }
            }
            if (best == null || result.containsKey(best.text())) continue;
            Long seconds = toSeconds(t.text(), cfg);
            if (seconds != null) result.put(best.text(), seconds);
        }
        return result;
    }

    private static boolean looksLikeLabel(String s) {
        if (s.length() < 2 || s.length() > 32) return false;
        if (s.contains("%") || s.contains(":")) return false;
        return s.chars().anyMatch(Character::isLetter);
    }

    static Long toSeconds(String s, FeurlegConfig cfg) {
        Matcher m = TIMER.matcher(s);
        if (!m.matches()) return null;
        long a = Long.parseLong(m.group(1));
        long b = Long.parseLong(m.group(2));
        if (m.group(3) != null) {
            return a * 3600 + b * 60 + Long.parseLong(m.group(3));
        }
        if ("h:mm".equalsIgnoreCase(cfg.timerFormat)) {
            return a * 3600 + b * 60;
        }
        return a * 60 + b;
    }

    private static void writeDump(List<Entry> entries, Screen screen, Map<String, Long> found, boolean manual) {
        MinecraftClient client = MinecraftClient.getInstance();
        Path file = client.runDirectory.toPath().resolve("config").resolve("feurleg-dump.txt");
        StringBuilder sb = new StringBuilder();
        sb.append("Feurleg dump - ").append(LocalDateTime.now()).append('\n');
        sb.append("Menu : ").append(screen.getClass().getName()).append('\n');
        sb.append("Titre : ").append(screen.getTitle().getString()).append('\n');
        sb.append("Timers reconnus : ").append(found).append("\n\n");
        List<Entry> sorted = new ArrayList<>(entries);
        sorted.sort(Comparator.comparingDouble(Entry::y).thenComparingDouble(Entry::x));
        for (Entry e : sorted) {
            sb.append(String.format("y=%7.1f  x=%7.1f  scale=%4.2f  | %s%n", e.y(), e.x(), e.scale(), e.text()));
        }
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
            if (manual && client.player != null) {
                client.player.sendMessage(Text.literal("§6[Feurleg]§r Texte du menu enregistré dans config/feurleg-dump.txt"), false);
            }
        } catch (IOException ex) {
            FeurlegClient.LOGGER.warn("Impossible d'écrire le dump", ex);
        }
    }
}
