package cn.tinsur.wokit.service.impl;

import cn.tinsur.wokit.exception.ServiceException;
import cn.tinsur.wokit.mapper.ProjectCategoryMapper;
import cn.tinsur.wokit.mapper.ProjectMapper;
import cn.tinsur.wokit.pojo.dto.ProjectCategoryDTO;
import cn.tinsur.wokit.pojo.entity.Project;
import cn.tinsur.wokit.pojo.entity.ProjectCategory;
import cn.tinsur.wokit.pojo.vo.ProjectCategoryVO;
import cn.tinsur.wokit.service.IProjectCategoryService;
import cn.tinsur.wokit.util.Result;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * 项目分类 服务实现类
 * </p>
 *
 * @author Tinsur
 */
@Service
public class ProjectCategoryServiceImpl extends ServiceImpl<ProjectCategoryMapper, ProjectCategory>
        implements IProjectCategoryService {

    @Autowired
    private ProjectMapper projectMapper;

    @Override
    public List<ProjectCategoryVO> listByUser(Long userId) {
        List<ProjectCategory> categories = list(new LambdaQueryWrapper<ProjectCategory>()
                .eq(ProjectCategory::getUserId, userId)
                .orderByAsc(ProjectCategory::getCreateTime)
                .orderByAsc(ProjectCategory::getId));
        Map<Long, ProjectCategory> categoryMap = categories.stream()
                .collect(Collectors.toMap(ProjectCategory::getId, Function.identity()));
        Map<Long, Long> projectCountMap = projectMapper.selectList(new LambdaQueryWrapper<Project>()
                        .eq(Project::getUserId, userId))
                .stream()
                .filter(project -> project.getCategoryId() != null)
                .collect(Collectors.groupingBy(Project::getCategoryId, Collectors.counting()));

        return categories.stream().map(category -> {
            ProjectCategoryVO vo = new ProjectCategoryVO();
            vo.setId(category.getId());
            vo.setName(category.getName());
            vo.setParentId(category.getParentId());
            vo.setPath(buildPath(category.getId(), categoryMap));
            vo.setCreateTime(category.getCreateTime());
            vo.setProjectCount(projectCountMap.getOrDefault(category.getId(), 0L).intValue());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public Result add(ProjectCategoryDTO dto, Long userId) {
        Long parentId = normalizeParentId(dto == null ? null : dto.getParentId());
        validateParent(parentId, userId, null);
        String name = validateName(dto == null ? null : dto.getName(), userId, parentId, null);
        ProjectCategory category = new ProjectCategory();
        category.setUserId(userId);
        category.setParentId(parentId);
        category.setName(name);
        save(category);
        return Result.ok("项目分类已创建");
    }

    @Override
    public Result update(Long id, ProjectCategoryDTO dto, Long userId) {
        getOwnCategory(id, userId);
        Long parentId = normalizeParentId(dto == null ? null : dto.getParentId());
        validateParent(parentId, userId, id);
        String name = validateName(dto == null ? null : dto.getName(), userId, parentId, id);
        ProjectCategory category = new ProjectCategory();
        category.setId(id);
        category.setParentId(parentId);
        category.setName(name);
        updateById(category);
        return Result.ok("项目分类已更新");
    }

    @Override
    public Result delete(Long id, Long userId) {
        getOwnCategory(id, userId);
        Long childCount = count(new LambdaQueryWrapper<ProjectCategory>()
                .eq(ProjectCategory::getUserId, userId)
                .eq(ProjectCategory::getParentId, id));
        if (childCount > 0) {
            throw new ServiceException("该分类下还有子分类，请先移动或删除这些子分类");
        }
        Long projectCount = projectMapper.selectCount(new LambdaQueryWrapper<Project>()
                .eq(Project::getUserId, userId)
                .eq(Project::getCategoryId, id));
        if (projectCount > 0) {
            throw new ServiceException("该分类下还有项目，请先移动或删除这些项目");
        }
        removeById(id);
        return Result.ok("项目分类已删除");
    }

    /**
     * 校验分类名称：必填、不超长、同一父分类下不可重名
     */
    private String validateName(String rawName, Long userId, Long parentId, Long excludeId) {
        if (ObjectUtils.isEmpty(rawName) || rawName.trim().isEmpty()) {
            throw new ServiceException("请填写分类名称");
        }
        String name = rawName.trim();
        if (name.length() > 20) {
            throw new ServiceException("分类名称不能超过20个字");
        }
        LambdaQueryWrapper<ProjectCategory> wrapper = new LambdaQueryWrapper<ProjectCategory>()
                .eq(ProjectCategory::getUserId, userId)
                .eq(ProjectCategory::getName, name);
        if (parentId == null) {
            wrapper.isNull(ProjectCategory::getParentId);
        } else {
            wrapper.eq(ProjectCategory::getParentId, parentId);
        }
        if (excludeId != null) {
            wrapper.ne(ProjectCategory::getId, excludeId);
        }
        if (count(wrapper) > 0) {
            throw new ServiceException(parentId == null ? "该一级分类名称已存在" : "同一父分类下名称已存在");
        }
        return name;
    }

    /**
     * 校验父分类：必须属于当前用户，且不能选择自己或自己的后代
     */
    private void validateParent(Long parentId, Long userId, Long excludeId) {
        if (parentId == null) {
            return;
        }
        ProjectCategory parent = getOwnCategory(parentId, userId);
        if (excludeId == null) {
            return;
        }
        if (parentId.equals(excludeId)) {
            throw new ServiceException("不能选择自己作为父分类");
        }
        Map<Long, ProjectCategory> categoryMap = list(new LambdaQueryWrapper<ProjectCategory>()
                .eq(ProjectCategory::getUserId, userId))
                .stream()
                .collect(Collectors.toMap(ProjectCategory::getId, Function.identity()));
        ProjectCategory cursor = parent;
        Set<Long> visited = new HashSet<>();
        while (cursor != null && visited.add(cursor.getId())) {
            if (cursor.getId().equals(excludeId)) {
                throw new ServiceException("不能选择自己的子分类作为父分类");
            }
            cursor = cursor.getParentId() == null ? null : categoryMap.get(cursor.getParentId());
        }
    }

    private Long normalizeParentId(Long parentId) {
        return parentId == null || parentId <= 0 ? null : parentId;
    }

    private String buildPath(Long categoryId, Map<Long, ProjectCategory> categoryMap) {
        List<String> names = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        Long cursor = categoryId;
        while (cursor != null && visited.add(cursor)) {
            ProjectCategory category = categoryMap.get(cursor);
            if (category == null) {
                break;
            }
            names.add(0, category.getName());
            cursor = category.getParentId();
        }
        return String.join(" / ", names);
    }

    private ProjectCategory getOwnCategory(Long id, Long userId) {
        ProjectCategory category = getById(id);
        if (category == null) {
            throw new ServiceException("项目分类不存在或已被删除");
        }
        if (!category.getUserId().equals(userId)) {
            throw new ServiceException("无权操作该项目分类");
        }
        return category;
    }
}
