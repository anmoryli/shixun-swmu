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
public interface CompanyMapper {
    @Select("<script>SELECT COUNT(*) FROM drugcompany <where>"
            + "<if test='name != null and name != \"\"'>company_name LIKE CONCAT('%', #{name}, '%')</if>"
            + "</where></script>")
    long count(@Param("name") String name);

    @Select("<script>SELECT company_id AS companyId, company_name AS companyName, company_phone AS companyPhone "
            + "FROM drugcompany <where>"
            + "<if test='name != null and name != \"\"'>company_name LIKE CONCAT('%', #{name}, '%')</if>"
            + "</where> ORDER BY company_id LIMIT #{offset}, #{size}</script>")
    List<Map<String, Object>> page(@Param("name") String name,
                                   @Param("offset") int offset,
                                   @Param("size") int size);

    @Select("SELECT company_id AS companyId, company_name AS companyName, company_phone AS companyPhone "
            + "FROM drugcompany ORDER BY company_id")
    List<Map<String, Object>> findAll();

    @Insert("INSERT INTO drugcompany(company_name, company_phone, createtime, updatetime) "
            + "VALUES(#{companyName}, #{companyPhone}, NOW(), NOW())")
    int insert(@Param("companyName") String companyName, @Param("companyPhone") String companyPhone);

    @Update("UPDATE drugcompany SET company_name=#{companyName}, company_phone=#{companyPhone}, updatetime=NOW() "
            + "WHERE company_id=#{companyId}")
    int update(@Param("companyId") Long companyId,
               @Param("companyName") String companyName,
               @Param("companyPhone") String companyPhone);

    @Delete("DELETE FROM drugcompany WHERE company_id=#{companyId}")
    int delete(@Param("companyId") Long companyId);
}
