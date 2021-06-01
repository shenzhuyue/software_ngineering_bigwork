package cn.edu.bupt.aircondition.common;

import java.io.Serializable;
import java.util.Date;
import static cn.edu.bupt.aircondition.entity.Constant.systemClock;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;


@Data
@ApiModel(value="接口返回对象", description="接口返回对象")
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final Integer SC_INTERNAL_SERVER_ERROR_500 = 500;

    public static final Integer SC_OK_200 = 200;

    @ApiModelProperty(value = "成功标志")
    private boolean success = true;

    @ApiModelProperty(value = "返回处理消息")
    private String message = "操作成功！";

    @ApiModelProperty(value = "返回代码")
    private Integer code = 0;

    @ApiModelProperty(value = "返回数据对象")
    private T result;

    @ApiModelProperty(value = "时间戳")
    private Date date ;

    public Result() {

    }

    public Result<T> success(String message) {
        this.message = message;
        this.code = SC_OK_200;
        this.success = true;
        this.date = systemClock;
        return this;
    }

    public static Result<Object> ok() {
        Result<Object> r = new Result<Object>();
        r.setSuccess(true);
        r.setCode(SC_OK_200);
        r.setMessage("成功");
        r.setDate(systemClock);
        return r;
    }

    public static Result<Object> ok(String msg) {
        Result<Object> r = new Result<Object>();
        r.setSuccess(true);
        r.setCode(SC_OK_200);
        r.setMessage(msg);
        r.setDate(systemClock);
        return r;
    }

    public static Result<Object> ok(Object data) {
        Result<Object> r = new Result<Object>();
        r.setSuccess(true);
        r.setCode(SC_OK_200);
        r.setResult(data);
        r.setDate(systemClock);
        return r;
    }

    public static Result<Object> error(String msg) {
        return error(SC_INTERNAL_SERVER_ERROR_500, msg);
    }

    public static Result<Object> error(int code, String msg) {
        Result<Object> r = new Result<Object>();
        r.setCode(code);
        r.setMessage(msg);
        r.setSuccess(false);
        r.setDate(systemClock);
        return r;
    }

    public Result<T> error500(String message) {
        this.message = message;
        this.code = SC_INTERNAL_SERVER_ERROR_500;
        this.success = false;
        this.date = systemClock;
        return this;
    }

}