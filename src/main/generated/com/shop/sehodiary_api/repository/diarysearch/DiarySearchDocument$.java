package com.shop.sehodiary_api.repository.diarysearch;

import com.redis.om.spring.metamodel.MetamodelField;
import com.redis.om.spring.metamodel.SearchFieldAccessor;
import com.redis.om.spring.metamodel.indexed.NumericField;
import com.redis.om.spring.metamodel.indexed.TextField;
import com.redis.om.spring.metamodel.indexed.TextTagField;
import com.shop.sehodiary_api.config.redis.diarysearch.DiarySearchDocument;

import java.lang.Long;
import java.lang.NoSuchFieldException;
import java.lang.SecurityException;
import java.lang.String;
import java.lang.reflect.Field;

public final class DiarySearchDocument$ {
  public static Field visibility;

  public static Field title;

  public static Field content;

  public static Field id;

  public static Field diaryId;

  public static Field createdAt;

  public static TextTagField<DiarySearchDocument, String> VISIBILITY;

  public static TextField<DiarySearchDocument, String> TITLE;

  public static TextField<DiarySearchDocument, String> CONTENT;

  public static TextTagField<DiarySearchDocument, String> ID;

  public static NumericField<DiarySearchDocument, Long> DIARY_ID;

  public static NumericField<DiarySearchDocument, Long> CREATED_AT;

  public static MetamodelField<DiarySearchDocument, String> _KEY;

  public static MetamodelField<DiarySearchDocument, DiarySearchDocument> _THIS;

  static {
    try {
      visibility = com.redis.om.spring.util.ObjectUtils.getDeclaredFieldTransitively(DiarySearchDocument.class, "visibility");
      title = com.redis.om.spring.util.ObjectUtils.getDeclaredFieldTransitively(DiarySearchDocument.class, "title");
      content = com.redis.om.spring.util.ObjectUtils.getDeclaredFieldTransitively(DiarySearchDocument.class, "content");
      id = com.redis.om.spring.util.ObjectUtils.getDeclaredFieldTransitively(DiarySearchDocument.class, "id");
      diaryId = com.redis.om.spring.util.ObjectUtils.getDeclaredFieldTransitively(DiarySearchDocument.class, "diaryId");
      createdAt = com.redis.om.spring.util.ObjectUtils.getDeclaredFieldTransitively(DiarySearchDocument.class, "createdAt");
      VISIBILITY = new TextTagField<DiarySearchDocument, String>(new SearchFieldAccessor("visibility", "$.visibility", visibility),true);
      TITLE = new TextField<DiarySearchDocument, String>(new SearchFieldAccessor("title", "$.title", title),true);
      CONTENT = new TextField<DiarySearchDocument, String>(new SearchFieldAccessor("content", "$.content", content),true);
      ID = new TextTagField<DiarySearchDocument, String>(new SearchFieldAccessor("id", "$.id", id),true);
      DIARY_ID = new NumericField<DiarySearchDocument, Long>(new SearchFieldAccessor("diaryId", "$.diaryId", diaryId),true);
      CREATED_AT = new NumericField<DiarySearchDocument, Long>(new SearchFieldAccessor("createdAt", "$.createdAt", createdAt),true);
      _KEY = new MetamodelField<DiarySearchDocument, String>("__key", String.class, true);
      _THIS = new MetamodelField<DiarySearchDocument, DiarySearchDocument>("__this", DiarySearchDocument.class, true);
    } catch(NoSuchFieldException | SecurityException e) {
      System.err.println(e.getMessage());
    }
  }
}
