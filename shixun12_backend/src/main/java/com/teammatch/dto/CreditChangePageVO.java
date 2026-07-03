package com.teammatch.dto;

import java.util.List;

/**
 * 用户端信誉流水分页响应 VO。
 */
public class CreditChangePageVO {
    private List<CreditChangeVO> list;
    private long total;
    private int page;
    private int pageSize;

    public List<CreditChangeVO> getList() {
        return list;
    }

    public void setList(List<CreditChangeVO> list) {
        this.list = list;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}
