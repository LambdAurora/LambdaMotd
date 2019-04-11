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
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import org.aperlambda.kimiko.Command;
import org.aperlambda.kimiko.CommandBuilder;
import org.aperlambda.kimiko.CommandContext;
import org.aperlambda.kimiko.CommandResult;
import org.aperlambda.lambdacommon.resources.ResourceName;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.shulker.core.Shulker;
import org.shulker.core.commands.BukkitCommandExecutor;
import org.shulker.core.commands.HelpSubCommand;

import static net.md_5.bungee.api.ChatColor.*;

public class LambdaMotdCommand implements BukkitCommandExecutor
{
    private final BaseComponent[]        HOVER_MESSAGE = new ComponentBuilder("Clicks on the command to execute it!").color(GRAY).create();
    private       LambdaMotd             plugin;
    private       Command<CommandSender> command;

    public LambdaMotdCommand(LambdaMotd plugin)
    {
        this.plugin = plugin;
        command = new CommandBuilder<CommandSender>(new ResourceName(plugin.getName(), "lambdamotd"))
                .usage("<command> [subcommand]")
                .description("The LambdaMotd command.")
                .permission("lambdamotd.commands.execute")
                .executor(this)
                .build();
        var help_command = new HelpSubCommand("lambdamotd.commands.help", ChatColor.DARK_AQUA, ChatColor.DARK_GREEN, ChatColor.GOLD);
        help_command.set_title("LambdaMotd");
        command.add_sub_command(help_command.get_result_command());
        command.add_sub_command(new ReloadCommand(plugin).get_command());
        Shulker.get_commands().register(command);
    }

    @Override
    public @NotNull CommandResult execute(CommandContext<CommandSender> context, @NotNull Command<CommandSender> command, String label, String[] args)
    {
        if (args.length > 0)
            return CommandResult.ERROR_USAGE;

        context.send_message(DARK_AQUA + "Lambda" + DARK_GREEN + "Motd" + GRAY + " v" + GOLD + plugin.getDescription().getVersion());
        context.send_message(GRAY + "Author: " + LIGHT_PURPLE + "LambdAurora");
        context.send_message(GRAY + "GitHub: " + RED + "https://github.com/LambdAurora/LambdaMotd.git");

        if (context.get_sender() instanceof Player) {
            var player = Shulker.get_mc().get_player((Player) context.get_sender());

            player.send_message(new ComponentBuilder("Help: ").color(GRAY).append("/lambdamotd help").color(GOLD)
                    .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/lambdamotd help"))
                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, HOVER_MESSAGE)).create());
        } else context.send_message(GRAY + "Help: " + GOLD + "/lambdamotd help");

        return CommandResult.SUCCESS;
    }

    public void print_to(CommandSender sender, BaseComponent... components)
    {
        sender.sendMessage(BaseComponent.toLegacyText(components));
    }
}
