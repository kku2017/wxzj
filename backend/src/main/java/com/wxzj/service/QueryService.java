package com.wxzj.service;

import com.wxzj.entity.Community;
import com.wxzj.entity.FundAccount;
import com.wxzj.entity.FundFlow;
import com.wxzj.repository.CommunityRepository;
import com.wxzj.repository.FundAccountRepository;
import com.wxzj.repository.FundFlowRepository;
import com.wxzj.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QueryService {

    private final FundAccountRepository fundAccountRepository;
    private final FundFlowRepository fundFlowRepository;
    private final CommunityRepository communityRepository;

    public List<FundFlow> flows(Long accountId, String type, String direction,
                                LocalDateTime start, LocalDateTime end, LoginUser user) {
        return fundFlowRepository.findAll((root, q, cb) -> {
            var p = cb.conjunction();
            if (accountId != null) {
                p = cb.and(p, cb.equal(root.get("accountId"), accountId));
            }
            if (StringUtils.hasText(type)) {
                p = cb.and(p, cb.equal(root.get("type"), type));
            }
            if (StringUtils.hasText(direction)) {
                p = cb.and(p, cb.equal(root.get("direction"), direction));
            }
            if (start != null) {
                p = cb.and(p, cb.greaterThanOrEqualTo(root.get("bizTime"), start));
            }
            if (end != null) {
                p = cb.and(p, cb.lessThanOrEqualTo(root.get("bizTime"), end));
            }
            if (user != null && user.isOwner()) {
                List<Long> accIds = fundAccountRepository.findByOwnerId(user.getOwnerId())
                        .stream().map(FundAccount::getId).toList();
                if (accIds.isEmpty()) {
                    return cb.disjunction();
                }
                p = cb.and(p, root.get("accountId").in(accIds));
            }
            return p;
        }, Sort.by(Sort.Direction.DESC, "bizTime"));
    }

    public List<Map<String, Object>> statistics() {
        Map<Long, Map<String, Object>> byCommunity = new LinkedHashMap<>();
        for (Community c : communityRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("communityId", c.getId());
            row.put("communityName", c.getName());
            row.put("accountCount", 0);
            row.put("totalBalance", BigDecimal.ZERO);
            row.put("totalDeposit", BigDecimal.ZERO);
            row.put("totalUsed", BigDecimal.ZERO);
            row.put("totalRefund", BigDecimal.ZERO);
            byCommunity.put(c.getId(), row);
        }
        for (FundAccount a : fundAccountRepository.findAll()) {
            Map<String, Object> row = byCommunity.computeIfAbsent(a.getCommunityId(), k -> {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("communityId", k);
                r.put("communityName", "-");
                r.put("accountCount", 0);
                r.put("totalBalance", BigDecimal.ZERO);
                r.put("totalDeposit", BigDecimal.ZERO);
                r.put("totalUsed", BigDecimal.ZERO);
                r.put("totalRefund", BigDecimal.ZERO);
                return r;
            });
            row.put("accountCount", (Integer) row.get("accountCount") + 1);
            row.put("totalBalance", ((BigDecimal) row.get("totalBalance")).add(a.getBalance()));
            row.put("totalDeposit", ((BigDecimal) row.get("totalDeposit")).add(a.getTotalDeposit()));
            row.put("totalUsed", ((BigDecimal) row.get("totalUsed")).add(a.getTotalUsed()));
            row.put("totalRefund", ((BigDecimal) row.get("totalRefund")).add(a.getTotalRefund()));
        }
        List<Map<String, Object>> list = new ArrayList<>(byCommunity.values());
        Map<String, Object> total = new LinkedHashMap<>();
        total.put("communityId", 0L);
        total.put("communityName", "合计");
        total.put("accountCount", list.stream().mapToInt(r -> (Integer) r.get("accountCount")).sum());
        total.put("totalBalance", list.stream().map(r -> (BigDecimal) r.get("totalBalance")).reduce(BigDecimal.ZERO, BigDecimal::add));
        total.put("totalDeposit", list.stream().map(r -> (BigDecimal) r.get("totalDeposit")).reduce(BigDecimal.ZERO, BigDecimal::add));
        total.put("totalUsed", list.stream().map(r -> (BigDecimal) r.get("totalUsed")).reduce(BigDecimal.ZERO, BigDecimal::add));
        total.put("totalRefund", list.stream().map(r -> (BigDecimal) r.get("totalRefund")).reduce(BigDecimal.ZERO, BigDecimal::add));
        list.add(total);
        return list;
    }
}
