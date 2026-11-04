package com.tiers.mixin.client;

import com.mojang.authlib.GameProfile;
import com.tiers.TiersClient;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class AddRenderedPlayersClientMixin {
    @Inject(at = @At(value = "TAIL"), method = "<init>")
    private void onConstruct(final Level level, final GameProfile gameProfile, CallbackInfo ci) {
        if (TiersClient.toggleMod)
            TiersClient.addGetPlayer(gameProfile.name(), false, gameProfile.id());
    }
}