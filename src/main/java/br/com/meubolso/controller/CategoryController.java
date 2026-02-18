package br.com.meubolso.controller;

import br.com.meubolso.dto.CategoryCreateRequest;
import br.com.meubolso.dto.CategoryResponse;
import br.com.meubolso.security.AuthenticatedUser;
import br.com.meubolso.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                   @Valid @RequestBody CategoryCreateRequest request) {
        return categoryService.create(currentUser.userId(), request);
    }

    @GetMapping
    public List<CategoryResponse> findAll(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return categoryService.findAllByUser(currentUser.userId());
    }

    @GetMapping("/{id}")
    public CategoryResponse findById(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                     @PathVariable UUID id) {
        return categoryService.findById(currentUser.userId(), id);
    }

    @PutMapping("/{id}")
    public CategoryResponse update(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                   @PathVariable UUID id,
                                   @Valid @RequestBody CategoryCreateRequest request) {
        return categoryService.update(currentUser.userId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable UUID id) {
        categoryService.delete(currentUser.userId(), id);
    }
}
