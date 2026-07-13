package com.medicine.business.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface CityMapper {
    String FROM_SQL = " FROM city c "
            + "LEFT JOIN sysregion r ON r.id = c.city_number "
            + "LEFT JOIN sysregion p ON p.id = r.parent_id ";

    @Select("<script>SELECT COUNT(*)" + FROM_SQL
            + "<where><if test='name != null and name != \"\"'>"
            + "AND (r.name LIKE CONCAT('%', #{name}, '%') OR p.name LIKE CONCAT('%', #{name}, '%'))"
            + "</if></where></script>")
    long count(@Param("name") String name);

    @Select("<script>SELECT c.city_id AS cityId, c.city_number AS cityNumber, "
            + "COALESCE(p.name, '') AS province, COALESCE(r.name, CAST(c.city_number AS CHAR)) AS city"
            + FROM_SQL
            + "<where><if test='name != null and name != \"\"'>"
            + "AND (r.name LIKE CONCAT('%', #{name}, '%') OR p.name LIKE CONCAT('%', #{name}, '%'))"
            + "</if></where> ORDER BY c.city_id LIMIT #{offset}, #{size}</script>")
    List<Map<String, Object>> page(@Param("name") String name,
                                   @Param("offset") int offset,
                                   @Param("size") int size);

    @Select("SELECT c.city_id AS cityId, c.city_number AS cityNumber, "
            + "COALESCE(p.name, '') AS province, COALESCE(r.name, CAST(c.city_number AS CHAR)) AS city"
            + FROM_SQL + "ORDER BY c.city_id")
    List<Map<String, Object>> findAll();

    @Select("SELECT COUNT(*) FROM city WHERE city_number = #{cityNumber}")
    long countByNumber(@Param("cityNumber") Integer cityNumber);

    @Insert("INSERT INTO city(city_number, createtime, updatetime) VALUES(#{cityNumber}, NOW(), NOW())")
    int insert(@Param("cityNumber") Integer cityNumber);

    @Delete("DELETE FROM city WHERE city_id = #{cityId}")
    int delete(@Param("cityId") Long cityId);
}
