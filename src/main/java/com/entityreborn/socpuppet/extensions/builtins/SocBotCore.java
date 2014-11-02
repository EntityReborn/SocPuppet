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
package com.entityreborn.socpuppet.extensions.builtins;

import com.entityreborn.socbot.events.PrivmsgEvent;
import com.entityreborn.socpuppet.console.ConsoleManager;
import com.entityreborn.socpuppet.extensions.AbstractExtension;
import com.entityreborn.socpuppet.extensions.AbstractTrigger;
import com.entityreborn.socpuppet.extensions.ConsoleCommand;
import com.entityreborn.socpuppet.extensions.annotations.Permission;
import com.entityreborn.socpuppet.extensions.annotations.SocBotPlugin;
import com.entityreborn.socpuppet.extensions.annotations.Trigger;
import com.entityreborn.socpuppet.util.Restart;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jason Unger <entityreborn@gmail.com>
 */
@SocBotPlugin("SocBotCore")
public class SocBotCore extends AbstractExtension {

    @Trigger(name = "ping", id = "core.general.ping")
    @Permission(node = "core.general.ping", defaultTo = Permission.DefaultTo.ALLOW)
    public static class ping extends AbstractTrigger {

        @Override
        public String exec(PrivmsgEvent event, String trigger, String args) {
            return "Ping!";
        }

        @Override
        public String docs() {
            return "ping - Pings the bot.";
        }
    }
    
    @Trigger(name = "ping", id = "core.general.ping")
    public static class console_ping extends ConsoleCommand {

        @Override
        public String exec(String trigger, String args) {
            return "Ping!";
        }

        @Override
        public String docs() {
            return "ping - Pings the bot.";
        }
    }
    
    @Trigger(name = "shutdown", id = "core.general.ping")
    public static class console_shutdown extends ConsoleCommand {

        @Override
        public String exec(String trigger, String args) {
            ConsoleManager.getInstance().stop();
            
            return "Shutting down.";
        }

        @Override
        public String docs() {
            return "shutdown - Shuts off the bot.";
        }
    }

    @Trigger(name = "pong", id = "core.general.pong")
    @Permission(node = "core.general.pong", defaultTo = Permission.DefaultTo.ALLOW)
    public static class pong extends AbstractTrigger {

        @Override
        public String exec(PrivmsgEvent event, String trigger, String args) {
            return "Pong!";
        }

        @Override
        public String docs() {
            return "pong - Pongs the bot.";
        }
    }

    @Trigger(name = "say", id = "core.general.say")
    @Permission(node = "core.general.say")
    public static class say extends AbstractTrigger {

        @Override
        public String exec(PrivmsgEvent event, String trigger, String args) {
            if (args != null && !args.trim().isEmpty()) {
                return args;
            }

            return null;
        }

        @Override
        public String docs() {
            return "say <something> - Tells the bot to say <something>.";
        }
    }
    
    @Trigger(name = "parseraw", id = "core.general.parseraw")
    @Permission(node = "core.general.parseraw")
    public static class parseraw extends AbstractTrigger {

        @Override
        public String exec(PrivmsgEvent event, String trigger, String args) {
            if (args != null && !args.trim().isEmpty()) {
                event.getBot().handleLine(args);
            }

            return null;
        }

        @Override
        public String docs() {
            return "say <something> - Tells the bot to say <something>.";
        }
    }

    @Trigger(name = "shutdown", id = "core.general.shutdown")
    @Permission(node = "core.general.shutdown")
    public static class shutdown extends AbstractTrigger {

        @Override
        public String exec(PrivmsgEvent event, String trigger, String args) {
            ConsoleManager.getInstance().stop();

            return null;
        }

        @Override
        public String docs() {
            return "say <something> - Tells the bot to say <something>.";
        }
    }

    @Trigger(name = "restart", id = "core.general.restart")
    @Permission(node = "core.general.restart")
    public static class restart extends AbstractTrigger {

        @Override
        public String exec(final PrivmsgEvent event, String trigger, String args) {
            ConsoleManager.getInstance().stop();

            Runnable restartThread = new Runnable() {
                @Override
                public void run() {
                    Runnable waitForDisconnect = new Runnable() {

                        @Override
                        public void run() {
                            try {
                                while (event.getBot().isConnected()) {
                                    Thread.sleep(50);
                                }
                            } catch (InterruptedException ex) {
                                Logger.getLogger(SocBotCore.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    };
                    
                    try {
                        Restart.restartApplication(waitForDisconnect);
                    } catch (IOException ex) {
                        Logger.getLogger(SocBotCore.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            };

            Thread shutdown = new Thread(restartThread);

            shutdown.setDaemon(true);
            shutdown.start();

            return null;
        }

        @Override
        public String docs() {
            return "say <something> - Tells the bot to say <something>.";
        }
    }
    
    @Trigger(name = "restart", id = "core.general.restart")
    @Permission(node = "core.general.restart")
    public static class console_restart extends ConsoleCommand {

        @Override
        public String exec(String trigger, String args) {
            ConsoleManager.getInstance().stop();

            Runnable restartThread = new Runnable() {
                @Override
                public void run() {
                    try {
                        Restart.restartApplication(new Runnable(){
                            @Override
                            public void run() {
                            }
                        });
                    } catch (IOException ex) {
                        Logger.getLogger(SocBotCore.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            };

            Thread shutdown = new Thread(restartThread);

            shutdown.setDaemon(true);
            shutdown.start();

            return null;
        }

        @Override
        public String docs() {
            return "say <something> - Tells the bot to say <something>.";
        }
    }
}
