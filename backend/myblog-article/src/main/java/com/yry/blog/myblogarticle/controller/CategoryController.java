package com.yry.blog.myblogarticle.controller;

import com.yry.blog.myblogarticle.dto.CategoryDTO;
import com.yry.blog.myblogarticle.service.CategoryService;
import com.yry.blog.myblogarticle.vo.CategoryVO;
import com.yry.blog.myblogcommon.annotation.RequiresPermission;
import com.yry.blog.myblogcommon.result.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    
    @Autowired
    private CategoryService categoryService;
    
    @GetMapping
    public Response<List<CategoryVO>> getAllCategories() {
        return categoryService.getAllCategories();
    }
    
    @GetMapping("/{id}")
    public Response<CategoryVO> getCategoryById(@PathVariable Long id) {
        return categoryService.getCategoryById(id);
    }
    
    @GetMapping("/article/{articleId}")
    public Response<List<CategoryVO>> getCategoriesByArticleId(@PathVariable Long articleId) {
        return categoryService.getCategoriesByArticleId(articleId);
    }
    
    @RequiresPermission("category:create")
    @PostMapping
    public Response<CategoryVO> createCategory(@RequestBody CategoryDTO dto) {
        return categoryService.createCategory(dto);
    }
    
    @RequiresPermission("category:update")
    @PutMapping("/{id}")
    public Response<CategoryVO> updateCategory(@PathVariable Long id, @RequestBody CategoryDTO dto) {
        return categoryService.updateCategory(id, dto);
    }
    
    @RequiresPermission("category:delete")
    @DeleteMapping("/{id}")
    public Response<Object> deleteCategory(@PathVariable Long id) {
        return categoryService.deleteCategory(id);
    }
}
