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
package com.entityreborn.socpuppet;

import com.entityreborn.socbot.Colors;
import com.entityreborn.socbot.Numerics.Numeric;
import com.entityreborn.socbot.SocBot;
import com.entityreborn.socbot.Styles;
import com.entityreborn.socbot.events.CTCPEvent;
import com.entityreborn.socbot.events.CTCPReplyEvent;
import com.entityreborn.socbot.events.ConnectedEvent;
import com.entityreborn.socbot.events.ConnectingEvent;
import com.entityreborn.socbot.events.JoinEvent;
import com.entityreborn.socbot.events.LineSendEvent;
import com.entityreborn.socbot.events.ModeChangeEvent;
import com.entityreborn.socbot.events.NoticeEvent;
import com.entityreborn.socbot.events.NumericEvent;
import com.entityreborn.socbot.events.PacketReceivedEvent;
import com.entityreborn.socbot.events.PartEvent;
import com.entityreborn.socbot.events.PrivmsgEvent;
import com.entityreborn.socbot.events.QuitEvent;
import com.entityreborn.socbot.events.WelcomeEvent;
import com.entityreborn.socbot.eventsystem.EventHandler;
import com.entityreborn.socbot.eventsystem.Listener;
import com.entityreborn.socpuppet.config.BotConfig;
import com.entityreborn.socpuppet.config.ChannelConfig;
import com.entityreborn.socpuppet.config.ConnectionConfig;
import com.entityreborn.socpuppet.extensions.AbstractTrigger;
import com.entityreborn.socpuppet.extensions.ExtensionManager;
import com.entityreborn.socpuppet.extensions.ExtensionTracker;
import com.entityreborn.socpuppet.extensions.annotations.Permission;
import com.entityreborn.socpuppet.extensions.annotations.Permission.DefaultTo;
import com.entityreborn.socpuppet.users.RegisteredUser;
import com.entityreborn.socpuppet.users.SocPuppetUser;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Jason Unger <entityreborn@gmail.com>
 */
public class BuiltinListener implements Listener {
    BotConfig config;

    public BuiltinListener(BotConfig config) {
        this.config = config;
    }
    
    public void debug(SocBot bot, String message) {
        System.out.println("[" + bot.getID() + "] " + 
                Styles.removeAll(Colors.removeAll(message)));
    }

    @EventHandler
    public void handleConnecting(ConnectingEvent event) {
        debug(event.getBot(), "Connecting to " + event.getServer() + (event.getPort() != 6667 ? event.getPort() : ""));
    }

    @EventHandler
    public void handleConnected(ConnectedEvent event) {
        debug(event.getBot(), "Connected");
    }

    @EventHandler
    public void handleWelcome(WelcomeEvent event) {
        debug(event.getBot(), event.getServerName() + " welcomed us to the server.");
        ConnectionConfig conn = config.getConnection(event.getBot().getID());
        
        for (String channame : conn.getChannelNames()) {
            ChannelConfig chan = conn.getChannel(channame);
            
            if (chan.getAutoJoin()) {
                event.getBot().join(channame, chan.getPassword());
            }
        }
    }

    @EventHandler
    public void handleJoin(JoinEvent event) {
        debug(event.getBot(), event.getUser().getName() + " joined "
                + event.getChannel());
    }

    @EventHandler
    public void handlePart(PartEvent event) {
        debug(event.getBot(), event.getUser().getName() + " left "
                + event.getChannel() + " (" + event.getPartMessage() + ")");
    }

    @EventHandler
    public void handleQuit(QuitEvent event) {
        debug(event.getBot(), event.getUser().getName() + " quit ("
                + event.getQuitMessage() + ")");
    }
    
    private final Pattern regex = Pattern.compile("^\\^([^\\ ]+)(\\s+?(.*))?", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.MULTILINE | Pattern.COMMENTS);
    @EventHandler
    public void handleMsg(PrivmsgEvent event) {
	Matcher regexMatcher = regex.matcher(event.getMessage());
        
        if (regexMatcher.matches()) {
            String trigger = regexMatcher.group(1);
            String args = regexMatcher.group(3);
            
            for (ExtensionTracker tracker : ExtensionManager.Get().getTrackers().values()) {
                if (tracker.getTriggers().keySet().contains(trigger)) {
                    AbstractTrigger trig = tracker.getTriggers().get(trigger);
                    System.out.println("Called " + trig.plugin() + ":" + trig.name());
                    
                    SocPuppetUser user = (SocPuppetUser)event.getUser();
                    Permission perm = trig.permission();
                    
                    if (perm != null) {
                        RegisteredUser regUser = user.getRegistration();
                        
                        if (regUser == null) {
                            event.getTarget().sendMsg("You aren't logged in! This"
                                    + " command requires the '" + perm.node()
                                    + "' permission.");
                            
                            event.setCancelled(true);
                            
                            return;
                        }
                        
                        boolean hasPerm = user.getRegistration().hasPerm(
                                perm.node(), perm.defaultTo() == DefaultTo.ALLOW);
                        
                        if (!hasPerm) {
                            event.getTarget().sendMsg("I'm sorry, you don't have"
                                    + " permission to run this command! This"
                                    + " command requires the '" + perm.node()
                                    + "' permission.");
                            
                            event.setCancelled(true);
                            
                            return;
                        }
                    }
                    
                    String response = trig.exec(event, trigger, args);
                    
                    if (response != null && !response.trim().isEmpty()) {
                        event.getTarget().sendMsg(response);
                    }
                    
                    break;
                }
            }
        }
    }

    @EventHandler
    public void handleNotice(NoticeEvent event) {
    }

    @EventHandler
    public void handleCTCP(CTCPEvent event) {
        if (event.getType().equals("PING")) {
            event.getSender().sendCTCPReply("PING", event.getMessage());
        }
    }

    @EventHandler
    public void handleCTCPReply(CTCPReplyEvent event) {
    }

    @EventHandler
    public void handleLineIn(PacketReceivedEvent event) {
        //debug(event.getBot(), event.getPacket().getOriginalLine());
    }
    
    @EventHandler
    public void handleNumeric(NumericEvent event) {
        if (event.getNumeric() == Numeric.ERR_NICKNAMEINUSE) {
            String newnick = event.getPacket().getArgs().get(0);
            event.getBot().setNickname(newnick + "_");
        }
    }

    @EventHandler
    public void handleLineOut(LineSendEvent event) {
    }

    @EventHandler
    public void handleModeChange(ModeChangeEvent event) {
    }
}
