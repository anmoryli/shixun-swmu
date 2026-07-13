package com.medicine.auth.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.medicine.auth.model.Account;

public interface AccountMapper {

    @Select("SELECT id, realname, uname, pwd, phonenumber, utype FROM account WHERE uname = #{username} LIMIT 1")
    Account findByUsername(@Param("username") String username);
}
