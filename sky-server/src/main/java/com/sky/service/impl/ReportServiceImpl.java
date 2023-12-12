package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;

    /**
     * 统计指定时间区间内的营业额数据
     * @param begin
     * @param end
     * @return
     */
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {

        //1、计算日期，把开始日期到结束日期放到一个集合里面，再把这个集合的每个元素取出来中间添加“，”放入到 dataList 里面去
        List<LocalDate> dateList = new ArrayList<>();  // 用于存放begin-end范围内的每天的日期

        dateList.add(begin);

        while (!begin.equals(end)) {
            //日期计算，计算指定日期的后一天对应的日期
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        //2、查询 datalist 里面的每天的营业额数据，最后替换为字符串并加“，”

        List<Double> turnoverList = new ArrayList<>();

        for (LocalDate date : dateList) {
            //查询data 日期对应的营业额数据，营业额是指状态为“已完成”的订单金额合计（查询订单表，每个订单都含有金额字段）
            //date: LocalData ，只有年月日， order_time： LocalDataTime ，既有年月日又有时分秒
            //order_time 应该是大于当天的最小时间，小于当天的最大时间
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN); //LocalTime.MIN： '00:00' ，对应的就是零点零分
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);   //'23:59:59.999999999'

            //select sum(amount) from orders where order_time > ? and order_time < ? and status = 5 (已完成)
            Map map = new HashMap();
            map.put("beginTime", beginTime);
            map.put("endTime", endTime);
            map.put("status", Orders.COMPLETED);  // 状态 5
            Double turnover = orderMapper.sumByMap(map);
            //这里的营业额如果为0的话，实际上返回的是空，但是我们需要数据0，所以这里需要判断
            turnover =  turnover == null ? 0.0 :turnover;
            turnoverList.add(turnover);
        }

        return TurnoverReportVO
                .builder()
                .dateList(StringUtils.join(dateList,","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
    }

    /**
     * 统计指定时间区间内的用户数据
     * @param begin
     * @param end
     * @return
     */
    public UserReportVO getuserStatistics(LocalDate begin, LocalDate end) {
        //1、计算日期，把开始日期到结束日期放到一个集合里面，再把这个集合的每个元素取出来中间添加“，”放入到 dataList 里面去
        List<LocalDate> dateList = new ArrayList<>();  // 用于存放begin-end范围内的每天的日期

        dateList.add(begin);

        while (!begin.equals(end)) {
            //日期计算，计算指定日期的后一天对应的日期
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        //2、存放每天的新用户集合 select count(id) from user where create_time < ? and create_time > ?
        List<Integer> newUserList = new ArrayList<>();
        //3、存放每天的总用户集合 select count(id) from user where create_time < ?
        //写一个动态sql兼容这两种情况就可以了
        List<Integer> totalUserList = new ArrayList<>();

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map map = new HashMap<>();
            map.put("endTime", endTime);
            
            //总用户数量
            Integer totalUser = userMapper.countByMap(map);
            totalUserList.add(totalUser);

            //新增用户数量
            map.put("beginTime", beginTime);
            Integer newUser = userMapper.countByMap(map);
            newUserList.add(newUser);
        }

        return UserReportVO
                .builder()
                .dateList(StringUtils.join(dateList,","))
                .newUserList(StringUtils.join(totalUserList,","))
                .totalUserList(StringUtils.join(newUserList, ","))
                .build();
    }

    /**
     * 统计指定时间区间内的订单数据
     * @param begin
     * @param end
     * @return
     */
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        //准备日期列表
        List<LocalDate> dateList = new ArrayList<>();  // 用于存放begin-end范围内的每天的日期
        dateList.add(begin);
        while (!begin.equals(end)) {
            //日期计算，计算指定日期的后一天对应的日期
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        //存放每天的订单总数
        List<Integer> orderCountList = new ArrayList<>();
        //存放每天的有效订单总数
        List<Integer> validOrderCountList = new ArrayList<>();
        //遍历 dateList 集合，查询每天的有效订单数和订单总数
        for (LocalDate date : dateList) {
            //时间格式转换
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            //查询每天的订单总数 select count(id) from orders where order_time < ? and order_time > ?
            Integer orderCount = getOrderCount(beginTime, endTime, null);
            //查询每天的有效订单数  select sum(id) from orders where order_time < ? and order_time > ? and status = ? (Orders.COMPLETED)
            Integer validOrderCount = getOrderCount(beginTime, endTime, Orders.COMPLETED);

            //存放数据
            orderCountList.add(orderCount);
            validOrderCountList.add(validOrderCount);

        }

        //计算时间区间内的订单总数量，可以通过 for 循环遍历上面两个集合，进行累加，也可以利用 stream 流来进行累加
        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).get();

        //计算时间区间内的有效订单数量
        Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).get();

        //计算订单完成率
        Double orderCompletionRate = 0.0;
        if (totalOrderCount != 0){
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
        }

        return OrderReportVO
                .builder()
                .dateList(StringUtils.join(dateList,","))
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    /**
     * 根据条件统计订单数量
     * @param beginTime
     * @param endTime
     * @param status
     * @return
     */
    private Integer getOrderCount(LocalDateTime beginTime, LocalDateTime endTime, Integer status) {
        Map map = new HashMap();
        map.put("beginTime", beginTime);
        map.put("endTime",endTime);
        map.put("status", status);
        Integer count = orderMapper.countByMap(map);
        return count;
    }

}
