package com.abw12.absolutefitness.productcatelog.controller;

import com.abw12.absolutefitness.productcatelog.dto.ProductInventoryDTO;
import com.abw12.absolutefitness.productcatelog.service.ProductInventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/catelog/inventory")
public class ProductInventoryController {

    private static Logger logger = LoggerFactory.getLogger(ProductInventoryController.class);

    @Autowired
    private ProductInventoryService inventoryService;

    @GetMapping("/getVariantInventory/{variantId}")
    public ResponseEntity<?> getVariantInventoryData(@PathVariable Long variantId){
        logger.info("Inside getVariantInventoryData rest call: {} " , variantId);
        try{
            return new ResponseEntity<>(inventoryService.getVariantById(variantId), HttpStatus.OK);

        }catch (Exception e){
            logger.error("Exception while fetching variant inventory data with variantId : {}", e.getMessage());
            return new ResponseEntity<>("Exception while fetching variant inventory data with variantId",HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/updateVariantInventory")
    public ResponseEntity<?> updateVariantInventory(@RequestBody ProductInventoryDTO inventoryDTO){
        logger.info("Inside updateVariantInventory rest call: {} " , inventoryDTO);
        try{
            return new ResponseEntity<>(inventoryService.updateVariantInventoryData(inventoryDTO), HttpStatus.OK);

        }catch (Exception e){
            logger.error("Exception while Inserting/updating variant inventory data with variantId : {}", e.getMessage());
            return new ResponseEntity<>("Exception while Inserting/updating variant inventory data with variantId",HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}