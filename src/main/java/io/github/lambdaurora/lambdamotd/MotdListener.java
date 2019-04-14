/*
 * Copyright © 2019 LambdAurora <aurora42lambda@gmail.com>
 *
 * This file is part of lambdamotd.
 *
 * Licensed under the MIT license. For more information,
 * see the LICENSE file.
 */

package io.github.lambdaurora.lambdamotd;

import com.mojang.authlib.GameProfile;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.shulker.core.Shulker;
import org.shulker.core.events.PacketEvent;
import org.shulker.core.events.PacketListener;
import org.shulker.core.packets.mc.status.ShulkerPacketStatusOutServerInfo;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.UUID;

public class MotdListener implements PacketListener, Listener
{
    private LambdaMotd plugin;
    private MotdConfig config;

    public MotdListener(LambdaMotd plugin)
    {
        this.plugin = plugin;
        this.config = plugin.get_configuration();
    }

    @Override
    public void on_packet_receive(PacketEvent packetEvent)
    {
    }

    @Override
    public void on_packet_send(PacketEvent packet_event)
    {
        if (packet_event.get_packet() instanceof ShulkerPacketStatusOutServerInfo) {
            var cached_player = plugin.get_player_cache().get_cached_player(packet_event.get_target_ip().toString());
            ShulkerPacketStatusOutServerInfo<?> packet = (ShulkerPacketStatusOutServerInfo<?>) packet_event.get_packet();
            var server_ping = packet.get_server_ping();

            var players = Bukkit.getOnlinePlayers();

            if (config.has_custom_motd())
                server_ping.set_motd(TextComponent.fromLegacyText(format_string(cached_player.name, config.pick_random_motd())));

            if (config.has_custom_version())
                server_ping.set_version_name(format_string(cached_player.name, config.get_custom_version()));

            if (config.has_custom_player_count()) {
                server_ping.set_version_name(format_string(cached_player.name, config.pick_player_count_content()));
                // Modifies the protocol to allow the display of the custom player count.
                server_ping.set_protocol(0);
            }

            if (config.has_favicon()) {
                if (config.has_random_favicon() && (!config.get_favicons().isEmpty() || config.has_player_favicon())) {
                    var list = new ArrayList<>(config.get_favicons());
                    if (config.has_player_favicon() && !cached_player.favicon.isEmpty())
                        list.add(cached_player.favicon);
                    if (!list.isEmpty()) {
                        int index = plugin.get_random().nextInt(list.size());
                        server_ping.set_favicon(list.get(index));
                    }
                } else if (config.has_player_favicon())
                    if (!cached_player.favicon.isEmpty())
                        server_ping.set_favicon(cached_player.favicon);
            }

            var playerlist_mode = config.get_playerlist_mode();
            Player players_array[] = new Player[players.size()];
            players_array = players.toArray(players_array);

            if (playerlist_mode == PlayerlistMode.NAME) {
                server_ping.set_players(new ArrayList<>());
                for (int i = 0; i < players.size() && i < this.config.get_max_displayed_players(); i++) {
                    var player = players_array[i];
                    server_ping.add_player(new GameProfile(player.getUniqueId(), player.getName()));
                }
                int remaining = players.size() - this.config.get_max_displayed_players();
                if (remaining > 0)
                    server_ping.add_player(new GameProfile(UUID.randomUUID(),
                            format_string(cached_player.name, this.config.get_remaining_players_message()).replace("%remaining%", String.valueOf(remaining))));
            } else if (playerlist_mode == PlayerlistMode.DISPLAY_NAME) {
                server_ping.set_players(new ArrayList<>());
                for (int i = 0; i < players.size() && i < this.config.get_max_displayed_players(); i++) {
                    var player = players_array[i];
                    server_ping.add_player(new GameProfile(player.getUniqueId(), player.getDisplayName()));
                }
                int remaining = players.size() - this.config.get_max_displayed_players();
                if (remaining > 0)
                    server_ping.add_player(new GameProfile(UUID.randomUUID(),
                            format_string(cached_player.name, this.config.get_remaining_players_message()).replace("%remaining%", String.valueOf(remaining))));
            } else if (playerlist_mode == PlayerlistMode.CUSTOM) {
                server_ping.set_players(new ArrayList<>());
                config.get_playerlist_contents().forEach(line -> server_ping.add_player(new GameProfile(UUID.randomUUID(), format_string(cached_player.name, line))));
            } else if (playerlist_mode == PlayerlistMode.DISABLED)
                server_ping.set_players(new ArrayList<>());

            // Done, need to update the NMS object.
            packet.set_server_ping(server_ping);
        }
    }

    @EventHandler
    public void on_player_join(PlayerJoinEvent event)
    {
        if (!config.has_player_cache_enabled())
            return;
        // Reset every months
        if (System.currentTimeMillis() - plugin.get_player_cache().get_last_cleanup() >= config.get_player_cache_duration())
            plugin.get_player_cache().reset();

        String ip = event.getPlayer().getAddress().toString();
        var cached_player = plugin.get_player_cache().get_cached_player(ip);
        var player_name = event.getPlayer().getName();

        if (!cached_player.name.equals(player_name)) {
            cached_player.name = player_name;
            cached_player.favicon = get_avatar(player_name);
            plugin.get_player_cache().put(ip, cached_player);
        }
    }

    public String format_string(String name, String input)
    {
        String output;
        // COLORS
        output = ChatColor.translateAlternateColorCodes('&', input);
        // SYMBOLS
        output = Shulker.get_symbols_manager().replace_with_symbols(output);
        // VARIABLES
        output = output.replace("%server%", Bukkit.getServerName())
                .replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("%max_players%", String.valueOf(Bukkit.getMaxPlayers()))
                .replace("%version%", plugin.get_server_version())
                .replace("%player%", get_player_name(name));
        return output;
    }

    public String get_player_name(String cached_name)
    {
        return cached_name != null && !cached_name.isEmpty() ? cached_name : this.config.get_default_name();
    }

    public String get_avatar(String player)
    {
        try {
            BufferedImage image = ImageIO.read(new URL("https://minotar.net/helm/" + player + "/64.png"));
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (IOException e) {
            return null;
        }
    }
}
