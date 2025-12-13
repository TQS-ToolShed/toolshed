package com.toolshed.backend.service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.toolshed.backend.dto.CreateToolInput;
import com.toolshed.backend.dto.UpdateToolInput;
import com.toolshed.backend.repository.BookingRepository;
import com.toolshed.backend.repository.ToolRepository;
import com.toolshed.backend.repository.UserRepository;
import com.toolshed.backend.repository.entities.Tool;
import com.toolshed.backend.repository.entities.User;

import jakarta.transaction.Transactional;

@Service
public class ToolServiceImpl implements ToolService {

    // Dependency Injection: Inject the repository bean
    private final ToolRepository toolRepo;
    private final UserRepository userRepo;
    private final BookingRepository bookingRepo;
    private final IGeoApiService geoApiService;

    public ToolServiceImpl(ToolRepository toolRepo, UserRepository userRepo, BookingRepository bookingRepo, IGeoApiService geoApiService) {
        this.toolRepo = toolRepo;
        this.userRepo = userRepo;
        this.bookingRepo = bookingRepo;
        this.geoApiService = geoApiService;
    }

    /**
     * Implements the search functionality based on US1 criteria.
     * Handles input validation (null/whitespace) before delegating to the repository.
     */
    @Override
    public List<Tool> searchTools(String keyword, String location, Double minPrice, Double maxPrice) {
        String trimmedKeyword = keyword == null ? null : keyword.trim();
        String trimmedLocation = location == null ? null : location.trim();
        
        // Sanitize negative prices
        if (minPrice != null && minPrice < 0) {
            minPrice = 0.0;
        }
        if (maxPrice != null && maxPrice < 0) {
            maxPrice = 0.0;
        }

        // If all filters are empty/null, return empty list (no search criteria)
        if ((trimmedKeyword == null || trimmedKeyword.isEmpty()) 
            && (trimmedLocation == null || trimmedLocation.isEmpty())
            && minPrice == null 
            && maxPrice == null) {
            return Collections.emptyList();
        }

        // Note: location parameter can now match district or municipality
        return toolRepo.searchTools(trimmedKeyword, trimmedLocation, minPrice, maxPrice);
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
    public List<Tool> getActive() {
        return toolRepo.findByActiveTrue();
    }

    @Override
    @Transactional
    public String createTool(CreateToolInput input) {
        User supplier = userRepo.findById(input.getSupplierId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier not found"));

        // Validate district using GeoAPI
        if (!geoApiService.districtExists(input.getDistrict())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid district: " + input.getDistrict());
        }

        Tool tool = new Tool();
        tool.setTitle(input.getTitle());
        tool.setDescription(input.getDescription());
        tool.setPricePerDay(input.getPricePerDay());
        tool.setDistrict(input.getDistrict());
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

        if (input.getTitle() != null) {
            tool.setTitle(input.getTitle());
        }
        if (input.getDescription() != null) {
            tool.setDescription(input.getDescription());
        }
        if (input.getPricePerDay() != null) {
            tool.setPricePerDay(input.getPricePerDay());
        }
        
        // Validate district if it is being updated
        if (input.getDistrict() != null) {
            if (!geoApiService.districtExists(input.getDistrict())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid district: " + input.getDistrict());
            }
            tool.setDistrict(input.getDistrict());
        }
        
        if (input.getActive() != null) {
            boolean requestedActive = input.getActive();
            if (requestedActive && !tool.isActive()) {
                long activeRentals = bookingRepo.countActiveApprovedBookingsForToolOnDate(id, LocalDate.now());
                if (activeRentals > 0) {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Tool is currently rented and cannot be marked as available"
                    );
                }
            }
            tool.setActive(requestedActive);
        }
        if (input.getAvailabilityCalendar() != null) {
            tool.setAvailabilityCalendar(input.getAvailabilityCalendar());
        }
        if (input.getOverallRating() != null) {
            tool.setOverallRating(input.getOverallRating());
        }
        if (input.getNumRatings() != null) {
            tool.setNumRatings(input.getNumRatings());
        }

        if (input.getOwnerId() != null) {
            User supplier = userRepo.findById(input.getOwnerId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier not found"));
            tool.setOwner(supplier);
        }

        toolRepo.save(tool);

    }
}
