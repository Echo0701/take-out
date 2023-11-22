package com.sky.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SetmealDishMapper {
    /**
     * 根据菜品 id 查询对应的套餐 id
     * @param dishIds
     * @return
     */
    //select setmeal id from setmeal dish where dish_id in (1, 2, 3, 4)
    //动态sql写到xml文件里去
    List<Long> getSetmealIdsByDishIds(List<Long> dishIds);

}
