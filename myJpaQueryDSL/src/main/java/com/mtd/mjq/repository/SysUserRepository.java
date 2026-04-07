package com.mtd.mjq.repository;

import com.mtd.mjq.entity.SysUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

public interface SysUserRepository extends JpaRepository<SysUser, Long>, QuerydslPredicateExecutor<SysUser> {
}
