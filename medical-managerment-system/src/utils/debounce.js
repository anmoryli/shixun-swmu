/**
 * 防抖工具:在连续触发的高频事件(如搜索输入)中,只在停止触发 wait 毫秒后执行一次,
 * 避免每次按键都发请求,降低后端压力与无效查询。
 *
 * @param {Function} fn 需要防抖的函数
 * @param {number} [wait=300] 等待毫秒数
 * @returns {Function & { cancel: () => void }} 防抖后的函数,附带 cancel() 立即取消待执行调用
 */
export function debounce(fn, wait = 300) {
  let timer = null;
  const debounced = function (...args) {
    if (timer) {
      clearTimeout(timer);
    }
    timer = setTimeout(() => {
      timer = null;
      fn.apply(this, args);
    }, wait);
  };
  debounced.cancel = () => {
    if (timer) {
      clearTimeout(timer);
      timer = null;
    }
  };
  return debounced;
}

export default debounce;
