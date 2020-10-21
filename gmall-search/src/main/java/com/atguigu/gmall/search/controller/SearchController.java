package com.atguigu.gmall.search.controller;

import com.atguigu.gmall.search.pojo.SearchParamVo;
import com.atguigu.gmall.search.pojo.SearchResponseVo;
import com.atguigu.gmall.search.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SearchController {
    @Autowired
    private SearchService searchService;

    @GetMapping("search")
    public String search(SearchParamVo searchParamVo, Model model){

        SearchResponseVo responseVO = searchService.search(searchParamVo);
        model.addAttribute("searchParam",searchParamVo);
        model.addAttribute("response",responseVO);
        return "search";
    }
}
