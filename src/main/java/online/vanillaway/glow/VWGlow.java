package online.vanillaway.glow;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class VWGlow extends JavaPlugin implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private GlowManager manager;
    private boolean floodgate;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        manager = new GlowManager(this);
        floodgate = Bukkit.getPluginManager().isPluginEnabled("floodgate");

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new GlowMenu.Listener(this), this);

        GlowCommand command = new GlowCommand(this);
        PluginCommand glow = getCommand("glow");
        if (glow != null) {
            glow.setExecutor(command);
            glow.setTabCompleter(command);
        }

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new GlowPlaceholders(this).register();
            getLogger().info("Плейсхолдеры: %vwglow_color%, %vwglow_name%, %vwglow_mode%");
        }

        manager.start();
        getLogger().info("Bedrock-формы: " + (floodgate ? "да (Floodgate)" : "нет"));
    }

    @Override
    public void onDisable() {
        if (manager != null) manager.stop();
    }

    public void reload() {
        manager.stop();
        reloadConfig();
        manager.start();
    }

    public GlowManager manager() { return manager; }

    public boolean floodgate() { return floodgate; }

    // ---------------------------------------------------------------- сообщения

    /** Текст из config.yml (messages.<key>) в MiniMessage. */
    public Component text(String key, TagResolver... resolvers) {
        String raw = getConfig().getString("messages." + key, "<red>messages." + key);
        return MM.deserialize(raw, resolvers).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    /** Сообщение игроку с префиксом. */
    public void send(CommandSender to, String key, TagResolver... resolvers) {
        String raw = getConfig().getString("messages." + key, "");
        if (raw.isEmpty()) return;
        Component prefix = MM.deserialize(getConfig().getString("messages.prefix", ""));
        to.sendMessage(prefix.append(MM.deserialize(raw, resolvers)));
    }

    // ---------------------------------------------------------------- вход/выход

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        manager.load(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        manager.unload(e.getPlayer());
    }
}
