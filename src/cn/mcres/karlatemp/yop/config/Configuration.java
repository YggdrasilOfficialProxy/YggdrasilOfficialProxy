/*
 * Copyright (c) 2018-2020 Karlatemp. All rights reserved.
 * Reserved.FileName: Configuration.java@author: karlatemp@vip.qq.com: 2020/1/25 下午7:33@version: 2.0
 */

package cn.mcres.karlatemp.yop.config;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public interface Configuration extends ConfigurationSection {
    /**
     * The header of this configuration. But Now NOT Implement this method.
     *
     * @return THe header of this configuration.
     */
    String header();

    /**
     * Set header of this configuration. But Now NOT Implement this method.
     *
     * @param header The header
     * @return this.
     */
    cn.mcres.karlatemp.yop.config.Configuration header(String header);

    /**
     * Load configuration from Path
     *
     * @param file The Path.
     * @throws IOException I/O Exception
     */
    default void load(Path file) throws IOException {
        try (InputStream stream = Files.newInputStream(file)) {
            load(stream);
        }
    }

    /**
     * Load configuration from file
     *
     * @param file The file
     * @throws IOException I/O Exception
     */
    default void load(File file) throws IOException {
        try (RAFInputStream stream = new RAFInputStream(new RandomAccessFile(file, "r"))) {
            load(stream);
        }
    }

    /**
     * Load configuration from a Reader.
     *
     * @param stream The reader.
     * @throws IOException I/O Exception
     */
    void load(Reader stream) throws IOException;

    /**
     * Load configuration from a stream
     *
     * @param stream The stream.
     * @throws IOException I/O Exception
     */
    void load(InputStream stream) throws IOException;

    /**
     * Write this configuration to steam
     *
     * @param stream The steam.
     * @throws IOException I/O Exception
     */
    void store(OutputStream stream) throws IOException;

    /**
     * Write this configuration to writer.
     *
     * @param writer The writer
     * @throws IOException I/O Exception
     */
    void store(Writer writer) throws IOException;

    /**
     * Save this configuration
     *
     * @param file The location
     * @throws IOException I/O Exception
     */
    default void store(File file) throws IOException {
        try (RAFOutputStream stream = new RAFOutputStream(new RandomAccessFile(file, "rw"))) {
            store(stream);
        }
    }

    /**
     * Save this configuration
     *
     * @param file The location
     * @throws IOException I/O Exception
     */
    default void store(Path file) throws IOException {
        try (OutputStream stream = Files.newOutputStream(file)) {
            store(stream);
        }
    }

    /**
     * Create a new reading/writing context of this configuration
     * <p>
     * Read and write operations are synchronized
     *
     * @return The new context.
     */
    @Override
    @NotNull cn.mcres.karlatemp.yop.config.Configuration newContext();

    /**
     * Copy this configuration.
     * @return Coped Configuration
     */
    @Override
    @NotNull
    default cn.mcres.karlatemp.yop.config.Configuration copy() {
        return clone();
    }

    @Override
    @NotNull cn.mcres.karlatemp.yop.config.Configuration clone();
}
