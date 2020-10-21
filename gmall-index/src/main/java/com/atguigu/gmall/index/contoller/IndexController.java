package com.atguigu.gmall.index.contoller;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.service.IndexService;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class IndexController {

    @Autowired
    private IndexService indexService;

    @GetMapping
    public String toIndex(Model model){
        List<CategoryEntity> categories = this.indexService.querylvlOneCategories();
        model.addAttribute("categories",categories);
        return "index";
    }

    // 根据一级分类id查询二、三级分类
    @GetMapping("index/cates/{pid}")
    @ResponseBody
    public ResponseVo<List<CategoryEntity>> querylvlTwoCategoriesWithSub(@PathVariable("pid")Long pid){
        List<CategoryEntity> categoryEntityList = this.indexService.querylvlTwoCategoriesWithSub(pid);
        return ResponseVo.ok(categoryEntityList);
    }

    // 测试分布式锁
    @GetMapping("index/test/lock")
    @ResponseBody
    public ResponseVo<Object> testLock(){
        this.indexService.testLock();
        return ResponseVo.ok();
    }

    //测试读写锁
    @GetMapping("index/test/read")
    @ResponseBody
    public ResponseVo<Object> testRead(){
        this.indexService.testRead();
        return ResponseVo.ok("读取成功！！");
    }

    @GetMapping("index/test/write")
    @ResponseBody
    public ResponseVo<Object> testWrite(){
        this.indexService.testWrite();
        return ResponseVo.ok("写入成功！！");
    }

    // 测试CountDownLatch闭锁
    @GetMapping("index/test/countdown")
    @ResponseBody
    public ResponseVo<Object> testCountDown(){
        this.indexService.testCountDown();
        return ResponseVo.ok("一名同学离开了教室");
    }

    @GetMapping("index/test/latch")
    @ResponseBody
    public ResponseVo<Object> testLatch() throws InterruptedException {
        this.indexService.testLatch();
        return ResponseVo.ok("班长锁门");
    }

}
