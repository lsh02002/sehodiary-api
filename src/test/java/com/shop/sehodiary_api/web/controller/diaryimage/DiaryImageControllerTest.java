package com.shop.sehodiary_api.web.controller.diaryimage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.sehodiary_api.repository.user.userDetails.CustomUserDetails;
import com.shop.sehodiary_api.service.diaryimage.DiaryImageService;
import com.shop.sehodiary_api.web.dto.diaryimage.DiaryImageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DiaryImageController.class)
class DiaryImageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DiaryImageService diaryImageService;

    private CustomUserDetails createCustomUserDetails(Long id) {
        return new CustomUserDetails(
                id,
                "test@test.com",
                "password",
                "test",
                List.of()
        );
    }

    private DiaryImageResponse createDiaryImageResponse(Long id, String fileUrl) {
        return DiaryImageResponse.builder()
                .id(id)
                .fileUrl(fileUrl)
                .build();
    }

    @Nested
    @DisplayName("단일 이미지 업로드")
    class UploadFileTest {

        @Test
        @DisplayName("성공")
        void uploadFile_success() throws Exception {
            Long userId = 1L;
            Long diaryId = 10L;
            CustomUserDetails customUserDetails = createCustomUserDetails(userId);

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "test.jpg",
                    MediaType.IMAGE_JPEG_VALUE,
                    "image-content".getBytes(StandardCharsets.UTF_8)
            );

            DiaryImageResponse response = createDiaryImageResponse(100L, "https://image.test/100.jpg");

            given(diaryImageService.uploadFileByUserIdAndDiaryId(eq(userId), eq(diaryId), any()))
                    .willReturn(response);

            mockMvc.perform(multipart("/diaryimage/{diaryId}", diaryId)
                            .file(file)
                            .with(authentication(
                                    new UsernamePasswordAuthenticationToken(
                                            customUserDetails,
                                            null,
                                            customUserDetails.getAuthorities()
                                    )
                            )).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(100L))
                    .andExpect(jsonPath("$.fileUrl").value("https://image.test/100.jpg"));
        }
    }

    @Nested
    @DisplayName("여러 이미지 업로드")
    class UploadManyFilesTest {

        @Test
        @DisplayName("성공")
        void uploadManyFiles_success() throws Exception {
            Long userId = 1L;
            Long diaryId = 20L;
            CustomUserDetails customUserDetails = createCustomUserDetails(userId);

            MockMultipartFile file1 = new MockMultipartFile(
                    "files",
                    "test1.jpg",
                    MediaType.IMAGE_JPEG_VALUE,
                    "image-content-1".getBytes(StandardCharsets.UTF_8)
            );

            MockMultipartFile file2 = new MockMultipartFile(
                    "files",
                    "test2.jpg",
                    MediaType.IMAGE_JPEG_VALUE,
                    "image-content-2".getBytes(StandardCharsets.UTF_8)
            );

            List<DiaryImageResponse> response = List.of(
                    createDiaryImageResponse(201L, "https://image.test/201.jpg"),
                    createDiaryImageResponse(202L, "https://image.test/202.jpg")
            );

            given(diaryImageService.uploadManyFiles(eq(userId), eq(diaryId), anyList()))
                    .willReturn(response);

            mockMvc.perform(multipart("/diaryimage/{diaryId}/batch", diaryId)
                            .file(file1)
                            .file(file2)
                            .with(authentication(
                                    new UsernamePasswordAuthenticationToken(
                                            customUserDetails,
                                            null,
                                            customUserDetails.getAuthorities()
                                    )
                            )).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].id").value(201L))
                    .andExpect(jsonPath("$[0].fileUrl").value("https://image.test/201.jpg"))
                    .andExpect(jsonPath("$[1].id").value(202L))
                    .andExpect(jsonPath("$[1].fileUrl").value("https://image.test/202.jpg"));
        }
    }

    @Nested
    @DisplayName("다이어리 이미지 단건 조회")
    @WithMockUser(roles = "USER")
    class FindDiaryImageByIdTest {

        @Test
        @DisplayName("성공")
        void findDiaryImageById_success() throws Exception {
            Long diaryImageId = 1L;
            DiaryImageResponse response = createDiaryImageResponse(diaryImageId, "https://image.test/1.jpg");

            given(diaryImageService.findDiaryImageId(diaryImageId)).willReturn(response);

            mockMvc.perform(get("/diaryimage/{diaryImageId}", diaryImageId).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.fileUrl").value("https://image.test/1.jpg"));
        }
    }

    @Nested
    @DisplayName("다이어리 이미지 삭제")
    class DeleteDiaryImageByIdTest {

        @Test
        @DisplayName("성공")
        void deleteDiaryImageById_success() throws Exception {
            Long userId = 1L;
            Long diaryImageId = 300L;
            CustomUserDetails customUserDetails = createCustomUserDetails(userId);

            willDoNothing().given(diaryImageService)
                    .deleteFileByUserIdAndDiaryImageId(userId, diaryImageId);

            mockMvc.perform(delete("/diaryimage/{diaryImageId}", diaryImageId)
                            .with(authentication(
                                    new UsernamePasswordAuthenticationToken(
                                            customUserDetails,
                                            null,
                                            customUserDetails.getAuthorities()
                                    )
                            )).with(csrf()))
                    .andExpect(status().isOk());
        }
    }
}