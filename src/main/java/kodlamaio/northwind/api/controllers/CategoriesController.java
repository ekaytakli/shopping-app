package kodlamaio.northwind.api.controllers;

import java.util.List;
import kodlamaio.northwind.business.abstracts.CategoryService;
import kodlamaio.northwind.core.utilities.results.DataResult;
import kodlamaio.northwind.entities.dtos.CategoryResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/categories")
public class CategoriesController {
    private final CategoryService categoryService;

    public CategoriesController(CategoryService categoryService) { this.categoryService = categoryService; }

    @GetMapping
    public DataResult<List<CategoryResponse>> getAll() { return categoryService.getAll(); }
}
