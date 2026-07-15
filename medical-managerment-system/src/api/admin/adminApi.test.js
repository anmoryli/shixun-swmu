import { beforeEach, describe, expect, it, vi } from 'vitest';

const mocks = vi.hoisted(() => ({
  request: vi.fn(),
  judge: vi.fn((response) => response),
}));

vi.mock('../../utils/request', () => ({ default: mocks.request }));
vi.mock('../../utils/app', () => ({
  judgeDeleteResult: mocks.judge,
  judgeAddResult: mocks.judge,
  judgeQueryResult: mocks.judge,
  judgeModifyResult: mocks.judge,
  judgeResetResult: mocks.judge,
}));

import * as city from './cityInfoManage';
import * as company from './companyInfoManage';
import * as companyPolicy from './companyPolicyInfoManage';
import * as doctor from './doctorInfoManage';
import * as drug from './drugInfoManage';
import * as material from './materialInfoManage';
import * as medicalPolicy from './medicalPolicyInfoManage';
import * as sale from './saleInfoManage';

const successfulResponse = { data: { code: 20000, data: { pages: 1 } } };

const cases = [
  ['city page', () => city.getCityInfo(2, 10, '泸州'), { url: '/citys/2/10', method: 'GET', params: { name: '泸州' } }],
  ['city all', () => city.getAllCityInfo(), { url: '/citys', method: 'GET' }],
  ['city add', () => city.addCity('510500'), { url: '/citys?cityNumber=510500', method: 'POST' }],
  ['city delete', () => city.deleteCity(7), { url: '/citys/7', method: 'DELETE' }],

  ['company page', () => company.getCompanyInfo(1, 5, '药业'), { url: '/companys/1/5', method: 'GET', params: { name: '药业' } }],
  ['company all', () => company.getAllCompanyInfo(), { url: '/companys', method: 'GET' }],
  ['company add', () => company.addCompany('慧医', '08301234567'), { url: '/companys', method: 'POST', data: { companyName: '慧医', companyPhone: '08301234567' } }],
  ['company delete', () => company.deleteCompany(8), { url: '/companys/8', method: 'DELETE' }],
  ['company update', () => company.modifyCompanyInfo(8, '慧医新', '08307654321'), { url: '/companys/8', method: 'PUT', data: { companyName: '慧医新', companyPhone: '08307654321' } }],

  ['company policy page', () => companyPolicy.getCompanyPolicyInfo(2, 6, '采购'), { url: '/company_policys', method: 'GET', params: { pn: 2, size: 6, keyword: '采购' } }],
  ['company policy add', () => companyPolicy.addCompanyPolicy(3, '正文', '标题'), { url: '/company_policys', method: 'POST', data: { companyId: 3, message: '正文', title: '标题' } }],
  ['company policy delete', () => companyPolicy.deleteCompanyPolicy(9), { url: '/company_policys/9', method: 'DELETE' }],
  ['company policy update', () => companyPolicy.modifyCompanyPolicyInfo(9, 3, '新标题', '新正文'), { url: '/company_policys/9', method: 'PUT', data: { companyId: 3, title: '新标题', message: '新正文' } }],

  ['doctor page', () => doctor.getDoctorInfo(3, 8, '李'), { url: '/doctors', method: 'GET', params: { pn: 3, size: 8, keyword: '李' } }],
  ['doctor dictionaries', () => doctor.getDoctorLevelAndType(), { url: '/doctors/info', method: 'GET' }],
  ['doctor add', () => doctor.addDoctor(30, 1, '李医生', '15900000000', 'secret', 1, 2), { url: '/doctors', method: 'POST', data: { age: 30, levelId: 1, name: '李医生', phoneNumber: '15900000000', pwd: 'secret', sex: 1, typeId: 2 } }],
  ['doctor delete', () => doctor.deleteDoctor(10), { url: '/doctors/10', method: 'DELETE' }],
  ['doctor update', () => doctor.modifyDoctor(20, 31, 2, '王医生', '15900000001', 'secret2', 2, 3, 10), { url: '/doctors/10', method: 'PUT', data: { accountId: 20, age: 31, levelId: 2, name: '王医生', phoneNumber: '15900000001', pwd: 'secret2', sex: 2, typeId: 3 } }],
  ['doctor password reset', () => doctor.resetPassword(20), { url: '/doctors/reset/20', method: 'PUT' }],

  ['drug page', () => drug.getDrugInfo(1, 9, '感冒'), { url: '/drugs/1/9', method: 'GET', params: { name: '感冒' } }],
  ['drug add', () => drug.addDrug('药品', '说明', '疗效', '/image/a.png', [1, 2], '厂商'), { url: '/drugs', method: 'POST', data: { drugName: '药品', drugInfo: '说明', drugEffect: '疗效', drugImg: '/image/a.png', saleIds: [1, 2], drugPublisher: '厂商' } }],
  ['drug delete', () => drug.deleteDrug(11), { url: '/drugs/11', method: 'DELETE' }],
  ['drug update', () => drug.modifyDrugInfo(11, '新药', '新说明', '新疗效', '/image/b.png', [2]), { url: '/drugs/11', method: 'PUT', data: { drugName: '新药', drugInfo: '新说明', drugEffect: '新疗效', drugImg: '/image/b.png', saleIds: [2] } }],

  ['material page', () => material.getMaterialInfo(2, 7, '报销'), { url: '/materials', method: 'GET', params: { pn: 2, size: 7, keyword: '报销' } }],
  ['material add', () => material.addMaterial('正文', '材料'), { url: '/materials', method: 'POST', data: { message: '正文', title: '材料' } }],
  ['material delete', () => material.deleteMaterial(12), { url: '/materials/12', method: 'DELETE', data: { id: 12 } }],
  ['material update', () => material.modifyMaterial(12, '新正文', '新材料'), { url: '/materials/12', method: 'PUT', data: { message: '新正文', title: '新材料' } }],

  ['medical policy page', () => medicalPolicy.getMedicalPolicyInfo({ pn: 1, cityId: 5 }), { url: '/medical_policys', method: 'GET', params: { pn: 1, cityId: 5 } }],
  ['medical policy add', () => medicalPolicy.addMedicalPolicy(5, '政策', '2026-07-15', '正文'), { url: '/medical_policys', method: 'POST', data: { cityId: 5, title: '政策', updateTime: '2026-07-15', message: '正文' } }],
  ['medical policy delete', () => medicalPolicy.deleteMedicalPolicy(13), { url: '/medical_policys/13', method: 'DELETE', data: { id: 13 } }],
  ['medical policy update', () => medicalPolicy.modifyMedicalPolicyInfo(13, 6, '新政策', '2026-07-16', '新正文'), { url: '/medical_policys/13', method: 'PUT', data: { cityId: 6, title: '新政策', updateTime: '2026-07-16', message: '新正文' } }],

  ['sale page', () => sale.getSalePlaceInfo(4, 10, '药房'), { url: '/sales/4/10', method: 'GET', params: { name: '药房' } }],
  ['sale all', () => sale.getAllSalePlaceInfo(), { url: '/sales', method: 'GET' }],
  ['sale add', () => sale.addSalePlace({ saleName: '药房', longitude: 105.4 }), { url: '/sales', method: 'POST', data: { saleName: '药房', longitude: 105.4 } }],
  ['sale delete', () => sale.deleteSalePlace(14), { url: '/sales/14', method: 'DELETE' }],
  ['sale update', () => sale.modifySalePlaceInfo(14, { saleName: '新药房' }), { url: '/sales/14', method: 'PUT', data: { saleName: '新药房' } }],
];

describe('admin API request contracts', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.request.mockResolvedValue(successfulResponse);
  });

  it.each(cases)('%s', async (_name, invoke, expectedConfig) => {
    await invoke();
    expect(mocks.request).toHaveBeenCalledOnce();
    expect(mocks.request).toHaveBeenCalledWith(expectedConfig);
  });
});
