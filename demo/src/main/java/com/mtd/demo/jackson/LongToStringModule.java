package com.mtd.demo.jackson;

import tools.jackson.core.json.PackageVersion;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.io.Serial;

/**
 * Long类型序列化为字符串模块，用于解决JavaScript中Long精度丢失问题
 */
public class LongToStringModule extends SimpleModule {

    @Serial
    private static final long serialVersionUID = 1L;

    public LongToStringModule() {
        super(PackageVersion.VERSION);
        // Long类型序列化为字符串
        this.addSerializer(Long.class, ToStringSerializer.instance);
        this.addSerializer(Long.TYPE, ToStringSerializer.instance);
    }

}
