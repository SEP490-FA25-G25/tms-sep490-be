package org.fyp.tmssep490be.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.fyp.tmssep490be.entities.enums.Skill;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA Converter to convert List<Skill> to JSON string for database storage
 * and vice versa for entity usage.
 */
@Converter
public class SkillListConverter implements AttributeConverter<List<Skill>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<Skill> skills) {
        if (skills == null || skills.isEmpty()) {
            return "[]";
        }
        try {
            // Convert List<Skill> to List<String> (enum names) then to JSON
            List<String> skillNames = skills.stream()
                    .map(Skill::name)
                    .toList();
            return objectMapper.writeValueAsString(skillNames);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    @Override
    public List<Skill> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank() || dbData.equals("null")) {
            return new ArrayList<>();
        }

        try {
            // Try parsing as JSON array first
            List<String> skillNames = objectMapper.readValue(dbData, new TypeReference<List<String>>() {});
            return skillNames.stream()
                    .map(name -> {
                        try {
                            return Skill.valueOf(name);
                        } catch (IllegalArgumentException e) {
                            return Skill.GENERAL;
                        }
                    })
                    .toList();
        } catch (JsonProcessingException e) {
            // Fallback: if dbData is a single enum value (migration from old data)
            try {
                Skill singleSkill = Skill.valueOf(dbData);
                return new ArrayList<>(List.of(singleSkill));
            } catch (IllegalArgumentException ex) {
                return new ArrayList<>(List.of(Skill.GENERAL));
            }
        }
    }
}
