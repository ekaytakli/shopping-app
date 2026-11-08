package kodlamaio.northwind.entities.dtos;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record CreateProductRequest(
    @NotBlank @Size(max=40) String productName,
    @NotNull @DecimalMin("0.0") @Digits(integer=10, fraction=2) BigDecimal unitPrice,
    @NotNull @Min(0) @Max(32767) Integer unitsInStock,
    @Size(max=20) String quantityPerUnit,
    @NotNull @Positive Integer categoryId) {}