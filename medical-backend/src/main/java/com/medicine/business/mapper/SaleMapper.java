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
public interface SaleMapper {
    @Select("<script>SELECT COUNT(*) FROM sale <where>"
            + "deleted_at IS NULL "
            + "<if test='name != null and name != \"\"'>AND sale_name LIKE CONCAT('%', #{name}, '%')</if>"
            + "</where></script>")
    long count(@Param("name") String name);

    @Select("<script>SELECT sale_id AS saleId, sale_name AS saleName, sale_phone AS salePhone, "
            + "address, longitude, latitude "
            + "FROM sale <where>"
            + "deleted_at IS NULL "
            + "<if test='name != null and name != \"\"'>AND sale_name LIKE CONCAT('%', #{name}, '%')</if>"
            + "</where> ORDER BY sale_id LIMIT #{offset}, #{size}</script>")
    List<Map<String, Object>> page(@Param("name") String name,
                                   @Param("offset") int offset,
                                   @Param("size") int size);

    @Select("SELECT sale_id AS saleId, sale_name AS saleName, sale_phone AS salePhone, "
            + "address, longitude, latitude FROM sale WHERE deleted_at IS NULL ORDER BY sale_id")
    List<Map<String, Object>> findAll();

    @Insert("INSERT INTO sale(sale_name, sale_phone, address, longitude, latitude, createtime, updatetime, create_by) "
            + "VALUES(#{saleName}, #{salePhone}, #{address}, #{longitude}, #{latitude}, NOW(), NOW(), #{createBy})")
    int insert(Map<String, Object> sale);

    @Update("UPDATE sale SET sale_name=#{saleName}, sale_phone=#{salePhone}, address=#{address}, "
            + "longitude=#{longitude}, latitude=#{latitude}, updatetime=NOW(), update_by=#{updateBy} WHERE sale_id=#{saleId}")
    int update(Map<String, Object> sale);

    @Update("UPDATE sale SET deleted_at=NOW(), deleted_by=#{deletedBy}, updatetime=NOW() "
            + "WHERE sale_id=#{saleId} AND deleted_at IS NULL")
    int softDelete(@Param("saleId") Long saleId, @Param("deletedBy") Long deletedBy);
}
