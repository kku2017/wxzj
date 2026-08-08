package com.wxzj.repository;

import com.wxzj.entity.FlowNode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FlowNodeRepository extends JpaRepository<FlowNode, Long> {

    List<FlowNode> findByFlowDefIdOrderByNodeNoAsc(Long flowDefId);

    void deleteByFlowDefId(Long flowDefId);
}
