package kodlamaio.northwind.business.abstracts;

import java.util.List;
import kodlamaio.northwind.core.utilities.results.DataResult;
import kodlamaio.northwind.entities.dtos.CategoryResponse;

public interface CategoryService {
    DataResult<List<CategoryResponse>> getAll();
}
