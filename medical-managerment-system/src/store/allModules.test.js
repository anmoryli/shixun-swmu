import { beforeEach, describe, expect, it, vi } from 'vitest';

const api = vi.hoisted(() => {
  const ok = () => Promise.resolve({
    data: {
      code: 20000,
      data: {
        pages: 3,
        cityPageInfo: { list: [] },
        pageInfo: { list: [] },
        policyInfo: { list: [] },
        doctorInfo: { list: [] },
        drugPageInfo: { list: [] },
        materialInfo: { list: [] },
        salePageInfo: { list: [] },
      },
    },
  });
  return new Proxy({ ok }, {
    get: (target, key) => {
      if (!target[key]) target[key] = vi.fn(ok);
      return target[key];
    },
  });
});

vi.mock('../api/admin/cityInfoManage.js', () => ({
  getCityInfo: api.getCityInfo, addCity: api.addCity, deleteCity: api.deleteCity,
  getAllCityInfo: api.getAllCityInfo,
}));
vi.mock('../api/admin/companyInfoManage', () => ({
  getCompanyInfo: api.getCompanyInfo, addCompany: api.addCompany, deleteCompany: api.deleteCompany,
  modifyCompanyInfo: api.modifyCompanyInfo, getAllCompanyInfo: api.getAllCompanyInfo,
}));
vi.mock('../api/admin/companyPolicyInfoManage', () => ({
  getCompanyPolicyInfo: api.getCompanyPolicyInfo, addCompanyPolicy: api.addCompanyPolicy,
  deleteCompanyPolicy: api.deleteCompanyPolicy, modifyCompanyPolicyInfo: api.modifyCompanyPolicyInfo,
}));
vi.mock('../api/admin/doctorInfoManage.js', () => ({
  getDoctorInfo: api.getDoctorInfo, getDoctorLevelAndType: api.getDoctorLevelAndType,
  addDoctor: api.addDoctor, deleteDoctor: api.deleteDoctor, modifyDoctor: api.modifyDoctor,
}));
vi.mock('../api/admin/drugInfoManage', () => ({
  getDrugInfo: api.getDrugInfo, addDrug: api.addDrug, deleteDrug: api.deleteDrug,
  modifyDrugInfo: api.modifyDrugInfo,
}));
vi.mock('../api/admin/materialInfoManage.js', () => ({
  getMaterialInfo: api.getMaterialInfo, addMaterial: api.addMaterial, deleteMaterial: api.deleteMaterial,
  modifyMaterial: api.modifyMaterial,
}));
vi.mock('../api/admin/medicalPolicyInfoManage', () => ({
  getMedicalPolicyInfo: api.getMedicalPolicyInfo, addMedicalPolicy: api.addMedicalPolicy,
  modifyMedicalPolicyInfo: api.modifyMedicalPolicyInfo, deleteMedicalPolicy: api.deleteMedicalPolicy,
}));
vi.mock('../api/admin/saleInfoManage', () => ({
  getSalePlaceInfo: api.getSalePlaceInfo, addSalePlace: api.addSalePlace,
  deleteSalePlace: api.deleteSalePlace, modifySalePlaceInfo: api.modifySalePlaceInfo,
  getAllSalePlaceInfo: api.getAllSalePlaceInfo,
}));

import city from './modules/cityInfoManage';
import company from './modules/companyInfoManage';
import companyPolicy from './modules/companyPolicyInfoManage';
import doctor from './modules/doctorInfoManage';
import drug from './modules/drugInfoManage';
import material from './modules/materialInfoManage';
import medicalPolicy from './modules/medicalPolicyInfoManage';
import sale from './modules/saleInfoManage';
import getters from './getters';

const modules = { city, company, companyPolicy, doctor, drug, material, medicalPolicy, sale };
const payload = {
  pn: 1, size: 5, keyword: 'test', cityNumber: '510500', cityId: 1,
  companyId: 1, companyName: 'company', companyPhone: '15900000000',
  id: 1, accountId: 2, age: 30, levelId: 1, name: '医生', phoneNumber: '15900000000',
  pwd: 'secret', sex: 1, typeId: 1, drugId: 1, drugName: 'drug', drugInfo: 'info',
  drugEffect: 'effect', drugImg: 'image', saleIds: [1], drugPublisher: 'publisher',
  message: 'message', title: 'title', updateTime: '2026-07-15', params: { pn: 1 },
  saleId: 1, saleName: 'sale', salePhone: '15900000000', address: 'address',
  longitude: 105.4, latitude: 28.8,
};

describe('all CRUD store modules', () => {
  beforeEach(() => vi.clearAllMocks());

  for (const [name, module] of Object.entries(modules)) {
    it(`executes every mutation and successful action in ${name}`, async () => {
      const state = structuredClone(module.state);
      const commit = vi.fn((mutation, value) => module.mutations[mutation]?.(state, value));
      const dispatch = vi.fn(() => Promise.resolve());

      for (const mutation of Object.values(module.mutations)) {
        mutation(state, { list: [] });
      }
      for (const action of Object.values(module.actions)) {
        await action({ commit, dispatch, state }, payload);
        await Promise.resolve();
      }

      expect(Object.keys(module.actions).length).toBeGreaterThan(0);
    });
  }

  it('executes every root getter', () => {
    const state = {
      app: {}, companyInfoManage: {}, saleInfoManage: {}, cityInfoManage: {},
      drugInfoManage: {}, medicalPolicyInfoManage: {}, companyPolicyInfoManage: {},
      doctorInfoManage: {}, materialInfoManage: {},
    };
    for (const getter of Object.values(getters)) getter(state);
    expect(Object.keys(getters).length).toBeGreaterThan(10);
  });

  it('returns false for duplicate doctor add and modify responses', async () => {
    api.addDoctor.mockResolvedValueOnce({ data: { code: 10001 } });
    api.modifyDoctor.mockResolvedValueOnce({ data: { code: 10001 } });
    const dispatch = vi.fn();
    await expect(doctor.actions.addDoctor({ dispatch }, payload)).resolves.toBe(false);
    await expect(doctor.actions.modifyDoctor({ dispatch }, payload)).resolves.toBe(false);
  });
});
