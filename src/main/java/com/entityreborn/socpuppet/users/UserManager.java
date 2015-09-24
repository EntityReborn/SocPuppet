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

import com.entityreborn.socpuppet.SocPuppet;
import com.entityreborn.socpuppet.users.UserException.UnknownUser;
import com.entityreborn.socpuppet.users.UserException.UserExists;
import com.entityreborn.socpuppet.users.UserException.UserRegistrationException;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.table.TableUtils;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A user registration management class.
 * @author Jason Unger <entityreborn@gmail.com>
 */
public class UserManager {
    private static final Map<String, UserManager> managers = new HashMap<>();
    private JdbcConnectionSource source;
    private Dao<RegisteredUser, String> dao;
    private final SocPuppet bot;
    private String identifier = null;

    /**
     * Get a specific user registration manager for a given bot instance.
     * Passing in null will use the default global database.
     * @param bot
     * @return 
     */
    public static UserManager get(SocPuppet bot) {
        UserManager manager;
        String identifier;
        File configDir;
        
        if (bot != null) {
            identifier = bot.getConfig().getConfigName();
            configDir = bot.getConfig().getParentConfig().getDirectory("config");
        } else {
            identifier = "__global__";
            configDir = new File("conf");
        }
        
        if (!managers.containsKey(identifier.toLowerCase())) {
            try {
                
                manager = new UserManager(bot);
                manager.identifier = identifier;
                
                managers.put(identifier.toLowerCase(), manager);
                
                configDir.mkdirs();
                
                File dbFile = new File(configDir, identifier.toLowerCase() + "-users.db");
                String url = "jdbc:sqlite:" + dbFile.getPath();
                
                manager.source = new JdbcConnectionSource(url);
                manager.dao = DaoManager.createDao(manager.source, RegisteredUser.class);

                TableUtils.createTableIfNotExists(manager.source, RegisteredUser.class);
            } catch (SQLException ex) {
                Logger.getLogger(RegisteredUser.class.getName())
                        .log(Level.SEVERE, null, ex);
                return null;
            }
        }

        return managers.get(identifier.toLowerCase());
    }
    
    /**
     * Private, to ensure factory use.
     * @param bot 
     */
    private UserManager(SocPuppet bot) {
        this.bot = bot;
    }

    public SocPuppet getBot() {
        return bot;
    }

    public boolean has(String key) {
        try {
            return dao.countOf(dao.queryBuilder().setCountOf(true).where()
                    .like("username", key).prepare()) != 0;
        } catch (SQLException ex) {
            return false;
        }
    }

    public RegisteredUser getUser(String key) throws UnknownUser {
        // Return the global user if one exists.
        if (!identifier.equalsIgnoreCase("__global__") && UserManager.get(null).has(key)) {
            return UserManager.get(null).getUser(key);
        }
        
        if (!has(key)) {
            throw new UnknownUser(key);
        }

        RegisteredUser user = null;

        try {
            user = dao.queryForFirst(dao.queryBuilder().where()
                    .like("username", key).prepare());
        } catch (SQLException ex) {
            Logger.getLogger(UserManager.class.getName())
                    .log(Level.SEVERE, null, ex);
        }
        
        user.setDao(dao);
        return user;
    }

    public RegisteredUser registerUser(String username, String password, String email)
            throws UserExists, UserRegistrationException {
        if (has(username)) {
            throw new UserExists(username);
        }

        RegisteredUser user;
        try {
            user = new RegisteredUser(username, password, email);
            user.setDao(dao);
        } catch (Throwable ex) {
            throw new UserRegistrationException(username, ex);
        }

        try {
            dao.createOrUpdate(user);
        } catch (SQLException ex) {
            Logger.getLogger(UserManager.class.getName())
                    .log(Level.SEVERE, null, ex);
        }

        return user;
    }

    public RegisteredUser deleteUser(String key) throws UnknownUser {
        if (!has(key)) {
            throw new UnknownUser(key);
        }

        RegisteredUser user = null;

        try {
            user = getUser(key);

            dao.delete(user);
        } catch (SQLException ex) {
            Logger.getLogger(UserManager.class.getName())
                    .log(Level.SEVERE, null, ex);
        }

        return user;
    }

    public List<RegisteredUser> getAllUsers() {
        try {
            return dao.queryForAll();
        } catch (SQLException ex) {
            Logger.getLogger(UserManager.class.getName())
                    .log(Level.SEVERE, null, ex);
            return new ArrayList<>();
        }
    }
}
