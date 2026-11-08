package kodlamaio.northwind.business.abstracts;

import kodlamaio.northwind.core.utilities.results.DataResult;
import kodlamaio.northwind.entities.dtos.*;
import java.util.List;

public interface ProductService {
    DataResult<List<ProductResponse>> getAll();
    DataResult<List<ProductResponse>> getAllSorted();
    DataResult<List<ProductResponse>> getAll(int pageNo, int pageSize);
    DataResult<PageResponse<ProductResponse>> getPage(int pageNo, int pageSize);
    DataResult<ProductResponse> getById(int id);
    DataResult<ProductResponse> add(@jakarta.validation.Valid CreateProductRequest request);
    DataResult<ProductResponse> getByProductName(String productName);
    DataResult<ProductResponse> getByProductNameAndCategory_CategoryId(String productName, int categoryId);
    DataResult<List<ProductResponse>> getByProductNameOrCategory_CategoryId(String productName, int categoryId);
    DataResult<List<ProductResponse>> getByCategory_CategoryIdIn(List<Integer> categories);
    DataResult<List<ProductResponse>> getByProductNameContains(String productName);
    DataResult<List<ProductResponse>> getByProductNameStartsWith(String productName);
    DataResult<List<ProductWithCategoryDto>> getProductWithCategoryDetails();
}
