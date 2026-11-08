package kodlamaio.northwind.business.concretes;

import kodlamaio.northwind.business.abstracts.ProductService;
import kodlamaio.northwind.core.exceptions.ResourceNotFoundException;
import kodlamaio.northwind.core.utilities.results.*;
import kodlamaio.northwind.dataAccess.abstracts.*;
import kodlamaio.northwind.entities.concretes.Product;
import kodlamaio.northwind.entities.dtos.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import java.util.List;

@Service
@Validated
@Transactional(readOnly = true)
public class ProductManager implements ProductService {
    private final ProductDao productDao;
    private final CategoryDao categoryDao;

    public ProductManager(ProductDao productDao, CategoryDao categoryDao) {
        this.productDao = productDao;
        this.categoryDao = categoryDao;
    }

    private DataResult<List<ProductResponse>> products(List<Product> products) {
        return new SuccessDataResult<>(products.stream().map(ProductResponse::from).toList(), "Ürünler listelendi");
    }

    private DataResult<ProductResponse> product(Product product) {
        if (product == null) throw new ResourceNotFoundException("Ürün bulunamadı");
        return new SuccessDataResult<>(ProductResponse.from(product), "Ürün bulundu");
    }

    private Pageable pageable(int pageNo, int pageSize) {
        if (pageNo < 1 || pageSize < 1 || pageSize > 100)
            throw new IllegalArgumentException("pageNo en az 1, pageSize 1 ile 100 arasında olmalıdır");
        return PageRequest.of(pageNo - 1, pageSize, Sort.by("id"));
    }

    private String searchText(String text) {
        if (text == null || text.isBlank() || text.length() > 100)
            throw new IllegalArgumentException("Arama metni boş olamaz ve en fazla 100 karakter olabilir");
        return text.strip();
    }

    @Override public DataResult<List<ProductResponse>> getAll() {
        return products(productDao.findAll(Sort.by("id")));
    }

    @Override public DataResult<List<ProductResponse>> getAllSorted() {
        return products(productDao.findAll(Sort.by(Sort.Direction.DESC, "productName").and(Sort.by("id"))));
    }

    @Override public DataResult<List<ProductResponse>> getAll(int pageNo, int pageSize) {
        return products(productDao.findAll(pageable(pageNo, pageSize)).getContent());
    }

    @Override public DataResult<PageResponse<ProductResponse>> getPage(int pageNo, int pageSize) {
        var page = productDao.findAll(pageable(pageNo, pageSize)).map(ProductResponse::from);
        return new SuccessDataResult<>(new PageResponse<>(page.getContent(), pageNo, pageSize,
            page.getTotalElements(), page.getTotalPages(), page.isLast()), "Ürünler listelendi");
    }

    @Override public DataResult<ProductResponse> getById(int id) {
        if (id < 1) throw new IllegalArgumentException("Ürün kimliği pozitif olmalıdır");
        return product(productDao.findById(id).orElseThrow(() -> new ResourceNotFoundException("Ürün bulunamadı")));
    }

    @Override @Transactional
    public DataResult<ProductResponse> add(CreateProductRequest request) {
        var category = categoryDao.findById(request.categoryId())
            .orElseThrow(() -> new ResourceNotFoundException("Kategori bulunamadı"));
        Product product = new Product();
        product.setProductName(request.productName().strip());
        product.setUnitPrice(request.unitPrice());
        product.setUnitsInStock(request.unitsInStock().shortValue());
        product.setQuantityPerUnit(request.quantityPerUnit());
        product.setCategory(category);
        return new SuccessDataResult<>(ProductResponse.from(productDao.saveAndFlush(product)), "Ürün eklendi");
    }

    @Override public DataResult<ProductResponse> getByProductName(String name) {
        return product(productDao.getByProductName(searchText(name)));
    }

    @Override public DataResult<ProductResponse> getByProductNameAndCategory_CategoryId(String name, int categoryId) {
        if (categoryId < 1) throw new IllegalArgumentException("Kategori kimliği pozitif olmalıdır");
        return product(productDao.getByProductNameAndCategory_CategoryId(searchText(name), categoryId));
    }

    @Override public DataResult<List<ProductResponse>> getByProductNameOrCategory_CategoryId(String name, int categoryId) {
        if (categoryId < 1) throw new IllegalArgumentException("Kategori kimliği pozitif olmalıdır");
        return products(productDao.getByProductNameOrCategory_CategoryId(searchText(name), categoryId));
    }

    @Override public DataResult<List<ProductResponse>> getByCategory_CategoryIdIn(List<Integer> categories) {
        if (categories == null || categories.isEmpty() || categories.size() > 100 ||
            categories.stream().anyMatch(id -> id == null || id < 1))
            throw new IllegalArgumentException("1 ile 100 arasında pozitif kategori kimliği gönderilmelidir");
        return products(productDao.getByCategory_CategoryIdIn(categories));
    }

    @Override public DataResult<List<ProductResponse>> getByProductNameContains(String name) {
        return products(productDao.getByProductNameContains(searchText(name)));
    }

    @Override public DataResult<List<ProductResponse>> getByProductNameStartsWith(String name) {
        return products(productDao.getByProductNameStartsWith(searchText(name)));
    }

    @Override public DataResult<List<ProductWithCategoryDto>> getProductWithCategoryDetails() {
        return new SuccessDataResult<>(productDao.getProductWithCategoryDetails(), "Ürünler listelendi");
    }
}
