/*
 * Copyright © 2019 LambdAurora <aurora42lambda@gmail.com>
 *
 * This file is part of lambdamotd.
 *
 * Licensed under the MIT license. For more information,
 * see the LICENSE file.
 */

package io.github.lambdaurora.lambdamotd.commands;

import io.github.lambdaurora.lambdamotd.LambdaMotd;
import org.aperlambda.kimiko.Command;
import org.aperlambda.kimiko.CommandBuilder;
import org.aperlambda.kimiko.CommandContext;
import org.aperlambda.kimiko.CommandResult;
import org.aperlambda.lambdacommon.resources.ResourceName;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.shulker.core.commands.BukkitCommandExecutor;

import java.util.Collections;

import static net.md_5.bungee.api.ChatColor.GREEN;

public class ReloadCommand implements BukkitCommandExecutor
{
    private LambdaMotd             plugin;
    private Command<CommandSender> command;

    public ReloadCommand(LambdaMotd plugin)
    {
        this.plugin = plugin;
        command = new CommandBuilder<CommandSender>(new ResourceName(plugin.getName(), "reload"))
                .usage("<command>")
                .description("Reloads the configuration of LambdaMotd.")
                .permission("lambdamotd.commands.reload")
                .executor(this)
                .tab_completer((context, command, label, args) -> Collections.emptyList())
                .build();
    }

    @Override
    public @NotNull CommandResult execute(CommandContext<CommandSender> context, @NotNull Command<CommandSender> command, String label, String[] args)
    {
        if (args.length > 0)
            return CommandResult.ERROR_USAGE;

        context.send_message(GREEN + "Reloading the configuration...");
        plugin.get_configuration().load();
        plugin.get_player_cache().load();

        return CommandResult.SUCCESS;
    }

    public Command<CommandSender> get_command()
    {
        return command;
    }
}
