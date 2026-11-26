package cn.tinsur.wokit.service;

import cn.tinsur.wokit.pojo.entity.TaskCategory;
import cn.tinsur.wokit.pojo.vo.TaskCategoryVO;
import cn.tinsur.wokit.util.Result;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 任务分类服务
 *
 * @author Tinsur
 */
public interface ITaskCategoryService extends IService<TaskCategory> {

    List<TaskCategoryVO> listWithUsage();

    Result add(TaskCategory category);

    Result update(Long id, TaskCategory category);

    Result delete(Long id);

    /**
     * 兼容旧开放接口：项目任务未传分类时取“其他”，没有则取排序最前的分类。
     */
    TaskCategory fallbackCategory();
}
