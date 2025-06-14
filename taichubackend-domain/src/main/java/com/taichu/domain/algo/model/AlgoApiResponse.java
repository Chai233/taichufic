package com.taichu.domain.algo.model;

import lombok.Data;

@Data
public class AlgoApiResponse<T> {
    private int code;
    private String msg;
    private T data;
} 