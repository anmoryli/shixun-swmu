/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

import {
    getSalePlaceInfo,
    addSalePlace,
    deleteSalePlace,
    modifySalePlaceInfo,
    getAllSalePlaceInfo,
} from '../../api/admin/saleInfoManage';

const initialState = {
    salePlaceInfo: {}, // 销售地点分页信息（列表视图）
    saleAllPlaceInfo: {}, // 全部销售地点信息（地图视图，与分页数据分离，避免互相覆盖）
};
const mutations = {
    GET_SALE_PLACE_INFO(state, payload) {
        state.salePlaceInfo = payload;
    },
    GET_ALL_SALE_PLACE_INFO(state, payload) {
        state.saleAllPlaceInfo = payload;
    },
};
const actions = {
    // 销售地点信息分页，关键字查询
    getSalePlaceInfo({ commit }, { pn, size, keyword }) {
        getSalePlaceInfo(pn, size, keyword).then((res) => {
            if (res) {
                commit('GET_SALE_PLACE_INFO', res.data.data.salePageInfo);
            }
        });
    },
    // 新增销售地点
    addSalePlace(
        { dispatch },
        { saleName, salePhone, address, longitude, latitude, size }
    ) {
        return addSalePlace({ saleName, salePhone, address, longitude, latitude }).then((res) => {
            //   新增之后跳转到最后一页
            dispatch('getSalePlaceInfo', { pn: res.data.data.pages, size });
            // 刷新地图标记点
            dispatch('getAllSalePlaceInfo');
        });
    },
    // 删除销售地点信息
    deleteSalePlace({ dispatch }, { saleId, pn, size, keyword }) {
        return deleteSalePlace(saleId).then(() => {
            dispatch('getSalePlaceInfo', { pn, size, keyword });
            dispatch('getAllSalePlaceInfo');
        });
    },
    // 修改销售地点信息
    modifySalePlaceInfo(
        { dispatch },
        { saleId, saleName, salePhone, address, longitude, latitude, pn, size, keyword }
    ) {
        return modifySalePlaceInfo(saleId, {
            saleName,
            salePhone,
            address,
            longitude,
            latitude,
        }).then(() => {
            dispatch('getSalePlaceInfo', { pn, size, keyword });
            dispatch('getAllSalePlaceInfo');
        });
    },
    // 获取所有销售地点（地图视图用，写入独立的 saleAllPlaceInfo）
    getAllSalePlaceInfo({ commit }) {
        return getAllSalePlaceInfo().then((res) => {
            if (res) {
                commit('GET_ALL_SALE_PLACE_INFO', res.data.data.salePageInfo);
            }
        });
    },
};
export default {
    namespaced: true,
    state: initialState,
    mutations,
    actions,
};
