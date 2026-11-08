package kodlamaio.northwind.entities.concretes;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;
@Entity
@Table(name="categories")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class Category {
    @Id @Column(name="category_id")
    private int categoryId;
    @Column(name="category_name")
    private String categoryName;
    @JsonIgnore @OneToMany(mappedBy="category")
    private List<Product> products;
}