package com.example.facility.facility.controller;

import com.example.facility.shared.apiresponse.ApiResponse;
import com.example.facility.facility.dto.request.CategoryRequest;
import com.example.facility.facility.dto.request.CreateCategoryRequest;
import com.example.facility.facility.dto.response.CategoryResponse;
import com.example.facility.facility.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/facilities/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    // FR-CAT-01: create
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @RequestBody CreateCategoryRequest request) {
        CategoryResponse response = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Category created successfully", response));
    }

    // FR-CAT-01: list — enabledOnly=true hides disabled categories for regular users
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TECHNICIAN')")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories(
            @RequestParam(defaultValue = "false") boolean enabledOnly) {
        List<CategoryResponse> categories = categoryService.getAllCategories(enabledOnly);
        return ResponseEntity.ok(ApiResponse.success("Categories retrieved successfully", categories));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TECHNICIAN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Category retrieved successfully",
                categoryService.getCategoryById(id)));
    }

    // FR-CAT-01: update (name, description, enabled)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Long id, @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Category updated successfully",
                categoryService.updateCategory(id, request)));
    }

    // FR-CAT-01: soft-delete (sets enabled=false — preserves FK integrity)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        categoryService.disableCategory(id);
        return ResponseEntity.ok(ApiResponse.success("Category disabled successfully", null));
    }
}

