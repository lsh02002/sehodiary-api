package com.shop.sehodiary_api.web.dto.follow;

public record FollowRelationshipResponse(
        boolean isFollowing,
        boolean isFollowedBy,
        boolean isMutual
) {}
