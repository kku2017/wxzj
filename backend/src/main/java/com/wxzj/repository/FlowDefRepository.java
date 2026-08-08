package com.wxzj.repository;

import com.wxzj.entity.FlowDef;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FlowDefRepository extends JpaRepository<FlowDef, Long> {

    Optional<FlowDef> findByCode(String code);
}
