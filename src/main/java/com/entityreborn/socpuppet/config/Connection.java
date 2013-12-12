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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import com.entityreborn.config.ConfigurationSection;
import com.entityreborn.config.exceptions.NoSuchSection;

/**
 *
 * @author Jason Unger <entityreborn@gmail.com>
 */
public class Connection extends BaseConfig {
    final Map<String, Channel> channelMap;
    
    public Connection(ConfigurationSection sect) {
        super(sect);
        this.channelMap = new HashMap<>();
    }
    
    public String getConfigName() {
        return getConfigSection().getKey();
    }
    
    public String getNickname() {
        return getConfigSection().getString("nickname");
    }
    
    public void setNickname(String name) {
        getConfigSection().set("nickname", name);
    }
    
    public String getServer() {
        return getConfigSection().getString("server");
    }
    
    public String getUsername() {
        return getConfigSection().getString("username");
    }
    
    public String getRealname() {
        return getConfigSection().getString("realname");
    }
    
    public int getPort() {
        int port = getConfigSection().getInt("port");
        
        if (port > 0 && port < 65536) {
            return port;
        }
        
        return 6667;
    }
    
    public String getPassword() {
        return getConfigSection().getString("password");
    }
    
    public Set<String> getChannelNames() {
        ConfigurationSection chansect = null;
        
        try {
            chansect = getConfigSection().getSection("channels");
        } catch (NoSuchSection sect) {
            // Fine, lets continue.
        }
        
        if (chansect != null) {
            return chansect.getKeys();
        }
        
        return new HashSet<>();
    }
    
    public Channel getChannel(String name) {
        ConfigurationSection chansect;
        
        try {
            chansect = getConfigSection().getSection("channels");
        } catch (NoSuchSection sect) {
            return null;
        }
        
        if (chansect != null) {
            try {
                chansect = chansect.getSection(name);
            } catch (NoSuchSection ex) {
                return null;
            }
            
            Channel chan = new Channel(chansect);
            channelMap.put(name.toLowerCase(), chan);
            
            return chan;
        }
        
        return null;
    }

    public boolean isActive() {
        return getConfigSection().getBoolean("active");
    }
}
