package online.vanillaway.glow;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Хранит выбор игрока и включает/выключает свечение.
 *
 * Режим — это id цвета (aqua, red ...), "rainbow" или "glitter". Нет режима — свечение выключено.
 * Выбор хранится в самом игроке (PersistentDataContainer), отдельная база не нужна.
 *
 * Цвет свечения в Minecraft = цвет команды (scoreboard team) игрока.
 *  - Режим TAB: командами управляет TAB, цвет отдаём через плейсхолдер %vwglow_color%,
 *    который стоит в конце tagprefix (TAB берёт цвет команды из последнего цвета префикса).
 *  - Режим TEAM: TAB нет — сами раскладываем игроков по командам vwglow_<код>.
 */
public final class GlowManager {

    public static final String RAINBOW = "rainbow";
    public static final String GLITTER = "glitter";
    public static final String OFF = "off";

    private static final GlowColor[] RAINBOW_ORDER = {
            GlowColor.RED, GlowColor.GOLD, GlowColor.YELLOW, GlowColor.GREEN,
            GlowColor.AQUA, GlowColor.BLUE, GlowColor.LIGHT_PURPLE
    };

    public enum ColorMode { TAB, TEAM }

    private final VWGlow plugin;
    private final NamespacedKey key;
    private final NamespacedKey litKey;
    private final Map<UUID, String> modes = new ConcurrentHashMap<>();
    private final Map<UUID, GlowColor> shown = new ConcurrentHashMap<>();
    private final Random random = new Random();

    private static final int TICK = 2; // таймер срабатывает раз в 2 тика (0.1 сек)

    private ColorMode colorMode = ColorMode.TEAM;
    private List<GlowColor> glitterColors = List.of(RAINBOW_ORDER);
    private BukkitTask task;
    private int rainbowInterval = 20;
    private int glitterInterval = 4;
    private int rainbowTimer;
    private int glitterTimer;
    private int rainbowStep;

    public GlowManager(VWGlow plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "mode");
        this.litKey = new NamespacedKey(plugin, "lit");
    }

    // ---------------------------------------------------------------- жизненный цикл

    public void start() {
        String cfg = plugin.getConfig().getString("color-mode", "auto").toLowerCase();
        boolean tabPresent = Bukkit.getPluginManager().isPluginEnabled("TAB");
        colorMode = switch (cfg) {
            case "tab" -> ColorMode.TAB;
            case "team" -> ColorMode.TEAM;
            default -> tabPresent ? ColorMode.TAB : ColorMode.TEAM;
        };
        if (colorMode == ColorMode.TAB && !Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            plugin.getLogger().warning("Режим TAB, но PlaceholderAPI не найден — цвет будет белым. Поставь PlaceholderAPI или color-mode: team.");
        }
        plugin.getLogger().info("Цвет свечения: " + (colorMode == ColorMode.TAB
                ? "через TAB (%vwglow_color% в tagprefix)"
                : "своими командами scoreboard"));

        List<GlowColor> list = new ArrayList<>();
        for (String id : plugin.getConfig().getStringList("glitter-colors")) {
            GlowColor c = GlowColor.byId(id);
            if (c != null) list.add(c);
        }
        if (!list.isEmpty()) glitterColors = List.copyOf(list);

        rainbowInterval = Math.max(TICK, plugin.getConfig().getInt("rainbow-interval-ticks", 20));
        glitterInterval = Math.max(TICK, plugin.getConfig().getInt("glitter-interval-ticks", 4));
        if (task != null) task.cancel();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, TICK, TICK);

        for (Player p : Bukkit.getOnlinePlayers()) load(p);
    }

    public void stop() {
        if (task != null) task.cancel();
        task = null;
        for (Player p : Bukkit.getOnlinePlayers()) hide(p);
        shown.clear();
    }

    public ColorMode colorMode() { return colorMode; }

    // ---------------------------------------------------------------- игроки

    public void load(Player p) {
        String m = p.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (m != null && !m.isEmpty()) {
            modes.put(p.getUniqueId(), m);
        } else if (p.getPersistentDataContainer().has(key)) {
            p.getPersistentDataContainer().remove(key);
        }
        refresh(p);
    }

    public void unload(Player p) {
        // Снимаем флаг до сохранения игрока: если плагин когда-нибудь удалят, никто не останется светиться навсегда.
        hide(p);
        modes.remove(p.getUniqueId());
    }

    /** Текущий выбранный режим или null. */
    public String mode(Player p) {
        return modes.get(p.getUniqueId());
    }

    /** Цвет, которым игрок светится прямо сейчас (для плейсхолдера), или null. */
    public GlowColor shown(UUID id) {
        return shown.get(id);
    }

    public boolean canUse(Player p, String mode) {
        if (mode == null) return false;
        if (RAINBOW.equals(mode)) return p.hasPermission("vwglow.effect.rainbow");
        if (GLITTER.equals(mode)) return p.hasPermission("vwglow.effect.glitter");
        GlowColor c = GlowColor.byId(mode);
        return c != null && p.hasPermission(c.permission());
    }

    public boolean isValidMode(String mode) {
        return RAINBOW.equals(mode) || GLITTER.equals(mode) || GlowColor.byId(mode) != null;
    }

    /** Сохраняет выбор (без проверки прав — проверяет вызывающий). null или "off" — выключить. */
    public void set(Player p, String mode) {
        if (mode == null || OFF.equals(mode)) {
            modes.remove(p.getUniqueId());
            p.getPersistentDataContainer().remove(key);
        } else {
            modes.put(p.getUniqueId(), mode);
            p.getPersistentDataContainer().set(key, PersistentDataType.STRING, mode);
        }
        refresh(p);
    }

    // ---------------------------------------------------------------- показ

    /**
     * Радуга и глиттер идут каждый в своём темпе:
     * радуга — спокойно по кругу (по умолчанию 1 цвет в секунду),
     * глиттер — быстро и вразнобой (по умолчанию 5 раз в секунду).
     */
    private void tick() {
        rainbowTimer += TICK;
        if (rainbowTimer >= rainbowInterval) {
            rainbowTimer = 0;
            rainbowStep++;
        }
        glitterTimer += TICK;
        boolean reroll = glitterTimer >= glitterInterval;
        if (reroll) glitterTimer = 0;
        for (Player p : Bukkit.getOnlinePlayers()) refresh(p, reroll);
    }

    /** Включает, перекрашивает или выключает свечение по текущему выбору, правам и состоянию игрока. */
    public void refresh(Player p) {
        refresh(p, false);
    }

    private void refresh(Player p, boolean reroll) {
        String m = modes.get(p.getUniqueId());
        if (m == null || !canUse(p, m) || isHidden(p)) {
            hide(p);
            return;
        }
        GlowColor c;
        if (RAINBOW.equals(m)) {
            c = RAINBOW_ORDER[Math.floorMod(rainbowStep, RAINBOW_ORDER.length)];
        } else if (GLITTER.equals(m)) {
            GlowColor now = shown.get(p.getUniqueId());
            c = (reroll || now == null || !glitterColors.contains(now)) ? randomGlitter(now) : now;
        } else {
            c = GlowColor.byId(m);
        }
        if (c == null) {
            hide(p);
            return;
        }
        show(p, c);
    }

    /** Случайный цвет глиттера, но не тот же, что сейчас — иначе мерцание «замирает». */
    private GlowColor randomGlitter(GlowColor now) {
        if (glitterColors.size() == 1) return glitterColors.get(0);
        GlowColor c;
        do {
            c = glitterColors.get(random.nextInt(glitterColors.size()));
        } while (c == now);
        return c;
    }

    private void show(Player p, GlowColor c) {
        GlowColor before = shown.put(p.getUniqueId(), c);
        if (!p.isGlowing()) p.setGlowing(true);
        if (before == null) p.getPersistentDataContainer().set(litKey, PersistentDataType.BYTE, (byte) 1);
        if (colorMode == ColorMode.TEAM && before != c) joinTeam(p, c);
    }

    /**
     * Выключает только то свечение, которое включили мы. Чужое (другие плагины) не трогаем.
     * Метка lit в данных игрока переживает падение сервера: после рестарта «залипшее» свечение снимется.
     */
    private void hide(Player p) {
        boolean had = shown.remove(p.getUniqueId()) != null;
        boolean marked = p.getPersistentDataContainer().has(litKey);
        if (!had && !marked) return;
        p.setGlowing(false);
        p.getPersistentDataContainer().remove(litKey);
        if (colorMode == ColorMode.TEAM) leaveTeams(p);
    }

    /** В ваниш-режиме и в наблюдателе свечение не показываем. */
    private boolean isHidden(Player p) {
        if (p.getGameMode() == GameMode.SPECTATOR) return true;
        for (MetadataValue v : p.getMetadata("vanished")) {
            if (v.asBoolean()) return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- команды scoreboard (режим без TAB)

    private Scoreboard board() {
        return Bukkit.getScoreboardManager().getMainScoreboard();
    }

    private void joinTeam(Player p, GlowColor c) {
        String name = "vwglow_" + c.code();
        Team team = board().getTeam(name);
        if (team == null) team = board().registerNewTeam(name);
        team.color(c.text());
        if (!team.hasEntry(p.getName())) team.addEntry(p.getName()); // из прошлой команды игрок уходит сам
    }

    private void leaveTeams(Player p) {
        for (GlowColor c : GlowColor.values()) {
            Team team = board().getTeam("vwglow_" + c.code());
            if (team != null && team.hasEntry(p.getName())) team.removeEntry(p.getName());
        }
    }
}
