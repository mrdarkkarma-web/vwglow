package online.vanillaway.glow;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.ArrayList;
import java.util.List;

/**
 * Меню /glow для игроков с Bedrock: форма со списком кнопок.
 * Класс загружается только если на сервере есть Floodgate (см. VWGlow#floodgate).
 */
final class FloodgateForms {

    private FloodgateForms() {}

    static boolean isBedrock(Player p) {
        try {
            return FloodgateApi.getInstance().isFloodgatePlayer(p.getUniqueId());
        } catch (Throwable t) {
            return false;
        }
    }

    static void open(VWGlow plugin, Player p) {
        String current = plugin.manager().mode(p);
        List<String> ids = new ArrayList<>();

        SimpleForm.Builder form = SimpleForm.builder()
                .title(legacy(plugin.getConfig().getString("bedrock.title", "Свечение")))
                .content(legacy(plugin.getConfig().getString("bedrock.content", "Сейчас: {current}"))
                        .replace("{current}", label(plugin, current)));

        // Показываем только то, что игроку доступно, — на телефоне длинный список с замками неудобен.
        for (GlowColor c : GlowColor.values()) {
            if (!plugin.manager().canUse(p, c.id())) continue;
            form.button(mark(c.id(), current) + c.section() + c.title());
            ids.add(c.id());
        }
        if (plugin.manager().canUse(p, GlowManager.RAINBOW)) {
            form.button(mark(GlowManager.RAINBOW, current) + legacy(plugin.getConfig().getString("bedrock.rainbow", "&cР&6а&eд&aу&bг&dа")));
            ids.add(GlowManager.RAINBOW);
        }
        if (plugin.manager().canUse(p, GlowManager.GLITTER)) {
            form.button(mark(GlowManager.GLITTER, current) + legacy(plugin.getConfig().getString("bedrock.glitter", "&dГлиттер")));
            ids.add(GlowManager.GLITTER);
        }
        form.button(legacy(plugin.getConfig().getString("bedrock.off", "&7Выключить свечение")));
        ids.add(GlowManager.OFF);

        form.validResultHandler(response -> {
            int i = response.clickedButtonId();
            if (i < 0 || i >= ids.size()) return;
            String mode = ids.get(i);
            // Ответ формы приходит не из основного потока сервера.
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (p.isOnline()) GlowCommand.choose(plugin, p, mode);
            });
        });

        FloodgateApi.getInstance().sendForm(p.getUniqueId(), form.build());
    }

    private static String mark(String mode, String current) {
        return mode.equals(current) ? "§l> §r" : "";
    }

    private static String label(VWGlow plugin, String mode) {
        if (mode == null) return "§7выключено";
        if (GlowManager.RAINBOW.equals(mode)) return legacy(plugin.getConfig().getString("bedrock.rainbow", "Радуга"));
        if (GlowManager.GLITTER.equals(mode)) return legacy(plugin.getConfig().getString("bedrock.glitter", "Глиттер"));
        GlowColor c = GlowColor.byId(mode);
        return c == null ? mode : c.section() + c.title();
    }

    private static String legacy(String s) {
        return s == null ? "" : s.replace('&', '§');
    }
}
