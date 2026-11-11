package com.ledgerline.backend.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AnalyticsService {
    private final JdbcTemplate jdbc;
    public AnalyticsService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static Timestamp monthsBack(int months) {
        return Timestamp.from(LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1).minusMonths(months - 1L)
                .atStartOfDay(ZoneOffset.UTC).toInstant());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> holder(Long userId, int months) {
        int m = Math.min(Math.max(months, 1), 12);
        String sum = "select coalesce(sum(e.amount),0) from ledger_entries e join accounts a on a.id = e.account_id "
                + "where a.owner_id = ? and e.direction = ?";
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("currentBalance", jdbc.queryForObject(
                "select coalesce(sum(balance),0) from accounts where owner_id = ?", BigDecimal.class, userId));
        out.put("totalReceived", jdbc.queryForObject(sum, BigDecimal.class, userId, "CREDIT"));
        out.put("totalSent", jdbc.queryForObject(sum, BigDecimal.class, userId, "DEBIT"));
        out.put("monthly", jdbc.queryForList(
                "select to_char(e.created_at at time zone 'UTC', 'YYYY-MM') as month, "
                + "coalesce(sum(case when e.direction = 'CREDIT' then e.amount end),0) as received, "
                + "coalesce(sum(case when e.direction = 'DEBIT' then e.amount end),0) as sent, "
                + "count(*) as entries "
                + "from ledger_entries e join accounts a on a.id = e.account_id "
                + "where a.owner_id = ? and e.created_at >= ? group by 1 order by 1", userId, monthsBack(m)));
        out.put("typeDistribution", jdbc.queryForList(
                "select t.type as type, count(*) as transactions from transactions t "
                + "where t.source_account_id in (select id from accounts where owner_id = ?) "
                + "or t.destination_account_id in (select id from accounts where owner_id = ?) "
                + "group by t.type order by t.type", userId, userId));
        return out;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> admin(int days) {
        int d = Math.min(Math.max(days, 1), 90);
        Timestamp since = Timestamp.from(LocalDate.now(ZoneOffset.UTC).minusDays(d - 1L)
                .atStartOfDay(ZoneOffset.UTC).toInstant());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("daily", jdbc.queryForList(
                "select to_char(created_at at time zone 'UTC', 'YYYY-MM-DD') as day, count(*) as transactions, "
                + "coalesce(sum(amount),0) as volume from transactions where created_at >= ? group by 1 order by 1", since));
        out.put("monthly", jdbc.queryForList(
                "select to_char(created_at at time zone 'UTC', 'YYYY-MM') as month, count(*) as transactions, "
                + "coalesce(sum(amount),0) as volume from transactions where created_at >= ? group by 1 order by 1",
                monthsBack(6)));
        out.put("byType", jdbc.queryForList(
                "select type, count(*) as transactions, coalesce(sum(amount),0) as volume "
                + "from transactions group by type order by type"));
        out.put("accountStatus", jdbc.queryForList(
                "select status, count(*) as accounts from accounts where type <> 'SYSTEM' group by status order by status"));
        out.put("usersByRole", jdbc.queryForList("select role, count(*) as users from users group by role order by role"));
        return out;
    }
}