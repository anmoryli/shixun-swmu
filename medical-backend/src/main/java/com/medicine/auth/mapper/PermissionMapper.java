package com.medicine.auth.mapper;

import com.medicine.auth.model.PermissionRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface PermissionMapper {

    @Select("SELECT p.id, p.pid, p.name, p.path, p.component, p.level, p.title "
            + "FROM permission p JOIN role_permission rp ON rp.per_id = p.id "
            + "WHERE rp.roleName = #{roleName} ORDER BY p.id")
    List<PermissionRecord> findByRoleName(@Param("roleName") String roleName);
}
