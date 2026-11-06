package com.tiers.textures;

import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class Icons {
    public static Identifier identifierMCTiers = Identifier.of("minecraft", "gamemodes/pvptiers");
    public static Identifier identifierPvPTiers = Identifier.of("minecraft", "gamemodes/pvptiers");
    public static final Identifier identifierSubtiers = Identifier.of("minecraft", "gamemodes/subtiers");

    public static Identifier identifierMCTiersTags = Identifier.of("minecraft", "gamemodes/pvptiers-tags");
    public static Identifier identifierPvPTiersTags = Identifier.of("minecraft", "gamemodes/pvptiers-tags");
    public static final Identifier identifierSubtiersTags = Identifier.of("minecraft", "gamemodes/subtiers-tags");

    private static final StyleSpriteSource.Font font = new StyleSpriteSource.Font(Identifier.of("minecraft", "misc"));

    public static Text GLOBE = Text.literal("\uF000").setStyle(Style.EMPTY.withColor(ColorControl.getColorMinecraftStandard("region")).withFont(font));
    public static Text OVERALL = Text.literal("\uF001").setStyle(Style.EMPTY.withColor(ColorControl.getColorMinecraftStandard("overall")).withFont(font));
    public static final Text CYCLE = Text.literal("\uF002").setStyle(Style.EMPTY.withFont(font));
    public static final Text ICONS = Text.literal("\uF004").setStyle(Style.EMPTY.withFont(font));
    public static final Text ICONS_DISABLED = Text.literal("\uF005").setStyle(Style.EMPTY.withFont(font));
    public static final Text TAB = Text.literal("\uF006").setStyle(Style.EMPTY.withFont(font));
    public static final Text TAB_DISABLED = Text.literal("\uF007").setStyle(Style.EMPTY.withFont(font));
    public static final Text CHAT = Text.literal("\uF008").setStyle(Style.EMPTY.withFont(font));
    public static final Text CHAT_DISABLED = Text.literal("\uF009").setStyle(Style.EMPTY.withFont(font));
    public static final Text NAMEMC = Text.literal("\uF00A").setStyle(Style.EMPTY.withFont(font));
    public static final Text DISCORD = Text.literal("\uF00B").setStyle(Style.EMPTY.withFont(font));
    public static final Text REGION = Text.literal("\uF00C").setStyle(Style.EMPTY.withFont(font));
    public static final Text REGION_DISABLED = Text.literal("\uF00D").setStyle(Style.EMPTY.withFont(font));
    public static final Text PEAK = Text.literal("\uF00E").setStyle(Style.EMPTY.withFont(font));
    public static final Text PEAK_DISABLED = Text.literal("\uF00F").setStyle(Style.EMPTY.withFont(font));
    public static final Text LEVER_ON = Text.literal("\uF011").setStyle(Style.EMPTY.withFont(font));
    public static final Text LEVER_OFF = Text.literal("\uF012").setStyle(Style.EMPTY.withFont(font));

    public enum Type {
        CLASSIC,
        PVPTIERS,
        MCTIERS
    }

    public static Text colorText(String string, String color) {
        return Text.literal(string).setStyle(Style.EMPTY.withColor(ColorControl.getColor(color)));
    }

    public static Text colorText(String string, int color) {
        return Text.literal(string).setStyle(Style.EMPTY.withColor(color));
    }
}