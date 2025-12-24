package com.example.mybatis.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
/**
 * 会员实体类
 * @author yihui
 */
@Data
@TableName("member")
public class Member {
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("member_name")
    private String memberName;

    @TableField("phone")
    private String phone;

    @TableField("id_card")
    private String idCard;

    @TableField("gender")
    private String gender;

    @TableField("join_date")
    private LocalDate joinDate;

    @TableField("motorcycle_brand")
    private String motorcycleBrand;

    @TableField("motorcycle_model")
    private String motorcycleModel;

    @TableField("plate_number")
    private String plateNumber;

    @TableField("address")
    private String address;

    @TableField("remark")
    private String remark;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}