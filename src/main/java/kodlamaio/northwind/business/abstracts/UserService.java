package kodlamaio.northwind.business.abstracts;
import kodlamaio.northwind.entities.dtos.*;
import kodlamaio.northwind.core.utilities.results.DataResult;
public interface UserService {
    DataResult<UserResponse> add(@jakarta.validation.Valid CreateUserRequest request);
}
