package com.geriatric.assessment.service;

import com.geriatric.assessment.entity.SysParam;
import com.geriatric.assessment.repository.SysParamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统参数服务：读写 sys_param，带进程内缓存。
 */
@Service
@RequiredArgsConstructor
public class ParamService {

    private final SysParamRepository repository;
    /** 简单缓存，后台修改参数后调用 invalidate 清缓存 */
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    /** 读取参数（带缓存），无则返回默认值 */
    public String get(String key, String defaultValue) {
        String v = cache.get(key);
        if (v != null) {
            return v;
        }
        String val = repository.findByParamKey(key).map(SysParam::getParamValue).orElse(defaultValue);
        cache.put(key, val);
        return val;
    }

    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(get(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public double getDouble(String key, double defaultValue) {
        try {
            return Double.parseDouble(get(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean getBool(String key, boolean defaultValue) {
        String v = get(key, String.valueOf(defaultValue));
        return "true".equalsIgnoreCase(v) || "1".equals(v);
    }

    /** 更新参数并清缓存 */
    public void set(String key, String value) {
        SysParam p = repository.findByParamKey(key).orElseGet(() -> {
            SysParam np = new SysParam();
            np.setParamKey(key);
            return np;
        });
        p.setParamValue(value);
        repository.save(p);
        cache.remove(key);
    }

    /** 全部参数（后台管理用） */
    public java.util.List<SysParam> listAll() {
        return repository.findAll();
    }

    /** 清空缓存 */
    public void invalidate(String key) {
        cache.remove(key);
    }
}
