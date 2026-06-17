package com.example.facility.facility.service;

import com.example.facility.shared.exception.ErrorCode;
import com.example.facility.shared.exception.WebException;
import com.example.facility.facility.dto.request.CategoryRequest;
import com.example.facility.facility.dto.request.CreateCategoryRequest;
import com.example.facility.facility.dto.response.CategoryResponse;
import com.example.facility.facility.model.Category;
import com.example.facility.facility.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        if (categoryRepository.findByName(request.getName()).isPresent()) {
            throw new WebException(ErrorCode.CONFLICT);
        }
        Category category = new Category();
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        return mapToResponse(categoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories(boolean enabledOnly) {
        List<Category> categories = enabledOnly
                ? categoryRepository.findAllByEnabled(true)
                : categoryRepository.findAll();
        return categories.stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        return mapToResponse(categoryRepository.findById(id)
                .orElseThrow(() -> new WebException(ErrorCode.RESOURCE_NOT_FOUND)));
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new WebException(ErrorCode.RESOURCE_NOT_FOUND));

        // Reject rename to an existing name that belongs to a different category
        if (request.getName() != null && !request.getName().equals(category.getName())) {
            categoryRepository.findByName(request.getName()).ifPresent(existing -> {
                if (!existing.getId().equals(id)) throw new WebException(ErrorCode.CONFLICT);
            });
            category.setName(request.getName());
        }
        if (request.getDescription() != null) category.setDescription(request.getDescription());
        if (request.getEnabled() != null)     category.setEnabled(request.getEnabled());

        return mapToResponse(categoryRepository.save(category));
    }

    // Soft-delete: sets enabled = false to preserve FK integrity with existing tickets
    @Transactional
    public void disableCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new WebException(ErrorCode.RESOURCE_NOT_FOUND));
        category.setEnabled(false);
        categoryRepository.save(category);
    }

    CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .enabled(category.getEnabled())
                .build();
    }
}

