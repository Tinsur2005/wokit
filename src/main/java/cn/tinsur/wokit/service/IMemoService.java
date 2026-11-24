package cn.tinsur.wokit.service;

import cn.tinsur.wokit.pojo.entity.Memo;
import cn.tinsur.wokit.util.Result;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface IMemoService extends IService<Memo> {

    /**
     * 查询当前用户的备忘录列表（置顶优先）
     */
    List<Memo> list(Long userId, String keyword);

    /**
     * 新增备忘录
     */
    Result add(Memo memo, Long userId);

    /**
     * 修改备忘录
     */
    Result update(Long id, Memo memo, Long userId);

    /**
     * 置顶 / 取消置顶
     */
    Result togglePin(Long id, Long userId);

    /**
     * 删除备忘录（真实删除）
     */
    Result delete(Long id, Long userId);
}
