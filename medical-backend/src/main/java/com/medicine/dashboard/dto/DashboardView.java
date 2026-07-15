/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.dashboard.dto;

import java.io.Serializable;
import java.util.List;

public class DashboardView implements Serializable {

    private static final long serialVersionUID = 1L;

    private DashboardCounts counts;
    private List<NameValue> doctorLevels;
    private List<NameValue> treatTypes;
    private List<DashboardNews> news;

    public DashboardView(DashboardCounts counts,
                         List<NameValue> doctorLevels,
                         List<NameValue> treatTypes,
                         List<DashboardNews> news) {
        this.counts = counts;
        this.doctorLevels = doctorLevels;
        this.treatTypes = treatTypes;
        this.news = news;
    }

    public DashboardCounts getCounts() {
        return counts;
    }

    public void setCounts(DashboardCounts counts) {
        this.counts = counts;
    }

    public List<NameValue> getDoctorLevels() {
        return doctorLevels;
    }

    public void setDoctorLevels(List<NameValue> doctorLevels) {
        this.doctorLevels = doctorLevels;
    }

    public List<NameValue> getTreatTypes() {
        return treatTypes;
    }

    public void setTreatTypes(List<NameValue> treatTypes) {
        this.treatTypes = treatTypes;
    }

    public List<DashboardNews> getNews() {
        return news;
    }

    public void setNews(List<DashboardNews> news) {
        this.news = news;
    }
}
