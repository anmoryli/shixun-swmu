/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.business.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface CompanyPolicyMapper {
    String FROM_SQL = " FROM company_policy cp LEFT JOIN drugcompany dc ON dc.company_id=cp.company_id ";

    @Select("<script>SELECT COUNT(*)" + FROM_SQL + "<where>"
            + "cp.deleted_at IS NULL "
            + "<if test='keyword != null and keyword != \"\"'>"
            + "(cp.title LIKE CONCAT('%', #{keyword}, '%') OR cp.message LIKE CONCAT('%', #{keyword}, '%') "
            + "OR dc.company_name LIKE CONCAT('%', #{keyword}, '%'))"
            + "</if></where></script>")
    long count(@Param("keyword") String keyword);

    @Select("<script>SELECT cp.id, cp.title, cp.message, cp.company_id AS companyId, "
            + "DATE_FORMAT(cp.update_time, '%Y-%m-%d') AS updateTime, dc.company_name AS companyName"
            + FROM_SQL + "<where>"
            + "cp.deleted_at IS NULL "
            + "<if test='keyword != null and keyword != \"\"'>"
            + "(cp.title LIKE CONCAT('%', #{keyword}, '%') OR cp.message LIKE CONCAT('%', #{keyword}, '%') "
            + "OR dc.company_name LIKE CONCAT('%', #{keyword}, '%'))"
            + "</if></where> ORDER BY cp.id LIMIT #{offset}, #{size}</script>")
    List<Map<String, Object>> page(@Param("keyword") String keyword,
                                   @Param("offset") int offset,
                                   @Param("size") int size);

    @Insert("INSERT INTO company_policy(title, message, company_id, create_time, update_time) "
            + "VALUES(#{title}, #{message}, #{companyId}, NOW(), NOW())")
    int insert(@Param("companyId") Long companyId,
               @Param("title") String title,
               @Param("message") String message);

    @Update("UPDATE company_policy SET company_id=#{companyId}, title=#{title}, message=#{message}, update_time=NOW() "
            + "WHERE id=#{id}")
    int update(@Param("id") Long id,
               @Param("companyId") Long companyId,
               @Param("title") String title,
               @Param("message") String message);

    @Update("UPDATE company_policy SET deleted_at=NOW(), deleted_by=#{deletedBy} "
            + "WHERE id=#{id} AND deleted_at IS NULL")
    int softDelete(@Param("id") Long id, @Param("deletedBy") Long deletedBy);
}
