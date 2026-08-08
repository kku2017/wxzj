package com.wxzj.service;

import com.wxzj.common.BizException;
import com.wxzj.entity.*;
import com.wxzj.repository.*;
import com.wxzj.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BasicDataService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final CommunityRepository communityRepository;
    private final BuildingRepository buildingRepository;
    private final HouseRepository houseRepository;
    private final OwnerRepository ownerRepository;
    private final HouseOwnerRepository houseOwnerRepository;
    private final FundAccountRepository fundAccountRepository;
    private final DepositStandardRepository depositStandardRepository;

    // ---------- 小区 ----------
    public List<Community> listCommunities(String name, String status) {
        return communityRepository.findAll((root, q, cb) -> {
            var p = cb.conjunction();
            if (StringUtils.hasText(name)) {
                p = cb.and(p, cb.like(root.get("name"), "%" + name + "%"));
            }
            if (StringUtils.hasText(status)) {
                p = cb.and(p, cb.equal(root.get("status"), status));
            }
            return p;
        }, Sort.by(Sort.Direction.ASC, "id"));
    }

    public Community saveCommunity(Community c) {
        if (c.getId() == null) {
            c.setHouseCount(0);
        }
        return communityRepository.save(c);
    }

    @Transactional
    public void deleteCommunity(Long id) {
        if (buildingRepository.findByCommunityIdOrderByBuildingNo(id).isEmpty()) {
            communityRepository.deleteById(id);
        } else {
            throw new BizException("小区下存在楼栋，不能删除");
        }
    }

    // ---------- 楼栋 ----------
    public List<Building> listBuildings(Long communityId, String buildingNo) {
        return buildingRepository.findAll((root, q, cb) -> {
            var p = cb.conjunction();
            if (communityId != null) {
                p = cb.and(p, cb.equal(root.get("communityId"), communityId));
            }
            if (StringUtils.hasText(buildingNo)) {
                p = cb.and(p, cb.like(root.get("buildingNo"), "%" + buildingNo + "%"));
            }
            return p;
        }, Sort.by(Sort.Direction.ASC, "buildingNo"));
    }

    public Building saveBuilding(Building b) {
        communityRepository.findById(b.getCommunityId()).orElseThrow(() -> new BizException("小区不存在"));
        return buildingRepository.save(b);
    }

    @Transactional
    public void deleteBuilding(Long id) {
        if (houseRepository.findByBuildingIdOrderByHouseNo(id).isEmpty()) {
            buildingRepository.deleteById(id);
        } else {
            throw new BizException("楼栋下存在房屋，不能删除");
        }
    }

    // ---------- 房屋 ----------
    public List<House> listHouses(Long communityId, Long buildingId, String houseNo, LoginUser user) {
        return houseRepository.findAll((root, q, cb) -> {
            var p = cb.conjunction();
            if (communityId != null) {
                p = cb.and(p, cb.equal(root.get("communityId"), communityId));
            }
            if (buildingId != null) {
                p = cb.and(p, cb.equal(root.get("buildingId"), buildingId));
            }
            if (StringUtils.hasText(houseNo)) {
                p = cb.and(p, cb.like(root.get("houseNo"), "%" + houseNo + "%"));
            }
            if (user != null && user.isOwner()) {
                p = cb.and(p, cb.equal(root.get("ownerId"), user.getOwnerId()));
            }
            return p;
        }, Sort.by(Sort.Direction.ASC, "id"));
    }

    @Transactional
    public House saveHouse(House h) {
        if (h.getId() == null) {
            houseRepository.findByBuildingIdOrderByHouseNo(h.getBuildingId()).stream()
                    .filter(e -> e.getHouseNo().equals(h.getHouseNo()))
                    .findFirst().ifPresent(e -> {
                        throw new BizException("房号重复: " + h.getHouseNo());
                    });
        }
        Building building = buildingRepository.findById(h.getBuildingId())
                .orElseThrow(() -> new BizException("楼栋不存在"));
        h.setCommunityId(building.getCommunityId());
        House saved = houseRepository.save(h);
        if (saved.getOwnerId() != null) {
            ensureHouseOwner(saved.getId(), saved.getOwnerId(), true);
        }
        openAccountIfNeeded(saved);
        refreshCommunityHouseCount(saved.getCommunityId());
        return saved;
    }

    @Transactional
    public void deleteHouse(Long id) {
        House h = houseRepository.findById(id).orElseThrow(() -> new BizException("房屋不存在"));
        FundAccount acc = fundAccountRepository.findByHouseId(id).orElse(null);
        if (acc != null && (acc.getBalance().signum() > 0 || acc.getTotalDeposit().signum() > 0)) {
            throw new BizException("账户存在资金，不能删除");
        }
        if (acc != null) {
            fundAccountRepository.delete(acc);
        }
        houseOwnerRepository.deleteByHouseId(id);
        houseRepository.deleteById(id);
        refreshCommunityHouseCount(h.getCommunityId());
    }

    @Transactional
    public void bindOwner(Long houseId, Long ownerId) {
        House h = houseRepository.findById(houseId).orElseThrow(() -> new BizException("房屋不存在"));
        ownerRepository.findById(ownerId).orElseThrow(() -> new BizException("业主不存在"));
        h.setOwnerId(ownerId);
        houseRepository.save(h);
        ensureHouseOwner(houseId, ownerId, true);
        openAccountIfNeeded(h);
    }

    private void ensureHouseOwner(Long houseId, Long ownerId, boolean main) {
        if (!houseOwnerRepository.existsByHouseIdAndOwnerId(houseId, ownerId)) {
            HouseOwner ho = new HouseOwner();
            ho.setHouseId(houseId);
            ho.setOwnerId(ownerId);
            ho.setRelationType("OWNER");
            ho.setIsMain(main);
            houseOwnerRepository.save(ho);
        }
    }

    // ---------- 业主 ----------
    public List<Owner> listOwners(String name, String phone) {
        return ownerRepository.findAll((root, q, cb) -> {
            var p = cb.conjunction();
            if (StringUtils.hasText(name)) {
                p = cb.and(p, cb.like(root.get("name"), "%" + name + "%"));
            }
            if (StringUtils.hasText(phone)) {
                p = cb.and(p, cb.like(root.get("phone"), "%" + phone + "%"));
            }
            return p;
        }, Sort.by(Sort.Direction.ASC, "id"));
    }

    public Owner saveOwner(Owner o) {
        return ownerRepository.save(o);
    }

    // ---------- 缴存标准 ----------
    public List<DepositStandard> listStandards(Long communityId, String type, String status) {
        return depositStandardRepository.findAll((root, q, cb) -> {
            var p = cb.conjunction();
            if (communityId != null) {
                p = cb.and(p, cb.equal(root.get("communityId"), communityId));
            }
            if (StringUtils.hasText(type)) {
                p = cb.and(p, cb.equal(root.get("type"), type));
            }
            if (StringUtils.hasText(status)) {
                p = cb.and(p, cb.equal(root.get("status"), status));
            }
            return p;
        }, Sort.by(Sort.Direction.DESC, "id"));
    }

    public DepositStandard saveStandard(DepositStandard s) {
        communityRepository.findById(s.getCommunityId()).orElseThrow(() -> new BizException("小区不存在"));
        return depositStandardRepository.save(s);
    }

    // ---------- 账户 ----------
    public List<FundAccount> listAccounts(Long communityId, Long buildingId, String houseNo,
                                          Long ownerId, String accountNo, LoginUser user) {
        return fundAccountRepository.findAll((root, q, cb) -> {
            var p = cb.conjunction();
            if (communityId != null) {
                p = cb.and(p, cb.equal(root.get("communityId"), communityId));
            }
            if (StringUtils.hasText(accountNo)) {
                p = cb.and(p, cb.like(root.get("accountNo"), "%" + accountNo + "%"));
            }
            if (user != null && user.isOwner()) {
                p = cb.and(p, cb.equal(root.get("ownerId"), user.getOwnerId()));
            } else if (ownerId != null) {
                p = cb.and(p, cb.equal(root.get("ownerId"), ownerId));
            }
            return p;
        }, Sort.by(Sort.Direction.ASC, "id")).stream()
                .filter(a -> filterByHouse(a, buildingId, houseNo))
                .toList();
    }

    private boolean filterByHouse(FundAccount a, Long buildingId, String houseNo) {
        House h = houseRepository.findById(a.getHouseId()).orElse(null);
        if (h == null) {
            return false;
        }
        if (buildingId != null && !buildingId.equals(h.getBuildingId())) {
            return false;
        }
        if (StringUtils.hasText(houseNo) && !h.getHouseNo().contains(houseNo)) {
            return false;
        }
        return true;
    }

    @Transactional
    public FundAccount openAccountIfNeeded(House h) {
        if (fundAccountRepository.findByHouseId(h.getId()).isPresent()) {
            return null;
        }
        FundAccount acc = new FundAccount();
        acc.setAccountNo("WX" + h.getId() + FMT.format(LocalDateTime.now()));
        acc.setHouseId(h.getId());
        acc.setOwnerId(h.getOwnerId());
        acc.setCommunityId(h.getCommunityId());
        acc.setOpenTime(LocalDateTime.now());
        return fundAccountRepository.save(acc);
    }

    private void refreshCommunityHouseCount(Long communityId) {
        Community c = communityRepository.findById(communityId).orElse(null);
        if (c != null) {
            c.setHouseCount((int) houseRepository.countByCommunityId(communityId));
            communityRepository.save(c);
        }
    }
}
