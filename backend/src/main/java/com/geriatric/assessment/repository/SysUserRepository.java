package com.geriatric.assessment.repository;

import com.geriatric.assessment.entity.SysUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 用户数据访问层。
 */
public interface SysUserRepository extends JpaRepository<SysUser, Long> {

    /** 按身份证号密文查询（身份证号经AES加密后唯一索引） */
    Optional<SysUser> findByIdCard(String idCard);

    /** 模糊查询姓名（后台用户管理用） */
    List<SysUser> findByNameContaining(String name);

    /** 后台统计：各年龄段用户数 */
    @Query("select u.age, count(u) from SysUser u group by u.age order by u.age")
    List<Object[]> countByAge();

    /** 后台统计：用户总数（含姓名、脱敏号） */
    @Query("select u from SysUser u where (:name is null or u.name like concat('%',:name,'%')) order by u.createdAt desc")
    List<SysUser> search(@Param("name") String name);
}
