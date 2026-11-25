package cn.tinsur.wokit.service.impl;

import cn.tinsur.wokit.exception.ServiceException;
import cn.tinsur.wokit.mapper.MemoMapper;
import cn.tinsur.wokit.mapper.ProjectCategoryMapper;
import cn.tinsur.wokit.mapper.ProjectMapper;
import cn.tinsur.wokit.mapper.TaskMapper;
import cn.tinsur.wokit.mapper.TimerItemMapper;
import cn.tinsur.wokit.mapper.WeeklyMapper;
import cn.tinsur.wokit.mapper.WorkLogMapper;
import cn.tinsur.wokit.pojo.dto.UserDataPayload;
import cn.tinsur.wokit.pojo.entity.Memo;
import cn.tinsur.wokit.pojo.entity.Project;
import cn.tinsur.wokit.pojo.entity.ProjectCategory;
import cn.tinsur.wokit.pojo.entity.Task;
import cn.tinsur.wokit.pojo.entity.TimerItem;
import cn.tinsur.wokit.pojo.entity.Weekly;
import cn.tinsur.wokit.pojo.entity.WorkLog;
import cn.tinsur.wokit.service.IUserDataService;
import cn.tinsur.wokit.util.Result;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户工作台数据导出与恢复
 */
@Service
public class UserDataServiceImpl implements IUserDataService {

    private static final String APP_NAME = "wokit";
    private static final int BACKUP_VERSION = 1;
    private static final byte[] BACKUP_MAGIC = "WOKITB1".getBytes(StandardCharsets.US_ASCII);
    private static final int IV_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private ProjectCategoryMapper projectCategoryMapper;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private MemoMapper memoMapper;

    @Autowired
    private WorkLogMapper workLogMapper;

    @Autowired
    private WeeklyMapper weeklyMapper;

    @Autowired
    private TimerItemMapper timerItemMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${wokit.backup.secret:wokit-backup-2026-v1-change-me}")
    private String backupSecret;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public byte[] export(Long userId) {
        UserDataPayload payload = collect(userId);
        JsonNode dataNode = objectMapper.valueToTree(payload);
        String exportedAt = LocalDateTime.now().format(TIME_FORMATTER);
        ObjectNode root = objectMapper.createObjectNode();
        root.put("app", APP_NAME);
        root.put("version", BACKUP_VERSION);
        root.put("exportedAt", exportedAt);
        root.put("ownerId", userId);
        root.set("data", dataNode);
        root.put("checksum", checksum(userId, exportedAt, dataNode));
        try {
            return encrypt(objectMapper.writeValueAsString(root));
        } catch (JsonProcessingException e) {
            throw new ServiceException("数据导出失败，请稍后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result restore(byte[] content, Long userId) {
        String json = decrypt(content);
        JsonNode root;
        try {
            root = objectMapper.readTree(json);
        } catch (Exception e) {
            throw damaged();
        }
        if (root == null || !root.isObject()
                || !APP_NAME.equals(root.path("app").asText())
                || root.path("version").asInt(-1) != BACKUP_VERSION) {
            throw damaged();
        }
        long ownerId = root.path("ownerId").asLong(-1);
        if (ownerId != userId.longValue()) {
            throw new ServiceException("该备份文件不属于当前账号，无法恢复");
        }
        JsonNode dataNode = root.get("data");
        String exportedAt = root.path("exportedAt").asText("");
        String savedChecksum = root.path("checksum").asText("");
        if (dataNode == null || !dataNode.isObject() || savedChecksum.isEmpty()
                || !savedChecksum.equals(checksum(userId, exportedAt, dataNode))) {
            throw damaged();
        }
        UserDataPayload payload;
        try {
            payload = objectMapper.treeToValue(dataNode, UserDataPayload.class);
        } catch (Exception e) {
            throw damaged();
        }
        if (payload == null) {
            throw damaged();
        }

        deleteAll(userId);
        importPayload(payload, userId);
        return Result.ok("数据恢复完成");
    }

    private UserDataPayload collect(Long userId) {
        UserDataPayload payload = new UserDataPayload();
        payload.setProjectCategories(projectCategoryMapper.selectList(
                new LambdaQueryWrapper<ProjectCategory>()
                        .eq(ProjectCategory::getUserId, userId)
                        .orderByAsc(ProjectCategory::getId)));
        payload.setProjects(projectMapper.selectList(
                new LambdaQueryWrapper<Project>()
                        .eq(Project::getUserId, userId)
                        .orderByAsc(Project::getId)));
        payload.setTasks(taskMapper.selectList(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getUserId, userId)
                        .orderByAsc(Task::getId)));
        payload.setMemos(memoMapper.selectList(
                new LambdaQueryWrapper<Memo>()
                        .eq(Memo::getUserId, userId)
                        .orderByAsc(Memo::getId)));
        payload.setWorkLogs(workLogMapper.selectList(
                new LambdaQueryWrapper<WorkLog>()
                        .eq(WorkLog::getUserId, userId)
                        .orderByAsc(WorkLog::getId)));
        payload.setWeeklies(weeklyMapper.selectList(
                new LambdaQueryWrapper<Weekly>()
                        .eq(Weekly::getUserId, userId)
                        .orderByAsc(Weekly::getCreateTime)
                        .orderByAsc(Weekly::getId)));
        payload.setTimers(timerItemMapper.selectList(
                new LambdaQueryWrapper<TimerItem>()
                        .eq(TimerItem::getUserId, userId)
                        .orderByAsc(TimerItem::getId)));
        return payload;
    }

    private void deleteAll(Long userId) {
        taskMapper.delete(new LambdaQueryWrapper<Task>().eq(Task::getUserId, userId));
        memoMapper.delete(new LambdaQueryWrapper<Memo>().eq(Memo::getUserId, userId));
        workLogMapper.delete(new LambdaQueryWrapper<WorkLog>().eq(WorkLog::getUserId, userId));
        weeklyMapper.delete(new LambdaQueryWrapper<Weekly>().eq(Weekly::getUserId, userId));
        timerItemMapper.delete(new LambdaQueryWrapper<TimerItem>().eq(TimerItem::getUserId, userId));
        projectMapper.delete(new LambdaQueryWrapper<Project>().eq(Project::getUserId, userId));
        projectCategoryMapper.delete(
                new LambdaQueryWrapper<ProjectCategory>().eq(ProjectCategory::getUserId, userId));
    }

    private void importPayload(UserDataPayload payload, Long userId) {
        Map<Long, Long> categoryIdMap = importCategories(safeList(payload.getProjectCategories()), userId);
        Map<Long, Long> projectIdMap = importProjects(safeList(payload.getProjects()), userId, categoryIdMap);

        for (Task old : safeList(payload.getTasks())) {
            Task task = new Task();
            task.setUserId(userId);
            task.setTitle(old.getTitle());
            task.setDescription(old.getDescription());
            task.setStartTime(old.getStartTime());
            task.setDeadline(old.getDeadline());
            task.setPriority(old.getPriority());
            task.setStatus(old.getStatus());
            task.setProjectId(mapId(projectIdMap, old.getProjectId()));
            taskMapper.insert(task);
        }

        for (Memo old : safeList(payload.getMemos())) {
            Memo memo = new Memo();
            memo.setUserId(userId);
            memo.setContent(old.getContent());
            memo.setColor(old.getColor());
            memo.setPinned(old.getPinned());
            memoMapper.insert(memo);
        }

        for (WorkLog old : safeList(payload.getWorkLogs())) {
            WorkLog workLog = new WorkLog();
            workLog.setUserId(userId);
            workLog.setWorkDate(old.getWorkDate());
            workLog.setProject(old.getProject());
            workLog.setProjectId(mapId(projectIdMap, old.getProjectId()));
            workLog.setContent(old.getContent());
            workLog.setHours(old.getHours());
            workLogMapper.insert(workLog);
        }

        for (Weekly old : safeList(payload.getWeeklies())) {
            Weekly weekly = new Weekly();
            weekly.setUserId(userId);
            weekly.setWeekStart(old.getWeekStart());
            weekly.setWeekEnd(old.getWeekEnd());
            weekly.setProjectId(mapId(projectIdMap, old.getProjectId()));
            weekly.setTaskTitle(old.getTaskTitle());
            weekly.setTaskContent(old.getTaskContent());
            weekly.setTaskResult(old.getTaskResult());
            weekly.setResolvedProblem(old.getResolvedProblem());
            weekly.setProblem(old.getProblem());
            weekly.setOutput(old.getOutput());
            weekly.setExtra(old.getExtra());
            weeklyMapper.insert(weekly);
        }

        for (TimerItem old : safeList(payload.getTimers())) {
            TimerItem timer = new TimerItem();
            timer.setUserId(userId);
            timer.setName(old.getName());
            timer.setMode(old.getMode());
            timer.setTargetTime(old.getTargetTime());
            timerItemMapper.insert(timer);
        }
    }

    private Map<Long, Long> importCategories(List<ProjectCategory> categories, Long userId) {
        Map<Long, Long> idMap = new HashMap<>();
        for (ProjectCategory old : categories) {
            ProjectCategory category = new ProjectCategory();
            category.setUserId(userId);
            category.setParentId(null);
            category.setName(old.getName());
            projectCategoryMapper.insert(category);
            if (old.getId() != null) {
                idMap.put(old.getId(), category.getId());
            }
        }
        for (ProjectCategory old : categories) {
            if (old.getId() == null || old.getParentId() == null) {
                continue;
            }
            Long newId = idMap.get(old.getId());
            Long newParentId = idMap.get(old.getParentId());
            if (newId != null && newParentId != null) {
                ProjectCategory update = new ProjectCategory();
                update.setId(newId);
                update.setParentId(newParentId);
                projectCategoryMapper.updateById(update);
            }
        }
        return idMap;
    }

    private Map<Long, Long> importProjects(List<Project> projects, Long userId,
                                           Map<Long, Long> categoryIdMap) {
        Map<Long, Long> idMap = new HashMap<>();
        for (Project old : projects) {
            Project project = new Project();
            project.setUserId(userId);
            project.setCategoryId(mapId(categoryIdMap, old.getCategoryId()));
            project.setName(old.getName());
            projectMapper.insert(project);
            if (old.getId() != null) {
                idMap.put(old.getId(), project.getId());
            }
        }
        return idMap;
    }

    private Long mapId(Map<Long, Long> idMap, Long oldId) {
        return oldId == null ? null : idMap.get(oldId);
    }

    private <T> List<T> safeList(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }

    private byte[] encrypt(String json) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, backupKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(json.getBytes(StandardCharsets.UTF_8));
            byte[] result = new byte[BACKUP_MAGIC.length + IV_LENGTH + encrypted.length];
            System.arraycopy(BACKUP_MAGIC, 0, result, 0, BACKUP_MAGIC.length);
            System.arraycopy(iv, 0, result, BACKUP_MAGIC.length, IV_LENGTH);
            System.arraycopy(encrypted, 0, result, BACKUP_MAGIC.length + IV_LENGTH, encrypted.length);
            return result;
        } catch (GeneralSecurityException e) {
            throw new ServiceException("数据导出失败，请稍后重试");
        }
    }

    private String decrypt(byte[] content) {
        int minimumLength = BACKUP_MAGIC.length + IV_LENGTH + 16;
        if (content == null || content.length < minimumLength) {
            throw damaged();
        }
        for (int i = 0; i < BACKUP_MAGIC.length; i++) {
            if (content[i] != BACKUP_MAGIC[i]) {
                throw damaged();
            }
        }
        byte[] iv = new byte[IV_LENGTH];
        System.arraycopy(content, BACKUP_MAGIC.length, iv, 0, IV_LENGTH);
        byte[] encrypted = new byte[content.length - BACKUP_MAGIC.length - IV_LENGTH];
        System.arraycopy(content, BACKUP_MAGIC.length + IV_LENGTH, encrypted, 0, encrypted.length);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, backupKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw damaged();
        }
    }

    private SecretKeySpec backupKey() throws GeneralSecurityException {
        byte[] key = MessageDigest.getInstance("SHA-256")
                .digest(backupSecret.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(key, "AES");
    }

    private String checksum(Long userId, String exportedAt, JsonNode dataNode) {
        ObjectNode signed = objectMapper.createObjectNode();
        signed.put("app", APP_NAME);
        signed.put("version", BACKUP_VERSION);
        signed.put("exportedAt", exportedAt);
        signed.put("ownerId", userId);
        signed.set("data", dataNode);
        try {
            String canonical = objectMapper.writeValueAsString(canonicalize(signed));
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b & 0xff));
            }
            return hex.toString();
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            throw new ServiceException("备份文件校验失败");
        }
    }

    private JsonNode canonicalize(JsonNode node) {
        if (node.isObject()) {
            List<String> names = new ArrayList<>();
            node.fieldNames().forEachRemaining(names::add);
            Collections.sort(names);
            ObjectNode sorted = objectMapper.createObjectNode();
            for (String name : names) {
                sorted.set(name, canonicalize(node.get(name)));
            }
            return sorted;
        }
        if (node.isArray()) {
            ArrayNode array = objectMapper.createArrayNode();
            node.forEach(item -> array.add(canonicalize(item)));
            return array;
        }
        return node;
    }

    private ServiceException damaged() {
        return new ServiceException("文件已损坏，无法恢复。请使用系统重新导出的文件，不要手动修改");
    }
}
