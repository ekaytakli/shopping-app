package kodlamaio.northwind.business.concretes;

import java.util.List;
import kodlamaio.northwind.business.abstracts.CategoryService;
import kodlamaio.northwind.core.utilities.results.*;
import kodlamaio.northwind.dataAccess.abstracts.CategoryDao;
import kodlamaio.northwind.entities.dtos.CategoryResponse;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CategoryManager implements CategoryService {
    private final CategoryDao categoryDao;

    public CategoryManager(CategoryDao categoryDao) { this.categoryDao = categoryDao; }

    @Override
    public DataResult<List<CategoryResponse>> getAll() {
        var categories = categoryDao.findAll(Sort.by("categoryId")).stream()
            .map(c -> new CategoryResponse(c.getCategoryId(), c.getCategoryName())).toList();
        return new SuccessDataResult<>(categories, "Kategoriler listelendi");
    }
}
