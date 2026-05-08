package ru.hits.car_school_automatization.util;

import ru.hits.car_school_automatization.entity.User;
import ru.hits.car_school_automatization.enums.Role;
import ru.hits.car_school_automatization.exception.ForbiddenException;

public final class RoleUtils {

    private RoleUtils() {
    }

    public static boolean isTeacher(User user) {
        return user.getRole().contains(Role.TEACHER);
    }

    public static boolean isTeacherOrManager(User user) {
        return user.getRole().contains(Role.TEACHER) || user.getRole().contains(Role.MANAGER);
    }

    public static void requireTeacher(User user, String message) {
        if (!isTeacher(user)) {
            throw new ForbiddenException(message);
        }
    }
}
