package com.yry.blog.myblogarticle.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yry.blog.myblogarticle.dto.CategoryDTO;
import com.yry.blog.myblogarticle.mapper.ArticleCategoryMapper;
import com.yry.blog.myblogarticle.mapper.CategoryMapper;
import com.yry.blog.myblogarticle.service.CategoryService;
import com.yry.blog.myblogarticle.vo.CategoryVO;
import com.yry.blog.myblogcommon.entity.article.ArticleCategory;
import com.yry.blog.myblogcommon.entity.article.Category;
import com.yry.blog.myblogcommon.enums.ResponseCodeEnums;
import com.yry.blog.myblogcommon.exception.BusinessException;
import com.yry.blog.myblogcommon.result.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl implements CategoryService {
    
    @Autowired
    private CategoryMapper categoryMapper;
    
    @Autowired
    private ArticleCategoryMapper articleCategoryMapper;

    @Override
    public Response<List<CategoryVO>> getAllCategories() {
        List<Category> categories = categoryMapper.selectList(
            new LambdaQueryWrapper<Category>().orderByAsc(Category::getCreatedAt)
        );
        
        List<CategoryVO> categoryVOs = categories.stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
        
        return Response.success(categoryVOs);
    }

    @Override
    public Response<CategoryVO> getCategoryById(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            return Response.error(ResponseCodeEnums.NOT_FOUND, "分类不存在");
        }
        return Response.success(convertToVO(category));
    }

    @Override
    public Response<List<CategoryVO>> getCategoriesByArticleId(Long articleId) {
        List<Category> categories = categoryMapper.selectByArticleId(articleId);
        List<CategoryVO> categoryVOs = categories.stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
        return Response.success(categoryVOs);
    }
    
    @Override
    @Transactional
    public Response<CategoryVO> createCategory(CategoryDTO dto) {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getName, dto.getName());
        if (categoryMapper.selectCount(wrapper) > 0) {
            return Response.error(ResponseCodeEnums.BAD_REQUEST, "分类名称已存在");
        }
        
        Category category = Category.builder()
            .name(dto.getName())
            .description(dto.getDescription())
            .build();
        categoryMapper.insert(category);
        
        return Response.success(convertToVO(category));
    }
    
    @Override
    @Transactional
    public Response<CategoryVO> updateCategory(Long id, CategoryDTO dto) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            return Response.error(ResponseCodeEnums.NOT_FOUND, "分类不存在");
        }
        
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getName, dto.getName()).ne(Category::getId, id);
        if (categoryMapper.selectCount(wrapper) > 0) {
            return Response.error(ResponseCodeEnums.BAD_REQUEST, "分类名称已存在");
        }
        
        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        categoryMapper.updateById(category);
        
        return Response.success(convertToVO(category));
    }
    
    @Override
    @Transactional
    public Response<Object> deleteCategory(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            return Response.error(ResponseCodeEnums.NOT_FOUND, "分类不存在");
        }
        
        long articleCount = articleCategoryMapper.selectCount(
            new LambdaQueryWrapper<ArticleCategory>().eq(ArticleCategory::getCategoryId, id)
        );
        if (articleCount > 0) {
            return Response.error(ResponseCodeEnums.BAD_REQUEST, "该分类下存在文章，无法删除");
        }
        
        categoryMapper.deleteById(id);
        return Response.success(id);
    }
    
    private CategoryVO convertToVO(Category category) {
        long articleCount = articleCategoryMapper.selectCount(
            new LambdaQueryWrapper<ArticleCategory>().eq(ArticleCategory::getCategoryId, category.getId())
        );
        
        return CategoryVO.builder()
            .id(category.getId())
            .name(category.getName())
            .description(category.getDescription())
            .createdAt(category.getCreatedAt())
            .articleCount(articleCount)
            .build();
    }
}
