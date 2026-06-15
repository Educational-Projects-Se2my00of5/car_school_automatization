package ru.hits.car_school_automatization.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import ru.hits.car_school_automatization.enums.Role;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Converter(autoApply = true)
public class RoleListConverter implements AttributeConverter<List<Role>, String> {

    private static final String DELIMITER = ",";

    @Override
    public String convertToDatabaseColumn(List<Role> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        return attribute.stream().map(Role::name).collect(Collectors.joining(DELIMITER));
    }

    @Override
    public List<Role> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(dbData.split(DELIMITER))
                .map(Role::valueOf)
                .collect(Collectors.toList());
    }
}
