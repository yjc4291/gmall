package com.atguigu.gmall.cart.controller;

import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.CartException;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
public class CartController {

    @Autowired
    private CartService cartService;

    /**
     * 根据userId查询已勾选的购物车记录
     * @param userId
     * @return
     */
    @GetMapping("user/{userId}")
    @ResponseBody
    public ResponseVo<List<Cart>> queryCheckedCartsByUserId(@PathVariable("userId")Long userId){
        List<Cart> carts = this.cartService.queryCheckedCartsByUserId(userId);
        return ResponseVo.ok(carts);
    }

    /**
     * 新增购物车方法
     *
     */
    @GetMapping
    public String addCart(Cart cart){
        if (cart == null || cart.getSkuId() == null){
            throw new CartException("请选择加入购物车的商品！");
        }

        this.cartService.addCart(cart);
        return "redirect:http://cart.gmall.com/addCart.html?skuId=" + cart.getSkuId();
    }

    @GetMapping("addCart.html")
    public String queryCartBySkuId(@RequestParam("skuId")Long skuId, Model model) throws JsonProcessingException {
        Cart cart = this.cartService.queryCartBySkuId(skuId);
        model.addAttribute("cart",cart);
        return "addCart";

    }

    @GetMapping("cart.html")
    public String queryCartsByUserId(Model model){
        List<Cart> carts = this.cartService.queryCartsByUserId();
        model.addAttribute("carts", carts);
        return "cart";
    }

    @PostMapping("updateNum")
    @ResponseBody
    public ResponseVo<Object> updateNum(@RequestBody Cart cart){
        this.cartService.updateNum(cart);
        return ResponseVo.ok();
    }

    @PostMapping("deleteCart")
    @ResponseBody
    public ResponseVo<Object> deleteCart(@RequestParam("skuId")Long skuId){
        this.cartService.deleteCart(skuId);
        return ResponseVo.ok();
    }

    @GetMapping("test")
    @ResponseBody
    public String test(HttpServletRequest request){
        long start = System.currentTimeMillis();
        System.out.println("controller方法开始执行============");
        this.cartService.executor1();
        this.cartService.executor2();
//        future1.addCallback(t -> System.out.println("controller方法获取了future1的返回结果集 " + t),
//                ex -> System.out.println("controller方法捕获了future1的异常信息 " + ex.getMessage()) );
//        future2.addCallback(t -> System.out.println("controller方法获取了future2的返回结果集 " + t),
//                ex -> System.out.println("controller方法捕获了future2的异常信息 " + ex.getMessage()) );
//        try {
//            System.out.println(future1.get());
//            System.out.println("controller 手动打印" + future2.get());
//        } catch (Exception e) {
//            System.out.println("controller 捕获异常后的打印" + e.getMessage());
//        }
        System.out.println("controller方法执行结束+++++++++++++\t" + (System.currentTimeMillis() - start));

        return "hello test";
    }
}
