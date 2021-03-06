package viroyal.com.dev.boot;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.suntiago.baseui.activity.base.AppDelegateBase;
import com.suntiago.baseui.activity.base.theMvp.model.IModel;
import com.suntiago.baseui.utils.SPUtils;
import com.suntiago.baseui.utils.ToastUtils;
import com.suntiago.baseui.utils.log.Slog;
import com.suntiago.network.network.Api;
import com.suntiago.network.network.BaseRspObserver;

import org.kymjs.kjframe.KJDB;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import startest.ys.com.poweronoff.PowerOnOffManager;
import viroyal.com.dev.NFCMonitorBaseActivity;
import viroyal.com.dev.boot.util.DateUtil;
import viroyal.com.dev.boot.util.LztekUtil;
import viroyal.com.dev.util.DeviceInfoUtil;


/**
 * @author chenjunwei
 * @desc 开关机操作
 * @date 2020-03-10
 */
public abstract class SplashBootActivity<T extends AppDelegateBase, D extends IModel> extends NFCMonitorBaseActivity<T, D> {
    private String todayEndDate;
    private String tomorrowStartDate;
    private String tomorrowEndDate;
    private PowerOnOffManager powerOnOffManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        powerOnOffManager = PowerOnOffManager.getInstance(this);
        syncOneSecond();
    }

    /**
     * 获取开关机策略
     */
    protected void getBootConfig() {
      BootRequest bootRequest = new BootRequest();
      bootRequest.IMEI = DeviceInfoUtil.getInstance(SplashBootActivity.this).getDeviceUuid();
      Subscription bootSubscription = Api.get().getApi(BootConfig.class, getBootHostApi())
          .bootApi(bootRequest)
          .subscribeOn(Schedulers.newThread())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(new BaseRspObserver<>(BootResponse.class, new Action1<BootResponse>() {
            @Override
            public void call(BootResponse rsp) {
              if (rsp.error_code == 1000) {
                chooseStrategy(rsp);
                //请求失败 无网络 数据解析失败等读取本地缓存数据
              } else if (rsp.error_code == 404 ||rsp.error_code == 1009 || rsp.error_code == 1010 || rsp.error_code == 1001 || rsp.error_code == 502 || rsp.error_code < 0){
                //本地策略
                chooseOffLineStrategy(rsp);
              } else {
                //无策略
                chooseNoStrategy(rsp);
              }
            }
          }));
      addRxSubscription(bootSubscription);

//        setTiming();
    }

    /**
     * 广州星马班牌测试开关机
     */
    private void setTiming() {
        PowerOnOffManager powerOnOffManager = PowerOnOffManager.getInstance(this);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        // time为转换格式后的字符串
        String time = dateFormat.format(new Date(System.currentTimeMillis()));
        String[] times = time.split(" ");
        String[] date = times[0].split("-");
        String[] new_time = times[1].split(":");
        int[] timeoffArray5 = {Integer.parseInt(date[0]), Integer.parseInt(date[1]), Integer.parseInt(date[2]), Integer.parseInt(new_time[0]),
                Integer.parseInt(new_time[1]) + 30 > 60 ? 60 : Integer.parseInt(new_time[1]) + 30};
        int[] timeonArray5 = {Integer.parseInt(date[0]), Integer.parseInt(date[1]), Integer.parseInt(date[2]), Integer.parseInt(new_time[0]) + 1,
                Integer.parseInt(new_time[1])};
        powerOnOffManager.setPowerOnOff(timeonArray5, timeoffArray5);
        Slog.d(TAG, "setTiming: timeoffArray5=" + Arrays.toString(timeoffArray5) + "timeonArray5=" + Arrays.toString(timeonArray5));
    }

    /**
     * 无策略，关闭定时开关机
     *
     * @param rsp
     */
    private void chooseNoStrategy(BootResponse rsp) {
        BootModel bootModel = rsp.bootModel;
        int device_type = null == bootModel ? SPUtils.getInstance(this).get("device_type", 0) : bootModel.device_type;
        switch (device_type) {
            case 0:
                //老班牌
//        setOffLineStrategyZero();

                if (android.os.Build.MODEL.equals("T2") || android.os.Build.MODEL.equals("T3") || android.os.Build.MODEL.equals("T6")) {
                    //新班牌(华瑞安)
                    resetStrategyFifteen();
                } else if (android.os.Build.MODEL.equals("rk3288")) {
                    //广州星马(班牌)
                    resetStrategyFifteenNew();
                }

                break;
            case 4:
                //大屏无关闭重置定时开关机，让其执行离线策略
                setOffLineStrategyFour();
                break;
            case 15:
                //教师考勤
                resetStrategyFifteenOld();
                break;
            case 16:
                //会议室
                resetStrategyFifteenOld();
                break;
            case 17:
                //大屏无关闭重置定时开关机，让其执行离线策略
                setOffLineStrategyFour();
                break;
        }
    }

    //清除定时开机策略
    private void resetStrategyFifteenNew() {
        powerOnOffManager.clearPowerOnOffTime();
    }

    /**
     * 选择离线模式
     *
     * @param rsp
     */
    private void chooseOffLineStrategy(BootResponse rsp) {
        BootModel bootModel = rsp.bootModel;
        int device_type = null == bootModel ? SPUtils.getInstance(this).get("device_type", 0) : bootModel.device_type;
        switch (device_type) {
            case 0:
                //老班牌
//        setOffLineStrategyZero();

                if (android.os.Build.MODEL.equals("T2") || android.os.Build.MODEL.equals("T3") || android.os.Build.MODEL.equals("T6")) {
                    //新班牌(华瑞安)
                    setOffLineStrategyFifteen();
                } else if (android.os.Build.MODEL.equals("rk3288")) {
                    //广州星马(班牌)
                    setOffLineStrategyFifteenNew();
                }

                break;
            case 4:
                //大屏
                setOffLineStrategyFour();
                break;
            case 15:
                //教师考勤
                setOffLineStrategyFifteenOld();
                break;
            case 16:
                //会议室
                setOffLineStrategyFifteenOld();
                break;
            case 17:
                //校园超脑屏幕
                setOffLineStrategyFour();
                break;
        }
    }

    //广州星马(班牌)
    private void setOffLineStrategyFifteenNew() {
        setTodayOffLineStrategy();

        setTomorrowOffLineStrategyNew();
    }

    //广州星马(班牌)
    private void setTomorrowOffLineStrategyNew() {
        String nextDate = DateUtil.getNextDate() + " 00:00:00";
        String tempWhereStr = "type='2' and '" + nextDate + "' between start_date and end_date";
        String whereStr = "type='1' and '" + nextDate + "' between start_date and end_date";
        List<Strategy> tempStrategyList = KJDB.getDefaultInstance().findAllByWhere(Strategy.class, tempWhereStr);
        List<Strategy> normalStrategyList = KJDB.getDefaultInstance().findAllByWhere(Strategy.class, whereStr);
        if (null != tempStrategyList && tempStrategyList.size() > 0) {
            //临时策略
            setTomorrowOffLineStrategyFifteenNew(tempStrategyList);
            return;
        }
        if (null != normalStrategyList && normalStrategyList.size() > 0) {
            //常规策略
            setTomorrowOffLineStrategyFifteenNew(normalStrategyList);
            return;
        }
        //无匹配策略
        resetStrategyFifteenNew();
    }

    //广州星马(班牌)
    private void setTomorrowOffLineStrategyFifteenNew(List<Strategy> strategyList) {
        if (strategyList.size() > 0) {
            Strategy strategy = strategyList.get(0);
            String on_hour = "0";
            String on_minute = "0";
            String off_hour = "0";
            String off_minute = "0";
            String week = strategy.weeks;
            String[] onTime = getTime(strategy.on_time);
            if (null != onTime) {
                on_hour = TextUtils.equals(onTime[0].substring(0, 1), "0") ? onTime[0].substring(1, 2) : onTime[0].substring(0, 2);
                on_minute = TextUtils.equals(onTime[1].substring(0, 1), "0") ? onTime[1].substring(1, 2) : onTime[1].substring(0, 2);
            }
            String[] offTime = getTime(strategy.off_time);
            if (null != offTime) {
                off_hour = TextUtils.equals(offTime[0].substring(0, 1), "0") ? offTime[0].substring(1, 2) : offTime[0].substring(0, 2);
                off_minute = TextUtils.equals(offTime[1].substring(0, 1), "0") ? offTime[1].substring(1, 2) : offTime[1].substring(0, 2);
            }
            Tomorrow tomorrow = new Tomorrow();
            tomorrow.on_hour = DateUtil.toInt(on_hour);
            tomorrow.on_minute = DateUtil.toInt(on_minute);
            tomorrow.off_hour = DateUtil.toInt(off_hour);
            tomorrow.off_minute = DateUtil.toInt(off_minute);
            tomorrow.week = week;

            setStrategyFifteenNew(tomorrow);
        }
    }


    /**
     * 选择在线模式
     *
     * @param rsp
     */
    private void chooseStrategy(BootResponse rsp) {
        BootModel bootModel = rsp.bootModel;
        SPUtils.getInstance(this).put("device_type", bootModel.device_type);
        switch (bootModel.device_type) {
            case 0:
                //老班牌
//        setStrategyZero(rsp);
                if (android.os.Build.MODEL.equals("T2") || android.os.Build.MODEL.equals("T3") || android.os.Build.MODEL.equals("T6")) {
                    //新班牌(华瑞安)
                    setStrategyFifteen(rsp);
                } else if (android.os.Build.MODEL.equals("rk3288")) {
                    //广州星马(班牌)
                    setStrategyFifteenNew(rsp.bootModel.tomorrow);
                    //保存开机策略
                    if (null != bootModel.strategy) {
                        KJDB.getDefaultInstance().deleteByWhere(Strategy.class, "1==1");
                        KJDB.getDefaultInstance().save(rsp.bootModel.strategy);
                    }
                }
                break;
            case 4:
                //大屏
                setStrategyFour(rsp);
                break;
            case 15:
                //教师考勤
                setStrategyFifteenOld(rsp);
                break;
            case 16:
                //会议室
                setStrategyFifteenOld(rsp);
                break;
            case 17:
                //校园超脑屏幕
                setStrategyFour(rsp);
                break;
        }
    }

    //广州星马(班牌)
    private void setStrategyFifteenNew(Tomorrow tomorrow) {
        if (null != tomorrow) {
            int[] timeoffArray1 = new int[2];
            int[] timeonArray1 = new int[2];
            timeonArray1[0] = tomorrow.on_hour;
            timeonArray1[1] = tomorrow.on_minute;
            timeoffArray1[0] = tomorrow.off_hour;
            timeoffArray1[1] = tomorrow.off_minute;

            int[] wkdays = new int[7];
            for (int i = 1; i <= 7; i++) {
                if (tomorrow.week.contains(i + "")) {
                    wkdays[i - 1] = 1;
                } else {
                    wkdays[i - 1] = 0;
                }
            }
            Slog.d(TAG,
                    "setStrategyFifteenNew: time on_hour=" + tomorrow.on_hour + "\n" + "on_minute=" + tomorrow.on_minute + "\n" + "off_hour=" + tomorrow.off_hour + "\n"
                    + "off_minute=" + tomorrow.off_minute + "\n" + "wkdays=" + Arrays.toString(wkdays));
            ToastUtils.showToast(SplashBootActivity.this, "on_hour=" + tomorrow.on_hour + "\n" + "on_minute=" + tomorrow.on_minute + "\n" +
                    "off_hour=" + tomorrow.off_hour + "\n"
                    + "off_minute=" + tomorrow.off_minute + "\n" + "wkdays=" + Arrays.toString(wkdays));
            powerOnOffManager.setPowerOnOffWithWeekly(timeonArray1, timeoffArray1, wkdays);
        } else {
            resetStrategyFifteenNew();
        }
    }

    /*-----------------------------------------策略十五开始(广州厂商机器)------------------------------------------------*/
    private void setStrategyFifteenOld(BootResponse rsp) {
        BootModel bootModel = rsp.bootModel;
        setTodayStrategy(bootModel);
        //成功
        Tomorrow tomorrow = bootModel.tomorrow;
        if (null != tomorrow) {
            setTomorrowStrategyFifteenOld(tomorrow);
        } else {
            //说明明天无策略
            resetStrategyFifteenOld();
        }
        //保存开机策略
        if (null != bootModel.strategy) {
            KJDB.getDefaultInstance().deleteByWhere(Strategy.class, "1==1");
            KJDB.getDefaultInstance().save(rsp.bootModel.strategy);
        }
        syncOneSecondFifteenOld();
    }

    /**
     * 这个机器的策略：关机时间必须要早于开机时间，所以只能关机时间为当日关机时间，开机时间为明日的开机时间
     * 参数为时间戳 毫秒级
     * @param tomorrow
     */
    private void setTomorrowStrategyFifteenOld(Tomorrow tomorrow) {
        tomorrowStartDate = DateUtil.getNextDate() + " " + DateUtil.getTime(tomorrow.on_hour, tomorrow.on_minute);
        tomorrowEndDate = DateUtil.getNextDate() + " " + DateUtil.getTime(tomorrow.off_hour, tomorrow.off_minute);
        long todayEndDateTimeStamp = DateUtil.getTimeStamp(todayEndDate);
        if (!TextUtils.isEmpty(tomorrowStartDate) && !TextUtils.isEmpty(tomorrowEndDate)) {
            long tomorrowStartDateTimeStamp = DateUtil.getTimeStamp(tomorrowStartDate);
//        long tomorrowEndDateTimeStamp = DateUtil.getTimeStamp(tomorrowEndDate);
            Bundle bundle = new Bundle();
//        long closeTime = System.currentTimeMillis()+60*10*1000;//表示当前时间后10分钟关机
//        long openTime = closeTime +60*10*1000;//表示关机后，10分钟开机
            //关机时间 是当天的关机时间
            bundle.putString("timeOff", (todayEndDateTimeStamp * 1000) + "");
//        bundle.putString("timeOff",(tomorrowEndDateTimeStamp * 1000) +"");
            //开机时间
            bundle.putString("timeOn", (tomorrowStartDateTimeStamp * 1000) + "");
            //1启用，0不启用
            bundle.putString("enable", 1 + "");
            Intent intent = new Intent("com.vsservice.TIMING");
            intent.putExtras(bundle);
            sendBroadcast(intent);
        }
    }

    /**
     * 离线
     */
    private void setOffLineStrategyFifteenOld() {
        setTodayOffLineStrategy();

        setTomorrowOffLineStrategyOld();

        syncOneSecondFifteenOld();
    }


    private void setTomorrowOffLineStrategyOld() {
        String nextDate = DateUtil.getNextDate() + " 00:00:00";
        String tempWhereStr = "type='2' and '" + nextDate + "' between start_date and end_date";
        String whereStr = "type='1' and '" + nextDate + "' between start_date and end_date";
        List<Strategy> tempStrategyList = KJDB.getDefaultInstance().findAllByWhere(Strategy.class, tempWhereStr);
        List<Strategy> normalStrategyList = KJDB.getDefaultInstance().findAllByWhere(Strategy.class, whereStr);
        if (null != tempStrategyList && tempStrategyList.size() > 0) {
            //临时策略
            setTomorrowOffLineStrategyFifteenOld(tempStrategyList);
            return;
        }
        if (null != normalStrategyList && normalStrategyList.size() > 0) {
            //常规策略
            setTomorrowOffLineStrategyFifteenOld(normalStrategyList);
            return;
        }
        //无匹配策略
        resetStrategyFifteenOld();
    }

    private void setTomorrowOffLineStrategyFifteenOld(List<Strategy> strategyList) {
        if (strategyList.size() > 0) {
            Strategy strategy = strategyList.get(0);
            String on_hour = "0";
            String on_minute = "0";
            String off_hour = "0";
            String off_minute = "0";
            String week = DateUtil.getWeek(strategy.weeks);
            String[] onTime = getTime(strategy.on_time);
            if (null != onTime) {
                on_hour = TextUtils.equals(onTime[0].substring(0, 1), "0") ? onTime[0].substring(1, 2) : onTime[0].substring(0, 2);
                on_minute = TextUtils.equals(onTime[1].substring(0, 1), "0") ? onTime[1].substring(1, 2) : onTime[1].substring(0, 2);
            }
            String[] offTime = getTime(strategy.off_time);
            if (null != offTime) {
                off_hour = TextUtils.equals(offTime[0].substring(0, 1), "0") ? offTime[0].substring(1, 2) : offTime[0].substring(0, 2);
                off_minute = TextUtils.equals(offTime[1].substring(0, 1), "0") ? offTime[1].substring(1, 2) : offTime[1].substring(0, 2);
            }
            Tomorrow tomorrow = new Tomorrow();
            tomorrow.on_hour = DateUtil.toInt(on_hour);
            tomorrow.on_minute = DateUtil.toInt(on_minute);
            tomorrow.off_hour = DateUtil.toInt(off_hour);
            tomorrow.off_minute = DateUtil.toInt(off_minute);
            tomorrow.week = week;

            setTomorrowStrategyFifteenOld(tomorrow);
        }
    }

    /**
     * 重置策略
     */
    private void resetStrategyFifteenOld() {
        Bundle bundle = new Bundle();
//        long closeTime = System.currentTimeMillis()+60*10*1000;//表示当前时间后10分钟关机
//        long openTime = closeTime +60*10*1000;//表示关机后，10分钟开机
        //关机时间
        bundle.putString("timeOff", 0 + "");
        //开机时间
        bundle.putString("timeOn", 0 + "");
        //1启用，0不启用
        bundle.putString("enable", 0 + "");
        Intent intent = new Intent("com.vsservice.TIMING");
        intent.putExtras(bundle);
        sendBroadcast(intent);
    }


    /**
     * 时间检测 定时器 1m一次
     */
    private void syncOneSecondFifteenOld() {
        Subscription syncOneSecondFour = Observable.timer(1, TimeUnit.MINUTES)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aLong -> {
                    if (TextUtils.isEmpty(todayEndDate)) {
                        syncOneSecondFifteenOld();
                        return;
                    }
                    long todayEndDateTimeStamp = DateUtil.getTimeStamp(todayEndDate);
                    if (System.currentTimeMillis() / 1000 >= todayEndDateTimeStamp) {
                        shutdownStrategyOld();
                    } else {
                        //时间未到
                        syncOneSecondFifteenOld();
                    }
                });
        addRxSubscription(syncOneSecondFour);
    }

    /**
     * 关机
     */
    private void shutdownStrategyOld() {
        Intent intent = new Intent("wits.com.simahuan.shutdown");
        sendBroadcast(intent);
    }
    /*-----------------------------------------策略十五结束广州厂商机器)------------------------------------------------*/

    /*-----------------------------------------策略零开始------------------------------------------------*/

    private void setStrategyZero(BootResponse rsp) {
        BootModel bootModel = rsp.bootModel;
        setTodayStrategy(bootModel);
        if (null == bootModel.tomorrow) {
            resetStrategyZero();
        } else {
            //当天和明天的策略一致
            bootModel.tomorrow.week = getWeek(bootModel.tomorrow.week);
            setTomorrowStrategyZero(bootModel.tomorrow);
        }
        //保存开机策略
        if (null != bootModel.strategy) {
            KJDB.getDefaultInstance().deleteByWhere(Strategy.class, "1==1");
            KJDB.getDefaultInstance().save(rsp.bootModel.strategy);
        }
    }

    private String getWeek(String week) {
        String weekStr = "";
        switch (week) {
            case "1":
                weekStr = "71";
                break;
            case "2":
                weekStr = "12";
                break;
            case "3":
                weekStr = "23";
                break;
            case "4":
                weekStr = "34";
                break;
            case "5":
                weekStr = "45";
                break;
            case "6":
                weekStr = "56";
                break;
            case "7":
                weekStr = "67";
                break;
        }
        return weekStr;
    }

    private void setTomorrowStrategyZero(Tomorrow tomorrow) {
        //成功
        StringBuilder stringBuilder = new StringBuilder();
        String tTitle = "{checkSwitch:true,type:0,settings:[{switch:true,";
        stringBuilder.append(tTitle);
        stringBuilder.append(getWakeupTimeStr(tomorrow.on_hour, tomorrow.on_minute));
        stringBuilder.append(",");
        stringBuilder.append(getSleepTimeStr(tomorrow.off_hour, tomorrow.off_minute));
        stringBuilder.append(",");
        stringBuilder.append(getWeekRepeatStr(tomorrow.week));
        stringBuilder.append("}]}");


        Intent powerOnOffTimerIntent = new Intent("com.zhsd.setting.POWER_ON_OFF_TIMER");
        powerOnOffTimerIntent.putExtra("data", stringBuilder.toString());
        powerOnOffTimerIntent.putExtra("owner", "0");
        sendBroadcast(powerOnOffTimerIntent);
    }

    private String getWakeupTimeStr(int startH, int startM) {
        return "wakeupTime:\"" + String.format("%02d", new Object[]{Integer.valueOf(startH)}) + ":" + String.format("%02d",
                new Object[]{Integer.valueOf(startM)}) + "\"";
    }

    private String getSleepTimeStr(int endH, int endM) {
        return "sleepTime:\"" + String.format("%02d", new Object[]{Integer.valueOf(endH)}) + ":" + String.format("%02d",
                new Object[]{Integer.valueOf(endM)}) + "\"";
    }

    private String getWeekRepeatStr(String weekdayStr) {
        String ret = "week:[";
        StringBuilder stringBuilder = new StringBuilder(ret);
        if (TextUtils.isEmpty(weekdayStr)) {
            stringBuilder.append("]");
            return stringBuilder.toString();
        }
        if (weekdayStr.contains("1")) {
            stringBuilder.append("\"monday\",");
        }

        if (weekdayStr.contains("2")) {
            stringBuilder.append("\"tuesday\",");
        }

        if (weekdayStr.contains("3")) {
            stringBuilder.append("\"wednesday\",");
        }

        if (weekdayStr.contains("4")) {
            stringBuilder.append("\"thursday\",");
        }

        if (weekdayStr.contains("5")) {
            stringBuilder.append("\"friday\",");
        }

        if (weekdayStr.contains("6")) {
            stringBuilder.append("\"saturday\",");
        }

        if (weekdayStr.contains("7")) {
            stringBuilder.append("\"sunday\",");
        }

        if (stringBuilder.toString().endsWith(",")) {
            stringBuilder.replace(stringBuilder.toString().length() - 1, stringBuilder.toString().length(), "");
        }

        stringBuilder.append("]");

        return stringBuilder.toString();
    }

    /**
     * 离线设置策略15
     * 临时>常规
     */
    private void setOffLineStrategyZero() {
        setTodayOffLineStrategy();

        setTomorrowOffLineStrategyZero();
    }

    private void setTomorrowOffLineStrategyZero() {
        String nextDate = DateUtil.getNextDate() + " 00:00:00";
        String tempWhereStr = "type='2' and '" + nextDate + "' between start_date and end_date";
        String whereStr = "type='1' and '" + nextDate + "' between start_date and end_date";
        List<Strategy> tempStrategyList = KJDB.getDefaultInstance().findAllByWhere(Strategy.class, tempWhereStr);
        List<Strategy> normalStrategyList = KJDB.getDefaultInstance().findAllByWhere(Strategy.class, whereStr);
        if (null != tempStrategyList && tempStrategyList.size() > 0) {
            //临时策略
            setTomorrowOffLineStrategyZero(tempStrategyList);
            return;
        }
        if (null != normalStrategyList && normalStrategyList.size() > 0) {
            //常规策略
            setTomorrowOffLineStrategyZero(normalStrategyList);
            return;
        }
        //无匹配策略
        resetStrategyZero();
    }

    private void setTomorrowOffLineStrategyZero(List<Strategy> strategyList) {
        if (strategyList.size() > 0) {
            Strategy strategy = strategyList.get(0);
            String on_hour = "0";
            String on_minute = "0";
            String off_hour = "0";
            String off_minute = "0";
            String[] onTime = getTime(strategy.on_time);
            if (null != onTime) {
                on_hour = TextUtils.equals(onTime[0].substring(0, 1), "0") ? onTime[0].substring(1, 2) : onTime[0].substring(0, 2);
                on_minute = TextUtils.equals(onTime[1].substring(0, 1), "0") ? onTime[1].substring(1, 2) : onTime[1].substring(0, 2);
            }
            String[] offTime = getTime(strategy.off_time);
            if (null != offTime) {
                off_hour = TextUtils.equals(offTime[0].substring(0, 1), "0") ? offTime[0].substring(1, 2) : offTime[0].substring(0, 2);
                off_minute = TextUtils.equals(offTime[1].substring(0, 1), "0") ? offTime[1].substring(1, 2) : offTime[1].substring(0, 2);
            }
            Tomorrow tomorrow = new Tomorrow();
            tomorrow.on_hour = DateUtil.toInt(on_hour);
            tomorrow.on_minute = DateUtil.toInt(on_minute);
            tomorrow.off_hour = DateUtil.toInt(off_hour);
            tomorrow.off_minute = DateUtil.toInt(off_minute);
            tomorrow.week = strategy.weeks;

            setTomorrowStrategyZero(tomorrow);
        }
    }

    private void resetStrategyZero() {
        Intent powerOnOffTimerIntent = new Intent("com.zhsd.setting.POWER_ON_OFF_TIMER");
        powerOnOffTimerIntent.putExtra("data", "{checkSwitch:false,type:0,settings:[]}");
        powerOnOffTimerIntent.putExtra("owner", "0");
        sendBroadcast(powerOnOffTimerIntent);
    }

    /*-----------------------------------------策略零结束------------------------------------------------*/

    /*-----------------------------------------策略十五开始------------------------------------------------*/

    /**
     * 时间检测 定时器 60s一次
     */
    private void syncOneSecond() {
        Subscription syncOneSecond = Observable.timer(60, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(Long aLong) {
                        if (TextUtils.isEmpty(todayEndDate)) {
                            syncOneSecond();
                            return;
                        }
                        //关机5分钟前拉一次开关机策略
                        if (System.currentTimeMillis() / 1000 + 5 * 60 >= DateUtil.getTimeStamp(todayEndDate)) {
                            syncOneMin();
                        } else {
                            //时间未到
                            syncOneSecond();
                        }
                    }
                });
        addRxSubscription(syncOneSecond);
    }

    /**
     * 时间检测 定时器 1分钟一次
     */
    private void syncOneMin() {
        getBootConfig();
//        Subscription syncOneMin = Observable.timer(60, TimeUnit.SECONDS)
//                .subscribeOn(Schedulers.newThread())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(new Action1<Long>() {
//                    @Override
//                    public void call(Long aLong) {
//                        //关机前5分钟，每隔一分钟拉一次开关机策略
//                        if (System.currentTimeMillis() / 1000 + 5 * 60 <= DateUtil.getTimeStamp(todayEndDate)) {
//                            syncOneMin();
//                        }
//                    }
//                });
//        addRxSubscription(syncOneMin);
    }

    /**
     * 离线设置策略15
     * 临时>常规
     */
    private void setOffLineStrategyFifteen() {
        setTodayOffLineStrategy();

        setTomorrowOffLineStrategy();

        syncOneSecondFifteen();
    }

    /**
     * 获取今日关机时间
     */
    private void setTodayOffLineStrategy() {
        //获取今天的结束时间
        String todayNextDate = DateUtil.getTodayDate() + " 00:00:00";
        String todayTempWhereStr = "type='2' and '" + todayNextDate + "' between start_date and end_date";
        String todayWhereStr = "type='1' and '" + todayNextDate + "' between start_date and end_date";
        List<Strategy> todayTempStrategyList = KJDB.getDefaultInstance().findAllByWhere(Strategy.class, todayTempWhereStr);
        List<Strategy> todayNormalStrategyList = KJDB.getDefaultInstance().findAllByWhere(Strategy.class, todayWhereStr);
        if (null != todayTempStrategyList && todayTempStrategyList.size() > 0) {
            Strategy strategy = todayTempStrategyList.get(0);
            todayEndDate = DateUtil.getTodayDate() + " " + strategy.off_time.split(" ")[1];
        } else if (null != todayNormalStrategyList && todayNormalStrategyList.size() > 0) {
            //常规策略
            Strategy strategy = todayNormalStrategyList.get(0);
            todayEndDate = DateUtil.getTodayDate() + " " + strategy.off_time.split(" ")[1];
        } else {
            todayEndDate = DateUtil.getTodayDate() + " 18:00:00";
        }
    }

    /**
     * 设置明日开机时间和今日关机时间
     */
    private void setTomorrowOffLineStrategy() {
        String nextDate = DateUtil.getNextDate() + " 00:00:00";
        String tempWhereStr = "type='2' and '" + nextDate + "' between start_date and end_date";
        String whereStr = "type='1' and '" + nextDate + "' between start_date and end_date";
        List<Strategy> tempStrategyList = KJDB.getDefaultInstance().findAllByWhere(Strategy.class, tempWhereStr);
        List<Strategy> normalStrategyList = KJDB.getDefaultInstance().findAllByWhere(Strategy.class, whereStr);
        if (null != tempStrategyList && tempStrategyList.size() > 0) {
            //临时策略
            setTomorrowOffLineStrategyFifteen(tempStrategyList);
            return;
        }
        if (null != normalStrategyList && normalStrategyList.size() > 0) {
            //常规策略
            setTomorrowOffLineStrategyFifteen(normalStrategyList);
            return;
        }
        //无匹配策略
        resetStrategyFifteen();
    }

    private void setStrategyFifteen(BootResponse rsp) {
        BootModel bootModel = rsp.bootModel;
        setTodayStrategy(bootModel);
        //成功
        Tomorrow tomorrow = bootModel.tomorrow;
        try {
            Slog.d(TAG, "setStrategyFifteen: tomorrow=" + tomorrow.toString());
        } catch (Exception e) {
            e.printStackTrace();
            Slog.d(TAG, "setStrategyFifteen: Exception=" + e.getMessage());
        }
        if (null != tomorrow) {
            setTomorrowStrategyFifteen(tomorrow);
        } else {
            //说明明天无策略
            resetStrategyFifteen();
        }
        //保存开机策略
        if (null != bootModel.strategy) {
            KJDB.getDefaultInstance().deleteByWhere(Strategy.class, "1==1");
            KJDB.getDefaultInstance().save(rsp.bootModel.strategy);
        }
        syncOneSecondFifteen();
    }

    private void setTodayStrategy(BootModel bootModel) {
        if (null != bootModel.today) {
            todayEndDate = DateUtil.getTodayDate() + " " + DateUtil.getTime(bootModel.today.today_off_hour, bootModel.today.today_off_minute);
        } else {
            //今天无策略 18点更新一次
            todayEndDate = DateUtil.getTodayDate() + " 18:00:00";
        }
        Slog.d(TAG, "setTodayStrategy: todayEndDate=" + todayEndDate);
    }

    private void setTomorrowOffLineStrategyFifteen(List<Strategy> strategyList) {
        if (strategyList.size() > 0) {
            Strategy strategy = strategyList.get(0);
            String on_hour = "0";
            String on_minute = "0";
            String off_hour = "0";
            String off_minute = "0";
            String week = DateUtil.getWeek(strategy.weeks);
            String[] onTime = getTime(strategy.on_time);
            if (null != onTime) {
                on_hour = TextUtils.equals(onTime[0].substring(0, 1), "0") ? onTime[0].substring(1, 2) : onTime[0].substring(0, 2);
                on_minute = TextUtils.equals(onTime[1].substring(0, 1), "0") ? onTime[1].substring(1, 2) : onTime[1].substring(0, 2);
            }
            String[] offTime = getTime(strategy.off_time);
            if (null != offTime) {
                off_hour = TextUtils.equals(offTime[0].substring(0, 1), "0") ? offTime[0].substring(1, 2) : offTime[0].substring(0, 2);
                off_minute = TextUtils.equals(offTime[1].substring(0, 1), "0") ? offTime[1].substring(1, 2) : offTime[1].substring(0, 2);
            }
            Tomorrow tomorrow = new Tomorrow();
            tomorrow.on_hour = DateUtil.toInt(on_hour);
            tomorrow.on_minute = DateUtil.toInt(on_minute);
            tomorrow.off_hour = DateUtil.toInt(off_hour);
            tomorrow.off_minute = DateUtil.toInt(off_minute);
            tomorrow.week = week;

            setTomorrowOffLineStrategyFifteen(tomorrow);
        }
    }

    /**
     * 拆分时间
     *
     * @param time
     * @return
     */
    private String[] getTime(String time) {
        try {
            return time.split(" ")[1].split(":");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 在线设置策略15
     *
     * @param tomorrow
     */
    private void setTomorrowStrategyFifteen(Tomorrow tomorrow) {
        Slog.d(TAG, "setTomorrowStrategyFifteen: tomorrow=" + tomorrow.toString());
        //false - 关闭自动开机 true - 开启自动开机
        Intent autoBootIntent = new Intent("com.hra.setAutoBoot");
        autoBootIntent.putExtra("key", true);
        sendBroadcast(autoBootIntent);
        //开机时间
        Intent bootIntent = new Intent("com.hra.setBootTime");
        bootIntent.putExtra("hourOfDay", tomorrow.on_hour);
        bootIntent.putExtra("minute", tomorrow.on_minute);
        sendBroadcast(bootIntent);

        //false - 关闭自动关机 true - 开启自动关机
        Intent autoShutdownIntent = new Intent("com.hra.setAutoShutdown");
        autoShutdownIntent.putExtra("key", true);
        sendBroadcast(autoShutdownIntent);

        //关机时间
        Intent shutdownIntent = new Intent("com.hra.setShutdownTime");
        shutdownIntent.putExtra("hourOfDay", tomorrow.off_hour);
        shutdownIntent.putExtra("minute", tomorrow.off_minute);
        sendBroadcast(shutdownIntent);

        //星期周期
        Intent bootWeekIntent = new Intent("com.hra.setBootWeek");
        bootWeekIntent.putExtra("key", DateUtil.getWeek(tomorrow.week));
        sendBroadcast(bootWeekIntent);

        Intent shutdownWeekIntent = new Intent("com.hra.setShutdownWeek");
        shutdownWeekIntent.putExtra("key", DateUtil.getWeek(tomorrow.week));
        sendBroadcast(shutdownWeekIntent);
    }


    /**
     * 离线设置策略15
     *
     * @param tomorrow
     */
    private void setTomorrowOffLineStrategyFifteen(Tomorrow tomorrow) {
        //false - 关闭自动开机 true - 开启自动开机
        Intent autoBootIntent = new Intent("com.hra.setAutoBoot");
        autoBootIntent.putExtra("key", true);
        sendBroadcast(autoBootIntent);
        //开机时间
        Intent bootIntent = new Intent("com.hra.setBootTime");
        bootIntent.putExtra("hourOfDay", tomorrow.on_hour);
        bootIntent.putExtra("minute", tomorrow.on_minute);
        sendBroadcast(bootIntent);

        //false - 关闭自动关机 true - 开启自动关机
        Intent autoShutdownIntent = new Intent("com.hra.setAutoShutdown");
        autoShutdownIntent.putExtra("key", true);
        sendBroadcast(autoShutdownIntent);

        //关机时间
        Intent shutdownIntent = new Intent("com.hra.setShutdownTime");
        shutdownIntent.putExtra("hourOfDay", tomorrow.off_hour);
        shutdownIntent.putExtra("minute", tomorrow.off_minute);
        sendBroadcast(shutdownIntent);

        //星期周期
        Intent bootWeekIntent = new Intent("com.hra.setBootWeek");
        bootWeekIntent.putExtra("key", tomorrow.week);
        sendBroadcast(bootWeekIntent);

        Intent shutdownWeekIntent = new Intent("com.hra.setShutdownWeek");
        shutdownWeekIntent.putExtra("key", tomorrow.week);
        sendBroadcast(shutdownWeekIntent);
    }

    /**
     * 重置策略
     */
    private void resetStrategyFifteen() {
        //false - 关闭自动开机 true - 开启自动开机
        Intent autoBootIntent = new Intent("com.hra.setAutoBoot");
        autoBootIntent.putExtra("key", false);
        sendBroadcast(autoBootIntent);

        //false - 关闭自动关机 true - 开启自动关机
        Intent autoShutdownIntent = new Intent("com.hra.setAutoShutdown");
        autoShutdownIntent.putExtra("key", false);
        sendBroadcast(autoShutdownIntent);

        Intent bootWeekIntent = new Intent("com.hra.setBootWeek");
        bootWeekIntent.putExtra("key", "0000000");
        sendBroadcast(bootWeekIntent);

        Intent intent = new Intent("com.hra.setShutdownWeek");
        intent.putExtra("key", "0000000");
        sendBroadcast(intent);
    }

    /**
     * 关机
     */
    private void shutdownStrategy() {
        Intent intent = new Intent("com.hra.shutdown");
        sendBroadcast(intent);
    }


    /**
     * 时间检测 定时器 1m一次
     */
    private void syncOneSecondFifteen() {
        Subscription syncOneSecondFour = Observable.timer(1, TimeUnit.MINUTES)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(Long aLong) {
                        if (TextUtils.isEmpty(todayEndDate)) {
                            syncOneSecondFifteen();
                            return;
                        }
                        long todayEndDateTimeStamp = DateUtil.getTimeStamp(todayEndDate);
                        Slog.d(TAG, "call: syncOneSecondFifteen todayEndDateTimeStamp=" + todayEndDateTimeStamp);
                        if (System.currentTimeMillis() / 1000 >= todayEndDateTimeStamp) {
                            Slog.d(TAG, "call: shutdown=" + System.currentTimeMillis() / 1000);
                            shutdownStrategy();
                        } else {
                            //时间未到
                            syncOneSecondFifteen();
                        }
                    }
                });
        addRxSubscription(syncOneSecondFour);
    }


    /*-----------------------------------------策略十五结束------------------------------------------------*/

    /*-----------------------------------------策略四开始------------------------------------------------*/

    private void setStrategyFour(BootResponse rsp) {
        BootModel bootModel = rsp.bootModel;
        setTodayStrategy(bootModel);
        setTomorrowStrategyFour(bootModel);
    }

    private void setTomorrowStrategyFour(BootModel bootModel) {
        //成功
        Tomorrow tomorrow = bootModel.tomorrow;
        if (null != tomorrow) {
            tomorrowStartDate = DateUtil.getNextDate() + " " + DateUtil.getTime(tomorrow.on_hour, tomorrow.on_minute);
        } else {
            tomorrowStartDate = "";
        }
        //保存开机策略
        if (null != bootModel.strategy) {
            KJDB.getDefaultInstance().deleteByWhere(Strategy.class, "1==1");
            KJDB.getDefaultInstance().save(bootModel.strategy);
        }

        //开启定时任务
        syncOneSecondFour();
    }

    private void setOffLineStrategyFour() {
        setTodayOffLineStrategy();
        setTomorrowOffLineStrategyFour();
    }

    private void setTomorrowOffLineStrategyFour() {
        String nextDate = DateUtil.getNextDate() + " 00:00:00";
        String tempWhereStr = "type='2' and '" + nextDate + "' between start_date and end_date";
        String whereStr = "type='1' and '" + nextDate + "' between start_date and end_date";
        List<Strategy> tempStrategyList = KJDB.getDefaultInstance().findAllByWhere(Strategy.class, tempWhereStr);
        List<Strategy> normalStrategyList = KJDB.getDefaultInstance().findAllByWhere(Strategy.class, whereStr);
        if (null != tempStrategyList && tempStrategyList.size() > 0) {
            Strategy strategy = tempStrategyList.get(0);
            tomorrowStartDate = DateUtil.getNextDate() + " " + strategy.on_time.split(" ")[1];
        } else if (null != normalStrategyList && normalStrategyList.size() > 0) {
            //常规策略
            Strategy strategy = normalStrategyList.get(0);
            tomorrowStartDate = DateUtil.getNextDate() + " " + strategy.on_time.split(" ")[1];
        } else {
            //无匹配策略
            tomorrowStartDate = "";
        }

        //开启定时任务
        syncOneSecondFour();
    }

    /**
     * 时间检测 定时器 1s一次
     */
    private void syncOneSecondFour() {
        Subscription syncOneSecondFour = Observable.timer(1, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(Long aLong) {
                        if (TextUtils.isEmpty(todayEndDate)) {
                            syncOneSecondFour();
                            return;
                        }
                        long todayEndDateTimeStamp = DateUtil.getTimeStamp(todayEndDate);
                        if (System.currentTimeMillis() / 1000 >= todayEndDateTimeStamp) {
                            if (TextUtils.isEmpty(tomorrowStartDate)) {
                                //明天无策略 直接关机
                                LztekUtil.getInstance().hardShutdown(SplashBootActivity.this);
                            } else {
                                long tomorrowStartDateTimeStamp = DateUtil.getTimeStamp(tomorrowStartDate);
                                Slog.d(TAG, "syncOneSecondFour todayEndDateTimeStamp=" + todayEndDateTimeStamp);
                                Slog.d(TAG, "syncOneSecondFour tomorrowStartDateTimeStamp=" + tomorrowStartDateTimeStamp);
                                Slog.d(TAG, "syncOneSecondFour tomorrowStartDateTimeStamp-todayEndDateTimeStamp=" + (int) (tomorrowStartDateTimeStamp - todayEndDateTimeStamp));
                                LztekUtil.getInstance().alarmPoweron((int) (Math.abs(tomorrowStartDateTimeStamp - todayEndDateTimeStamp)), SplashBootActivity.this);
                            }
                        } else {
                            //时间未到
                            syncOneSecondFour();
                        }
                    }
                });
        addRxSubscription(syncOneSecondFour);
    }

    /*-----------------------------------------策略四结束------------------------------------------------*/

    /**
     * 获取请求主接口的api
     */
    protected abstract String getBootHostApi();
}
