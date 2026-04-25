package com.tiers;

import com.tiers.misc.ConfigManager;
import com.tiers.misc.Mode;
import com.tiers.textures.Icons;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

import java.util.Set;

public class InventoryChecker {
    public static void checkInventory(MinecraftClient minecraftClient, boolean showMessage) {
        if (minecraftClient.player == null)
            return;

        if (showMessage && TiersClient.toggleAutoKitDetect) {
            TiersClient.sendMessageToPlayer(Icons.colorText("Auto kit detect is enabled. Pressing the keybind won't make a difference", "green"), true);
            return;
        }

        Mode oldActivePvPTiersMode = TiersClient.activePvPTiersMode;
        Mode detected = null;

        PlayerInventory playerInventory = minecraftClient.player.getInventory();

        if (checkVanilla(playerInventory)) {
            TiersClient.activePvPTiersMode = Mode.PVPTIERS_CRYSTAL;
            detected = Mode.MCTIERS_VANILLA;
        }

        if (checkSword(playerInventory)) {
            TiersClient.activePvPTiersMode = Mode.PVPTIERS_SWORD;
            detected = Mode.MCTIERS_SWORD;
        }

        if (checkUhc(playerInventory)) {
            TiersClient.activePvPTiersMode = Mode.PVPTIERS_UHC;
            detected = Mode.MCTIERS_UHC;
        }

        if (checkPot(playerInventory)) {
            TiersClient.activePvPTiersMode = Mode.PVPTIERS_POT;
            detected = Mode.MCTIERS_POT;
        }

        if (checkNethPot(playerInventory)) {
            TiersClient.activePvPTiersMode = Mode.PVPTIERS_NETH_POT;
            detected = Mode.MCTIERS_NETH_OP;
        }

        if (checkSmp(playerInventory)) {
            TiersClient.activePvPTiersMode = Mode.PVPTIERS_SMP;
            detected = Mode.MCTIERS_SMP;
        }

        if (checkAxe(playerInventory)) {
            TiersClient.activePvPTiersMode = Mode.PVPTIERS_AXE;
            detected = Mode.MCTIERS_AXE;
        }

        if (checkMace(playerInventory)) {
            TiersClient.activePvPTiersMode = Mode.PVPTIERS_MACE;
            detected = Mode.MCTIERS_MACE;
        }

        if (oldActivePvPTiersMode != TiersClient.activePvPTiersMode && detected != null) {
            ConfigManager.saveConfig();
            TiersClient.sendMessageToPlayer(Text.empty().append(detected.getTextLabel()).append(Text.of(" was detected")), true);
        } else if (showMessage)
            TiersClient.sendMessageToPlayer(Icons.colorText("No gamemode detected", "red"), true);
    }

    private static boolean checkVanilla(PlayerInventory playerInventory) {
        boolean hasObsidian = false;
        boolean hasCrystal = false;
        boolean hasAnchor = false;
        boolean hasGlowstone = false;
        boolean hasSword = false;
        boolean hasHelmet = false;
        boolean hasChestplate = false;
        boolean hasLeggings = false;
        boolean hasBoots = false;

        for (int i = 0; i < playerInventory.size(); i++) {
            ItemStack stack = playerInventory.getStack(i);

            hasObsidian |= hasItem(stack, Items.OBSIDIAN);
            hasCrystal |= hasItem(stack, Items.END_CRYSTAL);
            hasAnchor |= hasItem(stack, Items.RESPAWN_ANCHOR);
            hasGlowstone |= hasItem(stack, Items.GLOWSTONE);
            hasSword |= hasItem(stack, Items.NETHERITE_SWORD, true);
            hasHelmet |= hasItem(stack, Items.NETHERITE_HELMET, true);
            hasChestplate |= hasItem(stack, Items.NETHERITE_CHESTPLATE, true);
            hasLeggings |= hasItem(stack, Items.NETHERITE_LEGGINGS, true);
            hasBoots |= hasItem(stack, Items.NETHERITE_BOOTS, true);
        }

        return hasObsidian && hasCrystal && hasAnchor && hasGlowstone && hasSword && hasHelmet && hasChestplate && hasLeggings && hasBoots;
    }

    private static boolean checkSword(PlayerInventory playerInventory) {
        boolean hasSword = false;
        boolean hasHelmet = false;
        boolean hasChestplate = false;
        boolean hasLeggings = false;
        boolean hasBoots = false;

        for (int i = 0; i < playerInventory.size(); i++) {
            ItemStack stack = playerInventory.getStack(i);

            hasSword |= hasItem(stack, Items.DIAMOND_SWORD);
            hasHelmet |= hasItem(stack, Items.DIAMOND_HELMET);
            hasChestplate |= hasItem(stack, Items.DIAMOND_CHESTPLATE);
            hasLeggings |= hasItem(stack, Items.DIAMOND_LEGGINGS);
            hasBoots |= hasItem(stack, Items.DIAMOND_BOOTS);

            if (SWORD_NON_ALLOWED.contains(stack.getItem())) return false;
        }

        return hasSword && hasHelmet && hasChestplate && hasLeggings && hasBoots;
    }

    private static boolean checkUhc(PlayerInventory playerInventory) {
        boolean hasShield = false;
        boolean hasGaps = false;
        boolean hasLava = false;
        boolean hasWater = false;
        boolean hasCobwebs = false;
        boolean hasEnchantedBow = false;
        boolean hasEnchantedCrossbow = false;
        boolean hasEnchantedSword = false;
        boolean hasEnchantedAxe = false;
        boolean hasEnchantedHelmet = false;
        boolean hasEnchantedChestplate = false;
        boolean hasEnchantedLeggings = false;
        boolean hasEnchantedBoots = false;

        for (int i = 0; i < playerInventory.size(); i++) {
            ItemStack stack = playerInventory.getStack(i);

            hasShield |= hasItem(stack, Items.SHIELD);
            hasGaps |= hasItem(stack, Items.GOLDEN_APPLE);
            hasLava |= hasItem(stack, Items.LAVA_BUCKET);
            hasWater |= hasItem(stack, Items.WATER_BUCKET);
            hasCobwebs |= hasItem(stack, Items.COBWEB);
            hasEnchantedBow |= hasItem(stack, Items.BOW, true);
            hasEnchantedCrossbow |= hasItem(stack, Items.CROSSBOW, true);
            hasEnchantedSword |= hasItem(stack, Items.DIAMOND_SWORD, true);
            hasEnchantedAxe |= hasItem(stack, Items.DIAMOND_AXE, true);
            hasEnchantedHelmet |= hasItem(stack, Items.DIAMOND_HELMET, true);
            hasEnchantedChestplate |= hasItem(stack, Items.DIAMOND_CHESTPLATE, true);
            hasEnchantedLeggings |= hasItem(stack, Items.DIAMOND_LEGGINGS, true) || hasItem(stack, Items.IRON_LEGGINGS, true);
            hasEnchantedBoots |= hasItem(stack, Items.DIAMOND_BOOTS, true);

            if (UHC_NON_ALLOWED.contains(stack.getItem())) return false;
        }

        return hasShield && hasGaps && hasLava && hasWater && hasCobwebs && hasEnchantedBow && hasEnchantedCrossbow && hasEnchantedSword &&
                hasEnchantedAxe && hasEnchantedHelmet && hasEnchantedChestplate && hasEnchantedLeggings && hasEnchantedBoots;
    }

    private static boolean checkPot(PlayerInventory playerInventory) {
        boolean hasSteak = false;
        boolean hasPotions = false;
        boolean hasEnchantedSword = false;
        boolean hasEnchantedHelmet = false;
        boolean hasEnchantedChestplate = false;
        boolean hasEnchantedLeggings = false;
        boolean hasEnchantedBoots = false;

        for (int i = 0; i < playerInventory.size(); i++) {
            ItemStack stack = playerInventory.getStack(i);

            hasSteak |= hasItem(stack, Items.COOKED_BEEF);
            hasPotions |= hasItem(stack, Items.SPLASH_POTION);
            hasEnchantedSword |= hasItem(stack, Items.DIAMOND_SWORD, true);
            hasEnchantedHelmet |= hasItem(stack, Items.DIAMOND_HELMET, true);
            hasEnchantedChestplate |= hasItem(stack, Items.DIAMOND_CHESTPLATE, true);
            hasEnchantedLeggings |= hasItem(stack, Items.DIAMOND_LEGGINGS, true);
            hasEnchantedBoots |= hasItem(stack, Items.DIAMOND_BOOTS, true);

            if (POT_NON_ALLOWED.contains(stack.getItem())) return false;
        }

        return hasSteak && hasPotions && hasEnchantedSword && hasEnchantedHelmet && hasEnchantedChestplate && hasEnchantedLeggings && hasEnchantedBoots;
    }

    private static boolean checkNethPot(PlayerInventory playerInventory) {
        boolean hasGaps = false;
        boolean hasPotions = false;
        boolean hasTotem = false;
        boolean hasXp = false;
        boolean hasEnchantedSword = false;
        boolean hasEnchantedHelmet = false;
        boolean hasEnchantedChestplate = false;
        boolean hasEnchantedLeggings = false;
        boolean hasEnchantedBoots = false;

        for (int i = 0; i < playerInventory.size(); i++) {
            ItemStack stack = playerInventory.getStack(i);

            hasGaps |= hasItem(stack, Items.GOLDEN_APPLE);
            hasPotions |= hasItem(stack, Items.SPLASH_POTION);
            hasTotem |= hasItem(stack, Items.TOTEM_OF_UNDYING);
            hasXp |= hasItem(stack, Items.EXPERIENCE_BOTTLE);
            hasEnchantedSword |= hasItem(stack, Items.NETHERITE_SWORD, true);
            hasEnchantedHelmet |= hasItem(stack, Items.NETHERITE_HELMET, true);
            hasEnchantedChestplate |= hasItem(stack, Items.NETHERITE_CHESTPLATE, true);
            hasEnchantedLeggings |= hasItem(stack, Items.NETHERITE_LEGGINGS, true);
            hasEnchantedBoots |= hasItem(stack, Items.NETHERITE_BOOTS, true);

            if (NETHPOT_NON_ALLOWED.contains(stack.getItem())) return false;
        }

        return hasGaps && hasPotions && hasTotem && hasXp && hasEnchantedSword && hasEnchantedHelmet &&
                hasEnchantedChestplate && hasEnchantedLeggings && hasEnchantedBoots;
    }

    private static boolean checkSmp(PlayerInventory playerInventory) {
        boolean hasGaps = false;
        boolean hasPotions = false;
        boolean hasTotem = false;
        boolean hasXp = false;
        boolean hasPearls = false;
        boolean hasShield = false;
        boolean hasEnchantedSword = false;
        boolean hasEnchantedAxe = false;
        boolean hasEnchantedHelmet = false;
        boolean hasEnchantedChestplate = false;
        boolean hasEnchantedLeggings = false;
        boolean hasEnchantedBoots = false;

        for (int i = 0; i < playerInventory.size(); i++) {
            ItemStack stack = playerInventory.getStack(i);

            hasGaps |= hasItem(stack, Items.GOLDEN_APPLE);
            hasPotions |= hasItem(stack, Items.SPLASH_POTION);
            hasTotem |= hasItem(stack, Items.TOTEM_OF_UNDYING);
            hasXp |= hasItem(stack, Items.EXPERIENCE_BOTTLE);
            hasPearls |= hasItem(stack, Items.ENDER_PEARL);
            hasShield |= hasItem(stack, Items.SHIELD);
            hasEnchantedSword |= hasItem(stack, Items.NETHERITE_SWORD, true);
            hasEnchantedAxe |= hasItem(stack, Items.NETHERITE_AXE, true);
            hasEnchantedHelmet |= hasItem(stack, Items.NETHERITE_HELMET, true);
            hasEnchantedChestplate |= hasItem(stack, Items.NETHERITE_CHESTPLATE, true);
            hasEnchantedLeggings |= hasItem(stack, Items.NETHERITE_LEGGINGS, true);
            hasEnchantedBoots |= hasItem(stack, Items.NETHERITE_BOOTS, true);

            if (SMP_NON_ALLOWED.contains(stack.getItem())) return false;
        }

        return hasGaps && hasPotions && hasTotem && hasXp && hasPearls && hasShield && hasEnchantedAxe &&
                hasEnchantedSword && hasEnchantedHelmet && hasEnchantedChestplate && hasEnchantedLeggings && hasEnchantedBoots;
    }

    private static boolean checkAxe(PlayerInventory playerInventory) {
        boolean hasBow = false;
        boolean hasCrossbow = false;
        boolean hasShield = false;
        boolean hasSword = false;
        boolean hasAxe = false;
        boolean hasHelmet = false;
        boolean hasChestplate = false;
        boolean hasLeggings = false;
        boolean hasBoots = false;

        for (int i = 0; i < playerInventory.size(); i++) {
            ItemStack stack = playerInventory.getStack(i);

            hasBow |= hasItem(stack, Items.BOW, false);
            hasCrossbow |= hasItem(stack, Items.CROSSBOW, false);
            hasShield |= hasItem(stack, Items.SHIELD, false);
            hasSword |= hasItem(stack, Items.DIAMOND_SWORD, false);
            hasAxe |= hasItem(stack, Items.DIAMOND_AXE, false);
            hasHelmet |= hasItem(stack, Items.DIAMOND_HELMET, false);
            hasChestplate |= hasItem(stack, Items.DIAMOND_CHESTPLATE, false);
            hasLeggings |= hasItem(stack, Items.DIAMOND_LEGGINGS, false);
            hasBoots |= hasItem(stack, Items.DIAMOND_BOOTS, false);

            if (AXE_NON_ALLOWED.contains(stack.getItem())) return false;
        }

        return hasBow && hasCrossbow && hasShield && hasSword && hasAxe && hasHelmet && hasChestplate && hasLeggings && hasBoots;
    }

    private static boolean checkMace(PlayerInventory playerInventory) {
        boolean hasGaps = false;
        boolean hasPotions = false;
        boolean hasTotem = false;
        boolean hasPearls = false;
        boolean hasWindCharge = false;
        boolean hasElytra = false;
        boolean hasShield = false;
        boolean hasEnchantedMace = false;
        boolean hasEnchantedSword = false;
        boolean hasEnchantedAxe = false;
        boolean hasEnchantedHelmet = false;
        boolean hasEnchantedChestplate = false;
        boolean hasEnchantedLeggings = false;
        boolean hasEnchantedBoots = false;

        for (int i = 0; i < playerInventory.size(); i++) {
            ItemStack stack = playerInventory.getStack(i);

            hasGaps |= hasItem(stack, Items.GOLDEN_APPLE);
            hasPotions |= hasItem(stack, Items.SPLASH_POTION);
            hasTotem |= hasItem(stack, Items.TOTEM_OF_UNDYING);
            hasPearls |= hasItem(stack, Items.ENDER_PEARL);
            hasWindCharge |= hasItem(stack, Items.WIND_CHARGE);
            hasElytra |= hasItem(stack, Items.ELYTRA);
            hasShield |= hasItem(stack, Items.SHIELD);
            hasEnchantedMace |= hasItem(stack, Items.MACE, true);
            hasEnchantedSword |= hasItem(stack, Items.NETHERITE_SWORD, true);
            hasEnchantedAxe |= hasItem(stack, Items.NETHERITE_AXE, true);
            hasEnchantedHelmet |= hasItem(stack, Items.NETHERITE_HELMET, true);
            hasEnchantedChestplate |= hasItem(stack, Items.NETHERITE_CHESTPLATE, true);
            hasEnchantedLeggings |= hasItem(stack, Items.NETHERITE_LEGGINGS, true);
            hasEnchantedBoots |= hasItem(stack, Items.NETHERITE_BOOTS, true);

            if (MACE_NON_ALLOWED.contains(stack.getItem())) return false;
        }

        return hasGaps && hasPotions && hasTotem && hasPearls && hasWindCharge && hasElytra && hasShield && hasEnchantedMace &&
                hasEnchantedSword && hasEnchantedAxe && hasEnchantedHelmet && hasEnchantedChestplate && hasEnchantedLeggings && hasEnchantedBoots;
    }

    private static boolean hasItem(ItemStack itemStack, Item item, boolean needEnchant) {
        return itemStack.getItem() == item && (needEnchant == itemStack.hasEnchantments());
    }

    private static boolean hasItem(ItemStack itemStack, Item item) {
        return itemStack.getItem() == item;
    }

    private static final Set<Item> SWORD_NON_ALLOWED = Set.of(
            Items.DIAMOND_AXE,
            Items.COBWEB,
            Items.SHIELD,
            Items.ENDER_PEARL,
            Items.EXPERIENCE_BOTTLE,
            Items.SPLASH_POTION,

            Items.NETHERITE_SWORD,
            Items.NETHERITE_AXE,
            Items.NETHERITE_PICKAXE,
            Items.NETHERITE_HELMET,
            Items.NETHERITE_CHESTPLATE,
            Items.NETHERITE_LEGGINGS,
            Items.NETHERITE_BOOTS,

            Items.END_CRYSTAL,
            Items.OBSIDIAN,
            Items.RESPAWN_ANCHOR,
            Items.GLOWSTONE
    );

    private static final Set<Item> UHC_NON_ALLOWED = Set.of(
            Items.NETHERITE_SWORD,
            Items.NETHERITE_AXE,
            Items.NETHERITE_PICKAXE,
            Items.NETHERITE_HELMET,
            Items.NETHERITE_CHESTPLATE,
            Items.NETHERITE_LEGGINGS,
            Items.NETHERITE_BOOTS,

            Items.END_CRYSTAL,
            Items.OBSIDIAN,
            Items.RESPAWN_ANCHOR,
            Items.GLOWSTONE
    );

    private static final Set<Item> POT_NON_ALLOWED = Set.of(
            Items.DIAMOND_AXE,
            Items.GOLDEN_APPLE,
            Items.SHIELD,
            Items.COBWEB,

            Items.NETHERITE_SWORD,
            Items.NETHERITE_AXE,
            Items.NETHERITE_PICKAXE,
            Items.NETHERITE_HELMET,
            Items.NETHERITE_CHESTPLATE,
            Items.NETHERITE_LEGGINGS,
            Items.NETHERITE_BOOTS,

            Items.END_CRYSTAL,
            Items.OBSIDIAN,
            Items.RESPAWN_ANCHOR,
            Items.GLOWSTONE
    );

    private static final Set<Item> NETHPOT_NON_ALLOWED = Set.of(
            Items.ENDER_PEARL,
            Items.SHIELD,

            Items.NETHERITE_AXE,
            Items.NETHERITE_PICKAXE,

            Items.DIAMOND_SWORD,
            Items.DIAMOND_AXE,
            Items.DIAMOND_PICKAXE,
            Items.DIAMOND_HELMET,
            Items.DIAMOND_CHESTPLATE,
            Items.DIAMOND_LEGGINGS,
            Items.DIAMOND_BOOTS,

            Items.END_CRYSTAL,
            Items.OBSIDIAN,
            Items.RESPAWN_ANCHOR,
            Items.GLOWSTONE
    );

    private static final Set<Item> SMP_NON_ALLOWED = Set.of(
            Items.DIAMOND_SWORD,
            Items.DIAMOND_AXE,
            Items.DIAMOND_PICKAXE,
            Items.DIAMOND_HELMET,
            Items.DIAMOND_CHESTPLATE,
            Items.DIAMOND_LEGGINGS,
            Items.DIAMOND_BOOTS,

            Items.NETHERITE_PICKAXE,

            Items.END_CRYSTAL,
            Items.OBSIDIAN,
            Items.RESPAWN_ANCHOR,
            Items.GLOWSTONE
    );

    private static final Set<Item> AXE_NON_ALLOWED = Set.of(
            Items.ENDER_PEARL,
            Items.COBWEB,
            Items.GOLDEN_APPLE,

            Items.NETHERITE_SWORD,
            Items.NETHERITE_AXE,
            Items.NETHERITE_PICKAXE,
            Items.NETHERITE_HELMET,
            Items.NETHERITE_CHESTPLATE,
            Items.NETHERITE_LEGGINGS,
            Items.NETHERITE_BOOTS,

            Items.END_CRYSTAL,
            Items.OBSIDIAN,
            Items.RESPAWN_ANCHOR,
            Items.GLOWSTONE
    );

    private static final Set<Item> MACE_NON_ALLOWED = Set.of(
            Items.EXPERIENCE_BOTTLE,

            Items.NETHERITE_PICKAXE,

            Items.DIAMOND_SWORD,
            Items.DIAMOND_AXE,
            Items.DIAMOND_PICKAXE,
            Items.DIAMOND_HELMET,
            Items.DIAMOND_CHESTPLATE,
            Items.DIAMOND_LEGGINGS,
            Items.DIAMOND_BOOTS,

            Items.END_CRYSTAL,
            Items.OBSIDIAN,
            Items.RESPAWN_ANCHOR,
            Items.GLOWSTONE
    );
}