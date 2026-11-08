package online.vanillaway.glow;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

import java.util.Locale;

/** 16 цветов Minecraft, которыми может светиться игрок. */
public enum GlowColor {
    RED('c', "red", "Красный", Material.RED_DYE, NamedTextColor.RED),
    GOLD('6', "gold", "Золотой", Material.GOLD_INGOT, NamedTextColor.GOLD),
    YELLOW('e', "yellow", "Жёлтый", Material.YELLOW_DYE, NamedTextColor.YELLOW),
    GREEN('a', "green", "Зелёный", Material.LIME_DYE, NamedTextColor.GREEN),
    AQUA('b', "aqua", "Голубой", Material.LIGHT_BLUE_DYE, NamedTextColor.AQUA),
    BLUE('9', "blue", "Синий", Material.LAPIS_LAZULI, NamedTextColor.BLUE),
    LIGHT_PURPLE('d', "light_purple", "Розовый", Material.MAGENTA_DYE, NamedTextColor.LIGHT_PURPLE),

    DARK_RED('4', "dark_red", "Тёмно-красный", Material.NETHER_WART, NamedTextColor.DARK_RED),
    DARK_GREEN('2', "dark_green", "Тёмно-зелёный", Material.GREEN_DYE, NamedTextColor.DARK_GREEN),
    DARK_AQUA('3', "dark_aqua", "Бирюзовый", Material.CYAN_DYE, NamedTextColor.DARK_AQUA),
    DARK_BLUE('1', "dark_blue", "Тёмно-синий", Material.BLUE_DYE, NamedTextColor.DARK_BLUE),
    DARK_PURPLE('5', "dark_purple", "Фиолетовый", Material.PURPLE_DYE, NamedTextColor.DARK_PURPLE),
    GRAY('7', "gray", "Серый", Material.LIGHT_GRAY_DYE, NamedTextColor.GRAY),
    DARK_GRAY('8', "dark_gray", "Тёмно-серый", Material.GRAY_DYE, NamedTextColor.DARK_GRAY),

    WHITE('f', "white", "Белый", Material.WHITE_DYE, NamedTextColor.WHITE),
    BLACK('0', "black", "Чёрный", Material.BLACK_DYE, NamedTextColor.BLACK);

    private final char code;
    private final String id;
    private final String title;
    private final Material icon;
    private final NamedTextColor text;

    GlowColor(char code, String id, String title, Material icon, NamedTextColor text) {
        this.code = code;
        this.id = id;
        this.title = title;
        this.icon = icon;
        this.text = text;
    }

    public char code() { return code; }
    public String id() { return id; }
    public String title() { return title; }
    public Material icon() { return icon; }
    public NamedTextColor text() { return text; }

    /** Код для TAB и Bedrock-форм: &b, &c ... */
    public String legacy() { return "&" + code; }

    /** То же, но с §, для Bedrock-форм. */
    public String section() { return "§" + code; }

    /** Цвет для названия в меню: чёрный на тёмной подсказке не читается. */
    public NamedTextColor display() { return this == BLACK ? NamedTextColor.DARK_GRAY : text; }

    public String permission() { return "vwglow.color." + id; }

    public static GlowColor byId(String id) {
        if (id == null) return null;
        String key = id.toLowerCase(Locale.ROOT);
        for (GlowColor c : values()) if (c.id.equals(key)) return c;
        return null;
    }
}
