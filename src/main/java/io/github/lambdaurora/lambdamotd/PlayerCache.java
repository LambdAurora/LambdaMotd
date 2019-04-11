/*
 * Copyright © 2019 LambdAurora <aurora42lambda@gmail.com>
 *
 * This file is part of lambdamotd.
 *
 * Licensed under the MIT license. For more information,
 * see the LICENSE file.
 */

package io.github.lambdaurora.lambdamotd;

import com.google.common.io.FileWriteMode;
import com.google.common.io.Files;
import com.google.gson.JsonObject;
import org.aperlambda.lambdacommon.LambdaConstants;
import org.aperlambda.lambdacommon.config.Config;
import org.aperlambda.lambdacommon.config.json.JsonConfig;
import org.aperlambda.lambdacommon.config.json.VirtualJsonConfig;
import org.aperlambda.lambdacommon.resources.ResourceName;
import org.jetbrains.annotations.NotNull;
import org.shulker.core.Shulker;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;

public class PlayerCache
{
    private boolean            enabled;
    private Config<JsonObject> json;

    public PlayerCache(boolean enabled)
    {
        this.enabled = enabled;
    }

    public void load()
    {
        if (json == null && enabled) {
            json = Shulker.get_configs().new_json_config(new ResourceName("lambdamotd", "player-cache"));
            ((JsonConfig) json).set_auto_save(true);
        } else if (json == null) {
            json = new VirtualJsonConfig();
            json.set("last_cleanup", System.currentTimeMillis());
            json.set("cache", new HashMap<String, String>());
        }

        if (json instanceof JsonConfig && !((JsonConfig) json).get_file().exists()) {
            VirtualJsonConfig temp_config = new VirtualJsonConfig();
            temp_config.set("last_cleanup", System.currentTimeMillis());
            temp_config.set("cache", new HashMap<String, String>());
            try {
                Files.asCharSink(((JsonConfig) json).get_file(), Charset.defaultCharset(), new FileWriteMode[0]).write(LambdaConstants.GSON_PRETTY.toJson(temp_config.get_config()));
            } catch (IOException e) {
                Shulker.log_error(LambdaMotd.CONSOLE_PREFIX, "Cannot save the default player cache!");
                e.printStackTrace();
            }
        }

        if (json instanceof JsonConfig)
            ((JsonConfig) json).load();
    }

    public long get_last_cleanup()
    {
        long lastCleanup = json.get("last_cleanup", 0L, long.class);
        if (lastCleanup == 0) {
            lastCleanup = System.currentTimeMillis();
            json.set("last_cleanup", lastCleanup);
        }
        return lastCleanup;
    }

    public @NotNull CachedPlayer get_cached_player(String ip)
    {
        var cache = json.get_config().getAsJsonObject("cache");
        if (cache != null && cache.has(format_ip(ip))) {
            var cached = cache.getAsJsonObject(format_ip(ip));
            var name = cached.get("name").getAsString();
            var favicon = cached.get("favicon").getAsString();
            name = new String(Base64.getDecoder().decode(name.getBytes()), StandardCharsets.UTF_8);
            favicon = new String(Base64.getDecoder().decode(favicon.getBytes()), StandardCharsets.UTF_8);
            return new CachedPlayer(name, favicon);
        }
        return new CachedPlayer();
    }

    public String get_last_name(String ip)
    {
        return get_cached_player(format_ip(ip)).name;
    }

    public void put(String ip, CachedPlayer player)
    {
        if (this.json.get_config().getAsJsonObject("cache") == null)
            reset();
        var json = new JsonObject();
        json.addProperty("name", Base64.getEncoder().encodeToString(player.name.getBytes(StandardCharsets.UTF_8)));
        json.addProperty("favicon", Base64.getEncoder().encodeToString(player.favicon.getBytes(StandardCharsets.UTF_8)));

        this.json.set("cache." + format_ip(ip), json);
    }

    public void reset()
    {
        json.set("cache", new JsonObject());
    }

    private String format_ip(String ip)
    {
        ip = ip.replace('.', '_');
        if (ip.contains(":"))
            ip = ip.split(":")[0];
        return ip;
    }

    public static class CachedPlayer
    {
        String name    = "";
        String favicon = "";

        CachedPlayer()
        {
        }

        CachedPlayer(String name, String favicon)
        {
            this.name = name;
            this.favicon = favicon;
        }
    }
}
