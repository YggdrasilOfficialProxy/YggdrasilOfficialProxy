/*
 * Copyright (c) 2018-2020 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2020/02/27 04:54:03
 *
 * YggdrasilOfficialProxy/YggdrasilOfficialProxy/RAFInputStream.java
 */

package cn.mcres.karlatemp.yop.config;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

public class RAFInputStream extends InputStream {
    private final RandomAccessFile raf;

    public RAFInputStream(RandomAccessFile raf) {
        this.raf = raf;
    }

    @Override
    public int read() throws IOException {
        return raf.read();
    }

    @Override
    public int read(@NotNull byte[] b, int off, int len) throws IOException {
        return raf.read(b, off, len);
    }

    @Override
    public int read(@NotNull byte[] b) throws IOException {
        return raf.read(b);
    }

    @Override
    public void close() throws IOException {
        raf.close();
    }
}
