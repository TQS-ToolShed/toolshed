package com.toolshed.backend.repository;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.toolshed.backend.repository.entities.Tool;
import com.toolshed.backend.repository.entities.User;

@DataJpaTest
public class ToolRepositoryTest {
    
    @Autowired
    private ToolRepository repo;

    @BeforeEach
    void setUp() {
        
        // clean up
        repo.deleteAll();

    
        User defaultOwner = new User();
        defaultOwner.setId(UUID.randomUUID());
        defaultOwner.setEmail("jane.doe@example.com");
        defaultOwner.setFirstName("Jane");
        defaultOwner.setLastName("Doe");

        // 1. Direct title match
        Tool drill = new Tool();
        drill.setId(UUID.randomUUID());
        drill.setTitle("Power Drill");
        drill.setDescription("Cordless 18V battery powered");
        drill.setPricePerDay(4.5);
        drill.setActive(true);

        // 2. Description Match
        Tool bitSet = new Tool();
        bitSet.setId(UUID.randomUUID());
        bitSet.setTitle("Bit Set");
        bitSet.setDescription("Titanium bits for drill and driver");
        bitSet.setPricePerDay(5.0);
        bitSet.setActive(true);

        // 3. Case Insensitivity (Upper case in DB)
        Tool hammer = new Tool();
        hammer.setId(UUID.randomUUID());
        hammer.setTitle("Heavy HAMMER");
        hammer.setDescription("Standard claw hammer");
        hammer.setPricePerDay(10.0);
        hammer.setActive(true);

        // 4. Distractor (Should not match 'drill' or 'hammer')
        Tool saw = new Tool();
        saw.setId(UUID.randomUUID());
        saw.setTitle("Circular Saw");
        saw.setDescription("Perfect for cutting wood");
        saw.setPricePerDay(20.0);
        saw.setActive(true);
        
        // 5. Inactive Tool (constraint check)
        Tool inactive = new Tool();
        inactive.setId(UUID.randomUUID());
        inactive.setTitle("Old Drill");
        inactive.setDescription("Broken");
        inactive.setActive(false);

    }


}
