/*
 * The MIT License
 *
 * Copyright 2013 Jason Unger <entityreborn@gmail.com>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.entityreborn.socpuppet.config;

import com.entityreborn.config.ConfigurationSection;
import com.entityreborn.config.YamlConfig;
import com.entityreborn.config.exceptions.NoSuchSection;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Jason Unger <entityreborn@gmail.com>
 */
public class BotConfig {
    YamlConfig config;
    Map<String, Connection> connectionCache = new HashMap<>();

    public BotConfig() {
        config = new YamlConfig("config.yml");
    }
    
    public void load() throws Exception {
        config.load();
        InputStream stream = getClass().getResourceAsStream("/defaults.yml");
        YamlConfig defaults = new YamlConfig(stream);
        defaults.load();
        config.setDefaults(defaults);
    }
    
    public List<String> getConnectionNames() {
        return config.getStringList("connections");
    }
    
    public Connection getConnection(String name) {
        if (!connectionCache.containsKey(name.toLowerCase())) {
            ConfigurationSection sect;

            try {
                sect = config.getSection("connections").getSection(name);
            } catch (NoSuchSection s) {
                return null;
            }

            Connection conn = new Connection(sect);

            connectionCache.put(name.toLowerCase(), conn);
        }
        
        return connectionCache.get(name.toLowerCase());
    }
}
