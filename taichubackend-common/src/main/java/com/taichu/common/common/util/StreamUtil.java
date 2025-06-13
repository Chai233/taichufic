package com.taichu.common.common.util;

import java.util.Collection;
import java.util.stream.Stream;


public class StreamUtil {

    /**
     * 将集合转换为流,支持集合为null的场景
     * @param collection 集合
     * @param <T> 集合元素类型
     * @return 流
     */
    public static <T> Stream<T> toStream(Collection<T> collection) {
        return collection == null ? Stream.empty() : collection.stream();
    }
}
