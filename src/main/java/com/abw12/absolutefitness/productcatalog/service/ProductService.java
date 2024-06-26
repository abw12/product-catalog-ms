package com.abw12.absolutefitness.productcatalog.service;

import com.abw12.absolutefitness.productcatalog.advice.InvalidDataRequestException;
import com.abw12.absolutefitness.productcatalog.dto.*;
import com.abw12.absolutefitness.productcatalog.entity.ProductCategoryDAO;
import com.abw12.absolutefitness.productcatalog.entity.ProductDAO;
import com.abw12.absolutefitness.productcatalog.entity.ProductVariantDAO;
import com.abw12.absolutefitness.productcatalog.entity.VariantImagesDAO;
import com.abw12.absolutefitness.productcatalog.helper.Utils;
import com.abw12.absolutefitness.productcatalog.mappers.ProductCategoryMapper;
import com.abw12.absolutefitness.productcatalog.mappers.ProductMapper;
import com.abw12.absolutefitness.productcatalog.mappers.ProductVariantMapper;
import com.abw12.absolutefitness.productcatalog.persistence.ProductPersistenceLayer;
import com.abw12.absolutefitness.productcatalog.repository.ImageTableRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductService {

    @Autowired
    private ProductPersistenceLayer persistenceLayer;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private ProductVariantMapper productVariantMapper;
    @Autowired
    private ProductCategoryMapper productCategoryMapper;
    @Autowired
    private ProductInventoryService inventoryService;
    @Autowired
    private ImageTableRepository imageTableRepository;

    @Autowired
    private Utils utils;

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    @Transactional(readOnly = true)
    public Page<ProductDTO> ListProduct(PageRequest pageRequest){
        logger.info("Listing all the product and their variant data");
        Page<ProductDAO> products = persistenceLayer.listProductData(pageRequest);
        //map over each product and get its variant/category data
        Page<ProductDTO> response = products.map(productDAO -> {
            ProductDTO productDTO = productMapper.entityToDto(productDAO);
            //fetch product category data
            ProductCategoryDAO categoryData = persistenceLayer.getCategoryById(productDTO.getCategoryId());
            ProductCategoryDTO productCategoryDTO = productCategoryMapper.entityToDto(categoryData);

            //fetch product variant list
            List<ProductVariantDAO> variants = persistenceLayer.getVariantsByProductId(productDTO.getProductId());
            List<ProductVariantDTO> variantsDtoList = variants.stream().map(this::fetchVariantDetails).toList();

            productDTO.setProductVariants(variantsDtoList);
            productDTO.setProductCategory(productCategoryDTO);
            return productDTO;
        });
        logger.info("List Product Response :: totalElements : {} | totalPages : {} | numberOfElements: {} ",
                response.getTotalElements(),response.getTotalPages(),response.getNumberOfElements());
        return response;
    }

    @Transactional(readOnly = true)
    public ProductDTO getProductById(String productId) {
        logger.info("Fetching data for productId: {}" , productId);
        ProductDAO productEntity = persistenceLayer.getProductById(productId);
        String categoryId = productEntity.getCategoryId();
        List<ProductVariantDAO> variantsList = persistenceLayer.getVariantsByProductId(productId);
        logger.info("Fetching category data with categoryId: {}",categoryId);
        ProductCategoryDAO category = persistenceLayer.getCategoryById(categoryId);


        ProductDTO productDTO = productMapper.entityToDto(productEntity);
        logger.info("Fetched data for productId: {} => {}" , productId,productDTO);
        List<ProductVariantDTO> pVariantDTOList = variantsList.stream()
                .map(this::fetchVariantDetails)
                .toList();
        logger.debug("Fetched variants data for productId: {} => {}",productId,pVariantDTOList);
        ProductCategoryDTO pCategory = productCategoryMapper.entityToDto(category);
        logger.debug("Fetched category data with categoryId: {} => {}",categoryId,pCategory);
        productDTO.setProductVariants(pVariantDTOList);
        productDTO.setProductCategory(pCategory);
        logger.info("Fetched product data with productId:{} =>{}",productId,productDTO);
        return  productDTO;
    }
    @Transactional(readOnly = true)
    public List<ProductDTO> getProductByName(String productName) {

        logger.info("Fetching product data with productName: {}",productName);
        List<ProductDAO> productEntityList = persistenceLayer.getProductByName(productName);
        logger.info("Fetched product data with productName: {} => {}",productName,productEntityList);

        return productEntityList.stream()
                .map(product -> {
                    String productId = product.getProductId();
                    String categoryId = product.getCategoryId();
                    logger.info("Fetching variants data with productId: {}",productId);
                    List<ProductVariantDAO> variantsList = persistenceLayer.getVariantsByProductId(productId);
                    logger.info("Fetched variants data with productId: {} => {}",productId,variantsList);

                    logger.info("Fetching category data for productId: {} with categoryId: {}",productId,categoryId);
                    ProductCategoryDAO category = persistenceLayer.getCategoryById(categoryId);
                    logger.info("Fetched category data for productId: {} with categoryId: {} => {}",productId,categoryId,category);
                    ProductDTO productDTO = productMapper.entityToDto(product);

                    List<ProductVariantDTO> pVariantDTOList = variantsList.stream()
                            .map(this::fetchVariantDetails)
                            .toList();
                    ProductCategoryDTO pCategory = productCategoryMapper.entityToDto(category);
                    productDTO.setProductVariants(pVariantDTOList);
                    productDTO.setProductCategory(pCategory);
                    return productDTO;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> getProductsByCategoryId(String categoryId) {
        logger.info("Fetching product data with categoryId: {}",categoryId);
        return persistenceLayer.getProductByCategoryId(categoryId).stream()
                .map(productDAO -> {
                    ProductDTO product = productMapper.entityToDto(productDAO);
                    logger.info("Fetched product with categoryId: {} => {}",categoryId,productDAO);
                    String  productId = productDAO.getProductId();
                    logger.info("Fetching variant data with productId: {}",productId);
                    List<ProductVariantDTO> variantList =  persistenceLayer.getVariantsByProductId(productId).stream()
                            .map(this::fetchVariantDetails)
                            .toList();
                    logger.info("Fetched variants data with productId: {} => {}",productId,variantList);

                    logger.info("Fetching category data for productId: {} with categoryId: {}",productId,categoryId);
                    ProductCategoryDTO productCategoryDTO = productCategoryMapper.entityToDto(persistenceLayer.getCategoryById(productDAO.getCategoryId()));
                    logger.info("Fetched category data for productId: {} with categoryId: {} => {}",productId,categoryId,productCategoryDTO);
                    product.setProductCategory(productCategoryDTO);
                    product.setProductVariants(variantList);
                    return product;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductVariantDTO getVariantDataByVariantId(String variantId){
        logger.info("Fetching variant data with variantId :  {}",variantId);
        ProductVariantDAO variantDTO = persistenceLayer.getProductVariantDataById(variantId);
        logger.info("Fetched variant data with variantId : {} => {}",variantId,variantDTO);
        return fetchVariantDetails(variantDTO);
    }
    @Transactional
    public ProductDTO insertProduct(ProductDTO productDTO){

        logger.info("Inserting new product data...");
        ProductDAO productEntity = productMapper.DtoToEntity(productDTO);
        ProductCategoryDAO productCategory = productCategoryMapper.DtoToEntity(productDTO.getProductCategory());
        //check if there is any existing category or not
        ProductCategoryDTO productCategoryDTO = checkIfCategoryExist(productCategory.getCategoryName(), productCategory);
        productEntity.setCategoryId(productCategoryDTO.getProductCategoryId()); //to maintain foreign key in product table for categoryId

        ProductDTO storedProductResponse = productMapper.entityToDto(persistenceLayer.upsertProductData(productEntity));
        String productId = storedProductResponse.getProductId();
        List<ProductVariantDTO> productVariantsList = productDTO.getProductVariants().stream()
                .map(variantDTO -> {
                    ProductVariantDAO tempVariantDao = productVariantMapper.DtoToEntity(variantDTO);
                    tempVariantDao.setProductId(productId);

                    CalcOfferResponse calcOfferResponse = null;
                    if(variantDTO.getOffer()!=null){
                        //invoke offer-mgmt client to calculate the discounted onSalePrice for the variant based on given offerId
                        CalcOfferRequest request = utils.prepareCalcOfferRequest(productCategoryDTO,productDTO,variantDTO);
                         calcOfferResponse = utils.calcOfferApi(request, variantDTO.getVariantName());
                        //set the offerId & onSalePrice only if any offer is applied and discount is provide else keep the onSalePrice = null
                        if(calcOfferResponse!=null && calcOfferResponse.getStatusCode().equalsIgnoreCase(HttpStatus.OK.getReasonPhrase())){
                            tempVariantDao.setOfferId(variantDTO.getOffer().getOfferId());
                            tempVariantDao.setOnSalePrice(calcOfferResponse.getOnSalePrice());
                        }
                    }else {
                        logger.info("Variant does not have any offer data not calling offer-mgmt-ms API to calculate discount ( onSalePrice ) :: variantName={} , variantValue={}"
                                , variantDTO.getVariantName(),variantDTO.getVariantValue());
                    }

                    //store variant info in db
                    ProductVariantDAO storedVariantData = persistenceLayer.upsertVariant(tempVariantDao);
                    ProductVariantDTO response = productVariantMapper.entityToDto(storedVariantData);
                    logger.info("variant data with productId:{} stored in db : {}" ,productId,response);

                    //map the variantId to the offerId calling offer-mgmt-ms api(only if the discount was calculated for the variant)
                    if(calcOfferResponse!=null && calcOfferResponse.getStatusCode().equalsIgnoreCase(HttpStatus.OK.getReasonPhrase()))
                        utils.mapVariantIdToOffer(response.getVariantId(),variantDTO.getOffer().getOfferId());

                    //taking variantId after storing the variant in db
                    variantDTO.getInventoryData().setVariantId(response.getVariantId());
                    //save inventory data for each variant in the list
                    ProductInventoryDTO storedInventoryData = inventoryService.updateVariantInventoryData(variantDTO.getInventoryData());
                    logger.info("inventory data stored with productId: {} => {}",productId,storedInventoryData);
                    response.setInventoryData(storedInventoryData);

                    return response;
                })
                .toList();

        storedProductResponse.setProductVariants(productVariantsList);
        storedProductResponse.setProductCategory(productCategoryDTO);
        logger.info("Product data inserted: {}",storedProductResponse);
        return storedProductResponse;
    }

    public ProductCategoryDTO checkIfCategoryExist(String categoryName,ProductCategoryDAO productCategory){
        Optional<ProductCategoryDAO> categoryObj = persistenceLayer.getCategoryByName(categoryName);
        if(categoryObj.isPresent()){
            logger.info("product category already exist with categoryId: {}",categoryObj.get().getProductCategoryId());
            return productCategoryMapper.entityToDto(categoryObj.get());
        }else{
            ProductCategoryDTO newCategoryData = productCategoryMapper.entityToDto(persistenceLayer.upsertCategoryData(productCategory));
            logger.info("created new category with categoryId: {}",newCategoryData.getProductCategoryId());
            return newCategoryData;
        }
    }

    @Transactional
    public ProductDTO updateProduct(ProductDTO productDTO){
        if(productDTO.getProductId() == null)
            throw new RuntimeException("productId cannot be NULL");

        logger.info("updating product data with productId: {} => {}",productDTO.getProductId(),productDTO);
        String productId = productDTO.getProductId();
        ProductDAO productEntity = productMapper.DtoToEntity(productDTO);
        if(productDTO.getProductCategory().getProductCategoryId() == null)
            throw new RuntimeException("product category categoryId cannot be NULL");
        logger.info("updating product category data with productId:{}",productId);
        ProductCategoryDAO productCategoryEntity = productCategoryMapper.DtoToEntity(productDTO.getProductCategory());

        ProductDTO productResponse =productMapper.entityToDto(persistenceLayer.upsertProductData(productEntity));
        logger.info("updated product data: {}",productResponse);
        ProductCategoryDTO productCategoryResponse = productCategoryMapper.entityToDto(persistenceLayer.upsertCategoryData(productCategoryEntity));
        logger.info("updated category data with productId:{} => {}",productId,productCategoryResponse);
        List<ProductVariantDTO> productVariantsResponse = productDTO.getProductVariants().stream()
                .map(variantDTO -> {
                    ProductVariantDAO tempVariantDao = productVariantMapper.DtoToEntity(variantDTO);
                    tempVariantDao.setProductId(productId);
                    //store variant info in db
                    ProductVariantDAO storedVariantData = persistenceLayer.upsertVariant(tempVariantDao);
                    ProductVariantDTO response = productVariantMapper.entityToDto(storedVariantData);
                    logger.info("variant data updated with productId:{} => {}",productId,response);
                    //taking variantId after storing the variant in db
                    variantDTO.getInventoryData().setVariantId(response.getVariantId());
                    //save inventory data for each variant in the list
                    ProductInventoryDTO storedInventoryData = inventoryService.updateVariantInventoryData(variantDTO.getInventoryData());
                    logger.info("Inventory data updated for variant:{} => {}",response.getVariantId(),storedInventoryData);
                    response.setInventoryData(storedInventoryData);
                    return response;
                })
                .toList();
        productResponse.setProductVariants(productVariantsResponse);
        productResponse.setProductCategory(productCategoryResponse);
        logger.info("completed updating product data with productId:{}",productId);
        return productResponse;
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> filterProduct(ProductFiltersDTO filtersDTO){
        if(filtersDTO.getCategoryId() == null)
            throw new InvalidDataRequestException("categoryId cannot be NULL");
        //product list for current category
        List<ProductDTO> productList = persistenceLayer.getProductsByFilters(filtersDTO).stream()
                .map(productDAO -> productMapper.entityToDto(productDAO))
                .toList();

        return productList.stream()
                .peek(productDTO -> {
                    List<ProductVariantDTO> productVariants = persistenceLayer.getVariantsByFilters(productDTO.getProductId(), filtersDTO).stream()
                            .map(this::fetchVariantDetails)
                            .toList();
                    productDTO.setProductVariants(productVariants);
                    productDTO.setProductCategory(productCategoryMapper.entityToDto(persistenceLayer.getCategoryById(filtersDTO.getCategoryId())));
                })
                .filter(productDTO -> !productDTO.getProductVariants().isEmpty())
                .toList();
    }

    @Transactional
    public List<ProductVariantDTO> upsertVariantByProductId(String productId , List<ProductVariantDTO> variantsList){
        if(productId == null)
            throw new InvalidDataRequestException("ProductId cannot be null for a variant");

        logger.info("insert/update variants data with productId:{} ",productId);
        return variantsList.stream()
                .map(variantDto ->{
                    ProductVariantDAO variantDAO = productVariantMapper.DtoToEntity(variantDto);
                    variantDAO.setProductId(productId);
                    //store variant info in db
                    ProductVariantDAO storedVariantData = persistenceLayer.upsertVariant(variantDAO);
                    logger.info("stored variant data with productId:{} => {} ",productId,storedVariantData);
                    ProductVariantDTO response = productVariantMapper.entityToDto(storedVariantData);
                    //if variant is new and don't have variantId for it, so we are taking variantId from response in that case
                    variantDto.getInventoryData().setVariantId(response.getVariantId());
                    //save inventory data for each variant in the list
                    ProductInventoryDTO storedInventoryData = inventoryService.updateVariantInventoryData(variantDto.getInventoryData());
                    logger.info("stored inventory data with variantId:{} => {}",response.getVariantId(),storedInventoryData);
                    response.setInventoryData(storedInventoryData);
                    return response;
                })
                .peek(storedVariantsList -> logger.info("completed : variants list stored"))
                .toList();
    }
    @Transactional
    public DeleteOperationResponse deleteProductByProductId(String productId){
        logger.info("Starting the cascade delete process for the product with productId={} ",productId);
        //retrieve all variants for the product
        List<ProductVariantDAO> variantList = persistenceLayer.getVariantsByProductId(productId);
        List<String> variantIdList = variantList.stream().map(ProductVariantDAO::getVariantId).toList();
        //delete inventory data for variants
        Integer inventoryDelCount = inventoryService.deleteInventoryData(variantIdList);
        //then delete the variant record from the variant table
        logger.info("Deleting all variants having productId={}",productId);
        Integer variantDelCount = persistenceLayer.deleteVariantsByProductId(productId);
        logger.info("Deleted total {} variants having productId={}",variantDelCount,productId);
        //finally delete the product itself
        logger.info("delete the product itself with productId={}",productId);
        Integer productDelCount = persistenceLayer.deleteProduct(productId);
        logger.info("Deleted total {} product having productId={}",productDelCount,productId);
        return new DeleteOperationResponse(HttpStatus.OK.getReasonPhrase(), String .format("Successfully Deleted the Product with productId=%s",productId));
    }

    @Transactional
    public DeleteOperationResponse deleteVariantByVariantId(List<String> variantIdList){
        logger.info("Starting the cascade delete process for all the variants requested :: {}",variantIdList);
        //first delete inventory data
        inventoryService.deleteInventoryData(variantIdList);
        //then delete the variant itself
        Integer variantDelCount = persistenceLayer.deleteVariantsInVariantIdList(variantIdList);
        logger.info("Deleted total {} variants",variantDelCount);
        return new DeleteOperationResponse(HttpStatus.OK.getReasonPhrase(), "Successfully Deleted all the variants.");
    }

    private ProductVariantDTO fetchVariantDetails(ProductVariantDAO productVariantDAO) {
        String variantId = productVariantDAO.getVariantId();
        String offerId = productVariantDAO.getOfferId();

        ProductVariantDTO variantDTO = productVariantMapper.entityToDto(productVariantDAO);
        //If offerId is present for any variant then retrieve offer details by calling offer-mgmt-ms
        if(!StringUtils.isEmpty(offerId)){
            OfferDTO offerData = utils.fetchOfferDetails(offerId);
            variantDTO.setOffer(offerData);
        }

        //fetch inventory data for the current variant
        ProductInventoryDTO inventoryDTO = inventoryService.getVariantById(variantId);
        Long totalQuantity = inventoryDTO.isReserved()
                ? (inventoryDTO.getQuantity() - inventoryDTO.getReservedQuantity())
                : inventoryDTO.getQuantity();
        inventoryDTO.setQuantity(totalQuantity);
        variantDTO.setInventoryData(inventoryDTO);
        //fetch image urls for the current variant
        List<String> variantImageUrlList = imageTableRepository.getImageDataByVariantId(variantDTO.getVariantId())
                .orElseThrow(() -> new InvalidDataRequestException(String.format("Cannot find Image MetaData for variant by variantId=%s", variantId)))
                .stream()
                .map(VariantImagesDAO::getImageUrl)
                .toList();
        variantDTO.setImageUrlList(variantImageUrlList);
        return variantDTO;
    }
}
