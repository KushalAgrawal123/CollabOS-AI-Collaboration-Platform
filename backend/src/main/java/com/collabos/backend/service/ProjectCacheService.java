package com.collabos.backend.service;

import com.collabos.backend.dto.ProjectResponse;
import com.collabos.backend.repository.ProjectRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class ProjectCacheService {

    private static final Duration TTL = Duration.ofMinutes(2);

    private final ProjectRepository projectRepository;
    private final JsonCache jsonCache;

    public ProjectCacheService(ProjectRepository projectRepository, JsonCache jsonCache) {
        this.projectRepository = projectRepository;
        this.jsonCache = jsonCache;
    }

    public List<ProjectResponse> list(Long organizationId) {
        String key = key(organizationId);
        List<ProjectResponse> cached = jsonCache.getList(key, ProjectResponse.class);
        if (cached != null) {
            return cached;
        }

        List<ProjectResponse> fresh = projectRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId).stream()
                .map(ProjectResponse::from)
                .toList();
        jsonCache.putList(key, fresh, TTL);
        return fresh;
    }

    public void evict(Long organizationId) {
        jsonCache.evict(key(organizationId));
    }

    private String key(Long organizationId) {
        return "organizationProjects::" + organizationId;
    }
}
