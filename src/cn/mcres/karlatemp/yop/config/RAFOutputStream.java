/*
 * Copyright (c) 2018-2020 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2020/02/27 04:53:12
 *
 * YggdrasilOfficialProxy/YggdrasilOfficialProxy/RAFOutputStream.java
 */

package cn.mcres.karlatemp.yop.config;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

public class RAFOutputStream extends OutputStream {
    private final RandomAccessFile raf;

    public RAFOutputStream(RandomAccessFile raf) {
        this.raf = raf;
    }

    @Override
    public void write(@NotNull byte[] b) throws IOException {
        raf.write(b);
    }

    @Override
    public void write(int b) throws IOException {
        raf.write(b);
    }

    @Override
    public void write(@NotNull byte[] b, int off, int len) throws IOException {
        raf.write(b, off, len);
    }

    @Override
    public void close() throws IOException {
        raf.setLength(raf.getFilePointer());
        raf.close();
    }
}
