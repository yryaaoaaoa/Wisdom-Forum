package com.yry.blog.myblogcommon.result;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@NoArgsConstructor
@Data
public class PaginationResponse<T> {
    private List<T> records;
    private long total;
    private int current;
    private int size;
    private int pages;

    public PaginationResponse(IPage<T> page) {
        this.records = page.getRecords();
        this.total = page.getTotal();
        this.current = (int) page.getCurrent();
        this.size = (int) page.getSize();
        this.pages = (int) page.getPages();
    }
}
