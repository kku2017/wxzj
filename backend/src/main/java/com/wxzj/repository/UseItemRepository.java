package com.wxzj.repository;

import com.wxzj.entity.UseItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UseItemRepository extends JpaRepository<UseItem, Long> {

    List<UseItem> findByUseApplyIdOrderByIdAsc(Long useApplyId);

    void deleteByUseApplyId(Long useApplyId);
}
