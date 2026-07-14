/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.dashboard.mapper;

import com.medicine.dashboard.dto.DashboardCounts;
import com.medicine.dashboard.dto.DashboardNews;
import com.medicine.dashboard.dto.NameValue;

import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface DashboardMapper {

    @Select("SELECT "
            + "(SELECT COUNT(*) FROM doctor) AS doctorCount, "
            + "(SELECT COUNT(*) FROM drugcompany) AS companyCount, "
            + "(SELECT COUNT(*) FROM drug) AS drugCount, "
            + "(SELECT COUNT(*) FROM sale) AS saleCount, "
            + "(SELECT COUNT(*) FROM city) AS cityCount, "
            + "(SELECT COUNT(*) FROM medical_policy) AS medicalPolicyCount, "
            + "(SELECT COUNT(*) FROM company_policy) AS companyPolicyCount, "
            + "(SELECT COUNT(*) FROM material) AS materialCount")
    DashboardCounts findCounts();

    @Select("SELECT dl.name AS name, COUNT(d.id) AS value "
            + "FROM doctor_level dl LEFT JOIN doctor d ON d.level_id = dl.id "
            + "GROUP BY dl.id, dl.name ORDER BY dl.id")
    List<NameValue> findDoctorLevels();

    @Select("SELECT tt.name AS name, COUNT(d.id) AS value "
            + "FROM treat_type tt LEFT JOIN doctor d ON d.type_id = tt.id "
            + "GROUP BY tt.id, tt.name ORDER BY tt.id")
    List<NameValue> findTreatTypes();

    @Select("SELECT id, title, summary, publishedAt FROM ("
            + "SELECT CONCAT('medical-', id) AS id, title, LEFT(message, 120) AS summary, "
            + "COALESCE(NULLIF(update_time, ''), create_time) AS publishedAt FROM medical_policy "
            + "UNION ALL "
            + "SELECT CONCAT('company-', id), title, LEFT(message, 120), "
            + "CAST(COALESCE(update_time, create_time) AS CHAR) FROM company_policy "
            + "UNION ALL "
            + "SELECT CONCAT('material-', id), title, LEFT(message, 120), "
            + "CAST(COALESCE(update_time, create_time) AS CHAR) FROM material"
            + ") recent_news ORDER BY publishedAt DESC LIMIT 6")
    List<DashboardNews> findNews();
}
