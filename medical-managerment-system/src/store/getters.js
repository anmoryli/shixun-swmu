/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

const getters = {
  token: (state) => state.app.token,
  menuList: (state) => state.app.menuList,
  userInfo: (state) => state.app.userInfo,
  permissionCodes: (state) => state.app.permissionCodes,
  roles: (state) => state.app.roles,
  allowedRoutePaths: (state) => state.app.allowedRoutePaths,
  allowedRouteNames: (state) => state.app.allowedRouteNames,
  companyInfo: (state) => state.companyInfoManage.companyInfo,
  salePlaceInfo: (state) => state.saleInfoManage.salePlaceInfo,
  cityInfo: (state) => state.cityInfoManage.cityInfo,
  drugInfo: (state) => state.drugInfoManage.drugInfo,
  medicalPolicyInfo: (state) => state.medicalPolicyInfoManage.medicalPolicyInfo,
  drugInfo: (state) => state.drugInfoManage.drugInfo,
  companyPolicyInfo: (state) => state.companyPolicyInfoManage.companyPolicyInfo,
  doctorInfo: (state) => state.doctorInfoManage.doctorInfo,
  doctorLevelAndType: (state) => state.doctorInfoManage.doctorLevelAndType,
  materialInfo: (state) => state.materialInfoManage.materialInfo,
  saleAllPlaceInfo: (state) => state.saleInfoManage.saleAllPlaceInfo,


};
export default getters;
