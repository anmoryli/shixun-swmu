import { beforeEach, describe, expect, it, vi } from 'vitest';

const { messageMock } = vi.hoisted(() => ({ messageMock: vi.fn() }));

vi.mock('element-plus', () => ({
  ElMessage: messageMock,
}));

import {
  judgeAddResult,
  judgeDeleteResult,
  judgeModifyResult,
  judgeQueryResult,
  judgeResetResult,
} from './app';

const response = (code) => ({ data: { code } });

describe('business response helpers', () => {
  beforeEach(() => messageMock.mockClear());

  it.each([
    [judgeDeleteResult, 20000, 'success', '删除成功'],
    [judgeDeleteResult, 50000, 'error', '删除失败'],
    [judgeAddResult, 20000, 'success', '新增成功'],
    [judgeAddResult, 10001, 'error', '该手机号已被注册'],
    [judgeAddResult, 50000, 'error', '新增失败'],
    [judgeModifyResult, 20000, 'success', '修改成功'],
    [judgeModifyResult, 10001, 'error', '该手机号已被注册'],
    [judgeModifyResult, 50000, 'error', '修改失败'],
    [judgeResetResult, 20000, 'success', '重置密码成功'],
    [judgeResetResult, 50000, 'error', '重置密码失败'],
  ])('maps API code %i to the expected notification', (helper, code, type, text) => {
    const res = response(code);

    expect(helper(res)).toBe(res);
    expect(messageMock).toHaveBeenCalledWith(expect.objectContaining({ type }));
    expect(messageMock.mock.calls[0][0].message).toContain(text);
  });

  it('returns a successful query response unchanged without a notification', () => {
    const res = response(20000);

    expect(judgeQueryResult(res)).toBe(res);
    expect(messageMock).not.toHaveBeenCalled();
  });

  it('returns false and reports an unsuccessful query', () => {
    expect(judgeQueryResult(response(50000))).toBe(false);
    expect(messageMock).toHaveBeenCalledWith({ type: 'error', message: '加载数据失败!' });
  });
});
