package com.atguigu.cache.service.base.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

/**
 * 分页查询的基本
 */

public class Paging {

    private int start;

    private int page;

    public Paging(int start, int page) {
        this.start = start;
        this.page = page;
    }

    public Paging() {
    }


    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }


    @Override
    public String toString() {
        return "Paging{" +
                "start=" + start +
                ", page=" + page +
                '}';
    }
}
