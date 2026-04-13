package com.mtd.st.model.resp;

import com.mtd.st.model.entity.SysPermission;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.util.List;

@AutoMapper(target = SysPermission.class, convertGenerate = false)
@Data
public class SysPermissionResp {
    private Long id;

    private Long parentId;
    private String permCode;
    private String permName;
    private Integer permType;
    private String path;
    private String component;
    private String icon;
    private Integer sort;
    private Integer status;

    private List<SysPermissionResp> children;
}
