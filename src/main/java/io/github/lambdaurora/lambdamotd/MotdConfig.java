/*
 * Copyright © 2019 LambdAurora <aurora42lambda@gmail.com>
 *
 * This file is part of lambdamotd.
 *
 * Licensed under the MIT license. For more information,
 * see the LICENSE file.
 */

package io.github.lambdaurora.lambdamotd;

import org.aperlambda.lambdacommon.resources.ResourceName;
import org.mcelytra.core.ServerPing;
import org.shulker.core.Shulker;
import org.shulker.core.config.ConfigManager;
import org.shulker.core.config.YamlConfig;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents the configuration of LambdaMotd
 */
public class MotdConfig
{
    private LambdaMotd   plugin;
    private YamlConfig   config;
    private File         favicon_dir;
    private List<String> favicons     = new ArrayList<>();
    private List<String> random_motds = new ArrayList<>();

    public MotdConfig(LambdaMotd plugin)
    {
        this.plugin = plugin;
    }

    /**
     * Loads the configuration and handle wrong version.
     */
    public void load()
    {
        if (this.config == null) {
            this.config = Shulker.get_configs().new_yaml_config(new ResourceName("lambdamotd", "config"), plugin.getResource("config.yml"));
            this.favicon_dir = new File(config.get_file().getParentFile(), "favicons");

            if (!this.favicon_dir.exists())
                if (!this.favicon_dir.mkdirs())
                    Shulker.log_error(LambdaMotd.CONSOLE_PREFIX, "Cannot create favicon folder in '" + this.favicon_dir.getParentFile().getPath() + "'!");
        }

        Shulker.log_info(LambdaMotd.CONSOLE_PREFIX, "Loading configuration...");
        config.load();

        if (!config.get("version", "error").equalsIgnoreCase("1")) {
            try {
                Files.copy(config.get_file().toPath(), new File(config.get_file().getParent(), "config.yml.backup").toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                Shulker.log_error(LambdaMotd.CONSOLE_PREFIX, "Cannot backup config file!");
                e.printStackTrace();
            }

            // Replacing
            ConfigManager.get().save_resource(plugin.getResource("config.yml"), new ResourceName("lambdamotd", "config"), "yml", true);
            config.load();
        }

        // Load random motd
        {
            Map<String, Object> values = config.get_config().getConfigurationSection("motd.random_values").getValues(false);
            this.random_motds.clear();
            values.forEach((key, value) -> random_motds
                    .add(config.at("motd.random_values." + key + ".line1", "") + "\n" + config.at("motd.random_values." + key + ".line2", "")));
        }

        if (has_favicon() && this.favicon_dir.exists()) {
            var files = this.favicon_dir.listFiles();
            if (files != null) {
                var temp_server_ping = new ServerPing();
                favicons = Arrays.stream(files)
                        .filter(file -> file.getName().endsWith(".png"))
                        .map(file -> {
                            try {
                                temp_server_ping.load_favicon(file);
                                return temp_server_ping.get_favicon();
                            } catch (RuntimeException e) {
                                // not 64x64
                                try {
                                    var image = ImageIO.read(file);
                                    var resized_image = new BufferedImage(64, 64, BufferedImage.TYPE_4BYTE_ABGR);
                                    var graphics = resized_image.createGraphics();
                                    graphics.drawImage(image, 0, 0, 64, 64, null);
                                    graphics.dispose();
                                    var output = new ByteArrayOutputStream();
                                    ImageIO.write(resized_image, "png", output);
                                    output.flush();
                                    return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
                                } catch (IOException e1) {
                                    Shulker.log_error(LambdaMotd.CONSOLE_PREFIX, "Cannot load the favicon file '" + file.getName() + "'!");
                                    e1.printStackTrace();
                                    return null;
                                }
                            } catch (IOException e) {
                                Shulker.log_error(LambdaMotd.CONSOLE_PREFIX, "Cannot load the favicon file '" + file.getName() + "'!");
                                e.printStackTrace();
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                Shulker.log_info(LambdaMotd.CONSOLE_PREFIX, favicons.size() + " favicon(s) loaded!");
            }
        }

        if (has_custom_version() && has_custom_player_count())
            Shulker.log_error(LambdaMotd.CONSOLE_PREFIX, "You have custom version and custom player count enabled at the same time! The result may not be the expectation.");
    }

    public String get_default_name()
    {
        return this.config.get("default_name", "Guest");
    }

    public boolean has_player_cache_enabled()
    {
        return this.config.get("player_cache.enable", false, boolean.class);
    }

    public long get_player_cache_duration()
    {
        return this.config.get("player_cache.duration", 2628000000L, long.class);
    }

    public boolean has_custom_motd()
    {
        return this.config.get("motd.enable", true, boolean.class);
    }

    public String get_motd()
    {
        return this.config.get("motd.line1", "Welcome on &6%server%&r!") + "\n" + this.config.get("motd.line2", "&aHave fun!");
    }

    public boolean has_random_motd()
    {
        return this.config.get("motd.random", false, boolean.class);
    }

    public List<String> get_random_motds()
    {
        return this.random_motds;
    }

    public String pick_random_motd()
    {
        if (has_random_motd())
            return this.random_motds.get(plugin.get_random().nextInt(this.random_motds.size()));
        else
            return get_motd();
    }

    public boolean has_favicon()
    {
        return this.config.get("favicon.enable", true, boolean.class);
    }

    public boolean has_random_favicon()
    {
        return this.config.get("favicon.random", false, boolean.class);
    }

    public boolean has_player_favicon()
    {
        return this.config.get("favicon.player", false, boolean.class);
    }

    public List<String> get_favicons()
    {
        return this.favicons;
    }

    public boolean has_custom_version()
    {
        return this.config.get("playercount.enable_custom_version", false, boolean.class);
    }

    public String get_custom_version()
    {
        return this.config.get("playercount.custom_version", "MC Server %version%");
    }

    public boolean has_custom_player_count()
    {
        return this.config.get("playercount.enable", true, boolean.class);
    }

    public String get_custom_player_count_content()
    {
        return this.config.get("playercount.content", "&7>>> &c%online%&7/&c%max_players%");
    }

    public boolean has_random_player_count()
    {
        return this.config.get("playercount.random", false, boolean.class);
    }

    public List<String> get_random_player_count_values()
    {
        return this.config.get_config().getStringList("playercount.random_values");
    }

    public String pick_player_count_content()
    {
        if (has_random_player_count()) {
            var random_list = get_random_player_count_values();
            return random_list.get(this.plugin.get_random().nextInt(random_list.size()));
        } else
            return get_custom_player_count_content();
    }

    public boolean has_custom_playerlist()
    {
        return this.config.get("playerlist.enable", false, boolean.class);
    }

    public List<String> get_playerlist_contents()
    {
        return this.config.get_config().getStringList("playerlist.contents");
    }
}
