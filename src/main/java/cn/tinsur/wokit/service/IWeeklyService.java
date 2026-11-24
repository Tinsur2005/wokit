package cn.tinsur.wokit.service;

import cn.tinsur.wokit.pojo.entity.Weekly;
import cn.tinsur.wokit.util.Result;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface IWeeklyService extends IService<Weekly> {

    /**
     * 查询某一周的周报明细
     */
    List<Weekly> list(String weekStart, Long userId);

    /**
     * 新增周报记录
     */
    Result add(Weekly weekly, Long userId);

    /**
     * 修改周报记录
     */
    Result update(Long id, Weekly weekly, Long userId);

    /**
     * 删除周报记录（真实删除）
     */
    Result delete(Long id, Long userId);
}
