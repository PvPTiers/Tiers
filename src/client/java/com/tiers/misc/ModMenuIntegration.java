package com.tiers.misc;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.tiers.screens.ConfigScreen;
import org.jspecify.annotations.NonNull;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public @NonNull ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ConfigScreen::getConfigScreen;
    }
}