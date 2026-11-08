package kodlamaio.northwind.api.controllers;

import jakarta.validation.Valid;
import kodlamaio.northwind.business.abstracts.ProductService;
import kodlamaio.northwind.core.utilities.results.DataResult;
import kodlamaio.northwind.entities.dtos.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductsController {
    private final ProductService productService;
    public ProductsController(ProductService productService) { this.productService = productService; }

    @GetMapping
    public DataResult<PageResponse<ProductResponse>> getPage(
        @RequestParam(defaultValue="1") int pageNo, @RequestParam(defaultValue="10") int pageSize) {
        return productService.getPage(pageNo, pageSize);
    }

    @GetMapping("/{id}")
    public DataResult<ProductResponse> getById(@PathVariable int id) { return productService.getById(id); }

    @GetMapping("/getall")
    public DataResult<List<ProductResponse>> getAll() { return productService.getAll(); }

    @GetMapping("/getallsorted")
    public DataResult<List<ProductResponse>> getAllSorted() { return productService.getAllSorted(); }

    @GetMapping("/getallbypage")
    public DataResult<List<ProductResponse>> getAllByPage(
        @RequestParam(defaultValue="1") int pageNo, @RequestParam(defaultValue="10") int pageSize) {
        return productService.getAll(pageNo, pageSize);
    }

    @PostMapping({"", "/add"})
    public ResponseEntity<DataResult<ProductResponse>> add(@Valid @RequestBody CreateProductRequest request) {
        var result = productService.add(request);
        return ResponseEntity.created(URI.create("/api/products/" + result.getData().id())).body(result);
    }

    @GetMapping("/getByProductName")
    public DataResult<ProductResponse> getByProductName(@RequestParam String productName) {
        return productService.getByProductName(productName);
    }

    @GetMapping("/getByProductNameAndCategory_CategoryId")
    public DataResult<ProductResponse> getByProductNameAndCategory_CategoryId(
        @RequestParam String productName, @RequestParam int categoryId) {
        return productService.getByProductNameAndCategory_CategoryId(productName, categoryId);
    }

    @GetMapping("/getByProductNameOrCategory_CategoryId")
    public DataResult<List<ProductResponse>> getByProductNameOrCategory_CategoryId(
        @RequestParam String productName, @RequestParam int categoryId) {
        return productService.getByProductNameOrCategory_CategoryId(productName, categoryId);
    }

    @GetMapping("/getByCategory_CategoryIdIn")
    public DataResult<List<ProductResponse>> getByCategory_CategoryIdIn(@RequestParam List<Integer> categories) {
        return productService.getByCategory_CategoryIdIn(categories);
    }

    @GetMapping("/getByProductNameContains")
    public DataResult<List<ProductResponse>> getByProductNameContains(@RequestParam String productName) {
        return productService.getByProductNameContains(productName);
    }

    @GetMapping("/getByProductNameStartsWith")
    public DataResult<List<ProductResponse>> getByProductNameStartsWith(@RequestParam String productName) {
        return productService.getByProductNameStartsWith(productName);
    }

    @GetMapping("/getProductWithCategoryDetails")
    public DataResult<List<ProductWithCategoryDto>> getProductWithCategoryDetails() {
        return productService.getProductWithCategoryDetails();
    }
}
