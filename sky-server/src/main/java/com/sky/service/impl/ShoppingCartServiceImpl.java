package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 添加购物车
     * @param shoppingCartDTO
     */
    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO) {

        // 判断当前添加的商品是否已经在购物车中存在了
        //select * from shopping_cart where user_id = ? and setmeal_id = xx
        //select * from shopping_cart where user_id = ? and dish_id = xx and dish_flavor
        ShoppingCart shopingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shopingCart);
        Long userId = BaseContext.getCurrentId();
        shopingCart.setUserId(userId);

        List<ShoppingCart> list = shoppingCartMapper.list(shopingCart);

        // 若存在，只需要进行update方法更新商品数量，
        if(list != null && list.size() > 0) {
            //查到了，把这条购物车数据获取到，把 num + 1
            //由于user_id是唯一的，再加上上面的信息限制，所以查询的结果只可能有两种：1、查不到；2、查出来唯一的一条数据
            ShoppingCart cart = list.get(0);
            cart.setNumber(cart.getNumber() + 1); //加 1 操作以后，执行update语句：update shopping_cart set number = ? where id = ?
            shoppingCartMapper.updateNumberById(cart);
        } else{
            // 如果不存在才需要在购物车表里面插入一条新的数据
            //购物车对象仍然可以使用上面的shoppingcart，但是商品的名称、价格、图片仍然需要查询，如果是套餐到套餐表里面去查询，如果是菜品到菜品表里面去查询
            //判断本次添加到购物车的是菜品还是套餐,可以通过判断它们的id 是否为空来进行判断
            Long dishId = shoppingCartDTO.getDishId();
            if(dishId != null){
                //本次添加到购物车的是菜品
                Dish dish = dishMapper.getById(dishId);
                shopingCart.setName(dish.getName());
                shopingCart.setImage(dish.getImage());
                shopingCart.setAmount(dish.getPrice());
            } else {
                //本次添加到购物车的是套餐
                Long setmealId = shoppingCartDTO.getSetmealId();
                Setmeal setmeal = setmealMapper.getById(setmealId);
                shopingCart.setName(setmeal.getName());
                shopingCart.setImage(setmeal.getImage());
                shopingCart.setAmount(setmeal.getPrice());
            }
            shopingCart.setNumber(1);
            shopingCart.setCreateTime(LocalDateTime.now());
            shoppingCartMapper.insert(shopingCart);
        }
    }

    /**
     * 查看购物车
     * @return
     */
    public List<ShoppingCart> showShoppingCart() {
        //获取当前微信用户的id
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = ShoppingCart.builder()
                .userId(userId)
                .build();
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        return list;
    }


    /**
     * 清空购物车
     */
    public void cleanShoppingCart() {
        //获取当前微信用户的id
        Long userId = BaseContext.getCurrentId();
        shoppingCartMapper.deleteByUserId(userId);
    }

    /**
     * 减少购物车商品数量
     * @param shoppingCartDTO
     */
    public void subShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        //构造一个实体对象传入mapper.list
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);

        //获取当前用户id,并传入实体对象
        Long userId = BaseContext.getCurrentId();
        shoppingCart.setUserId(userId);

        //查看购物车商品，返回的是单个商品的具体信息
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);

        if(list != null && list.size() > 0) {
            shoppingCart = list.get(0);
            Integer number = shoppingCart.getNumber();
            if(number == 1) {
                //当前商品在购物车中份数为1，直接删除当前记录
                shoppingCartMapper.deleteById(shoppingCart.getId());
            } else {
                //当前份数不为一，直接修改数量即可
                shoppingCart.setNumber(shoppingCart.getNumber() - 1);
                shoppingCartMapper.updateNumberById(shoppingCart);
            }
        }


    }
}
