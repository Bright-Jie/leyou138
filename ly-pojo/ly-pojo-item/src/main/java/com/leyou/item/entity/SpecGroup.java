package com.leyou.item.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("tb_spec_group")
public class SpecGroup {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long cid;

    private String name;

    private Date createTime;

    private Date updateTime;
}