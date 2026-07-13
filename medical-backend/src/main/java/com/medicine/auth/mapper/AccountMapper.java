package com.medicine.auth.mapper;

import com.medicine.auth.model.Account;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface AccountMapper {

    @Select("SELECT id, realname, uname, pwd, phonenumber, utype FROM account WHERE uname = #{username} LIMIT 1")
    Account findByUsername(@Param("username") String username);
}
