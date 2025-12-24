package com.example.mybatis.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import java.time.LocalDate;
/**
 * 会员数据传输对象
 * @author yihui
 */
@Data
@Schema(description = "会员数据传输对象")
public class MemberDTO {
    @Schema(description = "会员ID")
    private Long id;

    @Schema(description = "关联用户ID")
    private Long userId;

    @NotBlank(message = "会员姓名不能为空")
    @Schema(description = "会员姓名")
    private String memberName;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号")
    private String phone;

    @Pattern(regexp = "^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx]$",
            message = "身份证号格式不正确")
    @Schema(description = "身份证号")
    private String idCard;

    @Schema(description = "性别")
    private String gender;

    @Schema(description = "加入日期")
    private LocalDate joinDate;

    @Schema(description = "摩托车品牌")
    private String motorcycleBrand;

    @Schema(description = "摩托车型号")
    private String motorcycleModel;

    @Schema(description = "车牌号")
    private String plateNumber;

    @Schema(description = "联系地址")
    private String address;

    @Schema(description = "备注")
    private String remark;
}