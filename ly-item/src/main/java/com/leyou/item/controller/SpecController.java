package com.leyou.item.controller;

import com.leyou.item.dto.SpecGroupDTO;
import com.leyou.item.entity.SpecGroup;
import com.leyou.item.entity.SpecParam;
import com.leyou.item.service.SpecService;
import com.sun.org.apache.regexp.internal.RE;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SpecController {

    @Autowired
    private SpecService specService;

    /**
     * 根据分类查询规格组
     */
    @GetMapping("/spec/groups/of/category")
    public ResponseEntity<List<SpecGroup>> findSpecGroupsByCid(@RequestParam("id") Long id){
        List<SpecGroup> specGroups = specService.findSpecGroupsByCid(id);
        return ResponseEntity.ok(specGroups);
    }

    /**
     * 查询规格参数
     */
    @GetMapping("/spec/params")
    public ResponseEntity<List<SpecParam>> fondParams(@RequestParam(value = "gid", required = false) Long gid,
                                                      @RequestParam(value = "cid", required = false) Long cid,
                                                      @RequestParam(value = "searching", required = false) Boolean searching){
        List<SpecParam> specParams = specService.fondParams(gid, cid, searching);
        return ResponseEntity.ok(specParams);
    }

    /**
     * 根据分类id查询规格组集合及其规格组对象内规格参数集合
     */
    @GetMapping("/spec/of/category")
    public ResponseEntity<List<SpecGroupDTO>> findSpecGroupDTOSByCid(@RequestParam("id") Long id){
        List<SpecGroupDTO> specGroupDTOS = specService.findSpecGroupDTOSByCid(id);
        return ResponseEntity.ok(specGroupDTOS);
    }
}