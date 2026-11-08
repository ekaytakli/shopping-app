package kodlamaio.northwind.entities.concretes;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
@Entity
@Table(name="products")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class Product {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="product_id")
    private Integer id;
    @Column(name="product_name", nullable=false, length=40)
    private String productName;
    @Column(name="unit_price")
    private BigDecimal unitPrice;
    @Column(name="units_in_stock")
    private short unitsInStock;
    @Column(name="quantity_per_unit", length=20)
    private String quantityPerUnit;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="category_id")
    private Category category;
}