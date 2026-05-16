package com.shop.sehodiary_api.config.redis.refreshToken;

import com.redis.om.spring.metamodel.MetamodelField;
import com.redis.om.spring.metamodel.SearchFieldAccessor;
import com.redis.om.spring.metamodel.indexed.TextTagField;
import com.redis.om.spring.metamodel.nonindexed.NonIndexedTextField;
import java.lang.NoSuchFieldException;
import java.lang.SecurityException;
import java.lang.String;
import java.lang.reflect.Field;

public final class RefreshToken$ {
  public static Field refreshToken;

  public static Field email;

  public static Field authId;

  public static NonIndexedTextField<RefreshToken, String> REFRESH_TOKEN;

  public static NonIndexedTextField<RefreshToken, String> EMAIL;

  public static TextTagField<RefreshToken, String> AUTH_ID;

  public static MetamodelField<RefreshToken, String> _KEY;

  public static MetamodelField<RefreshToken, RefreshToken> _THIS;

  static {
    try {
      refreshToken = com.redis.om.spring.util.ObjectUtils.getDeclaredFieldTransitively(RefreshToken.class, "refreshToken");
      email = com.redis.om.spring.util.ObjectUtils.getDeclaredFieldTransitively(RefreshToken.class, "email");
      authId = com.redis.om.spring.util.ObjectUtils.getDeclaredFieldTransitively(RefreshToken.class, "authId");
      REFRESH_TOKEN = new NonIndexedTextField<RefreshToken, String>(new SearchFieldAccessor("refreshToken", "$.refreshToken", refreshToken),false);
      EMAIL = new NonIndexedTextField<RefreshToken, String>(new SearchFieldAccessor("email", "$.email", email),false);
      AUTH_ID = new TextTagField<RefreshToken, String>(new SearchFieldAccessor("authId", "$.authId", authId),true);
      _KEY = new MetamodelField<RefreshToken, String>("__key", String.class, true);
      _THIS = new MetamodelField<RefreshToken, RefreshToken>("__this", RefreshToken.class, true);
    } catch(NoSuchFieldException | SecurityException e) {
      System.err.println(e.getMessage());
    }
  }
}
