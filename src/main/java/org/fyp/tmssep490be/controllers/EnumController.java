package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.entities.enums.MaterialType;
import org.fyp.tmssep490be.entities.enums.Skill;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/enums")
@Tag(name = "Enums", description = "API for retrieving enum values")
public class EnumController {

    @GetMapping("/skills")
    @Operation(summary = "Get all skills")
    public ResponseEntity<ResponseObject<List<String>>> getSkills() {
        List<String> skills = Arrays.stream(Skill.values())
                .map(Enum::name)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ResponseObject.<List<String>>builder()
                .success(true)
                .message("Skills retrieved successfully")
                .data(skills)
                .build());
    }

    @GetMapping("/material-types")
    @Operation(summary = "Get all material types")
    public ResponseEntity<ResponseObject<List<String>>> getMaterialTypes() {
        List<String> materialTypes = Arrays.stream(MaterialType.values())
                .map(Enum::name)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ResponseObject.<List<String>>builder()
                .success(true)
                .message("Material types retrieved successfully")
                .data(materialTypes)
                .build());
    }
}
