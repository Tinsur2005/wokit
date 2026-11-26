package cn.tinsur.wokit.service.impl;

import cn.tinsur.wokit.exception.ServiceException;
import cn.tinsur.wokit.mapper.TaskCategoryMapper;
import cn.tinsur.wokit.mapper.TaskMapper;
import cn.tinsur.wokit.pojo.entity.Task;
import cn.tinsur.wokit.pojo.entity.TaskCategory;
import cn.tinsur.wokit.pojo.vo.TaskCategoryVO;
import cn.tinsur.wokit.service.ITaskCategoryService;
import cn.tinsur.wokit.util.Result;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 任务分类服务实现
 *
 * @author Tinsur
 */
@Service
public class TaskCategoryServiceImpl extends ServiceImpl<TaskCategoryMapper, TaskCategory>
        implements ITaskCategoryService {

    private static final String COLOR_PATTERN = "^#[0-9a-fA-F]{6}$";

    @Autowired
    private TaskMapper taskMapper;

    @Override
    public List<TaskCategoryVO> listWithUsage() {
        List<TaskCategory> categories = list(new LambdaQueryWrapper<TaskCategory>()
                .orderByAsc(TaskCategory::getSort)
                .orderByAsc(TaskCategory::getId));
        if (categories.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, Long> usageMap = new HashMap<>();
        List<Map<String, Object>> rows = taskMapper.selectMaps(new QueryWrapper<Task>()
                .select("task_category_id", "COUNT(*) AS usage_count")
                .isNotNull("task_category_id")
                .groupBy("task_category_id"));
        for (Map<String, Object> row : rows) {
            Object id = row.get("task_category_id");
            Object count = row.get("usage_count");
            if (id instanceof Number idNumber && count instanceof Number countNumber) {
                usageMap.put(idNumber.longValue(), countNumber.longValue());
            }
        }

        return categories.stream().map(category -> {
            TaskCategoryVO vo = new TaskCategoryVO();
            vo.setId(category.getId());
            vo.setName(category.getName());
            vo.setColor(category.getColor());
            vo.setSort(category.getSort());
            vo.setCreateTime(category.getCreateTime());
            vo.setUsageCount(usageMap.getOrDefault(category.getId(), 0L));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public Result add(TaskCategory category) {
        validate(category, null);
        category.setId(null);
        category.setName(category.getName().trim());
        if (category.getSort() == null) {
            category.setSort(0);
        }
        save(category);
        return Result.ok("任务分类已创建");
    }

    @Override
    public Result update(Long id, TaskCategory category) {
        TaskCategory dbCategory = getById(id);
        if (dbCategory == null) {
            throw new ServiceException("任务分类不存在或已被删除");
        }
        validate(category, id);
        dbCategory.setName(category.getName().trim());
        dbCategory.setColor(category.getColor().trim());
        dbCategory.setSort(category.getSort() == null ? dbCategory.getSort() : category.getSort());
        updateById(dbCategory);
        return Result.ok("任务分类已更新");
    }

    @Override
    public Result delete(Long id) {
        TaskCategory category = getById(id);
        if (category == null) {
            throw new ServiceException("任务分类不存在或已被删除");
        }
        long usageCount = taskMapper.selectCount(
                new LambdaQueryWrapper<Task>().eq(Task::getTaskCategoryId, id));
        if (usageCount > 0) {
            throw new ServiceException("该分类已被 " + usageCount + " 条任务使用，只能修改不能删除");
        }
        removeById(id);
        return Result.ok("任务分类已删除");
    }

    @Override
    public TaskCategory fallbackCategory() {
        TaskCategory other = getOne(new LambdaQueryWrapper<TaskCategory>()
                .eq(TaskCategory::getName, "其他")
                .orderByAsc(TaskCategory::getSort)
                .last("LIMIT 1"));
        if (other != null) {
            return other;
        }
        return getOne(new LambdaQueryWrapper<TaskCategory>()
                .orderByAsc(TaskCategory::getSort)
                .orderByAsc(TaskCategory::getId)
                .last("LIMIT 1"));
    }

    private void validate(TaskCategory category, Long excludeId) {
        if (category == null || ObjectUtils.isEmpty(category.getName())
                || category.getName().trim().isEmpty()) {
            throw new ServiceException("请填写任务分类名称");
        }
        String name = category.getName().trim();
        if (name.length() > 30) {
            throw new ServiceException("任务分类名称不能超过30个字");
        }
        if (ObjectUtils.isEmpty(category.getColor())
                || !category.getColor().trim().matches(COLOR_PATTERN)) {
            throw new ServiceException("请选择有效的分类颜色");
        }
        if (category.getSort() != null && category.getSort() < 0) {
            throw new ServiceException("分类排序值不能为负数");
        }
        LambdaQueryWrapper<TaskCategory> wrapper = new LambdaQueryWrapper<TaskCategory>()
                .eq(TaskCategory::getName, name);
        if (excludeId != null) {
            wrapper.ne(TaskCategory::getId, excludeId);
        }
        if (count(wrapper) > 0) {
            throw new ServiceException("任务分类名称已存在");
        }
    }
}
