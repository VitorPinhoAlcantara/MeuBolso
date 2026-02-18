package br.com.meubolso.dto;

import br.com.meubolso.domain.enums.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CategoryCreateRequest {
    
    @NotBlank
    @Size(max = 100)
    private String name;

    @NotNull
    private CategoryType type;

    @Size(max = 20)
    private String color;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public CategoryType getType() { return type; }
    public void setType(CategoryType type) { this.type = type; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
}
