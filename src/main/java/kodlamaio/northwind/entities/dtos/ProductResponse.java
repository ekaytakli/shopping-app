package kodlamaio.northwind.entities.dtos;
import java.math.BigDecimal;
import kodlamaio.northwind.entities.concretes.Product;
public record ProductResponse(Integer id, String productName, BigDecimal unitPrice,
    short unitsInStock, String quantityPerUnit, CategorySummary category) {
    public record CategorySummary(int categoryId, String categoryName) {}
    public static ProductResponse from(Product p) {
        var c = p.getCategory();
        return new ProductResponse(p.getId(), p.getProductName(), p.getUnitPrice(),
            p.getUnitsInStock(), p.getQuantityPerUnit(),
            c == null ? null : new CategorySummary(c.getCategoryId(), c.getCategoryName()));
    }
}