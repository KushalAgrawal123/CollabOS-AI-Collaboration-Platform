package com.collabos.backend.service;

import com.collabos.backend.dto.TaskResponse;
import com.collabos.backend.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class TaskCacheService {

    private static final Duration TTL = Duration.ofSeconds(30);

    private final TaskRepository taskRepository;
    private final JsonCache jsonCache;

    public TaskCacheService(TaskRepository taskRepository, JsonCache jsonCache) {
        this.taskRepository = taskRepository;
        this.jsonCache = jsonCache;
    }

    public List<TaskResponse> list(Long organizationId, Long projectId) {
        String key = key(organizationId, projectId);
        List<TaskResponse> cached = jsonCache.getList(key, TaskResponse.class);
        if (cached != null) {
            return cached;
        }

        List<TaskResponse> fresh = taskRepository.findByProjectIdOrderByStatusAscPositionAsc(projectId).stream()
                .map(TaskResponse::from)
                .toList();
        jsonCache.putList(key, fresh, TTL);
        return fresh;
    }

    public void evict(Long organizationId, Long projectId) {
        jsonCache.evict(key(organizationId, projectId));
    }

    private String key(Long organizationId, Long projectId) {
        return "projectTasks::" + organizationId + ":" + projectId;
    }
}
