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

        Mode oldActiveMCTiersMode = TiersClient.activeMCTiersMode;
        Mode oldActivePvPTiersMode = TiersClient.activePvPTiersMode;
        Mode oldActiveSubtiersMode = TiersClient.activeSubtiersMode;
        Mode detected = null;

        PlayerInventory playerInventory = minecraftClient.player.getInventory();

        if (checkVanilla(playerInventory)) {
            TiersClient.activeMCTiersMode = Mode.MCTIERS_VANILLA;
            TiersClient.activePvPTiersMode = Mode.PVPTIERS_CRYSTAL;
            detected = Mode.MCTIERS_VANILLA;
        }

        if (checkSword(playerInventory)) {
            TiersClient.activeMCTiersMode = Mode.MCTIERS_SWORD;
            TiersClient.activePvPTiersMode = Mode.PVPTIERS_SWORD;
            detected = Mode.MCTIERS_SWORD;
        }

        if (checkUhc(playerInventory)) {
            TiersClient.activeMCTiersMode = Mode.MCTIERS_UHC;
            TiersClient.activePvPTiersMode = Mode.PVPTIERS_UHC;
            detected = Mode.MCTIERS_UHC;
        }

        if (checkPot(playerInventory)) {
            TiersClient.activeMCTiersMode = Mode.MCTIERS_POT;
            TiersClient.activePvPTiersMode = Mode.PVPTIERS_POT;
            detected = Mode.MCTIERS_POT;
        }

        if (checkNethPot(playerInventory)) {
            TiersClient.activeMCTiersMode = Mode.MCTIERS_NETH_OP;
            TiersClient.activePvPTiersMode = Mode.PVPTIERS_NETH_POT;
            detected = Mode.MCTIERS_NETH_OP;
        }

        if (checkSmp(playerInventory)) {
            TiersClient.activeMCTiersMode = Mode.MCTIERS_SMP;
            TiersClient.activePvPTiersMode = Mode.PVPTIERS_SMP;
            detected = Mode.MCTIERS_SMP;
        }

        if (checkAxe(playerInventory)) {
            TiersClient.activeMCTiersMode = Mode.MCTIERS_AXE;
            TiersClient.activePvPTiersMode = Mode.PVPTIERS_AXE;
            detected = Mode.MCTIERS_AXE;
        }

        if (checkMace(playerInventory)) {
            TiersClient.activeMCTiersMode = Mode.MCTIERS_MACE;
            TiersClient.activePvPTiersMode = Mode.PVPTIERS_MACE;
            detected = Mode.MCTIERS_MACE;
        }

        if (checkMinecart(playerInventory)) {
            TiersClient.activeSubtiersMode = Mode.SUBTIERS_MINECART;
            detected = Mode.SUBTIERS_MINECART;
        }

        if (checkDiamondVanilla(playerInventory)) {
            TiersClient.activeSubtiersMode = Mode.SUBTIERS_DIAMOND_VANILLA;
            detected = Mode.SUBTIERS_DIAMOND_VANILLA;
        }

        if (checkDeBuff(playerInventory)) {
            TiersClient.activeSubtiersMode = Mode.SUBTIERS_DEBUFF;
            detected = Mode.SUBTIERS_DEBUFF;
        }

        if (checkSubtiersElytra(playerInventory)) {
            TiersClient.activeSubtiersMode = Mode.SUBTIERS_ELYTRA;
            detected = Mode.SUBTIERS_ELYTRA;
        }

        if (checkSpeed(playerInventory)) {
            TiersClient.activeSubtiersMode = Mode.SUBTIERS_SPEED;
            detected = Mode.SUBTIERS_SPEED;
        }

        if (checkCreeper(playerInventory)) {
            TiersClient.activeSubtiersMode = Mode.SUBTIERS_CREEPER;
            detected = Mode.SUBTIERS_CREEPER;
        }

        if (checkManhunt(playerInventory)) {
            TiersClient.activeSubtiersMode = Mode.SUBTIERS_MANHUNT;
            detected = Mode.SUBTIERS_MANHUNT;
        }

        if (checkDiamondSmp(playerInventory)) {
            TiersClient.activeSubtiersMode = Mode.SUBTIERS_DIAMOND_SMP;
            detected = Mode.SUBTIERS_DIAMOND_SMP;
        }

        if (checkBow(playerInventory)) {
            TiersClient.activeSubtiersMode = Mode.SUBTIERS_BOW;
            detected = Mode.SUBTIERS_BOW;
        }

        if (checkBed(playerInventory)) {
            TiersClient.activeSubtiersMode = Mode.SUBTIERS_BED;
            detected = Mode.SUBTIERS_BED;
        }

        if (checkOgVanilla(playerInventory)) {
            TiersClient.activeSubtiersMode = Mode.SUBTIERS_OG_VANILLA;
            detected = Mode.SUBTIERS_OG_VANILLA;
        }

        if (checkTrident(playerInventory)) {
            TiersClient.activeSubtiersMode = Mode.SUBTIERS_TRIDENT;
            detected = Mode.SUBTIERS_TRIDENT;
        }

        if ((oldActiveMCTiersMode != TiersClient.activeMCTiersMode || oldActivePvPTiersMode != TiersClient.activePvPTiersMode || oldActiveSubtiersMode != TiersClient.activeSubtiersMode) && detected != null) {
            ConfigManager.saveConfig();
            TiersClient.sendMessageToPlayer(Text.empty().append(Text.literal("Now displaying: ")).append(detected.getTextLabel()), true);
        } else if (showMessage) {
            if (detected == null)
                TiersClient.sendMessageToPlayer(Icons.colorText("No gamemode detected", "red"), true);
            else
                TiersClient.sendMessageToPlayer(Text.empty().append(Text.literal("Now displaying: ")).append(detected.getTextLabel()), true);
        }
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

    private static boolean checkMinecart(PlayerInventory playerInventory) {
        boolean hasCart = false;
        boolean hasCobwebs = false;
        boolean hasPotions = false;
        boolean hasRails = false;
        boolean hasEnchantedAxe = false;
        boolean hasEnchantedSword = false;
        boolean hasEnchantedHelmet = false;
        boolean hasEnchantedChestplate = false;
        boolean hasEnchantedLeggings = false;
        boolean hasEnchantedBoots = false;

        for (int i = 0; i < playerInventory.size(); i++) {
            ItemStack stack = playerInventory.getStack(i);

            hasCart |= hasItem(stack, Items.TNT_MINECART);
            hasCobwebs |= hasItem(stack, Items.COBWEB);
            hasPotions |= hasItem(stack, Items.SPLASH_POTION);
            hasRails |= hasItem(stack, Items.RAIL);
            hasRails |= hasItem(stack, Items.POWERED_RAIL);
            hasEnchantedAxe |= hasItem(stack, Items.NETHERITE_AXE, true);
            hasEnchantedSword |= hasItem(stack, Items.NETHERITE_SWORD, true);
            hasEnchantedHelmet |= hasItem(stack, Items.NETHERITE_HELMET, true);
            hasEnchantedChestplate |= hasItem(stack, Items.NETHERITE_CHESTPLATE, true);
            hasEnchantedLeggings |= hasItem(stack, Items.NETHERITE_LEGGINGS, true);
            hasEnchantedBoots |= hasItem(stack, Items.NETHERITE_BOOTS, true);

            if (MINECART_NON_ALLOWED.contains(stack.getItem())) return false;
        }

        return hasCart && hasCobwebs && hasPotions && hasRails && hasEnchantedAxe && hasEnchantedSword && hasEnchantedHelmet &&
                hasEnchantedChestplate && hasEnchantedLeggings && hasEnchantedBoots;
    }

    private static boolean checkDiamondVanilla(PlayerInventory playerInventory) {
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
            hasSword |= hasItem(stack, Items.DIAMOND_SWORD, true);
            hasHelmet |= hasItem(stack, Items.DIAMOND_HELMET, true);
            hasChestplate |= hasItem(stack, Items.DIAMOND_CHESTPLATE, true);
            hasLeggings |= hasItem(stack, Items.DIAMOND_LEGGINGS, true);
            hasBoots |= hasItem(stack, Items.DIAMOND_BOOTS, true);

            if (DIAMOND_VANILLA_NON_ALLOWED.contains(stack.getItem())) return false;
        }

        return hasObsidian && hasCrystal && hasAnchor && hasGlowstone && hasSword && hasHelmet && hasChestplate && hasLeggings && hasBoots;
    }

    private static boolean checkDeBuff(PlayerInventory playerInventory) {
        boolean hasPots = false;
        boolean hasGoldenCarrots = false;
        boolean hasPearls = false;
        boolean hasCrossbow = false;
        boolean hasSword = false;
        boolean hasHelmet = false;
        boolean hasChestplate = false;
        boolean hasLeggings = false;
        boolean hasBoots = false;

        for (int i = 0; i < playerInventory.size(); i++) {
            ItemStack stack = playerInventory.getStack(i);

            hasPots |= hasItem(stack, Items.SPLASH_POTION);
            hasGoldenCarrots |= hasItem(stack, Items.GOLDEN_CARROT);
            hasPearls |= hasItem(stack, Items.ENDER_PEARL);
            hasCrossbow |= hasItem(stack, Items.CROSSBOW);
            hasSword |= hasItem(stack, Items.STONE_SWORD, false);
            hasHelmet |= hasItem(stack, Items.LEATHER_HELMET, true);
            hasChestplate |= hasItem(stack, Items.LEATHER_CHESTPLATE, true);
            hasLeggings |= hasItem(stack, Items.LEATHER_LEGGINGS, true);
            hasBoots |= hasItem(stack, Items.LEATHER_BOOTS, true);

            if (DEBUFF_NON_ALLOWED.contains(stack.getItem())) return false;
        }

        return hasPots && hasGoldenCarrots && hasPearls && hasCrossbow && hasSword && hasHelmet && hasChestplate && hasLeggings && hasBoots;
    }

    private static boolean checkSubtiersElytra(PlayerInventory playerInventory) {
        boolean hasCobweb = false;
        boolean hasPots = false;
        boolean hasGaps = false;
        boolean hasTotem = false;
        boolean hasWindcharge = false;
        boolean hasPearls = false;
        boolean hasCrossbow = false;
        boolean hasBow = false;
        boolean hasElytra = false;
        boolean hasMace = false;
        boolean hasSword = false;
        boolean hasAxe = false;
        boolean hasChestplate = false;

        for (int i = 0; i < playerInventory.size(); i++) {
            ItemStack stack = playerInventory.getStack(i);

            hasCobweb |= hasItem(stack, Items.COBWEB);
            hasPots |= hasItem(stack, Items.SPLASH_POTION);
            hasGaps |= hasItem(stack, Items.GOLDEN_APPLE);
            hasTotem |= hasItem(stack, Items.TOTEM_OF_UNDYING);
            hasWindcharge |= hasItem(stack, Items.WIND_CHARGE);
            hasPearls |= hasItem(stack, Items.ENDER_PEARL);
            hasCrossbow |= hasItem(stack, Items.CROSSBOW, true);
            hasBow |= hasItem(stack, Items.BOW, true);
            hasElytra |= hasItem(stack, Items.ELYTRA);
            hasMace |= hasItem(stack, Items.MACE);
            hasSword |= hasItem(stack, Items.NETHERITE_SWORD, true);
            hasAxe |= hasItem(stack, Items.NETHERITE_AXE, true);
            hasChestplate |= hasItem(stack, Items.LEATHER_CHESTPLATE);

            if (SUBTIERS_ELYTRA_NON_ALLOWED.contains(stack.getItem())) return false;
        }

        return hasCobweb && hasPots && hasGaps && hasTotem && hasWindcharge && hasPearls && hasCrossbow &&
                hasBow && hasElytra && hasMace && hasSword && hasAxe && hasChestplate;
    }

    private static boolean checkSpeed(PlayerInventory playerInventory) {
        boolean hasSword = false;
        boolean hasHelmet = false;
        boolean hasChestplate = false;
        boolean hasLeggings = false;
        boolean hasBoots = false;

        for (int i = 0; i < playerInventory.size(); i++) {
            ItemStack stack = playerInventory.getStack(i);

            hasSword |= hasItem(stack, Items.DIAMOND_SWORD, false);
            hasHelmet |= hasItem(stack, Items.DIAMOND_HELMET, true);
            hasChestplate |= hasItem(stack, Items.DIAMOND_CHESTPLATE, true);
            hasLeggings |= hasItem(stack, Items.IRON_LEGGINGS, true);
            hasBoots |= hasItem(stack, Items.DIAMOND_BOOTS, true);

            if (SPEED_NON_ALLOWED.contains(stack.getItem())) return false;
        }

        return hasSword && hasHelmet && hasChestplate && hasLeggings && hasBoots;
    }

    private static boolean checkCreeper(PlayerInventory playerInventory) {
        boolean hasCobweb = false;
        boolean hasCreeper = false;
        boolean hasWater = false;
        boolean hasTnt = false;
        boolean hasGaps = false;
        boolean hasTotem = false;
        boolean hasPearls = false;
        boolean hasCrossbow = false;
        boolean hasBow = false;
        boolean hasShield = false;
        boolean hasSword = false;
        boolean hasAxe = false;
        boolean hasHelmet = false;
        boolean hasChestplate = false;
        boolean hasLeggings = false;
        boolean hasBoots = false;

        for (int i = 0; i < playerInventory.size(); i++) {
            ItemStack stack = playerInventory.getStack(i);

            hasCobweb |= hasItem(stack, Items.COBWEB);
            hasCreeper |= hasItem(stack, Items.CREEPER_SPAWN_EGG);
            hasWater |= hasItem(stack, Items.WATER_BUCKET);
            hasTnt |= hasItem(stack, Items.TNT);
            hasGaps |= hasItem(stack, Items.GOLDEN_APPLE);
            hasTotem |= hasItem(stack, Items.TOTEM_OF_UNDYING);
            hasPearls |= hasItem(stack, Items.ENDER_PEARL);
            hasCrossbow |= hasItem(stack, Items.CROSSBOW, true);
            hasBow |= hasItem(stack, Items.BOW, true);
            hasShield |= hasItem(stack, Items.SHIELD);
            hasSword |= hasItem(stack, Items.STONE_SWORD, true);
            hasAxe |= hasItem(stack, Items.STONE_AXE);
            hasHelmet |= hasItem(stack, Items.CHAINMAIL_HELMET, true);
            hasChestplate |= hasItem(stack, Items.CHAINMAIL_CHESTPLATE, true);
            hasLeggings |= hasItem(stack, Items.CHAINMAIL_LEGGINGS, true);
            hasBoots |= hasItem(stack, Items.CHAINMAIL_BOOTS, true);

            if (CREEPER_NON_ALLOWED.contains(stack.getItem())) return false;
        }

        return hasCobweb && hasCreeper && hasWater && hasTnt && hasGaps && hasTotem && hasPearls && hasCrossbow &&
                hasBow && hasShield && hasSword && hasAxe && hasHelmet && hasChestplate && hasLeggings && hasBoots;
    }

    private static boolean checkManhunt(PlayerInventory playerInventory) {
        boolean hasCobweb = false;
        boolean hasWater = false;
        boolean hasLava = false;
        boolean hasSponge = false;
        boolean hasTnt = false;
        boolean hasGaps = false;
        boolean hasSlime = false;
        boolean hasFlint = false;
        boolean hasPearls = false;
        boolean hasCrossbow = false;
        boolean hasBow = false;
        boolean hasShield = false;
        boolean hasSword = false;
        boolean hasAxe = false;
        boolean hasHelmet = false;
        boolean hasChestplate = false;
        boolean hasLeggings = false;
        boolean hasBoots = false;

        for (int i = 0; i < playerInventory.size(); i++) {
            ItemStack stack = playerInventory.getStack(i);

            hasCobweb |= hasItem(stack, Items.COBWEB);
            hasWater |= hasItem(stack, Items.WATER_BUCKET);
            hasLava |= hasItem(stack, Items.LAVA_BUCKET);
            hasSponge |= hasItem(stack, Items.SPONGE);
            hasTnt |= hasItem(stack, Items.TNT);
            hasGaps |= hasItem(stack, Items.GOLDEN_APPLE);
            hasSlime |= hasItem(stack, Items.SLIME_BLOCK);
            hasFlint |= hasItem(stack, Items.FLINT_AND_STEEL);
            hasPearls |= hasItem(stack, Items.ENDER_PEARL);
            hasCrossbow |= hasItem(stack, Items.CROSSBOW, false);
            hasBow |= hasItem(stack, Items.BOW, false);
            hasShield |= hasItem(stack, Items.SHIELD);
            hasSword |= hasItem(stack, Items.IRON_SWORD, true);
            hasAxe |= hasItem(stack, Items.IRON_AXE);
            hasHelmet |= hasItem(stack, Items.IRON_HELMET, true);
            hasChestplate |= hasItem(stack, Items.DIAMOND_CHESTPLATE, true);
            hasLeggings |= hasItem(stack, Items.DIAMOND_LEGGINGS, true);
            hasBoots |= hasItem(stack, Items.IRON_BOOTS, true);

            if (MANHUNT_NON_ALLOWED.contains(stack.getItem())) return false;
        }

        return hasCobweb && hasWater && hasLava && hasSponge && hasTnt && hasGaps && hasSlime && hasFlint && hasPearls &&
                hasCrossbow && hasBow && hasShield && hasSword && hasAxe && hasHelmet && hasChestplate && hasLeggings && hasBoots;
    }

    private static boolean checkDiamondSmp(PlayerInventory playerInventory) {
        boolean hasGaps = false;
        boolean hasWater = false;
        boolean hasCobweb = false;
        boolean hasChorus = false;
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
            hasWater |= hasItem(stack, Items.WATER_BUCKET);
            hasCobweb |= hasItem(stack, Items.COBWEB);
            hasChorus |= hasItem(stack, Items.CHORUS_FRUIT);
            hasPotions |= hasItem(stack, Items.SPLASH_POTION);
            hasTotem |= hasItem(stack, Items.TOTEM_OF_UNDYING);
            hasXp |= hasItem(stack, Items.EXPERIENCE_BOTTLE);
            hasPearls |= hasItem(stack, Items.ENDER_PEARL);
            hasShield |= hasItem(stack, Items.SHIELD);
            hasEnchantedSword |= hasItem(stack, Items.DIAMOND_SWORD, true);
            hasEnchantedAxe |= hasItem(stack, Items.DIAMOND_AXE, true);
            hasEnchantedHelmet |= hasItem(stack, Items.DIAMOND_HELMET, true);
            hasEnchantedChestplate |= hasItem(stack, Items.DIAMOND_CHESTPLATE, true);
            hasEnchantedLeggings |= hasItem(stack, Items.DIAMOND_LEGGINGS, true);
            hasEnchantedBoots |= hasItem(stack, Items.DIAMOND_BOOTS, true);

            if (DIAMOND_SMP_NON_ALLOWED.contains(stack.getItem())) return false;
        }

        return hasGaps && hasWater && hasCobweb && hasChorus && hasPotions && hasTotem && hasXp && hasPearls && hasShield && hasEnchantedAxe &&
                hasEnchantedSword && hasEnchantedHelmet && hasEnchantedChestplate && hasEnchantedLeggings && hasEnchantedBoots;
    }

    private static boolean checkBow(PlayerInventory playerInventory) {
        boolean hasBow = false;
        boolean hasArrow = false;
        boolean hasEnchantedHelmet = false;
        boolean hasEnchantedChestplate = false;
        boolean hasEnchantedLeggings = false;
        boolean hasEnchantedBoots = false;

        for (int i = 0; i < playerInventory.size(); i++) {
            ItemStack stack = playerInventory.getStack(i);

            hasBow |= hasItem(stack, Items.BOW, true);
            hasArrow |= hasItem(stack, Items.ARROW);
            hasEnchantedHelmet |= hasItem(stack, Items.IRON_HELMET, true);
            hasEnchantedChestplate |= hasItem(stack, Items.IRON_CHESTPLATE, true);
            hasEnchantedLeggings |= hasItem(stack, Items.IRON_LEGGINGS, true);
            hasEnchantedBoots |= hasItem(stack, Items.IRON_BOOTS, true);

            if (BOW_NON_ALLOWED.contains(stack.getItem())) return false;
        }

        return hasBow && hasArrow && hasEnchantedHelmet && hasEnchantedChestplate && hasEnchantedLeggings && hasEnchantedBoots;
    }

    private static boolean checkBed(PlayerInventory playerInventory) {
        boolean hasTotem = false;
        boolean hasXp = false;
        boolean hasPearls = false;
        boolean hasGaps = false;
        boolean hasBed = false;
        boolean hasObsidian = false;
        boolean hasSword = false;
        boolean hasPickaxe = false;
        boolean hasEnchantedHelmet = false;
        boolean hasEnchantedChestplate = false;
        boolean hasEnchantedLeggings = false;
        boolean hasEnchantedBoots = false;

        for (int i = 0; i < playerInventory.size(); i++) {
            ItemStack stack = playerInventory.getStack(i);

            hasTotem |= hasItem(stack, Items.TOTEM_OF_UNDYING);
            hasXp |= hasItem(stack, Items.EXPERIENCE_BOTTLE);
            hasPearls |= hasItem(stack, Items.ENDER_PEARL);
            hasGaps |= hasItem(stack, Items.GOLDEN_APPLE);
            hasBed |= (hasItem(stack, Items.BLACK_BED) || hasItem(stack, Items.BLUE_BED) || hasItem(stack, Items.ORANGE_BED) || hasItem(stack, Items.BROWN_BED) ||
                    hasItem(stack, Items.RED_BED) || hasItem(stack, Items.WHITE_BED) || hasItem(stack, Items.GREEN_BED) || hasItem(stack, Items.YELLOW_BED));
            hasObsidian |= hasItem(stack, Items.OBSIDIAN);
            hasSword |= hasItem(stack, Items.NETHERITE_SWORD, true);
            hasPickaxe |= hasItem(stack, Items.NETHERITE_PICKAXE, true);
            hasEnchantedHelmet |= hasItem(stack, Items.NETHERITE_HELMET, true);
            hasEnchantedChestplate |= hasItem(stack, Items.NETHERITE_CHESTPLATE, true);
            hasEnchantedLeggings |= hasItem(stack, Items.NETHERITE_LEGGINGS, true);
            hasEnchantedBoots |= hasItem(stack, Items.NETHERITE_BOOTS, true);

            if (BED_NON_ALLOWED.contains(stack.getItem())) return false;
        }

        return hasTotem && hasXp && hasPearls && hasGaps && hasBed && hasObsidian && hasSword && hasPickaxe && hasEnchantedHelmet &&
                hasEnchantedChestplate && hasEnchantedLeggings && hasEnchantedBoots;
    }

    private static boolean checkOgVanilla(PlayerInventory playerInventory) {
        boolean hasSteak = false;
        boolean hasGaps = false;
        boolean hasPotions = false;
        boolean hasPearls = false;
        boolean hasEnchantedBow = false;
        boolean hasEnchantedSword = false;
        boolean hasEnchantedPickaxe = false;
        boolean hasEnchantedHelmet = false;
        boolean hasEnchantedChestplate = false;
        boolean hasEnchantedLeggings = false;
        boolean hasEnchantedBoots = false;

        for (int i = 0; i < playerInventory.size(); i++) {
            ItemStack stack = playerInventory.getStack(i);

            hasSteak |= hasItem(stack, Items.COOKED_BEEF);
            hasGaps |= hasItem(stack, Items.GOLDEN_APPLE);
            hasPotions |= hasItem(stack, Items.SPLASH_POTION);
            hasPearls |= hasItem(stack, Items.ENDER_PEARL);
            hasEnchantedBow |= hasItem(stack, Items.BOW, true);
            hasEnchantedSword |= hasItem(stack, Items.DIAMOND_SWORD, true);
            hasEnchantedPickaxe |= hasItem(stack, Items.DIAMOND_PICKAXE, true);
            hasEnchantedHelmet |= hasItem(stack, Items.DIAMOND_HELMET, true);
            hasEnchantedChestplate |= hasItem(stack, Items.DIAMOND_CHESTPLATE, true);
            hasEnchantedLeggings |= hasItem(stack, Items.DIAMOND_LEGGINGS, true);
            hasEnchantedBoots |= hasItem(stack, Items.DIAMOND_BOOTS, true);

            if (OG_VANILLA_NON_ALLOWED.contains(stack.getItem())) return false;
        }

        return hasSteak && hasGaps && hasPotions && hasPearls && hasEnchantedBow && hasEnchantedSword && hasEnchantedPickaxe && hasEnchantedHelmet && hasEnchantedChestplate && hasEnchantedLeggings && hasEnchantedBoots;
    }

    private static boolean checkTrident(PlayerInventory playerInventory) {
        boolean hasCobweb = false;
        boolean hasWater = false;
        boolean hasGaps = false;
        boolean hasSponge = false;
        boolean hasTotem = false;
        boolean hasShears = false;
        boolean hasTrident = false;
        boolean hasHelmet = false;
        boolean hasChestplate = false;
        boolean hasLeggings = false;
        boolean hasBoots = false;

        for (int i = 0; i < playerInventory.size(); i++) {
            ItemStack stack = playerInventory.getStack(i);

            hasCobweb |= hasItem(stack, Items.COBWEB);
            hasWater |= hasItem(stack, Items.WATER_BUCKET);
            hasGaps |= hasItem(stack, Items.GOLDEN_APPLE);
            hasSponge |= hasItem(stack, Items.SPONGE);
            hasTotem |= hasItem(stack, Items.TOTEM_OF_UNDYING);
            hasShears |= hasItem(stack, Items.SHEARS, true);
            hasTrident |= hasItem(stack, Items.TRIDENT, true);
            hasHelmet |= hasItem(stack, Items.TURTLE_HELMET, false);
            hasChestplate |= hasItem(stack, Items.GOLDEN_CHESTPLATE, true);
            hasLeggings |= hasItem(stack, Items.GOLDEN_LEGGINGS, true);
            hasBoots |= hasItem(stack, Items.GOLDEN_BOOTS, true);

            if (TRIDENT_NON_ALLOWED.contains(stack.getItem())) return false;
        }

        return hasCobweb && hasWater && hasGaps && hasSponge && hasTotem && hasShears &&
                hasTrident && hasHelmet && hasChestplate && hasLeggings && hasBoots;
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

    private static final Set<Item> MINECART_NON_ALLOWED = Set.of(
            Items.SHIELD,
            Items.MACE,

            Items.END_CRYSTAL,
            Items.OBSIDIAN,
            Items.RESPAWN_ANCHOR,
            Items.GLOWSTONE
    );

    private static final Set<Item> DIAMOND_VANILLA_NON_ALLOWED = Set.of(
            Items.NETHERITE_SWORD,
            Items.NETHERITE_PICKAXE,
            Items.NETHERITE_HELMET,
            Items.NETHERITE_CHESTPLATE,
            Items.NETHERITE_LEGGINGS,
            Items.NETHERITE_BOOTS
    );

    private static final Set<Item> DEBUFF_NON_ALLOWED = Set.of(
            Items.SHIELD,
            Items.TOTEM_OF_UNDYING,
            Items.EXPERIENCE_BOTTLE,

            Items.DIAMOND_SWORD,
            Items.DIAMOND_AXE,
            Items.DIAMOND_PICKAXE,
            Items.DIAMOND_HELMET,
            Items.DIAMOND_CHESTPLATE,
            Items.DIAMOND_LEGGINGS,
            Items.DIAMOND_BOOTS,

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

    private static final Set<Item> SUBTIERS_ELYTRA_NON_ALLOWED = Set.of(
            Items.EXPERIENCE_BOTTLE,

            Items.NETHERITE_HELMET,
            Items.NETHERITE_CHESTPLATE,
            Items.NETHERITE_LEGGINGS,
            Items.NETHERITE_BOOTS,

            Items.DIAMOND_HELMET,
            Items.DIAMOND_CHESTPLATE,
            Items.DIAMOND_LEGGINGS,
            Items.DIAMOND_BOOTS,

            Items.END_CRYSTAL,
            Items.OBSIDIAN,
            Items.RESPAWN_ANCHOR,
            Items.GLOWSTONE
    );

    private static final Set<Item> SPEED_NON_ALLOWED = Set.of(
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

    private static final Set<Item> CREEPER_NON_ALLOWED = Set.of(
            Items.ELYTRA,
            Items.WIND_CHARGE,
            Items.EXPERIENCE_BOTTLE,
            Items.SPLASH_POTION,

            Items.DIAMOND_SWORD,
            Items.DIAMOND_AXE,
            Items.DIAMOND_PICKAXE,
            Items.DIAMOND_HELMET,
            Items.DIAMOND_CHESTPLATE,
            Items.DIAMOND_LEGGINGS,
            Items.DIAMOND_BOOTS,

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

    private static final Set<Item> MANHUNT_NON_ALLOWED = Set.of(
            Items.TOTEM_OF_UNDYING,
            Items.WIND_CHARGE,
            Items.EXPERIENCE_BOTTLE,
            Items.SPLASH_POTION,

            Items.DIAMOND_SWORD,
            Items.DIAMOND_AXE,
            Items.DIAMOND_HELMET,
            Items.DIAMOND_BOOTS,

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

    private static final Set<Item> DIAMOND_SMP_NON_ALLOWED = Set.of(
            Items.WIND_CHARGE,

            Items.NETHERITE_SWORD,
            Items.NETHERITE_AXE,
            Items.NETHERITE_HELMET,
            Items.NETHERITE_CHESTPLATE,
            Items.NETHERITE_LEGGINGS,
            Items.NETHERITE_BOOTS,

            Items.END_CRYSTAL,
            Items.OBSIDIAN,
            Items.RESPAWN_ANCHOR,
            Items.GLOWSTONE
    );

    private static final Set<Item> BOW_NON_ALLOWED = Set.of(
            Items.ELYTRA,
            Items.WIND_CHARGE,
            Items.EXPERIENCE_BOTTLE,
            Items.SPLASH_POTION,
            Items.ENDER_PEARL,
            Items.SHIELD,
            Items.TOTEM_OF_UNDYING,

            Items.DIAMOND_SWORD,
            Items.DIAMOND_AXE,
            Items.DIAMOND_PICKAXE,
            Items.DIAMOND_HELMET,
            Items.DIAMOND_CHESTPLATE,
            Items.DIAMOND_LEGGINGS,
            Items.DIAMOND_BOOTS,

            Items.NETHERITE_SWORD,
            Items.NETHERITE_AXE,
            Items.NETHERITE_HELMET,
            Items.NETHERITE_CHESTPLATE,
            Items.NETHERITE_LEGGINGS,
            Items.NETHERITE_BOOTS,

            Items.END_CRYSTAL,
            Items.OBSIDIAN,
            Items.RESPAWN_ANCHOR,
            Items.GLOWSTONE
    );

    private static final Set<Item> BED_NON_ALLOWED = Set.of(
            Items.WIND_CHARGE,
            Items.MACE,
            Items.SHIELD,

            Items.END_CRYSTAL,
            Items.RESPAWN_ANCHOR,
            Items.GLOWSTONE
    );

    private static final Set<Item> OG_VANILLA_NON_ALLOWED = Set.of(
            Items.ELYTRA,
            Items.WIND_CHARGE,
            Items.CROSSBOW,
            Items.COBWEB,
            Items.WATER_BUCKET,
            Items.LAVA_BUCKET,
            Items.SHIELD,
            Items.TOTEM_OF_UNDYING,

            Items.DIAMOND_AXE,

            Items.NETHERITE_SWORD,
            Items.NETHERITE_AXE,
            Items.NETHERITE_HELMET,
            Items.NETHERITE_CHESTPLATE,
            Items.NETHERITE_LEGGINGS,
            Items.NETHERITE_BOOTS,

            Items.END_CRYSTAL,
            Items.OBSIDIAN,
            Items.RESPAWN_ANCHOR,
            Items.GLOWSTONE
    );

    private static final Set<Item> TRIDENT_NON_ALLOWED = Set.of(
            Items.ELYTRA,
            Items.WIND_CHARGE,
            Items.CROSSBOW,
            Items.LAVA_BUCKET,
            Items.SHIELD,

            Items.DIAMOND_SWORD,
            Items.DIAMOND_AXE,
            Items.DIAMOND_PICKAXE,
            Items.DIAMOND_HELMET,
            Items.DIAMOND_CHESTPLATE,
            Items.DIAMOND_LEGGINGS,
            Items.DIAMOND_BOOTS,

            Items.NETHERITE_SWORD,
            Items.NETHERITE_AXE,
            Items.NETHERITE_HELMET,
            Items.NETHERITE_CHESTPLATE,
            Items.NETHERITE_LEGGINGS,
            Items.NETHERITE_BOOTS,

            Items.END_CRYSTAL,
            Items.OBSIDIAN,
            Items.RESPAWN_ANCHOR,
            Items.GLOWSTONE
    );
}