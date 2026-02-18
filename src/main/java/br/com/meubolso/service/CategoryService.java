package br.com.meubolso.service;

import br.com.meubolso.domain.Category;
import br.com.meubolso.dto.CategoryCreateRequest;
import br.com.meubolso.dto.CategoryResponse;
import br.com.meubolso.repository.CategoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public CategoryResponse create(UUID userId, CategoryCreateRequest request) {
        Category category = new Category();
        category.setUserId(userId);
        category.setName(request.getName());
        category.setType(normalizeType(request.getType()));
        category.setColor(request.getColor());

        Category saved = categoryRepository.save(category);
        return toResponse(saved);
    }

    public List<CategoryResponse> findAllByUser(UUID userId) {
        return categoryRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public CategoryResponse findById(UUID userId, UUID categoryId) {
        Category category = findOwnedCategory(userId, categoryId);
        return toResponse(category);
    }

    public CategoryResponse update(UUID userId, UUID categoryId, CategoryCreateRequest request) {
        Category category = findOwnedCategory(userId, categoryId);

        category.setName(request.getName());
        category.setType(normalizeType(request.getType()));
        category.setColor(request.getColor());

        Category saved = categoryRepository.save(category);
        return toResponse(saved);
    }

    public void delete(UUID userId, UUID categoryId) {
        Category category = findOwnedCategory(userId, categoryId);
        categoryRepository.delete(category);
    }

    private Category findOwnedCategory(UUID userId, UUID categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoria não encontrada"));

        if (!category.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");
        }

        return category;
    }

    private String normalizeType(String type) {
        String normalized = type == null ? null : type.trim().toUpperCase();
        if (!"INCOME".equals(normalized) && !"EXPENSE".equals(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de categoria inválido");
        }
        return normalized;
    }

    private CategoryResponse toResponse(Category category) {
        CategoryResponse response = new CategoryResponse();
        response.setId(category.getId());
        response.setUserId(category.getUserId());
        response.setName(category.getName());
        response.setType(category.getType());
        response.setColor(category.getColor());
        response.setCreatedAt(category.getCreatedAt());
        response.setUpdatedAt(category.getUpdatedAt());
        return response;
    }
}
