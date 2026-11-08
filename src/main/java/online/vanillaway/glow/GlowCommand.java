package online.vanillaway.glow;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * /glow — меню; /glow <цвет|rainbow|glitter|off> — выбрать сразу;
 * /glow set <игрок> <режим> и /glow reload — для админов (vwglow.admin).
 */
public final class GlowCommand implements TabExecutor {

    private final VWGlow plugin;

    public GlowCommand(VWGlow plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("vwglow.admin")) return noPerm(sender);
            plugin.reload();
            plugin.send(sender, "reloaded");
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("set")) {
            if (!sender.hasPermission("vwglow.admin")) return noPerm(sender);
            if (args.length < 3) {
                plugin.send(sender, "admin-usage");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                plugin.send(sender, "player-not-found", Placeholder.unparsed("player", args[1]));
                return true;
            }
            String mode = args[2].toLowerCase(Locale.ROOT);
            if (!GlowManager.OFF.equals(mode) && !plugin.manager().isValidMode(mode)) {
                plugin.send(sender, "unknown-mode", Placeholder.unparsed("mode", args[2]));
                return true;
            }
            plugin.manager().set(target, mode);
            plugin.send(sender, "set-other",
                    Placeholder.unparsed("player", target.getName()),
                    Placeholder.component("glow", modeName(plugin, mode)));
            return true;
        }

        if (!(sender instanceof Player player)) {
            plugin.send(sender, "players-only");
            return true;
        }
        if (!player.hasPermission("vwglow.use")) return noPerm(player);

        if (args.length == 0) {
            GlowMenu.open(plugin, player);
            return true;
        }

        String mode = args[0].toLowerCase(Locale.ROOT);
        if (!GlowManager.OFF.equals(mode) && !plugin.manager().isValidMode(mode)) {
            plugin.send(player, "usage");
            return true;
        }
        choose(plugin, player, mode);
        return true;
    }

    private boolean noPerm(CommandSender sender) {
        plugin.send(sender, "no-permission");
        return true;
    }

    /** Выбор режима игроком: проверяет права, сохраняет, пишет сообщение и играет звук. */
    public static boolean choose(VWGlow plugin, Player p, String mode) {
        if (GlowManager.OFF.equals(mode)) {
            plugin.manager().set(p, null);
            plugin.send(p, "disabled");
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 0.8f);
            return true;
        }
        if (!plugin.manager().canUse(p, mode)) {
            plugin.send(p, "locked", Placeholder.unparsed("rank", rankFor(plugin, mode)));
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 1f);
            return false;
        }
        plugin.manager().set(p, mode);
        plugin.send(p, "selected", Placeholder.component("glow", modeName(plugin, mode)));
        p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.4f);
        return true;
    }

    /** С какого ранга доступен режим — для подсказки в меню и сообщении. */
    public static String rankFor(VWGlow plugin, String mode) {
        GlowColor c = GlowColor.byId(mode);
        if (c != null && plugin.getConfig().getStringList("mvp-colors").contains(c.id())) {
            return plugin.getConfig().getString("rank-names.basic", "MVP");
        }
        return plugin.getConfig().getString("rank-names.full", "MVP+");
    }

    /** Красивое название режима для сообщений. */
    public static Component modeName(VWGlow plugin, String mode) {
        if (mode == null || GlowManager.OFF.equals(mode)) return plugin.text("name-off");
        if (GlowManager.RAINBOW.equals(mode)) return plugin.text("name-rainbow");
        if (GlowManager.GLITTER.equals(mode)) return plugin.text("name-glitter");
        GlowColor c = GlowColor.byId(mode);
        return c == null ? Component.text(mode) : Component.text(c.title(), c.display());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            if (sender instanceof Player p) {
                for (GlowColor c : GlowColor.values()) if (plugin.manager().canUse(p, c.id())) out.add(c.id());
                if (plugin.manager().canUse(p, GlowManager.RAINBOW)) out.add(GlowManager.RAINBOW);
                if (plugin.manager().canUse(p, GlowManager.GLITTER)) out.add(GlowManager.GLITTER);
                out.add(GlowManager.OFF);
            }
            if (sender.hasPermission("vwglow.admin")) {
                out.add("set");
                out.add("reload");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("set") && sender.hasPermission("vwglow.admin")) {
            for (Player p : Bukkit.getOnlinePlayers()) out.add(p.getName());
        } else if (args.length == 3 && args[0].equalsIgnoreCase("set") && sender.hasPermission("vwglow.admin")) {
            for (GlowColor c : GlowColor.values()) out.add(c.id());
            out.add(GlowManager.RAINBOW);
            out.add(GlowManager.GLITTER);
            out.add(GlowManager.OFF);
        }
        String last = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        out.removeIf(s -> !s.toLowerCase(Locale.ROOT).startsWith(last));
        return out;
    }

    /** Для подстановки в меню, где нужен ресолвер с названием. */
    static TagResolver glowTag(VWGlow plugin, String mode) {
        return Placeholder.component("glow", modeName(plugin, mode));
    }
}
