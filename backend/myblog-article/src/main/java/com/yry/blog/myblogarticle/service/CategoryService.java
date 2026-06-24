package com.yry.blog.myblogarticle.service;

import com.yry.blog.myblogarticle.dto.CategoryDTO;
import com.yry.blog.myblogarticle.vo.CategoryVO;
import com.yry.blog.myblogcommon.result.Response;

import java.util.List;

public interface CategoryService {
    
    Response<List<CategoryVO>> getAllCategories();
    
    Response<CategoryVO> getCategoryById(Long id);
    
    Response<List<CategoryVO>> getCategoriesByArticleId(Long articleId);
    
    Response<CategoryVO> createCategory(CategoryDTO dto);
    
    Response<CategoryVO> updateCategory(Long id, CategoryDTO dto);
    
    Response<Object> deleteCategory(Long id);
}
