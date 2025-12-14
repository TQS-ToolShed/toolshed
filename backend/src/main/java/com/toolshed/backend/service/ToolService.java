package com.toolshed.backend.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.toolshed.backend.dto.UpdateToolInput;
import com.toolshed.backend.repository.entities.Tool;
import com.toolshed.backend.dto.CreateToolInput;

public interface ToolService {
    List<Tool> searchTools(String keyword, String district, Double minPrice, Double maxPrice);

    Optional<Tool> getById(UUID id);

    List<Tool> getAll();

    List<Tool> getActive();

    String createTool(CreateToolInput input);

    void deleteTool(String toolId);

    void updateTool(String toolId, UpdateToolInput input);

    List<Tool> getByOwner(UUID ownerId);

    void setMaintenance(String toolId, java.time.LocalDate availableDate);

}
