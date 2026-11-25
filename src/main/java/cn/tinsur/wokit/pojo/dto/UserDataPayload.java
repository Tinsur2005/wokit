package cn.tinsur.wokit.pojo.dto;

import cn.tinsur.wokit.pojo.entity.Memo;
import cn.tinsur.wokit.pojo.entity.Project;
import cn.tinsur.wokit.pojo.entity.ProjectCategory;
import cn.tinsur.wokit.pojo.entity.Task;
import cn.tinsur.wokit.pojo.entity.TimerItem;
import cn.tinsur.wokit.pojo.entity.Weekly;
import cn.tinsur.wokit.pojo.entity.WorkLog;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户工作台备份数据
 */
@Data
public class UserDataPayload {

    private List<ProjectCategory> projectCategories = new ArrayList<>();

    private List<Project> projects = new ArrayList<>();

    private List<Task> tasks = new ArrayList<>();

    private List<Memo> memos = new ArrayList<>();

    private List<WorkLog> workLogs = new ArrayList<>();

    private List<Weekly> weeklies = new ArrayList<>();

    private List<TimerItem> timers = new ArrayList<>();
}
