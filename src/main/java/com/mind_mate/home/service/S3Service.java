package com.mind_mate.home.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import lombok.RequiredArgsConstructor;
import net.coobird.thumbnailator.Thumbnails;

@Service
@RequiredArgsConstructor
public class S3Service {
	private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;
    
    public String uploadProfileImage(Long userId, MultipartFile file) throws IOException {
    	 if (file == null || file.isEmpty()) {
             throw new IllegalArgumentException("파일이 비어 있습니다.");
         }


         if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
             throw new IllegalArgumentException("이미지 파일만 업로드할 수 있습니다.");
         }
         
         BufferedImage originalImage = ImageIO.read(file.getInputStream());
         if (originalImage == null) {
             throw new IllegalArgumentException("이미지 파일을 읽을 수 없습니다.");
         }
         
         int origW = originalImage.getWidth();
         int origH = originalImage.getHeight();
         
         int maxSize = 512;
         int targetW = origW;
         int targetH = origH;
         
         if (Math.max(origW, origH) > maxSize) {
             double ratio = (double) maxSize / (double) Math.max(origW, origH);
             targetW = (int) Math.round(origW * ratio);
             targetH = (int) Math.round(origH * ratio);
         }
        
         // 프로필 이미지 압축 (JPEG 품질 0.8)
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        
        Thumbnails.of(originalImage)
        .size(targetW, targetH)
        .outputFormat("jpg")   
        .outputQuality(0.8f)   
        .toOutputStream(os);
        
        byte[] bytes = os.toByteArray();
        ByteArrayInputStream is = new ByteArrayInputStream(bytes);
        
        // S3 key: profile/{userId}/{UUID}.jpg
        String key = "profile/" + userId + "/" + UUID.randomUUID() + ".jpg";
        
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(bytes.length);
        metadata.setContentType("image/jpeg");
       
        
        amazonS3.putObject(bucket, key, is, metadata);
        
        return amazonS3.getUrl(bucket, key).toString();
    }
    
    public String uploadDiaryImage(Long userId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어 있습니다.");
        }

        if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드할 수 있습니다.");
        }

        BufferedImage originalImage = ImageIO.read(file.getInputStream());
        if (originalImage == null) {
            throw new IllegalArgumentException("이미지 파일을 읽을 수 없습니다.");
        }

        int origW = originalImage.getWidth();
        int origH = originalImage.getHeight();

        // 이미지 크기 1080 (프로필 보다는 크게)
        int maxSize = 1080;
        int targetW = origW;
        int targetH = origH;

        if (Math.max(origW, origH) > maxSize) {
            double ratio = (double) maxSize / (double) Math.max(origW, origH);
            targetW = (int) Math.round(origW * ratio);
            targetH = (int) Math.round(origH * ratio);
        }

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        Thumbnails.of(originalImage)
            .size(targetW, targetH)
            .outputFormat("jpg")      // PNG => jpg로 저장 (용량 절약)
            .outputQuality(0.85f)
            .toOutputStream(os);

        byte[] bytes = os.toByteArray();
        ByteArrayInputStream is = new ByteArrayInputStream(bytes);

        String key = "diary/" + userId + "/" + UUID.randomUUID() + ".jpg";

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(bytes.length);
        metadata.setContentType("image/jpeg");

        amazonS3.putObject(bucket, key, is, metadata);

        // DB에는 바로 img src로 쓸 수 있는 전체 URL을 저장
        return amazonS3.getUrl(bucket, key).toString();
    }
    
    public void deleteFileByUrl(String url) {
    	if (url == null || url.isBlank()) {
            return;
        }
        
    	 int index = url.indexOf(".amazonaws.com/");
         if (index == -1) {
        	 // S3 URL이 아니면 스킵
             return;
         }

         String key = url.substring(index + ".amazonaws.com/".length());
         amazonS3.deleteObject(bucket, key);
    }
    
    private void deleteByPrefix(String prefix) {
        String continuationToken = null;

        do {
            ListObjectsV2Request req = new ListObjectsV2Request()
                    .withBucketName(bucket)
                    .withPrefix(prefix)
                    .withContinuationToken(continuationToken);

            ListObjectsV2Result result = amazonS3.listObjectsV2(req);

            if (!result.getObjectSummaries().isEmpty()) {
                DeleteObjectsRequest dor = new DeleteObjectsRequest(bucket);
                dor.setKeys(
                        result.getObjectSummaries().stream()
                                .map(S3ObjectSummary::getKey)
                                .map(DeleteObjectsRequest.KeyVersion::new)
                                .collect(Collectors.toList())
                );
                amazonS3.deleteObjects(dor);
            }

            continuationToken = result.getNextContinuationToken();
        } while (continuationToken != null);
    }

  
    public void deleteAllUserImages(Long userId) {
        // 예: profile/1/, diary/1/
        deleteByPrefix("profile/" + userId + "/");
        deleteByPrefix("diary/" + userId + "/");
    }

    
    public S3Object getObject(String key) {
        return amazonS3.getObject(bucket, key);
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf(".");
        return (dot != -1) ? filename.substring(dot) : "";
    }
    
 
}
