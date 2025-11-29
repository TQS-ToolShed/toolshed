package com.toolshed.backend.service;

import java.util.List;

import com.toolshed.backend.repository.entities.Tool;

public interface ToolService {
    
    /**
     * Searches for tools based on a keyword.
     * * @param keyword The search term (title or description).
     * @return A list of matching tools.
     */
    List<Tool> searchTools(String keyword);
}