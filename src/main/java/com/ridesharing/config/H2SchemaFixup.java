package com.ridesharing.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Drops stale H2 check constraints that were generated from the old BookingStatus enum.
 * Needed because ddl-auto:update does not remove/update check constraints.
 * Only runs on the "local" profile.
 */
@Component
@Profile("local")
public class H2SchemaFixup {

    private static final Logger log = LoggerFactory.getLogger(H2SchemaFixup.class);
    private final JdbcTemplate jdbc;

    public H2SchemaFixup(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void fixBookingStatusConstraint() {
        // 1. Drop stale check constraints on BOOKINGS
        try {
            var constraints = jdbc.queryForList(
                "SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS " +
                "WHERE TABLE_NAME = 'BOOKINGS' AND CONSTRAINT_TYPE = 'CHECK'"
            );
            for (var row : constraints) {
                String name = (String) row.get("CONSTRAINT_NAME");
                log.info("Dropping check constraint on BOOKINGS: {}", name);
                jdbc.execute("ALTER TABLE bookings DROP CONSTRAINT IF EXISTS \"" + name + "\"");
            }
            log.info("H2 schema fixup complete — all check constraints on BOOKINGS dropped");
        } catch (Exception e) {
            log.warn("H2 schema fixup skipped (table may not exist yet): {}", e.getMessage());
        }

        // 2. Add loyalty_points column to USERS if missing (added in task 14)
        try {
            var cols = jdbc.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_NAME = 'USERS' AND COLUMN_NAME = 'LOYALTY_POINTS'"
            );
            if (cols.isEmpty()) {
                jdbc.execute("ALTER TABLE users ADD COLUMN loyalty_points INT DEFAULT 0 NOT NULL");
                log.info("H2 schema fixup: added loyalty_points column to USERS");
            }
        } catch (Exception e) {
            log.warn("H2 schema fixup loyalty_points skipped: {}", e.getMessage());
        }

        // 3. Add trust_score column to USERS if missing (added in task 16)
        try {
            var cols = jdbc.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_NAME = 'USERS' AND COLUMN_NAME = 'TRUST_SCORE'"
            );
            if (cols.isEmpty()) {
                jdbc.execute("ALTER TABLE users ADD COLUMN trust_score INT DEFAULT 0 NOT NULL");
                log.info("H2 schema fixup: added trust_score column to USERS");
            }
        } catch (Exception e) {
            log.warn("H2 schema fixup trust_score skipped: {}", e.getMessage());
        }

        // 4. Add tip_amount column to BOOKINGS if missing
        try {
            var cols = jdbc.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_NAME = 'BOOKINGS' AND COLUMN_NAME = 'TIP_AMOUNT'"
            );
            if (cols.isEmpty()) {
                jdbc.execute("ALTER TABLE bookings ADD COLUMN tip_amount FLOAT DEFAULT 0 NOT NULL");
                log.info("H2 schema fixup: added tip_amount column to BOOKINGS");
            }
        } catch (Exception e) {
            log.warn("H2 schema fixup tip_amount skipped: {}", e.getMessage());
        }
    }
}
