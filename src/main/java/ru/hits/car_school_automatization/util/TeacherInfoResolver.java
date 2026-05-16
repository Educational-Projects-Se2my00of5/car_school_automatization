package ru.hits.car_school_automatization.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.hits.car_school_automatization.entity.Metric;
import ru.hits.car_school_automatization.entity.MetricChange;
import ru.hits.car_school_automatization.entity.MetricValue;
import ru.hits.car_school_automatization.entity.User;
import ru.hits.car_school_automatization.repository.MetricChangeRepository;
import ru.hits.car_school_automatization.repository.MetricRepository;
import ru.hits.car_school_automatization.repository.MetricValueRepository;
import ru.hits.car_school_automatization.repository.UserRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TeacherInfoResolver {

    private final MetricRepository metricRepository;
    private final MetricValueRepository metricValueRepository;
    private final MetricChangeRepository metricChangeRepository;
    private final UserRepository userRepository;

    public TeacherInfo resolve(UUID postId, Long studentId) {
        List<Metric> metrics = metricRepository.findByPostId(postId);
        if (metrics.isEmpty()) {
            return new TeacherInfo(null, null, null);
        }

        List<UUID> metricIds = metrics.stream().map(Metric::getId).toList();
        List<MetricValue> values = metricValueRepository.findByMetricIdInAndUserId(metricIds, studentId);

        MetricChange latest = null;
        for (MetricValue value : values) {
            MetricChange change = metricChangeRepository.findFirstByMetricValueIdOrderByEditedAtDesc(value.getId())
                    .orElse(null);
            if (change == null) {
                continue;
            }
            if (latest == null || change.getEditedAt().isAfter(latest.getEditedAt())) {
                latest = change;
            }
        }

        if (latest == null) {
            return new TeacherInfo(null, null, null);
        }

        User teacher = userRepository.findById(latest.getEditorId()).orElse(null);
        String teacherName = teacher != null ? teacher.getFirstName() + " " + teacher.getLastName() : null;
        return new TeacherInfo(latest.getEditorId(), teacherName, latest.getEditedAt());
    }

    public record TeacherInfo(Long teacherId, String teacherName, Instant lastEditedAt) {
    }
}
