package com.teammatch.dto;

import java.io.Serializable;

/**
 * 申诉恢复命令 DTO
 * M5-7 申诉恢复 Service 入参：仅需已批准的 appealId
 */
public class AppealRestoreCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long appealId;

    public AppealRestoreCommand() {
    }

    public AppealRestoreCommand(Long appealId) {
        this.appealId = appealId;
    }

    public Long getAppealId() {
        return appealId;
    }

    public void setAppealId(Long appealId) {
        this.appealId = appealId;
    }
}
