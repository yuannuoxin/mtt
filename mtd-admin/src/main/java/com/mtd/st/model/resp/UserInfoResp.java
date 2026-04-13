package com.mtd.st.model.resp;

import com.mtd.st.model.entity.SysUser;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

import java.time.LocalDateTime;

@AutoMapper(target = SysUser.class, convertGenerate  = false)
@Data
public class UserInfoResp {

    @Parameter(description = "用户ID")
    private Long id;

    @Parameter(description = "用户名")
    private String username;

    @Parameter(description = "昵称")
    private String nickname;

    @Parameter(description = "邮箱")
    private String email;

    @Parameter(description = "手机号")
    private String phone;

    @Parameter(description = "头像")
    private String avatar;

    @Parameter(description = "状态")
    private Integer status;

    @Parameter(description = "创建时间")
    private LocalDateTime createTime;
}
