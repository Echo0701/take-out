package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface EmployeeMapper {

    /**
     * 根据用户名查询员工
     * @param username
     * @return
     */
    @Select("select * from employee where username = #{username}")
    Employee getByUsername(String username);


    /**
     * 插入员工数据
     * @param employee
     */
    @Insert("insert into employ(name, username, password, phone, sex, id_number, create_time, update_time, create_user, update_user, status)" +
            "values" +
            "(#{name}, #{username}, #{password}, #{phone}, #{sex}, #{idNumber}, #{createTime}, #{updateTime},#{createUser},#{updateUser}, #{status})")
    void insert(Employee employee);

    /**
     * 分页查询
     * @param employeePageQueryDTO
     * @return
     */
    Page<Employee> pageQuery(EmployeePageQueryDTO employeePageQueryDTO);
    //这里涉及到动态的sql，所以需要到映射文件（src/main/resources/mapper/EmployeeMapper.xml）里面去编写 sql 语句
}
