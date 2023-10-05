package com.abw12.absolutefitness.productcatelog.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductInventoryDTO {

    private String id;
    private String quantity;
    private Date createdDate;
    private Date modifiedDate;
}
