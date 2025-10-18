package com.example.miniodemo.util;

import com.github.f4b6a3.ulid.UlidCreator;

public class Ids {
    public static long newId() {
        // 取 ULID 的前 8 字节转 long（演示用；生产可以直接用字符串主键）
        var ulid = UlidCreator.getUlid().toBytes();
        long v = 0;
        for (int i = 0; i < 8; i++) v = (v << 8) | (ulid[i] & 0xff);
        return v & 0x7fffffffffffffffL; // 保证正数
    }
}