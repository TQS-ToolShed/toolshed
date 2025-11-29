package com.toolshed.backend.service;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import com.toolshed.backend.repository.ToolRepository;
import com.toolshed.backend.repository.entities.Tool;

@Service
public class ToolServiceImpl implements ToolService {

    // Dependency Injection: Inject the repository bean
    private final ToolRepository toolRepo;

    public ToolServiceImpl(ToolRepository toolRepo) {
        this.toolRepo = toolRepo;
    }

    /**
     * Implements the search functionality based on US1 criteria.
     * Handles input validation (null/whitespace) before delegating to the repository.
     */
    @Override
    public List<Tool> searchTools(String keyword) {

        if (keyword == null) {
            return Collections.emptyList();
        }

        String trimmedKeyword = keyword.trim();
        return toolRepo.searchTools(trimmedKeyword);
    }
}
