package com.group2.rms.admin.repository;

import com.group2.rms.admin.entity.AuditLog;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Current display names only. One batched lookup per entity type on the requested page. */
@Repository
@RequiredArgsConstructor
public class AuditLogTargetRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public record Target(String entity, String id) {}

    public Map<Target, String> findNames(List<AuditLog> logs) {
        Map<Target, String> names = new HashMap<>();
        var groups = logs.stream().collect(Collectors.groupingBy(AuditLog::getEntityName));
        groups.forEach((entity, entries) -> {
            String sql = query(entity);
            if (sql == null) return;
            var ids = entries.stream().map(log -> lookupId(entity, log.getEntityId()))
                    .filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
            if (ids.isEmpty()) return;
            Map<String, String> found = jdbc.query(sql, Map.of("ids", ids), rs -> {
                Map<String, String> values = new HashMap<>();
                while (rs.next()) values.put(rs.getString("id"), rs.getString("name"));
                return values;
            });
            for (AuditLog log : entries) {
                Object id = lookupId(entity, log.getEntityId());
                String name = id == null ? null : found.get(id.toString());
                if (name != null && !name.isBlank()) names.put(new Target(entity, log.getEntityId()), name);
            }
        });
        return names;
    }

    private static Object lookupId(String entity, String id) {
        if (id == null || id.isBlank()) return null;
        if ("SystemConfig".equals(entity)) return id;
        try { return Integer.valueOf(id); }
        catch (NumberFormatException ignored) { return null; }
    }

    private static String query(String entity) {
        return switch (entity) {
            case "Application" -> """
                    SELECT a.ApplicationId AS id, CONCAT(u.FullName, N' · ', j.PostingTitle) AS name
                    FROM Application a JOIN Candidate c ON c.CandidateId=a.CandidateId
                    JOIN [User] u ON u.UserId=c.UserId JOIN JobPosting j ON j.JobPostingId=a.JobPostingId
                    WHERE a.ApplicationId IN (:ids)
                    """;
            case "Candidate" -> """
                    SELECT c.CandidateId AS id, u.FullName AS name
                    FROM Candidate c JOIN [User] u ON u.UserId=c.UserId WHERE c.CandidateId IN (:ids)
                    """;
            case "User" -> "SELECT UserId AS id, CONCAT(FullName, N' (', Username, N')') AS name FROM [User] WHERE UserId IN (:ids)";
            case "Department" -> "SELECT DepartmentId AS id, DepartmentName AS name FROM Department WHERE DepartmentId IN (:ids)";
            case "JobRequisition" -> "SELECT RequisitionId AS id, Title AS name FROM JobRequisition WHERE RequisitionId IN (:ids)";
            case "JobPosting" -> "SELECT JobPostingId AS id, PostingTitle AS name FROM JobPosting WHERE JobPostingId IN (:ids)";
            case "TalentPoolNote" -> """
                    SELECT n.NoteId AS id, u.FullName AS name FROM TalentPoolNote n
                    JOIN Candidate c ON c.CandidateId=n.CandidateId JOIN [User] u ON u.UserId=c.UserId
                    WHERE n.NoteId IN (:ids)
                    """;
            case "InterviewSchedule" -> """
                    SELECT i.InterviewId AS id,
                           CONCAT(u.FullName, N' · ', j.PostingTitle, N' · ', CONVERT(NVARCHAR(16),i.StartTime,120)) AS name
                    FROM InterviewSchedule i JOIN Application a ON a.ApplicationId=i.ApplicationId
                    JOIN Candidate c ON c.CandidateId=a.CandidateId JOIN [User] u ON u.UserId=c.UserId
                    JOIN JobPosting j ON j.JobPostingId=a.JobPostingId WHERE i.InterviewId IN (:ids)
                    """;
            case "SystemConfig" -> "SELECT ConfigKey AS id, COALESCE(NULLIF([Description], N''),ConfigKey) AS name FROM SystemConfig WHERE ConfigKey IN (:ids)";
            default -> null;
        };
    }
}
