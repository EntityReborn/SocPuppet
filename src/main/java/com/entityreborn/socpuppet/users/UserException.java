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

/**
 *
 * @author Jason Unger <entityreborn@gmail.com>
 */
public class UserException extends Exception {

    String id;

    public String getID() {
        return id;
    }

    public UserException(String id) {
        this.id = id;
    }

    public static class UserRegistrationException extends UserException {

        private final Throwable exception;

        public UserRegistrationException(String id, Throwable ex) {
            super(id);
            exception = ex;
        }

        public Throwable getException() {
            return exception;
        }
    }

    public static class UserExists extends UserException {

        public UserExists(String id) {
            super(id);
        }
    }

    public static class UnknownUser extends UserException {

        public UnknownUser(String id) {
            super(id);
        }
    }
    
    public static class IncorrectPassword extends UserException {

        public IncorrectPassword(String id) {
            super(id);
        }
    }
}
