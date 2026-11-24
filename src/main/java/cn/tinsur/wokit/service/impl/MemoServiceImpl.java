package cn.tinsur.wokit.service.impl;

import cn.tinsur.wokit.exception.ServiceException;
import cn.tinsur.wokit.mapper.MemoMapper;
import cn.tinsur.wokit.pojo.entity.Memo;
import cn.tinsur.wokit.service.IMemoService;
import cn.tinsur.wokit.util.Result;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.List;

/**
 * <p>
 * 备忘录表 服务实现类
 * </p>
 *
 * @author Tinsur
 */
@Service
public class MemoServiceImpl extends ServiceImpl<MemoMapper, Memo> implements IMemoService {

    /**
     * 查询当前用户的备忘录列表（置顶优先，最近更新优先）
     */
    @Override
    public List<Memo> list(Long userId, String keyword) {
        LambdaQueryWrapper<Memo> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Memo::getUserId, userId)
                .like(!ObjectUtils.isEmpty(keyword), Memo::getContent, keyword)
                .orderByDesc(Memo::getPinned)
                .orderByDesc(Memo::getUpdateTime);
        return list(lambdaQueryWrapper);
    }

    /**
     * 新增备忘录
     */
    @Override
    public Result add(Memo memo, Long userId) {
        validateMemo(memo);
        memo.setId(null);
        memo.setUserId(userId);
        if (ObjectUtils.isEmpty(memo.getColor())) {
            memo.setColor("blue");
        }
        memo.setPinned(0);
        save(memo);
        return Result.ok("备忘已保存");
    }

    /**
     * 修改备忘录
     */
    @Override
    public Result update(Long id, Memo memo, Long userId) {
        Memo dbMemo = getOwnMemo(id, userId);
        validateMemo(memo);
        memo.setId(dbMemo.getId());
        memo.setUserId(userId);
        // 置顶状态走单独接口，编辑时不覆盖
        memo.setPinned(null);
        updateById(memo);
        return Result.ok("备忘已更新");
    }

    /**
     * 置顶 / 取消置顶
     */
    @Override
    public Result togglePin(Long id, Long userId) {
        Memo dbMemo = getOwnMemo(id, userId);
        Memo updateMemo = new Memo();
        updateMemo.setId(dbMemo.getId());
        updateMemo.setPinned(dbMemo.getPinned() != null && dbMemo.getPinned() == 1 ? 0 : 1);
        updateById(updateMemo);
        return Result.ok(updateMemo.getPinned() == 1 ? "已置顶" : "已取消置顶");
    }

    /**
     * 删除备忘录（真实删除）
     */
    @Override
    public Result delete(Long id, Long userId) {
        getOwnMemo(id, userId);
        removeById(id);
        return Result.ok("备忘已删除");
    }

    /**
     * 校验备忘内容
     */
    private void validateMemo(Memo memo) {
        if (ObjectUtils.isEmpty(memo.getContent()) || memo.getContent().trim().isEmpty()) {
            throw new ServiceException("请填写备忘内容");
        }
        if (memo.getContent().length() > 1000) {
            throw new ServiceException("备忘内容不能超过1000个字");
        }
        memo.setContent(memo.getContent().trim());
    }

    /**
     * 查询备忘并校验归属
     */
    private Memo getOwnMemo(Long id, Long userId) {
        Memo memo = getById(id);
        if (memo == null) {
            throw new ServiceException("备忘录不存在或已被删除");
        }
        if (!memo.getUserId().equals(userId)) {
            throw new ServiceException("无权操作该备忘录");
        }
        return memo;
    }
}
