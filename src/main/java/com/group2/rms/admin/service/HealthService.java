package com.group2.rms.admin.service;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.group2.rms.admin.exception.DatabaseHealthException;

@Service 
public class HealthService {
 private final JdbcTemplate jdbcTemplate;

    public HealthService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void checkDatabase() {
        Integer result;

        try {
            result = jdbcTemplate.queryForObject(
                    "SELECT 1", Integer.class);
        } catch (DataAccessException exception) {
            throw new DatabaseHealthException();
        }

        if (!Integer.valueOf(1).equals(result)) {
            throw new DatabaseHealthException();
        }
    }
}
