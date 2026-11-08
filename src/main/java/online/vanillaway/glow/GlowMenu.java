package online.vanillaway.glow;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Меню /glow.
 *
 *   ряд 0: рамка, по центру — «Свечение» и текущий выбор
 *   ряд 1: 7 ярких цветов
 *   ряд 2: 7 тёмных цветов
 *   ряд 3: белый и чёрный
 *   ряд 4: Радуга · Выключить · Глиттер
 *   ряд 5: рамка, по центру — «Закрыть»
 *
 * Игрокам с Bedrock (Floodgate) вместо сундука открывается форма с кнопками.
 */
public final class GlowMenu implements InventoryHolder {

    private static final int[] COLOR_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            30, 32
    };
    private static final int INFO_SLOT = 4;
    private static final int RAINBOW_SLOT = 38;
    private static final int OFF_SLOT = 40;
    private static final int GLITTER_SLOT = 42;
    private static final int CLOSE_SLOT = 49;

    private final Map<Integer, String> actions = new HashMap<>();
    private Inventory inventory;

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public static void open(VWGlow plugin, Player p) {
        if (plugin.floodgate() && FloodgateForms.isBedrock(p)) {
            FloodgateForms.open(plugin, p);
            return;
        }
        GlowMenu menu = new GlowMenu();
        menu.inventory = Bukkit.createInventory(menu, 54, plugin.text("menu-title"));
        menu.fill(plugin, p);
        p.openInventory(menu.inventory);
        p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.5f, 1.6f);
    }

    private void fill(VWGlow plugin, Player p) {
        actions.clear();
        inventory.clear();
        String current = plugin.manager().mode(p);

        // Однотонный фон: серые стёкла внутри выглядели как «дырки» в меню
        ItemStack filler = item(Material.BLACK_STAINED_GLASS_PANE, Component.text(" "), List.of(), false);
        for (int i = 0; i < 54; i++) inventory.setItem(i, filler);

        // Заголовок
        List<Component> infoLore = new ArrayList<>();
        infoLore.add(plugin.text("menu-info-current", Placeholder.component("glow", GlowCommand.modeName(plugin, current))));
        infoLore.add(Component.empty());
        infoLore.add(plugin.text("menu-info-hint"));
        inventory.setItem(INFO_SLOT, item(Material.GLOW_INK_SAC, plugin.text("menu-info-title"), infoLore, current != null));

        // Цвета
        GlowColor[] colors = GlowColor.values();
        for (int i = 0; i < colors.length && i < COLOR_SLOTS.length; i++) {
            GlowColor c = colors[i];
            boolean can = plugin.manager().canUse(p, c.id());
            boolean selected = c.id().equals(current);
            Component name = Component.text(c.title(), c.display()).decoration(TextDecoration.BOLD, true);
            inventory.setItem(COLOR_SLOTS[i], item(can ? c.icon() : Material.GRAY_DYE, name,
                    lore(plugin, c.id(), can, selected), selected));
            actions.put(COLOR_SLOTS[i], c.id());
        }

        // Эффекты и выключение
        effect(plugin, p, current, RAINBOW_SLOT, GlowManager.RAINBOW, Material.PRISMARINE_CRYSTALS, "name-rainbow", "menu-rainbow-desc");
        effect(plugin, p, current, GLITTER_SLOT, GlowManager.GLITTER, Material.AMETHYST_SHARD, "name-glitter", "menu-glitter-desc");

        List<Component> offLore = List.of(plugin.text(current == null ? "menu-off-already" : "menu-off-hint"));
        inventory.setItem(OFF_SLOT, item(Material.BARRIER, plugin.text("menu-off-title"), offLore, false));
        actions.put(OFF_SLOT, GlowManager.OFF);

        inventory.setItem(CLOSE_SLOT, item(Material.OAK_DOOR, plugin.text("menu-close"), List.of(), false));
        actions.put(CLOSE_SLOT, "close");
    }

    private void effect(VWGlow plugin, Player p, String current, int slot, String mode,
                        Material icon, String nameKey, String descKey) {
        boolean can = plugin.manager().canUse(p, mode);
        boolean selected = mode.equals(current);
        List<Component> lore = new ArrayList<>();
        lore.add(plugin.text(descKey));
        lore.add(Component.empty());
        lore.addAll(lore(plugin, mode, can, selected));
        inventory.setItem(slot, item(can ? icon : Material.GRAY_DYE,
                plugin.text(nameKey).decoration(TextDecoration.BOLD, true), lore, selected));
        actions.put(slot, mode);
    }

    private static List<Component> lore(VWGlow plugin, String mode, boolean can, boolean selected) {
        if (selected) return List.of(plugin.text("menu-selected"));
        if (can) return List.of(plugin.text("menu-click"));
        return List.of(plugin.text("menu-locked", Placeholder.unparsed("rank", GlowCommand.rankFor(plugin, mode))));
    }

    private static ItemStack item(Material material, Component name, List<Component> lore, boolean glint) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(name.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        List<Component> clean = new ArrayList<>();
        for (Component line : lore) clean.add(line.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        meta.lore(clean);
        meta.addItemFlags(ItemFlag.values());
        if (glint) meta.setEnchantmentGlintOverride(true);
        stack.setItemMeta(meta);
        return stack;
    }

    // ---------------------------------------------------------------- клики

    public static final class Listener implements org.bukkit.event.Listener {
        private final VWGlow plugin;

        public Listener(VWGlow plugin) {
            this.plugin = plugin;
        }

        @EventHandler
        public void onClick(InventoryClickEvent e) {
            if (!(e.getView().getTopInventory().getHolder() instanceof GlowMenu menu)) return;
            e.setCancelled(true); // в меню ничего нельзя ни взять, ни положить
            if (!(e.getWhoClicked() instanceof Player p)) return;
            if (e.getClickedInventory() != e.getView().getTopInventory()) return;

            String action = menu.actions.get(e.getRawSlot());
            if (action == null) return;
            if ("close".equals(action)) {
                p.closeInventory();
                return;
            }
            GlowCommand.choose(plugin, p, action);
            menu.fill(plugin, p); // перерисовать: отметка «выбрано», замки
        }

        @EventHandler
        public void onDrag(InventoryDragEvent e) {
            if (e.getView().getTopInventory().getHolder() instanceof GlowMenu) e.setCancelled(true);
        }
    }
}
