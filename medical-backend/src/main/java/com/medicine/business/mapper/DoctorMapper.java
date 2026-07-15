/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.business.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface DoctorMapper {
    String FROM_SQL = " FROM doctor d JOIN account a ON a.id=d.account_id AND a.status=1 "
            + "LEFT JOIN doctor_level dl ON dl.id=d.level_id "
            + "LEFT JOIN treat_type tt ON tt.id=d.type_id ";

    @Select("<script>SELECT COUNT(*)" + FROM_SQL + "<where>"
            + "<if test='keyword != null and keyword != \"\"'>"
            + "d.name LIKE CONCAT('%', #{keyword}, '%') OR d.phone LIKE CONCAT('%', #{keyword}, '%') "
            + "OR dl.name LIKE CONCAT('%', #{keyword}, '%') OR tt.name LIKE CONCAT('%', #{keyword}, '%')"
            + "</if></where></script>")
    long count(@Param("keyword") String keyword);

    @Select("<script>SELECT d.id, d.account_id AS accountId, d.name, d.age, d.sex, d.level_id AS levelId, "
            + "d.phone AS phoneNumber, d.type_id AS typeId, dl.name AS doctorLevel, tt.name AS treatType"
            + FROM_SQL + "<where>"
            + "<if test='keyword != null and keyword != \"\"'>"
            + "d.name LIKE CONCAT('%', #{keyword}, '%') OR d.phone LIKE CONCAT('%', #{keyword}, '%') "
            + "OR dl.name LIKE CONCAT('%', #{keyword}, '%') OR tt.name LIKE CONCAT('%', #{keyword}, '%')"
            + "</if></where> ORDER BY d.id LIMIT #{offset}, #{size}</script>")
    List<Map<String, Object>> page(@Param("keyword") String keyword,
                                   @Param("offset") int offset,
                                   @Param("size") int size);

    @Select("SELECT id, name FROM doctor_level ORDER BY id")
    List<Map<String, Object>> findAllLevels();

    @Select("SELECT id, name FROM treat_type ORDER BY id")
    List<Map<String, Object>> findAllTreatTypes();

    @Select("SELECT COUNT(*) FROM account WHERE phonenumber=#{phoneNumber}")
    long countPhone(@Param("phoneNumber") String phoneNumber);

    @Select("SELECT COUNT(*) FROM account WHERE phonenumber=#{phoneNumber} AND id != #{accountId}")
    long countPhoneExcept(@Param("phoneNumber") String phoneNumber, @Param("accountId") Long accountId);

    @Select("SELECT COUNT(*) FROM account WHERE uname=#{username}")
    long countUsername(@Param("username") String username);

    @Insert("INSERT INTO account(realname, uname, pwd, phonenumber, utype, status, updatetime, createtime) "
            + "VALUES(#{realname}, #{uname}, #{pwd}, #{phoneNumber}, 'ROLE_2', 1, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insertAccount(Map<String, Object> account);

    @Insert("INSERT INTO account_role(account_id, role_id, is_primary) "
            + "SELECT #{accountId}, r.id, 1 FROM rbac_role r "
            + "WHERE r.code='DOCTOR' AND r.enabled=1 "
            + "AND NOT EXISTS (SELECT 1 FROM account_role ar "
            + "WHERE ar.account_id=#{accountId} AND ar.role_id=r.id)")
    int bindDoctorRole(@Param("accountId") Long accountId);

    @Insert("INSERT INTO doctor(name, age, sex, level_id, phone, type_id, hospital, "
            + "updatetime, createtime, account_id) "
            + "VALUES(#{name}, #{age}, #{sex}, #{levelId}, #{phoneNumber}, #{typeId}, "
            + "'青岛第一人民医院', NOW(), NOW(), #{accountId})")
    int insertDoctor(Map<String, Object> doctor);

    @Update("<script>UPDATE doctor <set>"
            + "<if test='name != null'>name=#{name},</if>"
            + "<if test='age != null'>age=#{age},</if>"
            + "<if test='sex != null'>sex=#{sex},</if>"
            + "<if test='levelId != null'>level_id=#{levelId},</if>"
            + "<if test='phoneNumber != null'>phone=#{phoneNumber},</if>"
            + "<if test='typeId != null'>type_id=#{typeId},</if>"
            + "updatetime=NOW()"
            + "</set> WHERE id=#{id}</script>")
    int updateDoctor(Map<String, Object> doctor);

    @Update("<script>UPDATE account <set>"
            + "<if test='name != null'>realname=#{name},</if>"
            + "<if test='phoneNumber != null'>phonenumber=#{phoneNumber},</if>"
            + "updatetime=NOW()"
            + "</set> WHERE id=#{accountId}</script>")
    int updateAccount(Map<String, Object> doctor);

    @Select("SELECT account_id FROM doctor WHERE id=#{id}")
    Long findAccountId(@Param("id") Long id);

    @Delete("DELETE FROM doctor WHERE id=#{id}")
    int deleteDoctor(@Param("id") Long id);

    @Delete("DELETE FROM account WHERE id=#{accountId}")
    int deleteAccount(@Param("accountId") Long accountId);

    @Update("UPDATE account a JOIN account_role ar ON ar.account_id=a.id "
            + "JOIN rbac_role r ON r.id=ar.role_id AND r.code='DOCTOR' AND r.enabled=1 "
            + "SET a.pwd=#{encodedPassword}, a.updatetime=NOW() "
            + "WHERE a.id=#{accountId} AND a.status=1")
    int resetPassword(@Param("accountId") Long accountId,
                      @Param("encodedPassword") String encodedPassword);

    @Insert("INSERT INTO password_reset_audit(account_id, operator_account_id, reset_at) "
            + "VALUES(#{accountId}, #{operatorAccountId}, NOW())")
    int insertPasswordResetAudit(@Param("accountId") Long accountId,
                                 @Param("operatorAccountId") Long operatorAccountId);
}
