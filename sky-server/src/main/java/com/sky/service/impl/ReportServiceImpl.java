package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WorkspaceService workspaceService;

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

    /**
     * 销量排名前10菜品统计
     * @param begin
     * @param end
     * @return
     */
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        //需要查询 order_detail表中的 number 字段：销售的份数， 把这个number的值累加起来
        //还需要查询 orders 表中，status = 5 的订单，因为订单取消的时候我们只是更新了状态，并没有取消订单详细表的数据，所以还需要结合订单表来进行判断
        // select od.name,sum(od.number) number from order_detail, orders o where od.order_id = o.id and o.status = 5 and o.order_time > ? and o.order_time < ?
        // group by od.name
        // order by number desc
        // limit 0, 10  //limit [offset，] rows ：从offset+1行开始，检索rows行记录。
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop10(beginTime, endTime);
        //这里获得的结果 list 里面是 DTO 类型，需要将集合进行遍历，获得 DTO 里面的 name 属性然后用“，”拼接到一起，形成这个函数返回结果 VO 里面的 nameList 集合
        //  这里仍然用 stream 流来循环
        List<String> names= salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        String nameList = StringUtils.join(names, ",");
        List<Integer> numbers = salesTop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        String numberList = StringUtils.join(numbers, ",");

        return SalesTop10ReportVO
                .builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();
    }

    /**
     * 导出运行数据报表
     * @param response
     */
    public void exportBusinessData(HttpServletResponse response) {
        //1、查询数据库，获取营业数据 -- 查询最近30天数据
        LocalDate dateBegin = LocalDate.now().minusDays(30);
        LocalDate dateEnd = LocalDate.now().minusDays(1); //不算当天的，因为当天可能还没结束
        //查询概览数据
        BusinessDataVO businessDataVO = workspaceService.getBusinessData(LocalDateTime.of(dateBegin, LocalTime.MIN), LocalDateTime.of(dateBegin, LocalTime.MIN));
        //2、 通过 POI 将数据写入到 Excel 文件中
        //通过输入流读取模板文件 获得类对象.获得类加载器.从类路径下面来读取资源
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");

        try {
            //基于模板文件创建一个新的excel文件
            XSSFWorkbook excel = new XSSFWorkbook(in);
            //获取表格文件的标签页
            XSSFSheet sheet = excel.getSheetAt(0);
            //填充数据 -- 时间
            sheet.getRow(1).getCell(1).setCellValue("时间：" + dateBegin + "至" + dateEnd);

            //获得第4行
            //填充数据 -- 营业额
            XSSFRow row = sheet.getRow(3);
            row.getCell(2).setCellValue(businessDataVO.getTurnover());
            //填充数据 -- 订单完成率
            row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
            //填充数据 -- 新增用户数
            row.getCell(6).setCellValue(businessDataVO.getNewUsers());

            //获得第5行
            row = sheet.getRow(4);
            //填充数据 -- 有效订单
            row.getCell(2).setCellValue(businessDataVO.getValidOrderCount());
            //填充数据 -- 有效订单
            row.getCell(4).setCellValue(businessDataVO.getUnitPrice());

            //填充明细数据
            for (int i = 0; i < 30; i++) {
                LocalDate date = dateBegin.plusDays(i);
                //查询某一天的营业数据
                BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));
                //获得某一行
                row = sheet.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString()); // 2007-12-03
                row.getCell(2).setCellValue(businessData.getTurnover());
                row.getCell(3).setCellValue(businessData.getValidOrderCount());
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessData.getUnitPrice());
                row.getCell(6).setCellValue(businessData.getNewUsers());
            }

            //3、通过输出流将 Excel 文件下载到客户端浏览器
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);

            //关闭资源
            out.close();
            excel.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
