package cn.tinsur.wokit.service.impl;

import cn.tinsur.wokit.exception.ServiceException;
import cn.tinsur.wokit.mapper.ProjectCategoryMapper;
import cn.tinsur.wokit.mapper.ProjectMapper;
import cn.tinsur.wokit.mapper.TaskMapper;
import cn.tinsur.wokit.mapper.WeeklyMapper;
import cn.tinsur.wokit.mapper.WorkLogMapper;
import cn.tinsur.wokit.pojo.dto.ProjectDTO;
import cn.tinsur.wokit.pojo.entity.Project;
import cn.tinsur.wokit.pojo.entity.ProjectCategory;
import cn.tinsur.wokit.pojo.entity.Task;
import cn.tinsur.wokit.pojo.entity.Weekly;
import cn.tinsur.wokit.pojo.entity.WorkLog;
import cn.tinsur.wokit.pojo.vo.ProjectVO;
import cn.tinsur.wokit.service.IProjectService;
import cn.tinsur.wokit.util.Result;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
 * 项目 服务实现类
 * </p>
 *
 * @author Tinsur
 */
@Service
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project> implements IProjectService {

    @Autowired
    private ProjectCategoryMapper projectCategoryMapper;

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private WeeklyMapper weeklyMapper;

    @Autowired
    private WorkLogMapper workLogMapper;

    @Override
    public List<ProjectVO> listByUser(Long userId, Long categoryId, String keyword) {
        String search = ObjectUtils.isEmpty(keyword) ? null : keyword.trim();
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<Project>()
                .eq(Project::getUserId, userId)
                .eq(categoryId != null, Project::getCategoryId, categoryId)
                .like(!ObjectUtils.isEmpty(search), Project::getName, search)
                .orderByDesc(Project::getCreateTime)
                .orderByDesc(Project::getId);
        List<Project> projects = list(wrapper);

        Map<Long, ProjectCategory> categoryMap = projectCategoryMapper.selectList(
                        new LambdaQueryWrapper<ProjectCategory>().eq(ProjectCategory::getUserId, userId))
                .stream()
                .collect(Collectors.toMap(ProjectCategory::getId, Function.identity()));
        Map<Long, String> categoryPathMap = categoryMap.keySet().stream()
                .collect(Collectors.toMap(Function.identity(), id -> buildCategoryPath(id, categoryMap)));

        return projects.stream().map(project -> {
            ProjectVO vo = new ProjectVO();
            vo.setId(project.getId());
            vo.setName(project.getName());
            vo.setCategoryId(project.getCategoryId());
            vo.setCategoryName(categoryPathMap.getOrDefault(project.getCategoryId(), "未分类"));
            vo.setCreateTime(project.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public Result add(ProjectDTO dto, Long userId) {
        validateProject(dto, userId, null);
        Project project = new Project();
        project.setUserId(userId);
        project.setName(dto.getName().trim());
        project.setCategoryId(dto.getCategoryId());
        save(project);
        return Result.ok("项目已创建");
    }

    @Override
    public Result update(Long id, ProjectDTO dto, Long userId) {
        Project project = getOwnProject(id, userId);
        validateProject(dto, userId, id);
        project.setName(dto.getName().trim());
        project.setCategoryId(dto.getCategoryId());
        updateById(project);
        return Result.ok("项目已更新");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result delete(Long id, Long userId) {
        getOwnProject(id, userId);

        taskMapper.update(null, new LambdaUpdateWrapper<Task>()
                .eq(Task::getUserId, userId)
                .eq(Task::getProjectId, id)
                .set(Task::getProjectId, null));
        weeklyMapper.update(null, new LambdaUpdateWrapper<Weekly>()
                .eq(Weekly::getUserId, userId)
                .eq(Weekly::getProjectId, id)
                .set(Weekly::getProjectId, null));
        workLogMapper.update(null, new LambdaUpdateWrapper<WorkLog>()
                .eq(WorkLog::getUserId, userId)
                .eq(WorkLog::getProjectId, id)
                .set(WorkLog::getProjectId, null));

        removeById(id);
        return Result.ok("项目已删除，相关记录已解除绑定");
    }

    /**
     * 校验项目名称与分类归属
     */
    private void validateProject(ProjectDTO dto, Long userId, Long excludeId) {
        if (dto == null || ObjectUtils.isEmpty(dto.getName()) || dto.getName().trim().isEmpty()) {
            throw new ServiceException("请填写项目名称");
        }
        String name = dto.getName().trim();
        if (name.length() > 60) {
            throw new ServiceException("项目名称不能超过60个字");
        }
        if (dto.getCategoryId() == null) {
            throw new ServiceException("请选择项目分类");
        }
        ProjectCategory category = projectCategoryMapper.selectById(dto.getCategoryId());
        if (category == null || !category.getUserId().equals(userId)) {
            throw new ServiceException("项目分类不存在或无权使用");
        }

        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<Project>()
                .eq(Project::getUserId, userId)
                .eq(Project::getName, name);
        if (excludeId != null) {
            wrapper.ne(Project::getId, excludeId);
        }
        if (count(wrapper) > 0) {
            throw new ServiceException("项目名称已存在");
        }
    }

    private Project getOwnProject(Long id, Long userId) {
        Project project = getById(id);
        if (project == null) {
            throw new ServiceException("项目不存在或已被删除");
        }
        if (!project.getUserId().equals(userId)) {
            throw new ServiceException("无权操作该项目");
        }
        return project;
    }

    private String buildCategoryPath(Long categoryId, Map<Long, ProjectCategory> categoryMap) {
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
}
