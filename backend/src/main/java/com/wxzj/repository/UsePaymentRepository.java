package com.wxzj.repository;

import com.wxzj.entity.UsePayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UsePaymentRepository extends JpaRepository<UsePayment, Long> {

    List<UsePayment> findByUseApplyId(Long useApplyId);
}
