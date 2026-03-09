package com.shop.sehodiary_api.service.diaryimage;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.shop.sehodiary_api.web.dto.diaryimage.FileRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3StorageService {

    /**
     * 날짜 기반 하위 폴더 앞에 붙일 고정 루트(옵션, 필요 없으면 빈 문자열로 두셔도 됩니다)
     */
    private static final String KEY_ROOT = "diaryImages";
    private final AmazonS3 s3;
    @Value("${cloud.aws.s3.bucket}")
    private String bucket;
    @Value("${cloud.aws.region.static}")
    private String region;

    /**
     * 파일 업로드
     */
    public FileRequest saveFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("빈 파일은 업로드할 수 없습니다.");
        }

        String originalName = file.getOriginalFilename();

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("파일을 읽을 수 없습니다.", e);
        }

        String hash = sha256(bytes);
        String key = buildHashKey(hash);

        if (s3.doesObjectExist(bucket, key)) {
            ObjectMetadata meta = s3.getObjectMetadata(bucket, key);
            return FileRequest.builder()
                    .originalFileName(originalName)
                    .storedFileName(hash)
                    .storedKey(key)
                    .mimeType(meta.getContentType())
                    .sizeBytes(meta.getContentLength())
                    .build();
        }

        ObjectMetadata meta = new ObjectMetadata();
        meta.setContentType(safeContentType(file.getContentType()));
        meta.setContentLength(bytes.length);
        meta.addUserMetadata("original-filename", originalName);

        try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
            s3.putObject(new PutObjectRequest(bucket, key, in, meta));
        } catch (IOException e) {
            throw new RuntimeException("파일 업로드 실패", e);
        }

        return FileRequest.builder()
                .originalFileName(originalName)
                .storedFileName(hash)
                .storedKey(key)
                .mimeType(meta.getContentType())
                .sizeBytes((long) bytes.length)
                .build();
    }

    /**
     * 파일 삭제 (storedKey = S3 key 전체 경로)
     */
    public void deleteFile(String storedKey) {
        if (!StringUtils.hasText(storedKey)) return;
        try {
            s3.deleteObject(new DeleteObjectRequest(bucket, storedKey));
        } catch (AmazonS3Exception e) {
            log.warn("S3 삭제 실패 (key: {}): {}", storedKey, e.getMessage());
        }
    }

    /**
     * 존재 여부 확인
     */
    public boolean exists(String storedKey) {
        if (!StringUtils.hasText(storedKey)) return false;
        try {
            s3.getObjectMetadata(bucket, storedKey);
            return true;
        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() == 404) return false;
            log.warn("S3 존재 확인 실패 (key: {}): {}", storedKey, e.getMessage());
            return false;
        }
    }

    // ================= helpers =================

    private String buildDatedKey(String storedName) {
        LocalDate d = LocalDate.now();
        // KEY_ROOT가 비어있어도 안전하게 경로 생성
        String root = (KEY_ROOT == null || KEY_ROOT.isBlank()) ? "" : (KEY_ROOT.endsWith("/") ? KEY_ROOT : KEY_ROOT + "/");
        return root
                + d.getYear() + "/"
                + String.format("%02d", d.getMonthValue()) + "/"
                + String.format("%02d", d.getDayOfMonth()) + "/"
                + storedName;
    }

    private String resolveExtension(String name) {
        if (!StringUtils.hasText(name)) return "";
        int i = name.lastIndexOf('.');
        return (i < 0) ? "" : name.substring(i + 1);
    }

    private String safeContentType(String ct) {
        return (ct != null && !ct.isBlank()) ? ct : "application/octet-stream";
    }

    /**
     * S3 Virtual-hosted–style URL
     */
    private String buildS3Url(String key) {
        return key;
    }

    private String buildHashKey(String hash) {
        return "files/sha256/" + hash.substring(0, 2) + "/" + hash.substring(2, 4) + "/" + hash;
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);

            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 계산 실패", e);
        }
    }
}
