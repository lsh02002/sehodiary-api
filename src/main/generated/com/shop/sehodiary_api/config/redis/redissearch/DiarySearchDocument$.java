package com.shop.sehodiary_api.config.redis.redissearch;

import com.redis.om.spring.metamodel.MetamodelField;
import com.redis.om.spring.metamodel.SearchFieldAccessor;
import com.redis.om.spring.metamodel.indexed.DateField;
import com.redis.om.spring.metamodel.indexed.NumericField;
import com.redis.om.spring.metamodel.indexed.TextField;
import com.redis.om.spring.metamodel.indexed.TextTagField;
import java.lang.Long;
import java.lang.NoSuchFieldException;
import java.lang.SecurityException;
import java.lang.String;
import java.lang.reflect.Field;
import java.time.LocalDateTime;

public final class DiarySearchDocument$ {
  public static Field content;

  public static Field createdAt;

  public static Field visibility;

  public static Field title;

  public static Field id;

  public static Field userId;

  public static Field diaryId;

  public static TextField<DiarySearchDocument, String> CONTENT;

  public static DateField<DiarySearchDocument, LocalDateTime> CREATED_AT;

  public static TextTagField<DiarySearchDocument, String> VISIBILITY;

  public static TextField<DiarySearchDocument, String> TITLE;

  public static TextTagField<DiarySearchDocument, String> ID;

  public static NumericField<DiarySearchDocument, Long> USER_ID;

  public static NumericField<DiarySearchDocument, Long> DIARY_ID;

  public static MetamodelField<DiarySearchDocument, String> _KEY;

  public static MetamodelField<DiarySearchDocument, DiarySearchDocument> _THIS;

  static {
    try {
      content = com.redis.om.spring.util.ObjectUtils.getDeclaredFieldTransitively(DiarySearchDocument.class, "content");
      createdAt = com.redis.om.spring.util.ObjectUtils.getDeclaredFieldTransitively(DiarySearchDocument.class, "createdAt");
      visibility = com.redis.om.spring.util.ObjectUtils.getDeclaredFieldTransitively(DiarySearchDocument.class, "visibility");
      title = com.redis.om.spring.util.ObjectUtils.getDeclaredFieldTransitively(DiarySearchDocument.class, "title");
      id = com.redis.om.spring.util.ObjectUtils.getDeclaredFieldTransitively(DiarySearchDocument.class, "id");
      userId = com.redis.om.spring.util.ObjectUtils.getDeclaredFieldTransitively(DiarySearchDocument.class, "userId");
      diaryId = com.redis.om.spring.util.ObjectUtils.getDeclaredFieldTransitively(DiarySearchDocument.class, "diaryId");
      CONTENT = new TextField<DiarySearchDocument, String>(new SearchFieldAccessor("content", "$.content", content),true);
      CREATED_AT = new DateField<DiarySearchDocument, LocalDateTime>(new SearchFieldAccessor("createdAt", "$.createdAt", createdAt),true);
      VISIBILITY = new TextTagField<DiarySearchDocument, String>(new SearchFieldAccessor("visibility", "$.visibility", visibility),true);
      TITLE = new TextField<DiarySearchDocument, String>(new SearchFieldAccessor("title", "$.title", title),true);
      ID = new TextTagField<DiarySearchDocument, String>(new SearchFieldAccessor("id", "$.id", id),true);
      USER_ID = new NumericField<DiarySearchDocument, Long>(new SearchFieldAccessor("userId", "$.userId", userId),true);
      DIARY_ID = new NumericField<DiarySearchDocument, Long>(new SearchFieldAccessor("diaryId", "$.diaryId", diaryId),true);
      _KEY = new MetamodelField<DiarySearchDocument, String>("__key", String.class, true);
      _THIS = new MetamodelField<DiarySearchDocument, DiarySearchDocument>("__this", DiarySearchDocument.class, true);
    } catch(NoSuchFieldException | SecurityException e) {
      System.err.println(e.getMessage());
    }
  }
}
