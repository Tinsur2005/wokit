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
import cn.tinsur.wokit.util.ProjectStatus;
import cn.tinsur.wokit.util.Result;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
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
    public List<ProjectVO> listByUser(Long userId, Long categoryId, String keyword,
                                      Integer status, Boolean archived) {
        String search = ObjectUtils.isEmpty(keyword) ? null : keyword.trim();
        boolean archivedOnly = Boolean.TRUE.equals(archived);
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<Project>()
                .eq(Project::getUserId, userId)
                .eq(categoryId != null, Project::getCategoryId, categoryId)
                .eq(status != null, Project::getStatus, status)
                .isNotNull(archivedOnly, Project::getArchivedAt)
                .isNull(!archivedOnly, Project::getArchivedAt)
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
        Map<Long, long[]> taskCountMap = taskCounts(userId, projects);

        return projects.stream().map(project -> {
            ProjectVO vo = new ProjectVO();
            vo.setId(project.getId());
            vo.setName(project.getName());
            vo.setCategoryId(project.getCategoryId());
            vo.setCategoryName(categoryPathMap.getOrDefault(project.getCategoryId(), "未分类"));
            Integer projectStatus = project.getStatus() == null ? ProjectStatus.ACTIVE : project.getStatus();
            vo.setStatus(projectStatus);
            vo.setStatusName(ProjectStatus.name(projectStatus));
            vo.setArchived(project.getArchivedAt() != null);
            vo.setArchiveReason(project.getArchiveReason());
            vo.setStatusChangedAt(project.getStatusChangedAt());
            vo.setReleasedAt(project.getReleasedAt());
            vo.setCompletedAt(project.getCompletedAt());
            vo.setArchivedAt(project.getArchivedAt());
            vo.setCreateTime(project.getCreateTime());
            vo.setUpdateTime(project.getUpdateTime());
            long[] counts = taskCountMap.getOrDefault(project.getId(), new long[]{0, 0, 0});
            vo.setTaskTotal(counts[0]);
            vo.setTaskOpen(counts[1]);
            vo.setTaskDone(counts[2]);
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
        project.setStatus(ProjectStatus.ACTIVE);
        project.setStatusChangedAt(now());
        save(project);
        return Result.ok("项目已创建");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result update(Long id, ProjectDTO dto, Long userId) {
        Project project = getOwnProject(id, userId);
        ensureNotArchived(project);
        validateProject(dto, userId, id);
        validateStatus(dto.getStatus());
        project.setName(dto.getName().trim());
        project.setCategoryId(dto.getCategoryId());
        updateById(project);
        applyStatusChange(project, dto.getStatus());
        return Result.ok("项目已更新");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result delete(Long id, Long userId) {
        Project project = getOwnProject(id, userId);
        ensureNotArchived(project);

        long taskCount = taskMapper.selectCount(new LambdaQueryWrapper<Task>()
                .eq(Task::getUserId, userId)
                .eq(Task::getProjectId, id));
        long weeklyCount = weeklyMapper.selectCount(new LambdaQueryWrapper<Weekly>()
                .eq(Weekly::getUserId, userId)
                .eq(Weekly::getProjectId, id));
        long workLogCount = workLogMapper.selectCount(new LambdaQueryWrapper<WorkLog>()
                .eq(WorkLog::getUserId, userId)
                .eq(WorkLog::getProjectId, id));
        if (taskCount + weeklyCount + workLogCount > 0) {
            throw new ServiceException("项目还有关联的任务、周报或工时，不能直接删除，请使用归档");
        }

        removeById(id);
        return Result.ok("项目已删除");
    }

    @Override
    public Result changeStatus(Long id, Integer status, Long userId) {
        Project project = getOwnProject(id, userId);
        ensureNotArchived(project);
        validateStatus(status);
        if (!applyStatusChange(project, status)) {
            return Result.ok("项目状态未变化");
        }
        return Result.ok("项目状态已更新为「" + ProjectStatus.name(status) + "」");
    }

    /**
     * 应用项目业务状态变化，维护发布时间和完成时间。
     * 返回 false 表示状态没有变化。
     */
    private boolean applyStatusChange(Project project, Integer status) {
        if (status == null) {
            return false;
        }
        validateStatus(status);
        Integer currentStatus = project.getStatus() == null ? ProjectStatus.ACTIVE : project.getStatus();
        if (status.equals(currentStatus)) {
            return false;
        }

        Date now = now();
        LambdaUpdateWrapper<Project> wrapper = new LambdaUpdateWrapper<Project>()
                .eq(Project::getId, project.getId())
                .set(Project::getStatus, status)
                .set(Project::getStatusChangedAt, now)
                .set(Project::getUpdateTime, now);
        if (status == ProjectStatus.MAINTENANCE && project.getReleasedAt() == null) {
            wrapper.set(Project::getReleasedAt, now);
        }
        if (status == ProjectStatus.COMPLETED) {
            wrapper.set(Project::getCompletedAt, now);
        } else {
            wrapper.set(Project::getCompletedAt, null);
        }
        update(wrapper);
        return true;
    }

    private void validateStatus(Integer status) {
        if (status != null && (status < ProjectStatus.PLANNING || status > ProjectStatus.CANCELED)) {
            throw new ServiceException("项目状态值不正确");
        }
    }

    @Override
    public Result archive(Long id, String reason, Long userId) {
        Project project = getOwnProject(id, userId);
        if (project.getArchivedAt() != null) {
            throw new ServiceException("项目已归档");
        }
        String archiveReason = ObjectUtils.isEmpty(reason) ? null : reason.trim();
        if (archiveReason != null && archiveReason.length() > 200) {
            throw new ServiceException("归档原因不能超过200个字");
        }
        project.setStatusBeforeArchive(project.getStatus() == null
                ? ProjectStatus.ACTIVE : project.getStatus());
        project.setArchivedAt(now());
        project.setArchiveReason(archiveReason);
        updateById(project);
        return Result.ok("项目已归档，可在已归档列表中恢复");
    }

    @Override
    public Result restore(Long id, Long userId) {
        Project project = getOwnProject(id, userId);
        if (project.getArchivedAt() == null) {
            throw new ServiceException("项目未归档，无需恢复");
        }
        Integer restoreStatus = project.getStatusBeforeArchive() == null
                ? ProjectStatus.ACTIVE : project.getStatusBeforeArchive();
        Date now = now();
        update(null, new LambdaUpdateWrapper<Project>()
                .eq(Project::getId, id)
                .set(Project::getArchivedAt, null)
                .set(Project::getArchiveReason, null)
                .set(Project::getStatusBeforeArchive, null)
                .set(Project::getStatus, restoreStatus)
                .set(Project::getStatusChangedAt, now)
                .set(Project::getUpdateTime, now));
        return Result.ok("项目已恢复为「" + ProjectStatus.name(restoreStatus) + "」");
    }

    /**
     * 一次聚合查询当前用户所有项目的任务总数、未完成数和已完成数，避免 N+1。
     */
    private Map<Long, long[]> taskCounts(Long userId, List<Project> projects) {
        Map<Long, long[]> result = new HashMap<>();
        if (projects.isEmpty()) {
            return result;
        }
        List<Long> projectIds = projects.stream().map(Project::getId).collect(Collectors.toList());
        List<Map<String, Object>> rows = taskMapper.selectMaps(new QueryWrapper<Task>()
                .select("project_id",
                        "COUNT(*) AS task_total",
                        "SUM(CASE WHEN status = 3 THEN 1 ELSE 0 END) AS task_done")
                .eq("user_id", userId)
                .eq("task_kind", TaskServiceImpl.TASK_KIND_PROJECT)
                .in("project_id", projectIds)
                .groupBy("project_id"));
        for (Map<String, Object> row : rows) {
            Object projectId = row.get("project_id");
            Object total = row.get("task_total");
            Object done = row.get("task_done");
            if (projectId instanceof Number idNumber) {
                long totalValue = total instanceof Number number ? number.longValue() : 0L;
                long doneValue = done instanceof Number number ? number.longValue() : 0L;
                result.put(idNumber.longValue(), new long[]{totalValue, totalValue - doneValue, doneValue});
            }
        }
        return result;
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

    private void ensureNotArchived(Project project) {
        if (project.getArchivedAt() != null) {
            throw new ServiceException("项目已归档，请先恢复项目");
        }
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

    private Date now() {
        return new Date();
    }
}
