package com.medicine.business.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface MaterialMapper {
    @Select("<script>SELECT COUNT(*) FROM material <where>"
            + "<if test='keyword != null and keyword != \"\"'>"
            + "title LIKE CONCAT('%', #{keyword}, '%') OR message LIKE CONCAT('%', #{keyword}, '%')"
            + "</if></where></script>")
    long count(@Param("keyword") String keyword);

    @Select("<script>SELECT id, title, message, DATE_FORMAT(update_time, '%Y-%m-%d') AS updateTime "
            + "FROM material <where>"
            + "<if test='keyword != null and keyword != \"\"'>"
            + "title LIKE CONCAT('%', #{keyword}, '%') OR message LIKE CONCAT('%', #{keyword}, '%')"
            + "</if></where> ORDER BY id LIMIT #{offset}, #{size}</script>")
    List<Map<String, Object>> page(@Param("keyword") String keyword,
                                   @Param("offset") int offset,
                                   @Param("size") int size);

    @Insert("INSERT INTO material(title, message, create_time, update_time) VALUES(#{title}, #{message}, NOW(), NOW())")
    int insert(@Param("title") String title, @Param("message") String message);

    @Update("UPDATE material SET title=#{title}, message=#{message}, update_time=NOW() WHERE id=#{id}")
    int update(@Param("id") Long id, @Param("title") String title, @Param("message") String message);

    @Delete("DELETE FROM material WHERE id=#{id}")
    int delete(@Param("id") Long id);
}
