package kodlamaio.northwind.entities.dtos;
// Constructor order matches the JPQL projection in ProductDao.
public record ProductWithCategoryDto(int id, String productName, String categoryName) {}