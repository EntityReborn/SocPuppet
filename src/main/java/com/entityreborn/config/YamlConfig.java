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
package com.entityreborn.config;

import com.entityreborn.config.exceptions.ConfigException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 *
 * @author Jason Unger <entityreborn@gmail.com>
 */
public class YamlConfig extends ConfigurationSection {
    String filename;
    InputStream inStream;

    public YamlConfig(String filename) {
        super("", null, null);
        this.filename = filename;
        
        File file = new File(filename);
        
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            
            try {
                file.createNewFile();
            } catch (IOException ex) {
                throw new RuntimeException("Could not create file (" + filename + "!");
            }
        }
        
        try {
            inStream = new FileInputStream(file);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(YamlConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public YamlConfig(InputStream stream) {
        super("", null, null);
        inStream = stream;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void load() throws Exception {
        Yaml yml = new Yaml();

        nodes = (Map<String, Object>) yml.load(inStream);

        if (nodes == null) {
            nodes = new HashMap<>();
        }
    }
    
    public void saveTo(OutputStream stream) {
        OutputStreamWriter s = new OutputStreamWriter(stream);

        DumperOptions options = new DumperOptions();
        options.setWidth(50);
        options.setIndent(4);
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        Yaml yml = new Yaml(options);
        yml.dump(nodes, s);
    }
    
    @Override
    public void save() throws ConfigException {
        if (filename == null) {
            throw new ConfigException("No filename was defined to save to!");
        }
        
        File f = new File(filename);

        if (!f.exists()) {
            try {
                f.mkdirs();
                f.createNewFile();
            } catch (IOException ex) {
                Logger.getLogger(YamlConfig.class.getName()).log(Level.SEVERE, null, ex);
                return;
            }
        }
        try {
            saveTo(new FileOutputStream(f));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(YamlConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void main(String[] args) throws Exception {
        YamlConfig defaults = new YamlConfig("defaults.yml");
        defaults.load();
        YamlConfig config = new YamlConfig("config.yml");
        config.load();
        config.setDefaults(defaults.nodes);
        ConfigurationSection sect = config.getSection("connections").getSection("esper").getSection("channels").getSection("#testing");
        System.out.println(sect.getString("password"));
        System.out.println(sect.getBoolean("autojoin"));
        
        sect.set("password", "tests");
        System.out.println(sect.getString("password"));
        config.save();
    }
}
