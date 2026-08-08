package com.wxzj.repository;

import com.wxzj.entity.UseApply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UseApplyRepository extends JpaRepository<UseApply, Long>, JpaSpecificationExecutor<UseApply> {
}
