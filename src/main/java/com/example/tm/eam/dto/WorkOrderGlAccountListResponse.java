package com.example.tm.eam.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkOrderGlAccountListResponse {

    private List<String> glAccounts;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;
}
