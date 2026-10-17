package com.geriatric.assessment.controller;

import com.geriatric.assessment.common.Result;
import com.geriatric.assessment.entity.*;
import com.geriatric.assessment.service.AdminService;
import com.geriatric.assessment.service.ParamService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 后台管理接口（须携带管理令牌 Authorization: Bearer adminToken）。
 *
 * <p>覆盖方案"3.3 功能需求-后台管理模块"：量表配置管理、对话话术管理、
 * 语义映射词典管理、系统参数配置、用户查询与数据统计、日志查看。</p>
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final ParamService paramService;

    // ---------------- 数据统计 ----------------
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(adminService.stats());
    }

    // ---------------- 用户 ----------------
    @GetMapping("/users")
    public Result<List<SysUser>> users(@RequestParam(required = false) String name) {
        return Result.ok(adminService.users(name));
    }

    // ---------------- 量表 ----------------
    @GetMapping("/scales")
    public Result<List<Scale>> scales() {
        return Result.ok(adminService.scales());
    }

    @PostMapping("/scales")
    public Result<Scale> saveScale(@RequestBody Scale scale) {
        return Result.ok(adminService.saveScale(scale));
    }

    @GetMapping("/scales/{scaleId}/questions")
    public Result<List<ScaleQuestion>> questions(@PathVariable Long scaleId) {
        return Result.ok(adminService.questions(scaleId));
    }

    @PostMapping("/questions")
    public Result<ScaleQuestion> saveQuestion(@RequestBody ScaleQuestion q) {
        return Result.ok(adminService.saveQuestion(q));
    }

    // ---------------- 语义词典 ----------------
    @GetMapping("/dictionary")
    public Result<List<SemanticDictionary>> dictionary() {
        return Result.ok(adminService.dictionary());
    }

    @PostMapping("/dictionary")
    public Result<SemanticDictionary> saveDictionary(@RequestBody SemanticDictionary d) {
        return Result.ok(adminService.saveDictionary(d));
    }

    // ---------------- 对话话术 ----------------
    @GetMapping("/dialog")
    public Result<List<DialogConfig>> dialogs() {
        return Result.ok(adminService.dialogs());
    }

    @PostMapping("/dialog")
    public Result<DialogConfig> saveDialog(@RequestBody DialogConfig d) {
        return Result.ok(adminService.saveDialog(d));
    }

    // ---------------- 系统参数 ----------------
    @GetMapping("/params")
    public Result<List<SysParam>> params() {
        return Result.ok(adminService.params());
    }

    @PutMapping("/params/{key}")
    public Result<Void> updateParam(@PathVariable String key, @RequestBody Map<String, String> body) {
        paramService.set(key, body.get("value"));
        return Result.ok();
    }

    // ---------------- 日志 ----------------
    @GetMapping("/logs")
    public Result<List<SysLog>> logs() {
        return Result.ok(adminService.logs());
    }
}
