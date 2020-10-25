package com.leyou.upload.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.common.utils.BinaryUtil;
import com.aliyun.oss.model.MatchMode;
import com.aliyun.oss.model.PolicyConditions;
import com.leyou.common.constants.LyConstants;
import com.leyou.common.exception.pojo.ExceptionEnum;
import com.leyou.common.exception.pojo.LyException;
import com.leyou.upload.config.OSSProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

@Service
public class UploadService {

    @Autowired
    private OSSProperties prop;

    @Autowired
    private OSS ossClient;

    private List<String> ALLOW_IMG_TYPE = Arrays.asList("image/jpeg");

    public String uploadFileToNginx(MultipartFile file) {
        //获取当前文件的mime类型
        String contentType = file.getContentType();
        //判断当前文件的mime类型是否合法
        if(!ALLOW_IMG_TYPE.contains(contentType)){
            throw new LyException(ExceptionEnum.INVALID_PARAM_ERROR);
        }
        //解析图片流
        BufferedImage bufferedImage = null;
        try {
            bufferedImage = ImageIO.read(file.getInputStream());
        } catch (IOException e) {
            throw new LyException(ExceptionEnum.INVALID_PARAM_ERROR);
        }
        //判断是否有图片流
        if(bufferedImage==null){
            throw new LyException(ExceptionEnum.INVALID_PARAM_ERROR);
        }
        //创建出图片服务器文件对象
        File serverFile = new File(LyConstants.IMAGE_PATH);
        //指定上传后文件的名称
        String fileName = UUID.randomUUID()+file.getOriginalFilename();
        //创建一个上传的目标文件对象
        File targetImgFile = new File(serverFile, fileName);
        //上传图片
        try {
            file.transferTo(targetImgFile);
        } catch (IOException e) {
            throw new LyException(ExceptionEnum.FILE_UPLOAD_ERROR);
        }
        return LyConstants.IMAGE_URL+fileName;
    }

    public Map<String, Object> getSignature() {
        try {
            long expireTime = prop.getExpireTime();
            long expireEndTime = System.currentTimeMillis() + expireTime * 1000;
            Date expiration = new Date(expireEndTime);
            // PostObject请求最大可支持的文件大小为5 GB，即CONTENT_LENGTH_RANGE为5*1024*1024*1024。
            PolicyConditions policyConds = new PolicyConditions();
            policyConds.addConditionItem(PolicyConditions.COND_CONTENT_LENGTH_RANGE, 0, prop.getMaxFileSize());
            policyConds.addConditionItem(MatchMode.StartWith, PolicyConditions.COND_KEY, prop.getDir());

            String postPolicy = ossClient.generatePostPolicy(expiration, policyConds);
            byte[] binaryData = postPolicy.getBytes("utf-8");
            String encodedPolicy = BinaryUtil.toBase64String(binaryData);
            String postSignature = ossClient.calculatePostSignature(postPolicy);

            Map<String, Object> respMap = new LinkedHashMap<String, Object>();
            respMap.put("accessId", prop.getAccessKeyId());
            respMap.put("policy", encodedPolicy);
            respMap.put("signature", postSignature);
            respMap.put("dir", prop.getDir());
            respMap.put("host", prop.getHost());
            respMap.put("expire", expireEndTime);

            return respMap;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new LyException(ExceptionEnum.FILE_UPLOAD_ERROR);
        } finally {
            ossClient.shutdown();
        }
    }
}
