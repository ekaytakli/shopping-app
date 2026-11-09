package kodlamaio.northwind.dataAccess.abstracts;

import kodlamaio.northwind.entities.concretes.Product;
import kodlamaio.northwind.entities.dtos.ProductWithCategoryDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductDao extends JpaRepository<Product, Integer> {

    // Fold dotted/dotless I after uppercasing so searches also work with Turkish locales.
    String SEARCH_NAME = "replace(upper(p.productName), 'İ', 'I')";
    String SEARCH_PARAMETER = "replace(upper(:productName), 'İ', 'I')";

    @Override
    @EntityGraph(attributePaths = "category")
    List<Product> findAll(Sort sort);

    @Override
    @EntityGraph(attributePaths = "category")
    Page<Product> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "category")
    Optional<Product> findById(Integer id);

    @EntityGraph(attributePaths = "category")
    @Query("SELECT p FROM Product p WHERE " + SEARCH_NAME + " = " + SEARCH_PARAMETER)
    Product getByProductName(@Param("productName") String productName);

    @EntityGraph(attributePaths = "category")
    @Query("SELECT p FROM Product p WHERE " + SEARCH_NAME + " = " + SEARCH_PARAMETER +
            " AND p.category.categoryId = :categoryId")
    Product getByProductNameAndCategory_CategoryId(
            @Param("productName") String productName, @Param("categoryId") int categoryId
    );

    @EntityGraph(attributePaths = "category")
    @Query("SELECT p FROM Product p WHERE " + SEARCH_NAME + " = " + SEARCH_PARAMETER +
            " OR p.category.categoryId = :categoryId")
    List<Product> getByProductNameOrCategory_CategoryId(
            @Param("productName") String productName, @Param("categoryId") int categoryId
    );

    @EntityGraph(attributePaths = "category")
    List<Product> getByCategory_CategoryIdIn(
            List<Integer> categories
    );

    @EntityGraph(attributePaths = "category")
    @Query("SELECT p FROM Product p WHERE locate(" + SEARCH_PARAMETER + ", " + SEARCH_NAME + ") > 0")
    List<Product> getByProductNameContains(@Param("productName") String productName);

    @EntityGraph(attributePaths = "category")
    @Query("SELECT p FROM Product p WHERE locate(" + SEARCH_PARAMETER + ", " + SEARCH_NAME + ") = 1")
    List<Product> getByProductNameStartsWith(@Param("productName") String productName);

    @Query("SELECT p FROM Product p WHERE " + SEARCH_NAME + " = " + SEARCH_PARAMETER +
            " AND p.category.categoryId = :categoryId")
    List<Product> getByNameAndCategory(
            @Param("productName") String productName,
            @Param("categoryId") int categoryId
    );

    @Query("SELECT new kodlamaio.northwind.entities.dtos.ProductWithCategoryDto" +
            "(p.id, p.productName, c.categoryName) " +
            "FROM Category c INNER JOIN c.products p")
    List<ProductWithCategoryDto> getProductWithCategoryDetails();
}
