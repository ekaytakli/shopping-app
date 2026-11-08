package kodlamaio.northwind.business.concretes;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import kodlamaio.northwind.business.abstracts.UserService;
import kodlamaio.northwind.core.dataAccess.UserDao;
import kodlamaio.northwind.core.entities.User;
import kodlamaio.northwind.core.exceptions.ConflictException;
import kodlamaio.northwind.core.utilities.results.*;
import kodlamaio.northwind.entities.dtos.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
@Service
@Validated
public class UserManager implements UserService {
    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;
    public UserManager(UserDao userDao, PasswordEncoder passwordEncoder) {
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
    }
    @Override @Transactional
    public DataResult<UserResponse> add(CreateUserRequest request) {
        String email = request.email().strip().toLowerCase(Locale.ROOT);
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72)
            throw new IllegalArgumentException("Şifre UTF-8 olarak en fazla 72 bayt olabilir");
        if (userDao.existsByEmailIgnoreCase(email))
            throw new ConflictException("Bu e-posta zaten kullanılıyor");
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        User saved = userDao.saveAndFlush(user);
        return new SuccessDataResult<>(new UserResponse(saved.getId(), saved.getEmail()), "Kullanıcı eklendi");
    }
}
