package com.shop.sehodiary_api.config.redis.diarysearch;

import com.redis.om.spring.metamodel.MetamodelField;
import com.redis.om.spring.metamodel.SearchFieldAccessor;
import com.redis.om.spring.metamodel.indexed.NumericField;
import com.redis.om.spring.metamodel.indexed.TextField;
import com.redis.om.spring.metamodel.indexed.TextTagField;
import java.lang.Long;
import java.lang.NoSuchFieldException;
import java.lang.SecurityException;
import java.lang.String;
import java.lang.reflect.Field;

public final class DiarySearchDocument$ {
  public static Field id;

  public static Field content;

  public static Field diaryId;

  public static Field visibility;

  public static Field userId;

  public static Field title;

  public static Field createdAt;

  public static TextTagField<DiarySearchDocument, String> ID;

  public static TextField<DiarySearchDocument, String> CONTENT;

  public static NumericField<DiarySearchDocument, Long> DIARY_ID;

  public static TextTagField<DiarySearchDocument, String> VISIBILITY;

  public static NumericField<DiarySearchDocument, Long> USER_ID;

  public static TextField<DiarySearchDocument, String> TITLE;

  public static NumericField<DiarySearchDocument, Long> CREATED_AT;

  public static MetamodelField<DiarySearchDocument, String> _KEY;

  public static MetamodelField<DiarySearchDocument, DiarySearchDocument> _THIS;

  static {
    try {
      id = com.redis.om.spring.util.ObjectUtils.getDeclaredFieldTransitively(DiarySearchDocument.class, "id");
      content = com.redis.om.spring.util.ObjectUtils.getDeclaredFieldTransitively(DiarySearchDocument.class, "content");
      diaryId = com.redis.om.spring.util.ObjectUtils.getDeclaredFieldTransitively(DiarySearchDocument.class, "diaryId");
      visibility = com.redis.om.spring.util.ObjectUtils.getDeclaredFieldTransitively(DiarySearchDocument.class, "visibility");
      userId = com.redis.om.spring.util.ObjectUtils.getDeclaredFieldTransitively(DiarySearchDocument.class, "userId");
      title = com.redis.om.spring.util.ObjectUtils.getDeclaredFieldTransitively(DiarySearchDocument.class, "title");
      createdAt = com.redis.om.spring.util.ObjectUtils.getDeclaredFieldTransitively(DiarySearchDocument.class, "createdAt");
      ID = new TextTagField<DiarySearchDocument, String>(new SearchFieldAccessor("id", "$.id", id),true);
      CONTENT = new TextField<DiarySearchDocument, String>(new SearchFieldAccessor("content", "$.content", content),true);
      DIARY_ID = new NumericField<DiarySearchDocument, Long>(new SearchFieldAccessor("diaryId", "$.diaryId", diaryId),true);
      VISIBILITY = new TextTagField<DiarySearchDocument, String>(new SearchFieldAccessor("visibility", "$.visibility", visibility),true);
      USER_ID = new NumericField<DiarySearchDocument, Long>(new SearchFieldAccessor("userId", "$.userId", userId),true);
      TITLE = new TextField<DiarySearchDocument, String>(new SearchFieldAccessor("title", "$.title", title),true);
      CREATED_AT = new NumericField<DiarySearchDocument, Long>(new SearchFieldAccessor("createdAt", "$.createdAt", createdAt),true);
      _KEY = new MetamodelField<DiarySearchDocument, String>("__key", String.class, true);
      _THIS = new MetamodelField<DiarySearchDocument, DiarySearchDocument>("__this", DiarySearchDocument.class, true);
    } catch(NoSuchFieldException | SecurityException e) {
      System.err.println(e.getMessage());
    }
  }
}
