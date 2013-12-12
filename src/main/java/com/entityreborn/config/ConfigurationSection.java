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
package com.entityreborn.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.entityreborn.config.exceptions.NoSuchKey;
import com.entityreborn.config.exceptions.NoSuchSection;

/**
 *
 * @author Jason Unger <entityreborn@gmail.com>
 */
public class ConfigurationSection {
    final ConfigurationSection parentSection;
    final String sectionName;
    protected Map<String, Object> defaults = new HashMap<>();
    protected Map<String, Object> nodes = new HashMap<>();
    final Map<String, ConfigurationSection> sectionCache = new HashMap<>();

    public ConfigurationSection(String key, ConfigurationSection parent, Map<String, Object> nodes) {
        parentSection = parent;
        sectionName = key;

        if (nodes != null) {
            this.nodes = nodes;
        }
    }

    public String getKey() {
        return sectionName;
    }

    public void load() throws Exception {
        if (!isRoot()) {
            getRoot().load();
        }
    }

    public void save() throws Exception {
        if (!isRoot()) {
            getRoot().save();
        }
    }

    public boolean isRoot() {
        return parentSection == null;
    }

    public ConfigurationSection getRoot() {
        if (isRoot()) {
            return this;
        }

        return parentSection.getRoot();
    }

    public Object get(String key) throws NoSuchKey {
        Object retn = nodes.get(key);

        if (retn == null) {
            retn = getRoot().getDefault(key, this);
        }

        return retn;
    }

    @SuppressWarnings("unchecked")
    public ConfigurationSection getSection(String key) throws NoSuchSection {
        if (sectionCache.containsKey(key)) {
            return sectionCache.get(key);
        }

        Object obj;

        try {
            obj = get(key);
        } catch (NoSuchKey e) {
            throw new NoSuchSection(e.getKey());
        }

        ConfigurationSection sect = null;

        if (obj instanceof Map<?, ?>) {
            Map<String, Object> map = ((Map<String, Object>) obj);

            sect = new ConfigurationSection(key, this, map);

            if (defaults != null && defaults.containsKey(key)) {
                Object def = defaults.get(key);

                if (def instanceof Map<?, ?>) {
                    sect.setDefaults((Map<String, Object>) def);
                }
            }

            sectionCache.put(key, sect);
        }

        return sect;
    }

    public boolean isSet(String key) {
        try {
            // Gotta be a better way to do this.
            get(key);
        } catch (NoSuchKey e) {
            return false;
        }

        return true;
    }

    public String getString(String key) {
        Object obj;
        try {
            obj = get(key);
        } catch (NoSuchKey ex) {
            return null;
        }

        return String.valueOf(obj);
    }

    public Integer getInt(String key) {
        String str = getString(key);
        if (str == null) {
            return null;
        }

        return Integer.valueOf(str);
    }

    public Boolean getBoolean(String key) {
        String str = getString(key);
        if (str == null) {
            return null;
        }

        return Boolean.valueOf(str);
    }

    public List<?> getList(String key) {
        Object obj;
        try {
            obj = get(key);
        } catch (NoSuchKey ex) {
            return null;
        }

        if (obj instanceof List<?>) {
            return (List<?>) obj;
        } else if (obj instanceof Map) {
            List<String> retn = new ArrayList<>();

            for (Object o : ((Map<?, ?>) obj).keySet()) {
                retn.add(o.toString());
            }

            return retn;
        }

        return null;
    }

    public Set<String> getKeys() {
        return nodes.keySet();
    }

    public List<String> getStringList(String key) {
        List<?> lst = getList(key);
        if (lst == null) {
            return null;
        }

        List<String> retn = new ArrayList<>();

        for (Object item : lst) {
            retn.add(String.valueOf(item));
        }

        return retn;
    }

    public void set(String key, Object value) {
        if (value == null) {
            remove(key);
        } else {
            nodes.put(key, value);
        }
    }

    @SuppressWarnings("unchecked")
    public Object getDefault(String key, ConfigurationSection source) throws NoSuchKey {
        ArrayList<String> keys = new ArrayList<>();
        ConfigurationSection sect = source;

        while (!sect.isRoot()) {
            keys.add(0, sect.getKey());
            sect = sect.parentSection;
        }

        keys.add(key);

        Object retn = defaults;
        String k;

        while (!keys.isEmpty()) {
            k = keys.remove(0);

            if (retn instanceof Map) {
                Map<String, Object> map = (Map) retn;

                if (map.containsKey(key)) {
                    retn = map.get(key);
                } else if (map.containsKey(k)) {
                    retn = map.get(k);
                } else if (map.containsKey("*-" + k)) {
                    retn = map.get("*-" + k);
                } else if (map.containsKey("*")) {
                    retn = map.get("*");
                } else {
                    throw new NoSuchKey(key);
                }
            } else {
                if (!keys.isEmpty()) {
                    // We didn't find a complete path to what we were looking for.
                    // This doesn't exist in the defaults.
                    return null;
                }

                return retn;
            }
        }

        return retn;
    }

    public void setDefaults(Map<String, Object> value) {
        defaults = value;
    }

    public void setDefaults(ConfigurationSection sect) {
        defaults = sect.nodes;
    }

    public void setDefault(String key, Object value) {
        defaults.put(key, value);
    }

    public void remove(String key) {
        sectionCache.remove(key);
        nodes.remove(key);
    }
}
