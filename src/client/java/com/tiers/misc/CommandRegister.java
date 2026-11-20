package com.tiers.misc;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.tiers.TiersClient;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.commands.SharedSuggestionProvider;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class CommandRegister {
    private static final SuggestionProvider<FabricClientCommandSource> PLAYERS = (_, suggestionsBuilder) -> suggestPlayers(suggestionsBuilder);
    private static final List<Map.Entry<String, String>> COMMANDS = List.of(
            Map.entry("-toggle", "Toggle Tiers"),
            Map.entry("-config", "Open Tiers config screen"),
            Map.entry("-help", "Show Tiers help"),
            Map.entry("-debug", "Show Tiers debug info"),
            Map.entry("-clear", "Clear cached Tiers data"),
            Map.entry("-reset", "Reset Tiers settings"),
            Map.entry("-status", "Show Tiers status")
    );

    private static CompletableFuture<Suggestions> suggestPlayers(SuggestionsBuilder suggestionsBuilder) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.getConnection() == null)
            return suggestionsBuilder.buildFuture();

        String remaining = suggestionsBuilder.getRemaining().toLowerCase(Locale.ROOT);

        for (PlayerInfo playerListEntry : minecraft.getConnection().getOnlinePlayers()) {
            String name = playerListEntry.getProfile().name();
            if (name.length() > 2 && SharedSuggestionProvider.matchesSubStr(remaining, name.toLowerCase(Locale.ROOT)))
                suggestionsBuilder.suggest(name, () -> "Search tiers for " + name);
        }

        boolean showAllCommands = remaining.startsWith("-");
        for (Map.Entry<String, String> command : COMMANDS)
            if ((showAllCommands || command.getKey().equals("-config")) && SharedSuggestionProvider.matchesSubStr(remaining, command.getKey()))
                suggestionsBuilder.suggest(command.getKey(), command::getValue);

        return suggestionsBuilder.buildFuture();
    }

    public static void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((commandDispatcher, _) -> commandDispatcher.register(
                ClientCommands.literal("tiers").executes(ignored -> {
                            TiersClient.toggleMod(null);
                            return 1;
                        })
                        .then(ClientCommands.argument("Name", StringArgumentType.string()).suggests(PLAYERS).executes(context -> {
                                    TiersClient.tiersCommand(StringArgumentType.getString(context, "Name"));
                                    return 1;
                                })
                        )
        ));
    }
}