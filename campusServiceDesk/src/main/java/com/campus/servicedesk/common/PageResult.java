package com.campus.servicedesk.common;

import lombok.Data;
import java.util.List;

@Data
public class PageResult<T> {
    private long total;      // 总记录数
    private List<T> records; // 当前页的数据

    public PageResult(long total, List<T> records) {
        this.total = total;
        this.records = records;
    }
}