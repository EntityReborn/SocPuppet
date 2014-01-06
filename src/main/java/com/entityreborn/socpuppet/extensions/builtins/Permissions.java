/*
 * The MIT License
 *
 * Copyright 2014 Jason Unger <entityreborn@gmail.com>.
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
import com.entityreborn.socpuppet.SocPuppet;
import com.entityreborn.socpuppet.extensions.AbstractTrigger;
import com.entityreborn.socpuppet.extensions.annotations.Trigger;
import com.entityreborn.socpuppet.users.RegisteredUser;
import com.entityreborn.socpuppet.users.SocPuppetUser;
import com.entityreborn.socpuppet.users.UserException;
import com.entityreborn.socpuppet.users.UserManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jason Unger <entityreborn@gmail.com>
 */
public class Permissions {
    @Trigger("login")
    public static class login extends AbstractTrigger {
        @Override
        public String exec(PrivmsgEvent event, String trigger, String args) {
            if (!(event.getUser() instanceof SocPuppetUser)) {
                return null;
            }
            
            SocPuppetUser user = (SocPuppetUser)event.getUser();
            
            String[] parts = args.split(" ", 2);
            
            try {
                if (parts.length == 1) {
                    String username = event.getSender().getName();
                    user.attemptLogin(username, parts[0]);
                } else {
                    user.attemptLogin(parts[0], parts[1]);
                }
                
                return "Successfully logged in.";
            } catch (UserException.IncorrectPassword | UserException.UnknownUser ex) {
                return "Incorrect password or unknown user.";
            } catch (Exception ex) {
                return "Error ocurred during login: " + ex.getMessage();
            }
        }

        @Override
        public String docs() {
            return "login [username] <password> - Login to the bot."
                    + " Username is optional, and defaults to your nick.";
        }
    }
    
    @Trigger("register")
    public static class register extends AbstractTrigger {

        @Override
        public String exec(PrivmsgEvent event, String trigger, String args) {
            if (!(event.getUser() instanceof SocPuppetUser)) {
                return null;
            }
            
            SocPuppetUser user = (SocPuppetUser)event.getUser();
            
            String[] parts = args.split(" ", 3); // username, email, password
            
            UserManager manager = UserManager.get((SocPuppet) user.getBot());
            try {
                manager.registerUser(parts[0], parts[2], parts[1]);
            } catch (UserException.UserExists ex) {
                return "That user already exists!";
            } catch (UserException.UserRegistrationException ex) {
                return "Error ocurred during registration: " + ex.getMessage();
            }
            
            return "Successfully registered! Use ^login to log in.";
        }

        @Override
        public String docs() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }
    
    @Trigger("hasperm")
    public static class hasperm extends AbstractTrigger {

        @Override
        public String exec(PrivmsgEvent event, String trigger, String args) {
            if (!(event.getUser() instanceof SocPuppetUser)) {
                return null;
            }
            String[] parts = args.split(" ");
            
            UserManager manager = UserManager.get((SocPuppet) event.getBot());
            
            RegisteredUser user;
            try {
                user = manager.getUser(parts[0]);
            } catch (UserException.UnknownUser ex) {
                return "Unknown user";
            }
            
            if (user.hasPerm(parts[1])) {
                return "Yep";
            }
            
            return "Nope";
        }

        @Override
        public String docs() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }
    
    @Trigger("addperm")
    public static class addperm extends AbstractTrigger {

        @Override
        public String exec(PrivmsgEvent event, String trigger, String args) {
            if (!(event.getUser() instanceof SocPuppetUser)) {
                return null;
            }
            String[] parts = args.split(" ");
            
            UserManager manager = UserManager.get((SocPuppet) event.getBot());
            
            RegisteredUser user;
            
            try {
                user = manager.getUser(parts[0]);
            } catch (UserException.UnknownUser ex) {
                return "Unknown user";
            }
            
            if (user.hasPerm(parts[1])) {
                return "User already has that perm!";
            }
            
            user.addPerm(parts[1]);
            
            try {
                user.update();
            } catch (SQLException ex) {
                Logger.getLogger(SocBotCore.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            return "Done";
        }

        @Override
        public String docs() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }
    
    @Trigger("remperm")
    public static class remperm extends AbstractTrigger {

        @Override
        public String exec(PrivmsgEvent event, String trigger, String args) {
            if (!(event.getUser() instanceof SocPuppetUser)) {
                return null;
            }
            String[] parts = args.split(" ");
            
            UserManager manager = UserManager.get((SocPuppet) event.getBot());
            
            RegisteredUser user;
            try {
                user = manager.getUser(parts[0]);
            } catch (UserException.UnknownUser ex) {
                return "Unknown user";
            }
            
            if (!user.hasPerm(parts[1])) {
                return "User doesn't have that perm!";
            }
            
            user.removePerm(parts[1]);
            
            try {
                user.update();
            } catch (SQLException ex) {
                Logger.getLogger(SocBotCore.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            return "Done";
        }

        @Override
        public String docs() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }
}
