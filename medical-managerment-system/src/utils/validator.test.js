import { describe, expect, it, vi } from 'vitest';

import validators, { doctorRules, searchRules, validatePass } from './validator';

function validate(validator, value) {
  const callback = vi.fn();
  validator({}, value, callback);
  return callback.mock.calls[0]?.[0];
}

describe('validator rules', () => {
  it.each([
    ['a1234', true],
    ['Admin_2026', true],
    ['1abcd', false],
    ['abcd', false],
    ['abc-def', false],
    ['a1234567890123456789', false],
  ])('validates password %s', (password, expected) => {
    expect(validatePass(password)).toBe(expected);
  });

  it('accepts supported mainland mobile numbers', () => {
    const error = validate(doctorRules.phoneRules[0].validator, '15900000000');
    expect(error).toBeUndefined();
  });

  it('rejects empty and malformed mobile numbers', () => {
    expect(validate(doctorRules.phoneRules[0].validator, '')?.message).toContain('不能为空');
    expect(validate(doctorRules.phoneRules[0].validator, '123456')?.message).toContain('格式错误');
  });

  it('enforces Chinese doctor names', () => {
    expect(validate(doctorRules.nameRules[0].validator, '李医生')).toBeUndefined();
    expect(validate(doctorRules.nameRules[0].validator, 'Doctor')?.message).toContain('汉字');
  });

  it('enforces the doctor age range and integer type', () => {
    const ageValidator = doctorRules.ageRules[0].validator;
    expect(validate(ageValidator, 18)).toBeUndefined();
    expect(validate(ageValidator, 100)).toBeUndefined();
    expect(validate(ageValidator, 17)?.message).toContain('合理');
    expect(validate(ageValidator, 20.5)?.message).toContain('数字值');
    expect(validate(ageValidator, null)?.message).toContain('不能为空');
  });

  it('allows an empty numeric search and rejects non-digits', () => {
    const intValidator = searchRules.intRules[0].validator;
    expect(validate(intValidator, '')).toBeUndefined();
    expect(validate(intValidator, '1024')).toBeUndefined();
    expect(validate(intValidator, '10a')?.message).toContain('数字类型');
  });

  it('keeps the shared required and length rules available', () => {
    expect(validators.requiredRules[0].required).toBe(true);
    expect(validators.nameRules[1]).toMatchObject({ min: 4, max: 24 });
    expect(validators.infoRules[1]).toMatchObject({ min: 4, max: 480 });
  });
});
