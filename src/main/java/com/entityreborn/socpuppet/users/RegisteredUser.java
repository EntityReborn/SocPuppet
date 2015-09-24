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

import com.entityreborn.socpuppet.extensions.annotations.Permission;
import com.entityreborn.socpuppet.extensions.annotations.Permission.DefaultTo;
import com.entityreborn.socpuppet.util.Password;
import com.entityreborn.socpuppet.util.StringSetType;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.misc.BaseDaoEnabled;
import com.j256.ormlite.table.DatabaseTable;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Jason Unger <entityreborn@gmail.com>
 */
@DatabaseTable(tableName = "users")
public class RegisteredUser extends BaseDaoEnabled {
    @DatabaseField(generatedId = true)
    private long id;
    @DatabaseField(unique = true)
    private String username;
    @DatabaseField
    private String password;
    @DatabaseField
    private String email;
    @DatabaseField(dataType = DataType.DATE_LONG)
    private Date regDate;
    @DatabaseField(defaultValue = "", persisterClass = StringSetType.class)
    private final HashSet<String> perms;
    @DatabaseField(defaultValue = "", persisterClass = StringSetType.class)
    private final HashSet<String> autoAuthMasks;
    
    public RegisteredUser() {
        perms = new HashSet<>();
        autoAuthMasks = new HashSet<>();
    }

    public RegisteredUser(String username, String password, String email) throws Exception {
        setUsername(username);
        setPassword(password);
        setEmail(email);
        
        regDate = new Date();
        perms = new HashSet<>();
        autoAuthMasks = new HashSet<>();
    }
    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username the username to set
     */
    public final void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return the password's hash
     */
    public String getPasswordHash() {
        return password;
    }

    /**
     * @param password the password to set
     * @throws java.lang.Exception
     */
    public final void setPassword(String password) throws Exception {
        this.password = Password.getSaltedHash(password);
    }

    /**
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * @param email the email to set
     */
    public final void setEmail(String email) {
        this.email = email;
    }

    /**
     * @return the regDate
     */
    public Date getRegDate() {
        return regDate;
    }
    
    /**
     * Checks if user has a permission node. Will traverse down nodes,
     * in the case of using wildcard permissions.
     * @param perm
     * @return True if assigned, False if denied, and False if not assigned.
     */
    public boolean hasPerm(String perm) {
        return hasPerm(perm, false);
    }
    
    /**
     * Checks if user has a permission node. Will traverse down nodes,
     * in the case of using wildcard permissions.
     * @param perm
     * @return True if assigned, False if denied, and the perm's default if not assigned.
     */
    public boolean hasPerm(Permission perm) {
        return hasPerm(perm.node(), perm.defaultTo() == DefaultTo.ALLOW);
    }
    
    public boolean hasExactPerm(String perm) {
        return perms.contains(perm.toLowerCase());
    }
    
    public String getRelativePerm(String perm) {
        perm = perm.toLowerCase();
        
        if (perms.contains("*")) {
            return "*";
        }
        
        if (perms.contains(perm)) {
            return perm;
        }
        
        if (perms.contains("-" + perm)) {
            return "-" + perm;
        }
        
        int index = perm.lastIndexOf(".");
        
        while (index != -1) {
            perm = perm.substring(0, index);
            
            if (perms.contains(perm + ".*")) {
                return perm + ".*";
            }
            
            if (perms.contains("-" + perm + ".*")) {
                return "-" + perm + ".*";
            }
            
            index = perm.lastIndexOf(".");
        }
        
        return null;
    }
    
    /**
     * Checks if user has a permission node. Will traverse down nodes,
     * in the case of using wildcard permissions.
     * @param perm
     * @param def
     * @return True if assigned, False if denied, and def if not assigned.
     */
    public Boolean hasPerm(String perm, Boolean def) {
        perm = perm.toLowerCase();
        
        if (perms.contains("*")) {
            return true;
        }
        
        if (perms.contains(perm)) {
            return true;
        }
        
        if (perms.contains("-" + perm)) {
            return true;
        }
        
        int index = perm.lastIndexOf(".");
        
        while (index != -1) {
            perm = perm.substring(0, index);
            
            if (perms.contains(perm + ".*")) {
                return true;
            }
            
            if (perms.contains("-" + perm + ".*")) {
                return false;
            }
            
            index = perm.lastIndexOf(".");
        }
        
        return def;
    }
    
    public void addPerm(String perm) {
        addPerm(perm, false);
    }
    
    public void addPerm(String perm, boolean negate) {
        if (negate) {
            perm = "-" + perm;
        }
        
        perms.add(perm.toLowerCase());
    }

    /**
     * Removes a perm, regardless of if it's negated.
     * @param perm
     */
    public void removePerm(String perm) {
        perms.remove(perm);
        perms.remove("-" + perm);
    }
    
    /**
     * @return the permissions
     */
    public Set<String> getPerms() {
        return Collections.unmodifiableSet(perms);
    }
    
    public Set<String> getAuthMasks() {
        return Collections.unmodifiableSet(autoAuthMasks);
    }
    
    public void addAuthMask(String mask) {
        if (!autoAuthMasks.contains(mask)) {
            autoAuthMasks.add(mask);
        }
    }
    
    public void removeAuthMask(String mask) {
        if (autoAuthMasks.contains(mask)) {
            autoAuthMasks.remove(mask);
        }
    }
}
