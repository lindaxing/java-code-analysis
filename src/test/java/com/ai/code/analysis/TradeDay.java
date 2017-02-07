package com.ai.code.analysis;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TradeDay {

    /**
     * 一天当中的交易时段
     */
    private TimeRange[] tradeTimes = new TimeRange[]{
            new TimeRange(9, 30 ,11, 30), 
            new TimeRange(13, 30,15, 30)
    };
    
    private Date minDate ; // 初始化数据的最小日期
    private long minEpochDay; // 最小日期用Epoch的天数表示
    private BitSet bitSet;// 位数据，每一位映射一个日期，交易日的位置为1
    
    /**
      *  工具初始化，初始化的目的是让工具具备更加合适各的数据结构，方便计算提高效率

      *  @param  tradeDayList  包含一年内所有的交易日起，格式如：20160701  20160704  20160705，非交易日20160702  20160703不在其中.
     * @throws ParseException 
      */
    public  void  init  (List<String>  tradeDayList) throws ParseException{
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        List<Long> timeInDayList = new   ArrayList<>();
        long min=Long.MAX_VALUE, max = Long.MIN_VALUE;
        // 转化为 Epoch的天数表示 并计算出最小，最大时间
        for (String tradeDay : tradeDayList) {
            Date date = sdf.parse(tradeDay);
            long timeInDay = calcDays(date);
            System.out.println(timeInDay);
            timeInDayList.add(timeInDay);
            if(timeInDay < min){
                min = timeInDay;
                minDate = date;
            }
            if(timeInDay> max){
                max = timeInDay;
            }
        }
        this.minEpochDay = min; 
        // 初始化 BitSet
        BitSet bitSet = new BitSet((int)(max-min));
        for (Long timeInDay : timeInDayList) {
            bitSet.set((int)(timeInDay - min)); // 所有的交易日置 true
        }
        this.bitSet = bitSet;
    }

    /**
      *  
      *  给定任意时间，返回给定时间的T+n交易日
      *
      *  @param  time  给定要计算的时间。
      *  @param  offsetDays  交易日便宜量，offsetDays可以为负数，表示T-n的计算。
      */

    public  String  getTradeDay(Date  time,int  offsetDays){
        if(time == null){
            throw new IllegalArgumentException("time");//
        }
        if(time.before(minDate)){
            throw new IllegalArgumentException("time must after " + minDate);//
        }
        
        long timeInDay = calcDays(time);
        int newOffset = (int)(timeInDay - minEpochDay);
        
        // 判断时间是不是在当天的交易时间之前
        boolean isBefore = isBeforeTradeTime(time);
        if(!isBefore){
            newOffset ++;// 直接查下一个交易日即可
        }
        for (; newOffset < bitSet.size(); newOffset++) {
            if(bitSet.get(newOffset) ){
                break; // 找到当前交易日
            }
        }
        
        // 计算 T + n 交易日
        while(offsetDays !=0){
            newOffset = newOffset + (offsetDays > 0?  1 :-1);
            if(bitSet.get(newOffset)){
                offsetDays = offsetDays + (offsetDays >0? -1: 1);
            }
            if(newOffset >= bitSet.size()){
                throw new IllegalArgumentException("unknow trade date,can not calculate it.");
            }
        }

        return formatDate(newOffset);
    }

    /**
     * 计算 1970 到日期的天数
     * @param time
     * @return
     */
    private long calcDays(Date time) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(time);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        long timeInDay = calendar.getTimeInMillis() / TimeUnit.DAYS.toMillis(1);
        return timeInDay;
    }
    
    /**
     * 将计算出来的偏移值格式化为日期
     * @param offset 计算的出来偏移值
     * @return 字符串表示的日期
     */
    private String formatDate(int offset){
        Date d = new Date(minDate.getTime() + TimeUnit.DAYS.toMillis(offset));
        return new SimpleDateFormat("yyyyMMdd").format(d);
    }
    
    /**
     * 判断时间是不是在交易时间之前
     * @param time 指定的时间
     * @return 
     */
    private boolean isBeforeTradeTime(Date time){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(time);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        for (TimeRange timeRange : tradeTimes) {
            if(timeRange.isBefore(hour,minute)){
                return  true;
            }
        }
        return false;
    }
    
    /**
     * 交易时段
     */
    private static class TimeRange {
        @SuppressWarnings("unused")
        final long rangeStartMinutes; // 逻辑可以简化为只要判断在交易时段之前，
        final long rangeEndMinutes;
        
        public TimeRange(int startHour,int startMinute,int endHour,int endMinute){
            this.rangeStartMinutes = TimeUnit.HOURS.toMinutes(startHour) + startMinute;
            this.rangeEndMinutes = TimeUnit.HOURS.toMinutes(endHour) + endMinute;
        }
        
        boolean isBefore(int hour,int minute){
            // 只要判断区间结束即可
            long that = TimeUnit.HOURS.toMinutes(hour) + minute;
            return  that < rangeEndMinutes;
        }
    }
    
    public static void main(String[] args) throws ParseException {
        TradeDay tradeDay = new TradeDay();
        tradeDay.init(Arrays.asList("20160701","20160704","20160705","20160708"));
        test(tradeDay, "20160701-0800",0,"20160701");
        test(tradeDay, "20160701-1500",0,"20160701");
        test(tradeDay, "20160701-1529",0,"20160701");
        test(tradeDay, "20160701-1530",0,"20160704");
        test(tradeDay, "20160701-1700",0,"20160704");
        test(tradeDay, "20160701-1700",1,"20160705");
        test(tradeDay, "20160701-1700",-1,"20160701");
        test(tradeDay, "20160707-1700",0,"20160705");
    }

    private static void test(TradeDay tradeDay,String dateStr,int offset,String expect) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmm");
        String d = tradeDay.getTradeDay(sdf.parse(dateStr),offset);
        System.out.printf("%s,%d->%s[expect %s]\n",dateStr,offset,d,expect); // 20160705
    }
}
