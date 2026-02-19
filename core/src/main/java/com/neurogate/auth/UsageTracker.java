package com.neurogate.auth;

import com.neurogate.exception.RateLimitException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsageTracker {

    private static final String REQUEST_DAY_KEY = "usage:request:%s:%s:%s";
    private static final String TOKEN_DAY_KEY = "usage:token:%s:%s:%s";
    private static final String COST_DAY_KEY = "usage:cost:%s:%s:%s";
    private static final String ORG_MONTH_KEY = "usage:org:%s:%s";

    private final StringRedisTemplate redisTemplate;
    private final OrganizationRepository organizationRepository;
    private final ApiUsageRecordRepository apiUsageRecordRepository;

    @Value("${neurogate.usage.plans.free-monthly-requests:10000}")
    private long freeMonthlyRequests;

    @Value("${neurogate.usage.plans.pro-monthly-requests:100000}")
    private long proMonthlyRequests;

    @Value("${neurogate.usage.plans.team-monthly-requests:500000}")
    private long teamMonthlyRequests;

    public void enforceMonthlyLimit(String orgId) {
        long current = getCurrentOrgMonthRequests(orgId);
        long limit = resolveMonthlyLimit(orgId);
        if (current >= limit) {
            Duration retryAfter = secondsUntilNextMonth();
            throw new RateLimitException(
                    "Monthly request limit exceeded for organization. Current: %d, Limit: %d".formatted(current, limit),
                    retryAfter);
        }
    }

    public void trackRequest(UUID apiKeyId, String orgId) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        String month = YearMonth.from(today).toString();

        String requestKey = requestDayKey(orgId, apiKeyId, today);
        redisTemplate.opsForValue().increment(requestKey, 1);
        redisTemplate.expire(requestKey, Duration.ofDays(45));

        String orgMonthKey = orgMonthKey(orgId, month);
        redisTemplate.opsForValue().increment(orgMonthKey, 1);
        redisTemplate.expire(orgMonthKey, Duration.ofDays(45));
    }

    public void trackTokenAndCost(UUID apiKeyId, String orgId, int tokenCount, BigDecimal costUsd) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        String tokenKey = tokenDayKey(orgId, apiKeyId, today);
        String costKey = costDayKey(orgId, apiKeyId, today);

        if (tokenCount > 0) {
            redisTemplate.opsForValue().increment(tokenKey, tokenCount);
            redisTemplate.expire(tokenKey, Duration.ofDays(45));
        }

        if (costUsd != null && costUsd.compareTo(BigDecimal.ZERO) > 0) {
            redisTemplate.opsForValue().increment(costKey, costUsd.doubleValue());
            redisTemplate.expire(costKey, Duration.ofDays(45));
        }
    }

    public CurrentUsage getCurrentUsage(String orgId) {
        LocalDate now = LocalDate.now(ZoneOffset.UTC);
        String month = YearMonth.from(now).toString();
        long requests = getCurrentOrgMonthRequests(orgId);
        long limit = resolveMonthlyLimit(orgId);

        return new CurrentUsage(orgId, month, requests, limit, Math.max(limit - requests, 0));
    }

    @Transactional(readOnly = true)
    public List<DailyUsage> getHistory(String orgId, LocalDate from, LocalDate to) {
        Map<LocalDate, DailyUsage> usageByDay = new HashMap<>();
        apiUsageRecordRepository.findByOrgIdAndUsageDateBetweenOrderByUsageDateAsc(orgId, from, to)
                .forEach(record -> usageByDay.put(record.getUsageDate(),
                        new DailyUsage(
                                record.getUsageDate(),
                                record.getRequestCount(),
                                record.getTokenCount(),
                                record.getCostUsd())));

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        if (!today.isBefore(from) && !today.isAfter(to)) {
            Set<String> requestKeys = redisTemplate.keys("usage:request:" + orgId + ":*:" + today);
            if (requestKeys != null && !requestKeys.isEmpty()) {
                long dayRequests = requestKeys.stream().mapToLong(this::safeLongValue).sum();
                long dayTokens = requestKeys.stream()
                        .map(key -> key.replace("usage:request:", "usage:token:"))
                        .mapToLong(this::safeLongValue)
                        .sum();
                BigDecimal dayCost = requestKeys.stream()
                        .map(key -> key.replace("usage:request:", "usage:cost:"))
                        .map(this::safeDecimalValue)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                usageByDay.put(today, new DailyUsage(today, dayRequests, dayTokens, dayCost));
            }
        }

        List<DailyUsage> dailyUsages = new ArrayList<>(usageByDay.values());
        dailyUsages.sort(Comparator.comparing(DailyUsage::date));
        return dailyUsages;
    }

    @Transactional(readOnly = true)
    public KeyUsage getKeyUsage(UUID apiKeyId, LocalDate from, LocalDate to) {
        long totalRequests = Optional.ofNullable(apiUsageRecordRepository.getRequestCountForKeyBetween(apiKeyId, from, to))
                .orElse(0L);
        List<ApiUsageRecord> history = apiUsageRecordRepository.findByApiKeyIdAndUsageDateBetweenOrderByUsageDateAsc(
                apiKeyId, from, to);
        return new KeyUsage(apiKeyId, totalRequests, history.stream()
                .map(record -> new DailyUsage(record.getUsageDate(), record.getRequestCount(), record.getTokenCount(),
                        record.getCostUsd()))
                .toList());
    }

    @Scheduled(cron = "0 10 0 * * *")
    @Transactional
    public void rollupDailyUsageToPostgres() {
        LocalDate day = LocalDate.now(ZoneOffset.UTC).minusDays(1);
        Set<String> requestKeys = redisTemplate.keys("usage:request:*:*:" + day);
        if (requestKeys == null || requestKeys.isEmpty()) {
            return;
        }

        for (String requestKey : requestKeys) {
            String[] parts = requestKey.split(":");
            if (parts.length != 5) {
                continue;
            }

            String orgId = parts[2];
            String keyIdRaw = parts[3];
            String dateRaw = parts[4];

            if (!StringUtils.hasText(orgId) || !StringUtils.hasText(keyIdRaw)) {
                continue;
            }

            LocalDate usageDate;
            UUID apiKeyId;
            try {
                apiKeyId = UUID.fromString(keyIdRaw);
                usageDate = LocalDate.parse(dateRaw);
            } catch (Exception ex) {
                continue;
            }

            long requests = safeLongValue(requestKey);
            long tokens = safeLongValue(requestKey.replace("usage:request:", "usage:token:"));
            BigDecimal cost = safeDecimalValue(requestKey.replace("usage:request:", "usage:cost:"));

            ApiUsageRecord record = apiUsageRecordRepository.findByOrgIdAndApiKeyIdAndUsageDate(orgId, apiKeyId, usageDate)
                    .orElseGet(() -> ApiUsageRecord.builder()
                            .orgId(orgId)
                            .apiKeyId(apiKeyId)
                            .usageDate(usageDate)
                            .requestCount(0L)
                            .tokenCount(0L)
                            .costUsd(BigDecimal.ZERO)
                            .build());

            record.setRequestCount(record.getRequestCount() + requests);
            record.setTokenCount(record.getTokenCount() + tokens);
            record.setCostUsd(record.getCostUsd().add(cost));
            apiUsageRecordRepository.save(record);
        }
    }

    private long getCurrentOrgMonthRequests(String orgId) {
        String month = YearMonth.now(ZoneOffset.UTC).toString();
        String value = redisTemplate.opsForValue().get(orgMonthKey(orgId, month));
        if (!StringUtils.hasText(value)) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private long resolveMonthlyLimit(String orgId) {
        Organization.Plan plan = organizationRepository.findById(orgId)
                .map(Organization::getPlan)
                .orElse(Organization.Plan.FREE);
        return switch (plan) {
            case FREE -> freeMonthlyRequests;
            case PRO -> proMonthlyRequests;
            case TEAM -> teamMonthlyRequests;
        };
    }

    private Duration secondsUntilNextMonth() {
        LocalDate now = LocalDate.now(ZoneOffset.UTC);
        LocalDate firstNextMonth = now.withDayOfMonth(1).plusMonths(1);
        long seconds = firstNextMonth.atStartOfDay().toEpochSecond(ZoneOffset.UTC)
                - now.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
        return Duration.ofSeconds(Math.max(seconds, 60));
    }

    private long safeLongValue(String key) {
        String value = redisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(value)) {
            return 0L;
        }
        try {
            return Long.parseLong(value.split("\\.")[0]);
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private BigDecimal safeDecimalValue(String key) {
        String value = redisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(value)) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    private String requestDayKey(String orgId, UUID apiKeyId, LocalDate date) {
        return REQUEST_DAY_KEY.formatted(orgId, apiKeyId, date);
    }

    private String tokenDayKey(String orgId, UUID apiKeyId, LocalDate date) {
        return TOKEN_DAY_KEY.formatted(orgId, apiKeyId, date);
    }

    private String costDayKey(String orgId, UUID apiKeyId, LocalDate date) {
        return COST_DAY_KEY.formatted(orgId, apiKeyId, date);
    }

    private String orgMonthKey(String orgId, String month) {
        return ORG_MONTH_KEY.formatted(orgId, month);
    }

    public record CurrentUsage(
            String orgId,
            String period,
            long requests,
            long requestLimit,
            long requestsRemaining) {
    }

    public record DailyUsage(
            LocalDate date,
            long requests,
            long tokens,
            BigDecimal costUsd) {
    }

    public record KeyUsage(
            UUID apiKeyId,
            long totalRequests,
            List<DailyUsage> history) {
    }
}
