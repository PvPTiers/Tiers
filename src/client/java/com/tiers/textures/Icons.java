package com.tiers.textures;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

public class Icons {
    public static Identifier identifierMCTiers = Identifier.fromNamespaceAndPath("minecraft", "gamemodes/pvptiers");
    public static Identifier identifierPvPTiers = Identifier.fromNamespaceAndPath("minecraft", "gamemodes/pvptiers");
    public static final Identifier identifierSubtiers = Identifier.fromNamespaceAndPath("minecraft", "gamemodes/subtiers");

    public static Identifier identifierMCTiersTags = Identifier.fromNamespaceAndPath("minecraft", "gamemodes/pvptiers-tags");
    public static Identifier identifierPvPTiersTags = Identifier.fromNamespaceAndPath("minecraft", "gamemodes/pvptiers-tags");
    public static final Identifier identifierSubtiersTags = Identifier.fromNamespaceAndPath("minecraft", "gamemodes/subtiers-tags");

    private static final FontDescription fontDescription = new FontDescription.Resource((Identifier.fromNamespaceAndPath("minecraft", "misc")));

    public static Component GLOBE = Component.literal("\uF000").setStyle(Style.EMPTY.withColor(ColorControl.getColorMinecraftStandard("region")).withFont(fontDescription));
    public static Component OVERALL = Component.literal("\uF001").setStyle(Style.EMPTY.withColor(ColorControl.getColorMinecraftStandard("overall")).withFont(fontDescription));
    public static final Component CYCLE = Component.literal("\uF002").setStyle(Style.EMPTY.withFont(fontDescription));
    public static final Component ICONS = Component.literal("\uF004").setStyle(Style.EMPTY.withFont(fontDescription));
    public static final Component ICONS_DISABLED = Component.literal("\uF005").setStyle(Style.EMPTY.withFont(fontDescription));
    public static final Component TAB = Component.literal("\uF006").setStyle(Style.EMPTY.withFont(fontDescription));
    public static final Component TAB_DISABLED = Component.literal("\uF007").setStyle(Style.EMPTY.withFont(fontDescription));
    public static final Component CHAT = Component.literal("\uF008").setStyle(Style.EMPTY.withFont(fontDescription));
    public static final Component CHAT_DISABLED = Component.literal("\uF009").setStyle(Style.EMPTY.withFont(fontDescription));
    public static final Component NAMEMC = Component.literal("\uF00A").setStyle(Style.EMPTY.withFont(fontDescription));
    public static final Component DISCORD = Component.literal("\uF00B").setStyle(Style.EMPTY.withFont(fontDescription));
    public static final Component REGION = Component.literal("\uF00C").setStyle(Style.EMPTY.withFont(fontDescription));
    public static final Component REGION_DISABLED = Component.literal("\uF00D").setStyle(Style.EMPTY.withFont(fontDescription));
    public static final Component PEAK = Component.literal("\uF00E").setStyle(Style.EMPTY.withFont(fontDescription));
    public static final Component PEAK_DISABLED = Component.literal("\uF00F").setStyle(Style.EMPTY.withFont(fontDescription));
    public static final Component LEVER_ON = Component.literal("\uF011").setStyle(Style.EMPTY.withFont(fontDescription));
    public static final Component LEVER_OFF = Component.literal("\uF012").setStyle(Style.EMPTY.withFont(fontDescription));

    public enum Type {
        CLASSIC,
        PVPTIERS,
        MCTIERS
    }

    public static Component colorText(String string, String color) {
        return Component.literal(string).setStyle(Style.EMPTY.withColor(ColorControl.getColor(color)));
    }

    public static Component colorText(String string, int color) {
        return Component.literal(string).setStyle(Style.EMPTY.withColor(color));
    }
}