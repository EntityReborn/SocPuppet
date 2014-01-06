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
package com.entityreborn.socpuppet.util;

import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.field.SqlType;
import com.j256.ormlite.field.types.BaseDataType;
import com.j256.ormlite.support.DatabaseResults;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;

/**
 *
 * @author Jason Unger <entityreborn@gmail.com>
 */
public class StringSetType extends BaseDataType {

    public static String TOKEN = "\r\n";
    public static int DEFAULT_WIDTH = 1024;

    private static final StringSetType singleTon = new StringSetType();

    public static StringSetType getSingleton() {
        return singleTon;
    }

    private StringSetType() {
        super(SqlType.STRING, new Class<?>[]{HashSet.class});
    }

    protected StringSetType(SqlType sqlType, Class<?>[] classes) {
        super(sqlType, classes);
    }

    @Override
    public Object sqlArgToJava(FieldType fieldType, Object sqlArg, int columnPos) throws SQLException {
        HashSet<String> retn = new HashSet<>();
        retn.addAll(Arrays.asList(sqlArg.toString().split(TOKEN)));

        return retn;
    }

    @Override
    public Object javaToSqlArg(FieldType fieldType, Object javaObject) throws SQLException {
        HashSet<String> set = (HashSet<String>) javaObject;
        String retn = "";

        for (String item : set) {
            if (!retn.isEmpty()) {
                retn += TOKEN;
            }

            retn += item;
        }

        return retn;
    }

    @Override
    public int getDefaultWidth() {
        return DEFAULT_WIDTH;
    }

    @Override
    public Object parseDefaultString(FieldType fieldType, String defaultStr) throws SQLException {
        return defaultStr;
    }

    @Override
    public Object resultToSqlArg(FieldType fieldType, DatabaseResults results, int columnPos) throws SQLException {
        String str = results.getString(columnPos);
        return str;
    }
}
