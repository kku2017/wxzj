package com.wxzj.controller;

import com.wxzj.common.Result;
import com.wxzj.entity.FlowDef;
import com.wxzj.entity.FlowNode;
import com.wxzj.repository.FlowDefRepository;
import com.wxzj.repository.FlowNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workflow")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class WorkflowController {

    private final FlowDefRepository flowDefRepository;
    private final FlowNodeRepository flowNodeRepository;

    @GetMapping("/def")
    public Result<List<FlowDef>> defs() {
        return Result.ok(flowDefRepository.findAll());
    }

    @GetMapping("/def/{id}/node")
    public Result<List<FlowNode>> nodes(@PathVariable Long id) {
        return Result.ok(flowNodeRepository.findByFlowDefIdOrderByNodeNoAsc(id));
    }

    @PostMapping("/def")
    public Result<FlowDef> saveDef(@RequestBody FlowDef def) {
        return Result.ok(flowDefRepository.save(def));
    }

    @PostMapping("/node")
    public Result<FlowNode> saveNode(@RequestBody FlowNode node) {
        flowDefRepository.findById(node.getFlowDefId()).orElseThrow(
                () -> new com.wxzj.common.BizException("流程定义不存在"));
        return Result.ok(flowNodeRepository.save(node));
    }

    @DeleteMapping("/node/{id}")
    public Result<Void> deleteNode(@PathVariable Long id) {
        flowNodeRepository.deleteById(id);
        return Result.ok();
    }

    @GetMapping("/view")
    public Result<Map<String, List<FlowNode>>> viewAll() {
        java.util.LinkedHashMap<String, List<FlowNode>> map = new java.util.LinkedHashMap<>();
        for (FlowDef d : flowDefRepository.findAll()) {
            map.put(d.getCode() + "(" + d.getName() + ")", flowNodeRepository.findByFlowDefIdOrderByNodeNoAsc(d.getId()));
        }
        return Result.ok(map);
    }
}
