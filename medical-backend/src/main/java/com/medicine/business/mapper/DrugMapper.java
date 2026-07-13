package com.medicine.business.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface DrugMapper {
    @Select("<script>SELECT COUNT(*) FROM drug <where>"
            + "<if test='name != null and name != \"\"'>drug_name LIKE CONCAT('%', #{name}, '%')</if>"
            + "</where></script>")
    long count(@Param("name") String name);

    @Select("<script>SELECT drug_id AS drugId, drug_name AS drugName, drug_info AS drugInfo, "
            + "drug_effect AS drugEffect, drug_img AS drugImg, publisher AS drugPublisher "
            + "FROM drug <where>"
            + "<if test='name != null and name != \"\"'>drug_name LIKE CONCAT('%', #{name}, '%')</if>"
            + "</where> ORDER BY drug_id LIMIT #{offset}, #{size}</script>")
    List<Map<String, Object>> page(@Param("name") String name,
                                   @Param("offset") int offset,
                                   @Param("size") int size);

    @Select("<script>SELECT ds.drug_id AS drugId, s.sale_id AS saleId, s.sale_name AS saleName "
            + "FROM drug_sale ds JOIN sale s ON s.sale_id=ds.sale_id WHERE ds.drug_id IN "
            + "<foreach item='id' collection='drugIds' open='(' separator=',' close=')'>#{id}</foreach> "
            + "ORDER BY ds.id</script>")
    List<Map<String, Object>> findSales(@Param("drugIds") List<Long> drugIds);

    @Insert("INSERT INTO drug(drug_name, drug_info, drug_effect, drug_img, createtime, updatetime, publisher) "
            + "VALUES(#{drugName}, #{drugInfo}, #{drugEffect}, #{drugImg}, NOW(), NOW(), #{drugPublisher})")
    @Options(useGeneratedKeys = true, keyProperty = "drugId", keyColumn = "drug_id")
    int insertDrug(Map<String, Object> drug);

    @Insert("INSERT INTO drug_sale(drug_id, sale_id) VALUES(#{drugId}, #{saleId})")
    int insertSaleRelation(@Param("drugId") Long drugId, @Param("saleId") Long saleId);

    @Update("UPDATE drug SET drug_name=#{drugName}, drug_info=#{drugInfo}, drug_effect=#{drugEffect}, "
            + "drug_img=#{drugImg}, updatetime=NOW() WHERE drug_id=#{drugId}")
    int updateDrug(Map<String, Object> drug);

    @Delete("DELETE FROM drug_sale WHERE drug_id=#{drugId}")
    int deleteSaleRelations(@Param("drugId") Long drugId);

    @Delete("DELETE FROM drug WHERE drug_id=#{drugId}")
    int deleteDrug(@Param("drugId") Long drugId);
}
