package com.medicine.business.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface SaleMapper {
    @Select("<script>SELECT COUNT(*) FROM sale <where>"
            + "<if test='name != null and name != \"\"'>sale_name LIKE CONCAT('%', #{name}, '%')</if>"
            + "</where></script>")
    long count(@Param("name") String name);

    @Select("<script>SELECT sale_id AS saleId, sale_name AS saleName, sale_phone AS salePhone, "
            + "address, longitude, latitude "
            + "FROM sale <where>"
            + "<if test='name != null and name != \"\"'>sale_name LIKE CONCAT('%', #{name}, '%')</if>"
            + "</where> ORDER BY sale_id LIMIT #{offset}, #{size}</script>")
    List<Map<String, Object>> page(@Param("name") String name,
                                   @Param("offset") int offset,
                                   @Param("size") int size);

    @Select("SELECT sale_id AS saleId, sale_name AS saleName, sale_phone AS salePhone, "
            + "address, longitude, latitude FROM sale ORDER BY sale_id")
    List<Map<String, Object>> findAll();

    @Insert("INSERT INTO sale(sale_name, sale_phone, address, longitude, latitude, createtime, updatetime) "
            + "VALUES(#{saleName}, #{salePhone}, #{address}, #{longitude}, #{latitude}, NOW(), NOW())")
    int insert(Map<String, Object> sale);

    @Update("UPDATE sale SET sale_name=#{saleName}, sale_phone=#{salePhone}, address=#{address}, "
            + "longitude=#{longitude}, latitude=#{latitude}, updatetime=NOW() WHERE sale_id=#{saleId}")
    int update(Map<String, Object> sale);

    @Delete("DELETE FROM drug_sale WHERE sale_id=#{saleId}")
    int deleteDrugRelations(@Param("saleId") Long saleId);

    @Delete("DELETE FROM sale WHERE sale_id=#{saleId}")
    int delete(@Param("saleId") Long saleId);
}
