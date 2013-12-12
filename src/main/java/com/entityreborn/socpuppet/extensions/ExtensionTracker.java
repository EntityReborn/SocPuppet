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
package com.entityreborn.socpuppet.extensions;

import com.laytonsmith.PureUtilities.ClassLoading.ClassDiscovery;
import com.laytonsmith.PureUtilities.ClassLoading.DynamicClassLoader;
import com.laytonsmith.PureUtilities.Common.StackTraceUtils;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jason Unger <entityreborn@gmail.com>
 */
public class ExtensionTracker {
    private final URL location;
    private final ClassDiscovery cd;
    private final DynamicClassLoader dcl;
    private final Map<String, Extension> plugins = new HashMap<>();
    private final Map<String, AbstractTrigger> triggers = new HashMap<>();

    public ExtensionTracker(URL location, ClassDiscovery cd, DynamicClassLoader dcl) {
        this.location = location;
        this.cd = cd;
        this.dcl = dcl;
    }

    public URL getLocation() {
        return location;
    }

    public void addPlugin(Extension plugin) {
        plugins.put(plugin.getName().toLowerCase(), plugin);
    }

    public Map<String, Extension> getPlugins() {
        return Collections.unmodifiableMap(plugins);
    }

    public ClassDiscovery getClassDiscovery() {
        return cd;
    }

    public void startup() {
        for (Extension plugin : plugins.values()) {
            System.out.println("Loading " + plugin.getName());

            try {
                plugin.onPluginLoad();
            } catch (Throwable e) {
                Logger log = Logger.getLogger(ExtensionManager.class.getName());
                log.log(Level.SEVERE, plugin.getName() + "'s onStartup caused an exception:");
                log.log(Level.SEVERE, StackTraceUtils.GetStacktrace(e));
            }
        }
    }

    public void shutdown() {
        for (Extension plugin : plugins.values()) {
            try {
                plugin.onPluginUnload();
            } catch (Throwable e) {
                Logger log = Logger.getLogger(ExtensionManager.class.getName());
                log.log(Level.SEVERE, plugin.getName() + "'s onShutdown caused an exception:");
                log.log(Level.SEVERE, StackTraceUtils.GetStacktrace(e));
            }
        }
    }

    public void addTrigger(AbstractTrigger trig) {
        triggers.put(trig.name().toLowerCase(), trig);
    }

    public Map<String, AbstractTrigger> getTriggers() {
        return Collections.unmodifiableMap(triggers);
    }
}
