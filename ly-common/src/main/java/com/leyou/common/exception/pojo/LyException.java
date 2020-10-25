package com.leyou.common.exception.pojo;

import lombok.Getter;

/**
 * 只有RuntimeException才会回滚
 */
@Getter
public class LyException extends RuntimeException {
    private Integer status;

    public LyException(Integer status, String message) {
        super(message);
        this.status = status;
    }

    public LyException(ExceptionEnum e) {
        super(e.getMessage());
        this.status = e.getStatus();
    }

}
