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
public interface DrugMapper {
    @Select("<script>SELECT COUNT(*) FROM drug <where>"
            + "deleted_at IS NULL "
            + "<if test='name != null and name != \"\"'>AND drug_name LIKE CONCAT('%', #{name}, '%')</if>"
            + "</where></script>")
    long count(@Param("name") String name);

    @Select("<script>SELECT drug_id AS drugId, drug_name AS drugName, drug_info AS drugInfo, "
            + "drug_effect AS drugEffect, drug_img AS drugImg, publisher AS drugPublisher "
            + "FROM drug <where>"
            + "deleted_at IS NULL "
            + "<if test='name != null and name != \"\"'>AND drug_name LIKE CONCAT('%', #{name}, '%')</if>"
            + "</where> ORDER BY drug_id LIMIT #{offset}, #{size}</script>")
    List<Map<String, Object>> page(@Param("name") String name,
                                   @Param("offset") int offset,
                                   @Param("size") int size);

    @Select("<script>SELECT ds.drug_id AS drugId, s.sale_id AS saleId, s.sale_name AS saleName "
            + "FROM drug_sale ds JOIN sale s ON s.sale_id=ds.sale_id AND s.deleted_at IS NULL WHERE ds.drug_id IN "
            + "<foreach item='id' collection='drugIds' open='(' separator=',' close=')'>#{id}</foreach> "
            + "ORDER BY ds.id</script>")
    List<Map<String, Object>> findSales(@Param("drugIds") List<Long> drugIds);

    @Insert("INSERT INTO drug(drug_name, drug_info, drug_effect, drug_img, createtime, updatetime, publisher) "
            + "VALUES(#{drugName}, #{drugInfo}, #{drugEffect}, #{drugImg}, NOW(), NOW(), #{drugPublisher})")
    @Options(useGeneratedKeys = true, keyProperty = "drugId", keyColumn = "drug_id")
    int insertDrug(Map<String, Object> drug);

    @Insert("INSERT INTO drug_sale(drug_id, sale_id) VALUES(#{drugId}, #{saleId})")
    int insertSaleRelation(@Param("drugId") Long drugId, @Param("saleId") Long saleId);

    @Update("<script>UPDATE drug <set>"
            + "<if test='drugName != null'>drug_name=#{drugName},</if>"
            + "<if test='drugInfo != null'>drug_info=#{drugInfo},</if>"
            + "<if test='drugEffect != null'>drug_effect=#{drugEffect},</if>"
            + "<if test='drugImg != null'>drug_img=#{drugImg},</if>"
            + "updatetime=NOW()"
            + "</set> WHERE drug_id=#{drugId}</script>")
    int updateDrug(Map<String, Object> drug);

    @Delete("DELETE FROM drug_sale WHERE drug_id=#{drugId}")
    int deleteSaleRelations(@Param("drugId") Long drugId);

    @Update("UPDATE drug SET deleted_at=NOW(), deleted_by=#{deletedBy}, updatetime=NOW() "
            + "WHERE drug_id=#{drugId} AND deleted_at IS NULL")
    int softDeleteDrug(@Param("drugId") Long drugId, @Param("deletedBy") Long deletedBy);
}
