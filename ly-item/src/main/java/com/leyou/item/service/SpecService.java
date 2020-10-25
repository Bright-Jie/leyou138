package com.leyou.item.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.leyou.common.exception.pojo.ExceptionEnum;
import com.leyou.common.exception.pojo.LyException;
import com.leyou.common.utils.BeanHelper;
import com.leyou.item.dto.SpecGroupDTO;
import com.leyou.item.entity.SpecGroup;
import com.leyou.item.entity.SpecParam;
import com.leyou.item.mapper.SpecGroupMapper;
import com.leyou.item.mapper.SpecParamMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class SpecService {

    @Autowired
    private SpecGroupMapper groupMapper;

    @Autowired
    private SpecParamMapper paramMapper;

    public List<SpecGroup> findSpecGroupsByCid(Long id) {
        SpecGroup entity = new SpecGroup();
        entity.setCid(id);
        QueryWrapper<SpecGroup> wrapper = Wrappers.query(entity);
        List<SpecGroup> specGroups = groupMapper.selectList(wrapper);
        if(CollectionUtils.isEmpty(specGroups)){
            throw new LyException(ExceptionEnum.SPEC_NOT_FOUND);
        }
        return specGroups;
    }

    public List<SpecParam> fondParams(Long gid, Long cid, Boolean searching) {
        //限定gid和cid必须至少一个有值
        if(gid==null && cid==null){
            throw new LyException(ExceptionEnum.INVALID_PARAM_ERROR);
        }
        SpecParam entity = new SpecParam();
        entity.setCid(cid);
        entity.setGroupId(gid);
        entity.setSearching(searching);
        QueryWrapper<SpecParam> wrapper = Wrappers.query(entity);
        List<SpecParam> specParams = paramMapper.selectList(wrapper);
        if(CollectionUtils.isEmpty(specParams)){
            throw new LyException(ExceptionEnum.SPEC_NOT_FOUND);
        }
        return specParams;
    }

    public List<SpecGroupDTO> findSpecGroupDTOSByCid(Long id) {
        //方式三
        //根据分类id查询规格组集合
        List<SpecGroup> specGroups = findSpecGroupsByCid(id);
        //把规格组集合转成规格组DTO集合
        List<SpecGroupDTO> groupDTOS = BeanHelper.copyWithCollection(specGroups, SpecGroupDTO.class);
        //一次把所有用到的规格参数全部查询出来
        List<SpecParam> specParams = fondParams(null, id, null);
        //对规格参数集合根据规格组id进行分组
        Map<Long, List<SpecParam>> specParamMap = specParams.stream().collect(Collectors.groupingBy(SpecParam::getGroupId));
        //遍历规格组
        groupDTOS.forEach(specGroup -> {
            specGroup.setParams(specParamMap.get(specGroup.getId()));
        });

        //方式二
        //根据分类id查询规格组集合
//        List<SpecGroup> specGroups = findSpecGroupsByCid(id);
//        //把规格组集合转成规格组DTO集合
//        List<SpecGroupDTO> groupDTOS = BeanHelper.copyWithCollection(specGroups, SpecGroupDTO.class);
//        //一次把所有用到的规格参数全部查询出来
//        List<SpecParam> specParams = fondParams(null, id, null);
//        //遍历规格组
//        groupDTOS.forEach(groupDTO->{
//            //遍历规格参数
//            specParams.forEach(specParam -> {
//                if(groupDTO.getId().equals(specParam.getGroupId())){
//                    groupDTO.getParams().add(specParam);
//                }
//            });
//        });

        //方式一
//        //根据分类id查询规格组集合
//        List<SpecGroup> specGroups = findSpecGroupsByCid(id);
//        //把规格组集合转成规格组DTO集合
//        List<SpecGroupDTO> groupDTOS = BeanHelper.copyWithCollection(specGroups, SpecGroupDTO.class);
//        //遍历
//        groupDTOS.forEach(groupDTO->{
//            //根据规格组id查询规格参数集合
//            List<SpecParam> specParams = fondParams(groupDTO.getId(), null, null);
//            groupDTO.setParams(specParams);
//        });

        return groupDTOS;
    }
}