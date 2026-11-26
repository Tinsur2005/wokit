package cn.tinsur.wokit.util;

/**
 * 项目业务状态常量。归档是独立字段，不占状态位。
 *
 * @author Tinsur
 */
public final class ProjectStatus {

    public static final int PLANNING = 0;
    public static final int ACTIVE = 1;
    public static final int MAINTENANCE = 2;
    public static final int PAUSED = 3;
    public static final int COMPLETED = 4;
    public static final int CANCELED = 5;

    private ProjectStatus() {
    }

    public static String name(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case PLANNING -> "规划中";
            case ACTIVE -> "进行中";
            case MAINTENANCE -> "维护中";
            case PAUSED -> "暂停";
            case COMPLETED -> "已完成";
            case CANCELED -> "已取消";
            default -> "未知状态";
        };
    }

    public static boolean canCreateTask(Integer status) {
        return status != null && (status == PLANNING || status == ACTIVE || status == MAINTENANCE);
    }
}
