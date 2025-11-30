package com.toolshed.backend.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.toolshed.backend.dto.CreateToolInput;
import com.toolshed.backend.dto.UpdateToolInput;
import com.toolshed.backend.repository.UserRepository;
import com.toolshed.backend.repository.entities.User;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.toolshed.backend.repository.ToolRepository;
import com.toolshed.backend.repository.entities.Tool;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ToolServiceImpl implements ToolService {

    // Dependency Injection: Inject the repository bean
    private final ToolRepository toolRepo;
    private final UserRepository userRepo;

    public ToolServiceImpl(ToolRepository toolRepo, UserRepository userRepo) {
        this.toolRepo = toolRepo;
        this.userRepo = userRepo;
    }

    /**
     * Implements the search functionality based on US1 criteria.
     * Handles input validation (null/whitespace) before delegating to the repository.
     */
    @Override
    public List<Tool> searchTools(String keyword, String location) {

        if (keyword == null) {
            return Collections.emptyList();
        }

        String trimmedKeyword = keyword.trim();
        String trimmedLocation = location == null ? null : location.trim();
        return toolRepo.searchTools(trimmedKeyword, trimmedLocation);
    }

    @Override
    public Optional<Tool> getById(UUID id) {
        return toolRepo.findById(id);
    }

    @Override
    public List<Tool> getAll() {
        return toolRepo.findAll();
    }

    @Override
    @Transactional
    public String createTool(CreateToolInput input) {
        User supplier = userRepo.findById(input.getSupplierId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier not found"));

        Tool tool = new Tool();
        tool.setTitle(input.getTitle());
        tool.setDescription(input.getDescription());
        tool.setPricePerDay(input.getPricePerDay());
        tool.setLocation(input.getLocation());
        tool.setOwner(supplier);
        tool.setActive(true);
        tool.setOverallRating(0.0);
        tool.setNumRatings(0);

        toolRepo.save(tool);
        return tool.getId().toString();

    }

    @Override
    @Transactional
    public void deleteTool(String toolId) {
        UUID id = UUID.fromString(toolId);
        if (!toolRepo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tool not found");
        }
        toolRepo.deleteById(id);

    }

    @Override
    @Transactional
    public void updateTool(String toolId, UpdateToolInput input) {
        UUID id = UUID.fromString(toolId);
        Tool tool = toolRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tool not found"));

        tool.setTitle(input.getTitle());
        tool.setDescription(input.getDescription());
        tool.setPricePerDay(input.getPricePerDay());
        tool.setLocation(input.getLocation());
        tool.setActive(input.getActive());
        tool.setAvailabilityCalendar(input.getAvailabilityCalendar());
        tool.setOverallRating(input.getOverallRating());
        tool.setNumRatings(input.getNumRatings());

        if (input.getOwnerId() != null) {
            User supplier = userRepo.findById(input.getOwnerId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier not found"));
            tool.setOwner(supplier);
        }

        toolRepo.save(tool);

    }
}
