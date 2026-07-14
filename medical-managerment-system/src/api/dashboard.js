/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

import request from '../utils/request';

export function getDashboard() {
  return request({
    url: '/dashboard',
    method: 'GET',
  });
}
