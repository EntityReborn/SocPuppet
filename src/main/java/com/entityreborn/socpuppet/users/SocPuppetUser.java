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

package com.entityreborn.socpuppet.users;

import com.entityreborn.socpuppet.util.Password;
import com.entityreborn.socbot.SocBot;
import com.entityreborn.socbot.User;
import com.entityreborn.socbot.UserFactory;
import com.entityreborn.socpuppet.SocPuppet;
import com.entityreborn.socpuppet.users.UserException.IncorrectPassword;
import com.entityreborn.socpuppet.users.UserException.UnknownUser;
import com.entityreborn.socpuppet.users.UserManager;

/**
 *
 * @author Jason Unger <entityreborn@gmail.com>
 */
public class SocPuppetUser extends User {
    public static class Factory implements UserFactory {
        @Override
        public User createUser(String nick, SocBot owningBot) {
            return new SocPuppetUser(nick, owningBot);
        }
    }
    
    private String loginName = "";
    
    public boolean attemptLogin(String name, String password) throws IncorrectPassword, UnknownUser, Exception {
        UserManager manager = UserManager.get((SocPuppet) getBot());
        RegisteredUser user = manager.getUser(name);
        
        if (Password.check(password, user.getPasswordHash())) {
            loginName = name;
            return true;
        } else {
            return false;
        }
    }
    
    public SocPuppetUser(String userline, SocBot b) {
        super(userline, b);
    }
    
    public RegisteredUser getRegistration() {
        if ("".equals(loginName)) {
            return null;
        }
        
        return null;
    }

    public String getLoginName() {
        return loginName;
    }
}
