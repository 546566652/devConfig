package viroyal.com.dev.boot;

import com.google.gson.annotations.SerializedName;

public class Tomorrow {
  /**
   * 开机小时
   */
  @SerializedName("on_hour")
  public int on_hour;
  /**
   * 开机分钟
   */
  @SerializedName("on_minute")
  public int on_minute;
  /**
   * 关机小时
   */
  @SerializedName("off_hour")
  public int off_hour;
  /**
   * 关机分钟
   */
  @SerializedName("off_minute")
  public int off_minute;

  @SerializedName("week")
  public String week;

  @Override
  public String toString() {
    return "Tomorrow{" +
            "on_hour=" + on_hour +
            ", on_minute=" + on_minute +
            ", off_hour=" + off_hour +
            ", off_minute=" + off_minute +
            ", week='" + week + '\'' +
            '}';
  }
}
