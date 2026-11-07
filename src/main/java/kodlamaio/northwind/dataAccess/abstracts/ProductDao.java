
package kodlamaio.northwind.dataAccess.abstracts;

import kodlamaio.northwind.entities.concretes.Product;
import kodlamaio.northwind.entities.dtos.ProductWithCategoryDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductDao extends JpaRepository<Product, Integer> {

    Product getByProductName(String productName);

    Product getByProductNameAndCategory_CategoryId(
            String productName, int categoryId
    );

    List<Product> getByProductNameOrCategory_CategoryId(
            String productName, int categoryId
    );

    List<Product> getByCategory_CategoryIdIn(
            List<Integer> categories
    );

    List<Product> getByProductNameContains(String productName);

    List<Product> getByProductNameStartsWith(String productName);

    @Query("SELECT p FROM Product p WHERE p.productName = :productName AND p.category.categoryId = :categoryId")
    List<Product> getByNameAndCategory(
            @Param("productName") String productName,
            @Param("categoryId") int categoryId
    );

    @Query("SELECT new kodlamaio.northwind.entities.dtos.ProductWithCategoryDto" +
            "(p.id, p.productName, c.categoryName) " +
            "FROM Category c INNER JOIN c.products p")
    List<ProductWithCategoryDto> getProductWithCategoryDetails();
}
