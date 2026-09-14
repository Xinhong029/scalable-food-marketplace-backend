package com.lastcalleats.product.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lastcalleats.product.dto.TemplateRequest;
import com.lastcalleats.product.dto.TemplateResponse;
import com.lastcalleats.product.service.ProductTemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for {@link ProductTemplateController}.
 *
 * <p>This test class verifies that the controller correctly handles HTTP
 * requests, delegates work to the service layer, and returns successful
 * responses.</p>
 *
 * <p>These tests use standalone MockMvc setup, so they do not require a full
 * Spring Boot application context.</p>
 */
@ExtendWith(MockitoExtension.class)
class ProductTemplateControllerTest {

  private MockMvc mockMvc;

  private ObjectMapper objectMapper;

  @Mock
  private ProductTemplateService productTemplateService;

  @InjectMocks
  private ProductTemplateController productTemplateController;

  /**
   * Initializes MockMvc and JSON serializer before each test.
   */
  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    objectMapper.findAndRegisterModules();

    mockMvc = MockMvcBuilders
        .standaloneSetup(productTemplateController)
        .build();
  }

  /**
   * Verifies that creating a template with a valid request returns HTTP 200
   * and delegates to the service layer.
   */
  @Test
  @DisplayName("createTemplate should return 200 for valid request")
  void createTemplate_validRequest_returnsOk() throws Exception {
    TemplateRequest request = new TemplateRequest();
    request.setName("Bread");
    request.setDescription("Fresh bread");
    request.setOriginalPrice(new BigDecimal("15.99"));

    when(productTemplateService.createTemplate(isNull(), any(TemplateRequest.class)))
        .thenReturn(
            TemplateResponse.builder()
                .id(1L)
                .merchantId(10L)
                .name("Bread")
                .description("Fresh bread")
                .originalPrice(new BigDecimal("15.99"))
                .isActive(true)
                .build()
        );

    mockMvc.perform(post("/api/merchant/templates")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());

    verify(productTemplateService).createTemplate(isNull(), any(TemplateRequest.class));
  }

  /**
   * Verifies that fetching all templates for the current merchant returns
   * HTTP 200 and delegates to the service layer.
   */
  @Test
  @DisplayName("getTemplates should return 200")
  void getTemplates_success_returnsOk() throws Exception {
    when(productTemplateService.getTemplates(isNull()))
        .thenReturn(
            List.of(
                TemplateResponse.builder()
                    .id(1L)
                    .merchantId(10L)
                    .name("Bread")
                    .description("Fresh bread")
                    .originalPrice(new BigDecimal("15.99"))
                    .isActive(true)
                    .build()
            )
        );

    mockMvc.perform(get("/api/merchant/templates"))
        .andExpect(status().isOk());

    verify(productTemplateService).getTemplates(isNull());
  }

  /**
   * Verifies that updating a template with a valid request returns HTTP 200
   * and delegates to the service layer.
   */
  @Test
  @DisplayName("updateTemplate should return 200 for valid request")
  void updateTemplate_validRequest_returnsOk() throws Exception {
    TemplateRequest request = new TemplateRequest();
    request.setName("Updated Bread");
    request.setDescription("Updated description");
    request.setOriginalPrice(new BigDecimal("12.99"));

    when(productTemplateService.updateTemplate(isNull(), eq(1L), any(TemplateRequest.class)))
        .thenReturn(
            TemplateResponse.builder()
                .id(1L)
                .merchantId(10L)
                .name("Updated Bread")
                .description("Updated description")
                .originalPrice(new BigDecimal("12.99"))
                .isActive(true)
                .build()
        );

    mockMvc.perform(put("/api/merchant/templates/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());

    verify(productTemplateService).updateTemplate(isNull(), eq(1L), any(TemplateRequest.class));
  }

  /**
   * Verifies that deleting a template returns HTTP 200
   * and delegates to the service layer.
   */
  @Test
  @DisplayName("deleteTemplate should return 200")
  void deleteTemplate_success_returnsOk() throws Exception {
    doNothing().when(productTemplateService).deleteTemplate(isNull(), eq(1L));

    mockMvc.perform(delete("/api/merchant/templates/1"))
        .andExpect(status().isOk());

    verify(productTemplateService).deleteTemplate(isNull(), eq(1L));
  }
}