package com.geriatric.assessment.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 选项DTO。
 */
@Data
@Builder
public class OptionDTO {
    private String value;
    private String label;
}
