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
import com.entityreborn.socpuppet.extensions.AbstractExtension;
import com.entityreborn.socpuppet.extensions.AbstractTrigger;
import com.entityreborn.socpuppet.extensions.annotations.Permission;
import com.entityreborn.socpuppet.extensions.annotations.SocBotPlugin;
import com.entityreborn.socpuppet.extensions.annotations.Trigger;

/**
 *
 * @author Jason Unger <entityreborn@gmail.com>
 */
@SocBotPlugin("SocBotCore")
public class SocBotCore extends AbstractExtension {
    @Trigger("ping")
    @Permission(node="core.general.ping", defaultTo = Permission.DefaultTo.ALLOW)
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
    
    @Trigger("pong")
    @Permission(node="core.general.pong", defaultTo = Permission.DefaultTo.ALLOW)
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
    
    @Trigger("say")
    @Permission(node="core.general.say")
    public static class say extends AbstractTrigger {
        @Override
        public String exec(PrivmsgEvent event, String trigger, String args) {
            if (args != null && !args.trim().isEmpty())
                return args;
            
            return null;
        }

        @Override
        public String docs() {
            return "say <something> - Tells the bot to say <something>.";
        }
    }
}
