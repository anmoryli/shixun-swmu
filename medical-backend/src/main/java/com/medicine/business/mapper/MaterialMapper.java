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
public interface MaterialMapper {
    @Select("<script>SELECT COUNT(*) FROM material <where>"
            + "deleted_at IS NULL "
            + "<if test='keyword != null and keyword != \"\"'>"
            + "AND (title LIKE CONCAT('%', #{keyword}, '%') OR message LIKE CONCAT('%', #{keyword}, '%'))"
            + "</if></where></script>")
    long count(@Param("keyword") String keyword);

    @Select("<script>SELECT id, title, message, DATE_FORMAT(update_time, '%Y-%m-%d') AS updateTime "
            + "FROM material <where>"
            + "deleted_at IS NULL "
            + "<if test='keyword != null and keyword != \"\"'>"
            + "AND (title LIKE CONCAT('%', #{keyword}, '%') OR message LIKE CONCAT('%', #{keyword}, '%'))"
            + "</if></where> ORDER BY id LIMIT #{offset}, #{size}</script>")
    List<Map<String, Object>> page(@Param("keyword") String keyword,
                                   @Param("offset") int offset,
                                   @Param("size") int size);

    @Insert("INSERT INTO material(title, message, create_time, update_time, create_by) "
            + "VALUES(#{title}, #{message}, NOW(), NOW(), #{createBy})")
    int insert(@Param("title") String title, @Param("message") String message, @Param("createBy") Long createBy);

    @Update("UPDATE material SET title=#{title}, message=#{message}, update_time=NOW(), update_by=#{updateBy} WHERE id=#{id}")
    int update(@Param("id") Long id, @Param("title") String title, @Param("message") String message,
               @Param("updateBy") Long updateBy);

    @Update("UPDATE material SET deleted_at=NOW(), deleted_by=#{deletedBy} "
            + "WHERE id=#{id} AND deleted_at IS NULL")
    int softDelete(@Param("id") Long id, @Param("deletedBy") Long deletedBy);
}
