/*
 * Copyright © 2019 LambdAurora <aurora42lambda@gmail.com>
 *
 * This file is part of lambdamotd.
 *
 * Licensed under the MIT license. For more information,
 * see the LICENSE file.
 */

package io.github.lambdaurora.lambdamotd;

import io.github.lambdaurora.lambdamotd.commands.LambdaMotdCommand;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.shulker.core.Shulker;

import java.util.Random;

import static org.fusesource.jansi.Ansi.Color;
import static org.fusesource.jansi.Ansi.ansi;

public class LambdaMotd extends JavaPlugin
{
    public static final String      CONSOLE_PREFIX = ansi().fg(Color.WHITE).a("[").fg(Color.CYAN).a("Lambda")
            .fg(Color.GREEN).a("MOTD").fg(Color.WHITE).a("]").reset().toString();
    private final       Random      random         = new Random();
    private             MotdConfig  config;
    private             PlayerCache player_cache;
    private             String      server_version;

    @Override
    public void onLoad()
    {
        super.onLoad();
    }

    @Override
    public void onDisable()
    {
        super.onDisable();
    }

    @Override
    public void onEnable()
    {
        super.onEnable();

        if (!getServer().getPluginManager().isPluginEnabled("Shulker")) {
            System.out.println("DISABLING LAMBDAMOTD, PLEASE INSTALL SHULKER TO MAKE IT WORKS.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        server_version = Bukkit.getVersion().substring(Bukkit.getVersion().indexOf(':')).replace(" ", "").replace(")", "").replace(":", "");

        config = new MotdConfig(this);
        config.load();
        player_cache = new PlayerCache(config.has_player_cache_enabled());
        player_cache.load();
        new LambdaMotdCommand(this);
        var listener = new MotdListener(this);
        getServer().getPluginManager().registerEvents(listener, this);
        Shulker.register_packet_listener(listener);
    }

    public Random get_random()
    {
        return this.random;
    }

    public MotdConfig get_configuration()
    {
        return this.config;
    }

    public PlayerCache get_player_cache()
    {
        return this.player_cache;
    }

    public String get_server_version()
    {
        return this.server_version;
    }
}
