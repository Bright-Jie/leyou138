package com.leyou.page.test;

import com.leyou.page.service.PageService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class PageTest {

    @Autowired
    private PageService pageService;

    @Test
    public void createStaticItemPage() {
        pageService.createStaticItemPage(81L);
    }
}