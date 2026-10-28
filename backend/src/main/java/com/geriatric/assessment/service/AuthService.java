package com.geriatric.assessment.service;

import com.geriatric.assessment.common.BizException;
import com.geriatric.assessment.config.AppProperties;
import com.geriatric.assessment.config.SessionManager;
import com.geriatric.assessment.dto.LoginRequest;
import com.geriatric.assessment.dto.LoginResponse;
import com.geriatric.assessment.dto.UserProfileDTO;
import com.geriatric.assessment.entity.SysUser;
import com.geriatric.assessment.repository.SysUserRepository;
import com.geriatric.assessment.util.AesUtil;
import com.geriatric.assessment.util.IdCardUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * 实名登录服务。
 *
 * <p>以"姓名＋身份证号"核验登录：校验身份证号合法性、姓名一致性，
 * 自动解析出生日期/年龄/性别生成画像，并生成会话 token。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserRepository userRepository;
    private final SessionManager sessionManager;
    private final AppProperties props;

    @Transactional
    public LoginResponse login(LoginRequest req) {
        String name = req.getName().trim();
        String idCard = req.getIdCard().trim().toUpperCase(Locale.ROOT);

        // 1. 校验身份证号合法性（18位格式+校验位+出生日期）
        if (!IdCardUtil.isValid(idCard)) {
            throw new BizException(400, "身份证号格式不正确，请核对后重试");
        }

        // 2. 按身份证号密文查询是否已建档
        String encrypted = AesUtil.encrypt(idCard, props.getAesKey());
        SysUser user = userRepository.findByIdCard(encrypted).orElseGet(() -> {
            // 未建档：自动注册并解析画像
            SysUser nu = new SysUser();
            nu.setName(name);
            nu.setIdCard(encrypted);
            nu.setIdCardMask(IdCardUtil.mask(idCard));
            nu.setBirthDate(IdCardUtil.parseBirthDate(idCard));
            nu.setAge(IdCardUtil.calcAge(IdCardUtil.parseBirthDate(idCard)));
            nu.setGender(IdCardUtil.parseGender(idCard));
            return userRepository.save(nu);
        });

        // 3. 姓名一致性校验（防止输错身份信息）
        if (!user.getName().equals(name)) {
            throw new BizException(400, "姓名与身份证号不匹配，请核对后重试");
        }

        // 4. 生成会话
        sessionManager.init(props.getSessionExpireHours());
        String token = sessionManager.create(user.getId());

        return LoginResponse.builder()
                .token(token)
                .profile(buildProfile(user))
                .build();
    }

    /** 构造用户画像 */
    public UserProfileDTO buildProfile(SysUser user) {
        String genderText = user.getGender() != null && user.getGender() == 1 ? "男" : "女";
        return UserProfileDTO.builder()
                .userId(user.getId())
                .name(user.getName())
                .idCardMask(user.getIdCardMask())
                .birthDate(user.getBirthDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .age(user.getAge())
                .gender(user.getGender())
                .genderText(genderText)
                .title(buildTitle(user))
                .build();
    }

    /** 根据年龄性别生成称谓（高龄更尊重；与 AssessmentService.titleOf 保持单一口径） */
    private String buildTitle(SysUser user) {
        boolean male = user.getGender() != null && user.getGender() == 1;
        int age = user.getAge() == null ? 0 : user.getAge();
        if (age >= 80) {
            return male ? "爷爷" : "奶奶";
        } else if (age >= 70) {
            return male ? "大爷" : "大妈";
        } else if (age >= 60) {
            return male ? "叔叔" : "阿姨";
        }
        return male ? "先生" : "女士";
    }
}
