package online.vanillaway.glow;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

/**
 * %vwglow_color% — код цвета свечения прямо сейчас (&b, &c ...) или пусто. Ставится в конец tagprefix в TAB.
 * %vwglow_name%  — название цвета прямо сейчас (Голубой) или пусто.
 * %vwglow_mode%  — выбранный режим: aqua / rainbow / glitter / off.
 */
final class GlowPlaceholders extends PlaceholderExpansion {

    private final VWGlow plugin;

    GlowPlaceholders(VWGlow plugin) {
        this.plugin = plugin;
    }

    @Override public String getIdentifier() { return "vwglow"; }
    @Override public String getAuthor() { return "VanillaWay"; }
    @Override public String getVersion() { return plugin.getPluginMeta().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null) return "";
        GlowColor shown = plugin.manager().shown(player.getUniqueId());
        return switch (params.toLowerCase()) {
            case "color" -> shown == null ? "" : shown.legacy();
            case "name" -> shown == null ? "" : shown.title();
            case "mode" -> {
                if (!(player instanceof org.bukkit.entity.Player online)) yield "off";
                String m = plugin.manager().mode(online);
                yield m == null ? "off" : m;
            }
            default -> null;
        };
    }
}
