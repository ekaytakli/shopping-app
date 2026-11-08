package kodlamaio.northwind.entities.dtos;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonProperty;
public record CreateUserRequest(
    @NotBlank @Email @Size(max=254) String email,
    @JsonProperty(access=JsonProperty.Access.WRITE_ONLY) @NotBlank @Size(min=8, max=64) String password) {
    @Override public String toString() { return "CreateUserRequest[password=REDACTED]"; }
}