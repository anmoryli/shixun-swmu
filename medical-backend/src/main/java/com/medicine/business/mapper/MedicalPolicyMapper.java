/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.business.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface MedicalPolicyMapper {
    String FROM_SQL = " FROM medical_policy mp LEFT JOIN city c ON c.city_id=mp.city_id "
            + "LEFT JOIN sysregion r ON r.id=c.city_number LEFT JOIN sysregion p ON p.id=r.parent_id ";
    String FILTER_SQL = "<where>"
            + "<if test='id != null'>AND mp.id=#{id}</if>"
            + "<if test='cityId != null'>AND mp.city_id=#{cityId}</if>"
            + "<if test='title != null and title != \"\"'>AND mp.title LIKE CONCAT('%', #{title}, '%')</if>"
            + "<if test='updateTime != null and updateTime != \"\"'>AND mp.update_time=#{updateTime}</if>"
            + "</where>";

    @Select("<script>SELECT COUNT(*)" + FROM_SQL + FILTER_SQL + "</script>")
    long count(@Param("id") Long id,
               @Param("cityId") Long cityId,
               @Param("title") String title,
               @Param("updateTime") String updateTime);

    @Select("<script>SELECT mp.id, mp.title, mp.message, mp.city_id AS cityId, mp.update_time AS updateTime, "
            + "c.city_number AS cityNumber, COALESCE(r.name, CAST(c.city_number AS CHAR)) AS city, "
            + "COALESCE(p.name, '') AS province" + FROM_SQL + FILTER_SQL
            + " ORDER BY mp.id LIMIT #{offset}, #{size}</script>")
    List<Map<String, Object>> page(@Param("id") Long id,
                                   @Param("cityId") Long cityId,
                                   @Param("title") String title,
                                   @Param("updateTime") String updateTime,
                                   @Param("offset") int offset,
                                   @Param("size") int size);

    @Insert("INSERT INTO medical_policy(title, message, city_id, create_time, update_time) "
            + "VALUES(#{title}, #{message}, #{cityId}, #{updateTime}, #{updateTime})")
    int insert(@Param("cityId") Long cityId,
               @Param("title") String title,
               @Param("updateTime") String updateTime,
               @Param("message") String message);

    @Update("UPDATE medical_policy SET city_id=#{cityId}, title=#{title}, "
            + "message=#{message}, update_time=#{updateTime} "
            + "WHERE id=#{id}")
    int update(@Param("id") Long id,
               @Param("cityId") Long cityId,
               @Param("title") String title,
               @Param("updateTime") String updateTime,
               @Param("message") String message);

    @Delete("DELETE FROM medical_policy WHERE id=#{id}")
    int delete(@Param("id") Long id);
}
