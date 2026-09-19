package com.tiers.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.authlib.GameProfile;
import com.tiers.TiersClient;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerInfo.class)
public abstract class ModifyTabClientMixin {
    @Shadow
    public abstract GameProfile getProfile();

    @Unique
    private int tiers_cacheVersion;

    @Unique
    private Component tiers_lastOriginal;

    @Unique
    private Component tiers_cached;

    @Inject(at = @At(value = "TAIL"), method = "<init>")
    private void onConstruct(GameProfile profile, boolean enforcesSecureChat, CallbackInfo ci) {
        if (TiersClient.toggleMod)
            TiersClient.addGetPlayer(profile.name(), false, profile.id());
    }

    @ModifyReturnValue(at = @At("RETURN"), method = "getTabListDisplayName")
    private Component modifyPlayerName(Component original) {
        if (!TiersClient.toggleMod || !TiersClient.toggleTab || original == null)
            return original;

        if (original == tiers_lastOriginal && tiers_cacheVersion == TiersClient.cacheVersion)
            return tiers_cached;
        tiers_cacheVersion = TiersClient.cacheVersion;
        tiers_lastOriginal = original;

        return tiers_cached = TiersClient.addGetPlayer(getProfile().name(), false, getProfile().id()).deepReplace(original);
    }
}