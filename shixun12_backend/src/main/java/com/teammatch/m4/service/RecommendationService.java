package com.teammatch.m4.service;

import com.teammatch.dto.RecommendationItem;

import java.util.List;

public interface RecommendationService {
    List<RecommendationItem> recommend(Long projectId, int limit);
}
