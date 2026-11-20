package com.tiers.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.authlib.GameProfile;
import com.tiers.TiersClient;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class ModifyNametagsClientMixin {
    @Shadow
    public abstract String getScoreboardName();

    @Shadow
    public abstract GameProfile getGameProfile();

    @Unique
    private int tiers_cacheVersion;

    @Unique
    private Component tiers_lastOriginal;

    @Unique
    private Component tiers_cached;

    @ModifyReturnValue(at = @At("RETURN"), method = "getDisplayName")
    private Component modifyDisplayName(Component original) {
        if (!TiersClient.toggleMod)
            return original;

        if (original == tiers_lastOriginal && tiers_cacheVersion == TiersClient.cacheVersion)
            return tiers_cached;
        tiers_cacheVersion = TiersClient.cacheVersion;
        tiers_lastOriginal = original;

        return tiers_cached = TiersClient.addGetPlayer(getScoreboardName(), false, getGameProfile().id()).getFullName(original);
    }
}