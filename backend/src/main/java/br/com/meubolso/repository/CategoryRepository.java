package br.com.meubolso.repository;

import br.com.meubolso.domain.Category;
import br.com.meubolso.domain.enums.CategoryType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    Page<Category> findByUserId(UUID userId, Pageable pageable);

    Page<Category> findByUserIdAndType(UUID userId, CategoryType type, Pageable pageable);
}
