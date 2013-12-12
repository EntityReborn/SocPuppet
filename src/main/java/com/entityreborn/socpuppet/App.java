/*
 * The MIT License
 *
 * Copyright 2012 Jason Unger <entityreborn@gmail.com>.
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
package com.entityreborn.socpuppet;

import com.entityreborn.socbot.SocBot;
import com.entityreborn.socbot.eventsystem.EventManager;
import com.entityreborn.socpuppet.config.BotConfig;
import com.entityreborn.socpuppet.config.Connection;
import com.entityreborn.socpuppet.extensions.ExtensionManager;
import com.laytonsmith.PureUtilities.ClassLoading.ClassDiscovery;
import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 *
 * @author Jason Unger <entityreborn@gmail.com>
 */
public class App {
    static String VERSION = "";
    
    static {
        Package p = App.class.getPackage();

        if (p == null) {
            p = Package.getPackage("com.entityreborn.socpuppet");
        }

        if (p == null) {
            VERSION = "(unknown)";
        } else {
            String v = p.getImplementationVersion();

            if (v == null) {
                VERSION = "(unknown)";
            } else {
                VERSION = v;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        BotConfig c = new BotConfig();
        c.load();
        
        URL thisurl = ClassDiscovery.GetClassContainer(App.class);
        ClassDiscovery.getDefaultInstance().addDiscoveryLocation(thisurl);
        //ClassDiscovery.getDefaultInstance().addDiscoveryLocation(new File("../SocBotGeneral/target/SocBotFactoids-0.0.0-SNAPSHOT.jar").toURI().toURL());
        ExtensionManager.AddDiscoveryLocation(new File("../SocBotGeneral/target/SocBotFactoids-0.0.0-SNAPSHOT.jar"));
        ExtensionManager.Initialize(ClassDiscovery.getDefaultInstance());
        ExtensionManager.Startup();
        
        for (String connname : c.getConnectionNames()) {
            Connection conn = c.getConnection(connname);
            
            if (!conn.isActive()) {
                continue;
            }
            
            final SocBot bot = new SocBot(connname);
            EventManager.registerEvents(new BuiltinListener(c), bot);
            
            String nickname = conn.getNickname();
            bot.setNickname(nickname);
            
            final String server = conn.getServer();
            final int port = conn.getPort();
            final String password = conn.getPassword();
            
            Thread t = new Thread() {
                @Override
                public void run() {
                    boolean success = false;
                    
                    while (!success) {
                        try {
                            bot.connect(server, port, password);
                            success = true;
                        } catch (IOException ex) {
                            System.out.println("Retrying connection. " + ex.getLocalizedMessage());
                        }
                    }
                }
            };
            
            t.setName(connname + "-connector");
            t.start();
        }
    }
}
