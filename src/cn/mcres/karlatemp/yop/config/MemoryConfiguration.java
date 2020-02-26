/*
 * Copyright (c) 2018-2020 Karlatemp. All rights reserved.
 * Reserved.FileName: MemoryConfiguration.java@author: karlatemp@vip.qq.com: 2020/1/25 下午8:35@version: 2.0
 */

package cn.mcres.karlatemp.yop.config;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.Map;

public abstract class MemoryConfiguration extends MemoryConfigurationSection implements Configuration {
    protected String header;

    public MemoryConfiguration() {
    }

    protected MemoryConfiguration(Void us) {
        super(us);
    }

    protected MemoryConfiguration(@NotNull Map<String, Object> values) {
        super(values);
    }

    @Override
    public void load(InputStream stream) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(stream)) {
            load(reader);
        }
    }

    @Override
    public void store(OutputStream stream) throws IOException {
        try (OutputStreamWriter writer = new OutputStreamWriter(stream)) {
            store(writer);
        }
    }

    @Override
    public String header() {
        return header;
    }

    @Override
    protected abstract cn.mcres.karlatemp.yop.config.MemoryConfiguration newInstance();

    @Override
    public @NotNull cn.mcres.karlatemp.yop.config.MemoryConfiguration newContext() {
        return (cn.mcres.karlatemp.yop.config.MemoryConfiguration) super.newContext();
    }

    @Override
    public @NotNull Configuration clone() {
        return (cn.mcres.karlatemp.yop.config.MemoryConfiguration) super.clone();
    }

    @Override
    public Configuration header(String header) {
        this.header = header;
        return this;
    }
}
