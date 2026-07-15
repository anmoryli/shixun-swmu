import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { debounce } from './debounce';

describe('debounce', () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  it('只在停止触发 wait 毫秒后执行一次', () => {
    const fn = vi.fn();
    const debounced = debounce(fn, 300);

    debounced('a');
    debounced('ab');
    debounced('abc');

    expect(fn).not.toHaveBeenCalled();
    vi.advanceTimersByTime(299);
    expect(fn).not.toHaveBeenCalled();
    vi.advanceTimersByTime(1);
    expect(fn).toHaveBeenCalledTimes(1);
    expect(fn).toHaveBeenCalledWith('abc');
  });

  it('cancel() 取消待执行调用', () => {
    const fn = vi.fn();
    const debounced = debounce(fn, 300);

    debounced();
    debounced.cancel();
    vi.advanceTimersByTime(1000);

    expect(fn).not.toHaveBeenCalled();
  });

  it('保持调用时的 this 与参数', () => {
    const obj = { factor: 10 };
    const fn = vi.fn(function (x) {
      return this.factor * x;
    });
    const debounced = debounce(fn, 100);

    debounced.call(obj, 5);
    vi.advanceTimersByTime(100);

    expect(fn).toHaveBeenCalledTimes(1);
    expect(fn.mock.results[0].value).toBe(50);
  });
});
