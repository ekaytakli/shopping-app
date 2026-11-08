package kodlamaio.northwind.core.entities;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Table(name="users", uniqueConstraints=@UniqueConstraint(name="uk_users_email", columnNames="email"))
public class User {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private int id;
    @Column(name="email", nullable=false, length=254)
    private String email;
    @JsonIgnore
    @Column(name="password", nullable=false)
    private String password;
}