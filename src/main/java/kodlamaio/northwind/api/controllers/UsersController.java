package kodlamaio.northwind.api.controllers;
import jakarta.validation.Valid;
import kodlamaio.northwind.business.abstracts.UserService;
import kodlamaio.northwind.core.utilities.results.DataResult;
import kodlamaio.northwind.entities.dtos.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/users")
public class UsersController {
    private final UserService userService;
    public UsersController(UserService userService) { this.userService = userService; }
    @PostMapping({"", "/add"})
    public ResponseEntity<DataResult<UserResponse>> add(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.add(request));
    }
}